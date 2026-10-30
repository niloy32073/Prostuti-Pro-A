package com.prostuti.student.ui.screen.group

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prostuti.student.ui.component.BackButton
import com.prostuti.student.ui.component.ObserveErrors
import com.prostuti.student.ui.screen.practice.ChapterCard
import com.prostuti.student.ui.screen.practice.SubjectSelectHeader
import com.prostuti.student.ui.theme.ProstutiTypography
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun GroupRoutineChapterScreen(
    groupId: String,
    subjectIds: List<String>,
    onBack: () -> Unit,
    onNext: (chapterIds: List<String>, topicIds: List<String>) -> Unit,
    viewModel: GroupRoutineChapterViewModel = koinViewModel(parameters = { parametersOf(groupId, subjectIds) })
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    ObserveErrors(errors = state.errors, snackbarHostState = snackbarHostState, onConsumed = viewModel::clearError)

    Scaffold(containerColor = MaterialTheme.colorScheme.background, snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                BackButton(onClick = onBack)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = "অধ্যায় ও টপিক বেছে নাও", style = ProstutiTypography.bnTitleMedium, color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        text = "গ্রুপ রুটিন তৈরি · ${subjectIds.size}টি সাবজেক্ট · ধাপ ২/৩", style = ProstutiTypography.enLabelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    items(state.groups, key = { it.subjectId }) { group ->
                        Column {
                            SubjectSelectHeader(
                                title = group.subjectNameBn, group = group, selectedTopicIds = state.selectedTopicIds,
                                onToggle = { viewModel.toggleSubjectAll(group.subjectId) }, modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                group.chapters.forEach { chapterGroup ->
                                    ChapterCard(
                                        chapterGroup = chapterGroup, selectedTopicIds = state.selectedTopicIds,
                                        onToggleOpen = { viewModel.toggleChapterOpen(chapterGroup.chapter.id) },
                                        onToggleAll = { viewModel.toggleChapterAll(chapterGroup.chapter.id) },
                                        onToggleTopic = viewModel::toggleTopic
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${state.selectedTopicIds.size}/${state.totalTopicCount}টি টপিক নির্বাচিত", style = ProstutiTypography.bnLabelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { onNext(state.selectedChapterIds.toList(), state.selectedTopicIds.toList()) },
                    enabled = !state.isLoading && state.canProceed, shape = RoundedCornerShape(50.dp), modifier = Modifier.height(46.dp)
                ) {
                    Text(text = "পরবর্তী", style = ProstutiTypography.bnBodyLarge)
                }
            }
        }
    }
}
