package com.prostuti.student.ui.screen.examengine.calculator

import com.prostuti.student.domain.calculator.AngleUnit
import com.prostuti.student.domain.calculator.CalcNumberFormat
import com.prostuti.student.domain.calculator.CalcToken

data class CalculatorUiState(
    val mode: CalcMode = CalcMode.COMP,
    val shift: Boolean = false,
    val alpha: Boolean = false,
    val hyp: Boolean = false,
    val memoryCommand: MemoryCommand? = null,
    val angleUnit: AngleUnit = AngleUnit.DEG,
    val numberFormat: CalcNumberFormat = CalcNumberFormat.Norm(1),
    val editor: ExpressionEditor = ExpressionEditor(),
    val display: CalcDisplay = CalcDisplay.Input,
    val prompt: VariablePrompt? = null,
    val equation: EquationSession? = null,
    val variables: Map<String, Double> = CalcToken.VARIABLES.associateWith { 0.0 },
    val ans: Double = 0.0,
    val history: List<CalcHistoryEntry> = emptyList(),
    /** Position while browsing history with ▲ (0 = newest), -1 when not browsing. */
    val historyIndex: Int = -1,
) {
    val memoryInUse: Boolean get() = (variables["M"] ?: 0.0) != 0.0
}
