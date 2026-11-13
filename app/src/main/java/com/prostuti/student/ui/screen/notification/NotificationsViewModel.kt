package com.prostuti.student.ui.screen.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.data.model.enums.GroupRoutineErrorCode
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.NotificationItem
import com.prostuti.student.domain.notification.FetchNotificationsUseCase
import com.prostuti.student.domain.notification.MarkNotificationReadUseCase
import com.prostuti.student.domain.notification.NotificationTarget
import com.prostuti.student.util.ApiErrorMessages
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val unreadOnly: Boolean = false,
    val isLoading: Boolean = true,
    val items: List<NotificationItem> = emptyList(),
    val page: Int = 0,
    val canLoadMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    /** One-shot: open where the tapped notification points. */
    val target: NotificationTarget? = null,
    val errors: Map<String, String> = emptyMap(),
)

/**
 * The in-app inbox (GROUP_ROUTINE_PLAN.md §6.18, D4): newest first, paged, "all" or "unread". A tap
 * marks it read at once (the server call follows) and opens its `target_url` when the app knows it.
 */
class NotificationsViewModel(
    private val fetchNotifications: FetchNotificationsUseCase,
    private val markNotificationRead: MarkNotificationReadUseCase,
    private val errorMessages: ApiErrorMessages,
) : ViewModel() {
    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init { load(reset = true) }

    fun refresh() = load(reset = true)

    fun setUnreadOnly(unreadOnly: Boolean) {
        if (unreadOnly == _state.value.unreadOnly) return
        _state.update { it.copy(unreadOnly = unreadOnly, items = emptyList()) }
        load(reset = true)
    }

    fun loadMore() {
        val s = _state.value
        if (s.canLoadMore && !s.isLoading && !s.isLoadingMore) load(reset = false)
    }

    private fun load(reset: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val page = if (reset) 1 else _state.value.page + 1
            _state.update { if (reset) it.copy(isLoading = true) else it.copy(isLoadingMore = true) }
            when (val r = fetchNotifications(unreadOnly = _state.value.unreadOnly, page = page, limit = PAGE_LIMIT)) {
                is ApiResponse.Success -> _state.update {
                    val totalPages = r.metaData?.totalPages
                    it.copy(
                        isLoading = false, isLoadingMore = false, page = page,
                        items = if (reset) r.data else (it.items + r.data).distinctBy { n -> n.id },
                        canLoadMore = if (totalPages != null) page < totalPages else r.data.size == PAGE_LIMIT,
                        errors = it.errors - ERR_LOAD,
                    )
                }
                is ApiResponse.Failure -> _state.update {
                    it.copy(isLoading = false, isLoadingMore = false, errors = it.errors + (ERR_LOAD to errorMessages.messageFor(r)))
                }
            }
        }
    }

    fun open(item: NotificationItem) {
        _state.update { it.copy(target = NotificationTarget.parse(item.targetUrl)) }
        if (item.isRead) return
        setRead(item.id, true)
        viewModelScope.launch {
            val r = markNotificationRead(item.id)
            // Gone on the server (expired or not mine): nothing left to mark.
            if (r is ApiResponse.Failure && r.errorCode != GroupRoutineErrorCode.NOTIFICATION_NOT_FOUND) {
                setRead(item.id, false)
                _state.update { it.copy(errors = it.errors + (ERR_READ to errorMessages.messageFor(r))) }
            }
        }
    }

    private fun setRead(id: String, read: Boolean) = _state.update { s ->
        s.copy(items = s.items.map { if (it.id == id) it.copy(isRead = read) else it })
    }

    fun consumeTarget() = _state.update { it.copy(target = null) }
    fun clearError(tag: String) = _state.update { it.copy(errors = it.errors - tag) }

    private companion object {
        const val ERR_LOAD = "LOAD"
        const val ERR_READ = "READ"
        const val PAGE_LIMIT = 20
    }
}
