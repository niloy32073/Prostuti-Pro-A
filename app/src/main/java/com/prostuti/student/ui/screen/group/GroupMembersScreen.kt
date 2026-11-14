package com.prostuti.student.ui.screen.group

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import com.prostuti.student.ui.component.ProstutiSpinner
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostuti.student.R
import com.prostuti.student.data.model.enums.GroupRole
import com.prostuti.student.data.model.response.StudyGroupMember
import com.prostuti.student.ui.component.BackButton
import com.prostuti.student.ui.component.PullToRefreshErrorOverlayWrapper
import com.prostuti.student.ui.component.adaptiveColumnCount
import com.prostuti.student.ui.screen.group.component.GroupConfirmDialog
import com.prostuti.student.ui.theme.ProstutiTypography
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun GroupMembersScreen(
    groupId: String,
    onBack: () -> Unit,
    /** Admin's "add a member": the AddGroupMember sheet (it shares this screen's ViewModel). */
    onOpenAddMember: () -> Unit = {},
    viewModel: GroupMembersViewModel = koinViewModel(parameters = { parametersOf(groupId) })
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

    GroupMembersContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onQueryChange = viewModel::setQuery,
        onAddMember = onOpenAddMember,
        onMakeAdmin = viewModel::makeAdmin,
        onRemoveAdmin = viewModel::removeAdmin,
        onRemove = viewModel::remove,
        onRefresh = viewModel::refresh,
        onClearError = viewModel::clearError,
    )
}

@Composable
fun GroupMembersContent(
    state: GroupMembersUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onAddMember: () -> Unit,
    onMakeAdmin: (StudyGroupMember) -> Unit,
    onRemoveAdmin: (StudyGroupMember) -> Unit,
    onRemove: (StudyGroupMember) -> Unit,
    onRefresh: () -> Unit,
    onClearError: (String) -> Unit,
) {
    // userId of the member awaiting a confirm: removal, or an admin stepping down themselves.
    var confirmRemoveId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmSelfDemote by rememberSaveable { mutableStateOf(false) }

    state.members.firstOrNull { it.userId == confirmRemoveId }?.let { member ->
        GroupConfirmDialog(
            title = stringResource(R.string.group_remove_member_title),
            body = stringResource(R.string.group_remove_member_body, member.username),
            confirmLabel = stringResource(R.string.group_remove_member_confirm),
            destructive = true,
            onConfirm = { confirmRemoveId = null; onRemove(member) },
            onDismiss = { confirmRemoveId = null },
        )
    }
    if (confirmSelfDemote) {
        state.members.firstOrNull { it.userId == state.myUserId }?.let { me ->
            GroupConfirmDialog(
                title = stringResource(R.string.group_self_demote_title),
                body = stringResource(R.string.group_self_demote_body),
                confirmLabel = stringResource(R.string.group_remove_admin),
                onConfirm = { confirmSelfDemote = false; onRemoveAdmin(me) },
                onDismiss = { confirmSelfDemote = false },
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (state.isAdmin) {
                ExtendedFloatingActionButton(
                    onClick = onAddMember,
                    icon = { Icon(Icons.Filled.PersonAdd, contentDescription = null) },
                    text = { Text(stringResource(R.string.group_add_member_title), style = ProstutiTypography.bnLabelLarge) },
                    modifier = Modifier.testTag(TAG_ADD_MEMBER_FAB)
                )
            }
        }
    ) { padding ->
        PullToRefreshErrorOverlayWrapper(
            errors = state.errors,
            errorCleaner = onClearError,
            overlayPadding = padding,
            pullToRefreshEnabled = true,
            isRefreshing = state.isLoading && state.members.isNotEmpty(),
            onRefresh = onRefresh
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    BackButton(onClick = onBack)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = stringResource(R.string.group_members_title), style = ProstutiTypography.bnTitleMedium, color = MaterialTheme.colorScheme.onBackground)
                }

                OutlinedTextField(
                    value = state.query, onValueChange = onQueryChange,
                    placeholder = { Text(stringResource(R.string.group_members_search), style = ProstutiTypography.bnBodyMedium) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                )

                if (state.isLoading && state.members.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ProstutiSpinner()
                    }
                } else {
                    val columns = adaptiveColumnCount()
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        state = rememberLazyGridState(),
                        modifier = Modifier.fillMaxSize(),
                        // Bottom room so the FAB never covers the last row.
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(if (columns > 1) 8.dp else 0.dp)
                    ) {
                        if (state.filtered.isEmpty() && state.query.isNotBlank()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text(
                                    stringResource(R.string.group_members_no_match), style = ProstutiTypography.bnBodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 24.dp)
                                )
                            }
                        }
                        items(state.filtered, key = { it.userId }) { member ->
                            val isMe = member.userId == state.myUserId
                            MemberRow(member = member, isMe = isMe) {
                                if (state.isAdmin) {
                                    MemberActions(
                                        member = member,
                                        isMe = isMe,
                                        isBusy = state.busyUserId == member.userId,
                                        enabled = state.busyUserId == null,
                                        onMakeAdmin = { onMakeAdmin(member) },
                                        onRemoveAdmin = { if (isMe) confirmSelfDemote = true else onRemoveAdmin(member) },
                                        onRemove = { confirmRemoveId = member.userId },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The admin's ⋮ menu on one member row. An admin leaves via GroupSettings, so "remove" is not offered on their own row. */
@Composable
private fun MemberActions(
    member: StudyGroupMember,
    isMe: Boolean,
    isBusy: Boolean,
    enabled: Boolean,
    onMakeAdmin: () -> Unit,
    onRemoveAdmin: () -> Unit,
    onRemove: () -> Unit,
) {
    if (isBusy) {
        ProstutiSpinner(modifier = Modifier.padding(horizontal = 12.dp).size(20.dp))
        return
    }
    var open by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }, enabled = enabled, modifier = Modifier.testTag("$TAG_MEMBER_MENU_PREFIX${member.userId}")) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.group_member_actions_cd, member.username))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (member.role == GroupRole.ADMIN) {
                DropdownMenuItem(text = { Text(stringResource(R.string.group_remove_admin)) }, onClick = { open = false; onRemoveAdmin() })
            } else {
                DropdownMenuItem(text = { Text(stringResource(R.string.group_make_admin)) }, onClick = { open = false; onMakeAdmin() })
            }
            if (!isMe) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.group_remove_member_confirm), color = MaterialTheme.colorScheme.error) },
                    onClick = { open = false; onRemove() }
                )
            }
        }
    }
}

const val TAG_ADD_MEMBER_FAB = "group_add_member_fab"
const val TAG_MEMBER_MENU_PREFIX = "group_member_menu_"
