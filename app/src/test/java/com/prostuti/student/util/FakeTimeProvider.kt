package com.prostuti.student.util

/** Configurable fake of [TimeProvider] — lets ViewModel tests pin "now" instead of depending on
 * the wall clock, so window-state derivation (UPCOMING/OPEN/CLOSED) is deterministic. */
class FakeTimeProvider(var now: Long = 0L) : TimeProvider {
    override fun nowEpochMillis(): Long = now
}
