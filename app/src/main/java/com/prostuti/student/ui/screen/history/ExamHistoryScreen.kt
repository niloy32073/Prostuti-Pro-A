package com.prostuti.student.ui.screen.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import com.prostuti.student.ui.component.ProstutiSpinner
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import com.prostuti.student.R
import com.prostuti.student.data.model.enums.AttemptMode
import com.prostuti.student.data.model.enums.HistoryStatus
import com.prostuti.student.data.model.response.ExamHistoryItem
import com.prostuti.student.data.model.response.Subject
import com.prostuti.student.domain.HistoryTimeRange
import com.prostuti.student.domain.parseServerTimeMillis
import com.prostuti.student.ui.component.BackButton
import com.prostuti.student.ui.component.FormFactorPreviews
import com.prostuti.student.ui.component.PullToRefreshErrorOverlayWrapper
import com.prostuti.student.ui.component.ResultBar
import com.prostuti.student.ui.component.adaptiveColumnCount
import com.prostuti.student.ui.theme.ProstutiTheme
import com.prostuti.student.ui.theme.ProstutiTypography
import com.prostuti.student.ui.theme.extendedColors
import com.prostuti.student.util.LANGUAGE_BANGLA
import com.prostuti.student.util.toBengaliDigits
import org.koin.compose.viewmodel.koinViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

const val HISTORY_LIST_TAG = "history_list"
const val HISTORY_ITEM_TAG = "history_item_"
const val HISTORY_MISSED_TAG = "history_missed_"
const val HISTORY_SUBJECT_CHIP_TAG = "history_subject_chip"
const val HISTORY_TIME_CHIP_TAG = "history_time_chip"
const val HISTORY_STATUS_CHIP_TAG = "history_status_"
const val HISTORY_RESET_TAG = "history_reset"

private val DHAKA: ZoneId = ZoneId.of("Asia/Dhaka")
private enum class HistorySheet { SUBJECT, TIME }

