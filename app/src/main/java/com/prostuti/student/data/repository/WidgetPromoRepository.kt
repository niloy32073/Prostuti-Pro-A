package com.prostuti.student.data.repository

/** How often the "add the widget" card on Home was closed, and when last (0 = never). */
data class WidgetPromoDismissals(val count: Int = 0, val lastAtMillis: Long = 0L)

interface WidgetPromoRepository {
    suspend fun dismissals(): WidgetPromoDismissals
    suspend fun recordDismissal(atMillis: Long)
}
