package com.prostuti.student.domain.graph.space

import com.prostuti.student.domain.calculator.AngleUnit
import com.prostuti.student.domain.graph.Axis3
import com.prostuti.student.domain.graph.CompiledGraph
import com.prostuti.student.domain.graph.GraphCompiler
import com.prostuti.student.domain.graph.GraphErrorKind
import com.prostuti.student.domain.graph.GraphLineInput
import com.prostuti.student.domain.graph.GraphLineKind
import com.prostuti.student.domain.graph.GraphPlot
import com.prostuti.student.domain.graph.GraphSpace
import com.prostuti.student.domain.graph.GraphTestSupport.assertClose
import com.prostuti.student.domain.graph.plot.LineOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sqrt

class SpaceTest {

    private fun space(vararg lines: String): CompiledGraph =
        GraphCompiler().compile(lines.mapIndexed { i, t -> GraphLineInput(i.toLong(), t) }, AngleUnit.RAD, GraphSpace.SPACE)

    private fun last(vararg lines: String) = space(*lines).lines.last()
    private fun plot(vararg lines: String) = last(*lines).plot

    private fun geometry(text: String, range: Double = 5.0, options: LineOptions = LineOptions()) =
        SpacePlotter().plot(last(text), options, range)!!

    // ── Classification ──────────────────────────────────────────────────────────────────

    @Test fun classifiesSpaceLines() {
        assertEquals(Axis3.Z, (plot("z = x^2 + y^2") as GraphPlot.Surface).axis)
        assertEquals(Axis3.Z, (plot("x^2 - y^2") as GraphPlot.Surface).axis) // bare f(x, y) means z = f
        assertEquals(Axis3.Y, (plot("y = x^2") as GraphPlot.Surface).axis) // a parabolic sheet in space
        assertEquals(Axis3.X, (plot("x = sin(y) + z") as GraphPlot.Surface).axis)
        assertNotNull((plot("x^2 + y^2 + z^2 = 9") as GraphPlot.ImplicitSurface).gradient)
        assertTrue(plot("x^2 + y^2 = 4") is GraphPlot.ImplicitSurface) // cylinder
        assertTrue(plot("2x + 3y - z = 6") is GraphPlot.Surface || plot("2x + 3y - z = 6") is GraphPlot.ImplicitSurface)
        assertTrue(plot("(cos(t), sin(t), t/4)") is GraphPlot.SpaceCurve)
        assertTrue(plot("(cos(u) sin(v), sin(u) sin(v), cos(v))") is GraphPlot.ParametricSurface)
        assertTrue(plot("(1, 2, 3)") is GraphPlot.Point3)
        val v = plot("vector((1, 0, 0), (1, 2, 3))") as GraphPlot.Vector3
        assertEquals(listOf(1.0, 2.0, 3.0), v.to.toList())
        assertEquals(listOf(0.0, 0.0, 0.0), (plot("vector((4, 5, 6))") as GraphPlot.Vector3).from.toList())
    }

    @Test fun cylindricalRInSpace() {
        val p = plot("r = 2") // r = √(x² + y²) in space: a cylinder
        assertTrue(p is GraphPlot.ImplicitSurface)
    }

    @Test fun inequalitiesAndPlaneShapesAreNotSurfaces() {
        assertEquals(GraphErrorKind.UNSUPPORTED_EQUATION, last("z > x").error?.kind)
        assertEquals(GraphErrorKind.UNSUPPORTED_EQUATION, last("(1, 2)").error?.kind)
    }

    @Test fun uAndVAreOnlyReservedInSpace() {
        val plane = GraphCompiler().compile(listOf(GraphLineInput(0, "u = 2"), GraphLineInput(1, "y = u x")), AngleUnit.RAD)
        assertEquals(GraphLineKind.PARAMETER, plane.lines[0].kind)
        assertTrue(plane.lines[1].plot is GraphPlot.ExplicitY)
    }

    @Test fun planeDefersSpaceLinesAndDrawsVectors() {
        val plane = GraphCompiler().compile(
            listOf(GraphLineInput(0, "z = x"), GraphLineInput(1, "vector((0, 0), (2, 3))"), GraphLineInput(2, "revolvex(x, 0, 1)")),
            AngleUnit.RAD,
        )
        assertEquals(GraphErrorKind.NEEDS_3D, plane.lines[0].error?.kind)
        assertTrue(plane.lines[1].plot is GraphPlot.Vector2)
        assertEquals(GraphErrorKind.NEEDS_3D, plane.lines[2].error?.kind)
    }

    // ── Volumes ─────────────────────────────────────────────────────────────────────────

