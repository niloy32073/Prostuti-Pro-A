package com.prostuti.student.ui.screen.group

import com.prostuti.student.ui.component.paneIsMediumOrWider
import androidx.compose.material3.VerticalDivider
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.ColumnScope
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import com.prostuti.student.ui.component.ProstutiSpinner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import com.prostuti.student.R
import com.prostuti.student.ui.screen.routine.slot.RefreshAtWindowEdges
import com.prostuti.student.ui.screen.routine.slot.HandleExamLaunch
import com.prostuti.student.ui.screen.routine.slot.SlotAction
import com.prostuti.student.ui.screen.routine.topicLabelText
import com.prostuti.student.domain.routine.TopicLabel
import com.prostuti.student.ui.screen.routine.slot.rememberNow
import java.time.Instant
import com.prostuti.student.data.model.enums.GroupRole
import com.prostuti.student.data.model.response.StudyGroup
import com.prostuti.student.data.model.response.StudyGroupMember
import com.prostuti.student.ui.component.BackButton
import com.prostuti.student.ui.component.PullToRefreshErrorOverlayWrapper
import com.prostuti.student.ui.component.localizedNumber
import com.prostuti.student.ui.screen.group.component.GroupConfirmDialog
import com.prostuti.student.ui.theme.ProstutiTypography
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun GroupDetailScreen(
    groupId: String,
    onBack: () -> Unit,
    onNavigateToMembers: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToJoinRequests: () -> Unit,
    onCreateRoutine: () -> Unit,
    onScheduleAdhoc: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenExamAttempt: (attemptId: String, title: String, lockResultUntil: String?) -> Unit,
    onOpenExamResult: (attemptId: String, title: String, lockResultUntil: String?) -> Unit,
    onOpenRanking: (examId: String, title: String, closesAt: String) -> Unit,
    onSeeAllPastExams: () -> Unit,
    /** The group is gone, or I just left it: close every screen of it. */
    onGroupClosed: () -> Unit,
    /** False beside the group list (medium and wider), which is already on screen. */
    showBack: Boolean = true,
    /** A slot was opened: show its sheet (SlotDetail, which shares this ViewModel). */
    onShowSlot: () -> Unit = {},
    viewModel: GroupDetailViewModel = koinViewModel(parameters = { parametersOf(groupId) })
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // A slot was opened: its sheet is a destination (SlotDetail) that reads this same ViewModel.
    LaunchedEffect(state.openSlotId) { if (state.openSlotId != null) onShowSlot() }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val copiedMessage = stringResource(R.string.group_code_copied)

    // The ViewModel survives a pop back to this entry, so reload whenever the screen is shown again.
    LaunchedEffect(Unit) { viewModel.refresh() }

    RefreshAtWindowEdges(slots = state.todaySlots, onRefresh = viewModel::refresh)
    // Exam titles for the engine's screens: the slot's name, else its topics.
    val titles = (state.todaySlots + state.pastSlots).associate { it.id to (it.title ?: topicLabelText(state.labels[it.id] ?: TopicLabel.Count(it.topicIds.size))) }
    HandleExamLaunch(
        launch = state.examLaunch, titleOf = { titles[it].orEmpty() }, onConsumed = viewModel::consumeExamLaunch,
        onOpenAttempt = onOpenExamAttempt, onOpenResult = onOpenExamResult,
    )


    LaunchedEffect(state.notice) {
        state.notice?.let {
            viewModel.consumeNotice()
            snackbarHostState.showSnackbar(context.getString(it.res, it.arg))
        }
    }

    LaunchedEffect(state.isGone) {
        if (state.isGone) {
            viewModel.consumeGone()
            onGroupClosed()
        }
    }

    GroupDetailContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        showBack = showBack,
        onSeeAllMembers = onNavigateToMembers,
        onOpenSettings = onNavigateToSettings,
        onOpenJoinRequests = onNavigateToJoinRequests,
        onCreateRoutine = onCreateRoutine,
        onScheduleAdhoc = onScheduleAdhoc,
        onOpenCalendar = onOpenCalendar,
        onOpenSlot = viewModel::openSlot,
        onSlotAction = { slotId, action ->
            val slot = (state.todaySlots + state.pastSlots).firstOrNull { it.id == slotId }
            val examId = slot?.examId
            if (action == SlotAction.RANKING) { if (examId != null) onOpenRanking(examId, titles[slotId].orEmpty(), slot.closesAt) }
            else viewModel.openExam(slotId, action)
        },
        onSeeAllPastExams = onSeeAllPastExams,
        onLeave = viewModel::leave,
        onCopyCode = { code ->
            context.copyToClipboard(code)
            scope.launch { snackbarHostState.showSnackbar(copiedMessage) }
        },
        onShareCode = { group, code -> context.shareInviteCode(context.getString(R.string.group_share_text, group.name, code)) },
        onRefresh = viewModel::refresh,
        onClearError = viewModel::clearError,
    )
}

