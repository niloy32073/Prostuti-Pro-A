package com.prostuti.student.ui.screen.intro

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostuti.student.R
import com.prostuti.student.ui.component.PagerDots
import com.prostuti.student.ui.component.PrimaryButton
import com.prostuti.student.ui.component.carouselVisibleCount
import com.prostuti.student.ui.theme.ProstutiTheme
import com.prostuti.student.ui.theme.ProstutiTypography
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

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

private val CardShape = RoundedCornerShape(32.dp)

/** The card is half the screen height (capped by the space actually left for it). */
private const val CARD_HEIGHT_FRACTION = 0.5f

/** The looping carousel fakes an endless ring with this many copies of the card list. */
private const val LOOP_REPEATS = 1_000

/** Slots trimmed off the carousel viewport so the outermost cards peek in, hinting the ring goes on. */
private const val CAROUSEL_PEEK_TRIM = 0.4f

private fun lerp(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isFinished) {
        if (state.isFinished) onFinished()
    }

    OnboardingContent(cards = viewModel.currentCards, onFinish = viewModel::finish)
}

/**
 * Compact width: a swipeable deck — the card in front, its neighbours peeking out scaled down
 * and tilted. Wider windows: a looping carousel with the current card popped up in the middle
 * and the previous / next ones beside it; the card before the first is the last and the one
 * after the last is the first.
 */