    @Test fun doubleIntegralVolume() {
        val line = last("∫_0^2 ∫_0^1 (x + y) dy dx") // ∫_0^2 (x + 1/2) dx = 3
        assertEquals(GraphLineKind.VOLUME, line.kind)
        assertClose(3.0, line.value!!, 1e-10)
        // region bounded by a curve: 0 ≤ y ≤ x², 0 ≤ x ≤ 1 under z = 1 → area 1/3
        assertClose(1.0 / 3, last("∫_0^1 ∫_0^(x^2) 1 dy dx").value!!, 1e-10)
    }

    @Test fun solidsOfRevolution() {
        assertClose(32 * PI / 5, last("revolvex(x^2, 0, 2)").value!!, 1e-9) // disc: π∫x⁴
        assertClose(8 * PI, last("revolvey(x^2, 0, 2)").value!!, 1e-9) // shells: 2π∫x·x²
        assertClose(2 * PI / 15, last("revolvex(x, x^2, 0, 1)").value!!, 1e-9) // washer
        assertClose(4.0 / 3 * PI, last("revolvex(sqrt(1 - x^2), -1, 1)").value!!, 1e-6) // unit sphere
        assertEquals(GraphLineKind.REVOLUTION, last("revolvex(x^2, 0, 2)").kind)
    }

    // ── Meshes ──────────────────────────────────────────────────────────────────────────

    @Test fun implicitSphereVerticesLieOnTheSphere() {
        val mesh = geometry("x^2 + y^2 + z^2 = 9").surfaces.single()
        assertTrue(mesh.vertexCount > 1000)
        val p = mesh.positions
        val n = mesh.normals
        for (i in 0 until mesh.vertexCount) {
            val x = p[3 * i].toDouble(); val y = p[3 * i + 1].toDouble(); val z = p[3 * i + 2].toDouble()
            assertClose(3.0, sqrt(x * x + y * y + z * z), 1e-4)
            // normal points along the radius (exact gradient)
            val dot = (x * n[3 * i] + y * n[3 * i + 1] + z * n[3 * i + 2]) / 3.0
            assertClose(1.0, abs(dot), 1e-4)
        }
    }

    @Test fun explicitSurfaceVerticesAreOnTheSurface() {
        val mesh = geometry("z = x^2 - y^2").surfaces.single()
        val p = mesh.positions
        for (i in 0 until mesh.vertexCount) {
            val x = p[3 * i]; val y = p[3 * i + 1]
            assertEquals(x * x - y * y, p[3 * i + 2], 1e-3f)
        }
    }

    @Test fun polesAreNotBridged() {
        // z = 1/(xy) jumps from −∞ to +∞ across the axes: no triangle may span the jump.
        val range = 5.0
        val mesh = geometry("z = 1/(x y)", range).surfaces.single()
        val p = mesh.positions
        for (t in 0 until mesh.vertexCount / 3) {
            val zs = (0..2).map { p[9 * t + 3 * it + 2] }
            assertTrue((zs.max() - zs.min()) <= 2 * range + 1e-3)
        }
    }

    @Test fun implicitPoleIsNotASurface() {
        // x·z = 1 written as 1/x − z = 0: the sign flips at x = 0 but there is no surface there.
        val mesh = geometry("1/x - z = 0").surfaces.single()
        val p = mesh.positions
        for (i in 0 until mesh.vertexCount) assertTrue(abs(p[3 * i]) > 0.05f)
    }

    @Test fun curveTubeHugsTheHelix() {
        val range = 5.0
        val g = geometry("(cos(t), sin(t), t/4)", range)
        val p = g.solidParts.single().positions
        val radius = range * 0.012
        for (i in 0 until p.size / 3) {
            val r = sqrt((p[3 * i] * p[3 * i] + p[3 * i + 1] * p[3 * i + 1]).toDouble())
            assertTrue(abs(r - 1.0) <= radius * 1.01)
        }
    }

    @Test fun wireframeAndContoursFollowOptions() {
        assertNull(geometry("z = x y", options = LineOptions(showWireframe = false)).lines)
        val contours = geometry("z = x^2 + y^2", options = LineOptions(showWireframe = false, showContours = true)).lines!!
        val p = contours.positions
        // contour segments are level: both ends have the same z, on a grid multiple
        for (i in p.indices step 6) assertEquals(p[i + 2], p[i + 5], 1e-6f)
    }

    @Test fun revolutionSkinHasTheRightRadius() {
        val g = geometry("revolvex(x^2, 0, 2)")
        val skin = g.surfaces.first().positions
        for (i in 0 until skin.size / 3) {
            val x = skin[3 * i]
            val r = sqrt((skin[3 * i + 1] * skin[3 * i + 1] + skin[3 * i + 2] * skin[3 * i + 2]).toDouble())
            assertClose((x * x).toDouble(), r, 1e-4)
        }
    }

    @Test fun pointAndVectorHaveLabels() {
        assertEquals(listOf(1.0, 2.0, 3.0), geometry("(1, 2, 3)").labels.single().toList())
        assertTrue(geometry("vector((0,0,0),(1,1,1))").solidParts.single().vertexCount > 0)
    }
}
