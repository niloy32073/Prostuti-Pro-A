package com.prostuti.student.domain.widget

import com.prostuti.student.data.repository.WidgetPromoRepository
import java.time.Instant

/** The user closed the "add the widget" card (see [ShouldShowWidgetPromoUseCase]). */
class DismissWidgetPromoUseCase(private val promo: WidgetPromoRepository) {
    suspend operator fun invoke(now: Instant) = promo.recordDismissal(now.toEpochMilli())
}
