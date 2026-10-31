package com.prostuti.student.ui.screen.examengine.calculator

/** Everything the calculator LCD draws, already formatted. */
data class LcdContent(
    val indicators: List<String>,
    val showUp: Boolean = false,
    val showDown: Boolean = false,
    val body: Body,
) {
    sealed interface Body {
        /** Input line on top, answer bottom-right. [cursor] is a character offset into [input]. */
        data class Calculation(
            val input: String,
            val cursor: Int? = null,
            val resultLabel: String? = null,
            val result: Value? = null,
            val footnote: String? = null,
        ) : Body

        data class Lines(val lines: List<String>) : Body

        /** EQN coefficient table; [entry] is the bottom line (typed value or the selected cell). */
        data class Coefficients(
            val headers: List<String>,
            val rowLabels: List<String>,
            val cells: List<List<String>>,
            val selectedRow: Int,
            val selectedColumn: Int,
            val entry: String,
            val entryCursor: Int?,
        ) : Body
    }

    sealed interface Value {
        data class Number(val mantissa: String, val exponent: Int?) : Value
        data class Fraction(val negative: Boolean, val numerator: Long, val denominator: Long) : Value
        data class Text(val text: String) : Value
    }
}
