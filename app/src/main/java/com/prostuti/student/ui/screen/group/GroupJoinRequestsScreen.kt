package com.prostuti.student.ui.screen.group

import androidx.compose.foundation.background
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
import com.prostuti.student.ui.component.ProstutiSpinner
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostuti.student.R
import com.prostuti.student.data.model.enums.JoinRequestStatus
import com.prostuti.student.data.model.response.GroupJoinRequest
import com.prostuti.student.ui.component.BackButton
import com.prostuti.student.ui.component.PrimaryButton
import com.prostuti.student.ui.component.PullToRefreshErrorOverlayWrapper
import com.prostuti.student.ui.component.adaptiveColumnCount
import com.prostuti.student.ui.screen.group.component.groupDateTimeText
import com.prostuti.student.ui.theme.ProstutiTypography
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun GroupJoinRequestsScreen(
    groupId: String,
    onBack: () -> Unit,
    viewModel: GroupJoinRequestsViewModel = koinViewModel(parameters = { parametersOf(groupId) })
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
    LaunchedEffect(state.isNotAdmin) {
        if (state.isNotAdmin) { viewModel.consumeNotAdmin(); onBack() }
    }

    GroupJoinRequestsContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onSelectStatus = viewModel::selectStatus,
        onApprove = viewModel::approve,
        onReject = viewModel::reject,
        onRefresh = viewModel::refresh,
        onClearError = viewModel::clearError,
    )
}

private val TABS = listOf(
    JoinRequestStatus.PENDING to R.string.group_requests_pending,
    JoinRequestStatus.APPROVED to R.string.group_requests_approved,
    JoinRequestStatus.REJECTED to R.string.group_requests_rejected,
)

@Composable
fun GroupJoinRequestsContent(
    state: GroupJoinRequestsUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBack: () -> Unit,
    onSelectStatus: (JoinRequestStatus) -> Unit,
    onApprove: (GroupJoinRequest) -> Unit,
    onReject: (GroupJoinRequest) -> Unit,
    onRefresh: () -> Unit,
    onClearError: (String) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        PullToRefreshErrorOverlayWrapper(
            errors = state.errors,
            errorCleaner = onClearError,
            overlayPadding = padding,
            pullToRefreshEnabled = true,
            isRefreshing = state.isLoading && state.requests.isNotEmpty(),
            onRefresh = onRefresh
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp, start = 16.dp, end = 16.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BackButton(onClick = onBack)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.group_join_requests_title), style = ProstutiTypography.bnTitleLarge, color = MaterialTheme.colorScheme.onSurface)
                }
                PrimaryTabRow(selectedTabIndex = TABS.indexOfFirst { it.first == state.status }) {
                    TABS.forEach { (status, label) ->
                        Tab(
                            selected = status == state.status,
                            onClick = { onSelectStatus(status) },
                            text = { Text(stringResource(label), style = ProstutiTypography.bnLabelLarge) }
                        )
                    }
                }

                if (state.isLoading && state.requests.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ProstutiSpinner()
                    }
                } else {
                    val columns = adaptiveColumnCount()
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        state = rememberLazyGridState(),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(if (columns > 1) 8.dp else 0.dp)
                    ) {
                        if (state.requests.isEmpty() && state.errors.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text(
                                    stringResource(R.string.group_requests_empty), style = ProstutiTypography.bnBodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 24.dp)
                                )
                            }
                        }
                        items(state.requests, key = { it.id }) { request ->
                            RequestRow(
                                request = request,
                                isBusy = state.busyId == request.id,
                                enabled = state.busyId == null,
                                onApprove = { onApprove(request) },
                                onReject = { onReject(request) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestRow(request: GroupJoinRequest, isBusy: Boolean, enabled: Boolean, onApprove: () -> Unit, onReject: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            request.username ?: stringResource(R.string.group_request_unknown_user),
            style = ProstutiTypography.bnBodyLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Text(
            stringResource(R.string.group_request_sent_at, groupDateTimeText(request.createdAt)),
            style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (request.status == JoinRequestStatus.PENDING) {
            if (isBusy) {
                ProstutiSpinner(modifier = Modifier.size(24.dp))
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryButton(
                        text = stringResource(R.string.group_request_approve), onClick = onApprove, enabled = enabled,
                        modifier = Modifier.weight(1f).testTag("$TAG_APPROVE_PREFIX${request.id}")
                    )
                    OutlinedButton(onClick = onReject, enabled = enabled, modifier = Modifier.weight(1f).testTag("$TAG_REJECT_PREFIX${request.id}")) {
                        Text(stringResource(R.string.group_request_reject), style = ProstutiTypography.bnLabelLarge)
                    }
                }
            }
        } else {
            request.decidedAt?.let {
                Text(
                    stringResource(R.string.group_request_decided_at, groupDateTimeText(it)),
                    style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

const val TAG_APPROVE_PREFIX = "group_request_approve_"
const val TAG_REJECT_PREFIX = "group_request_reject_"
