package com.prostuti.student.domain.calculator

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh
import com.prostuti.student.domain.calculator.CalcToken as T

/**
 * Evaluates calculator token lists. Pure Kotlin, no Android — every failure is a
 * [CalcException] carrying the error screen to show.
 */
class ExpressionEvaluator {

    /** Evaluates [tokens] (no "=" allowed) and returns the value rounded to 15 digits. */
    fun evaluate(tokens: List<String>, context: CalcContext): Double {
        val root = ExpressionParser(tokens).parse(allowEquation = false)
        return finish(Evaluation(context).eval(root))
    }

    /** Variables used in [tokens], in the calculator's prompt order. */
    fun variablesIn(tokens: List<String>): List<String> =
        T.VARIABLES.filter { T.variable(it) in tokens }

    /**
     * SOLVE: finds [variable] so that left = right (or expression = 0 when there is no "="),
     * starting the search at [initialGuess].
     */
    fun solve(tokens: List<String>, variable: String, initialGuess: Double, context: CalcContext): SolveOutcome {
        val root = ExpressionParser(tokens).parse(allowEquation = true)
        val f: (Double) -> Double? = { x ->
            try {
                Evaluation(context.with(variable, x)).eval(root).takeIf { it.isFinite() }
            } catch (e: CalcException) {
                null
            }
        }
        val x = newton(f, initialGuess) ?: bracketAndBisect(f, initialGuess)
            ?: throw CalcException(CalcErrorKind.CANT_SOLVE)
        val residual = f(x) ?: throw CalcException(CalcErrorKind.CANT_SOLVE)
        return SolveOutcome(finish(x), CalcMath.roundSignificant(residual, CalcMath.INTERNAL_DIGITS))
    }

    private fun finish(value: Double): Double {
        if (!value.isFinite() || abs(value) >= MAX_MAGNITUDE) throw CalcException(CalcErrorKind.MATH)
        return CalcMath.roundSignificant(value, CalcMath.INTERNAL_DIGITS)
    }

    private fun newton(f: (Double) -> Double?, guess: Double): Double? {
        var x = guess
        repeat(NEWTON_STEPS) {
            val fx = f(x) ?: return null
            if (fx == 0.0) return x
            val h = 1e-7 * max(1.0, abs(x))
            val up = f(x + h) ?: return null
            val down = f(x - h) ?: return null
            val slope = (up - down) / (2 * h)
            if (slope == 0.0 || !slope.isFinite()) return null
            val next = x - fx / slope
            if (!next.isFinite()) return null
            if (abs(next - x) <= 1e-13 * max(1.0, abs(x))) {
                val fn = f(next) ?: return null
                return if (abs(fn) <= 1e-7 * max(1.0, abs(fx) + abs(next))) next else null
            }
            x = next
        }
        return null
    }

    /** Widening scan around the guess for a sign change, then bisection. */
    private fun bracketAndBisect(f: (Double) -> Double?, guess: Double): Double? {
        val start = f(guess)
        val lastX = doubleArrayOf(guess, guess)              // [up, down]
        val lastF = arrayOf(start, start)
        var step = 0.05 * max(1.0, abs(guess))
        repeat(80) {
            for (side in 0..1) {
                val x = if (side == 0) guess + step else guess - step
                val fx = f(x) ?: continue
                if (fx == 0.0) return x
                val previous = lastF[side]
                if (previous != null && previous * fx < 0) return bisect(f, lastX[side], x)
                lastX[side] = x
                lastF[side] = fx
            }
            step *= 1.35
        }
        return null
    }

    private fun bisect(f: (Double) -> Double?, a0: Double, b0: Double): Double? {
        var a = a0
        var b = b0
        var fa = f(a) ?: return null
        repeat(200) {
            val m = (a + b) / 2
            val fm = f(m) ?: return null
            if (fm == 0.0 || abs(b - a) <= 1e-15 * max(1.0, abs(m))) return m
            if (fa * fm < 0) b = m else { a = m; fa = fm }
        }
        return (a + b) / 2
    }

    /** One evaluation run; counts function calls so a runaway ∫ or Σ stops with an error. */
    private class Evaluation(private val context: CalcContext) {
        private var budget = MAX_STEPS

        fun eval(node: CalcNode): Double = eval(node, context)

        private fun eval(node: CalcNode, ctx: CalcContext): Double {
            if (--budget < 0) throw CalcException(CalcErrorKind.MATH)
            val value = when (node) {
                is CalcNode.Num -> node.value
                is CalcNode.Var -> ctx.variable(node.name)
                CalcNode.Ans -> ctx.ans
                CalcNode.Random -> floor(ctx.random() * 1000) / 1000
                is CalcNode.Negate -> -eval(node.operand, ctx)
                is CalcNode.Binary -> binary(node.op, eval(node.left, ctx), eval(node.right, ctx))
                is CalcNode.Postfix -> postfix(node.op, eval(node.operand, ctx), ctx)
                is CalcNode.Call -> call(node, ctx)
                is CalcNode.Equation -> eval(node.left, ctx) - eval(node.right, ctx)
            }
            if (!value.isFinite() || abs(value) >= MAX_MAGNITUDE) math()
            return value
        }

