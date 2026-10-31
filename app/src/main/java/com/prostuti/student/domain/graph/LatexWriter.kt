package com.prostuti.student.domain.graph

/** Turns a parsed line back into LaTeX for the pretty preview (what the student typed, typeset). */
object LatexWriter {

    fun write(statement: MathStatement): String = buildString {
        statement.operands.forEachIndexed { i, node ->
            if (i > 0) append(' ').append(relation(statement.relations[i - 1])).append(' ')
            append(write(node))
        }
    }

    fun write(node: MathNode): String = latex(node, Prec.LOWEST)

    private enum class Prec { LOWEST, ADD, MUL, UNARY, POW, POSTFIX, ATOM }

    private fun relation(r: Relation): String = when (r) {
        Relation.EQ -> "="
        Relation.LT -> "<"
        Relation.LE -> "\\le"
        Relation.GT -> ">"
        Relation.GE -> "\\ge"
    }

    private fun precOf(node: MathNode): Prec = when (node) {
        is MathNode.Num -> if (node.value < 0) Prec.UNARY else Prec.ATOM
        is MathNode.Sym, is MathNode.Tuple, is MathNode.Piecewise -> Prec.ATOM
        is MathNode.Neg -> Prec.UNARY
        is MathNode.Bin -> when (node.op) {
            BinOp.ADD, BinOp.SUB -> Prec.ADD
            BinOp.MUL -> Prec.MUL
            BinOp.DIV -> Prec.ATOM // \frac is self-delimiting
            BinOp.POW -> Prec.POW
        }
        is MathNode.Call -> if (node.name == "factorial") Prec.POSTFIX else Prec.ATOM
        // ∫, Σ, d/dx run to the right, so they bind like a product.
        is MathNode.Integral, is MathNode.Series, is MathNode.Diff, is MathNode.NumericDiff -> Prec.MUL
        is MathNode.Let -> precOf(node.body)
    }

    private fun latex(node: MathNode, context: Prec): String {
        val text = raw(node)
        return if (precOf(node) < context) paren(text) else text
    }

    /** Brackets sized to their content: plain for x + 1, stretchy only around tall things. */
    private fun paren(content: String): String =
        if (TALL.any { it in content }) "\\left($content\\right)" else "($content)"

    private val TALL = listOf("\\frac", "\\int", "\\sum", "\\prod", "\\begin", "\\sqrt", "\\left")

    private fun raw(node: MathNode): String = when (node) {
        is MathNode.Num -> number(node.value)
        is MathNode.Sym -> symbol(node.name)
        is MathNode.Neg -> "-" + latex(node.operand, Prec.UNARY)
        is MathNode.Bin -> bin(node)
        is MathNode.Call -> call(node)
        is MathNode.Diff -> "\\frac{d}{d${symbol(node.variable)}}" + latex(node.body, Prec.ATOM)
        is MathNode.NumericDiff -> "\\frac{d}{d${symbol(node.variable)}}" + latex(node.body, Prec.ATOM)
        is MathNode.Integral -> "\\int_{${write(node.lower)}}^{${write(node.upper)}}" +
            latex(node.body, Prec.ADD) + "\\,d${symbol(node.variable)}"
        is MathNode.Series -> (if (node.product) "\\prod" else "\\sum") +
            "_{${symbol(node.variable)}=${write(node.from)}}^{${write(node.to)}}" + latex(node.body, Prec.MUL)
        is MathNode.Piecewise -> piecewise(node)
        is MathNode.Tuple -> paren(node.items.joinToString(", ") { write(it) })
        is MathNode.Let -> raw(node.body)
    }

    private fun bin(node: MathNode.Bin): String = when (node.op) {
        BinOp.ADD -> latex(node.left, Prec.ADD) + "+" + latex(node.right, Prec.MUL)
        BinOp.SUB -> latex(node.left, Prec.ADD) + "-" + latex(node.right, Prec.MUL)
        BinOp.MUL -> {
            val left = latex(node.left, Prec.MUL)
            // A negative right factor gets brackets: x(−2), never "x −2".
            val right = latex(node.right, Prec.POW)
            // 2·3 needs a dot; 2x, x sin x read fine side by side.
            val needsDot = endsWithDigit(left) && startsWithDigit(right)
            left + (if (needsDot) "\\cdot " else " ") + right
        }
        BinOp.DIV -> "\\frac{${write(node.left)}}{${write(node.right)}}"
        BinOp.POW -> latex(node.left, Prec.POSTFIX) + "^{${write(node.right)}}"
    }

