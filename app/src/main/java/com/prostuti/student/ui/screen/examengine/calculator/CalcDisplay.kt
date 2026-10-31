package com.prostuti.student.ui.screen.examengine.calculator

import com.prostuti.student.domain.calculator.CalcErrorKind

/** What the LCD is showing right now. */
sealed interface CalcDisplay {

    /** Typing: the COMP expression, a CALC/SOLVE value, or an EQN coefficient. */
    data object Input : CalcDisplay

    data class Result(
        val expression: List<String>,
        val value: Double,
        val label: String? = null,          // "X=" after SOLVE, "A=" after RCL
        val suffix: String = "",            // "→A" after STO, "M+" after M+
        val asDecimal: Boolean = false,     // S⇔D
        val engineeringStep: Int? = null,   // ENG / ←ENG
        val sexagesimal: Boolean = false,   // °'" pressed on a result
        val residual: Double? = null,       // SOLVE: L−R
    ) : CalcDisplay

    data class Error(val kind: CalcErrorKind, val position: Int? = null) : CalcDisplay

    data class Menu(val menu: CalcMenu) : CalcDisplay

    data class ConfirmClear(val target: ClearTarget) : CalcDisplay

    data class Message(val text: String) : CalcDisplay
}
