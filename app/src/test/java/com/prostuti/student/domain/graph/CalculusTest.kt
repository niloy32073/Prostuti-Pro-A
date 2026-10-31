package com.prostuti.student.domain.graph

import com.prostuti.student.domain.calculator.AngleUnit
import com.prostuti.student.domain.graph.GraphTestSupport.assertClose
import com.prostuti.student.domain.graph.GraphTestSupport.explicit
import com.prostuti.student.domain.graph.GraphTestSupport.value
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.E
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

class CalculusTest {

    private fun derivativeAt(body: String, x: Double, angle: AngleUnit = AngleUnit.RAD): Double =
        explicit("y = $body", angle = angle).derivative!!.evaluate(x)

    // ── Differentiation (symbolic, so these are exact to round-off) ────────────────────────

    @Test fun powerProductQuotientChainRules() {
        assertClose(12.0, derivativeAt("x^3", 2.0))
        assertClose(-1.0 / 4, derivativeAt("1/x", 2.0))
        assertClose(sin(2.0) + 2 * cos(2.0), derivativeAt("x sin x", 2.0))
        assertClose(2 * 2.0 * cos(4.0), derivativeAt("sin(x^2)", 2.0))
        assertClose(exp(4.0) * 2, derivativeAt("e^(2x)", 2.0))
        assertClose(ln(2.0) * 8, derivativeAt("2^x", 3.0))
        assertClose(4.0 * (1 + ln(2.0)), derivativeAt("x^x", 2.0)) // u^v with both varying
        assertClose(1 / (2 * sqrt(3.0)), derivativeAt("sqrt(x)", 3.0))
        assertClose(1.0 / 3.0, derivativeAt("x^(1/3)", 1.0))
    }

    @Test fun trigAndInverseTrigRules() {
        assertClose(1 / cos(1.0) / cos(1.0), derivativeAt("tan x", 1.0))
        assertClose(1 / sqrt(1 - 0.25), derivativeAt("asin x", 0.5))
        assertClose(1 / (1 + 4.0), derivativeAt("atan x", 2.0))
        assertClose(-1 / sin(1.0) / sin(1.0), derivativeAt("cot x", 1.0))
        assertClose(1 / (2.0 * ln(10.0)), derivativeAt("log x", 2.0))
        assertClose(1 / (8.0 * ln(2.0)), derivativeAt("log_2(x)", 8.0))
    }

    @Test fun degreeModeDerivativeHasPiOver180() {
        assertClose(PI / 180 * cos(PI / 3), derivativeAt("sin x", 60.0, AngleUnit.DEG))
    }

    @Test fun absPiecewiseAndFactorialDerivatives() {
        assertClose(-1.0, derivativeAt("|x|", -3.0))
        assertClose(2 * 3.0, derivativeAt("{x<0: -x, x^2}", 3.0))
        // x! = Γ(x+1): d/dx at 1 is 1 − γ (Euler–Mascheroni) — numeric fallback, still ~1e-9.
        assertClose(1 - 0.5772156649015329, derivativeAt("x!", 1.0), 1e-7)
    }

    @Test fun primeAndLeibnizNotation() {
        assertClose(12.0, value("f(x) = x^3", "f'(2)"))
        assertClose(12.0, value("f(x) = x^3", "f''(2)"))
        assertClose(12.0, explicit("y = d/dx(x^3)").f.evaluate(2.0))
        assertClose(12.0, explicit("y = d^2/dx^2(x^3)").f.evaluate(2.0))
    }

    @Test fun fundamentalTheoremOfCalculus() {
        // d/dx ∫_0^x cos t dt = cos x
        assertClose(cos(1.2), derivativeAt("∫_0^x cos(t) dt", 1.2))
        // d/dx ∫_0^{x²} t dt = x²·2x
        assertClose(2.0 * 2.0 * 2.0 * 2.0, derivativeAt("∫_0^(x^2) t dt", 2.0), 1e-8)
        // integrand depends on x too (Leibniz): ∫_0^1 x t dt = x/2
        assertClose(0.5, derivativeAt("∫_0^1 x t dt", 2.0), 1e-10)
    }

    // ── Integration ─────────────────────────────────────────────────────────────────────

    @Test fun definiteIntegralsMatchClosedForms() {
        assertClose(8.0 / 3, value("∫_0^2 x^2 dx"), 1e-12)
        assertClose(2.0, value("∫_0^π sin(x) dx"), 1e-12)
        assertClose(E - 1, value("∫_0^1 e^x dx"), 1e-12)
        assertClose(ln(2.0), value("∫_1^2 1/x dx"), 1e-12)
        assertClose(-8.0 / 3, value("∫_2^0 x^2 dx"), 1e-12) // reversed bounds flip the sign
        assertClose(PI / 4, value("∫_0^1 sqrt(1 - x^2) dx"), 1e-9) // quarter circle, √ singular slope at 1
        assertClose(2.0, value("∫_0^1 1/sqrt(x) dx"), 1e-6) // integrable end-point singularity
    }

    @Test fun improperIntegralsWithInfiniteBounds() {
        assertClose(1.0, value("∫_0^∞ e^(-x) dx"), 1e-9)
        assertClose(sqrt(PI), value("∫_(-∞)^∞ e^(-x^2) dx"), 1e-9)
        assertClose(PI / 2, value("∫_0^∞ 1/(1+x^2) dx"), 1e-9)
    }

    @Test fun poleInsideIntervalIsUndefined() {
        assertTrue(value("∫_(-1)^1 1/x dx").isNaN())
    }

    @Test fun antiderivativeWithoutBoundsStartsAtZero() {
        assertClose(8.0 / 3, explicit("y = ∫ x^2 dx").f.evaluate(2.0), 1e-12)
    }

    @Test fun nestedIntegrals() {
        // ∫_0^1 ∫_0^x y dy dx = ∫_0^1 x²/2 dx = 1/6
        assertClose(1.0 / 6, value("∫_0^1 ∫_0^x y dy dx"), 1e-10)
    }

    @Test fun integratorReportsConvergence() {
        val ok = Integrator.integrate({ x -> x * x }, 0.0, 3.0)
        assertTrue(ok.converged)
        assertClose(9.0, ok.value, 1e-13)
        val rough = Integrator.integrate({ x -> sin(1 / x) / x }, 1e-9, 1.0, maxIntervals = 50)
        assertFalse(rough.converged)
    }

    // ── Roots ───────────────────────────────────────────────────────────────────────────

    @Test fun brentFindsRootsPrecisely() {
        assertClose(sqrt(2.0), RootFinder.brent({ x -> x * x - 2 }, 0.0, 2.0)!!, 1e-14)
        assertClose(PI, RootFinder.brent({ x -> sin(x) }, 3.0, 4.0)!!, 1e-14)
        assertClose(0.7390851332151607, RootFinder.brent({ x -> cos(x) - x }, 0.0, 1.0)!!, 1e-14)
        assertTrue(RootFinder.brent({ x -> x * x + 1 }, -1.0, 1.0) == null)
    }

    @Test fun goldenSectionMinimises() {
        assertClose(3.0, RootFinder.minimize({ x -> (x - 3) * (x - 3) }, 0.0, 10.0), 1e-7)
    }
}
