package com.prostuti.student.domain.graph

import com.prostuti.student.domain.calculator.AngleUnit
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.acosh
import kotlin.math.asin
import kotlin.math.asinh
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.atanh
import kotlin.math.cbrt
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh

/**
 * Turns a [MathNode] (user functions already inlined, d/dx already expanded) into an [Eval] tree.
 * Parameters and constants are folded in as numbers, integer powers become multiplications and
 * angle conversion is baked in, so evaluation does no lookups at all.
 *
 * With [polarFromXY], `r` and `θ` in an x–y equation mean √(x²+y²) and the polar angle in [0, 2π).
 */
internal class ExpressionCompiler(
    private val angleUnit: AngleUnit,
    private val constants: Map<String, Double>,
    private val polarFromXY: Boolean = false,
) {
    private var nextSlot = 0

    private val toRadians = if (angleUnit == AngleUnit.DEG) PI / 180.0 else 1.0
    private val fromRadians = 1.0 / toRadians

    fun compile(node: MathNode, variables: List<String>): CompiledFunction {
        nextSlot = variables.size
        val scope = HashMap<String, Int>()
        variables.forEachIndexed { i, name -> scope[name] = i }
        val root = node(node, scope)
        return CompiledFunction(root, nextSlot, variables)
    }

    private fun undefined(name: String): Nothing =
        throw CompileException(GraphError(GraphErrorKind.UNDEFINED_VARIABLE, names = listOf(name)))

    private fun node(n: MathNode, scope: Map<String, Int>): Eval = when (n) {
        is MathNode.Num -> Eval.Const(n.value)
        is MathNode.Sym -> symbol(n.name, scope)
        is MathNode.Neg -> {
            val a = node(n.operand, scope)
            if (a is Eval.Const) Eval.Const(-a.value) else Eval { env -> -a.eval(env) }
        }
        is MathNode.Bin -> binary(n, scope)
        is MathNode.Call -> call(n, scope)
        is MathNode.Integral -> integral(n, scope)
        is MathNode.Series -> series(n, scope)
        is MathNode.Piecewise -> piecewise(n, scope)
        is MathNode.Let -> {
            val value = node(n.value, scope)
            val slot = nextSlot++
            val body = node(n.body, scope + (n.name to slot))
            if (body is Eval.Const) body else Eval { env -> env[slot] = value.eval(env); body.eval(env) }
        }
        is MathNode.NumericDiff -> numericDiff(n, scope)
        is MathNode.Diff -> node(Differentiator(angleUnit).derivative(n.body, n.variable), scope)
        is MathNode.Tuple -> throw CompileException(GraphError(GraphErrorKind.TUPLE_NOT_ALLOWED))
    }

    private fun symbol(name: String, scope: Map<String, Int>): Eval {
        scope[name]?.let { return Eval.Slot(it) }
        constants[name]?.let { return Eval.Const(it) }
        if (polarFromXY) {
            val x = scope["x"]
            val y = scope["y"]
            if (x != null && y != null) {
                when (name) {
                    "r" -> return Eval { env -> val a = env[x]; val b = env[y]; sqrt(a * a + b * b) }
                    "θ" -> return Eval { env ->
                        val t = atan2(env[y], env[x])
                        (if (t < 0) t + 2 * PI else t) * fromRadians
                    }
                }
            }
        }
        undefined(name)
    }

    private fun binary(n: MathNode.Bin, scope: Map<String, Int>): Eval {
        val a = node(n.left, scope)
        val b = node(n.right, scope)
        if (a is Eval.Const && b is Eval.Const) {
            return Eval.Const(applyBin(n.op, a.value, b.value))
        }
        return when (n.op) {
            BinOp.ADD -> Eval { env -> a.eval(env) + b.eval(env) }
            BinOp.SUB -> Eval { env -> a.eval(env) - b.eval(env) }
            BinOp.MUL -> if (a is Eval.Const) {
                val k = a.value
                Eval { env -> k * b.eval(env) }
            } else {
                Eval { env -> a.eval(env) * b.eval(env) }
            }
            BinOp.DIV -> Eval { env -> a.eval(env) / b.eval(env) }
            BinOp.POW -> power(a, b)
        }
    }

    private fun applyBin(op: BinOp, x: Double, y: Double): Double = when (op) {
        BinOp.ADD -> x + y
        BinOp.SUB -> x - y
        BinOp.MUL -> x * y
        BinOp.DIV -> x / y
        BinOp.POW -> SpecialFunctions.realPow(x, y)
    }

    private fun power(a: Eval, b: Eval): Eval {
        if (b is Eval.Const) {
            val e = b.value
            val n = e.toInt()
            if (n.toDouble() == e && abs(n) <= 64) {
                return when (n) {
                    0 -> Eval { env -> val v = a.eval(env); if (v.isNaN()) Double.NaN else 1.0 }
                    1 -> a
                    2 -> Eval { env -> val v = a.eval(env); v * v }
                    3 -> Eval { env -> val v = a.eval(env); v * v * v }
                    -1 -> Eval { env -> 1.0 / a.eval(env) }
                    else -> Eval { env -> SpecialFunctions.intPow(a.eval(env), n) }
                }
            }
            if (e == 0.5) return Eval { env -> sqrt(a.eval(env)) }
            return Eval { env -> SpecialFunctions.realPow(a.eval(env), e) }
        }
        return Eval { env -> SpecialFunctions.realPow(a.eval(env), b.eval(env)) }
    }

    private fun call(n: MathNode.Call, scope: Map<String, Int>): Eval {
        val fn = MathFunctions.lookup(n.name) ?: undefined(n.name)
        if (fn.structural) throw CompileException(GraphError(GraphErrorKind.UNSUPPORTED_EQUATION))
        var args = n.args.map { node(it, scope) }
        if (fn.angle == AngleRole.INPUT && toRadians != 1.0) {
            val a = args[0]
            args = listOf(if (a is Eval.Const) Eval.Const(a.value * toRadians) else Eval { env -> a.eval(env) * toRadians })
        }
        val raw = builtin(fn.name, args)
        val result = if (fn.angle == AngleRole.OUTPUT && fromRadians != 1.0) {
            Eval { env -> raw.eval(env) * fromRadians }
        } else {
            raw
        }
        return if (args.all { it is Eval.Const }) Eval.Const(result.eval(DoubleArray(0))) else result
    }

    private fun interface DoubleOp {
        fun apply(v: Double): Double
    }

    private fun unary(arg: Eval, op: DoubleOp): Eval = Eval { env -> op.apply(arg.eval(env)) }

    private fun builtin(name: String, args: List<Eval>): Eval {
        val u = args[0]
        return when (name) {
            "sin" -> unary(u) { sin(it) }
            "cos" -> unary(u) { cos(it) }
            "tan" -> unary(u) { tan(it) }
            "sec" -> unary(u) { 1.0 / cos(it) }
            "csc" -> unary(u) { 1.0 / sin(it) }
            "cot" -> unary(u) { cos(it) / sin(it) }
            "asin" -> unary(u) { asin(it) }
            "acos" -> unary(u) { acos(it) }
            "atan" -> unary(u) { atan(it) }
            "asec" -> unary(u) { acos(1.0 / it) }
            "acsc" -> unary(u) { asin(1.0 / it) }
            "acot" -> unary(u) { PI / 2 - atan(it) }
            "sinh" -> unary(u) { sinh(it) }
            "cosh" -> unary(u) { cosh(it) }
            "tanh" -> unary(u) { tanh(it) }
            "sech" -> unary(u) { 1.0 / cosh(it) }
            "csch" -> unary(u) { 1.0 / sinh(it) }
            "coth" -> unary(u) { cosh(it) / sinh(it) }
            "asinh" -> unary(u) { asinh(it) }
            "acosh" -> unary(u) { acosh(it) }
            "atanh" -> unary(u) { atanh(it) }
            "ln" -> unary(u) { ln(it) }
            "log" -> if (args.size == 2) {
                val b = args[1]
                Eval { env -> SpecialFunctions.log(u.eval(env), b.eval(env)) }
            } else {
                unary(u) { log10(it) }
            }
            "exp" -> unary(u) { exp(it) }
            "sqrt" -> unary(u) { sqrt(it) }
            "cbrt" -> unary(u) { cbrt(it) }
            "nthroot" -> {
                val k = args[1]
                Eval { env -> nthRoot(u.eval(env), k.eval(env)) }
            }
            "abs" -> unary(u) { abs(it) }
            "sign" -> unary(u) { SpecialFunctions.sign(it) }
            "floor" -> unary(u) { floor(it) }
            "ceil" -> unary(u) { ceil(it) }
            "round" -> unary(u) { SpecialFunctions.roundHalfAway(it) }
            "factorial" -> unary(u) { SpecialFunctions.factorial(it) }
            "gamma" -> unary(u) { SpecialFunctions.gamma(it) }
            "mod" -> binaryOp(args) { a, b -> SpecialFunctions.mod(a, b) }
            "gcd" -> binaryOp(args) { a, b -> SpecialFunctions.gcd(a, b) }
            "lcm" -> binaryOp(args) { a, b -> SpecialFunctions.lcm(a, b) }
            "nCr" -> binaryOp(args) { a, b -> SpecialFunctions.nCr(a, b) }
            "nPr" -> binaryOp(args) { a, b -> SpecialFunctions.nPr(a, b) }
            "min" -> Eval { env ->
                var m = Double.POSITIVE_INFINITY
                for (a in args) { val v = a.eval(env); if (v.isNaN()) return@Eval Double.NaN; if (v < m) m = v }
                m
            }
            "max" -> Eval { env ->
                var m = Double.NEGATIVE_INFINITY
                for (a in args) { val v = a.eval(env); if (v.isNaN()) return@Eval Double.NaN; if (v > m) m = v }
                m
            }
            else -> undefined(name)
        }
    }

    private fun interface BinaryDoubleOp {
        fun apply(a: Double, b: Double): Double
    }

    private fun binaryOp(args: List<Eval>, op: BinaryDoubleOp): Eval {
        val a = args[0]
        val b = args[1]
        return Eval { env -> op.apply(a.eval(env), b.eval(env)) }
    }

    /** Real n-th root: odd n keeps the sign (∛−8 = −2), even n of a negative is undefined. */
    private fun nthRoot(x: Double, n: Double): Double {
        if (n == 0.0) return Double.NaN
        val ni = round(n)
        if (x < 0) {
            if (ni != n || ni.toLong() % 2 == 0L) return Double.NaN
            return -SpecialFunctions.realPow(-x, 1.0 / n)
        }
        return SpecialFunctions.realPow(x, 1.0 / n)
    }

    private fun integral(n: MathNode.Integral, scope: Map<String, Int>): Eval {
        val lower = node(n.lower, scope)
        val upper = node(n.upper, scope)
        val slot = nextSlot++
        val body = node(n.body, scope + (n.variable to slot))
        return Eval { env ->
            val a = lower.eval(env)
            val b = upper.eval(env)
            val saved = env[slot]
            val result = Integrator.integrate({ t -> env[slot] = t; body.eval(env) }, a, b)
            env[slot] = saved
            result.value
        }
    }

    private fun series(n: MathNode.Series, scope: Map<String, Int>): Eval {
        val from = node(n.from, scope)
        val to = node(n.to, scope)
        val slot = nextSlot++
        val body = node(n.body, scope + (n.variable to slot))
        val product = n.product
        return Eval { env ->
            val a = from.eval(env)
            val b = to.eval(env)
            if (a.isNaN() || b.isNaN()) return@Eval Double.NaN
            val start = round(a).toLong()
            val end = round(b).toLong()
            if (end - start > MAX_SERIES_TERMS) return@Eval Double.NaN
            val saved = env[slot]
            var acc = if (product) 1.0 else 0.0
            var k = start
            while (k <= end) {
                env[slot] = k.toDouble()
                val v = body.eval(env)
                acc = if (product) acc * v else acc + v
                if (acc.isNaN()) break
                k++
            }
            env[slot] = saved
            acc
        }
    }

    private fun piecewise(n: MathNode.Piecewise, scope: Map<String, Int>): Eval {
        val conditions = n.branches.map { condition(it.condition, scope) }.toTypedArray()
        val values = n.branches.map { node(it.value, scope) }.toTypedArray()
        val otherwise = n.otherwise?.let { node(it, scope) }
        return Eval { env ->
            for (i in conditions.indices) {
                if (conditions[i].test(env)) return@Eval values[i].eval(env)
            }
            otherwise?.eval(env) ?: Double.NaN
        }
    }

    private fun condition(c: MathNode.Condition, scope: Map<String, Int>): CondEval {
        val operands = c.operands.map { node(it, scope) }.toTypedArray()
        val relations = c.relations.toTypedArray()
        return CondEval { env ->
            var left = operands[0].eval(env)
            for (i in relations.indices) {
                val right = operands[i + 1].eval(env)
                if (!holds(left, relations[i], right)) return@CondEval false
                left = right
            }
            true
        }
    }

    private fun holds(a: Double, r: Relation, b: Double): Boolean {
        if (a.isNaN() || b.isNaN()) return false
        return when (r) {
            Relation.EQ -> abs(a - b) <= 1e-12 * max(1.0, max(abs(a), abs(b)))
            Relation.LT -> a < b
            Relation.LE -> a <= b
            Relation.GT -> a > b
            Relation.GE -> a >= b
        }
    }

    /** Central difference with one Richardson step: error O(h⁴), about 1e-10 for smooth functions. */
    private fun numericDiff(n: MathNode.NumericDiff, scope: Map<String, Int>): Eval {
        val slot = scope[n.variable] ?: return Eval.Const(0.0)
        val body = node(n.body, scope)
        return Eval { env ->
            val x0 = env[slot]
            val h = 1e-3 * max(1.0, abs(x0))
            fun at(x: Double): Double { env[slot] = x; return body.eval(env) }
            val d1 = (at(x0 + h) - at(x0 - h)) / (2 * h)
            val d2 = (at(x0 + h / 2) - at(x0 - h / 2)) / h
            env[slot] = x0
            (4 * d2 - d1) / 3
        }
    }

    private companion object {
        const val MAX_SERIES_TERMS = 100_000L
    }
}
