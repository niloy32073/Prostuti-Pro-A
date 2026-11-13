package com.prostuti.student.domain.graph

import com.prostuti.student.domain.graph.GraphTestSupport.assertClose
import com.prostuti.student.domain.graph.GraphTestSupport.explicit
import org.junit.Test

class MathPrinterTest {

    /** parse → print → parse must mean the same function. */
    private fun roundTrip(text: String) {
        val node = (MathParser.parse("y = $text") as ParseResult.Success).statement.operands[1]
        val printed = MathPrinter.print(node)
        val original = explicit("y = $text").f
        val reparsed = explicit("y = $printed").f
        for (x in listOf(-2.3, -0.5, 0.7, 1.9, 3.1)) {
            val a = original.evaluate(x)
            val b = reparsed.evaluate(x)
            if (a.isNaN()) assertClose(Double.NaN, b, message = printed) else assertClose(a, b, 1e-12, printed)
        }
    }

    @Test fun printedTextMeansTheSame() {
        listOf(
            "x^2 - 3x + 1", "-x^2", "(x + 1)^2", "2^x^2", "x^(-1)", "1/(x + 2)", "x/2x", "sin(x)^2", "sin^2 x",
            "asin(x/4)", "|x - 1|", "sqrt(x^2 + 1)", "log_2(x^2 + 1)", "ln|x| + e^x", "x!", "{x < 0: -x, x^2}",
            "x^2 {-1 ≤ x ≤ 2}", "∫_0^x cos(t) dt", "d/dx(x^3)", "Σ_(n=1)^(5) x^n/n", "nCr(5, 2) x", "-(-x)", "(-2)^2 x",
            "cbrt(x - 4)", "floor(x) + ceil(x)",
        ).forEach(::roundTrip)
    }
}
