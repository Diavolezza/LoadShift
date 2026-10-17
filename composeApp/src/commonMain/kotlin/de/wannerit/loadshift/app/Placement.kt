package de.wannerit.loadshift.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.wannerit.loadshift.core.GameTime
import de.wannerit.loadshift.core.Packing
import de.wannerit.loadshift.core.VmKind

/** Colors of the VM kinds. */
fun vmColor(kind: VmKind): Color = when (kind) {
    VmKind.FRONT_END -> Palette.interactiveLoad
    VmKind.BATCH_WORKER -> Palette.amber
    VmKind.REPORTING -> Color(0xFF8E6CC9)
    VmKind.DATABASE -> Color(0xFF2A9D8F)
    VmKind.CACHE -> Color(0xFF9AA5A1)
}

/**
 * Scenario 5: the hosts as bins. Each column is one host; its height is the RAM, the VMs are stacked in it by their
 * RAM. Below each bin the host's CPU over the day: the expected work of its VMs (dotted), what happened so far
 * (area), and the line above which requests get slow.
 */
@Composable
fun PlacementBoard(game: GameController, modifier: Modifier = Modifier) {
    val simulation = game.simulation
    val vms = game.scenario.vms
    val placement = game.placement ?: return
    val selected = game.selectedVm
    // Drag and drop: where each host column is, where the dragged VM is (root coordinates).
    val hostBounds = remember { mutableStateMapOf<Int, Rect>() }
    var dragPos by remember { mutableStateOf<Offset?>(null) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    BoxWithConstraints(modifier.padding(16.dp).onGloballyPositioned { origin = it.positionInRoot() }) {
        val chartHeight = 74.dp
        val binHeight = (maxHeight - chartHeight - 92.dp).coerceAtLeast(160.dp)
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (host in simulation.servers.indices) {
                val server = simulation.servers[host]
                val onHost = vms.indices.filter { placement[it] == host }
                val ramUsed = onHost.sumOf { vms[it].ramGb }
                val ram = server.kind.ramGb
                val peak = Packing.expectedPeakMilli(vms, placement, host)
                val safe = server.kind.capacity * Packing.SAFE
                val fits = selected != null && placement[selected] != host && vms[selected].ramGb <= ram - ramUsed
                Column(
                    Modifier.weight(1f).onGloballyPositioned { hostBounds[host] = it.boundsInRoot() }.clip(RoundedCornerShape(10.dp))
                        .background(
                            when {
                                selected == null || placement[selected] == host -> Color.Transparent
                                fits -> Palette.accent.copy(alpha = 0.10f)
                                else -> Palette.red.copy(alpha = 0.06f)
                            },
                        )
                        .clickable(enabled = game.canPlace && selected != null) { game.moveSelectedTo(host) }
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Txt(t("Host %lld", host + 1), Type.style(13, FontWeight.SemiBold, if (onHost.isEmpty()) Palette.secondary else Palette.ink))
                    Txt(if (onHost.isEmpty()) t("off") else t("%lld GB free", ram - ramUsed), Type.style(11, color = Palette.secondary))
                    // The bin: RAM from the bottom up.
                    Column(
                        Modifier.fillMaxWidth().height(binHeight)
                            .border(1.5.dp, if (onHost.isEmpty()) Palette.divider else Palette.secondary.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(3.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Bottom),
                    ) {
                        for (vm in onHost.sortedByDescending { vms[it].ramGb }) key(vm) {
                            val kind = vms[vm]
                            val isSelected = vm == selected
                            var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                            Box(
                                Modifier.fillMaxWidth().height(binHeight * (kind.ramGb.toFloat() / ram) - 2.dp)
                                    .onGloballyPositioned { coords = it }
                                    .pointerInput(vm, game.canPlace) {
                                        if (!game.canPlace) return@pointerInput
                                        detectDragGestures(
                                            onDragStart = { o -> game.pickVm(vm); dragPos = coords?.localToRoot(o) },
                                            onDrag = { change, amount -> change.consume(); dragPos = dragPos?.plus(amount) },
                                            onDragEnd = {
                                                val p = dragPos
                                                val target = hostBounds.entries.firstOrNull { p != null && it.value.contains(p) }?.key
                                                if (target != null) game.moveSelectedTo(target) else game.dropVm()
                                                dragPos = null
                                            },
                                            onDragCancel = { dragPos = null; game.dropVm() },
                                        )
                                    }
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(vmColor(kind).copy(alpha = if (isSelected) 1f else 0.8f))
                                    .border(if (isSelected) 2.5.dp else 0.dp, if (isSelected) Palette.ink else Color.Transparent, RoundedCornerShape(4.dp))
                                    .clickable(enabled = game.canPlace) { game.selectVm(vm) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Txt(t(kind.short), Type.style(11, FontWeight.SemiBold, Color.White).copy(textAlign = TextAlign.Center), maxLines = 1)
                            }
                        }
                    }
                    HostChart(game, host, Modifier.fillMaxWidth().height(chartHeight))
                    Txt(
                        t("peak %@", Format.number(peak / 1_000.0, 1)),
                        Type.style(11, FontWeight.SemiBold, if (peak > safe) Palette.red else Palette.secondary),
                    )
                }
            }
        }
        // The dragged VM follows the mouse.
        val p = dragPos
        if (p != null && selected != null) {
            val kind = vms[selected]
            Box(
                Modifier.offset { IntOffset((p.x - origin.x).toInt() - 36.dp.roundToPx(), (p.y - origin.y).toInt() - 14.dp.roundToPx()) }
                    .size(72.dp, 28.dp).alpha(0.9f).background(vmColor(kind), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Txt(t(kind.short), Type.style(11, FontWeight.SemiBold, Color.White))
            }
        }
    }
}

/** The CPU of one host over the day: expected (dotted), so far (area), capacity and the slow line. */
@Composable
private fun HostChart(game: GameController, host: Int, modifier: Modifier) {
    val simulation = game.simulation
    val vms = game.scenario.vms
    val placement = game.placement ?: return
    val version = game.version
    val capacity = simulation.servers[host].kind.capacity
    val start = game.scenario.startTick
    val end = game.scenario.endTick
    val onHost = vms.indices.filter { placement[it] == host }
    val past = simulation.hostHistory(host)
    Canvas(modifier.background(Palette.background, RoundedCornerShape(4.dp))) {
        if (version < 0) return@Canvas
        val top = capacity * 1.25f
        fun x(t: Int) = size.width * (t - start) / (end - start).toFloat()
        fun y(units: Float) = size.height * (1f - (units / top).coerceIn(0f, 1f))
        drawLine(Palette.secondary.copy(alpha = 0.5f), Offset(0f, y(capacity.toFloat())), Offset(size.width, y(capacity.toFloat())), 1f)
        val slow = capacity * Packing.SAFE / 1_000f
        drawLine(Palette.red.copy(alpha = 0.6f), Offset(0f, y(slow)), Offset(size.width, y(slow)), 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f)))
        if (past.isNotEmpty()) {
            val area = Path().apply {
                moveTo(x(start), size.height)
                past.forEachIndexed { i, milli -> lineTo(x(start + i), y(milli / 1_000f)); lineTo(x(start + i + 1), y(milli / 1_000f)) }
                lineTo(x(start + past.size), size.height)
                close()
            }
            drawPath(area, Palette.capacity.copy(alpha = 0.8f))
        }
        if (onHost.isNotEmpty()) {
            val line = Path().apply {
                for (t in start..end) {
                    val units = onHost.sumOf { vms[it].expectedMilli(t) } / 1_000f
                    if (t == start) moveTo(x(t), y(units)) else lineTo(x(t), y(units))
                }
            }
            drawPath(line, Palette.demand, style = Stroke(1.2f * density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f * density, 2f * density))))
        }
    }
}