@Composable
fun OnboardingContent(
    cards: List<OnboardingCard>,
    onFinish: () -> Unit,
    visibleCount: Int = carouselVisibleCount()
) {
    if (cards.isEmpty()) return
    val isCarousel = visibleCount > 1

    // Real card index, kept when the layout switches (rotation, resize, fold/unfold).
    var savedIndex by rememberSaveable { mutableIntStateOf(0) }
    val loopStart = cards.size * (LOOP_REPEATS / 2)
    val pagerState = if (isCarousel) {
        rememberPagerState(initialPage = loopStart + savedIndex) { cards.size * LOOP_REPEATS }
    } else {
        rememberPagerState(initialPage = savedIndex) { cards.size }
    }
    val currentIndex = pagerState.currentPage.mod(cards.size)
    LaunchedEffect(currentIndex) { savedIndex = currentIndex }

    val scope = rememberCoroutineScope()
    val isLast = currentIndex == cards.lastIndex

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        // Sizing only, not a layout breakpoint.
        val cardHeight = maxHeight * CARD_HEIGHT_FRACTION

        Column(modifier = Modifier.fillMaxSize()) {
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
                        .clickable(onClick = onFinish)
                        .padding(8.dp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (isCarousel) {
                    OnboardingCarousel(
                        cards = cards,
                        pagerState = pagerState,
                        visibleCount = visibleCount,
                        cardHeight = cardHeight,
                        onCardClick = { page -> scope.launch { pagerState.animateScrollToPage(page) } }
                    )
                } else {
                    OnboardingDeck(cards = cards, pagerState = pagerState, cardHeight = cardHeight)
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 10.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                PagerDots(
                    count = cards.size,
                    activeIndex = currentIndex,
                    activeColor = MaterialTheme.colorScheme.primary
                )
                PrimaryButton(
                    text = stringResource(if (isLast) R.string.onboarding_start else R.string.onboarding_next),
                    onClick = {
                        if (isLast) {
                            onFinish()
                        } else {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun OnboardingDeck(cards: List<OnboardingCard>, pagerState: PagerState, cardHeight: Dp) {
    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(horizontal = 48.dp),
        pageSpacing = 16.dp,
        modifier = Modifier.fillMaxWidth()
    ) { page ->
        OnboardingCardContent(
            card = cards[page],
            icon = cardIcons[page % cardIcons.size],
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = cardHeight)
                .fillMaxHeight()
                .graphicsLayer {
                    // Distance of this page from the current one, in whole pages — 0 when centered,
                    // growing as it's swiped away in either direction.
                    val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                    val depth = pageOffset.absoluteValue.coerceIn(0f, 1f)

                    scaleX = lerp(1f, 0.84f, depth)
                    scaleY = scaleX
                    translationY = lerp(0f, -26.dp.toPx(), depth)
                    rotationZ = lerp(0f, if (pageOffset < 0f) 6f else -6f, depth)
                    alpha = lerp(1f, 0.45f, depth)
                }
                .shadow(elevation = 10.dp, shape = CardShape, clip = false)
        )
    }
}

/** Each page is one slot of the viewport, trimmed so the outer cards only peek in. */
private class CarouselPageSize(private val visibleCount: Int) : PageSize {
    override fun Density.calculateMainAxisPageSize(availableSpace: Int, pageSpacing: Int): Int =
        (availableSpace / (visibleCount - CAROUSEL_PEEK_TRIM)).roundToInt()
}

@Composable
private fun OnboardingCarousel(
    cards: List<OnboardingCard>,
    pagerState: PagerState,
    visibleCount: Int,
    cardHeight: Dp,
    onCardClick: (page: Int) -> Unit
) {
    val pageSize = remember(visibleCount) { CarouselPageSize(visibleCount) }

    HorizontalPager(
        state = pagerState,
        pageSize = pageSize,
        snapPosition = SnapPosition.Center,
        // Room for the centre card's shadow so the pager doesn't clip it.
        contentPadding = PaddingValues(vertical = 24.dp),
        modifier = Modifier.fillMaxWidth()
    ) { page ->
        val index = page.mod(cards.size)
        OnboardingCardContent(
            card = cards[index],
            icon = cardIcons[index % cardIcons.size],
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = cardHeight)
                .fillMaxHeight()
                .graphicsLayer {
                    // Signed distance from the centre slot: negative on the left, positive on the right.
                    val offset = (page - pagerState.currentPage) - pagerState.currentPageOffsetFraction
                    val depth = offset.absoluteValue
                    val near = depth.coerceIn(0f, 1f)
                    val far = (depth - 1f).coerceIn(0f, 1f)

                    // Centre card pops out; side cards shrink, fade and turn away like a ring.
                    scaleX = lerp(1f, 0.82f, near) - 0.14f * far
                    scaleY = scaleX
                    alpha = lerp(1f, 0.7f, near) - 0.25f * far
                    rotationY = -offset.coerceIn(-1f, 1f) * 16f
                    cameraDistance = 12f * density
                    // Pull side cards slightly inward so the ring looks tight.
                    translationX = -offset * size.width * 0.08f
                    // Only the centre card casts a shadow; a shadow on a turned card draws as a flat wedge.
                    shadowElevation = lerp(10.dp.toPx(), 0f, near)
                    shape = CardShape
                    clip = false
                }
                .clickable(enabled = page != pagerState.currentPage) { onCardClick(page) }
        )
    }
}

@Composable
private fun OnboardingCardContent(card: OnboardingCard, icon: ImageVector, modifier: Modifier = Modifier) {
    val onColor = card.containerRole.onContainerColor()

    Box(
        modifier = modifier
            .background(card.containerRole.containerColor(), CardShape)
            .padding(horizontal = 28.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        // Scrolls only when the window is too short for the card (landscape phone, split screen).
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
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

private val previewCards = listOf(
    OnboardingCard(R.string.onboarding_card1_title, R.string.onboarding_card1_desc, ContainerRole.PRIMARY),
    OnboardingCard(R.string.onboarding_card2_title, R.string.onboarding_card2_desc, ContainerRole.SECONDARY),
    OnboardingCard(R.string.onboarding_card3_title, R.string.onboarding_card3_desc, ContainerRole.TERTIARY),
    OnboardingCard(R.string.onboarding_card4_title, R.string.onboarding_card4_desc, ContainerRole.PRIMARY),
    OnboardingCard(R.string.onboarding_card5_title, R.string.onboarding_card5_desc, ContainerRole.SECONDARY)
)

@Preview(name = "Phone", device = Devices.PHONE, showBackground = true)
@Preview(name = "Foldable", device = Devices.FOLDABLE, showBackground = true)
@Preview(name = "Tablet", device = Devices.TABLET, showBackground = true)
@Preview(name = "Desktop", device = Devices.DESKTOP, showBackground = true)
@Composable
private fun OnboardingContentPreview() {
    ProstutiTheme {
        OnboardingContent(cards = previewCards, onFinish = {})
    }
}
