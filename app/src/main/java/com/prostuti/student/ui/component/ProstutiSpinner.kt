package com.prostuti.student.ui.component

import android.graphics.drawable.AnimatedVectorDrawable
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.ImageView
import androidx.annotation.StyleRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.prostuti.student.R

/**
 * The app's loading indicator: the animated Prostuti mark (`avd_prostuti_spinner`, 2400 ms loop).
 * Use it everywhere a `CircularProgressIndicator` would go. Default size is 40dp; pass
 * `Modifier.size(...)` to change it.
 *
 * With [color] unspecified it draws in brand colours (red mark, road in black / white for the
 * current theme). Pass a [color] — e.g. `onPrimary` inside a filled button — to draw the whole
 * mark in that one colour.
 *
 * Drawn with the platform `AnimatedVectorDrawable`, which supports the file's keyframes and
 * infinite repeat natively.
 */
@Composable
fun ProstutiSpinner(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    @StyleRes val markTheme = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
        R.style.ThemeOverlay_Prostuti_Mark_Dark
    } else {
        R.style.ThemeOverlay_Prostuti_Mark_Light
    }
    val tint = if (color.isSpecified) color.toArgb() else null

    AndroidView(
        factory = { context -> SpinnerView(context) },
        update = { view -> view.bind(markTheme, tint) },
        modifier = modifier
            .size(40.dp)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate },
    )
}

/** ImageView that owns one looping spinner drawable and runs it only while attached. */
private class SpinnerView(context: android.content.Context) : ImageView(context) {
    private var markTheme = 0
    private var spinner: AnimatedVectorDrawable? = null

    init {
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO // semantics come from Compose
    }

    fun bind(@StyleRes theme: Int, tint: Int?) {
        if (theme != markTheme || spinner == null) {
            markTheme = theme
            spinner?.stop()
            spinner = ContextThemeWrapper(context, theme)
                .getDrawable(R.drawable.avd_prostuti_spinner) as AnimatedVectorDrawable
            setImageDrawable(spinner)
            if (isAttachedToWindow) spinner?.start()
        }
        if (tint != null) spinner?.setTint(tint) else spinner?.setTintList(null)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        spinner?.start()
    }

    override fun onDetachedFromWindow() {
        spinner?.stop()
        super.onDetachedFromWindow()
    }
}
