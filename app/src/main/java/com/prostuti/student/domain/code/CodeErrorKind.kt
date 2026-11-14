package com.prostuti.student.domain.code

/** Why a program could not be read; the UI turns each into a sentence. */
enum class CodeErrorKind {
    /** An opening bracket ([CodeError.detail]) is never closed. */
    UNCLOSED_BRACKET,
    /** A closing bracket ([CodeError.detail]) with no matching opening one. */
    UNEXPECTED_CLOSE,
    UNTERMINATED_STRING,
    UNTERMINATED_COMMENT,
    MISSING_SEMICOLON,
    /** Something ([CodeError.detail]) was expected here, such as `:` or `(`. */
    EXPECTED,
    /** Python: a line is indented more than the block it belongs to. */
    UNEXPECTED_INDENT,
    /** Python: a line goes back to an indent no outer block uses. */
    BAD_DEDENT,
    /** Python: a header ending in `:` has no indented block under it. */
    EXPECTED_BLOCK,
    /** A keyword ([CodeError.detail]) where it cannot be, such as `else` without `if` or `break` outside a loop. */
    MISPLACED,
}
