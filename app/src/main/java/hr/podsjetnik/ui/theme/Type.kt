package hr.podsjetnik.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import hr.podsjetnik.R

// Figtree variable font (res/font/figtree.ttf), one file for weights 400–700 (HANDOFF §2, §7.2).
private fun figtree(weight: FontWeight) = Font(
    resId = R.font.figtree,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Figtree = FontFamily(
    figtree(FontWeight.Normal),
    figtree(FontWeight.Medium),
    figtree(FontWeight.SemiBold),
    figtree(FontWeight.Bold),
)

// ponytail: tabular figures on every style, since dates and times appear everywhere (HANDOFF §7.2)
private fun style(size: Int, lineHeight: Int, weight: FontWeight) = TextStyle(
    fontFamily = Figtree,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    fontWeight = weight,
    fontFeatureSettings = "tnum",
)

// HANDOFF §7.2 styles mapped onto Material 3 roles.
val PodsjetnikTypography = Typography(
    headlineLarge = style(32, 38, FontWeight.Bold), // Display: "Danas", big numbers
    headlineMedium = style(28, 34, FontWeight.Bold), // Headline: screen and Detalji titles
    titleLarge = style(22, 28, FontWeight.Bold), // Title: form and sheet titles
    titleMedium = style(17, 22, FontWeight.SemiBold), // Card title
    titleSmall = style(15, 20, FontWeight.Bold), // Section label
    bodyLarge = style(16, 24, FontWeight.Normal), // Body
    bodyMedium = style(14, 20, FontWeight.Normal), // Body small: meta, helper
    labelLarge = style(16, 24, FontWeight.SemiBold), // Buttons
    labelMedium = style(13, 18, FontWeight.SemiBold), // Caption: chips
    labelSmall = style(13, 18, FontWeight.Medium),
)
