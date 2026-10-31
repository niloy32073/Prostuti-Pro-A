package com.prostuti.student.ui.screen.examengine.calculator

/** CALC / SOLVE asking for each variable's value in turn ("A?", then "X?"…). */
data class VariablePrompt(
    val purpose: PromptPurpose,
    val expression: List<String>,
    val variables: List<String>,
    val index: Int = 0,
) {
    val current: String get() = variables[index]
    val isLast: Boolean get() = index == variables.lastIndex

    /** SOLVE's last prompt is the starting guess for the unknown. */
    val isSolveTarget: Boolean get() = purpose == PromptPurpose.SOLVE && isLast
}