@Composable
fun ExamHistoryScreen(
    onBack: () -> Unit,
    onOpenAttempt: (ExamHistoryItem) -> Unit,
    viewModel: ExamHistoryViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ExamHistoryContent(
        state = state,
        onBack = onBack,
        onOpenAttempt = onOpenAttempt,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onStatus = viewModel::setStatus,
        onSubject = viewModel::setSubject,
        onTimeRange = viewModel::setTimeRange,
        onCustomRange = viewModel::setCustomRange,
        onReset = viewModel::resetFilters,
        onClearError = viewModel::clearError
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamHistoryContent(
    state: ExamHistoryUiState,
    onBack: () -> Unit,
    onOpenAttempt: (ExamHistoryItem) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onStatus: (String?) -> Unit,
    onSubject: (String?) -> Unit,
    onTimeRange: (HistoryTimeRange) -> Unit,
    onCustomRange: (LocalDate, LocalDate) -> Unit,
    onReset: () -> Unit,
    onClearError: (String) -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]
    var openSheet by rememberSaveable { mutableStateOf<HistorySheet?>(null) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            HistoryHeader(
                totalText = (state.totalItems ?: state.items.size).toString().localizedDigits(locale),
                showCount = !state.isLoading,
                onBack = onBack
            )
            HistoryFilterBar(
                state = state,
                locale = locale,
                onStatus = onStatus,
                onOpenSubject = { openSheet = HistorySheet.SUBJECT },
                onOpenTime = { openSheet = HistorySheet.TIME },
                onReset = onReset
            )
            PullToRefreshErrorOverlayWrapper(
                errors = state.errors,
                errorCleaner = onClearError,
                pullToRefreshEnabled = true,
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh
            ) {
                when {
                    state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ProstutiSpinner()
                    }
                    state.items.isEmpty() -> HistoryEmpty(hasFilter = state.hasFilter, onReset = onReset)
                    else -> HistoryList(
                        items = state.items,
                        isLoadingMore = state.isLoadingMore,
                        locale = locale,
                        onOpenAttempt = onOpenAttempt,
                        onLoadMore = onLoadMore
                    )
                }
            }
        }
    }

    when (openSheet) {
        HistorySheet.SUBJECT -> OptionSheet(
            title = stringResource(R.string.history_subject_sheet_title),
            options = listOf<Pair<String?, String>>(null to stringResource(R.string.history_subject_all)) +
                state.subjects.map { it.id to it.displayName(locale) },
            selected = state.subjectId,
            onPick = { onSubject(it); openSheet = null },
            onDismiss = { openSheet = null }
        )
        HistorySheet.TIME -> OptionSheet(
            title = stringResource(R.string.history_time_sheet_title),
            options = HistoryTimeRange.entries.map { it to stringResource(it.labelRes()) },
            selected = state.timeRange,
            onPick = { range ->
                openSheet = null
                if (range == HistoryTimeRange.CUSTOM) showDatePicker = true else onTimeRange(range)
            },
            onDismiss = { openSheet = null }
        )
        null -> Unit
    }

    if (showDatePicker) {
        HistoryDateRangeDialog(
            onConfirm = { start, end -> showDatePicker = false; onCustomRange(start, end) },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
private fun HistoryHeader(totalText: String, showCount: Boolean, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BackButton(onClick = onBack)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = stringResource(R.string.history_title),
                style = ProstutiTypography.bnTitleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (showCount) {
                Text(
                    text = stringResource(R.string.history_total_count, totalText),
                    style = ProstutiTypography.bnLabelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HistoryFilterBar(
    state: ExamHistoryUiState,
    locale: Locale,
    onStatus: (String?) -> Unit,
    onOpenSubject: () -> Unit,
    onOpenTime: () -> Unit,
    onReset: () -> Unit
) {
    val statuses = listOf(
        null to R.string.history_status_all,
        HistoryStatus.COMPLETED to R.string.history_status_completed,
        HistoryStatus.MISSED to R.string.history_status_missed
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(bottom = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            statuses.forEach { (value, label) ->
                FilterChip(
                    selected = state.status == value,
                    onClick = { onStatus(value) },
                    label = { Text(stringResource(label), style = ProstutiTypography.bnLabelLarge) },
                    modifier = Modifier.testTag(HISTORY_STATUS_CHIP_TAG + (value ?: "ALL"))
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val subjectLabel = state.subjects.firstOrNull { it.id == state.subjectId }?.displayName(locale)
            DropdownChip(
                label = subjectLabel ?: stringResource(R.string.history_filter_subject),
                active = state.subjectId != null,
                onClick = onOpenSubject,
                modifier = Modifier.testTag(HISTORY_SUBJECT_CHIP_TAG)
            )
            val timeLabel = when (state.timeRange) {
                HistoryTimeRange.ALL -> stringResource(R.string.history_filter_time)
                HistoryTimeRange.CUSTOM -> customRangeLabel(state.customStart, state.customEnd, locale)
                else -> stringResource(state.timeRange.labelRes())
            }
            DropdownChip(
                label = timeLabel,
                active = state.timeRange != HistoryTimeRange.ALL,
                onClick = onOpenTime,
                modifier = Modifier.testTag(HISTORY_TIME_CHIP_TAG)
            )
            if (state.hasFilter) {
                TextButton(onClick = onReset, modifier = Modifier.testTag(HISTORY_RESET_TAG)) {
                    Text(stringResource(R.string.history_filter_reset), style = ProstutiTypography.bnLabelLarge)
                }
            }
        }
    }
}

@Composable
private fun DropdownChip(label: String, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilterChip(
        selected = active,
        onClick = onClick,
        label = { Text(label, style = ProstutiTypography.bnLabelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp)) },
        shape = CircleShape,
        modifier = modifier
    )
}

@Composable
private fun HistoryList(
    items: List<ExamHistoryItem>,
    isLoadingMore: Boolean,
    locale: Locale,
    onOpenAttempt: (ExamHistoryItem) -> Unit,
    onLoadMore: () -> Unit
) {
    val gridState = rememberLazyGridState()
    val groups = remember(items, locale) { items.groupByMonth(locale) }
    val nearEnd by remember {
        derivedStateOf {
            val info = gridState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 4
        }
    }
    LaunchedEffect(nearEnd, items.size) { if (nearEnd) onLoadMore() }

    LazyVerticalGrid(
        columns = GridCells.Fixed(adaptiveColumnCount()),
        state = gridState,
        modifier = Modifier.fillMaxSize().testTag(HISTORY_LIST_TAG),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        groups.forEach { (month, monthItems) ->
            item(key = "month_$month", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = month,
                    style = ProstutiTypography.bnLabelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp, start = 2.dp)
                )
            }
            items(monthItems, key = { it.attemptId }) { item ->
                if (item.status == HistoryStatus.MISSED) {
                    MissedCard(item = item, locale = locale)
                } else {
                    CompletedCard(item = item, locale = locale, onClick = dropUnlessResumed { onOpenAttempt(item) })
                }
            }
        }
        if (isLoadingMore) {
            item(key = "loading_more", span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                    ProstutiSpinner(modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun CompletedCard(item: ExamHistoryItem, locale: Locale, onClick: () -> Unit) {
    val total = item.correctCount + item.wrongCount + item.skippedCount
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag(HISTORY_ITEM_TAG + item.attemptId),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.extendedColors.successContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.extendedColors.onSuccessContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.displayTitle(locale),
                    style = ProstutiTypography.bnLabelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOfNotNull(item.dayLabel(locale), modeLabel(item.mode)).joinToString(" · "),
                    style = ProstutiTypography.enLabelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatScore(item.totalScore).localizedDigits(locale),
                    style = ProstutiTypography.enTitleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.history_score_unit),
                    style = ProstutiTypography.bnLabelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        ResultBar(correct = item.correctCount, wrong = item.wrongCount, total = total, barHeight = 6.dp)
        Text(
            text = stringResource(
                R.string.history_counts,
                item.correctCount.toString().localizedDigits(locale),
                item.wrongCount.toString().localizedDigits(locale),
                item.skippedCount.toString().localizedDigits(locale)
            ),
            style = ProstutiTypography.bnLabelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MissedCard(item: ExamHistoryItem, locale: Locale) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag(HISTORY_MISSED_TAG + item.attemptId),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.displayTitle(locale),
                style = ProstutiTypography.bnLabelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = listOfNotNull(item.dayLabel(locale), stringResource(R.string.history_missed_meta)).joinToString(" · "),
                style = ProstutiTypography.bnLabelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.history_missed_badge),
            style = ProstutiTypography.bnLabelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun HistoryEmpty(hasFilter: Boolean, onReset: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(if (hasFilter) R.string.history_empty_filtered else R.string.history_empty),
            style = ProstutiTypography.bnBodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (hasFilter) {
            Spacer(Modifier.height(12.dp))
            Button(onClick = onReset) {
                Text(stringResource(R.string.history_reset_filters), style = ProstutiTypography.bnLabelLarge)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> OptionSheet(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onPick: (T) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                text = title,
                style = ProstutiTypography.bnTitleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            options.forEach { (value, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .selectable(selected = value == selected, role = Role.RadioButton, onClick = { onPick(value) })
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = value == selected, onClick = null)
                    Spacer(Modifier.width(12.dp))
                    Text(text = label, style = ProstutiTypography.bnBodyLarge, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryDateRangeDialog(onConfirm: (LocalDate, LocalDate) -> Unit, onDismiss: () -> Unit) {
    val pickerState = rememberDateRangePickerState()
    val start = pickerState.selectedStartDateMillis
    val end = pickerState.selectedEndDateMillis ?: start
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = start != null,
                onClick = {
                    if (start != null && end != null) onConfirm(start.toUtcDate(), end.toUtcDate())
                }
            ) { Text(stringResource(R.string.history_date_picker_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.history_date_picker_cancel)) } }
    ) {
        DateRangePicker(state = pickerState, modifier = Modifier.weight(1f))
    }
}

// ── formatting helpers ──

private fun Long.toUtcDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

private fun String.localizedDigits(locale: Locale): String =
    if (locale.language == LANGUAGE_BANGLA) toBengaliDigits() else this

private fun HistoryTimeRange.labelRes(): Int = when (this) {
    HistoryTimeRange.ALL -> R.string.history_time_all
    HistoryTimeRange.TODAY -> R.string.history_time_today
    HistoryTimeRange.LAST_7_DAYS -> R.string.history_time_7_days
    HistoryTimeRange.LAST_30_DAYS -> R.string.history_time_30_days
    HistoryTimeRange.CUSTOM -> R.string.history_time_custom
}

@Composable
private fun modeLabel(mode: String?): String? = when (mode) {
    AttemptMode.LIVE.name -> stringResource(R.string.history_mode_live)
    AttemptMode.PRACTICE.name -> stringResource(R.string.history_mode_practice)
    else -> null
}

private fun Subject.displayName(locale: Locale): String = if (locale.language == LANGUAGE_BANGLA) nameBn else nameEn

private fun ExamHistoryItem.displayTitle(locale: Locale): String =
    if (locale.language == LANGUAGE_BANGLA) examTitleBn.ifBlank { examTitleEn } else examTitleEn.ifBlank { examTitleBn }

// The server sends attempted_at without an offset ("2026-11-08T05:18:05.529238"); that is UTC.
private fun ExamHistoryItem.localDate(): LocalDate? =
    parseServerTimeMillis(attemptedAt)?.let { Instant.ofEpochMilli(it).atZone(DHAKA).toLocalDate() }

private fun ExamHistoryItem.dayLabel(locale: Locale): String? =
    localDate()?.let { DateTimeFormatter.ofPattern("d MMM", locale).format(it).localizedDigits(locale) }

private fun customRangeLabel(start: LocalDate?, end: LocalDate?, locale: Locale): String {
    val f = DateTimeFormatter.ofPattern("d MMM", locale)
    return listOfNotNull(start, end).distinct().joinToString(" – ") { f.format(it) }.localizedDigits(locale)
}

/** Items in server order, split into "November 2026" groups (Dhaka calendar month). */
private fun List<ExamHistoryItem>.groupByMonth(locale: Locale): List<Pair<String, List<ExamHistoryItem>>> {
    val f = DateTimeFormatter.ofPattern("MMMM yyyy", locale)
    val groups = linkedMapOf<String, MutableList<ExamHistoryItem>>()
    forEach { item ->
        val key = item.localDate()?.let { f.format(it).localizedDigits(locale) } ?: ""
        groups.getOrPut(key) { mutableListOf() }.add(item)
    }
    return groups.map { it.key to it.value.toList() }
}

/** 16.75 → "16.75", 13.0 → "13", 10.5 → "10.5". */
internal fun formatScore(score: Double): String =
    if (score % 1.0 == 0.0) score.toLong().toString()
    else "%.2f".format(Locale.US, score).trimEnd('0').trimEnd('.')

// ── previews ──

private val previewItems = listOf(
    ExamHistoryItem("e1", "a1", "Weekly test — Physics 1st", "সাপ্তাহিক টেস্ট — পদার্থবিজ্ঞান ১ম পত্র", "2026-11-05T04:00:00Z", "LIVE", HistoryStatus.COMPLETED, 18, 5, 2, 16.75),
    ExamHistoryItem("e2", "a2", "Routine exam — Chemistry 1st", "রুটিন এক্সাম — রসায়ন ১ম পত্র", "2026-11-03T00:00:00Z", null, HistoryStatus.MISSED),
    ExamHistoryItem("e3", "a3", "Practice — Higher Math 1st", "প্র্যাকটিস — উচ্চতর গণিত ১ম পত্র", "2026-10-27T10:00:00Z", "PRACTICE", HistoryStatus.COMPLETED, 12, 6, 2, 10.5)
)

@FormFactorPreviews
@Preview(name = "Phone dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExamHistoryContentPreview() {
    ProstutiTheme {
        ExamHistoryContent(
            state = ExamHistoryUiState(isLoading = false, items = previewItems, totalItems = 3, status = null),
            onBack = {}, onOpenAttempt = {}, onRefresh = {}, onLoadMore = {}, onStatus = {}, onSubject = {},
            onTimeRange = {}, onCustomRange = { _, _ -> }, onReset = {}, onClearError = {}
        )
    }
}
