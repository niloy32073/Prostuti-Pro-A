package com.prostuti.student.ui.screen.playground.graph

import com.prostuti.student.R

/** HSC-level examples, grouped the way the textbook chapters are. */
object GraphExamples {
    val categories: List<GraphExampleCategory> = listOf(
        GraphExampleCategory(
            R.string.graph_ex_cat_basic,
            listOf(
                GraphExample(R.string.graph_ex_line, listOf("y = 2x + 1")),
                GraphExample(R.string.graph_ex_quadratic, listOf("y = x^2 - 4x + 3")),
                GraphExample(R.string.graph_ex_cubic, listOf("y = x^3 - 3x")),
                GraphExample(R.string.graph_ex_abs, listOf("y = |x - 2|")),
                GraphExample(R.string.graph_ex_exponential, listOf("y = e^x", "y = 2^(-x)")),
                GraphExample(R.string.graph_ex_log, listOf("y = ln(x)", "y = log(x)")),
                GraphExample(R.string.graph_ex_rational, listOf("y = 1/(x - 1)")),
                GraphExample(R.string.graph_ex_piecewise, listOf("y = {x < 0: -x, x^2}")),
                GraphExample(R.string.graph_ex_floor, listOf("y = floor(x)")),
            ),
        ),
        GraphExampleCategory(
            R.string.graph_ex_cat_conics,
            listOf(
                GraphExample(R.string.graph_ex_circle, listOf("x^2 + y^2 = 25")),
                GraphExample(R.string.graph_ex_circle_shifted, listOf("(x - 2)^2 + (y + 1)^2 = 9", "(2, -1)")),
                GraphExample(R.string.graph_ex_ellipse, listOf("x^2/16 + y^2/9 = 1")),
                GraphExample(R.string.graph_ex_parabola, listOf("y^2 = 8x")),
                GraphExample(R.string.graph_ex_hyperbola, listOf("x^2/9 - y^2/4 = 1")),
            ),
        ),
        GraphExampleCategory(
            R.string.graph_ex_cat_trig,
            listOf(
                GraphExample(R.string.graph_ex_sine, listOf("y = sin(x)", "y = cos(x)")),
                GraphExample(R.string.graph_ex_amplitude, listOf("y = 2sin(3x)")),
                GraphExample(R.string.graph_ex_tan, listOf("y = tan(x)")),
                GraphExample(R.string.graph_ex_inverse_trig, listOf("y = sin⁻¹(x)", "y = tan⁻¹(x)")),
                GraphExample(R.string.graph_ex_sum_trig, listOf("y = sin(x) + cos(x)")),
            ),
        ),
        GraphExampleCategory(
            R.string.graph_ex_cat_polar,
            listOf(
                GraphExample(R.string.graph_ex_rose, listOf("r = 4cos(3θ)")),
                GraphExample(R.string.graph_ex_cardioid, listOf("r = 2(1 + cos(θ))")),
                GraphExample(R.string.graph_ex_spiral, listOf("r = θ/2")),
                GraphExample(R.string.graph_ex_lemniscate, listOf("r^2 = 9cos(2θ)")),
                GraphExample(R.string.graph_ex_param_circle, listOf("(3cos(t), 3sin(t))")),
                GraphExample(R.string.graph_ex_lissajous, listOf("(4sin(3t), 4sin(2t))")),
                GraphExample(R.string.graph_ex_cycloid, listOf("(t - sin(t), 1 - cos(t))")),
            ),
        ),
        GraphExampleCategory(
            R.string.graph_ex_cat_inequality,
            listOf(
                GraphExample(R.string.graph_ex_ineq_above, listOf("y > x^2 - 4")),
                GraphExample(R.string.graph_ex_ineq_disc, listOf("x^2 + y^2 < 16")),
                GraphExample(R.string.graph_ex_ineq_band, listOf("x - 1 ≤ y ≤ x + 2")),
            ),
        ),
        GraphExampleCategory(
            R.string.graph_ex_cat_calculus,
            listOf(
                GraphExample(R.string.graph_ex_integral, listOf("∫_0^2 x^2 dx")),
                GraphExample(R.string.graph_ex_signed_area, listOf("∫_(-π)^π sin(x) dx")),
                GraphExample(R.string.graph_ex_between, listOf("∫_0^1 (x - x^2) dx")),
                GraphExample(R.string.graph_ex_improper, listOf("∫_0^∞ e^(-x) dx")),
                GraphExample(R.string.graph_ex_antiderivative, listOf("y = cos(x)", "y = ∫_0^x cos(t) dt")),
                GraphExample(R.string.graph_ex_derivative, listOf("f(x) = x^3 - 3x", "y = f'(x)")),
                GraphExample(R.string.graph_ex_series, listOf("Σ_(n=1)^(100) 1/n^2")),
            ),
        ),
        GraphExampleCategory(
            R.string.graph_ex_cat_sliders,
            listOf(
                GraphExample(R.string.graph_ex_slider_wave, listOf("a = 1", "b = 1", "y = a sin(b x)")),
                GraphExample(R.string.graph_ex_slider_quadratic, listOf("a = 1", "b = 0", "c = 0", "y = a x^2 + b x + c")),
                GraphExample(R.string.graph_ex_slider_line, listOf("m = 1", "c = 0", "y = m x + c")),
            ),
        ),
    )