/** Scenario 5, right side: what to do, the state of the placement, the VM kinds. */
@Composable
fun PlacementPanel(game: GameController) {
    val vms = game.scenario.vms
    val placement = game.placement ?: return
    val servers = game.simulation.servers
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Txt(t("Placement"), Type.heading)
        Txt(t("The previous team spread the 24 VMs over all ten hosts. Pack them onto fewer hosts – a host without a VM is switched off. Drag a VM onto another host, or click it and then the host. Once the day runs, the placement is fixed."), Type.style(15, color = Palette.secondary))
        val used = placement.toSet()
        val ramUsed = vms.sumOf { it.ramGb }
        val ramAll = used.sumOf { servers[it].kind.ramGb }
        Card(Modifier.fillMaxWidth(), color = Palette.background, padding = 14.dp) {
            Row(Modifier.fillMaxWidth()) {
                Txt(t("Hosts in use"), Type.callout, Modifier.weight(1f))
                Txt(t("%lld of %lld", used.size, servers.size), Type.style(15, FontWeight.SemiBold))
            }
            Row(Modifier.fillMaxWidth()) {
                Txt(t("RAM in use"), Type.callout, Modifier.weight(1f))
                Txt(t("%lld of %lld GB", ramUsed, ramAll), Type.style(15, FontWeight.SemiBold))
            }
        }
        val hot = used.sorted().filter { Packing.expectedPeakMilli(vms, placement, it) > servers[it].kind.capacity * Packing.SAFE }
        for (host in hot) {
            val peak = Packing.expectedPeakMilli(vms, placement, host)
            Row(Modifier.fillMaxWidth().background(Palette.red.copy(alpha = 0.1f), RoundedCornerShape(10.dp)).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CheckMark(ok = false, warning = true)
                Txt(t("Host %lld: expected peak %@ of %lld load units – above %@ requests get slow.", host + 1,
                    Format.number(peak / 1_000.0, 1), servers[host].kind.capacity, Format.wholePercent(Packing.SAFE)), Type.callout, Modifier.weight(1f))
            }
        }
        game.placementMessage?.let { Txt(it, Type.style(14, FontWeight.SemiBold, Palette.red)) }
        game.selectedVm?.let { vm ->
            Txt(t("Picked up: %@ (%lld GB). Now click a host.", t(vms[vm].title), vms[vm].ramGb), Type.style(14, FontWeight.SemiBold, Palette.accent))
        }
        if (!game.canPlace) Txt(t("The day is running: the placement is fixed."), Type.callout)
        HDivider()
        Txt(t("The VMs"), Type.subheading)
        for (kind in VmKind.entries) {
            val count = vms.count { it == kind }
            if (count == 0) continue
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(14.dp).background(vmColor(kind), RoundedCornerShape(3.dp)))
                Column(Modifier.weight(1f)) {
                    Txt("$count × " + t(kind.title), Type.style(14, FontWeight.SemiBold))
                    Txt(t("%lld GB RAM · peak %lld load units", kind.ramGb, kind.peak), Type.caption)
                }
                Profile(kind, Modifier.width(72.dp).height(26.dp))
            }
        }
        Txt(t("Each host: %lld GB RAM, %lld load units.", servers.first().kind.ramGb, servers.first().kind.capacity), Type.caption)
        SecondaryButton(t("Spread again (as found)"), enabled = game.canPlace) { game.spreadAgain() }
    }
}

/** The daily profile of a VM kind, small. */
@Composable
private fun Profile(kind: VmKind, modifier: Modifier) {
    Canvas(modifier.background(Palette.background, RoundedCornerShape(4.dp))) {
        val path = Path()
        for (t in 0..GameTime.TICKS_PER_DAY) {
            val x = size.width * t / GameTime.TICKS_PER_DAY
            val y = size.height * (1f - kind.expectedMilli(t) / (kind.peak * 1_000f) * 0.9f)
            if (t == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, vmColor(kind), style = Stroke(1.5f * density))
    }
}
