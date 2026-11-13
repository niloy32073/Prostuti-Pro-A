package com.prostuti.student.ui.screen.group

import com.prostuti.student.ui.component.paneIsMediumOrWider
import androidx.compose.material3.VerticalDivider
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.prostuti.student.ui.component.ProstutiSpinner
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import com.prostuti.student.R
import com.prostuti.student.ui.component.BackButton
import com.prostuti.student.ui.component.InfoNote
import com.prostuti.student.ui.component.PrimaryButton
import com.prostuti.student.ui.component.PullToRefreshErrorOverlayWrapper
import com.prostuti.student.ui.component.localizedNumber
import com.prostuti.student.ui.screen.group.GroupSettingsUiState.MaxMembersProblem
import com.prostuti.student.ui.screen.group.component.GroupConfirmDialog
import com.prostuti.student.ui.theme.ProstutiTypography
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private enum class SettingsConfirm { REGENERATE, LEAVE, DELETE }

@Composable
fun GroupSettingsScreen(
    groupId: String,
    onBack: () -> Unit,
    onOpenJoinRequests: () -> Unit,
    onOpenMembers: () -> Unit,
    /** I deleted or left the group: close every screen of it. */
    onGroupClosed: () -> Unit,
    viewModel: GroupSettingsViewModel = koinViewModel(parameters = { parametersOf(groupId) })
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.notice) {
        state.notice?.let {
            viewModel.consumeNotice()
            snackbarHostState.showSnackbar(context.getString(it.res, it.arg))
        }
    }
    LaunchedEffect(state.isClosed) {
        if (state.isClosed) { viewModel.consumeClosed(); onGroupClosed() }
    }
    LaunchedEffect(state.isNotAdmin) {
        if (state.isNotAdmin) { viewModel.consumeNotAdmin(); onBack() }
    }

    GroupSettingsContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onNameChange = viewModel::onNameChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onPublicChange = viewModel::onPublicChange,
        onMaxMembersChange = viewModel::onMaxMembersChange,
        onSave = viewModel::save,
        onRegenerateCode = viewModel::regenerateCode,
        onOpenJoinRequests = onOpenJoinRequests,
        onOpenMembers = onOpenMembers,
        onLeave = viewModel::leave,
        onDelete = viewModel::delete,
        onRefresh = viewModel::refresh,
        onClearError = viewModel::clearError,
    )
}

