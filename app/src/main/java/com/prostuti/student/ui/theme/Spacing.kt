package com.prostuti.student.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Prostuti's four-pixel spacing grid. Pick the smallest step that still gives a control its
 * minimum 48x48dp touch target; [space16] is the default screen margin and card padding.
 */
object ProstutiSpacing {
    val space4 = 4.dp   // icon-to-label gap, tight internal padding
    val space8 = 8.dp   // compact control padding, gap between related elements
    val space12 = 12.dp // default gap between list rows, chip padding
    val space16 = 16.dp // card padding, screen horizontal margin on compact widths
    val space20 = 20.dp // section internal spacing
    val space24 = 24.dp // gap between unrelated sections
    val space32 = 32.dp // screen top padding below app bar, large section breaks
    val space40 = 40.dp // empty-state illustration spacing
    val space48 = 48.dp // hero section padding
    val space64 = 64.dp // splash/onboarding vertical rhythm
}

/**
 * Prostuti's six M3 elevation tiers. In dark/true-black theme these read as very subtle by
 * design — reach for `MaterialTheme.colorScheme.surfaceContainer*` first to separate a card from
 * the page, and layer elevation on top only where the platform needs a real shadow (FAB, a
 * dragged element).
 */
object ProstutiElevation {
    val level0 = 0.dp  // resting surfaces, outlined components
    val level1 = 1.dp  // cards at rest, search bar
    val level2 = 3.dp  // raised buttons, chips on drag
    val level3 = 6.dp  // FAB at rest, dropdown menus
    val level4 = 8.dp  // navigation drawer, bottom sheet
    val level5 = 12.dp // dialogs, FAB pressed
}
