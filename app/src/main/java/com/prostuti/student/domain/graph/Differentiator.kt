package com.prostuti.student.domain.graph

import com.prostuti.student.domain.calculator.AngleUnit
import kotlin.math.PI
import kotlin.math.ln
import com.prostuti.student.domain.graph.MathNode.Bin
import com.prostuti.student.domain.graph.MathNode.Call
import com.prostuti.student.domain.graph.MathNode.Neg
import com.prostuti.student.domain.graph.MathNode.Num

/**
 * Symbolic differentiation, so f'(x), tangents, extrema and d/dx are exact instead of
 * finite-difference guesses. Trig rules include the degree factor π/180 when angles are in degrees.
 * Where no closed rule exists (Γ, x!, sums with moving bounds) it falls back to [MathNode.NumericDiff].
 */
internal class Differentiator(private val angleUnit: AngleUnit) {

    private var fresh = 0

    /** Replaces every [MathNode.Diff] in [node] by its derivative (innermost first). */
    fun expand(node: MathNode): MathNode = MathTree.rewrite(node) { n ->
        if (n is MathNode.Diff) derivative(n.body, n.variable) else n
    }

    /** d[node]/d[v], simplified. [node] may still contain Diff nodes. */
    fun derivative(node: MathNode, v: String): MathNode = Simplifier.simplify(d(expand(node), v))

    /** Angle factor for trig: d/dx sin(x°) = (π/180)·cos(x°). */
    private val k: Double = if (angleUnit == AngleUnit.DEG) PI / 180.0 else 1.0

    private fun d(node: MathNode, v: String): MathNode {
        if (!MathTree.dependsOn(node, v)) return Num(0.0)
        return when (node) {
            is Num -> Num(0.0)
            is MathNode.Sym -> Num(if (node.name == v) 1.0 else 0.0)
            is Neg -> Neg(d(node.operand, v))
            is Bin -> bin(node, v)
            is Call -> call(node, v)
            is MathNode.Piecewise -> MathNode.Piecewise(
                node.branches.map { MathNode.Branch(it.condition, d(it.value, v)) },
                node.otherwise?.let { d(it, v) },
            )
            is MathNode.Let -> {
                // d/dv body(p = e) = ∂body/∂p · e' + ∂body/∂v
                val inner = add(mul(d(node.body, node.name), d(node.value, v)), d(node.body, v))
                MathNode.Let(node.name, node.value, inner)
            }
            is MathNode.Integral -> integral(node, v)
            is MathNode.Series -> {
                val boundsMove = MathTree.dependsOn(node.from, v) || MathTree.dependsOn(node.to, v)
                if (node.product || boundsMove) {
                    MathNode.NumericDiff(v, node)
                } else {
                    MathNode.Series(node.variable, node.from, node.to, d(node.body, v), product = false)
                }
            }
            is MathNode.Diff -> d(derivative(node.body, node.variable), v)
            is MathNode.NumericDiff -> MathNode.NumericDiff(v, node)
            is MathNode.Tuple -> MathNode.NumericDiff(v, node)
        }
    }

    /** Leibniz rule: d/dv ∫_a(v)^b(v) g(t, v) dt = g(b)·b' − g(a)·a' + ∫_a^b ∂g/∂v dt. */
    private fun integral(node: MathNode.Integral, v: String): MathNode {
        val t = node.variable
        val atUpper = bindAt(node, node.upper)
        val atLower = bindAt(node, node.lower)
        var result: MathNode = sub(mul(atUpper, d(node.upper, v)), mul(atLower, d(node.lower, v)))
        if (t != v && MathTree.dependsOn(node.body, v)) {
            result = add(result, MathNode.Integral(t, node.lower, node.upper, d(node.body, v)))
        }
        return result
    }

    private fun bindAt(node: MathNode.Integral, at: MathNode): MathNode {
        val name = "${node.variable}#d${fresh++}"
        return MathNode.Let(name, at, MathTree.renameFree(node.body, node.variable, name))
    }

    private fun bin(node: Bin, v: String): MathNode {
        val u = node.left
        val w = node.right
        return when (node.op) {
            BinOp.ADD -> add(d(u, v), d(w, v))
            BinOp.SUB -> sub(d(u, v), d(w, v))
            BinOp.MUL -> add(mul(d(u, v), w), mul(u, d(w, v)))
            BinOp.DIV -> Bin(BinOp.DIV, sub(mul(d(u, v), w), mul(u, d(w, v))), Bin(BinOp.POW, w, Num(2.0)))
            BinOp.POW -> {
                val baseVaries = MathTree.dependsOn(u, v)
                val expVaries = MathTree.dependsOn(w, v)
                when {
                    !expVaries -> mul(mul(w, Bin(BinOp.POW, u, sub(w, Num(1.0)))), d(u, v))
                    !baseVaries -> mul(mul(node, call1("ln", u)), d(w, v))
                    // u^w · (w'·ln u + w·u'/u)
                    else -> mul(node, add(mul(d(w, v), call1("ln", u)), Bin(BinOp.DIV, mul(w, d(u, v)), u)))
                }
            }
        }
    }

