package de.wannerit.loadshift.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Txt(text: String, style: TextStyle = Type.body, modifier: Modifier = Modifier, maxLines: Int = Int.MAX_VALUE) {
    BasicText(text, modifier, style, maxLines = maxLines)
}

/** Filled button in the accent color. */
@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, large: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier
            .alpha(if (enabled) 1f else 0.4f)
            .background(Palette.accent, RoundedCornerShape(50))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = if (large) 14.dp else 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Txt(text, Type.style(if (large) 22 else 16, FontWeight.SemiBold, Color.White))
    }
}

/** Light button. */
@Composable
fun SecondaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier
            .alpha(if (enabled) 1f else 0.4f)
            .background(Palette.ink.copy(alpha = 0.07f), RoundedCornerShape(50))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Txt(text, Type.style(16, FontWeight.Medium))
    }
}

/** Round button with a plus or minus. */
@Composable
fun RoundButton(plus: Boolean, size: Dp = 56.dp, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier
            .size(size)
            .alpha(if (enabled) 1f else 0.35f)
            .background(Palette.accent.copy(alpha = 0.14f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size * 0.36f)) {
            val w = this.size.width
            val stroke = w * 0.16f
            drawLine(Palette.accent, Offset(0f, w / 2), Offset(w, w / 2), stroke)
            if (plus) drawLine(Palette.accent, Offset(w / 2, 0f), Offset(w / 2, w), stroke)
        }
    }
}

/** A five-pointed star, drawn (filled or outlined). */
fun DrawScope.star(center: Offset, radius: Float, color: Color, filled: Boolean) {
    val path = Path()
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) radius else radius * 0.45f
        val angle = -PI / 2 + i * PI / 5
        val p = Offset(center.x + (r * cos(angle)).toFloat(), center.y + (r * sin(angle)).toFloat())
        if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
    }
    path.close()
    if (filled) drawPath(path, color) else drawPath(path, color, style = Stroke(radius * 0.12f))
}

@Composable
fun StarRow(stars: Int, size: Dp = 30.dp, of: Int = 3) {
    Row(horizontalArrangement = Arrangement.spacedBy(size * 0.15f)) {
        repeat(of) { i ->
            Canvas(Modifier.size(size)) {
                star(center, this.size.width / 2, if (i < stars) Palette.amber else Palette.secondary.copy(alpha = 0.5f), i < stars)
            }
        }
    }
}

/** Stars as small glyphs in front of a goal ("★★"). */
@Composable
fun StarLabel(count: Int, size: Dp = 15.dp) {
    Row(Modifier.width(size * 3.6f)) {
        repeat(count) {
            Canvas(Modifier.size(size)) { star(center, this.size.width / 2, Palette.amber, true) }
        }
    }
}

/** A goal with its stars; with a check mark when [ok] is not null. */
@Composable
fun GoalLine(ok: Boolean?, stars: Int, text: String, style: TextStyle = Type.callout) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (ok != null) CheckMark(ok)
        StarLabel(stars)
        Txt(text, style)
    }
}

/** A filled circle with a check, or an empty circle. */
@Composable
fun CheckMark(ok: Boolean, size: Dp = 18.dp, warning: Boolean = false) {
    Canvas(Modifier.size(size)) {
        val r = this.size.width / 2
        when {
            ok -> {
                drawCircle(Palette.accent, r)
                val p = Path().apply {
                    moveTo(r * 0.55f, r * 1.02f); lineTo(r * 0.88f, r * 1.35f); lineTo(r * 1.45f, r * 0.68f)
                }
                drawPath(p, Color.White, style = Stroke(r * 0.22f))
            }
            warning -> {
                drawCircle(Palette.red, r)
                drawLine(Color.White, Offset(r, r * 0.45f), Offset(r, r * 1.15f), r * 0.22f)
                drawCircle(Color.White, r * 0.12f, Offset(r, r * 1.5f))
            }
            else -> drawCircle(Palette.secondary, r * 0.9f, style = Stroke(r * 0.14f))
        }
    }
}

/** White rounded panel. */
@Composable
fun Card(modifier: Modifier = Modifier, color: Color = Palette.panel, padding: Dp = 16.dp, content: @Composable () -> Unit) {
    Column(modifier.background(color, RoundedCornerShape(16.dp)).padding(padding)) { content() }
}

/** Title and value in the top bar. */
@Composable
fun Metric(title: String, value: String, color: Color = Palette.ink) {
    Column {
        Txt(title, Type.caption)
        Txt(value, Type.style(21, FontWeight.SemiBold, color).copy(fontFeatureSettings = "tnum"))
    }
}

/** A small colored swatch with a label (legend of the hall). */
@Composable
fun LegendSwatch(color: Color, text: String, fill: Color? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(width = 22.dp, height = 10.dp).background(color, RoundedCornerShape(2.dp)), contentAlignment = Alignment.CenterStart) {
            if (fill != null) Box(Modifier.padding(start = 3.dp).size(width = 12.dp, height = 4.dp).background(fill))
        }
        Txt(text, Type.caption)
    }
}

/** A thin horizontal line. */
@Composable
fun HDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.divider))
}

/** A thin vertical line. */
@Composable
fun VDivider() {
    Box(Modifier.fillMaxHeight().width(1.dp).background(Palette.divider))
}
