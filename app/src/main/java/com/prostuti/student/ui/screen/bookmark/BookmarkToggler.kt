package com.prostuti.student.ui.screen.bookmark

import com.prostuti.student.data.model.enums.BookmarkResourceType
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.domain.AddBookmarkUseCase
import com.prostuti.student.domain.ObserveBookmarkedIdsUseCase
import com.prostuti.student.domain.RemoveBookmarkUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The bookmark icon on cards of one [type], shared by every screen that lists them. Filled state
 * comes from Room, so a resource bookmarked anywhere shows filled everywhere. A tap shows at once;
 * the add/remove call runs behind it and the icon flips back if it fails. Create one per ViewModel
 * with its `viewModelScope`; [onChange] gets the ids to draw filled, [onError] a message to show.
 */
class BookmarkToggler(
    private val scope: CoroutineScope,
    private val type: BookmarkResourceType,
    observeBookmarkedIds: ObserveBookmarkedIdsUseCase,
    private val addBookmark: AddBookmarkUseCase,
    private val removeBookmark: RemoveBookmarkUseCase,
    private val onChange: (Set<String>) -> Unit,
    private val onError: (String) -> Unit
) {
    private var saved: Set<String> = emptySet()

    /** resourceId -> wanted state, while its call runs. A second tap on it is ignored until then. */
    private val pending = mutableMapOf<String, Boolean>()

    init {
        scope.launch {
            observeBookmarkedIds(type).collect { ids ->
                saved = ids
                publish()
            }
        }
    }

    fun toggle(resourceId: String) {
        if (resourceId in pending) return
        val bookmark = resourceId !in current()
        pending[resourceId] = bookmark
        publish()
        scope.launch {
            val result = if (bookmark) addBookmark(type, resourceId) else removeBookmark(type, resourceId)
            pending.remove(resourceId)
            // Apply it now rather than wait for Room's re-emit, so the icon doesn't flicker back.
            if (result is ApiResponse.Success) saved = if (bookmark) saved + resourceId else saved - resourceId
            publish()
            if (result is ApiResponse.Failure) onError(result.message)
        }
    }

    private fun current(): Set<String> =
        saved + pending.filterValues { it }.keys - pending.filterValues { !it }.keys

    private fun publish() = onChange(current())
}
