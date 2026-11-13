package com.prostuti.student.navigation

import android.net.Uri

/**
 * A place in the app that something outside it (the home screen widget) can open. Carried as the
 * launch intent's data, `prostuti://open/<path>`; [RootNavDisplay] builds the back stack for it.
 */
sealed interface AppLink {
    /** Just bring the app up (signed out: it opens on sign-in by itself). */
    data object Home : AppLink
    data object LiveExams : AppLink
    data class LiveExam(val examId: String) : AppLink
    data class GroupCalendar(val groupId: String) : AppLink
    data object RoutineTab : AppLink
    data object CreateRoutine : AppLink
    data object LeaderboardTab : AppLink

    val uri: String
        get() = "$SCHEME://$HOST/" + when (this) {
            Home -> PATH_HOME
            LiveExams -> PATH_LIVE
            is LiveExam -> "$PATH_LIVE/${Uri.encode(examId)}"
            is GroupCalendar -> "$PATH_GROUP/${Uri.encode(groupId)}"
            RoutineTab -> PATH_ROUTINE
            CreateRoutine -> "$PATH_ROUTINE/$PATH_CREATE"
            LeaderboardTab -> PATH_LEADERBOARD
        }

    companion object {
        const val SCHEME = "prostuti"
        const val HOST = "open"
        private const val PATH_HOME = "home"
        private const val PATH_LIVE = "live"
        private const val PATH_GROUP = "group"
        private const val PATH_ROUTINE = "routine"
        private const val PATH_CREATE = "create"
        private const val PATH_LEADERBOARD = "leaderboard"

        /** Null for anything that isn't one of ours. */
        fun parse(uri: Uri?): AppLink? {
            if (uri == null || uri.scheme != SCHEME || uri.host != HOST) return null
            val path = uri.pathSegments
            return when (path.getOrNull(0)) {
                PATH_HOME -> Home
                PATH_LIVE -> path.getOrNull(1)?.let { LiveExam(it) } ?: LiveExams
                PATH_GROUP -> path.getOrNull(1)?.let { GroupCalendar(it) }
                PATH_ROUTINE -> if (path.getOrNull(1) == PATH_CREATE) CreateRoutine else RoutineTab
                PATH_LEADERBOARD -> LeaderboardTab
                else -> null
            }
        }
    }
}
