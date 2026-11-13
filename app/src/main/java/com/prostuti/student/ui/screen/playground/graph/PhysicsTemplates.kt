package com.prostuti.student.ui.screen.playground.graph

import com.prostuti.student.R
import com.prostuti.student.domain.graph.RegressionModel

/**
 * HSC physics as live graphs. Each template writes its formula with sliders for the quantities a
 * student would change, a note saying what the axes and sliders mean, and a window that frames it.
 * Angles are sliders in degrees (απ/180 in the formula), the way the textbook states them.
 */
object PhysicsTemplates {

    private fun s(min: Double, max: Double, step: Double) = SliderRange(min, max, step)

    val categories: List<GraphExampleCategory> = listOf(
        GraphExampleCategory(
            R.string.phys_cat_motion,
            listOf(
                GraphExample(
                    R.string.phys_projectile,
                    listOf(
                        "v_0 = 20", "α = 45", "g = 9.8",
                        "R = v_0^2 sin(2απ/180)/g",
                        "H = (v_0 sin(απ/180))^2/(2g)",
                        "y = x tan(απ/180) - g x^2/(2 v_0^2 cos(απ/180)^2) {0 ≤ x ≤ R}",
                        "(R/2, H)", "(R, 0)",
                    ),
                    noteRes = R.string.phys_projectile_note,
                    sliders = mapOf("v_0" to s(0.0, 60.0, 0.5), "α" to s(0.0, 90.0, 1.0), "g" to s(1.0, 25.0, 0.1)),
                    window = listOf(-5.0, 50.0, -5.0, 30.0),
                ),
                GraphExample(
                    R.string.phys_vt_area,
                    listOf("u = 5", "a = 2", "T = 4", "y = u + a x {x ≥ 0}", "∫_0^T (u + a x) dx"),
                    noteRes = R.string.phys_vt_area_note,
                    sliders = mapOf("u" to s(0.0, 20.0, 0.5), "a" to s(-5.0, 5.0, 0.1), "T" to s(0.0, 10.0, 0.1)),
                    window = listOf(-1.0, 12.0, -10.0, 30.0),
                ),
                GraphExample(
                    R.string.phys_free_fall,
                    listOf("g = 9.8", "y = g x^2/2 {x ≥ 0}", "y = g x {x ≥ 0}"),
                    noteRes = R.string.phys_free_fall_note,
                    sliders = mapOf("g" to s(1.0, 25.0, 0.1)),
                    window = listOf(-0.5, 5.0, -5.0, 60.0),
                ),
                GraphExample(
                    R.string.phys_circular,
                    listOf(
                        "R = 3", "ω = 1", "T = 0",
                        "(R cos(t), R sin(t))",
                        "(R cos(ωT), R sin(ωT))",
                        "vector((R cos(ωT), R sin(ωT)), (R cos(ωT) - 1.5 sin(ωT), R sin(ωT) + 1.5 cos(ωT)))",
                        "vector((R cos(ωT), R sin(ωT)), ((R - 1.5) cos(ωT), (R - 1.5) sin(ωT)))",
                    ),
                    noteRes = R.string.phys_circular_note,
                    sliders = mapOf("R" to s(1.0, 5.0, 0.1), "ω" to s(0.1, 3.0, 0.1), "T" to s(0.0, 20.0, 0.05)),
                    window = listOf(-6.0, 6.0, -6.0, 6.0),
                ),
            ),
        ),
        GraphExampleCategory(
            R.string.phys_cat_waves,
            listOf(
                GraphExample(
                    R.string.phys_shm,
                    listOf(
                        "A = 2", "ω = 2", "φ = 0",
                        "y = A sin(ω x + φ)", "y = A ω cos(ω x + φ)", "y = -A ω^2 sin(ω x + φ)",
                    ),
                    noteRes = R.string.phys_shm_note,
                    sliders = mapOf("A" to s(0.5, 4.0, 0.1), "ω" to s(0.5, 4.0, 0.1), "φ" to s(-3.14, 3.14, 0.01)),
                    window = listOf(-0.5, 10.0, -10.0, 10.0),
                ),
                GraphExample(
                    R.string.phys_damped,
                    listOf("A = 3", "b = 0.2", "ω = 3", "y = A e^(-b x) cos(ω x)", "y = A e^(-b x)", "y = -A e^(-b x)"),
                    noteRes = R.string.phys_damped_note,
                    sliders = mapOf("A" to s(0.5, 5.0, 0.1), "b" to s(0.0, 1.0, 0.01), "ω" to s(0.5, 8.0, 0.1)),
                    window = listOf(-0.5, 20.0, -4.0, 4.0),
                ),
                GraphExample(
                    R.string.phys_travelling,
                    listOf("A = 1.5", "λ = 4", "f = 0.5", "T = 0", "y = A sin(2π(x/λ - f T))"),
                    noteRes = R.string.phys_travelling_note,
                    sliders = mapOf("A" to s(0.5, 3.0, 0.1), "λ" to s(1.0, 10.0, 0.1), "f" to s(0.1, 2.0, 0.05), "T" to s(0.0, 10.0, 0.02)),
                    window = listOf(-1.0, 16.0, -4.0, 4.0),
                ),
                GraphExample(
                    R.string.phys_standing,
                    listOf("A = 1", "λ = 4", "f = 0.5", "T = 0", "y = 2A sin(2πx/λ) cos(2π f T)"),
                    noteRes = R.string.phys_standing_note,
                    sliders = mapOf("A" to s(0.5, 2.0, 0.1), "λ" to s(1.0, 10.0, 0.1), "f" to s(0.1, 2.0, 0.05), "T" to s(0.0, 10.0, 0.02)),
                    window = listOf(-1.0, 16.0, -4.0, 4.0),
                ),
                GraphExample(
                    R.string.phys_beats,
                    listOf("f_1 = 5", "f_2 = 5.5", "y = sin(2π f_1 x)", "y = sin(2π f_2 x)", "y = sin(2π f_1 x) + sin(2π f_2 x)"),
                    noteRes = R.string.phys_beats_note,
                    sliders = mapOf("f_1" to s(1.0, 10.0, 0.1), "f_2" to s(1.0, 10.0, 0.1)),
                    window = listOf(0.0, 4.0, -3.0, 3.0),
                ),
            ),
        ),
        GraphExampleCategory(
            R.string.phys_cat_electricity,
            listOf(
                GraphExample(
                    R.string.phys_inverse_square,
                    listOf("K = 10", "y = K/x^2 {x > 0}", "y = K/x {x > 0}"),
                    noteRes = R.string.phys_inverse_square_note,
                    sliders = mapOf("K" to s(1.0, 50.0, 0.5)),
                    window = listOf(-0.5, 10.0, -1.0, 20.0),
                ),
                GraphExample(
                    R.string.phys_earth_gravity,
                    listOf("R_e = 6.4", "g_0 = 9.8", "y = {x < R_e: g_0 x/R_e, g_0 R_e^2/x^2} {x ≥ 0}", "(R_e, g_0)"),
                    noteRes = R.string.phys_earth_gravity_note,
                    sliders = mapOf("R_e" to s(1.0, 10.0, 0.1), "g_0" to s(1.0, 25.0, 0.1)),
                    window = listOf(-1.0, 30.0, -1.0, 12.0),
                ),
                GraphExample(
                    R.string.phys_ohm,
                    listOf("R = 5", "y = x/R {x ≥ 0}"),
                    noteRes = R.string.phys_ohm_note,
                    sliders = mapOf("R" to s(0.5, 20.0, 0.5)),
                    window = listOf(-1.0, 12.0, -0.5, 5.0),
                ),
                GraphExample(
                    R.string.phys_rc,
                    listOf(
                        "V_0 = 10", "R = 2", "C = 1",
                        "y = V_0(1 - e^(-x/(R C))) {x ≥ 0}", "y = V_0 e^(-x/(R C)) {x ≥ 0}", "(R C, 0.632 V_0)",
                    ),
                    noteRes = R.string.phys_rc_note,
                    sliders = mapOf("V_0" to s(1.0, 20.0, 0.5), "R" to s(0.5, 5.0, 0.1), "C" to s(0.5, 5.0, 0.1)),
                    window = listOf(-1.0, 15.0, -1.0, 12.0),
                ),
                GraphExample(
                    R.string.phys_ac,
                    listOf("V_0 = 311", "f = 50", "y = V_0 sin(2π f x/1000)", "y = V_0/√2"),
                    noteRes = R.string.phys_ac_note,
                    sliders = mapOf("V_0" to s(10.0, 400.0, 1.0), "f" to s(10.0, 100.0, 1.0)),
                    window = listOf(-2.0, 40.0, -350.0, 350.0),
                ),
            ),
        ),
        GraphExampleCategory(
            R.string.phys_cat_fields,
            listOf(
                GraphExample(
                    R.string.phys_vector_add,
                    listOf(
                        "a_1 = 3", "a_2 = 1", "b_1 = 1", "b_2 = 3",
                        "vector((0, 0), (a_1, a_2))",
                        "vector((a_1, a_2), (a_1 + b_1, a_2 + b_2))",
                        "vector((0, 0), (a_1 + b_1, a_2 + b_2))",
                    ),
                    noteRes = R.string.phys_vector_add_note,
                    sliders = (listOf("a_1", "a_2", "b_1", "b_2").associateWith { s(-5.0, 5.0, 0.1) }),
                    window = listOf(-6.0, 8.0, -6.0, 8.0),
                ),
                GraphExample(
                    R.string.phys_point_charge,
                    listOf("q = 1", "field(q x/(x^2 + y^2)^1.5, q y/(x^2 + y^2)^1.5)", "(0, 0)"),
                    noteRes = R.string.phys_point_charge_note,
                    sliders = mapOf("q" to s(-3.0, 3.0, 0.1)),
                    window = listOf(-5.0, 5.0, -5.0, 5.0),
                ),
                GraphExample(
                    R.string.phys_dipole,
                    listOf(
                        "field((x - 1)/((x - 1)^2 + y^2)^1.5 - (x + 1)/((x + 1)^2 + y^2)^1.5, y/((x - 1)^2 + y^2)^1.5 - y/((x + 1)^2 + y^2)^1.5)",
                        "c = 0.5",
                        "1/sqrt((x - 1)^2 + y^2) - 1/sqrt((x + 1)^2 + y^2) = c",
                        "(1, 0)", "(-1, 0)",
                    ),
                    noteRes = R.string.phys_dipole_note,
                    sliders = mapOf("c" to s(-2.0, 2.0, 0.05)),
                    window = listOf(-4.0, 4.0, -4.0, 4.0),
                ),
                GraphExample(
                    R.string.phys_gravity_field,
                    listOf("field(-x/(x^2 + y^2)^1.5, -y/(x^2 + y^2)^1.5)", "x^2 + y^2 ≤ 1"),
                    noteRes = R.string.phys_gravity_field_note,
                    window = listOf(-5.0, 5.0, -5.0, 5.0),
                ),
            ),
        ),
        GraphExampleCategory(
            R.string.phys_cat_magnetism,
            listOf(
                GraphExample(
                    R.string.phys_wire_field,
                    listOf("field(-y/(x^2 + y^2), x/(x^2 + y^2))", "(0, 0)"),
                    noteRes = R.string.phys_wire_field_note,
                    window = listOf(-5.0, 5.0, -5.0, 5.0),
                ),
                GraphExample(
                    R.string.phys_wire_b,
                    listOf("I = 5", "y = 2I/x {x > 0}"),
                    noteRes = R.string.phys_wire_b_note,
                    sliders = mapOf("I" to s(0.5, 20.0, 0.5)),
                    window = listOf(-0.5, 10.0, -1.0, 20.0),
                ),
                GraphExample(
                    R.string.phys_helix,
                    listOf("R = 2", "p = 1", "(R cos(3t), R sin(3t), p t - 3)", "vector((0, 0, -4), (0, 0, 4))"),
                    noteRes = R.string.phys_helix_note,
                    sliders = mapOf("R" to s(0.5, 4.0, 0.1), "p" to s(0.0, 2.0, 0.05)),
                    window = listOf(5.0),
                    space = true,
                ),
                GraphExample(
                    R.string.phys_radius,
                    listOf("m = 1", "q = 1", "B = 1", "y = m x/(q B) {x ≥ 0}"),
                    noteRes = R.string.phys_radius_note,
                    sliders = mapOf("m" to s(0.1, 5.0, 0.1), "q" to s(0.1, 5.0, 0.1), "B" to s(0.1, 5.0, 0.1)),
                    window = listOf(-0.5, 10.0, -0.5, 10.0),
                ),
            ),
        ),
        GraphExampleCategory(
            R.string.phys_cat_light,
            listOf(
                GraphExample(
                    R.string.phys_lens,
                    listOf("f = 10", "y = f x/(x - f) {x > 0}", "x = f", "y = f"),
                    noteRes = R.string.phys_lens_note,
                    sliders = mapOf("f" to s(2.0, 30.0, 0.5)),
                    window = listOf(-2.0, 60.0, -60.0, 60.0),
                ),
                GraphExample(
                    R.string.phys_snell,
                    listOf(
                        "n = 1.5",
                        "y = (180/π) asin(sin(xπ/180)/n) {0 ≤ x ≤ 90}",
                        "y = (180/π) asin(n sin(xπ/180)) {0 ≤ x ≤ 90}",
                        "y = x {0 ≤ x ≤ 90}",
                    ),
                    noteRes = R.string.phys_snell_note,
                    sliders = mapOf("n" to s(1.0, 2.5, 0.01)),
                    window = listOf(-5.0, 100.0, -5.0, 100.0),
                ),
                GraphExample(
                    R.string.phys_double_slit,
                    listOf("d = 0.5", "D = 1", "λ = 600", "β = λ D/(1000 d)", "y = cos(πx/β)^2"),
                    noteRes = R.string.phys_double_slit_note,
                    sliders = mapOf("d" to s(0.1, 2.0, 0.05), "D" to s(0.5, 3.0, 0.1), "λ" to s(400.0, 700.0, 5.0)),
                    window = listOf(-6.0, 6.0, -0.2, 1.5),
                ),
                GraphExample(
                    R.string.phys_single_slit,
                    listOf("a = 0.1", "D = 1", "λ = 600", "w = λ D/(1000 a)", "y = {x = 0: 1, (sin(πx/w)/(πx/w))^2}"),
                    noteRes = R.string.phys_single_slit_note,
                    sliders = mapOf("a" to s(0.02, 0.5, 0.01), "D" to s(0.5, 3.0, 0.1), "λ" to s(400.0, 700.0, 5.0)),
                    window = listOf(-20.0, 20.0, -0.1, 1.2),
                ),
                GraphExample(
                    R.string.phys_malus,
                    listOf("I_0 = 1", "y = I_0 cos(xπ/180)^2 {0 ≤ x ≤ 360}"),
                    noteRes = R.string.phys_malus_note,
                    sliders = mapOf("I_0" to s(0.1, 2.0, 0.1)),
                    window = listOf(-10.0, 370.0, -0.2, 1.5),
                ),
            ),
        ),
        GraphExampleCategory(
            R.string.phys_cat_modern,
            listOf(
                GraphExample(
                    R.string.phys_decay,
                    listOf("N_0 = 1000", "λ = 0.1", "y = N_0 e^(-λ x) {x ≥ 0}", "(ln(2)/λ, N_0/2)"),
                    noteRes = R.string.phys_decay_note,
                    sliders = mapOf("N_0" to s(100.0, 2000.0, 10.0), "λ" to s(0.01, 1.0, 0.01)),
                    window = listOf(-2.0, 40.0, -100.0, 1200.0),
                ),
                GraphExample(
                    R.string.phys_photoelectric,
                    listOf("W = 2.3", "y = 0.4136 x - W {x ≥ W/0.4136}", "(W/0.4136, 0)"),
                    noteRes = R.string.phys_photoelectric_note,
                    sliders = mapOf("W" to s(1.0, 5.0, 0.05)),
                    window = listOf(-1.0, 20.0, -3.0, 6.0),
                ),
                GraphExample(
                    R.string.phys_boyle,
                    listOf("T = 300", "y = 8.314 T/x {x > 0}", "y = 8.314 (2T)/x {x > 0}"),
                    noteRes = R.string.phys_boyle_note,
                    sliders = mapOf("T" to s(100.0, 600.0, 5.0)),
                    window = listOf(-2.0, 50.0, -50.0, 1000.0),
                ),
                GraphExample(
                    R.string.phys_charles,
                    listOf("V_0 = 22.4", "y = V_0 (1 + x/273.15) {x ≥ -273.15}", "(-273.15, 0)"),
                    noteRes = R.string.phys_charles_note,
                    sliders = mapOf("V_0" to s(5.0, 40.0, 0.1)),
                    window = listOf(-300.0, 200.0, -5.0, 40.0),
                ),
                GraphExample(
                    R.string.phys_cooling,
                    listOf("T_s = 25", "T_0 = 90", "k = 0.1", "y = T_s + (T_0 - T_s) e^(-k x) {x ≥ 0}", "y = T_s"),
                    noteRes = R.string.phys_cooling_note,
                    sliders = mapOf("T_s" to s(0.0, 50.0, 1.0), "T_0" to s(50.0, 100.0, 1.0), "k" to s(0.01, 0.5, 0.01)),
                    window = listOf(-2.0, 60.0, -5.0, 100.0),
                ),
            ),
        ),
        GraphExampleCategory(
            R.string.phys_cat_lab,
            listOf(
                GraphExample(
                    R.string.phys_lab_hooke, emptyList(), noteRes = R.string.phys_lab_hooke_note,
                    table = TableState(
                        listOf("0", "1", "2", "3", "4", "5"), listOf("0", "2.1", "3.9", "6.1", "8", "9.9"), RegressionModel.LINEAR,
                    ),
                    window = listOf(-0.5, 6.0, -1.0, 12.0),
                ),
                GraphExample(
                    R.string.phys_lab_ohm, emptyList(), noteRes = R.string.phys_lab_ohm_note,
                    table = TableState(
                        listOf("0", "1", "2", "3", "4", "5"), listOf("0", "0.21", "0.39", "0.61", "0.79", "1.02"), RegressionModel.LINEAR,
                    ),
                    window = listOf(-0.5, 6.0, -0.2, 1.3),
                ),
                GraphExample(
                    R.string.phys_lab_pendulum, emptyList(), noteRes = R.string.phys_lab_pendulum_note,
                    table = TableState(
                        listOf("0.2", "0.4", "0.6", "0.8", "1.0", "1.2"), listOf("0.90", "1.27", "1.55", "1.79", "2.01", "2.20"), RegressionModel.POWER,
                    ),
                    window = listOf(-0.1, 1.5, -0.2, 2.6),
                ),
                GraphExample(
                    R.string.phys_lab_decay, emptyList(), noteRes = R.string.phys_lab_decay_note,
                    table = TableState(
                        listOf("0", "1", "2", "3", "4", "5"), listOf("1000", "740", "552", "405", "300", "224"), RegressionModel.EXPONENTIAL,
                    ),
                    window = listOf(-0.5, 6.0, -50.0, 1100.0),
                ),
            ),
        ),
    )
}
