package de.wannerit.loadshift.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.wannerit.loadshift.core.ImageFormat
import de.wannerit.loadshift.core.LazyLoading
import de.wannerit.loadshift.core.PageConfig
import de.wannerit.loadshift.core.Tracking
import de.wannerit.loadshift.core.WebModel

/** E4: images, video, lists and tracking of the shop's pages. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PagesPanel(game: GameController) {
    val model = game.scenario.web ?: return
    val d = game.pageDraft
    fun set(c: PageConfig) { game.pageDraft = c }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Txt(t("Pages"), Type.heading)
        Txt(t("Every byte of a visit travels through the network and is processed on the visitor's device – this scenario counts both, not only the data center. The page must stay fast: the largest image or text block must appear within %@ s (LCP).",
            Format.number(model.lcpLimit / 10.0, 1)), Type.style(15, color = Palette.secondary))

        Section(t("Product images")) {
            Txt(t("Format"), Type.caption)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (f in ImageFormat.entries) Chip(t(f.label), d.format == f) { set(d.copy(format = f)) }
            }
            Option(d.responsive, t("Image sizes for the screen"), t("A phone gets a smaller image than a large monitor (srcset); now every screen gets the 2,000-pixel image.")) {
                set(d.copy(responsive = !d.responsive))
            }
            Txt(t("Lazy loading"), Type.caption)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (l in LazyLoading.entries) Chip(t(l.label), d.lazy == l) { set(d.copy(lazy = l)) }
            }
            Txt(t("Lazily loaded images load only when the visitor scrolls to them."), Type.caption)
        }
        Section(t("Product video")) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip(t("plays by itself"), d.autoplay) { set(d.copy(autoplay = true)) }
                Chip(t("on click"), !d.autoplay) { set(d.copy(autoplay = false)) }
            }
            Txt(t("About one visitor in twenty watches the video."), Type.caption)
        }
        Section(t("Product lists")) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip(t("100 products"), d.longLists) { set(d.copy(longLists = true)) }
                Chip(t("24 and a \"more\" button"), !d.longLists) { set(d.copy(longLists = false)) }
            }
        }
        Section(t("Counting visitors")) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (tr in Tracking.entries) Chip(t(tr.label), d.tracking == tr) { set(d.copy(tracking = tr)) }
            }
            Txt(t("Marketing needs the visitor counts; each way delivers them."), Type.caption)
        }

        // As measured: the pages online now
        val running = game.simulation.policy.pages
        val w = model.weight(running)
        Card(Modifier.fillMaxWidth(), color = Palette.background, padding = 14.dp) {
            Txt(t("A visit to the pages online now"), Type.subheading)
            val rows = listOf(
                t("Images") to w.images, t("Video") to w.video, t("HTML, CSS and scripts") to w.code, t("Tracking") to w.tracking,
            )
            for ((label, kb) in rows) {
                Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                    Txt(label, Type.callout, Modifier.weight(1f))
                    Txt(t("%@ MB", Format.number(kb / 1_000.0, 1)), Type.callout)
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Txt(t("Together"), Type.callout, Modifier.weight(1f))
                Txt(t("%@ MB", Format.number(w.total / 1_000.0, 1)), Type.style(15, FontWeight.SemiBold))
            }
            val lcp = model.lcp(running)
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Txt(t("LCP (75 % of the visits faster)"), Type.callout, Modifier.weight(1f))
                Txt(t("%@ s", Format.number(lcp / 10.0, 1)), Type.style(15, FontWeight.SemiBold, if (lcp <= model.lcpLimit) Palette.ink else Palette.red))
            }
        }
        ApplyRow(game) { pagesText(it.pages) }
    }
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

/** "AVIF, sizes for the screen, lazy below the fold, video on click, 24 per list, one analytics tag" */
fun pagesText(c: PageConfig): String = listOfNotNull(
    t(c.format.label),
    if (c.responsive) t("sizes for the screen") else null,
    if (c.lazy != LazyLoading.OFF) t("lazy: %@", t(c.lazy.label)) else null,
    if (c.autoplay) t("video plays by itself") else t("video on click"),
    if (c.longLists) t("100 per list") else t("24 per list"),
    t(c.tracking.label),
).joinToString(", ")
