package com.prostuti.student

import com.prostuti.student.ui.component.LatexNormalizer
import org.junit.Assert.assertEquals
import org.junit.Test

class LatexNormalizerTest {

    private fun n(s: String) = LatexNormalizer.normalize(s)

    @Test fun fractionShorthand() {
        assertEquals("\\dfrac{1}{2}", n("\\dfrac12"))
        assertEquals("\\frac{1}{2}", n("\\frac 1 2"))
        assertEquals("\\frac{\\pi}{4}", n("\\frac\\pi4"))
        assertEquals("\\frac{a+b}{2}", n("\\frac{a+b}2"))
        assertEquals("\\tfrac{3}{4}x", n("\\tfrac34x"))
    }

    @Test fun alreadyBracedIsUnchanged() {
        val s = "\\frac{1}{2} + \\sqrt{x^{2}+y_{1}}"
        assertEquals(s, n(s))
    }

    @Test fun nestedFractions() {
        assertEquals("\\frac{\\frac{1}{2}}{3}", n("\\frac{\\frac12}3"))
    }

    @Test fun scriptsTakeOneToken() {
        assertEquals("x^{2}3", n("x^23"))
        assertEquals("a_{n}+1", n("a_n+1"))
        assertEquals("e^{\\pi}", n("e^\\pi"))
    }

    @Test fun sqrtWithIndex() {
        assertEquals("\\sqrt[3]{8}", n("\\sqrt[3]8"))
        assertEquals("\\sqrt{2}x", n("\\sqrt2x"))
    }

    @Test fun controlSpaceBecomesThickSpace() {
        assertEquals("a,\\;b", n("a,\\ b"))
    }

    @Test fun otherCommandsUntouched() {
        assertEquals("\\left( x \\right) \\\\ \\cdot", n("\\left( x \\right) \\\\ \\cdot"))
        assertEquals("plain text", n("plain text"))
    }

    @Test fun unsupportedEquilibriumArrowIsRewritten() {
        assertEquals(
            "2SO_{2}(g)+O_{2}(g)\\;\\mathrm{⇌}\\; 2SO_{3}(g)",
            n("2SO_2(g)+O_2(g)\\rightleftharpoons 2SO_3(g)")
        )
        // A longer, unrelated command sharing the same prefix must be left alone.
        assertEquals("\\rightleftharpoonsfoo", n("\\rightleftharpoonsfoo"))
    }

    @Test fun splitOnPercentHandlesEscapedAndBareForms() {
        assertEquals(listOf("5", " Na_2CO_3"), LatexNormalizer.splitOnPercent("5\\% Na_2CO_3"))
        assertEquals(listOf("5", " Na_2CO_3"), LatexNormalizer.splitOnPercent("5% Na_2CO_3"))
        assertEquals(listOf("x^2"), LatexNormalizer.splitOnPercent("x^2"))
        assertEquals(listOf("a", "b", "c"), LatexNormalizer.splitOnPercent("a\\%b%c"))
    }

    @Test fun splitOnPercentKeepsGroupsBalanced() {
        val s = LatexNormalizer::splitOnPercent
        assertEquals(listOf("\\text{10}", " NaCl"), s("\\text{10\\%} NaCl"))
        assertEquals(listOf("\\ce{5}", "\\ce{ NaCl}"), s("\\ce{5\\% NaCl}"))
        assertEquals(listOf("\\mathrm{\\mathbf{a}}", "\\mathrm{\\mathbf{b}}"), s("\\mathrm{\\mathbf{a\\%b}}"))
        assertEquals(listOf("{a}", "{b}"), s("{a\\%b}"))
        // Groups the split would leave empty are dropped.
        assertEquals(listOf("5", ""), s("5\\text{\\%}"))
        // Author-written empty groups survive.
        assertEquals(listOf("{}^{14}C", ""), s("{}^{14}C\\%"))
        // `\\` is a line break, so the `%` after it is a separate (bare) one.
        assertEquals(listOf("a\\\\", "b"), s("a\\\\%b"))
        // Escaped braces aren't groups.
        assertEquals(listOf("\\{5", "\\}"), s("\\{5\\%\\}"))
    }

    // ── Name operators (\lim, \max, …) ──────────────────────────────────────────────────
    // The renderer draws these like ∑ (bigger, lighter, limits underneath even inline),
    // so they are rebuilt the way TeX draws them.

    private fun d(s: String) = LatexNormalizer.normalize(s, display = true)

    @Test fun inlineLimitGoesBesideTheOperator() {
        assertEquals("\\mathrm{lim}_{x\\to a}\\, f(x) = l", n("\\lim_{x\\to a} f(x) = l"))
        // The screenshot's question: no thin space before an opening bracket.
        assertEquals("\\mathrm{lim}_{x\\to a}[f(x)-g(x)]", n("\\lim_{x\\to a}[f(x)-g(x)]"))
        assertEquals("\\mathrm{lim}_{x\\to a}\\,\\frac{g(x)}{f(x)}", n("\\lim_{x\\to a}\\frac{g(x)}{f(x)}"))
    }

    @Test fun displayLimitGoesUnderneath() {
        assertEquals("\\underset{x\\to a}{\\mathrm{lim}}\\, f(x)", d("\\lim_{x\\to a} f(x)"))
        assertEquals("\\displaystyle\\underset{n}{\\mathrm{max}}\\, a_{n}", n("\\displaystyle\\max_n a_n"))
    }

    @Test fun limitsAndNolimitsOverrideTheStyle() {
        assertEquals("\\underset{x\\to 0}{\\mathrm{lim}}", n("\\lim\\limits_{x\\to 0}"))
        assertEquals("\\mathrm{lim}_{x\\to 0}", d("\\lim\\nolimits_{x\\to 0}"))
    }

    @Test fun bothScriptsAndBraceLessScript() {
        assertEquals("\\mathrm{sup}_{n}^{k}", n("\\sup_n^k"))
        assertEquals("\\overset{k}{\\underset{n}{\\mathrm{sup}}}", d("\\sup_n^k"))
    }

    @Test fun operatorWithoutLimitsUsesOperatorname() {
        assertEquals("\\operatorname{max}(a,b)", n("\\max(a,b)"))
        assertEquals("\\operatorname{det} A", n("\\det A"))
        assertEquals("\\operatorname{lim}\\operatorname{sup} x_{n}", n("\\limsup x_n"))
    }

    @Test fun thinSpaceBeforeAScriptedOperatorAfterAnOrdinary() {
        assertEquals("2\\,\\mathrm{lim}_{x}\\, f", n("2\\lim_x f"))
        assertEquals("a=\\mathrm{lim}_{x}\\, f", n("a=\\lim_x f"))
    }

    @Test fun otherCommandsStartingWithLimAreUntouched() {
        assertEquals("\\limits", n("\\limits"))
        assertEquals("\\mathrm{lim}_{n}\\,\\mathrm{min}_{k}\\, x", n("\\lim_n\\min_k x"))
    }
}
