package com.prostuti.student.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.prostuti.student.R

/*
 * Prostuti's type system — two scripts, two jobs, three font groups. Never mix a Bangla style
 * onto Latin/numeral text or vice-versa:
 *  - mono (JetBrains Mono): every piece of Latin-script and numeral UI — buttons, nav labels,
 *    scores, dates, timers, the exam-countdown hero number. The signature "builder-made" texture
 *    of the UI; lean into it, don't treat it as a fallback.
 *  - banglaDisplay (BCC Purno, bundled): headlines, screen titles, hero/splash moments,
 *    subject-card titles. Kept display-only for now; re-test before using at body size.
 *  - banglaText (Noto Sans Bengali, placeholder): body copy, question stems, MCQ options,
 *    captions — anywhere a student reads more than one line of Bangla.
 */

private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private fun googleFontFamily(name: String, vararg weights: FontWeight) = FontFamily(
    weights.map { weight ->
        Font(googleFont = GoogleFont(name), fontProvider = googleFontProvider, weight = weight)
    }
)

val MonoFontFamily = googleFontFamily(
    "JetBrains Mono",
    FontWeight.Normal,
    FontWeight.Medium,
    FontWeight.SemiBold,
    FontWeight.Bold
)

val BanglaTextFontFamily = googleFontFamily(
    "Noto Sans Bengali",
    FontWeight.Normal,
    FontWeight.Medium
)

// BCC Purno — the renamed "July" typeface (bangla.gov.bd). Bundled as a real font, not a
// fallback: res/font/purno_*.ttf.
val BanglaDisplayFontFamily = FontFamily(
    Font(R.font.purno_regular, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.purno_bold, FontWeight.Bold, FontStyle.Normal),
    Font(R.font.purno_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.purno_bold_italic, FontWeight.Bold, FontStyle.Italic)
)

/**
 * The 18 named text styles from the design system's three type groups — use these directly
 * (e.g. `ProstutiTypography.bnBodyLarge`) wherever a style doesn't map onto one of M3's 15
 * canonical [Typography] slots (every `en*` numeral/mono style below also backs
 * [ProstutiMaterialTypography], so prefer `MaterialTheme.typography.*` for those).
 */
object ProstutiTypography {
    // Bangla Display (BCC Purno) — headlines, screen titles, hero/splash moments.
    val bnDisplayLarge = TextStyle(
        fontFamily = BanglaDisplayFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 48.sp,
        lineHeight = 56.sp
    )
    val bnHeadlineLarge = TextStyle(
        fontFamily = BanglaDisplayFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp
    )
    val bnHeadlineMedium = TextStyle(
        fontFamily = BanglaDisplayFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp
    )
    val bnTitleLarge = TextStyle(
        fontFamily = BanglaDisplayFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp
    )

    // Bangla Text (Noto Sans Bengali, placeholder) — body copy, question stems, captions.
    val bnTitleMedium = TextStyle(
        fontFamily = BanglaTextFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    )
    val bnBodyLarge = TextStyle(
        fontFamily = BanglaTextFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp
    )
    val bnBodyMedium = TextStyle(
        fontFamily = BanglaTextFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp
    )
    // MCQ question / option text — a step larger than body text so it's easy to read.
    val bnMcqQuestion = TextStyle(
        fontFamily = BanglaTextFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 29.sp
    )
    val bnMcqOption = TextStyle(
        fontFamily = BanglaTextFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 27.sp
    )
    val bnLabelLarge = TextStyle(
        fontFamily = BanglaTextFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
    val bnLabelMedium = TextStyle(
        fontFamily = BanglaTextFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )

    // English & Numerals (JetBrains Mono) — buttons, nav labels, scores, timers.
    val enDisplayLarge = TextStyle(
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 45.sp,
        lineHeight = 52.sp
    )
    val enHeadlineLarge = TextStyle(
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 30.sp,
        lineHeight = 38.sp
    )

    /** Hero score, streak count, days-left countdown — tabular figures, the signature moment. */
    val enNumeralDisplay = TextStyle(
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 56.sp,
        lineHeight = 60.sp
    )
    val enTitleLarge = TextStyle(
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    )
    val enTitleMedium = TextStyle(
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp
    )
    val enBodyLarge = TextStyle(
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    )
    val enBodyMedium = TextStyle(
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )
    val enLabelLarge = TextStyle(
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )
    val enLabelMedium = TextStyle(
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp
    )
}

// M3's 15 canonical Typography slots, filled with the mono scale above wherever a slot has a
// 1:1 match (English/numeral UI is the default for generic Material components — buttons, nav
// labels, text fields); the remaining slots keep M3's default metrics with the family swapped
// to mono, since this design system doesn't scope them yet.
private val materialTypographyBaseline = Typography()

val ProstutiMaterialTypography = Typography(
    displayLarge = ProstutiTypography.enDisplayLarge,
    displayMedium = materialTypographyBaseline.displayMedium.copy(fontFamily = MonoFontFamily),
    displaySmall = materialTypographyBaseline.displaySmall.copy(fontFamily = MonoFontFamily),
    headlineLarge = ProstutiTypography.enHeadlineLarge,
    headlineMedium = materialTypographyBaseline.headlineMedium.copy(fontFamily = MonoFontFamily),
    headlineSmall = materialTypographyBaseline.headlineSmall.copy(fontFamily = MonoFontFamily),
    titleLarge = ProstutiTypography.enTitleLarge,
    titleMedium = ProstutiTypography.enTitleMedium,
    titleSmall = materialTypographyBaseline.titleSmall.copy(fontFamily = MonoFontFamily),
    bodyLarge = ProstutiTypography.enBodyLarge,
    bodyMedium = ProstutiTypography.enBodyMedium,
    bodySmall = materialTypographyBaseline.bodySmall.copy(fontFamily = MonoFontFamily),
    labelLarge = ProstutiTypography.enLabelLarge,
    labelMedium = ProstutiTypography.enLabelMedium,
    labelSmall = materialTypographyBaseline.labelSmall.copy(fontFamily = MonoFontFamily)
)
