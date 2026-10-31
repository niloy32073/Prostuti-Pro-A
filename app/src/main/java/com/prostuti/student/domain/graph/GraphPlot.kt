package com.prostuti.student.domain.graph

/** What a line draws, with its compiled functions. Variable slot 0 is the plotted input. */
sealed interface GraphPlot {
    /** y = f(x). [derivative] is f'(x), used for tangents, extrema and trace slopes. */
    class ExplicitY(val f: CompiledFunction, val derivative: CompiledFunction?) : GraphPlot

    /** x = g(y). */
    class ExplicitX(val g: CompiledFunction) : GraphPlot

    /** y [relation] f(x), e.g. y > x². */
    class ExplicitYInequality(val f: CompiledFunction, val relation: Relation) : GraphPlot

    /** x [relation] g(y). */
    class ExplicitXInequality(val g: CompiledFunction, val relation: Relation) : GraphPlot

    /** r = f(θ); θ is in the current angle unit, [toRadians] converts it for x = r cos θ. */
    class Polar(val r: CompiledFunction, val toRadians: Double, val defaultRange: ClosedFloatingPointRange<Double>) : GraphPlot

    /** (x(t), y(t)). */
    class Parametric(val x: CompiledFunction, val y: CompiledFunction, val defaultRange: ClosedFloatingPointRange<Double>) : GraphPlot

    /** F(x, y) = 0. */
    class Implicit(val field: CompiledFunction) : GraphPlot

    /** The set where field(x, y) < 0 (≤ 0 when not [strict]); the boundary is field = 0. */
    class Region(val field: CompiledFunction, val strict: Boolean) : GraphPlot

    class Point(val x: Double, val y: Double) : GraphPlot

    /**
     * ∫_a^b of [top] (minus [bottom] when the integrand was f − g): shaded between the curve and the
     * x-axis, or between the two curves. Variable slot 0 of both functions is the x-axis.
     */
    class IntegralArea(
        val top: CompiledFunction,
        val bottom: CompiledFunction?,
        val lower: Double,
        val upper: Double,
        val result: IntegralResult,
    ) : GraphPlot
}
