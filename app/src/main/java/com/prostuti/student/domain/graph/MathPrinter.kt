package com.prostuti.student.domain.graph

/**
 * Prints a parsed tree back as text the parser reads again (parse(print(n)) has the same meaning).
 * Used when the app writes a line for the student, e.g. the 3D solid from a 2D integral.
 */
object MathPrinter {

    fun print(node: MathNode): String = p(node, 0)

    private const val ADD = 1
    private const val MUL = 2
    private const val UNARY = 3
    private const val POW = 4
    private const val ATOM = 5

    private fun prec(node: MathNode): Int = when (node) {
        is MathNode.Num -> if (node.value < 0) UNARY else ATOM
        is MathNode.Sym, is MathNode.Call, is MathNode.Tuple, is MathNode.Piecewise -> ATOM
        is MathNode.Neg -> UNARY
        is MathNode.Bin -> when (node.op) {
            BinOp.ADD, BinOp.SUB -> ADD
            BinOp.MUL, BinOp.DIV -> MUL
            BinOp.POW -> POW
        }
        // ∫, Σ and d/dx swallow everything to their right, so they are bracketed unless alone.
        is MathNode.Integral, is MathNode.Series, is MathNode.Diff, is MathNode.NumericDiff, is MathNode.Let -> ADD
    }

    private fun p(node: MathNode, context: Int): String {
        val text = raw(node)
        return if (prec(node) < context) "($text)" else text
    }

    private fun raw(node: MathNode): String = when (node) {
        is MathNode.Num -> number(node.value)
        is MathNode.Sym -> node.name
        is MathNode.Neg -> "-" + p(node.operand, POW)
        is MathNode.Bin -> when (node.op) {
            BinOp.ADD -> p(node.left, ADD) + " + " + p(node.right, MUL)
            BinOp.SUB -> p(node.left, ADD) + " - " + p(node.right, MUL)
            BinOp.MUL -> p(node.left, MUL) + "*" + p(node.right, UNARY + 1)
            BinOp.DIV -> p(node.left, MUL) + "/" + p(node.right, UNARY + 1)
            // ^ is right-associative: the base needs brackets at its own level.
            BinOp.POW -> p(node.left, ATOM) + "^" + p(node.right, ATOM)
        }
        is MathNode.Call -> call(node)
        is MathNode.Diff -> "d/d${node.variable}(" + print(node.body) + ")"
        is MathNode.NumericDiff -> "d/d${node.variable}(" + print(node.body) + ")"
        is MathNode.Integral -> "∫_(" + print(node.lower) + ")^(" + print(node.upper) + ") (" + print(node.body) + ") d" + node.variable
        is MathNode.Series -> (if (node.product) "∏" else "Σ") +
            "_(" + node.variable + "=" + print(node.from) + ")^(" + print(node.to) + ") (" + print(node.body) + ")"
        is MathNode.Piecewise -> "{" + (
            node.branches.map { condition(it.condition) + ": " + print(it.value) } +
                listOfNotNull(node.otherwise?.let { print(it) })
            ).joinToString(", ") + "}"
        is MathNode.Tuple -> "(" + node.items.joinToString(", ") { print(it) } + ")"
        is MathNode.Let -> raw(node.body)
    }

    private fun call(node: MathNode.Call): String {
        val name = when (node.name) {
            "factorial" -> return p(node.args[0], ATOM) + "!"
            "log" -> if (node.args.size == 2) return "log_(" + print(node.args[1]) + ")(" + print(node.args[0]) + ")" else "log"
            else -> node.name
        }
        return name + "'".repeat(node.primes) + "(" + node.args.joinToString(", ") { print(it) } + ")"
    }

    private fun condition(c: MathNode.Condition): String = buildString {
        c.operands.forEachIndexed { i, op ->
            if (i > 0) append(c.relations[i - 1].symbol)
            append(print(op))
        }
    }

    private fun number(v: Double): String {
        if (v == Double.POSITIVE_INFINITY) return "∞"
        if (v == Double.NEGATIVE_INFINITY) return "(-∞)"
        if (v.isNaN()) return "(0/0)"
        val l = v.toLong()
        val text = if (l.toDouble() == v && kotlin.math.abs(v) < 1e15) l.toString() else v.toBigDecimal().stripTrailingZeros().toPlainString()
        return if (v < 0) "($text)" else text
    }
}