@Composable
fun GroupSettingsContent(
    state: GroupSettingsUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPublicChange: (Boolean) -> Unit,
    onMaxMembersChange: (String) -> Unit,
    onSave: () -> Unit,
    onRegenerateCode: () -> Unit,
    onOpenJoinRequests: () -> Unit,
    onOpenMembers: () -> Unit,
    onLeave: () -> Unit,
    onDelete: () -> Unit,
    onRefresh: () -> Unit,
    onClearError: (String) -> Unit,
) {
    var confirm by rememberSaveable { mutableStateOf<SettingsConfirm?>(null) }
    val group = state.group

    if (group != null) {
        when (confirm) {
            SettingsConfirm.REGENERATE -> GroupConfirmDialog(
                title = stringResource(R.string.group_regenerate_title),
                body = stringResource(R.string.group_regenerate_body),
                confirmLabel = stringResource(R.string.group_regenerate),
                onConfirm = { confirm = null; onRegenerateCode() },
                onDismiss = { confirm = null },
            )
            SettingsConfirm.LEAVE -> GroupConfirmDialog(
                title = stringResource(R.string.group_leave_title),
                body = stringResource(R.string.group_leave_admin_body),
                confirmLabel = stringResource(R.string.group_leave),
                destructive = true,
                onConfirm = { confirm = null; onLeave() },
                onDismiss = { confirm = null },
            )
            SettingsConfirm.DELETE -> GroupConfirmDialog(
                title = stringResource(R.string.group_delete_title),
                body = stringResource(R.string.group_delete_body, group.name),
                confirmLabel = stringResource(R.string.group_delete),
                destructive = true,
                typeToConfirm = group.name,
                onConfirm = { confirm = null; onDelete() },
                onDismiss = { confirm = null },
            )
            null -> Unit
        }
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
            isRefreshing = state.isLoading && group != null,
            onRefresh = onRefresh
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp, start = 16.dp, end = 16.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BackButton(onClick = onBack)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.group_settings_title), style = ProstutiTypography.bnTitleLarge, color = MaterialTheme.colorScheme.onSurface)
                }

                if (group == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (state.isLoading) ProstutiSpinner()
                    }
                    return@PullToRefreshErrorOverlayWrapper
                }

                val busy = state.running != null
                // The group's details form, and everything else (code, people, leaving or deleting).
                val form: @Composable ColumnScope.() -> Unit = {
                    SectionTitle(stringResource(R.string.group_settings_info))
                    OutlinedTextField(
                        value = state.form.name, onValueChange = onNameChange, singleLine = true,
                        label = { Text(stringResource(R.string.group_name_label), style = ProstutiTypography.bnBodyMedium) },
                        isError = !state.nameValid,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag(TAG_SETTINGS_NAME)
                    )
                    OutlinedTextField(
                        value = state.form.description, onValueChange = onDescriptionChange, minLines = 2,
                        label = { Text(stringResource(R.string.group_description_label), style = ProstutiTypography.bnBodyMedium) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.group_public_label), style = ProstutiTypography.bnBodyLarge, color = MaterialTheme.colorScheme.onSurface)
                            Text(stringResource(R.string.group_public_hint), style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = state.form.isPublic, onCheckedChange = onPublicChange, modifier = Modifier.testTag(TAG_SETTINGS_PUBLIC))
                    }
                    if (state.isTurningPrivate) {
                        InfoNote(text = stringResource(R.string.group_turning_private_warning))
                    }
                    val maxProblem = state.maxMembersProblem
                    OutlinedTextField(
                        value = state.form.maxMembers, onValueChange = onMaxMembersChange, singleLine = true,
                        label = { Text(stringResource(R.string.group_max_members_label), style = ProstutiTypography.bnBodyMedium) },
                        isError = maxProblem != null,
                        supportingText = {
                            Text(
                                when (maxProblem) {
                                    MaxMembersProblem.BELOW_MEMBER_COUNT -> stringResource(R.string.api_err_max_members_too_low)
                                    MaxMembersProblem.NOT_A_NUMBER, MaxMembersProblem.OUT_OF_RANGE -> stringResource(R.string.group_max_members_range)
                                    null -> stringResource(R.string.group_max_members_now, localizedNumber(group.memberCount))
                                },
                                style = ProstutiTypography.bnBodyMedium
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag(TAG_SETTINGS_MAX)
                    )
                    PrimaryButton(
                        text = stringResource(R.string.group_settings_save),
                        enabled = state.canSave,
                        loading = state.running == GroupSettingsAction.SAVE,
                        onClick = onSave,
                        modifier = Modifier.testTag(TAG_SETTINGS_SAVE)
                    )
                }
                val manage: @Composable ColumnScope.() -> Unit = {
                    SectionTitle(stringResource(R.string.group_invite_code_title))
                    Text(group.inviteCode.orEmpty(), style = ProstutiTypography.enTitleLarge, color = MaterialTheme.colorScheme.onSurface)
                    OutlinedButton(onClick = { confirm = SettingsConfirm.REGENERATE }, enabled = !busy) {
                        Text(stringResource(R.string.group_regenerate), style = ProstutiTypography.bnLabelLarge)
                    }

                    HorizontalDivider()
                    SettingsLink(stringResource(R.string.group_members_title), onClick = dropUnlessResumed { onOpenMembers() })
                    if (group.isPublic) {
                        SettingsLink(stringResource(R.string.group_join_requests_title), onClick = dropUnlessResumed { onOpenJoinRequests() })
                    }

                    HorizontalDivider()
                    TextButton(onClick = { confirm = SettingsConfirm.LEAVE }, enabled = !busy, modifier = Modifier.testTag(TAG_SETTINGS_LEAVE)) {
                        Text(stringResource(R.string.group_leave), style = ProstutiTypography.bnLabelLarge, color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { confirm = SettingsConfirm.DELETE }, enabled = !busy, modifier = Modifier.testTag(TAG_SETTINGS_DELETE)) {
                        Text(stringResource(R.string.group_delete), style = ProstutiTypography.bnLabelLarge, color = MaterialTheme.colorScheme.error)
                    }
                    Text(stringResource(R.string.group_delete_hint), style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                if (paneIsMediumOrWider()) {
                    Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Column(Modifier.weight(0.55f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp), content = form)
                        VerticalDivider(Modifier.padding(vertical = 8.dp))
                        Column(Modifier.weight(0.45f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp), content = manage)
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        form()
                        HorizontalDivider()
                        manage()
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = ProstutiTypography.bnBodyLarge, color = MaterialTheme.colorScheme.onSurface)
}

@Composable
private fun SettingsLink(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text("›", style = ProstutiTypography.bnTitleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

const val TAG_SETTINGS_NAME = "group_settings_name"
const val TAG_SETTINGS_PUBLIC = "group_settings_public"
const val TAG_SETTINGS_MAX = "group_settings_max"
const val TAG_SETTINGS_SAVE = "group_settings_save"
const val TAG_SETTINGS_LEAVE = "group_settings_leave"
const val TAG_SETTINGS_DELETE = "group_settings_delete"
