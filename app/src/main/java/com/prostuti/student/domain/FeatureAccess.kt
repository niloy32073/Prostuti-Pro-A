package com.prostuti.student.domain

import com.prostuti.student.data.model.response.MeData

/**
 * Which scoped features the signed-in user may use (GROUP_ROUTINE_PLAN.md §6.20), from `auth/me`'s
 * `scopes`. An empty list (RBAC not set up on the server) turns both off. Before the profile is known
 * both stay on: the server still answers `INSUFFICIENT_SCOPE`, which the screens show as a message.
 */
data class FeatureAccess(val group: Boolean = true, val routine: Boolean = true) {
    companion object {
        const val SCOPE_GROUP = "group:use"
        const val SCOPE_ROUTINE = "routine:use"

        fun of(me: MeData?): FeatureAccess =
            if (me == null) FeatureAccess() else FeatureAccess(group = SCOPE_GROUP in me.scopes, routine = SCOPE_ROUTINE in me.scopes)
    }
}
