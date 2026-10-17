package de.wannerit.loadshift.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import de.wannerit.loadshift.app.resources.Res
import de.wannerit.loadshift.app.resources.inter_bold
import de.wannerit.loadshift.app.resources.inter_regular
import de.wannerit.loadshift.app.resources.inter_semibold
import org.jetbrains.compose.resources.Font
import kotlin.math.abs
import kotlin.math.roundToLong

/** Colors of the app. */
object Palette {
    val background = Color(0xFFF2F5F2)
    val panel = Color.White
    val ink = Color(0xFF212E2B)
    val secondary = Color(0xFF6B7672)
    val divider = Color(0xFFDDE3DF)
    val accent = Color(0xFF1F8F66)
    val amber = Color(0xFFE8A33A)
    val red = Color(0xFFCC4D40)
    val demand = Color(0xFF295CAD)
    val capacity = Color(0xFF9EBFEB)
    val off = Color(0xFFD6D6D6)
    val slab = Color(0xFF404D4A)
    val booting = Color(0xFFB8D1EB)
    val cache = Color(0xFF8E6FD8)
    val interactiveLoad = Color(0xFF5C9EF2)
    val batchLoad = amber

    /** Color of a carbon intensity: green when clean, amber, red when dirty (g/kWh). */
    fun intensity(gramsPerKWh: Int): Color = when {
        gramsPerKWh < 150 -> accent
        gramsPerKWh < 350 -> Color(0xFF8CA845)
        gramsPerKWh < 500 -> amber
        else -> red
    }
}

/** The app's font (Inter, SIL Open Font License) and text styles. */
object Type {
    /** State, so text drawn before the font has loaded (web) is drawn again with it. */
    var family: FontFamily by mutableStateOf(FontFamily.Default)

    fun style(size: Int, weight: FontWeight = FontWeight.Normal, color: Color = Palette.ink) =
        TextStyle(fontFamily = family, fontSize = size.sp, fontWeight = weight, color = color, lineHeight = (size * 1.3).sp)

    val display get() = style(48, FontWeight.Bold)
    val title get() = style(30, FontWeight.Bold)
    val heading get() = style(22, FontWeight.Bold)
    val subheading get() = style(17, FontWeight.SemiBold)
    val body get() = style(17)
    val bodyLarge get() = style(20)
    val callout get() = style(15)
    val caption get() = style(13, color = Palette.secondary)
    val small get() = style(11, color = Palette.secondary)
}

@Composable
fun interFamily(): FontFamily = FontFamily(
    Font(Res.font.inter_regular, FontWeight.Normal),
    Font(Res.font.inter_semibold, FontWeight.SemiBold),
    Font(Res.font.inter_bold, FontWeight.Bold),
)

/** Numbers in the app's language: decimal comma and thin spaces in German. */
object Format {
    private const val NNBSP = ' '

    /** A number with a fixed count of decimals, grouped by thousands ("1.234,5" or "1,234.5"). */
    fun number(value: Double, decimals: Int = 0): String {
        var factor = 1L
        repeat(decimals) { factor *= 10 }
        val scaled = (abs(value) * factor).roundToLong()
        val whole = (scaled / factor).toString()
        val fraction = (scaled % factor).toString().padStart(decimals, '0')
        val group = if (Language.german) '.' else ','
        val grouped = whole.reversed().chunked(3).joinToString(group.toString()).reversed()
        val sign = if (value < 0 && scaled != 0L) "−" else ""
        return sign + grouped + if (decimals > 0) (if (Language.german) "," else ".") + fraction else ""
    }

    /** µg CO₂e as kg with one decimal. */
    fun kilograms(micrograms: Long) = number(micrograms / 1e9, 1)

    /** Per mille as percent with one decimal ("99,3 %"). */
    fun percent(permille: Int) = number(permille / 10.0, 1) + NNBSP + "%"

    /** Per mille as percent without decimals ("70 %"). */
    fun wholePercent(permille: Int) = ((permille + 5) / 10).toString() + NNBSP + "%"

    /** A change given in per mille, signed, with one decimal ("−12,3 %"). */
    fun changePermille(permille: Long): String =
        (if (permille < 0) "−" else "+") + number(abs(permille) / 10.0, 1) + NNBSP + "%"

    /** Signed change of a value against a base, in percent with one decimal. */
    fun change(value: Long, base: Long): String =
        if (base == 0L) "–" else changePermille((value - base) * 1_000 / base)

    /** W as kW with one decimal. */
    fun kilowatts(watts: Int) = number(watts / 1_000.0, 1)

    /** mWh as kWh without decimals. */
    fun kilowattHours(milliwattHours: Long) = number((milliwattHours / 1_000_000).toDouble())
}
