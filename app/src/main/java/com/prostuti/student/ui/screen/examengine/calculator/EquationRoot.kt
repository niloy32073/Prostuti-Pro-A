package com.prostuti.student.ui.screen.examengine.calculator

import com.prostuti.student.domain.calculator.Complex

/** One EQN answer line, e.g. "X1=" 2 or "X3=" -0.5+0.866i. */
data class EquationRoot(val label: String, val value: Complex)
