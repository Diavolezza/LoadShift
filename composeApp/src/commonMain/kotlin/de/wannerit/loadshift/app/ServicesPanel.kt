package de.wannerit.loadshift.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import de.wannerit.loadshift.core.MasterData
import de.wannerit.loadshift.core.Protocol
import de.wannerit.loadshift.core.ServiceConfig
import de.wannerit.loadshift.core.ServiceCut
import de.wannerit.loadshift.core.ServiceModel

/** E7: how the logistics system's services are cut and talk to each other. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ServicesPanel(game: GameController) {
    val model = game.scenario.services ?: return
    val d = game.servicesDraft
    fun set(c: ServiceConfig) { game.servicesDraft = c }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Txt(t("Architecture"), Type.heading)
        Txt(t("Customers track their deliveries; for every request the routes service needs the truck, the customer, the truck's GPS position and the traffic. The tracking must answer within %lld ms on average; positions may be at most %lld s old, traffic information %lld min.",
            model.latencyLimit, model.gpsLimit, model.mapLimit / 60), Type.style(15, color = Palette.secondary))

        ArchitectureDiagram(d, Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)).background(Palette.background))

        Section(t("Cut")) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (cut in ServiceCut.entries) Chip(t(cut.label), d.cut == cut) { set(d.copy(cut = cut)) }
            }
            Txt(t("Microservices: four deployments with two instances each, deployed and scaled on their own. Modular monolith: the same modules in one deployment, calls between them are method calls."), Type.caption)
        }
        if (d.cut == ServiceCut.MICROSERVICES) {
            Section(t("Between the services")) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (p in Protocol.entries) Chip(t(p.label), d.protocol == p) { set(d.copy(protocol = p)) }
                }
                Txt(t("Master data (customers, trucks)"), Type.caption)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (m in MasterData.entries) Chip(t(m.label), d.masterData == m) { set(d.copy(masterData = m)) }
                }
                Txt(t("With events the routes service keeps its own copy of customers and trucks; a change arrives a few seconds later."), Type.caption)
                Option(d.serverless, t("Customer and truck services as serverless functions"), t("No instance runs while nobody calls them; a call starts one (the first after a pause takes seconds).")) {
                    set(d.copy(serverless = !d.serverless))
                }
            }
        }
        Section(t("External providers")) {
            Txt(t("GPS positions reused for"), Type.caption)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (s in ServiceModel.GPS_TTLS) Chip(ttlText(s), d.gpsTtl == s) { set(d.copy(gpsTtl = s)) }
            }
            Txt(t("Traffic information reused for"), Type.caption)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (s in ServiceModel.MAP_TTLS) Chip(ttlText(s), d.mapTtl == s) { set(d.copy(mapTtl = s)) }
            }
            Txt(t("A cache instance (Redis) keeps the answers; without it every request asks both providers."), Type.caption)
        }

        // The draft: work per request and response time at the evening peak
        val peak = model.peakRate.toDouble()
        Card(Modifier.fillMaxWidth(), color = Palette.background, padding = 14.dp) {
            Txt(t("At the evening peak with these settings"), Type.subheading)
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Txt(t("CPU per tracking request"), Type.callout, Modifier.weight(1f))
                Txt(t("%@ ms", Format.number(model.cpuPerRequest(d, peak), 1)), Type.callout)
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Txt(t("Instances always running"), Type.callout, Modifier.weight(1f))
                Txt("${model.replicas(d)}", Type.callout)
            }
            val latency = model.latency(d, peak)
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Txt(t("Response time of the tracking"), Type.callout, Modifier.weight(1f))
                Txt(t("%lld ms", latency), Type.style(15, FontWeight.SemiBold, if (latency <= model.latencyLimit) Palette.ink else Palette.red))
            }
        }
        ApplyRow(game) { servicesText(it.services) }
    }
}

private fun ttlText(s: Int): String = when {
    s == 0 -> t("never")
    s < 60 -> "$s s"
    s < 3_600 -> "${s / 60} min"
    else -> "${s / 3_600} h"
}

/** Boxes and arrows of the logistics system as the draft would build it. */
@Composable
private fun ArchitectureDiagram(c: ServiceConfig, modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    Canvas(modifier.padding(8.dp)) { drawArchitecture(c, measurer) }
}

