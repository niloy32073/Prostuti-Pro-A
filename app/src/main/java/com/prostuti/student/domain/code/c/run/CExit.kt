package com.prostuti.student.domain.code.c.run

/** Thrown by exit(): ends the program normally with [code]. */
internal class CExit(val code: Long) : RuntimeException(null, null, false, false)
