package com.prostuti.student.data.model.enums

import kotlinx.serialization.Serializable


/** A day of the week a routine holds exams on (`MON` … `SUN`). */
@Serializable
enum class Weekday {
    MON, TUE, WED, THU, FRI, SAT, SUN
}
