package com.prostuti.student.ui.screen.routine.builder

import kotlinx.serialization.Serializable

/** Whose routine or exam the builder makes: my personal one, or a group's (admin only). */
@Serializable
sealed interface RoutineOwner {
    @Serializable data object Personal : RoutineOwner
    @Serializable data class Group(val groupId: String) : RoutineOwner
}

/** ROUTINE = a repeating routine (4 steps, with preview). ADHOC = one group exam on one day (3 steps). */
enum class RoutineBuilderMode { ROUTINE, ADHOC }

/** The builder's steps, in order. ADHOC stops at [CONFIG]. */
enum class BuilderStep { SUBJECTS, CHAPTERS, CONFIG, PREVIEW }

val RoutineOwner.groupIdOrNull: String? get() = (this as? RoutineOwner.Group)?.groupId
