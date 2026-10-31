package com.prostuti.student.domain.graph

/** One compiled node. Implementations are tiny final classes so ART can inline the hot calls. */
internal fun interface Eval {
    fun eval(env: DoubleArray): Double

    class Const(val value: Double) : Eval {
        override fun eval(env: DoubleArray): Double = value
    }

    class Slot(private val index: Int) : Eval {
        override fun eval(env: DoubleArray): Double = env[index]
    }
}

/** A compiled condition chain such as 0 < x < 2. */
internal fun interface CondEval {
    fun test(env: DoubleArray): Boolean
}
