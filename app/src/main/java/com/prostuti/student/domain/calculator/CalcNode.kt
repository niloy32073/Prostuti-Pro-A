package com.prostuti.student.domain.calculator

/** Parsed expression tree. Kept as a tree (not a number) so ∫, d/dx, Σ and SOLVE can re-run it. */
internal sealed interface CalcNode {
    data class Num(val value: Double) : CalcNode
    data class Var(val name: String) : CalcNode
    data object Ans : CalcNode
    data object Random : CalcNode
    data class Negate(val operand: CalcNode) : CalcNode
    data class Binary(val op: String, val left: CalcNode, val right: CalcNode) : CalcNode
    data class Postfix(val op: String, val operand: CalcNode) : CalcNode
    data class Call(val function: String, val args: List<CalcNode>) : CalcNode
    data class Equation(val left: CalcNode, val right: CalcNode) : CalcNode
}