private fun DrawScope.drawArchitecture(c: ServiceConfig, measurer: TextMeasurer) {
    val w = size.width; val h = size.height
    val modulith = c.cut == ServiceCut.MODULITH
    val events = !modulith && c.masterData == MasterData.EVENTS
    val serverless = !modulith && c.serverless
    val ink = Palette.ink
    fun box(x0: Float, y0: Float, x1: Float, y1: Float, label: String, fill: Color, dashed: Boolean = false) {
        val o = Offset(x0 * w, y0 * h); val s = Size((x1 - x0) * w, (y1 - y0) * h)
        drawRoundRect(fill, o, s, CornerRadius(6.dp.toPx()))
        drawRoundRect(ink.copy(alpha = 0.6f), o, s, CornerRadius(6.dp.toPx()),
            style = Stroke(1.dp.toPx(), pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(5f, 4f)) else null))
        val layout = measurer.measure(label, Type.style(11, FontWeight.SemiBold))
        drawText(layout, topLeft = Offset(o.x + (s.width - layout.size.width) / 2, o.y + (s.height - layout.size.height) / 2))
    }
    fun arrow(x0: Float, y0: Float, x1: Float, y1: Float, color: Color, dotted: Boolean, label: String? = null) {
        val a = Offset(x0 * w, y0 * h); val b = Offset(x1 * w, y1 * h)
        drawLine(color, a, b, 1.5.dp.toPx(), pathEffect = if (dotted) PathEffect.dashPathEffect(floatArrayOf(3f, 4f)) else null)
        val dir = (b - a) / (b - a).getDistance()
        val n = Offset(-dir.y, dir.x)
        val tip = 6.dp.toPx()
        drawLine(color, b, b - dir * tip + n * tip * 0.5f, 1.5.dp.toPx())
        drawLine(color, b, b - dir * tip - n * tip * 0.5f, 1.5.dp.toPx())
        label?.let {
            val layout = measurer.measure(it, Type.style(9, color = color))
            val m = (a + b) / 2f
            drawText(layout, topLeft = Offset(m.x - layout.size.width / 2f, m.y - layout.size.height - 2f))
        }
    }
    val service = Color(0xFFDCE9F7)
    val external = Color(0xFFEDEDED)
    if (modulith) {
        drawRoundRect(Palette.accent.copy(alpha = 0.08f), Offset(0.01f * w, 0.02f * h), Size(0.64f * w, 0.92f * h), CornerRadius(10.dp.toPx()))
        val layout = measurer.measure(t("Modular monolith"), Type.style(10, FontWeight.SemiBold, Palette.accent))
        drawText(layout, topLeft = Offset(0.03f * w, 0.84f * h))
    }
    val callColor = if (modulith) Palette.secondary else Color(0xFFB0741A)
    val proto = if (modulith) null else if (c.protocol == Protocol.GRPC) "gRPC" else "JSON"
    box(0.03f, 0.08f, 0.27f, 0.30f, t("Tracking"), service)
    box(0.37f, 0.08f, 0.62f, 0.30f, t("Routes"), service)
    box(0.05f, 0.62f, 0.29f, 0.80f, (if (serverless) "λ " else "") + t("Customers"), service, dashed = serverless)
    box(0.37f, 0.62f, 0.61f, 0.80f, (if (serverless) "λ " else "") + t("Trucks"), service, dashed = serverless)
    arrow(0.27f, 0.19f, 0.37f, 0.19f, callColor, false, proto)
    if (events) {
        box(0.20f, 0.40f, 0.46f, 0.52f, "Kafka", Color(0xFFE6F2EA))
        arrow(0.17f, 0.62f, 0.25f, 0.52f, Palette.accent, true)
        arrow(0.49f, 0.62f, 0.42f, 0.52f, Palette.accent, true)
        arrow(0.40f, 0.40f, 0.46f, 0.30f, Palette.accent, true, t("events"))
    } else {
        arrow(0.42f, 0.30f, 0.20f, 0.62f, callColor, false, proto)
        arrow(0.50f, 0.30f, 0.49f, 0.62f, callColor, false)
    }
    val cached = c.gpsTtl > 0 || c.mapTtl > 0
    if (cached) box(0.655f, 0.08f, 0.79f, 0.56f, t("Cache"), Color(0xFFF3E9FB))
    box(0.82f, 0.08f, 0.99f, 0.28f, "GPS", external, dashed = true)
    box(0.82f, 0.36f, 0.99f, 0.56f, t("Traffic"), external, dashed = true)
    val from = if (cached) 0.79f else 0.62f
    if (cached) arrow(0.62f, 0.25f, 0.655f, 0.25f, callColor, false)
    arrow(from, 0.18f, 0.82f, 0.18f, Palette.secondary, c.gpsTtl > 0)
    arrow(from, if (cached) 0.46f else 0.28f, 0.82f, 0.46f, Palette.secondary, c.mapTtl > 0)
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Palette.background).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Txt(title, Type.style(15, FontWeight.SemiBold))
        content()
    }
}

@Composable
private fun Option(on: Boolean, title: String, note: String, toggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = toggle).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Tick(on)
        Column(Modifier.weight(1f)) {
            Txt(title, Type.style(14, FontWeight.SemiBold))
            Txt(note, Type.caption)
        }
    }
}

@Composable
private fun Chip(text: String, chosen: Boolean, onClick: () -> Unit) {
    Txt(
        text, Type.style(13, FontWeight.SemiBold, if (chosen) Color.White else Palette.ink),
        Modifier.clip(RoundedCornerShape(50)).background(if (chosen) Palette.accent else Palette.panel)
            .border(1.dp, Palette.divider, RoundedCornerShape(50))
            .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** "microservices, gRPC with Protobuf, events, functions, positions 1 min, traffic 5 min" */
fun servicesText(c: ServiceConfig): String = listOfNotNull(
    t(c.cut.label),
    if (c.cut == ServiceCut.MICROSERVICES) t(c.protocol.label) else null,
    if (c.cut == ServiceCut.MICROSERVICES) t(c.masterData.label) else null,
    if (c.cut == ServiceCut.MICROSERVICES && c.serverless) t("serverless functions") else null,
    t("positions %@", ttlText(c.gpsTtl)),
    t("traffic %@", ttlText(c.mapTtl)),
).joinToString(", ")