@Composable
fun GroupDetailContent(
    state: GroupDetailUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBack: () -> Unit,
    showBack: Boolean = true,
    onSeeAllMembers: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenJoinRequests: () -> Unit = {},
    onCreateRoutine: () -> Unit = {},
    onScheduleAdhoc: () -> Unit = {},
    onOpenCalendar: () -> Unit = {},
    onOpenSlot: (String) -> Unit = {},
    onSlotAction: (String, SlotAction) -> Unit = { _, _ -> },
    onSeeAllPastExams: () -> Unit = {},
    onLeave: () -> Unit = {},
    onCopyCode: (String) -> Unit,
    onShareCode: (StudyGroup, String) -> Unit,
    onRefresh: () -> Unit,
    onClearError: (String) -> Unit,
    /** Fixed time for tests; null = the ticking clock. */
    clock: Instant? = null,
) {
    val ticking by rememberNow()
    val now = clock ?: ticking
    val scope = rememberCoroutineScope()
    val comingSoon = stringResource(R.string.feature_coming_soon)

    var confirmLeave by rememberSaveable { mutableStateOf(false) }
    if (confirmLeave) {
        GroupConfirmDialog(
            title = stringResource(R.string.group_leave_title),
            body = stringResource(R.string.group_leave_body),
            confirmLabel = stringResource(R.string.group_leave),
            destructive = true,
            onConfirm = { confirmLeave = false; onLeave() },
            onDismiss = { confirmLeave = false },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        PullToRefreshErrorOverlayWrapper(
            errors = state.errors,
            errorCleaner = onClearError,
            overlayPadding = padding,
            pullToRefreshEnabled = true,
            isRefreshing = state.isLoading && state.group != null,
            onRefresh = onRefresh
        ) {
            val group = state.group
            if (group == null) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    if (state.isLoading) ProstutiSpinner()
                }
                return@PullToRefreshErrorOverlayWrapper
            }

            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp, start = 16.dp, end = 16.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showBack) {
                        BackButton(onClick = onBack)
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = group.name, style = ProstutiTypography.bnTitleLarge, color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.group_member_count, localizedNumber(group.memberCount)),
                            style = ProstutiTypography.enBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    if (group.myRole == GroupRole.ADMIN) {
                        IconButton(onClick = dropUnlessResumed { onOpenSettings() }, modifier = Modifier.testTag(TAG_SETTINGS)) {
                            Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.group_settings_title))
                        }
                    }
                }

                // What to do now (exams, prep) and the group around it (past exams, members).
                val main: @Composable ColumnScope.() -> Unit = {
                    group.description?.takeIf { it.isNotBlank() }?.let {
                        Text(text = it, style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    if (group.myRole == GroupRole.ADMIN && state.pendingRequestCount > 0) {
                        JoinRequestsBanner(count = state.pendingRequestCount, onClick = dropUnlessResumed { onOpenJoinRequests() })
                    }

                    val code = group.inviteCode
                    if (group.myRole == GroupRole.ADMIN && code != null) {
                        InviteCodeCard(code = code, onCopy = { onCopyCode(code) }, onShare = { onShareCode(group, code) })
                    }

                    // A non-member (e.g. opened from a notification later) has no exams to see.
                    if (group.myRole != null) {
                        GroupExamSection(
                            state = state, now = now, onOpenSlot = onOpenSlot, onSlotAction = onSlotAction,
                            onOpenCalendar = dropUnlessResumed { onOpenCalendar() },
                            onCreateRoutine = dropUnlessResumed { onCreateRoutine() },
                            onScheduleAdhoc = dropUnlessResumed { onScheduleAdhoc() },
                            onConcepts = { scope.launch { snackbarHostState.showSnackbar(comingSoon) } },
                        )
                    }
                }
                val side: @Composable ColumnScope.() -> Unit = {
                    if (group.myRole != null && state.pastSlots.isNotEmpty()) {
                        PastExamsPreview(state, onOpen = { onSlotAction(it, SlotAction.RANKING) }, onSeeAll = dropUnlessResumed { onSeeAllPastExams() })
                    }

                    MembersSection(members = state.memberPreview, totalCount = group.memberCount, onSeeAll = dropUnlessResumed { onSeeAllMembers() })

                    // An admin leaves from GroupSettings, where the last-admin rule is explained.
                    if (group.myRole == GroupRole.MEMBER) {
                        TextButton(
                            onClick = { confirmLeave = true },
                            enabled = !state.isLeaving,
                            modifier = Modifier.fillMaxWidth().testTag(TAG_LEAVE)
                        ) {
                            Text(stringResource(R.string.group_leave), style = ProstutiTypography.bnLabelLarge, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (paneIsMediumOrWider()) {
                    // Medium and up: two columns, each scrolling on its own.
                    Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Column(
                            Modifier.weight(0.6f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp), content = main
                        )
                        VerticalDivider(Modifier.padding(vertical = 8.dp))
                        Column(
                            Modifier.weight(0.4f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(vertical = 6.dp).testTag(TAG_DETAIL_SIDE_PANE),
                            verticalArrangement = Arrangement.spacedBy(20.dp), content = side
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        main()
                        side()
                    }
                }
            }
        }
    }
}

/** The last three ended exams; a row opens its ranking. */
@Composable
private fun PastExamsPreview(state: GroupDetailUiState, onOpen: (String) -> Unit, onSeeAll: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag(TAG_PAST_PREVIEW)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.past_title), style = ProstutiTypography.bnTitleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            TextButton(onClick = onSeeAll) { Text(stringResource(R.string.group_see_all), style = ProstutiTypography.bnLabelLarge) }
        }
        state.pastSlots.forEach { slot ->
            val title = slot.title ?: topicLabelText(state.labels[slot.id] ?: TopicLabel.Count(slot.topicIds.size))
            PastExamRow(slot, title, state.pastStats[slot.id], onClick = dropUnlessResumed { onOpen(slot.id) })
        }
    }
}

