package com.prostuti.student.domain.graph

/**
 * A compiled expression. Variables live in a `DoubleArray` "environment": the first slots are the
 * [variables] passed to the compiler (x, y, t, θ…), the rest are scratch slots for ∫/Σ/Let.
 * One environment per thread — [newEnv] is cheap, so make one per plotting job.
 */
class CompiledFunction internal constructor(
    private val root: Eval,
    val slotCount: Int,
    val variables: List<String>,
) {
    fun newEnv(): DoubleArray = DoubleArray(slotCount)

    fun eval(env: DoubleArray): Double = root.eval(env)

    /** True when the function ignores every variable — it was folded to one number. */
    val isConstant: Boolean get() = root is Eval.Const

    /** Convenience for one-off evaluations (allocates). */
    fun evaluate(vararg values: Double): Double {
        val env = newEnv()
        values.forEachIndexed { i, v -> env[i] = v }
        return root.eval(env)
    }

    /** f(x) with every other variable at its current env value. */
    fun asFunctionOf(slot: Int, env: DoubleArray = newEnv()): RealFunction = RealFunction { v ->
        env[slot] = v
        root.eval(env)
    }
}
