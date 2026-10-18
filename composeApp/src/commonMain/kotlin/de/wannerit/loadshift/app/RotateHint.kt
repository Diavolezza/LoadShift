package de.wannerit.loadshift.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Whether the player has chosen to play upright anyway (for this run of the app). */
private object Upright {
    var anyway by mutableStateOf(false)
}

/**
 * The play screens need width: on a phone held upright they ask to turn it. The game behind stays as it is;
 * "Continue upright" hides the hint until the app is opened again.
 */
@Composable
fun RotateHint(width: Dp, height: Dp) {
    if (Upright.anyway || width >= height || width >= 700.dp) return
    Box(
        Modifier.fillMaxSize().background(Palette.background)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.widthIn(max = 320.dp).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Canvas(Modifier.size(120.dp)) {
                val s = size.width
                val stroke = Stroke(s * 0.035f)
                // The phone upright, an arrow, the phone turned.
                drawRoundRect(Palette.secondary, Offset(s * 0.06f, s * 0.08f), Size(s * 0.26f, s * 0.44f), CornerRadius(s * 0.04f), style = stroke)
                drawRoundRect(Palette.accent, Offset(s * 0.44f, s * 0.56f), Size(s * 0.5f, s * 0.3f), CornerRadius(s * 0.04f), style = stroke)
                val arrow = Path().apply {
                    moveTo(s * 0.42f, s * 0.18f)
                    quadraticTo(s * 0.72f, s * 0.16f, s * 0.74f, s * 0.44f)
                }
                drawPath(arrow, Palette.accent, style = stroke)
                val head = Path().apply {
                    moveTo(s * 0.66f, s * 0.38f); lineTo(s * 0.74f, s * 0.48f); lineTo(s * 0.82f, s * 0.38f)
                }
                drawPath(head, Palette.accent, style = stroke)
            }
            Txt(t("Turn your device"), Type.style(24, FontWeight.Bold).copy(textAlign = TextAlign.Center))
            Txt(t("The game needs the width: the hall, the controls and the timeline sit side by side. Hold your phone sideways."),
                Type.style(17, color = Palette.secondary).copy(textAlign = TextAlign.Center))
            SecondaryButton(t("Continue upright")) { Upright.anyway = true }
        }
    }
}

/**
 * A phone held sideways is only about 330 points high. The play screens are drawn a little smaller there,
 * so the panel's controls and the chart fit next to each other.
 */
@Composable
fun FitLowScreen(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        if (maxWidth > maxHeight && maxHeight < 500.dp) {
            CompositionLocalProvider(LocalDensity provides Density(density.density * 0.85f, density.fontScale)) { content() }
        } else content()
    }
}
