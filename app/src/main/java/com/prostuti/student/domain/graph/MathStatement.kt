package com.prostuti.student.domain.graph

/** One parsed expression-list line: a bare expression, or a chain like `y = x²` / `-1 < y < x`. */
data class MathStatement(val operands: List<MathNode>, val relations: List<Relation>) {
    init {
        require(operands.size == relations.size + 1)
    }

    val isBareExpression: Boolean get() = relations.isEmpty()
    val single: MathNode get() = operands.first()
}
