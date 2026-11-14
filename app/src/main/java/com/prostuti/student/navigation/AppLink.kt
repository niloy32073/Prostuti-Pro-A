package com.prostuti.student.navigation

import android.net.Uri

/**
 * A place in the app that something outside it can open (DEEP_LINKS.md). Two forms, same paths:
 * - `prostuti://open/<path>` — the app's own scheme (home screen widget, notifications);
 * - `https://prostuti.com.bd/app/<path>` — a verified App Link from the website (or a shared link):
 *   only the `/app/` part of the site belongs to the app, so the rest of the website stays in the browser.
 * [OpenAppLinks] opens it in its tab with a real back stack. Links only navigate; they never act.
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
        /** The website's App Link paths: https://prostuti.com.bd/app/<path> (and www.). */
        const val WEB_HOST = "prostuti.com.bd"
        const val WEB_PATH_PREFIX = "app"
        private const val PATH_HOME = "home"
        private const val PATH_LIVE = "live"
        private const val PATH_GROUP = "group"
        private const val PATH_ROUTINE = "routine"
        private const val PATH_CREATE = "create"
        private const val PATH_LEADERBOARD = "leaderboard"

        /** Null for anything that isn't one of ours. */
        fun parse(uri: Uri?): AppLink? = uri?.let { parse(it.scheme, it.host, it.pathSegments) }

        /** [parse] without android.net.Uri, so plain unit tests can run it. */
        fun parse(scheme: String?, host: String?, segments: List<String>): AppLink? {
            val path = when {
                scheme == SCHEME && host == HOST -> segments
                scheme == "https" && (host.equals(WEB_HOST, ignoreCase = true) || host.equals("www.$WEB_HOST", ignoreCase = true)) &&
                    segments.firstOrNull() == WEB_PATH_PREFIX -> segments.drop(1)
                else -> return null
            }
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
