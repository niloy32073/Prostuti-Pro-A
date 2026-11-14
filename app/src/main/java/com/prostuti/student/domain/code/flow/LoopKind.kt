package com.prostuti.student.domain.code.flow

enum class LoopKind {
    /** Condition first (`while`, and C `for` with its init and update steps). */
    WHILE,
    /** Python `for x in items`: the diamond asks whether there is a next item. */
    FOR_EACH,
    /** Body first, condition after (C `do … while`). */
    DO_WHILE,
}
