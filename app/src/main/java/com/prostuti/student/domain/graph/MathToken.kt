package com.prostuti.student.domain.graph

internal enum class TokenType {
    NUMBER, IDENT,
    PLUS, MINUS, TIMES, DIVIDE, POWER, FACTORIAL, PRIME,
    COMMA, COLON, BAR, UNDERSCORE,
    OPEN_PAREN, CLOSE_PAREN, OPEN_BRACE, CLOSE_BRACE,
    EQ, LT, LE, GT, GE,
    INTEGRAL, SUM, PRODUCT,
}

/** [end] is exclusive. Two tokens are "adjacent" (dx, d/dx) when one ends where the next starts. */
internal data class MathToken(
    val type: TokenType,
    val start: Int,
    val end: Int,
    val text: String = "",
    val number: Double = 0.0,
)
