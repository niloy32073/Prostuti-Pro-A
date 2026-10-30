package com.prostuti.student.ui.theme

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Prostuti radius scale. Follows the M3 shape scale exactly; radius-xl (28dp) matches the
// app-icon's own corner rounding and radius-full matches the diamond mark's point-rounding /
// the favicon's circular badge — reach for those on components that should feel "branded"
// (FAB, hero cards, the exam-countdown card).
val RadiusNone = 0.dp
val RadiusXs = 4.dp
val RadiusSm = 8.dp
val RadiusMd = 12.dp
val RadiusLg = 16.dp
val RadiusXl = 28.dp
val RadiusFull = CornerSize(percent = 50) // pills, avatars, filter chips

val ProstutiShapes = Shapes(
    extraSmall = RoundedCornerShape(RadiusXs),
    small = RoundedCornerShape(RadiusSm),
    medium = RoundedCornerShape(RadiusMd),
    large = RoundedCornerShape(RadiusLg),
    extraLarge = RoundedCornerShape(RadiusXl)
)
