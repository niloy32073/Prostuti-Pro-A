package com.prostuti.student.data.repository

import com.prostuti.student.dataStore.PrefKeys
import kotlinx.coroutines.flow.first

/** Kept in the app's DataStore. */
class WidgetPromoRepositoryImpl(private val storage: StorageRepository) : WidgetPromoRepository {

    override suspend fun dismissals() = WidgetPromoDismissals(
        count = storage.getLong(PrefKeys.WIDGET_PROMO_DISMISS_COUNT).first()?.toInt() ?: 0,
        lastAtMillis = storage.getLong(PrefKeys.WIDGET_PROMO_DISMISSED_AT).first() ?: 0L,
    )

    override suspend fun recordDismissal(atMillis: Long) {
        storage.putLong(PrefKeys.WIDGET_PROMO_DISMISS_COUNT, dismissals().count + 1L)
        storage.putLong(PrefKeys.WIDGET_PROMO_DISMISSED_AT, atMillis)
    }
}
