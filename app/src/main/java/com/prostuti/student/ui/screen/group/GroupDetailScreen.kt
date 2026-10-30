package com.prostuti.student.ui.screen.group

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prostuti.student.BuildConfig
import com.prostuti.student.data.model.enums.GroupRole
import com.prostuti.student.data.model.enums.TimedExamState
import com.prostuti.student.data.model.response.GroupDetail
import com.prostuti.student.data.model.response.GroupMember
import com.prostuti.student.data.model.response.GroupPastExam
import com.prostuti.student.ui.component.BackButton
import com.prostuti.student.ui.component.ObserveErrors
import com.prostuti.student.ui.component.PrimaryButton
import com.prostuti.student.ui.theme.ProstutiTypography
import com.prostuti.student.util.formatRemainingBn
import com.prostuti.student.util.toBengaliDigits
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun GroupDetailScreen(
    groupId: String,
    onBack: () -> Unit,
    onNavigateToRoutineCreate: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToPastExams: () -> Unit,
    onNavigateToMembers: () -> Unit,
    onNavigateToPastExam: (examId: String) -> Unit,
    onNavigateToAttempt: (examId: String) -> Unit,
    onNavigateToLockedResult: (examId: String) -> Unit,
    onNavigateToRanking: (examId: String) -> Unit,
    viewModel: GroupDetailViewModel = koinViewModel(parameters = { parametersOf(groupId) })
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    ObserveErrors(errors = state.errors, snackbarHostState = snackbarHostState, onConsumed = viewModel::clearError)

    // The ViewModel is retained across a pop-back-to (e.g. after creating a routine, or
    // submitting the exam, three/one screens down) — recomposing this entry doesn't re-run its
    // init{}, so without this the screen would keep showing whatever it loaded on first entry.
    LaunchedEffect(Unit) { viewModel.refresh() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (state.isLoading && state.detail == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }
        val detail = state.detail ?: return@Scaffold

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp, start = 16.dp, end = 16.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButton(onClick = onBack)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = detail.group.nameBn, style = ProstutiTypography.bnTitleLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = "${detail.group.memberCount.toBengaliDigits()} সদস্য",
                        style = ProstutiTypography.enBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .clickable(onClick = onNavigateToCalendar)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = "গ্রুপ রুটিন ক্যালেন্ডার দেখো", style = ProstutiTypography.bnBodyMedium,
                        color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f).padding(start = 10.dp)
                    )
                }

                ExamStatusSection(
                    detail = detail,
                    examState = state.examState,
                    now = state.now,
                    isDemoAdvancing = state.isDemoAdvancing,
                    onDemoAdvance = viewModel::advanceDemoState,
                    onNavigateToRoutineCreate = onNavigateToRoutineCreate,
                    onNavigateToAttempt = onNavigateToAttempt,
                    onNavigateToLockedResult = onNavigateToLockedResult,
                    onNavigateToRanking = onNavigateToRanking
                )

                PastExamsSection(pastExams = detail.recentPastExams, onSeeAll = onNavigateToPastExams, onOpen = onNavigateToPastExam)

                MembersSection(members = detail.memberPreview, totalCount = detail.group.memberCount, onSeeAll = onNavigateToMembers)

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ExamStatusSection(
    detail: GroupDetail,
    examState: TimedExamState?,
    now: Long,
    isDemoAdvancing: Boolean,
    onDemoAdvance: () -> Unit,
    onNavigateToRoutineCreate: () -> Unit,
    onNavigateToAttempt: (String) -> Unit,
    onNavigateToLockedResult: (String) -> Unit,
    onNavigateToRanking: (String) -> Unit
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "এক্সাম স্ট্যাটাস", style = ProstutiTypography.bnBodyLarge, color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (BuildConfig.DEBUG && detail.currentExam != null) {
                Text(
                    text = if (isDemoAdvancing) "…" else "ডেমো: অবস্থা পাল্টাও",
                    style = ProstutiTypography.enLabelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .clickable(enabled = !isDemoAdvancing, onClick = onDemoAdvance)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        val exam = detail.currentExam
        when {
            detail.routine == null && detail.group.myRole == GroupRole.ADMIN -> NoRoutineAdminCard(onCreate = onNavigateToRoutineCreate)
            detail.routine == null -> NoRoutineMemberCard()
            exam == null -> Text(
                text = "আজ এই গ্রুপের কোনো এক্সাম-দিন নেই।", style = ProstutiTypography.bnBodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            examState == TimedExamState.CLOSED -> ClosedExamCard(
                titleBn = exam.titleBn,
                onViewRanking = { onNavigateToRanking(exam.examId) }
            )
            examState == TimedExamState.OPEN_SUBMITTED -> OpenExamCard(
                remainingLabel = formatRemainingBn(exam.windowEndEpochMillis - now),
                titleBn = exam.titleBn, subtitleBn = exam.subtitleBn,
                ctaLabel = "তুমি সাবমিট করেছো — দেখো", onCta = { onNavigateToLockedResult(exam.examId) }
            )
            examState == TimedExamState.OPEN_NOT_SUBMITTED -> OpenExamCard(
                remainingLabel = formatRemainingBn(exam.windowEndEpochMillis - now),
                titleBn = exam.titleBn, subtitleBn = exam.subtitleBn,
                ctaLabel = "এক্সামে অংশ নিন", onCta = { onNavigateToAttempt(exam.examId) }
            )
            else -> Text(
                text = "উইন্ডো এখনও শুরু হয়নি।", style = ProstutiTypography.bnBodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NoRoutineAdminCard(onCreate: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "এখনও কোনো গ্রুপ রুটিন তৈরি হয়নি — রুটিন সেট করলে এক্সাম-দিনগুলোতে এক্সাম অটো শুরু হবে",
            style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        PrimaryButton(text = "গ্রুপ রুটিন তৈরি করুন", onClick = onCreate, modifier = Modifier.fillMaxWidth(0.8f))
    }
}

@Composable
private fun NoRoutineMemberCard() {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(16.dp)) {
        Text(
            text = "এখনও অ্যাডমিন গ্রুপের রুটিন সেট করেনি — রুটিন সেট হলে নির্দিষ্ট দিনে এক্সাম এখানে অটো দেখাবে।",
            style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OpenExamCard(remainingLabel: String, titleBn: String, subtitleBn: String, ctaLabel: String, onCta: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            Text(
                text = "উইন্ডো খোলা আছে আরও $remainingLabel", style = ProstutiTypography.enLabelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(start = 8.dp)
            )
        }
        Text(text = titleBn, style = ProstutiTypography.bnTitleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(text = subtitleBn, style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        PrimaryButton(text = ctaLabel, onClick = onCta)
    }
}

@Composable
private fun ClosedExamCard(titleBn: String, onViewRanking: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 8.dp, vertical = 3.dp)) {
            Text(text = "উইন্ডো শেষ", style = ProstutiTypography.bnLabelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Text(text = titleBn, style = ProstutiTypography.bnTitleMedium, color = MaterialTheme.colorScheme.onSurface)
        PrimaryButton(text = "র‍্যাংকিং ও অ্যান্সার শিট দেখো", onClick = onViewRanking)
    }
}

@Composable
private fun PastExamsSection(pastExams: List<GroupPastExam>, onSeeAll: () -> Unit, onOpen: (String) -> Unit) {
    if (pastExams.isEmpty()) return
    Column {
        SectionHeader(title = "আগের এক্সামগুলো", onSeeAll = onSeeAll)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            pastExams.forEach { exam ->
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .clickable { onOpen(exam.examId) }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = exam.titleBn, style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = "${exam.dateLabelBn} · ${exam.submittedCount.toBengaliDigits()} জন সাবমিট · গড় ${exam.averagePercent.toBengaliDigits()}%",
                            style = ProstutiTypography.enLabelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MembersSection(members: List<GroupMember>, totalCount: Int, onSeeAll: () -> Unit) {
    Column {
        SectionHeader(title = "সদস্যরা (${totalCount.toBengaliDigits()})", onSeeAll = onSeeAll)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            members.forEach { member ->
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                        Text(text = member.initial, style = ProstutiTypography.bnLabelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Text(
                        text = member.name, style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f).padding(start = 10.dp), maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    if (member.isAdmin) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.tertiaryContainer).padding(horizontal = 8.dp, vertical = 2.dp)) {
                            Text(text = "অ্যাডমিন", style = ProstutiTypography.bnLabelMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, onSeeAll: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text = title, style = ProstutiTypography.bnBodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(
            text = "সবাই দেখো", style = ProstutiTypography.bnLabelLarge, color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onSeeAll)
        )
    }
}
