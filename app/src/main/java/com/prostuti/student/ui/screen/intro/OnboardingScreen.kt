package com.prostuti.student.ui.screen.intro

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prostuti.student.R
import com.prostuti.student.ui.component.PagerDots
import com.prostuti.student.ui.component.PrimaryButton
import com.prostuti.student.ui.theme.ProstutiTypography
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.absoluteValue

@Composable
private fun ContainerRole.containerColor(): Color = when (this) {
    ContainerRole.PRIMARY -> MaterialTheme.colorScheme.primaryContainer
    ContainerRole.SECONDARY -> MaterialTheme.colorScheme.secondaryContainer
    ContainerRole.TERTIARY -> MaterialTheme.colorScheme.tertiaryContainer
}

@Composable
private fun ContainerRole.onContainerColor(): Color = when (this) {
    ContainerRole.PRIMARY -> MaterialTheme.colorScheme.onPrimaryContainer
    ContainerRole.SECONDARY -> MaterialTheme.colorScheme.onSecondaryContainer
    ContainerRole.TERTIARY -> MaterialTheme.colorScheme.onTertiaryContainer
}

private val cardIcons: List<ImageVector> = listOf(
    Icons.Filled.Edit,
    Icons.Filled.EmojiEvents,
    Icons.Filled.School,
    Icons.Filled.Lightbulb,
    Icons.Filled.Speed
)

private fun lerp(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction

/**
 * A deck of cards rather than a flat slide show: each card peeks out from behind the one in
 * front (scaled down, faded, tilted), and a swipe pops the next one to the front — closer to a
 * physical card stack than the old edge-to-edge color-swap pager.
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val cards = viewModel.currentCards
    val pagerState = rememberPagerState(pageCount = { cards.size })
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.isFinished) {
        if (state.isFinished) onFinished()
    }

    val isLast = pagerState.currentPage == cards.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = stringResource(R.string.onboarding_skip),
                style = ProstutiTypography.bnLabelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable(onClick = viewModel::finish)
                    .padding(8.dp)
            )
        }

        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 48.dp),
            pageSpacing = 16.dp,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            OnboardingCard(
                card = cards[page],
                icon = cardIcons[page],
                page = page,
                pagerState = pagerState
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 10.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            PagerDots(
                count = cards.size,
                activeIndex = pagerState.currentPage,
                activeColor = MaterialTheme.colorScheme.primary
            )
            PrimaryButton(
                text = stringResource(if (isLast) R.string.onboarding_start else R.string.onboarding_next),
                onClick = {
                    if (isLast) {
                        viewModel.finish()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                }
            )
        }
    }
}

@Composable
private fun OnboardingCard(card: OnboardingCard, icon: ImageVector, page: Int, pagerState: PagerState) {
    val onColor = card.containerRole.onContainerColor()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .graphicsLayer {
                // Distance of this page from the current one, in whole pages — 0 when centered,
                // growing as it's swiped away in either direction.
                val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                val depth = pageOffset.absoluteValue.coerceIn(0f, 1f)

                scaleX = lerp(1f, 0.84f, depth)
                scaleY = scaleX
                translationY = lerp(0f, -26.dp.toPx(), depth)
                rotationZ = lerp(0f, if (pageOffset < 0f) 6f else -6f, depth)
                alpha = lerp(1f, 0.45f, depth)
            }
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(32.dp), clip = false)
            .background(card.containerRole.containerColor(), RoundedCornerShape(32.dp))
            .padding(horizontal = 28.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = onColor,
                    modifier = Modifier.size(46.dp)
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(card.titleRes),
                    style = ProstutiTypography.bnTitleLarge,
                    color = onColor,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(card.descRes),
                    style = ProstutiTypography.bnBodyMedium.copy(lineHeight = 21.sp),
                    color = onColor.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
