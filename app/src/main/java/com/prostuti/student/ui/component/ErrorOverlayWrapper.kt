package com.prostuti.student.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

const val ERROR_OVERLAY_TAG = "error_overlay"

/**
 * The app's one way to show errors: wraps a screen's content and stacks every entry of the
 * ViewModel's `errors: Map<tag, message>` as an [ErrorCard] in the top-end corner, in the order
 * the errors happened (the map keeps insertion order). Each card stays until the user closes it,
 * which calls [errorCleaner] with its tag. Optionally adds pull-to-refresh around the content.
 *
 * [overlayPadding] only moves the error stack and the refresh indicator (pass the Scaffold's
 * inner padding so they clear the system bars). The content always gets the full area and
 * applies its own padding, so existing screens keep their layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullToRefreshErrorOverlayWrapper(
    errors: Map<String, String>,
    errorCleaner: (tag: String) -> Unit,
    modifier: Modifier = Modifier,
    overlayPadding: PaddingValues = PaddingValues(0.dp),
    errorTopPadding: Dp = 72.dp,
    pullToRefreshEnabled: Boolean = false,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    if (pullToRefreshEnabled) {
        val pullToRefreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            modifier = modifier.fillMaxSize(),
            state = pullToRefreshState,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter).padding(overlayPadding)
                )
            }
        ) {
            content()
            ErrorStack(errors, errorCleaner, overlayPadding, errorTopPadding)
        }
    } else {
        Box(modifier = modifier.fillMaxSize()) {
            content()
            ErrorStack(errors, errorCleaner, overlayPadding, errorTopPadding)
        }
    }
}

@Composable
private fun BoxScope.ErrorStack(
    errors: Map<String, String>,
    errorCleaner: (tag: String) -> Unit,
    overlayPadding: PaddingValues,
    errorTopPadding: Dp
) {
    // Keep the last non-empty list so the final card can slide out instead of vanishing.
    // A plain holder (not State): it only changes when `errors` does, which already recomposes.
    val lastShown = remember { arrayOf(errors) }
    if (errors.isNotEmpty()) lastShown[0] = errors
    val shown = lastShown[0]

    // Nothing is composed while there are no errors, so the empty stack never blocks touches
    // on the screen underneath.
    AnimatedVisibility(
        visible = errors.isNotEmpty(),
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(overlayPadding)
            .testTag(ERROR_OVERLAY_TAG)
    ) {
        LazyColumn(
            modifier = Modifier
                .wrapContentHeight(align = Alignment.Top)
                .widthIn(min = 250.dp, max = 350.dp),
            contentPadding = PaddingValues(top = errorTopPadding, end = 16.dp, bottom = 16.dp, start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items = shown.entries.toList(), key = { it.key }) { error ->
                ErrorCard(
                    error = error.value,
                    tag = error.key,
                    onClean = { errorCleaner(error.key) },
                    // Cards after the first slide in/out and the rest move up smoothly.
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}
