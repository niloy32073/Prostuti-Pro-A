package com.prostuti.student.domain.graph

import com.prostuti.student.domain.calculator.AngleUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail

/** Small helpers so each engine test reads like the math it checks. */
internal object GraphTestSupport {

    fun compileLines(vararg lines: String, angle: AngleUnit = AngleUnit.RAD): CompiledGraph =
        GraphCompiler().compile(lines.mapIndexed { i, t -> GraphLineInput(i.toLong(), t) }, angle)

    /** Value of the last line (earlier lines may define parameters/functions). */
    fun value(vararg lines: String, angle: AngleUnit = AngleUnit.RAD): Double {
        val line = compileLines(*lines, angle = angle).lines.last()
        line.error?.let { fail("unexpected error ${it.kind} ${it.names} for '${lines.last()}'") }
        return line.value ?: throw AssertionError("line '${lines.last()}' has no value (kind ${line.kind})")
    }

    /** y = f(x) of the last line, as a callable. */
    fun explicit(vararg lines: String, angle: AngleUnit = AngleUnit.RAD): GraphPlot.ExplicitY {
        val line = compileLines(*lines, angle = angle).lines.last()
        line.error?.let { fail("unexpected error ${it.kind} ${it.names} for '${lines.last()}'") }
        return line.plot as? GraphPlot.ExplicitY ?: throw AssertionError("not explicit: ${line.kind}")
    }

    fun errorFor(vararg lines: String): GraphError =
        compileLines(*lines).lines.last().error ?: throw AssertionError("expected an error for '${lines.last()}'")

    fun assertClose(expected: Double, actual: Double, tolerance: Double = 1e-9, message: String = "") {
        if (expected.isNaN()) {
            assertTrue("$message expected NaN but was $actual", actual.isNaN())
            return
        }
        assertEquals(message, expected, actual, tolerance * maxOf(1.0, kotlin.math.abs(expected)))
    }
}
