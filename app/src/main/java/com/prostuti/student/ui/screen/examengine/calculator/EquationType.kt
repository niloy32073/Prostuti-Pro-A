package com.prostuti.student.ui.screen.examengine.calculator

/** EQN-mode equation kinds; the coefficient table is [rows] × [columns]. */
enum class EquationType(val menuLabel: String, val rows: Int, val columns: Int, val isLinear: Boolean) {
    LINEAR_2("anX+bnY=cn", 2, 3, true),
    LINEAR_3("anX+bnY+cnZ=dn", 3, 4, true),
    LINEAR_4("anX+bnY+cnZ+dnT=en", 4, 5, true),
    QUADRATIC("aX²+bX+c=0", 1, 3, false),
    CUBIC("aX³+bX²+cX+d=0", 1, 4, false),
    QUARTIC("aX⁴+bX³+cX²+dX+e=0", 1, 5, false);

    val columnLabels: List<String> get() = listOf("a", "b", "c", "d", "e").take(columns)
}