    private fun call(node: MathNode.Call): String {
        val args = node.args
        val a = args.firstOrNull()?.let { write(it) } ?: ""
        return when (node.name) {
            "sqrt" -> "\\sqrt{$a}"
            "cbrt" -> "\\sqrt[3]{$a}"
            "nthroot" -> "\\sqrt[${write(args[1])}]{$a}"
            "abs" -> "\\left|$a\\right|"
            "floor" -> "\\left\\lfloor $a\\right\\rfloor"
            "ceil" -> "\\left\\lceil $a\\right\\rceil"
            "factorial" -> latex(args[0], Prec.ATOM) + "!"
            "exp" -> "e^{$a}"
            "log" -> if (args.size == 2) "\\log_{${write(args[1])}}" + paren(a) else "\\log" + paren(a)
            "nCr" -> "{}^{${write(args[0])}}C_{${write(args[1])}}"
            "nPr" -> "{}^{${write(args[0])}}P_{${write(args[1])}}"
            in TRIG_INVERSE -> "\\${TRIG_INVERSE.getValue(node.name)}^{-1}" + paren(a)
            in STANDARD -> "\\${node.name}" + paren(a)
            else -> {
                val name = if (MathFunctions.isBuiltin(node.name)) "\\operatorname{${node.name}}" else symbol(node.name)
                name + "'".repeat(node.primes) + paren(args.joinToString(", ") { write(it) })
            }
        }
    }

    private fun piecewise(node: MathNode.Piecewise): String {
        // A lone condition {0<x<2} is a restriction: show it as braces, not a cases block.
        if (node.branches.size == 1 && node.otherwise == null && node.branches[0].value == MathNode.Num(1.0)) {
            return "\\left\\{${condition(node.branches[0].condition)}\\right\\}"
        }
        val rows = node.branches.map { "${write(it.value)} & ${condition(it.condition)}" } +
            listOfNotNull(node.otherwise?.let { "${write(it)} & \\text{otherwise}" })
        return "\\begin{cases}" + rows.joinToString("\\\\") + "\\end{cases}"
    }

    private fun condition(c: MathNode.Condition): String = buildString {
        c.operands.forEachIndexed { i, op ->
            if (i > 0) append(relation(c.relations[i - 1]))
            append(write(op))
        }
    }

    private fun symbol(name: String): String {
        val base = name.substringBefore('_').substringBefore('#')
        val sub = name.substringAfter('_', "").substringBefore('#')
        val head = when (base) {
            "π" -> "\\pi"
            "τ" -> "\\tau"
            "θ" -> "\\theta"
            "φ" -> "\\phi"
            "α" -> "\\alpha"
            "β" -> "\\beta"
            "γ" -> "\\gamma"
            "ω" -> "\\omega"
            "λ" -> "\\lambda"
            "∞" -> "\\infty"
            else -> base
        }
        return if (sub.isEmpty()) head else "${head}_{$sub}"
    }

    private fun number(v: Double): String {
        if (v == Double.POSITIVE_INFINITY) return "\\infty"
        if (v == Double.NEGATIVE_INFINITY) return "-\\infty"
        val l = v.toLong()
        return if (l.toDouble() == v && kotlin.math.abs(v) < 1e15) l.toString() else v.toBigDecimal().stripTrailingZeros().toPlainString()
    }

    private fun endsWithDigit(s: String) = s.lastOrNull()?.isDigit() == true
    private fun startsWithDigit(s: String) = s.firstOrNull()?.isDigit() == true

    private val STANDARD = setOf(
        "sin", "cos", "tan", "sec", "csc", "cot", "sinh", "cosh", "tanh", "coth", "ln",
    )
    private val TRIG_INVERSE = mapOf(
        "asin" to "sin", "acos" to "cos", "atan" to "tan", "asec" to "sec", "acsc" to "csc", "acot" to "cot",
        "asinh" to "sinh", "acosh" to "cosh", "atanh" to "tanh",
    )
}
