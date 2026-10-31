package com.prostuti.student.domain.graph

/**
 * Parsed math expression tree for the graph builder. Pure data: it is simplified, differentiated
 * and turned into LaTeX as a tree, then compiled once into fast evaluators ([ExpressionCompiler]).
 */
sealed interface MathNode {
    data class Num(val value: Double) : MathNode

    /** A variable, parameter or constant name: x, y, t, θ, a, b_1, π, e. */
    data class Sym(val name: String) : MathNode

    data class Neg(val operand: MathNode) : MathNode

    data class Bin(val op: BinOp, val left: MathNode, val right: MathNode) : MathNode

    /** Built-in function (sin, ln, abs…) or user function (f, g). [primes] = f', f''. */
    data class Call(val name: String, val args: List<MathNode>, val primes: Int = 0) : MathNode

    /** d/dvar (body). */
    data class Diff(val variable: String, val body: MathNode) : MathNode

    /** Definite integral of [body] over [variable] from [lower] to [upper]. */
    data class Integral(val variable: String, val lower: MathNode, val upper: MathNode, val body: MathNode) : MathNode

    /** Σ (or Π when [product]) of [body] for [variable] = from..to. */
    data class Series(val variable: String, val from: MathNode, val to: MathNode, val body: MathNode, val product: Boolean) : MathNode

    /** `{c1: v1, c2: v2, otherwise}`. A missing otherwise is undefined. `{c}` alone is 1 where c holds. */
    data class Piecewise(val branches: List<Branch>, val otherwise: MathNode?) : MathNode

    /** `(a, b)` or `(a, b, c)` — only valid as a whole statement side (points, parametric curves). */
    data class Tuple(val items: List<MathNode>) : MathNode

    /** Numeric d/dvar used when a symbolic rule does not exist (gamma, factorial, sums with x-dependent bounds). */
    data class NumericDiff(val variable: String, val body: MathNode) : MathNode

    /**
     * `body` with [name] bound to [value]. A user-function call f(e) becomes Let(x#1, e, body of f),
     * so the argument is evaluated once and no substitution can capture an integral's variable.
     * Created by [GraphCompiler] only — never by the parser.
     */
    data class Let(val name: String, val value: MathNode, val body: MathNode) : MathNode

    data class Branch(val condition: Condition, val value: MathNode)

    /** A comparison chain: `0 < x < 2` is operands [0, x, 2] with relations [<, <]. */
    data class Condition(val operands: List<MathNode>, val relations: List<Relation>)
}
