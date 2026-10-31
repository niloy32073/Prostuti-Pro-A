package com.prostuti.student.ui.screen.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.prostuti.student.data.model.response.Subject
import com.prostuti.student.ui.component.BackButton
import com.prostuti.student.ui.component.PullToRefreshErrorOverlayWrapper
import com.prostuti.student.ui.theme.ProstutiTypography
import org.koin.compose.viewmodel.koinViewModel

/** The Medium/Expanded combined step — subject list + the focused subject's chapter/topic tree
 *  side by side (SubjectChapterMedium/Expanded in the design), replacing the compact flow's two
 *  separate screens. Leads straight into ExamProperties. */
@Composable
fun PracticeSubjectChapterScreen(
    onBack: () -> Unit,
    onNext: (subjectIds: List<String>, chapterIds: List<String>, topicIds: List<String>) -> Unit,
    viewModel: PracticeSubjectChapterViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        PullToRefreshErrorOverlayWrapper(
            errors = state.errors,
            errorCleaner = viewModel::clearError,
            overlayPadding = padding
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BackButton(onClick = onBack)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "সাবজেক্ট ও চ্যাপ্টার-টপিক বাছাই করো",
                            style = ProstutiTypography.bnTitleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "প্র্যাকটিস এক্সাম",
                            style = ProstutiTypography.enLabelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                if (state.isLoadingSubjects) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        LazyColumn(
                            modifier = Modifier.width(280.dp).fillMaxHeight(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            item {
                                Text(
                                    text = "বিষয় (একাধিক নির্বাচনযোগ্য)",
                                    style = ProstutiTypography.bnLabelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                            items(state.subjects, key = { it.id }) { subject ->
                                SubjectFocusRow(
                                    subject = subject,
                                    checked = subject.id in state.selectedSubjectIds,
                                    focused = subject.id == state.focusedSubjectId,
                                    onToggle = { viewModel.toggleSubject(subject.id) },
                                    onFocus = { viewModel.focusSubject(subject.id) }
                                )
                            }
                        }

                        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            val focusedId = state.focusedSubjectId
                            when {
                                focusedId == null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "বাম পাশ থেকে একটা সাবজেক্ট বেছে নাও",
                                        style = ProstutiTypography.bnBodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                focusedId in state.loadingSubjectIds -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                }

                                else -> {
                                    val group = state.focusedGroup
                                    if (group == null) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "এই সাবজেক্ট এখনো নির্বাচিত নয়",
                                                style = ProstutiTypography.bnBodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(20.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            item {
                                                SubjectSelectHeader(
                                                    title = "${group.subjectNameBn} — চ্যাপ্টার ও টপিক",
                                                    group = group,
                                                    selectedTopicIds = state.selectedTopicIds,
                                                    onToggle = { viewModel.toggleSubjectAll(focusedId) }
                                                )
                                            }
                                            items(group.chapters, key = { it.chapter.id }) { chapterGroup ->
                                                ChapterCard(
                                                    chapterGroup = chapterGroup,
                                                    selectedTopicIds = state.selectedTopicIds,
                                                    onToggleOpen = { viewModel.toggleChapterOpen(focusedId, chapterGroup.chapter.id) },
                                                    onToggleAll = { viewModel.toggleChapterAll(focusedId, chapterGroup.chapter.id) },
                                                    onToggleTopic = viewModel::toggleTopic
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${state.selectedSubjectIds.size}টি বিষয়, ${state.selectedTopicIds.size}টি টপিক নির্বাচিত",
                        style = ProstutiTypography.bnLabelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            onNext(
                                state.selectedSubjectIds.toList(),
                                state.selectedChapterIds.toList(),
                                state.selectedTopicIds.toList()
                            )
                        },
                        enabled = state.canProceed,
                        shape = RoundedCornerShape(50.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(text = "পরবর্তী ধাপ", style = ProstutiTypography.bnBodyLarge)
                    }
                }
            }
    
        }
    }
}

@Composable
private fun SubjectFocusRow(
    subject: Subject,
    checked: Boolean,
    focused: Boolean,
    onToggle: () -> Unit,
    onFocus: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (focused) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
            .clickable(enabled = checked, onClick = onFocus)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Text(
            text = subject.nameBn,
            style = ProstutiTypography.bnBodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (checked) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow)
                    .clickable(onClick = onFocus),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "${subject.nameBn} এর চ্যাপ্টার দেখো",
                    tint = if (focused) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