        private fun binary(op: String, l: Double, r: Double): Double = when (op) {
            T.PLUS -> l + r
            T.MINUS -> l - r
            T.TIMES -> l * r
            T.DIVIDE, T.FRACTION -> if (r == 0.0) math() else l / r
            T.POWER -> power(l, r)
            T.XROOT -> root(index = l, radicand = r)
            T.NPR -> permutations(l, r)
            T.NCR -> combinations(l, r)
            else -> math()
        }

        private fun postfix(op: String, x: Double, ctx: CalcContext): Double = when (op) {
            T.SQUARE -> x * x
            T.CUBE -> x * x * x
            T.INVERSE -> if (x == 0.0) math() else 1 / x
            T.FACTORIAL -> factorial(x)
            T.PERCENT -> x / 100
            T.UNIT_DEG -> AngleUnit.DEG.convert(x, ctx.angleUnit)
            T.UNIT_RAD -> AngleUnit.RAD.convert(x, ctx.angleUnit)
            T.UNIT_GRAD -> AngleUnit.GRAD.convert(x, ctx.angleUnit)
            else -> math()
        }

        private fun call(node: CalcNode.Call, ctx: CalcContext): Double {
            val args = node.args
            return when (node.function) {
                T.INTEGRAL -> integrate(args[0], eval(args[1], ctx), eval(args[2], ctx), ctx)
                T.DERIVATIVE -> derivative(args[0], eval(args[1], ctx), ctx)
                T.SUM -> sum(args[0], eval(args[1], ctx), eval(args[2], ctx), ctx)
                else -> function(node.function, args.map { eval(it, ctx) }, ctx.angleUnit)
            }
        }

        private fun function(fn: String, a: List<Double>, unit: AngleUnit): Double {
            val x = a[0]
            return when (fn) {
                T.SIN -> sinOf(x, unit)
                T.COS -> cosOf(x, unit)
                T.TAN -> tanOf(x, unit)
                T.ASIN -> if (abs(x) > 1) math() else unit.fromRadians(asin(x))
                T.ACOS -> if (abs(x) > 1) math() else unit.fromRadians(acos(x))
                T.ATAN -> unit.fromRadians(atan(x))
                T.SINH -> sinh(x)
                T.COSH -> cosh(x)
                T.TANH -> tanh(x)
                T.ASINH -> ln(x + sqrt(x * x + 1))
                T.ACOSH -> if (x < 1) math() else ln(x + sqrt(x * x - 1))
                T.ATANH -> if (abs(x) >= 1) math() else 0.5 * ln((1 + x) / (1 - x))
                T.LOG -> if (a.size == 2) logBase(base = a[0], x = a[1]) else if (x <= 0) math() else log10(x)
                T.LN -> if (x <= 0) math() else ln(x)
                T.POW10 -> power(10.0, x)
                T.EXP -> exp(x)
                T.SQRT -> if (x < 0) math() else sqrt(x)
                T.CBRT -> cbrt(x)
                T.ABS -> abs(x)
                T.RND -> CalcMath.roundSignificant(x, 10)
                else -> math()
            }
        }

        private fun integrate(f: CalcNode, a: Double, b: Double, ctx: CalcContext): Double {
            if (a == b) return 0.0
            val g = { x: Double -> eval(f, ctx.with("X", x)) }
            val fa = g(a)
            val fb = g(b)
            val m = (a + b) / 2
            val fm = g(m)
            val whole = (b - a) / 6 * (fa + 4 * fm + fb)
            val eps = 1e-11 * max(1.0, abs(whole))
            return simpson(g, a, b, fa, fm, fb, whole, eps, depth = 48)
        }

        private fun simpson(
            g: (Double) -> Double, a: Double, b: Double,
            fa: Double, fm: Double, fb: Double, whole: Double, eps: Double, depth: Int,
        ): Double {
            val m = (a + b) / 2
            val lm = (a + m) / 2
            val rm = (m + b) / 2
            val flm = g(lm)
            val frm = g(rm)
            val left = (m - a) / 6 * (fa + 4 * flm + fm)
            val right = (b - m) / 6 * (fm + 4 * frm + fb)
            val delta = left + right - whole
            if (depth <= 0 || abs(delta) <= 15 * eps) return left + right + delta / 15
            return simpson(g, a, m, fa, flm, fm, left, eps / 2, depth - 1) +
                simpson(g, m, b, fm, frm, fb, right, eps / 2, depth - 1)
        }

