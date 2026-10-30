package com.prostuti.student.ui.theme

import androidx.compose.ui.graphics.Color

// Prostuti brand design system — color tokens.
// Source: the brand design-system artifact (tokens.json, v1). Dark theme is true black
// (#000000 background/surface) by the founder's explicit call; elevation there is carried by the
// surface-container-* ladder rather than shadow. Do not re-tint these — every text-on-fill pair
// here was checked to >=4.5:1 (>=3:1 for large/bold-only text) and is the contract, not a draft.

// primary — filled buttons, active nav/tab indicators, FAB, links.
val PrimaryLight = Color(0xFFE01A00)
val PrimaryDark = Color(0xFFFFA599)
val OnPrimaryLight = Color(0xFFFFFFFF)
val OnPrimaryDark = Color(0xFF660C00)
val PrimaryContainerLight = Color(0xFFFFD2CC)
val PrimaryContainerDark = Color(0xFF991200)
val OnPrimaryContainerLight = Color(0xFF330600)
val OnPrimaryContainerDark = Color(0xFFFFD2CC)

// secondary — tonal buttons, less prominent selected states, supporting accents.
val SecondaryLight = Color(0xFF854F47)
val SecondaryDark = Color(0xFFDBC0BD)
val OnSecondaryLight = Color(0xFFFFFFFF)
val OnSecondaryDark = Color(0xFF422724)
val SecondaryContainerLight = Color(0xFFEDE0DE)
val SecondaryContainerDark = Color(0xFF633B36)
val OnSecondaryContainerLight = Color(0xFF211412)
val OnSecondaryContainerDark = Color(0xFFEDE0DE)

// tertiary — achievement/streak accents, gold highlights, "Go Premium" CTAs.
val TertiaryLight = Color(0xFF97700C)
val TertiaryDark = Color(0xFFF7DFA1)
val OnTertiaryLight = Color(0xFFFFFFFF)
val OnTertiaryDark = Color(0xFF5E4608)
val TertiaryContainerLight = Color(0xFFFBEFD0)
val TertiaryContainerDark = Color(0xFF84620B)
val OnTertiaryContainerLight = Color(0xFF2F2304)
val OnTertiaryContainerDark = Color(0xFFFBEFD0)

// error — destructive actions, form validation errors, failed states.
val ErrorLight = Color(0xFFC43A08)
val ErrorDark = Color(0xFFFBB69D)
val OnErrorLight = Color(0xFFFFFFFF)
val OnErrorDark = Color(0xFF621D04)
val ErrorContainerLight = Color(0xFFFDDBCE)
val ErrorContainerDark = Color(0xFF932C06)
val OnErrorContainerLight = Color(0xFF310F02)
val OnErrorContainerDark = Color(0xFFFDDBCE)

// success — extended role beyond the core five M3 roles. Correct-answer feedback, completed
// states. Always pair with error for MCQ/model-test results; never rely on hue alone (label it too).
val SuccessLight = Color(0xFF2F7F4C)
val SuccessDark = Color(0xFFB5E3C6)
val OnSuccessLight = Color(0xFFFFFFFF)
val OnSuccessDark = Color(0xFF1C4A2D)
val SuccessContainerLight = Color(0xFFDAF1E2)
val SuccessContainerDark = Color(0xFF297043)
val OnSuccessContainerLight = Color(0xFF0E2516)
val OnSuccessContainerDark = Color(0xFFDAF1E2)

// background / surface — true black in dark theme.
val BackgroundLight = Color(0xFFFAFAFA)
val BackgroundDark = Color(0xFF000000)
val OnBackgroundLight = Color(0xFF1B1918)
val OnBackgroundDark = Color(0xFFE7E5E4)
val SurfaceLight = Color(0xFFFAFAFA)
val SurfaceDark = Color(0xFF000000)
val OnSurfaceLight = Color(0xFF1B1918)
val OnSurfaceDark = Color(0xFFE7E5E4)

// surface-variant — input fields, inactive segmented controls, subdued fills.
val SurfaceVariantLight = Color(0xFFE8E4E3)
val SurfaceVariantDark = Color(0xFF544745)
val OnSurfaceVariantLight = Color(0xFF544745)
val OnSurfaceVariantDark = Color(0xFFD1C8C7)

// surface-dim / surface-bright — ends of the light-theme surface ladder.
val SurfaceDimLight = Color(0xFFE0DDDC)
val SurfaceDimDark = Color(0xFF000000)
val SurfaceBrightLight = Color(0xFFFAFAFA)
val SurfaceBrightDark = Color(0xFF403B3A)

// surface-container-* — the tonal elevation ladder (carries elevation in dark/true-black theme).
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowestDark = Color(0xFF000000)
val SurfaceContainerLowLight = Color(0xFFF5F4F4)
val SurfaceContainerLowDark = Color(0xFF0B0A0A)
val SurfaceContainerLight = Color(0xFFF0EFEF)
val SurfaceContainerDark = Color(0xFF151413)
val SurfaceContainerHighLight = Color(0xFFECEAEA)
val SurfaceContainerHighDark = Color(0xFF252222)
val SurfaceContainerHighestLight = Color(0xFFE7E5E4)
val SurfaceContainerHighestDark = Color(0xFF302C2C)

// outline — borders/dividers.
val OutlineLight = Color(0xFF8C7673)
val OutlineDark = Color(0xFFA3918F)
val OutlineVariantLight = Color(0xFFD1C8C7)
val OutlineVariantDark = Color(0xFF544745)

// inverse — snackbars/toasts/tooltips (flip relative to the current theme).
val InverseSurfaceLight = Color(0xFF363130)
val InverseSurfaceDark = Color(0xFFE7E5E4)
val InverseOnSurfaceLight = Color(0xFFF5F4F4)
val InverseOnSurfaceDark = Color(0xFF363130)
val InversePrimaryLight = Color(0xFFFFA599)
val InversePrimaryDark = Color(0xFF991200)

// shadow / scrim — identical across themes.
val ShadowLight = Color(0xFF000000)
val ShadowDark = Color(0xFF000000)
val ScrimLight = Color(0xFF000000)
val ScrimDark = Color(0xFF000000)

// surface-tint — the primary-tinted overlay M3 mixes into elevated surfaces (barely visible on
// dark/true-black theme by design; the surface-container-* ladder does that work there instead).
val SurfaceTintLight = Color(0xFFE01A00)
val SurfaceTintDark = Color(0xFFFFA599)
