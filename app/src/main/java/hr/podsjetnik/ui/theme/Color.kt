package hr.podsjetnik.ui.theme

// Reference tokens from the Podsjetnik design (HANDOFF.md section 7.1).

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class PodsjetnikColors(
    val bg: Color,
    val card: Color,
    val c1: Color,
    val c2: Color,
    val line: Color,
    val outline: Color,
    val text: Color,
    val muted: Color,
    val faint: Color,
    val pri: Color,
    val onPri: Color,
    val priC: Color,
    val onPriC: Color,
    // Status: overdue (red)
    val red: Color,
    val redS: Color,
    val onRedS: Color,
    val redCard: Color,
    val redLine: Color,
    val redC: Color,
    val onRedC: Color,
    // Status: today (amber)
    val amb: Color,
    val ambDot: Color,
    val ambLine: Color,
    val ambC: Color,
    val onAmbC: Color,
    // Status: upcoming (neutral)
    val neuC: Color,
    val onNeuC: Color,
    // Status: done (green)
    val grn: Color,
    val grnS: Color,
    val onGrnS: Color,
    val grnC: Color,
    val onGrnC: Color,
    // Chrome
    val nav: Color,
    val navInd: Color,
    val sheet: Color,
    val scrim: Color,
)

val LightPodsjetnikColors = PodsjetnikColors(
    bg = Color(0xFFF5F6F8),
    card = Color(0xFFFFFFFF),
    c1 = Color(0xFFEDEFF3),
    c2 = Color(0xFFE2E6EC),
    line = Color(0xFFDDE1E7),
    outline = Color(0xFFB9BEC8),
    text = Color(0xFF171A1F),
    muted = Color(0xFF4A505B),
    faint = Color(0xFF666C77),
    pri = Color(0xFF2E55A5),
    onPri = Color(0xFFFFFFFF),
    priC = Color(0xFFDCE4F7),
    onPriC = Color(0xFF132F69),
    red = Color(0xFFB3261E),
    redS = Color(0xFFB3261E),
    onRedS = Color(0xFFFFFFFF),
    redCard = Color(0xFFFDF2F0),
    redLine = Color(0xFFF1C9C3),
    redC = Color(0xFFFBE1DD),
    onRedC = Color(0xFF7A1913),
    amb = Color(0xFF865700),
    ambDot = Color(0xFFE09B00),
    ambLine = Color(0xFFEED9A4),
    ambC = Color(0xFFFFE8AE),
    onAmbC = Color(0xFF563900),
    neuC = Color(0xFFE8EBEF),
    onNeuC = Color(0xFF3E444F),
    grn = Color(0xFF1C773D),
    grnS = Color(0xFF1C773D),
    onGrnS = Color(0xFFFFFFFF),
    grnC = Color(0xFFD5F0DC),
    onGrnC = Color(0xFF0F4F27),
    nav = Color(0xFFECEFF3),
    navInd = Color(0xFFD5DEF4),
    sheet = Color(0xFFFFFFFF),
    scrim = Color(0x800F1115), // rgba(15,17,21,0.5)
)

val DarkPodsjetnikColors = PodsjetnikColors(
    bg = Color(0xFF0F1115),
    card = Color(0xFF191C21),
    c1 = Color(0xFF22262C),
    c2 = Color(0xFF2C3138),
    line = Color(0xFF2F343C),
    outline = Color(0xFF4A505A),
    text = Color(0xFFE3E6EB),
    muted = Color(0xFFB0B6C0),
    faint = Color(0xFF8D94A0),
    pri = Color(0xFFADC4FF),
    onPri = Color(0xFF0D2860),
    priC = Color(0xFF27427D),
    onPriC = Color(0xFFDAE3FF),
    red = Color(0xFFFFB4AB),
    redS = Color(0xFFFFB4AB),
    onRedS = Color(0xFF5C1210),
    redCard = Color(0xFF241819),
    redLine = Color(0xFF55302C),
    redC = Color(0xFF4F1D19),
    onRedC = Color(0xFFFFD9D3),
    amb = Color(0xFFF0C25A),
    ambDot = Color(0xFFF0C25A),
    ambLine = Color(0xFF4A3C17),
    ambC = Color(0xFF453300),
    onAmbC = Color(0xFFFFE1A0),
    neuC = Color(0xFF2A2F36),
    onNeuC = Color(0xFFC4C9D2),
    grn = Color(0xFF8FD5A4),
    grnS = Color(0xFF8FD5A4),
    onGrnS = Color(0xFF0B3A1D),
    grnC = Color(0xFF163D24),
    onGrnC = Color(0xFFBCEDCA),
    nav = Color(0xFF16191D),
    navInd = Color(0xFF2B4170),
    sheet = Color(0xFF1D2026),
    scrim = Color(0x9E000000), // rgba(0,0,0,0.62)
)

val LocalPodsjetnikColors = staticCompositionLocalOf { LightPodsjetnikColors }

/** Maps the tokens onto Material 3 roles so stock M3 components look right. */
fun PodsjetnikColors.toColorScheme(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = pri,
        onPrimary = onPri,
        primaryContainer = priC,
        onPrimaryContainer = onPriC,
        secondaryContainer = priC,          // selected segments, nav indicator fallback
        onSecondaryContainer = onPriC,
        background = bg,
        onBackground = text,
        surface = bg,
        onSurface = text,
        onSurfaceVariant = muted,
        surfaceContainerLowest = card,
        surfaceContainerLow = card,
        surfaceContainer = nav,
        surfaceContainerHigh = sheet,
        surfaceContainerHighest = c1,
        outline = outline,
        outlineVariant = line,
        error = red,
        onError = onRedS,
        errorContainer = redC,
        onErrorContainer = onRedC,
        scrim = scrim,
    )
}