    /** 3D examples: surfaces, solids, curves, vectors and volumes. */
    val spaceCategories: List<GraphExampleCategory> = spaceExamples().map { c -> c.copy(examples = c.examples.map { it.copy(space = true) }) }

    private fun spaceExamples(): List<GraphExampleCategory> = listOf(
        GraphExampleCategory(
            R.string.graph_ex_cat_surfaces,
            listOf(
                GraphExample(R.string.graph_ex_paraboloid, listOf("z = (x^2 + y^2)/4")),
                GraphExample(R.string.graph_ex_saddle, listOf("z = (x^2 - y^2)/4")),
                GraphExample(R.string.graph_ex_ripple, listOf("z = 2sin(sqrt(x^2 + y^2))")),
                GraphExample(R.string.graph_ex_gaussian, listOf("z = 4e^(-(x^2 + y^2)/4)")),
                GraphExample(R.string.graph_ex_plane, listOf("2x + 3y + 2z = 6")),
            ),
        ),
        GraphExampleCategory(
            R.string.graph_ex_cat_solids,
            listOf(
                GraphExample(R.string.graph_ex_sphere, listOf("x^2 + y^2 + z^2 = 9")),
                GraphExample(R.string.graph_ex_ellipsoid, listOf("x^2/16 + y^2/9 + z^2/4 = 1")),
                GraphExample(R.string.graph_ex_cone, listOf("x^2 + y^2 = z^2")),
                GraphExample(R.string.graph_ex_cylinder, listOf("x^2 + y^2 = 4")),
                GraphExample(R.string.graph_ex_hyperboloid, listOf("x^2 + y^2 - z^2 = 1")),
                GraphExample(R.string.graph_ex_torus, listOf("((3 + cos(v))cos(u), (3 + cos(v))sin(u), sin(v))")),
            ),
        ),
        GraphExampleCategory(
            R.string.graph_ex_cat_space_curves,
            listOf(
                GraphExample(R.string.graph_ex_helix, listOf("(2cos(4t), 2sin(4t), t/2)")),
                GraphExample(R.string.graph_ex_line3d, listOf("(1 + 2t, -1 + t, 3t/2)", "(1, -1, 0)")),
                GraphExample(R.string.graph_ex_vectors, listOf("vector((0, 0, 0), (3, 2, 4))", "vector((0, 0, 0), (-2, 3, 1))", "(3, 2, 4)")),
            ),
        ),
        GraphExampleCategory(
            R.string.graph_ex_cat_volumes,
            listOf(
                GraphExample(R.string.graph_ex_double_integral, listOf("∫_0^2 ∫_0^1 (x + y) dy dx")),
                GraphExample(R.string.graph_ex_revolution, listOf("revolvex(sqrt(x), 0, 4)")),
                GraphExample(R.string.graph_ex_washer, listOf("revolvex(x, x^2, 0, 1)")),
                GraphExample(R.string.graph_ex_shell, listOf("revolvey(4 - x^2, 0, 2)")),
            ),
        ),
    )
}