    private fun call(node: Call, v: String): MathNode {
        val args = node.args
        val u = args.firstOrNull() ?: return Num(0.0)
        val du by lazy { d(u, v) }
        fun chain(outer: MathNode) = mul(outer, du)
        val kn = Num(k)
        val invK = Num(1.0 / k)
        return when (node.name) {
            "sin" -> chain(mul(kn, call1("cos", u)))
            "cos" -> chain(Neg(mul(kn, call1("sin", u))))
            "tan" -> chain(mul(kn, Bin(BinOp.POW, call1("sec", u), Num(2.0))))
            "sec" -> chain(mul(kn, mul(call1("sec", u), call1("tan", u))))
            "csc" -> chain(Neg(mul(kn, mul(call1("csc", u), call1("cot", u)))))
            "cot" -> chain(Neg(mul(kn, Bin(BinOp.POW, call1("csc", u), Num(2.0)))))
            "asin" -> chain(div(invK, call1("sqrt", sub(Num(1.0), sq(u)))))
            "acos" -> chain(Neg(div(invK, call1("sqrt", sub(Num(1.0), sq(u))))))
            "atan" -> chain(div(invK, add(Num(1.0), sq(u))))
            "acot" -> chain(Neg(div(invK, add(Num(1.0), sq(u)))))
            "asec" -> chain(div(invK, mul(call1("abs", u), call1("sqrt", sub(sq(u), Num(1.0))))))
            "acsc" -> chain(Neg(div(invK, mul(call1("abs", u), call1("sqrt", sub(sq(u), Num(1.0)))))))
            "sinh" -> chain(call1("cosh", u))
            "cosh" -> chain(call1("sinh", u))
            "tanh" -> chain(Bin(BinOp.POW, call1("sech", u), Num(2.0)))
            "sech" -> chain(Neg(mul(call1("sech", u), call1("tanh", u))))
            "csch" -> chain(Neg(mul(call1("csch", u), call1("coth", u))))
            "coth" -> chain(Neg(Bin(BinOp.POW, call1("csch", u), Num(2.0))))
            "asinh" -> chain(div(Num(1.0), call1("sqrt", add(sq(u), Num(1.0)))))
            "acosh" -> chain(div(Num(1.0), call1("sqrt", sub(sq(u), Num(1.0)))))
            "atanh" -> chain(div(Num(1.0), sub(Num(1.0), sq(u))))
            "ln" -> chain(div(Num(1.0), u))
            "log" -> if (args.size == 2) {
                d(div(call1("ln", u), call1("ln", args[1])), v)
            } else {
                chain(div(Num(1.0), mul(u, Num(ln(10.0)))))
            }
            "exp" -> chain(node)
            "sqrt" -> chain(div(Num(1.0), mul(Num(2.0), node)))
            "cbrt" -> chain(div(Num(1.0), mul(Num(3.0), Bin(BinOp.POW, node, Num(2.0)))))
            "nthroot" -> d(Bin(BinOp.POW, u, div(Num(1.0), args[1])), v)
            "abs" -> chain(call1("sign", u))
            "sign", "floor", "ceil", "round", "gcd", "lcm", "nCr", "nPr" -> Num(0.0)
            "mod" -> sub(du, mul(call1("floor", div(u, args[1])), d(args[1], v)))
            "min", "max" -> if (args.size == 2) {
                val rel = if (node.name == "min") Relation.LT else Relation.GT
                MathNode.Piecewise(
                    listOf(MathNode.Branch(MathNode.Condition(listOf(args[0], args[1]), listOf(rel)), d(args[0], v))),
                    d(args[1], v),
                )
            } else {
                MathNode.NumericDiff(v, node)
            }
            else -> MathNode.NumericDiff(v, node) // factorial, gamma, user calls left un-inlined
        }
    }

    private fun call1(name: String, arg: MathNode) = Call(name, listOf(arg))
    private fun sq(u: MathNode) = Bin(BinOp.POW, u, Num(2.0))
    private fun add(a: MathNode, b: MathNode) = Bin(BinOp.ADD, a, b)
    private fun sub(a: MathNode, b: MathNode) = Bin(BinOp.SUB, a, b)
    private fun mul(a: MathNode, b: MathNode) = Bin(BinOp.MUL, a, b)
    private fun div(a: MathNode, b: MathNode) = Bin(BinOp.DIV, a, b)
}
