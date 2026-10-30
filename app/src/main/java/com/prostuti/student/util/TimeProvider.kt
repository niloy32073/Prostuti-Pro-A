package com.prostuti.student.util

/** Indirection around "now" so exam-window state derivation stays a pure, unit-testable
 * function instead of calling [System.currentTimeMillis] directly. */
interface TimeProvider {
    fun nowEpochMillis(): Long
}

class SystemTimeProvider : TimeProvider {
    override fun nowEpochMillis(): Long = System.currentTimeMillis()
}
