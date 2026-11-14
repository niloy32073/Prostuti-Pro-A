package com.prostuti.student.domain.code.flow

enum class JumpKind {
    BREAK,
    CONTINUE,
    RETURN,
    /** Python `raise`: leaves the function like a return. */
    RAISE,
}
