package com.prostuti.student.ui.screen.routine.builder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import com.prostuti.student.ui.component.ProstutiSpinner
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.ui.component.adaptiveColumnCount
import com.prostuti.student.ui.component.localizedNumber
import com.prostuti.student.ui.screen.practice.ChapterCard
import com.prostuti.student.ui.screen.practice.SubjectSelectHeader
import com.prostuti.student.ui.theme.ProstutiTypography
import com.prostuti.student.ui.component.localized

/** Step 1: 1–20 subjects, ticked in a size-class grid. */
@Composable
internal fun SubjectStep(state: RoutineBuilderUiState, actions: RoutineBuilderActions, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        if (state.isLoadingSubjects && state.subjects.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                ProstutiSpinner()
            }
        } else {
            val columns = adaptiveColumnCount()
            val full = state.selectedSubjectIds.size >= RoutineScope.MAX_SUBJECTS
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                state = rememberLazyGridState(),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = stringResource(if (state.isAdhoc) R.string.routine_subjects_intro_adhoc else R.string.routine_subjects_intro_routine),
                        style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                if (state.subjects.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(stringResource(R.string.routine_subjects_empty), style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                items(state.subjects, key = { it.id }) { subject ->
                    val checked = subject.id in state.selectedSubjectIds
                    SubjectCheckRow(
                        name = localized(subject.nameBn, subject.nameEn), checked = checked, enabled = checked || !full,
                        onToggle = { actions.onToggleSubject(subject.id) },
                        modifier = Modifier.testTag(TAG_BUILDER_SUBJECT_PREFIX + subject.id)
                    )
                }
                if (full) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        ProblemText(stringResource(R.string.routine_subjects_max, localizedNumber(RoutineScope.MAX_SUBJECTS)))
                    }
                }
            }
        }
        BuilderBottomBar(
            summary = stringResource(R.string.routine_subjects_selected, localizedNumber(state.selectedSubjectIds.size)),
            button = stringResource(R.string.routine_next),
            enabled = state.canLeaveSubjects,
            onClick = actions.onNext
        )
    }
}

@Composable
private fun SubjectCheckRow(name: String, checked: Boolean, enabled: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(enabled = enabled, onClick = onToggle).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() }, enabled = enabled)
        Text(text = name, style = ProstutiTypography.bnBodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
    }
}

/** Step 2: tri-state subject → chapter → topic picks (the practice builder's cards), all ticked at first. */
@Composable
internal fun ChapterStep(state: RoutineBuilderUiState, actions: RoutineBuilderActions, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        if (state.isLoadingChapters && state.groups.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                ProstutiSpinner()
            }
        } else {
            LazyColumn(
                state = rememberLazyListState(),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                items(state.groups, key = { it.subjectId }) { group ->
                    Column {
                        SubjectSelectHeader(
                            title = localized(group.subjectNameBn, group.subjectNameEn), group = group, selectedTopicIds = state.selectedTopicIds,
                            onToggle = { actions.onToggleSubjectAll(group.subjectId) }, modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            group.chapters.forEach { chapterGroup ->
                                ChapterCard(
                                    chapterGroup = chapterGroup, selectedTopicIds = state.selectedTopicIds,
                                    onToggleOpen = { actions.onToggleChapterOpen(chapterGroup.chapter.id) },
                                    onToggleAll = { actions.onToggleChapterAll(chapterGroup.chapter.id) },
                                    onToggleTopic = actions.onToggleTopic
                                )
                            }
                        }
                    }
                }
                when (state.scope.problem) {
                    RoutineScope.Problem.NOTHING_SELECTED -> item { ProblemText(stringResource(R.string.routine_scope_nothing)) }
                    RoutineScope.Problem.TOO_MANY_CHAPTERS, RoutineScope.Problem.TOO_MANY_TOPICS -> item {
                        ProblemText(
                            stringResource(
                                R.string.routine_scope_too_many,
                                localizedNumber(RoutineScope.MAX_CHAPTERS), localizedNumber(RoutineScope.MAX_TOPICS)
                            )
                        )
                    }
                    RoutineScope.Problem.TOO_MANY_SUBJECTS, null -> Unit
                }
            }
        }
        BuilderBottomBar(
            summary = stringResource(
                R.string.routine_chapters_selected,
                localizedNumber(state.selectedChapterCount), localizedNumber(state.selectedTopicIds.size)
            ),
            button = stringResource(R.string.routine_next),
            enabled = state.canLeaveChapters,
            onClick = actions.onNext
        )
    }
}

@Composable
internal fun CenteredProgress(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ProstutiSpinner()
    }
}
