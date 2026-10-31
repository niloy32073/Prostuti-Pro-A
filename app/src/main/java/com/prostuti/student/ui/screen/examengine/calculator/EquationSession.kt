package com.prostuti.student.ui.screen.examengine.calculator

/** EQN mode: the coefficient table being filled in, and the answers once solved. */
data class EquationSession(
    val type: EquationType,
    val coefficients: List<List<Double>> = List(type.rows) { List(type.columns) { 0.0 } },
    val row: Int = 0,
    val column: Int = 0,
    val roots: List<EquationRoot>? = null,
    val rootIndex: Int = 0,
    val rootsAsDecimal: Boolean = false,
) {
    val selectedValue: Double get() = coefficients[row][column]

    fun withValue(value: Double): EquationSession = copy(
        coefficients = coefficients.mapIndexed { r, cells ->
            if (r != row) cells else cells.mapIndexed { c, old -> if (c == column) value else old }
        }
    )

    /** Next cell to the right, then the next row; stays on the last cell. */
    fun advance(): EquationSession = when {
        column < type.columns - 1 -> copy(column = column + 1)
        row < type.rows - 1 -> copy(row = row + 1, column = 0)
        else -> this
    }

    fun move(dRow: Int, dColumn: Int): EquationSession = copy(
        row = (row + dRow).coerceIn(0, type.rows - 1),
        column = (column + dColumn).coerceIn(0, type.columns - 1),
    )
}
