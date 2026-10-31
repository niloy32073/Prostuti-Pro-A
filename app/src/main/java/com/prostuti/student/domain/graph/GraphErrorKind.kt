package com.prostuti.student.domain.graph

/** Why a line can't be drawn. The UI maps each kind to a translated message. */
enum class GraphErrorKind {
    SYNTAX,
    UNBALANCED_BRACKETS,
    UNKNOWN_CHARACTER,
    EMPTY_ARGUMENT,
    WRONG_ARGUMENT_COUNT,
    MISSING_INTEGRAL_DIFFERENTIAL,
    TUPLE_NOT_ALLOWED,

    /** Uses a letter nothing defines — the UI offers to add a slider for it. */
    UNDEFINED_VARIABLE,
    DUPLICATE_DEFINITION,
    CIRCULAR_DEFINITION,

    /** Uses a parameter or function whose own line has an error. */
    BROKEN_DEPENDENCY,
    INVALID_DEFINITION,
    RESERVED_NAME,
    UNSUPPORTED_EQUATION,

    /** 3D input (z, or a 3-item point) — drawn by the 3D view in the next phase. */
    NEEDS_3D,
    TOO_COMPLEX,
}
