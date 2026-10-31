package com.prostuti.student.domain.graph

import org.junit.Assert.assertEquals
import org.junit.Test

class LatexWriterTest {

    private fun latex(text: String, functions: Set<String> = emptySet()) =
        LatexWriter.write((MathParser.parse(text, functions) as ParseResult.Success).statement)

    @Test fun typesetsCommonForms() {
        assertEquals("y = x^{2}", latex("y = x^2"))
        assertEquals("y = \\frac{1}{x}", latex("y = 1/x"))
        assertEquals("y = \\sqrt{x+1}", latex("y = sqrt(x+1)"))
        assertEquals("y = 2 x", latex("y = 2x"))
        assertEquals("2\\cdot 3", latex("2*3"))
        assertEquals("y = \\sin(x)", latex("y = sin x"))
        assertEquals("\\sin^{-1}(x)", latex("arcsin x"))
        assertEquals("\\left|x\\right|", latex("|x|"))
        assertEquals("x^{2}+y^{2} = 25", latex("x^2+y^2=25"))
        assertEquals("y \\ge x", latex("y >= x"))
        assertEquals("r = 1+\\cos(\\theta)", latex("r = 1 + cos θ"))
    }

    @Test fun calculusAndPiecewise() {
        assertEquals("\\int_{0}^{2}x^{2}\\,dx", latex("∫_0^2 x^2 dx"))
        assertEquals("\\frac{d}{dx}(x^{3})", latex("d/dx(x^3)"))
        assertEquals("\\sum_{n=1}^{10}n^{2}", latex("Σ_(n=1)^10 n^2"))
        assertEquals("f'(x)", latex("f'(x)", setOf("f")))
        assertEquals(
            "\\begin{cases}-x & x<0\\\\x & \\text{otherwise}\\end{cases}",
            latex("{x<0: -x, x}"),
        )
        assertEquals("x^{2} \\left\\{0<x<2\\right\\}", latex("x^2 {0<x<2}"))
    }

    @Test fun negativeFactorsGetBrackets() {
        assertEquals("x (-2)", latex("x(-2)"))
    }

    @Test fun tallArgumentsGetStretchyBrackets() {
        assertEquals("\\sin\\left(\\frac{x}{2}\\right)", latex("sin(x/2)"))
    }

    @Test fun greekAndSubscripts() {
        assertEquals("a_{1}+\\pi", latex("a_1 + π"))
    }
}
