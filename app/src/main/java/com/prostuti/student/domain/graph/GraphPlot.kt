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

    /** Arrows of (P(x, y), Q(x, y)) on a grid: electric, magnetic and velocity fields. */
    class VectorField(val p: CompiledFunction, val q: CompiledFunction) : GraphPlot

    /** Data points and their best-fit curve (null when no fit was asked for or possible). */
    class Table(val xs: DoubleArray, val ys: DoubleArray, val fit: ExplicitY?, val result: RegressionResult?) : GraphPlot

    /** An arrow from (x0, y0) to (x1, y1) in the plane. */
    class Vector2(val x0: Double, val y0: Double, val x1: Double, val y1: Double) : GraphPlot

    // ── 3D ──────────────────────────────────────────────────────────────────────────────────

    /**
     * A surface solved for [axis]: z = f(x, y), x = f(y, z) or y = f(x, z). [f]'s two inputs are
     * the other axes in x, y, z order; [partials] are ∂f/∂ of each input (null when unavailable).
     */
    class Surface(val axis: Axis3, val f: CompiledFunction, val partials: List<CompiledFunction>?) : GraphPlot

    /** F(x, y, z) = 0, meshed by marching tetrahedra. [gradient] = (∂F/∂x, ∂F/∂y, ∂F/∂z) when exact. */
    class ImplicitSurface(val field: CompiledFunction, val gradient: List<CompiledFunction>?) : GraphPlot

    /** (x(u, v), y(u, v), z(u, v)). */
    class ParametricSurface(
        val x: CompiledFunction,
        val y: CompiledFunction,
        val z: CompiledFunction,
        val uRange: ClosedFloatingPointRange<Double>,
        val vRange: ClosedFloatingPointRange<Double>,
    ) : GraphPlot

    /** (x(t), y(t), z(t)). */
    class SpaceCurve(
        val x: CompiledFunction,
        val y: CompiledFunction,
        val z: CompiledFunction,
        val defaultRange: ClosedFloatingPointRange<Double>,
    ) : GraphPlot

    class Point3(val x: Double, val y: Double, val z: Double) : GraphPlot

    class Vector3(val from: DoubleArray, val to: DoubleArray) : GraphPlot

    /**
     * ∫∫ f(x, y) dA as the solid under z = f over the region: the outer variable runs over
     * [outerMin, outerMax], the inner one between [innerMin] and [innerMax] (functions of the outer).
     */
    class VolumeUnder(
        val f: CompiledFunction,
        val outerIsX: Boolean,
        val outerMin: Double,
        val outerMax: Double,
        val innerMin: CompiledFunction,
        val innerMax: CompiledFunction,
        val value: Double,
    ) : GraphPlot

    /**
     * The solid from turning the region between y = [top] and y = [bottom] (or the x-axis) for
     * a ≤ x ≤ b about [axis] (X or Y). [volume] is π∫(f² − g²)dx about x, 2π∫|x|(f − g)dx about y.
     */
    class Revolution(
        val axis: Axis3,
        val top: CompiledFunction,
        val bottom: CompiledFunction?,
        val a: Double,
        val b: Double,
        val volume: IntegralResult,
    ) : GraphPlot
}
