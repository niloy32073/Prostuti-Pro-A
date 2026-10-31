package com.prostuti.student.domain.calculator

/** The error screens the calculator can show. */
enum class CalcErrorKind(val label: String) {
    MATH("Math ERROR"),
    SYNTAX("Syntax ERROR"),
    ARGUMENT("Argument ERROR"),
    CANT_SOLVE("Can't Solve"),
    NO_SOLUTION("No Solution"),
    INFINITE_SOLUTION("Infinite Solution"),
}
