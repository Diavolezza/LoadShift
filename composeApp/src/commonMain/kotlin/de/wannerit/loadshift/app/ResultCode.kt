package de.wannerit.loadshift.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.wannerit.loadshift.core.Scenario
import kotlinx.coroutines.delay

/**
 * A result code for courses (step 9): scenario, stars and CO₂ change, bound to the player's name by a check sum, so
 * that a teacher can check a handed-in result without a server. "LS-12-3-821-K7Q4M": scenario number, stars, CO₂ below
 * the reference in per mille (N for more), check. Not a secret – the app is open source – but enough to make copying a
 * code from someone else pointless.
 */
object ResultCode {
    private const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

    private fun normal(name: String) = name.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" ")

    private fun check(name: String, number: Int, stars: Int, permille: Long): String {
        var h = 0x811c9dc5L
        for (c in "${normal(name)}|$number|$stars|$permille|load-shift") {
            h = h xor c.code.toLong()
            h = (h * 0x01000193L) and 0xffffffffL
        }
        return buildString { repeat(5) { append(ALPHABET[(h % 32).toInt()]); h /= 32 } }
    }

    fun code(name: String, number: Int, stars: Int, permille: Long): String =
        "LS-$number-$stars-${if (permille < 0) "N${-permille}" else "$permille"}-${check(name, number, stars, permille)}"

    data class Decoded(val scenario: Scenario, val stars: Int, val permille: Long, val valid: Boolean)

    /** The result a code stands for, and whether it belongs to this name; null when it is no code at all. */
    fun decode(name: String, code: String): Decoded? {
        val parts = code.trim().uppercase().split("-")
        if (parts.size != 5 || parts[0] != "LS") return null
        val number = parts[1].toIntOrNull() ?: return null
        val scenario = Scenario.ALL.firstOrNull { it.number == number } ?: return null
        val stars = parts[2].toIntOrNull()?.takeIf { it in 0..3 } ?: return null
        val permille = (if (parts[3].startsWith("N")) parts[3].drop(1).toLongOrNull()?.let { -it } else parts[3].toLongOrNull()) ?: return null
        return Decoded(scenario, stars, permille, check(name, number, stars, permille) == parts[4])
    }
}

/** A one-line text field in the app's look. */
@Composable
fun TextInput(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    BasicTextField(
        value, onChange, singleLine = true, textStyle = Type.style(16), cursorBrush = SolidColor(Palette.accent),
        modifier = modifier,
        decorationBox = { inner ->
            Box(
                Modifier.fillMaxWidth().background(Palette.panel, RoundedCornerShape(10.dp)).border(1.dp, Palette.divider, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
            ) {
                if (value.isEmpty()) Txt(placeholder, Type.style(16, color = Palette.secondary))
                inner()
            }
        },
    )
}

/** End of a scenario: the result code for a course, with the player's name (kept on the device). */
@Composable
fun ResultCodeCard(scenario: Scenario, stars: Int, permille: Long) {
    var name by remember { mutableStateOf(Storage.getString("name")) }
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) { if (copied) { delay(2_000); copied = false } }
    Card(Modifier.fillMaxWidth(), color = Palette.background, padding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Txt(t("Result code for your course"), Type.subheading)
            Txt(t("Your name and this result give a code your teacher can check. It shows the scenario, the stars and the CO₂."), Type.style(14, color = Palette.secondary))
            TextInput(name, { name = it; Storage.setString("name", it) }, t("Your name"), Modifier.fillMaxWidth())
            if (name.isNotBlank()) {
                val code = ResultCode.code(name, scenario.number, stars, permille)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SelectionContainer(Modifier.weight(1f)) { Txt(code, Type.style(20, FontWeight.Bold).copy(fontFeatureSettings = "tnum")) }
                    SecondaryButton(if (copied) t("Copied") else t("Copy")) { copyText(code); copied = true }
                }
            }
        }
    }
}

/** For teachers: is a handed-in code valid for this name, and what does it stand for? */
@Composable
fun CodeCheck() {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Txt(t("Check a result code"), Type.style(15, FontWeight.SemiBold))
        TextInput(name, { name = it }, t("Name of the student"), Modifier.fillMaxWidth())
        TextInput(code, { code = it }, t("Code, e.g. LS-12-3-821-K7Q4M"), Modifier.fillMaxWidth())
        if (code.isNotBlank()) {
            val d = ResultCode.decode(name, code)
            val text = when {
                d == null -> t("This is not a result code.")
                !d.valid -> t("The code does not belong to this name.")
                else -> t("Valid: scenario %@ %@ · %lld of 3 stars · CO₂ %@", d.scenario.shownLabel, t(d.scenario.title), d.stars, Format.changePermille(-d.permille))
            }
            Txt(text, Type.style(15, FontWeight.SemiBold, if (d?.valid == true) Palette.accent else Palette.red))
        }
    }
}
