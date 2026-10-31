package com.prostuti.student.domain.graph

/**
 * Recursive-descent parser for one graph line. Precedence, lowest first:
 * relations (= < ≤ > ≥, chains allowed) · `+ −` · `× ÷` and implicit multiplication (2x, x sin x)
 * · prefix − · `^` (right-assoc) · postfix ! · numbers, names, calls, ( ), { }, | |, ∫, Σ, d/dx.
 *
 * Desmos-style conveniences: `sin x`, `sin 2x`, `sin²x`, `sin⁻¹x`, `log_2(x)`, `|x|`,
 * `{x<0: −x, x}` piecewise, `x² {0<x<2}` restriction, `∫_0^1 x² dx`, `∫ x² dx` (= ∫_0^x),
 * `Σ_(n=1)^10 n²`, `d/dx(x³)`, `d²/dx²(…)`, `f'(x)`. Missing closing brackets at the very end are allowed.
 */
class MathParser private constructor(
    private val tokens: List<MathToken>,
    private val userFunctions: Set<String>,
) {
    private var pos = 0
    private var absDepth = 0

    private val peek: MathToken? get() = tokens.getOrNull(pos)
    private fun peekType(offset: Int = 0): TokenType? = tokens.getOrNull(pos + offset)?.type
    private fun next(): MathToken = tokens[pos++]

    private fun fail(kind: GraphErrorKind, at: Int = peek?.start ?: tokens.lastOrNull()?.end ?: 0): Nothing =
        throw GraphParseException(GraphError(kind, at))

    companion object {
        /** Parses [text]. [userFunctions] are the names defined elsewhere as `f(x) = …`, so `f(2)` is a call. */
        fun parse(text: String, userFunctions: Set<String> = emptySet()): ParseResult = try {
            val tokens = MathTokenizer.tokenize(text)
            if (tokens.isEmpty()) {
                ParseResult.Failure(GraphError(GraphErrorKind.SYNTAX, 0))
            } else {
                ParseResult.Success(MathParser(tokens, userFunctions).statement())
            }
        } catch (e: GraphParseException) {
            ParseResult.Failure(e.error)
        } catch (e: StackOverflowError) {
            ParseResult.Failure(GraphError(GraphErrorKind.TOO_COMPLEX, 0))
        }

        private val RELATIONS = mapOf(
            TokenType.EQ to Relation.EQ, TokenType.LT to Relation.LT, TokenType.LE to Relation.LE,
            TokenType.GT to Relation.GT, TokenType.GE to Relation.GE,
        )
        private val OPENERS = setOf(TokenType.OPEN_PAREN, TokenType.OPEN_BRACE)
        private val CLOSERS = setOf(TokenType.CLOSE_PAREN, TokenType.CLOSE_BRACE)
        private val ROOT_FUNCTIONS = setOf("sqrt", "cbrt")
    }

    private fun statement(): MathStatement {
        val operands = mutableListOf(expression())
        val relations = mutableListOf<Relation>()
        while (true) {
            val rel = peek?.let { RELATIONS[it.type] } ?: break
            next()
            relations += rel
            operands += expression()
        }
        if (pos < tokens.size) fail(unexpectedKind())
        return MathStatement(operands, relations)
    }

    private fun unexpectedKind(): GraphErrorKind =
        if (peekType() in CLOSERS || peekType() == TokenType.BAR) GraphErrorKind.UNBALANCED_BRACKETS else GraphErrorKind.SYNTAX

    /** Parses the whole token list as one expression (used for integral bodies). */
    private fun wholeExpression(): MathNode {
        val node = expression()
        if (pos < tokens.size) fail(unexpectedKind())
        return node
    }

    private fun expression(): MathNode = additive()

    private fun additive(): MathNode {
        var node = term()
        while (true) {
            node = when (peekType()) {
                TokenType.PLUS -> { next(); MathNode.Bin(BinOp.ADD, node, term()) }
                TokenType.MINUS -> { next(); MathNode.Bin(BinOp.SUB, node, term()) }
                else -> return node
            }
        }
    }

    private fun term(): MathNode {
        var node = unary()
        while (true) {
            node = when {
                peekType() == TokenType.TIMES -> { next(); MathNode.Bin(BinOp.MUL, node, unary()) }
                peekType() == TokenType.DIVIDE -> { next(); MathNode.Bin(BinOp.DIV, node, unary()) }
                startsImplicitOperand() -> MathNode.Bin(BinOp.MUL, node, power())
                else -> return node
            }
        }
    }

    private fun startsImplicitOperand(): Boolean = when (peekType()) {
        TokenType.NUMBER, TokenType.IDENT, TokenType.OPEN_PAREN, TokenType.OPEN_BRACE,
        TokenType.INTEGRAL, TokenType.SUM, TokenType.PRODUCT -> true
        TokenType.BAR -> absDepth == 0
        else -> false
    }

    private fun unary(): MathNode = when (peekType()) {
        TokenType.MINUS -> { next(); MathNode.Neg(unary()) }
        TokenType.PLUS -> { next(); unary() }
        else -> power()
    }

    private fun power(): MathNode {
        val base = postfix()
        if (peekType() != TokenType.POWER) return base
        next()
        return MathNode.Bin(BinOp.POW, base, exponent())
    }

    /** Right side of ^: allows a sign and is right-associative (2^3^2 = 2^9). */
    private fun exponent(): MathNode = when (peekType()) {
        TokenType.MINUS -> { next(); MathNode.Neg(exponent()) }
        TokenType.PLUS -> { next(); exponent() }
        else -> {
            val base = postfix()
            if (peekType() == TokenType.POWER) { next(); MathNode.Bin(BinOp.POW, base, exponent()) } else base
        }
    }

    private fun postfix(): MathNode {
        var node = primary()
        while (peekType() == TokenType.FACTORIAL) {
            next()
            node = MathNode.Call("factorial", listOf(node))
        }
        return node
    }

    private fun primary(): MathNode {
        val token = peek ?: fail(GraphErrorKind.SYNTAX)
        return when (token.type) {
            TokenType.NUMBER -> { next(); MathNode.Num(token.number) }
            TokenType.IDENT -> identifier()
            TokenType.OPEN_PAREN -> parenGroup(allowTuple = true)
            TokenType.OPEN_BRACE -> braceGroup()
            TokenType.BAR -> absBars()
            TokenType.INTEGRAL -> integral()
            TokenType.SUM -> series(product = false)
            TokenType.PRODUCT -> series(product = true)
            TokenType.CLOSE_PAREN, TokenType.CLOSE_BRACE -> fail(GraphErrorKind.UNBALANCED_BRACKETS)
            else -> fail(GraphErrorKind.SYNTAX)
        }
    }

    private fun identifier(): MathNode {
        val token = next()
        val name = token.text
        if (name == "d") derivativeOperator(pos - 1)?.let { return it }
        MathFunctions.lookup(name)?.let { return builtinCall(it, token) }
        if (name in userFunctions && (peekType() == TokenType.PRIME || peekType() == TokenType.OPEN_PAREN)) {
            var primes = 0
            while (peekType() == TokenType.PRIME) { next(); primes++ }
            if (peekType() != TokenType.OPEN_PAREN) fail(GraphErrorKind.SYNTAX)
            return MathNode.Call(name, argumentList(), primes)
        }
        return MathNode.Sym(name)
    }

    private fun builtinCall(fn: BuiltinFunction, token: MathToken): MathNode {
        var function = fn
        var base: MathNode? = null
        var power: MathNode? = null
        if (fn.name == "log" && peekType() == TokenType.UNDERSCORE) {
            next()
            base = bound()
        }
        if (peekType() == TokenType.POWER) {
            next()
            val exp = exponent()
            val inverse = fn.inverse?.let { MathFunctions.lookup(it) }
            if (inverse != null && isMinusOne(exp)) function = inverse else power = exp
        }
        val args = when {
            peekType() == TokenType.OPEN_PAREN -> argumentList()
            peek == null -> fail(GraphErrorKind.EMPTY_ARGUMENT, token.end)
            else -> listOf(bareArgument(function.name))
        }
        val allArgs = if (base != null) args + base else args
        if (allArgs.size !in function.minArgs..function.maxArgs || (base != null && args.size != 1)) {
            fail(GraphErrorKind.WRONG_ARGUMENT_COUNT, token.start)
        }
        val call = MathNode.Call(function.name, allArgs)
        return if (power != null) MathNode.Bin(BinOp.POW, call, power) else call
    }

    private fun isMinusOne(node: MathNode): Boolean =
        (node is MathNode.Num && node.value == -1.0) ||
            (node is MathNode.Neg && node.operand is MathNode.Num && (node.operand as MathNode.Num).value == 1.0)

    /**
     * `sin 2x` → sin(2x), `sin x cos x` → sin(x)·cos(x), `ln x + 1` → ln(x) + 1: the argument is one
     * factor plus any numbers/variables written right after it. √ takes just one factor (√2 x = √2·x).
     */
    private fun bareArgument(function: String): MathNode {
        val first = if (peekType() == TokenType.MINUS) { next(); MathNode.Neg(power()) } else power()
        if (function in ROOT_FUNCTIONS) return first
        var node = first
        while (true) {
            val token = peek ?: break
            val continues = when (token.type) {
                TokenType.NUMBER -> true
                TokenType.IDENT -> !MathFunctions.isBuiltin(token.text) && token.text !in userFunctions &&
                    !(token.text == "d" && derivativeOperatorAhead(pos))
                else -> false
            }
            if (!continues) break
            node = MathNode.Bin(BinOp.MUL, node, power())
        }
        return node
    }

    private fun argumentList(): List<MathNode> {
        val open = next() // (
        val savedAbs = absDepth
        absDepth = 0
        val args = mutableListOf<MathNode>()
        if (peekType() == TokenType.CLOSE_PAREN) fail(GraphErrorKind.EMPTY_ARGUMENT)
        while (true) {
            if (peek == null) break // auto-close at end of input
            if (peekType() == TokenType.COMMA || peekType() == TokenType.CLOSE_PAREN) fail(GraphErrorKind.EMPTY_ARGUMENT)
            args += expression()
            if (peekType() == TokenType.COMMA) { next(); continue }
            break
        }
        closeParen(open)
        absDepth = savedAbs
        return args
    }

    private fun closeParen(open: MathToken) {
        when (peekType()) {
            null -> Unit
            TokenType.CLOSE_PAREN -> next()
            else -> fail(if (peekType() == TokenType.COMMA) GraphErrorKind.TUPLE_NOT_ALLOWED else GraphErrorKind.UNBALANCED_BRACKETS, open.start)
        }
    }

    private fun parenGroup(allowTuple: Boolean): MathNode {
        val open = next()
        val savedAbs = absDepth
        absDepth = 0
        if (peekType() == TokenType.CLOSE_PAREN) fail(GraphErrorKind.EMPTY_ARGUMENT)
        val first = expression()
        val node = if (allowTuple && peekType() == TokenType.COMMA) {
            val items = mutableListOf(first)
            while (peekType() == TokenType.COMMA) {
                next()
                items += expression()
            }
            MathNode.Tuple(items)
        } else {
            first
        }
        if (peekType() == TokenType.COMMA) fail(GraphErrorKind.TUPLE_NOT_ALLOWED)
        closeParen(open)
        absDepth = savedAbs
        return node
    }

    /** `{2}` grouping, `{x>0}` (1 where true), `{x<0: −x, 0≤x≤1: x², 1}` piecewise. */
    private fun braceGroup(): MathNode {
        val open = next()
        val savedAbs = absDepth
        absDepth = 0
        val branches = mutableListOf<MathNode.Branch>()
        var otherwise: MathNode? = null
        if (peekType() == TokenType.CLOSE_BRACE) fail(GraphErrorKind.EMPTY_ARGUMENT)
        while (true) {
            if (otherwise != null) fail(GraphErrorKind.SYNTAX) // the default must be the last item
            val left = expression()
            val rel = peek?.let { RELATIONS[it.type] }
            if (rel != null) {
                val operands = mutableListOf(left)
                val relations = mutableListOf<Relation>()
                while (true) {
                    val r = peek?.let { RELATIONS[it.type] } ?: break
                    next()
                    relations += r
                    operands += expression()
                }
                val condition = MathNode.Condition(operands, relations)
                val value = if (peekType() == TokenType.COLON) { next(); expression() } else MathNode.Num(1.0)
                branches += MathNode.Branch(condition, value)
            } else {
                if (peekType() == TokenType.COLON) fail(GraphErrorKind.SYNTAX)
                otherwise = left
            }
            if (peekType() == TokenType.COMMA) { next(); continue }
            break
        }
        when (peekType()) {
            null -> Unit
            TokenType.CLOSE_BRACE -> next()
            else -> fail(GraphErrorKind.UNBALANCED_BRACKETS, open.start)
        }
        absDepth = savedAbs
        return if (branches.isEmpty()) otherwise!! else MathNode.Piecewise(branches, otherwise)
    }

    private fun absBars(): MathNode {
        val open = next()
        absDepth++
        // "||x| − 1|": a bar right after an opening bar opens a nested |…|, so no empty check here.
        val inner = expression()
        when (peekType()) {
            null -> Unit
            TokenType.BAR -> next()
            else -> fail(GraphErrorKind.UNBALANCED_BRACKETS, open.start)
        }
        absDepth--
        return MathNode.Call("abs", listOf(inner))
    }

    /** A bound or sub/superscript: a number, name, (…), {…} or a sign before one. */
    private fun bound(): MathNode = when (peekType()) {
        TokenType.MINUS -> { next(); MathNode.Neg(bound()) }
        TokenType.PLUS -> { next(); bound() }
        TokenType.NUMBER -> MathNode.Num(next().number)
        TokenType.IDENT -> {
            val token = peek!!
            if (MathFunctions.isBuiltin(token.text)) identifier() else { next(); MathNode.Sym(token.text) }
        }
        TokenType.OPEN_PAREN -> parenGroup(allowTuple = false)
        TokenType.OPEN_BRACE -> braceGroup()
        else -> fail(GraphErrorKind.SYNTAX)
    }

    private fun integral(): MathNode {
        val start = next()
        var lower: MathNode? = null
        var upper: MathNode? = null
        repeat(2) {
            when (peekType()) {
                TokenType.UNDERSCORE -> { next(); lower = bound() }
                TokenType.POWER -> { next(); upper = bound() }
                else -> Unit
            }
        }
        if ((lower == null) != (upper == null)) fail(GraphErrorKind.SYNTAX, start.start)
        val end = findDifferential(pos) ?: fail(GraphErrorKind.MISSING_INTEGRAL_DIFFERENTIAL, start.start)
        if (end == pos) fail(GraphErrorKind.EMPTY_ARGUMENT, start.end)
        val body = MathParser(tokens.subList(pos, end), userFunctions).wholeExpression()
        val variable = tokens[end + 1].text
        pos = end + 2
        // ∫ f dx without bounds is the antiderivative that is 0 at 0: ∫_0^x f dt.
        return MathNode.Integral(variable, lower ?: MathNode.Num(0.0), upper ?: MathNode.Sym("x"), body)
    }

    /** Index of the "d" of the integral's own dx, skipping inner integrals' and bracketed ones. */
    private fun findDifferential(from: Int): Int? {
        var depth = 0
        var nested = 0
        var j = from
        while (j < tokens.size) {
            val t = tokens[j]
            when {
                t.type in OPENERS -> depth++
                t.type in CLOSERS -> { depth--; if (depth < 0) return null }
                depth == 0 && t.type == TokenType.INTEGRAL -> nested++
                depth == 0 && isDifferential(j) -> if (nested == 0) return j else nested--
            }
            j++
        }
        return null
    }

    private fun isDifferential(j: Int): Boolean {
        val d = tokens[j]
        val v = tokens.getOrNull(j + 1) ?: return false
        return d.type == TokenType.IDENT && d.text == "d" && v.type == TokenType.IDENT && v.start == d.end &&
            isVariableName(v.text)
    }

    private fun isVariableName(name: String): Boolean = !MathFunctions.isBuiltin(name) && name.first().isLetter()

    private fun derivativeOperatorAhead(index: Int): Boolean = matchDerivative(index) != null

    /** `d/dx` (order 1) or `d^n/dx^n`: returns (order, variable, index after the operator). */
    private fun matchDerivative(i: Int): Triple<Int, String, Int>? {
        fun at(k: Int) = tokens.getOrNull(k)
        if (at(i + 1)?.type == TokenType.DIVIDE && at(i + 2)?.text == "d" && isDifferential(i + 2)) {
            return Triple(1, at(i + 3)!!.text, i + 4)
        }
        val order = at(i + 2)?.takeIf { at(i + 1)?.type == TokenType.POWER && it.type == TokenType.NUMBER }?.number
        if (order != null && order == order.toInt().toDouble() && order.toInt() in 1..6 &&
            at(i + 3)?.type == TokenType.DIVIDE && at(i + 4)?.text == "d" && isDifferential(i + 4) &&
            at(i + 6)?.type == TokenType.POWER && at(i + 7)?.type == TokenType.NUMBER && at(i + 7)!!.number == order
        ) {
            return Triple(order.toInt(), at(i + 5)!!.text, i + 8)
        }
        return null
    }

    private fun derivativeOperator(index: Int): MathNode? {
        val (order, variable, after) = matchDerivative(index) ?: return null
        pos = after
        if (peek == null) fail(GraphErrorKind.EMPTY_ARGUMENT)
        var body = if (peekType() == TokenType.OPEN_PAREN) parenGroup(allowTuple = false) else term()
        repeat(order) { body = MathNode.Diff(variable, body) }
        return body
    }

    private fun series(product: Boolean): MathNode {
        val start = next()
        if (peekType() != TokenType.UNDERSCORE) fail(GraphErrorKind.SYNTAX, start.end)
        next()
        val grouped = peekType() == TokenType.OPEN_PAREN || peekType() == TokenType.OPEN_BRACE
        val opener = if (grouped) next() else null
        val variable = peek?.takeIf { it.type == TokenType.IDENT }?.text ?: fail(GraphErrorKind.SYNTAX)
        next()
        if (peekType() != TokenType.EQ) fail(GraphErrorKind.SYNTAX)
        next()
        val from = if (grouped) expression() else bound()
        if (opener != null) {
            val expected = if (opener.type == TokenType.OPEN_PAREN) TokenType.CLOSE_PAREN else TokenType.CLOSE_BRACE
            if (peekType() != expected) fail(GraphErrorKind.UNBALANCED_BRACKETS, opener.start)
            next()
        }
        if (peekType() != TokenType.POWER) fail(GraphErrorKind.SYNTAX)
        next()
        val to = bound()
        if (peek == null) fail(GraphErrorKind.EMPTY_ARGUMENT)
        return MathNode.Series(variable, from, to, term(), product)
    }
}