        /** Central differences with Richardson extrapolation. */
        private fun derivative(f: CalcNode, x0: Double, ctx: CalcContext): Double {
            val g = { x: Double -> eval(f, ctx.with("X", x)) }
            var h = 0.01 * max(1.0, abs(x0))
            val levels = 5
            val table = Array(levels) { DoubleArray(levels) }
            for (i in 0 until levels) {
                table[i][0] = (g(x0 + h) - g(x0 - h)) / (2 * h)
                var factor = 4.0
                for (j in 1..i) {
                    table[i][j] = table[i][j - 1] + (table[i][j - 1] - table[i - 1][j - 1]) / (factor - 1)
                    factor *= 4
                }
                h /= 2
            }
            return table[levels - 1][levels - 1]
        }

        private fun sum(f: CalcNode, from: Double, to: Double, ctx: CalcContext): Double {
            if (!CalcMath.isWhole(from) || !CalcMath.isWhole(to) || to < from || to - from > 1e6) math()
            var total = 0.0
            var i = from
            while (i <= to) {
                total += eval(f, ctx.with("X", i))
                i += 1
            }
            return total
        }

        private fun sinOf(x: Double, unit: AngleUnit): Double {
            val q = x / unit.quarterTurn
            if (CalcMath.isWhole(q)) return when (Math.floorMod(q.toLong(), 4L)) { 1L -> 1.0; 3L -> -1.0; else -> 0.0 }
            return tidy(sin(unit.toRadians(x)))
        }

        private fun cosOf(x: Double, unit: AngleUnit): Double {
            val q = x / unit.quarterTurn
            if (CalcMath.isWhole(q)) return when (Math.floorMod(q.toLong(), 4L)) { 0L -> 1.0; 2L -> -1.0; else -> 0.0 }
            return tidy(cos(unit.toRadians(x)))
        }

        private fun tanOf(x: Double, unit: AngleUnit): Double {
            val q = x / unit.quarterTurn
            if (CalcMath.isWhole(q)) return if (Math.floorMod(q.toLong(), 2L) == 0L) 0.0 else math()
            val r = tan(unit.toRadians(x))
            return if (abs(r) > 1e15) math() else tidy(r)
        }

        private fun tidy(r: Double): Double = if (abs(r) < 1e-15) 0.0 else r

        private fun logBase(base: Double, x: Double): Double =
            if (base <= 0 || base == 1.0 || x <= 0) math() else ln(x) / ln(base)

        private fun power(base: Double, exponent: Double): Double {
            if (base == 0.0 && exponent <= 0) math()
            if (base < 0 && !CalcMath.isWhole(exponent)) {
                // (−8)^(1/3) = −2: allowed when the exponent is p/q with q odd.
                val (p, q) = CalcMath.approximateFraction(exponent, 999, 1e-12) ?: math()
                if (q % 2 == 0L) math()
                val magnitude = (-base).pow(exponent)
                return if (p % 2 == 0L) magnitude else -magnitude
            }
            return base.pow(exponent)
        }

        private fun root(index: Double, radicand: Double): Double {
            if (index == 0.0) math()
            if (radicand < 0) {
                if (!CalcMath.isWhole(index) || Math.floorMod(index.toLong(), 2L) == 0L) math()
                return -(-radicand).pow(1 / index)
            }
            return radicand.pow(1 / index)
        }

        private fun factorial(x: Double): Double {
            if (!CalcMath.isWhole(x) || x < 0 || x > 69) math()
            var r = 1.0
            for (i in 2..x.toInt()) r *= i
            return r
        }

        private fun permutations(n: Double, r: Double): Double {
            checkCombinatorics(n, r)
            var result = 1.0
            var i = n - r + 1
            while (i <= n) { result *= i; i += 1 }
            return result
        }

        private fun combinations(n: Double, r: Double): Double {
            checkCombinatorics(n, r)
            val k = min(r, n - r)
            var result = 1.0
            var i = 1.0
            while (i <= k) {
                result = result * (n - k + i) / i
                i += 1
            }
            return CalcMath.roundSignificant(result, CalcMath.INTERNAL_DIGITS)
        }

        private fun checkCombinatorics(n: Double, r: Double) {
            if (!CalcMath.isWhole(n) || !CalcMath.isWhole(r) || r < 0 || r > n || n >= 1e10) math()
        }

        private fun math(): Nothing = throw CalcException(CalcErrorKind.MATH)
    }

    private companion object {
        const val MAX_MAGNITUDE = 1e100
        const val MAX_STEPS = 2_000_000
        const val NEWTON_STEPS = 100
    }
}
