package com.prostuti.student.domain.code.run

import kotlinx.serialization.Serializable

/**
 * One variable (or an element/member of one) at a paused step. [value] is how it reads (`5`,
 * `3.5`, `"Rina"`, `{1, 2, 3}`, `→ a[2]`); null when it has not been given a value yet.
 * [children] are the elements of an array/list or the members of a struct/object.
 */
@Serializable
data class TraceVar(val name: String, val value: String?, val children: List<TraceVar> = emptyList())
