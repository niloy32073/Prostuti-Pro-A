package com.prostuti.student.ui.screen.playground.graph

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Line colours: distinct hues that stay readable on both the light and the dark graph paper. */
object GraphColors {
    private val light = listOf(
        Color(0xFFC74440), Color(0xFF2D70B3), Color(0xFF388C46),
        Color(0xFF6042A6), Color(0xFFE07A10), Color(0xFF1F1F1F),
    )
    private val dark = listOf(
        Color(0xFFFF7A73), Color(0xFF6FA8FF), Color(0xFF6CD37A),
        Color(0xFFB59CFF), Color(0xFFFFB04D), Color(0xFFEDEDED),
    )

    val count: Int get() = light.size

    @Composable
    fun palette(): List<Color> = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) dark else light

    @Composable
    fun color(index: Int): Color = palette()[Math.floorMod(index, count)]
}
