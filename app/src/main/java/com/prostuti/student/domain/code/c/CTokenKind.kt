package com.prostuti.student.domain.code.c

/** [DIRECTIVE] is a whole preprocessor line (only when the lexer is asked to keep them). */
enum class CTokenKind { IDENT, NUMBER, STRING, CHAR, PUNCT, DIRECTIVE }
