package com.prostuti.student.data.repository

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.MeData
import kotlinx.coroutines.flow.Flow

/** The signed-in user's own profile (`GET auth/me`), cached in Room. */
interface UserRepository {
    /**
     * Cache-first: fetches from the API when [refresh] is true or nothing is cached, then keeps
     * emitting the cached row. A failed fetch is emitted once; cached data (if any) follows it.
     */
    fun observeMe(refresh: Boolean): Flow<ApiResponse<MeData>>

    /** Drops the cached profile — called on logout so the next user never sees it. */
    suspend fun clearCachedMe()
}
