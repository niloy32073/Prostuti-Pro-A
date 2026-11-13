package com.prostuti.student.ui.screen.playground.graph

import com.prostuti.student.domain.calculator.AngleUnit
import com.prostuti.student.domain.graph.GraphCompiler
import com.prostuti.student.domain.graph.GraphLineInput
import com.prostuti.student.domain.graph.GraphLineKind
import com.prostuti.student.domain.graph.GraphPlot
import com.prostuti.student.domain.graph.GraphSpace
import com.prostuti.student.domain.graph.TableInput
import com.prostuti.student.ui.screen.playground.graph.share.GraphShareImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Every example and physics template a student can tap must compile without a single error. */
class TemplatesCompileTest {

    private fun compile(example: GraphExample) = GraphCompiler().compile(
        listOf(GraphLineInput(0, "# note")) +
            example.lines.mapIndexed { i, t -> GraphLineInput(i + 1L, t) } +
            listOfNotNull(example.table?.let { GraphLineInput(999, "", TableInput(it.xs, it.ys, it.model)) }),
        AngleUnit.RAD,
        if (example.space) GraphSpace.SPACE else GraphSpace.PLANE,
    )

    private fun checkAll(categories: List<GraphExampleCategory>) {
        for (example in categories.flatMap { it.examples }) {
            val graph = compile(example)
            for (line in graph.lines) {
                line.error?.let { fail("example ${example.labelRes}: '${example.lines.getOrNull(line.id.toInt() - 1)}' → ${it.kind} ${it.names}") }
            }
            // Every slider range belongs to a parameter the template actually defines.
            val parameters = graph.lines.mapNotNull { it.parameter?.name }.toSet()
            for (name in example.sliders.keys) assertTrue("${example.labelRes}: slider $name has no line", name in parameters)
        }
    }

    @Test fun mathExamplesCompile() = checkAll(GraphExamples.categories)

    @Test fun spaceExamplesCompile() = checkAll(GraphExamples.spaceCategories)

    @Test fun physicsTemplatesCompile() = checkAll(PhysicsTemplates.categories)

    @Test fun projectileTemplateIsPhysicallyRight() {
        val projectile = PhysicsTemplates.categories.first().examples.first()
        val graph = compile(projectile)
        val r = graph.parameters.getValue("R")
        val h = graph.parameters.getValue("H")
        // v₀ = 20 m/s, α = 45°, g = 9.8: R = v₀²/g, H = v₀²/(4g)
        assertEquals(400 / 9.8, r, 1e-9)
        assertEquals(100 / 9.8, h, 1e-9)
        val path = graph.lines.mapNotNull { it.plot as? GraphPlot.ExplicitY }.single()
        assertEquals(0.0, path.f.evaluate(r), 1e-9) // lands at the range
        assertEquals(h, path.f.evaluate(r / 2), 1e-9) // highest in the middle
    }

    @Test fun labTablesGetSensibleFits() {
        val lab = PhysicsTemplates.categories.last().examples
        val pendulum = compile(lab.first { it.labelRes == com.prostuti.student.R.string.phys_lab_pendulum })
        val fit = (pendulum.lines.last().plot as GraphPlot.Table).result
        assertNotNull(fit)
        // T = 2π√(L/g): exponent ≈ 0.5 and g from a ≈ 9.8
        assertEquals(0.5, fit!!.coefficients[1], 0.02)
        val g = 4 * Math.PI * Math.PI / (fit.coefficients[0] * fit.coefficients[0])
        assertEquals(9.8, g, 0.3)
        assertEquals(GraphLineKind.TABLE, pendulum.lines.last().kind)
    }

    @Test fun prettyTextForSharing() {
        assertEquals("y = x² + 3·x⁻¹", GraphShareImage.pretty("y = x^2 + 3*x^-1"))
        assertEquals("√(x) ≤ π", GraphShareImage.pretty("sqrt(x) <= pi"))
        assertEquals("∫₀³ x² dx", GraphShareImage.pretty("∫_0^3 x^2 dx"))
        assertEquals("∫[0, π] sin(x) dx", GraphShareImage.pretty("∫_0^π sin(x) dx"))
        assertEquals("∫[-π, π] sin(x) dx", GraphShareImage.pretty("∫_(-π)^π sin(x) dx"))
        assertEquals("v₀ = 20   x₁²", GraphShareImage.pretty("v_0 = 20   x_1^2"))
    }
}
