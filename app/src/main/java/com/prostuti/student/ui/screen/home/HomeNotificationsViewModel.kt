package com.prostuti.student.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.domain.notification.FetchNotificationsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Home's bell dot: on when there's an unread notification, from one 1-item page of unread ones
 * (`total_items`, or the page itself). A failure keeps the last answer. B8 would make this one call.
 */
class HomeNotificationsViewModel(private val fetchNotifications: FetchNotificationsUseCase) : ViewModel() {
    private val _hasUnread = MutableStateFlow(false)
    val hasUnread: StateFlow<Boolean> = _hasUnread.asStateFlow()

    private var job: Job? = null

    fun refresh() {
        job?.cancel()
        job = viewModelScope.launch {
            when (val r = fetchNotifications(unreadOnly = true, page = 1, limit = 1)) {
                is ApiResponse.Success -> _hasUnread.value = (r.metaData?.totalItems ?: r.data.size) > 0
                is ApiResponse.Failure -> Timber.w("Unread notifications lookup failed: %s", r.errorCode)
            }
        }
    }
}
