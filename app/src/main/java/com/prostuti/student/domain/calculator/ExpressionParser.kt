package com.prostuti.student.domain.calculator

import com.prostuti.student.domain.calculator.CalcToken as T

/**
 * Recursive-descent parser over editor tokens. Priority, lowest first (as on the real calculator):
 * `+ −` · `× ÷` · implicit multiplication (2π, 3sin(30)) · nPr nCr · prefix (−) · fraction ⌟ ·
 * `^( ˣ√(` · postfix `² ³ ⁻¹ ! % °` · numbers, constants, variables, ( ) and functions.
 * Closing brackets at the very end may be left out.
 */
internal class ExpressionParser(private val tokens: List<String>) {

    private var pos = 0
    private val peek: String? get() = tokens.getOrNull(pos)

    private fun syntax(at: Int = pos): Nothing =
        throw CalcException(CalcErrorKind.SYNTAX, at.coerceIn(0, tokens.size))

    fun parse(allowEquation: Boolean): CalcNode {
        if (tokens.isEmpty()) syntax(0)
        val left = parseAdditive()
        val node = if (peek == T.EQUALS) {
            if (!allowEquation) syntax()
            pos++
            CalcNode.Equation(left, parseAdditive())
        } else {
            left
        }
        if (pos < tokens.size) syntax()
        return node
    }

    private fun parseAdditive(): CalcNode {
        var node = parseMultiplicative()
        while (peek == T.PLUS || peek == T.MINUS) {
            val op = tokens[pos++]
            node = CalcNode.Binary(op, node, parseMultiplicative())
        }
        return node
    }

    private fun parseMultiplicative(): CalcNode {
        var node = parseImplicit()
        while (peek == T.TIMES || peek == T.DIVIDE) {
            val op = tokens[pos++]
            node = CalcNode.Binary(op, node, parseImplicit())
        }
        return node
    }

    private fun parseImplicit(): CalcNode {
        var node = parseCombination()
        while (startsOperand(peek)) node = CalcNode.Binary(T.TIMES, node, parseCombination())
        return node
    }

    private fun parseCombination(): CalcNode {
        var node = parseUnary()
        while (peek == T.NPR || peek == T.NCR) {
            val op = tokens[pos++]
            node = CalcNode.Binary(op, node, parseUnary())
        }
        return node
    }

    private fun parseUnary(): CalcNode = when (peek) {
        T.NEG, T.MINUS -> { pos++; CalcNode.Negate(parseUnary()) }
        T.PLUS -> { pos++; parseUnary() }
        else -> parseFraction()
    }

    private fun parseFraction(): CalcNode {
        var node = parsePower()
        while (peek == T.FRACTION) {
            pos++
            node = CalcNode.Binary(T.FRACTION, node, parsePower())
        }
        return node
    }

    private fun parsePower(): CalcNode {
        var node = parsePostfix(parsePrimary())
        while (peek == T.POWER || peek == T.XROOT) {
            val op = tokens[pos++]
            node = parsePostfix(CalcNode.Binary(op, node, parseGroupBody()))
        }
        return node
    }

    private fun parsePostfix(start: CalcNode): CalcNode {
        var node = start
        while (true) {
            when (peek) {
                T.SQUARE, T.CUBE, T.INVERSE, T.FACTORIAL, T.PERCENT,
                T.UNIT_DEG, T.UNIT_RAD, T.UNIT_GRAD -> node = CalcNode.Postfix(tokens[pos++], node)
                T.DMS -> { pos++; node = parseSexagesimal(node) }
                else -> return node
            }
        }
    }

    /** d°m°s° → d + m/60 + s/3600 (the degrees part was already read). */
    private fun parseSexagesimal(degrees: CalcNode): CalcNode {
        var total = degrees
        for (divisor in doubleArrayOf(60.0, 3600.0)) {
            val next = peek ?: break
            if (!T.isDigit(next) && next != T.DOT) break
            val part = parseNumber()
            if (peek != T.DMS) syntax()
            pos++
            total = CalcNode.Binary(T.PLUS, total, CalcNode.Binary(T.DIVIDE, part, CalcNode.Num(divisor)))
        }
        return total
    }

    private fun parsePrimary(): CalcNode {
        val token = peek ?: syntax()
        val variable = T.variableName(token)
        return when {
            T.isDigit(token) || token == T.DOT || token == T.EXP10 -> parseNumber()
            token == T.PI -> { pos++; CalcNode.Num(Math.PI) }
            token == T.E -> { pos++; CalcNode.Num(Math.E) }
            token == T.ANS -> { pos++; CalcNode.Ans }
            token == T.RANDOM -> { pos++; CalcNode.Random }
            variable != null -> { pos++; CalcNode.Var(variable) }
            token == T.OPEN -> { pos++; parseGroupBody() }
            token in T.FUNCTIONS -> {
                val at = pos++
                val args = parseArguments()
                if (args.size !in arity(token)) syntax(at)
                CalcNode.Call(token, args)
            }
            else -> syntax()
        }
    }

    private fun parseGroupBody(): CalcNode {
        val node = parseAdditive()
        expectClose()
        return node
    }

    private fun parseArguments(): List<CalcNode> {
        val args = mutableListOf(parseAdditive())
        while (peek == T.COMMA) {
            pos++
            args += parseAdditive()
        }
        expectClose()
        return args
    }

    private fun expectClose() {
        when (peek) {
            T.CLOSE -> pos++
            null -> Unit            // auto-close at the end of the input
            else -> syntax()
        }
    }

    private fun parseNumber(): CalcNode {
        val start = pos
        val mantissa = StringBuilder()
        var dotSeen = false
        while (true) {
            val t = peek ?: break
            if (t == T.DOT) {
                if (dotSeen) syntax()
                dotSeen = true
            } else if (!T.isDigit(t)) {
                break
            }
            mantissa.append(t)
            pos++
        }
        if (mantissa.toString() == ".") syntax(start)
        var text = if (mantissa.isEmpty()) "1" else mantissa.toString()
        if (peek == T.EXP10) {
            pos++
            var sign = ""
            when (peek) {
                T.NEG, T.MINUS -> { sign = "-"; pos++ }
                T.PLUS -> pos++
            }
            val exponent = StringBuilder()
            while (peek?.let(T::isDigit) == true) exponent.append(tokens[pos++])
            if (exponent.isEmpty()) syntax()
            text += "E$sign$exponent"
        }
        return CalcNode.Num(text.toDouble())
    }

    private fun startsOperand(token: String?): Boolean {
        if (token == null) return false
        return T.isDigit(token) || token == T.DOT || token == T.EXP10 ||
            token == T.PI || token == T.E || token == T.ANS || token == T.RANDOM ||
            token == T.OPEN || token in T.FUNCTIONS || T.variableName(token) != null
    }

    private fun arity(function: String): IntRange = when (function) {
        T.LOG -> 1..2
        T.DERIVATIVE -> 2..2
        T.INTEGRAL, T.SUM -> 3..3
        else -> 1..1
    }
}
