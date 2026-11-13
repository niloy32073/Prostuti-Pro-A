package com.prostuti.student.data.model.widget

import kotlinx.serialization.Serializable

/**
 * What the home screen widget last loaded, stored so it can draw without the network (offline,
 * or between refreshes). Titles are already text in the app language; times stay as instants so
 * "running now / today / tomorrow" is worked out when the widget draws.
 */
@Serializable
data class WidgetSnapshot(
    val signedIn: Boolean = false,
    /** When the shown data last came from the server (shown as "updated …"). */
    val updatedAtMillis: Long = 0,
    /** When a refresh was last tried, worked or not; keeps a failing refresh from retrying on every redraw. */
    val checkedAtMillis: Long = 0,
    /** Null = never loaded. */
    val exams: List<WidgetExamItem>? = null,
    /** The account can't use routines (no `routine:use` scope). */
    val routineOff: Boolean = false,
    /** Null = never loaded. */
    val today: List<WidgetExamItem>? = null,
    val hasPersonalRoutine: Boolean = false,
)

/** Where an exam comes from; picks its icon. */
@Serializable
enum class WidgetExamKind { LIVE, GROUP, PERSONAL }

@Serializable
data class WidgetExamItem(
    val id: String,
    val kind: WidgetExamKind = WidgetExamKind.LIVE,
    val title: String,
    /** "Live exam", the group's name, or "Personal routine". */
    val source: String,
    val startsAtMillis: Long,
    val endsAtMillis: Long,
    /** Already taken (today's list keeps finished exams, marked as done). */
    val done: Boolean = false,
    /** Where a tap goes: an [com.prostuti.student.navigation.AppLink] as a URI string. */
    val link: String,
)
