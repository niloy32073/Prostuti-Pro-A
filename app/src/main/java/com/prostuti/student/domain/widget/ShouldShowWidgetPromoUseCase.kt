package com.prostuti.student.domain.widget

import com.prostuti.student.data.repository.WidgetPromoRepository
import java.time.Duration
import java.time.Instant

/**
 * Whether Home shows the "add the widget" card: only while no widget is placed and the launcher
 * can add one for us. Closing it hides it for [SNOOZE]; after [MAX_DISMISSALS] closes it's gone.
 */
class ShouldShowWidgetPromoUseCase(
    private val host: HomeWidgetHost,
    private val promo: WidgetPromoRepository,
) {
    suspend operator fun invoke(now: Instant): Boolean {
        if (!host.canPin() || host.isPlaced()) return false
        val dismissals = promo.dismissals()
        if (dismissals.count >= MAX_DISMISSALS) return false
        return dismissals.lastAtMillis == 0L || now.toEpochMilli() - dismissals.lastAtMillis >= SNOOZE.toMillis()
    }

    companion object {
        val SNOOZE: Duration = Duration.ofDays(7)
        const val MAX_DISMISSALS = 5
    }
}
