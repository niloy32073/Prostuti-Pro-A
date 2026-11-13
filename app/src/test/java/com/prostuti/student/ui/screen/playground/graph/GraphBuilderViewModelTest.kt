package com.prostuti.student.ui.screen.playground.graph

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.prostuti.student.R
import com.prostuti.student.domain.graph.Axis3
import com.prostuti.student.data.repository.fake.FakeSavedGraphRepository
import com.prostuti.student.domain.GetSavedGraphUseCase
import com.prostuti.student.domain.SaveGraphUseCase
import com.prostuti.student.domain.graph.GraphCompiler
import com.prostuti.student.domain.graph.space.SpacePlotter
import com.prostuti.student.domain.graph.GraphSpace
import com.prostuti.student.domain.graph.GraphErrorKind
import com.prostuti.student.domain.graph.GraphLineKind
import com.prostuti.student.domain.graph.plot.GraphPlotter
import com.prostuti.student.domain.graph.plot.RiemannMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GraphBuilderViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setup() = Dispatchers.setMain(dispatcher)

    @After fun teardown() = Dispatchers.resetMain()

    private val library = FakeSavedGraphRepository()

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle(), savedGraphId: Long? = null) =
        GraphBuilderViewModel(
            GraphCompiler(), GraphPlotter(), dispatcher, handle, SpacePlotter(),
            GetSavedGraphUseCase(library), SaveGraphUseCase(library), savedGraphId,
        )

    private fun GraphBuilderViewModel.type(vararg keys: String) =
        keys.forEach { onKey(MathKeyAction.Insert(it)) }

    private fun GraphBuilderViewModel.lineTexts() = state.value.lines.map { it.text }

    @Test fun startsWithOneEmptyLineAndNoViewport() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(listOf(""), vm.lineTexts())
        assertNull(vm.state.value.plotViewport)
    }

    @Test fun firstCanvasSizeSetsATwentyUnitWindow() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onCanvasSize(1000, 800)
        advanceUntilIdle()
        val vp = vm.state.value.plotViewport!!
        assertEquals(20.0, vp.yMax - vp.yMin, 1e-9) // the shorter side spans 20 units
        assertEquals(0.0, (vp.xMin + vp.xMax) / 2, 1e-12)
    }

    @Test fun typingCompilesAndShowsTheValue() = runTest(dispatcher) {
        val vm = viewModel()
        val id = vm.state.value.lines.single().id
        vm.onFocus(id)
        vm.type("2", "+", "3")
        advanceUntilIdle()
        val status = vm.state.value.status(id)
        assertEquals(GraphLineKind.VALUE, status.kind)
        assertEquals(5.0, status.value!!, 0.0)
        assertTrue(vm.state.value.keyboardVisible)
    }

    @Test fun functionKeysPutTheCursorInsideTheBrackets() = runTest(dispatcher) {
        val vm = viewModel()
        val id = vm.state.value.lines.single().id
        vm.onFocus(id)
        vm.type("y = ")
        vm.onKey(MathKeyAction.Insert("sin()", 1))
        vm.type("x")
        assertEquals("y = sin(x)", vm.lineTexts().single())
    }

    @Test fun enterAddsALineBelowAndBackspaceOnEmptyRemovesIt() = runTest(dispatcher) {
        val vm = viewModel()
        val first = vm.state.value.lines.single().id
        vm.onFocus(first)
        vm.type("y = x")
        vm.onKey(MathKeyAction.Enter)
        assertEquals(2, vm.state.value.lines.size)
        val second = vm.state.value.focusedId
        assertNotEquals(first, second)
        vm.onKey(MathKeyAction.Backspace)
        assertEquals(1, vm.state.value.lines.size)
        assertEquals(first, vm.state.value.focusedId)
    }

    @Test fun addSliderDefinesTheMissingLetter() = runTest(dispatcher) {
        val vm = viewModel()
        val id = vm.state.value.lines.single().id
        vm.onTextChange(id, "y = a x", 7, 7)
        advanceUntilIdle()
        assertEquals(GraphErrorKind.UNDEFINED_VARIABLE, vm.state.value.status(id).error?.kind)
        vm.addSlider("a", id)
        advanceUntilIdle()
        assertEquals(listOf("y = a x", "a = 1"), vm.lineTexts())
        assertNull(vm.state.value.status(id).error)
        val sliderLine = vm.state.value.lines[1].id
        assertTrue(vm.state.value.status(sliderLine).isSlider)
    }

    @Test fun sliderSnapsToItsStepAndRewritesTheLine() = runTest(dispatcher) {
        val vm = viewModel()
        val id = vm.state.value.lines.single().id
        vm.onTextChange(id, "a = 1", 5, 5)
        advanceUntilIdle()
        vm.onSliderChange(id, 2.34)
        advanceUntilIdle()
        assertEquals("a = 2.3", vm.lineTexts().single())
        assertEquals(2.3, vm.state.value.status(id).value!!, 1e-12)
        vm.setSliderRange(id, SliderRange(0.0, 100.0, 5.0))
        vm.onSliderChange(id, 42.0)
        assertEquals("a = 40", vm.lineTexts().single())
    }

    @Test fun pinchZoomKeepsThePointUnderTheFingers() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onCanvasSize(1000, 1000)
        advanceUntilIdle()
        val before = vm.state.value.plotViewport!!
        val worldX = before.worldX(750f)
        val worldY = before.worldY(250f)
        vm.onTransform(0f, 0f, 2f, 750f, 250f)
        val after = vm.state.value.plotViewport!!
        assertEquals(worldX, after.worldX(750f), 1e-9)
        assertEquals(worldY, after.worldY(250f), 1e-9)
        assertEquals((before.xMax - before.xMin) / 2, after.xMax - after.xMin, 1e-9)
    }

    @Test fun panMovesTheWindowWithTheFinger() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onCanvasSize(1000, 1000)
        val before = vm.state.value.plotViewport!!
        vm.onTransform(100f, 0f, 1f, 500f, 500f) // drag right by 100 px
        val after = vm.state.value.plotViewport!!
        assertEquals(before.xMin - 100 * before.unitsPerPxX, after.xMin, 1e-9)
    }

    @Test fun onlyVisibleLinesAreDrawn() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onCanvasSize(800, 800)
        vm.addExample(GraphExample(R.string.graph_ex_line, listOf("y = x", "y = x^2")))
        advanceUntilIdle()
        assertEquals(2, vm.state.value.geometries.size)
        vm.toggleVisible(vm.state.value.lines[0].id)
        advanceUntilIdle()
        assertEquals(1, vm.state.value.geometries.size)
    }

    @Test fun examplesGetDifferentColours() = runTest(dispatcher) {
        val vm = viewModel()
        vm.addExample(GraphExample(R.string.graph_ex_line, listOf("y = x", "y = x^2", "y = x^3")))
        val colours = vm.state.value.lines.map { it.colorIndex }
        assertEquals(3, colours.toSet().size)
    }

    @Test fun tappingNearACurveTracesItWithItsSlope() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onCanvasSize(1000, 1000)
        vm.addExample(GraphExample(R.string.graph_ex_line, listOf("y = x^2")))
        advanceUntilIdle()
        val vp = vm.state.value.plotViewport!!
        // the curve passes through (2, 4)
        vm.onTap(vp.px(2.0), vp.py(4.0) + 5f, touchRadiusPx = 30f)
        val trace = vm.state.value.trace!!
        assertEquals(4.0, trace.y, 0.05)
        assertEquals(2 * trace.x, trace.slope!!, 1e-9)
        vm.tangentAtTrace()
        assertEquals(trace.x, vm.state.value.lines.single().options.tangentAt!!, 0.0)
    }

    @Test fun tappingEmptyPaperClearsTraceAndKeyboard() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onCanvasSize(1000, 1000)
        vm.onFocus(vm.state.value.lines.single().id)
        advanceUntilIdle()
        vm.onTap(10f, 10f, 30f)
        assertNull(vm.state.value.trace)
        assertFalse(vm.state.value.keyboardVisible)
    }

    @Test fun graphSurvivesProcessDeath() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val vm = viewModel(handle)
        vm.onCanvasSize(800, 800)
        vm.addExample(GraphExample(R.string.graph_ex_line, listOf("a = 2", "y = a x")))
        vm.updateSettings(GraphSettings(piXAxis = true))
        vm.zoomBy(2f)
        advanceUntilIdle()
        val restored = viewModel(handle)
        assertEquals(listOf("a = 2", "y = a x"), restored.lineTexts())
        assertTrue(restored.state.value.settings.piXAxis)
        assertEquals(vm.state.value.viewport, restored.state.value.viewport)
    }

    @Test fun riemannOptionReportsTheSum() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onCanvasSize(800, 800)
        vm.addExample(GraphExample(R.string.graph_ex_integral, listOf("∫_0^2 x^2 dx")))
        val id = vm.state.value.lines.single().id
        vm.setOptions(id, vm.state.value.lines.single().options.copy(riemann = RiemannMode.LEFT, riemannCount = 4))
        advanceUntilIdle()
        assertEquals(1.75, vm.state.value.status(id).riemannSum!!, 1e-12)
    }

    @Test fun rangeFieldsAcceptExpressions() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onCanvasSize(1000, 500)
        vm.applyRangeTexts("-2π", "2π", "-2", "2")
        val vp = vm.state.value.plotViewport!!
        assertEquals(-2 * Math.PI, vp.xMin, 1e-9)
        assertEquals(2.0, vp.yMax, 1e-9)
    }

    @Test fun stateEmitsWhenALineIsDeleted() = runTest(dispatcher) {
        val vm = viewModel()
        vm.addExample(GraphExample(R.string.graph_ex_line, listOf("y = x", "y = 2x")))
        vm.state.test {
            assertEquals(2, awaitItem().lines.size)
            vm.deleteLine(vm.state.value.lines[0].id)
            assertEquals(listOf("y = 2x"), awaitItem().lines.map { it.text })
            cancelAndIgnoreRemainingEvents()
        }
        assertNotNull(vm.state.value.lines.single())
    }

    // ── 3D ──────────────────────────────────────────────────────────────────────────────────

    @Test fun twoAndThreeDimensionalListsAreSeparate() = runTest(dispatcher) {
        val vm = viewModel()
        vm.addExample(GraphExample(R.string.graph_ex_line, listOf("y = x")))
        vm.setSpace(GraphSpace.SPACE)
        assertEquals(listOf(""), vm.lineTexts())
        vm.addExample(GraphExample(R.string.graph_ex_sphere, listOf("x^2 + y^2 + z^2 = 9"), space = true))
        advanceUntilIdle()
        assertEquals(1, vm.state.value.spaceGeometries.size)
        assertEquals(GraphLineKind.IMPLICIT_SURFACE, vm.state.value.status(vm.state.value.lines.single().id).kind)
        vm.setSpace(GraphSpace.PLANE)
        assertEquals(listOf("y = x"), vm.lineTexts())
    }

    @Test fun revolveWritesTheSolidAndItsDefinitionsInto3D() = runTest(dispatcher) {
        val vm = viewModel()
        vm.addExample(GraphExample(R.string.graph_ex_integral, listOf("k = 2", "f(t) = k t^2", "∫_0^1 f(t) dt")))
        advanceUntilIdle()
        val integral = vm.state.value.lines.last().id
        vm.revolve(integral, Axis3.X)
        advanceUntilIdle()
        assertTrue(vm.state.value.isSpace)
        val texts = vm.lineTexts()
        assertEquals(listOf("k = 2", "f(t) = k t^2"), texts.dropLast(1))
        assertTrue(texts.last().startsWith("revolvex("))
        val status = vm.state.value.status(vm.state.value.lines.last().id)
        assertEquals(GraphLineKind.REVOLUTION, status.kind)
        // π∫_0^1 (2x²)² dx = 4π/5
        assertEquals(4 * Math.PI / 5, status.value!!, 1e-9)
        // the box fits the solid: radius up to f(1) = 2, plus a margin
        assertEquals(2.5, vm.state.value.spaceSettings.range, 1e-6)
    }

    @Test fun pinchInThreeDChangesTheBox() = runTest(dispatcher) {
        val vm = viewModel()
        vm.setSpace(GraphSpace.SPACE)
        val before = vm.state.value.spaceSettings.range
        vm.zoomSpace(2f)
        assertEquals(before / 2, vm.state.value.spaceSettings.range, 1e-12)
        vm.rotateCamera(30f, 100f)
        assertEquals(89f, vm.state.value.camera.pitch, 0f) // never flips over the top
        vm.resetCamera()
        assertEquals(before, vm.state.value.spaceSettings.range, 0.0)
    }

    // ── Phase 3: templates, tables, saving ──────────────────────────────────────────────────

    @Test fun templateAddsNoteSlidersAndWindow() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onCanvasSize(1000, 1000)
        val projectile = PhysicsTemplates.categories.first().examples.first()
        vm.addExample(projectile, "note text", replace = true)
        advanceUntilIdle()
        val lines = vm.state.value.lines
        assertEquals("# note text", lines.first().text)
        assertEquals(GraphLineKind.NOTE, vm.state.value.status(lines.first().id).kind)
        assertEquals(SliderRange(0.0, 60.0, 0.5), lines.first { it.text.startsWith("v_0") }.slider)
        val vp = vm.state.value.plotViewport!!
        assertEquals(-5.0, vp.xMin, 1e-9)
        assertEquals(50.0, vp.xMax, 1e-9)
        assertTrue(vm.state.value.statuses.values.none { it.error != null })
    }

    @Test fun replacingClearsTheListAndSpaceTemplatesOpen3D() = runTest(dispatcher) {
        val vm = viewModel()
        vm.addExample(GraphExample(R.string.graph_ex_line, listOf("g = 3", "y = g x")))
        val helix = PhysicsTemplates.categories.flatMap { it.examples }.first { it.space }
        vm.addExample(helix, null, replace = true)
        advanceUntilIdle()
        assertTrue(vm.state.value.isSpace)
        assertTrue(vm.state.value.spaceGeometries.isNotEmpty())
        vm.setSpace(GraphSpace.PLANE)
        assertEquals(listOf("g = 3", "y = g x"), vm.lineTexts()) // the 2D list was not touched
    }

    @Test fun tablesFitAsYouType() = runTest(dispatcher) {
        val vm = viewModel()
        vm.addTable()
        val id = vm.state.value.lines.single { it.table != null }.id
        vm.setTable(id, TableState(listOf("1", "2", "3"), listOf("3", "5", "7")))
        advanceUntilIdle()
        val status = vm.state.value.status(id)
        assertEquals(GraphLineKind.TABLE, status.kind)
        assertEquals(3, status.pointCount)
        assertEquals("y = 2 x + 1", status.fitText)
        assertEquals(1.0, status.fitRSquared!!, 1e-12)
        assertEquals(1.0, status.fitR!!, 1e-12)
    }

    @Test fun firstSaveAsksForANameThenOverwrites() = runTest(dispatcher) {
        val vm = viewModel()
        vm.addExample(GraphExample(R.string.graph_ex_line, listOf("y = x^2")))
        assertTrue(vm.state.value.isDirty)
        vm.requestSave()
        assertEquals(GraphDialog.SaveName(false), vm.state.value.dialog)
        vm.saveAs("Parabola")
        advanceUntilIdle()
        assertEquals(1, library.all.size)
        assertEquals("Parabola", library.all.single().name)
        assertEquals("y = x^2", library.all.single().preview)
        assertFalse(vm.state.value.isDirty)
        assertEquals(GraphMessage.SAVED, vm.state.value.message)
        // later saves keep the same entry
        vm.addExample(GraphExample(R.string.graph_ex_line, listOf("y = x")))
        vm.requestSave()
        advanceUntilIdle()
        assertEquals(1, library.all.size)
        assertTrue(library.all.single().preview.contains("y = x"))
    }

    @Test fun panningIsNotAnUnsavedChange() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onCanvasSize(800, 800)
        vm.addExample(GraphExample(R.string.graph_ex_line, listOf("y = x")))
        vm.saveAs("Line")
        advanceUntilIdle()
        vm.onTransform(120f, 40f, 1.5f, 400f, 400f)
        vm.rotateCamera(30f, 10f)
        assertFalse(vm.state.value.isDirty)
    }

    @Test fun leavingWithChangesAsksFirst() = runTest(dispatcher) {
        val vm = viewModel()
        vm.requestExit()
        assertTrue(vm.state.value.exitRequested) // nothing typed: leave at once
        vm.onExitHandled()
        vm.addExample(GraphExample(R.string.graph_ex_line, listOf("y = x")))
        vm.requestExit()
        assertEquals(GraphDialog.UnsavedChanges, vm.state.value.dialog)
        assertFalse(vm.state.value.exitRequested)
        vm.requestSave(exitAfter = true)
        vm.saveAs("Mine", exitAfter = true)
        advanceUntilIdle()
        assertTrue(vm.state.value.exitRequested)
    }

    @Test fun openingASavedGraphRestoresIt() = runTest(dispatcher) {
        val first = viewModel()
        first.addExample(GraphExample(R.string.graph_ex_line, listOf("a = 3", "y = a x")))
        first.setSpace(GraphSpace.SPACE)
        first.addExample(GraphExample(R.string.graph_ex_sphere, listOf("x^2 + y^2 + z^2 = 4"), space = true))
        first.saveAs("Both")
        advanceUntilIdle()
        val id = library.all.single().id
        val reopened = viewModel(savedGraphId = id)
        advanceUntilIdle()
        assertEquals("Both", reopened.state.value.savedName)
        assertTrue(reopened.state.value.isSpace)
        assertEquals(listOf("x^2 + y^2 + z^2 = 4"), reopened.lineTexts())
        reopened.setSpace(GraphSpace.PLANE)
        assertEquals(listOf("a = 3", "y = a x"), reopened.lineTexts())
        assertFalse(reopened.state.value.isDirty || reopened.state.value.dialog != null)
    }

    @Test fun missingSavedGraphShowsAMessage() = runTest(dispatcher) {
        val vm = viewModel(savedGraphId = 42)
        advanceUntilIdle()
        assertEquals(GraphMessage.OPEN_FAILED, vm.state.value.message)
    }

    @Test fun collapsingThePanelEndsEditingAndEditingBringsItBack() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        val first = vm.state.value.lines.first().id
        vm.onFocus(first)
        vm.setPanelCollapsed(true)
        with(vm.state.value) {
            assertTrue(panelCollapsed)
            assertNull(focusedId)
            assertFalse(keyboardVisible)
        }
        vm.setPanelCollapsed(false)
        assertFalse(vm.state.value.panelCollapsed)

        vm.setPanelCollapsed(true)
        vm.onFocus(first)
        assertFalse("focusing a line opens the panel", vm.state.value.panelCollapsed)
        vm.setPanelCollapsed(true)
        vm.addLine()
        assertFalse("a new line opens the panel", vm.state.value.panelCollapsed)
        assertTrue(vm.state.value.keyboardVisible)
    }
}