@Composable
private fun JoinRequestsBanner(count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer).clickable(onClick = onClick).padding(14.dp)
            .testTag(TAG_REQUESTS_BANNER),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.group_pending_requests, localizedNumber(count)),
            style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.weight(1f)
        )
        Text(stringResource(R.string.group_see_all), style = ProstutiTypography.bnLabelLarge, color = MaterialTheme.colorScheme.onTertiaryContainer)
    }
}

@Composable
private fun InviteCodeCard(code: String, onCopy: () -> Unit, onShare: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primaryContainer).padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp)
            .testTag(TAG_INVITE_CODE),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.group_invite_code_title), style = ProstutiTypography.bnLabelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(code, style = ProstutiTypography.enTitleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(vertical = 2.dp))
            Text(stringResource(R.string.group_invite_code_hint), style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        IconButton(onClick = onCopy) {
            Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.group_copy_code_cd), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        IconButton(onClick = onShare) {
            Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.group_share_code_cd), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun MembersSection(members: List<StudyGroupMember>, totalCount: Int, onSeeAll: () -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.group_members_section, localizedNumber(totalCount)),
                style = ProstutiTypography.bnBodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.group_see_all), style = ProstutiTypography.bnLabelLarge, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onSeeAll).padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            members.forEach { member -> MemberRow(member = member, isMe = false) }
        }
    }
}

/** One member row: initial, username (+ "তুমি"), admin badge, then [trailing] (the admin's menu). Shared with GroupMembersScreen. */
@Composable
internal fun MemberRow(member: StudyGroupMember, isMe: Boolean, trailing: @Composable () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
            Text(text = member.username.take(1).uppercase(), style = ProstutiTypography.bnLabelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Text(
            text = if (isMe) stringResource(R.string.group_member_you, member.username) else member.username,
            style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(start = 10.dp), maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        if (member.role == GroupRole.ADMIN) {
            Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.tertiaryContainer).padding(horizontal = 8.dp, vertical = 2.dp)) {
                Text(text = stringResource(R.string.group_admin_badge), style = ProstutiTypography.bnLabelMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
            }
        }
        trailing()
    }
}

private fun Context.copyToClipboard(text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("group code", text))
}

private fun Context.shareInviteCode(message: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, message)
    }
    startActivity(Intent.createChooser(send, null))
}

const val TAG_DETAIL_SIDE_PANE = "group_detail_side_pane"
const val TAG_INVITE_CODE = "group_invite_code"
const val TAG_SETTINGS = "group_settings"
const val TAG_REQUESTS_BANNER = "group_requests_banner"
const val TAG_LEAVE = "group_leave"
const val TAG_PAST_PREVIEW = "group_past_preview"
const val TAG_ADMIN_SCHEDULE = "group_admin_schedule"
const val TAG_CREATE_ROUTINE = "group_create_routine"
const val TAG_SCHEDULE_ADHOC = "group_schedule_adhoc"
