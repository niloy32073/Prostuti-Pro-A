package com.prostuti.student.ui.screen.playground.graph

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostuti.student.data.service.local.SavedGraphEntity
import com.prostuti.student.ui.screen.playground.PlaygroundHubContent
import com.prostuti.student.ui.screen.playground.PlaygroundHubUiState
import com.prostuti.student.ui.screen.playground.graph.share.GraphCapture
import com.prostuti.student.ui.screen.playground.graph.share.GraphShareImage
import com.prostuti.student.ui.screen.playground.graph.share.ShareRows
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.prostuti.student.R
import com.prostuti.student.data.repository.fake.FakeSavedGraphRepository
import com.prostuti.student.domain.GetSavedGraphUseCase
import com.prostuti.student.domain.SaveGraphUseCase
import com.prostuti.student.domain.graph.GraphCompiler
import com.prostuti.student.domain.graph.space.SpacePlotter
import com.prostuti.student.domain.graph.plot.GraphPlotter
import com.prostuti.student.ui.theme.ProstutiTheme
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class GraphBuilderScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun launch(): GraphBuilderViewModel {
        val library = FakeSavedGraphRepository()
        val vm = GraphBuilderViewModel(
            GraphCompiler(), GraphPlotter(), Dispatchers.Default, SavedStateHandle(), SpacePlotter(),
            GetSavedGraphUseCase(library), SaveGraphUseCase(library),
        )
        rule.setContent { ProstutiTheme { GraphBuilderScreen(onBack = {}, viewModel = vm) } }
        return vm
    }

    private fun key(id: String) = rule.onNodeWithTag("math_key_$id").performClick()

    private fun waitForTag(tag: String) = rule.waitUntil(timeoutMillis = 10_000) {
        rule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
    }

    private fun waitForText(text: String) = rule.waitUntil(timeoutMillis = 10_000) {
        rule.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()
    }

    /** Saves the screen to the app's external files dir (pulled with adb for visual review). */
    private fun shot(name: String) {
        rule.waitForIdle()
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(context.getExternalFilesDir(null), "graph_shots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun typingWithTheMathKeyboardDrawsAndEvaluates() {
        launch()
        rule.onNodeWithTag("expression_display_0").performClick()
        rule.onNodeWithTag("math_keyboard").assertIsDisplayed()
        // 2 + 3 → "= 5"
        key("2"); key("+"); key("3")
        waitForText("= 5")
        shot("01_value")
        // Backspace clears it, then y = x²
        repeat(3) { key("backspace") }
        key("y"); key("="); key("x"); key("square")
        waitForTag("expression_preview")
        rule.onNodeWithTag("expression_editor").assertIsDisplayed()
        shot("02_parabola")
    }

    @Test
    fun undefinedLetterOffersASlider() {
        val vm = launch()
        rule.runOnIdle { vm.addExample(GraphExample(R.string.graph_ex_line, listOf("y = a sin(x)"))) }
        waitForTag("add_slider_a")
        rule.onNodeWithTag("add_slider_a").performClick()
        waitForTag("parameter_slider")
        shot("03_slider")
    }

    @Test
    fun integralShowsItsExactValueAndArea() {
        val vm = launch()
        rule.runOnIdle { vm.addExample(GraphExample(R.string.graph_ex_integral, listOf("∫_0^2 x^2 dx"))) }
        waitForText("8/3")
        rule.onNodeWithText(context.getString(R.string.graph_area_note)).assertIsDisplayed()
        shot("04_integral")
    }

    @Test
    fun examplesSheetAddsLines() {
        val vm = launch()
        rule.onNodeWithTag("open_examples").performClick()
        waitForTag("examples_sheet")
        rule.onNodeWithTag("example_${R.string.graph_ex_circle}").performClick()
        rule.waitUntil(10_000) { vm.state.value.lines.any { it.text == "x^2 + y^2 = 25" } }
        rule.waitUntil(10_000) { vm.state.value.geometries.isNotEmpty() }
        shot("05_circle")
    }

    @Test
    fun errorsAreExplainedInPlainWords() {
        val vm = launch()
        rule.runOnIdle { vm.addExample(GraphExample(R.string.graph_ex_line, listOf("y = (x + 1"))) }
        // a missing ")" at the very end is forgiven: no error, the line draws
        rule.waitUntil(10_000) { vm.state.value.geometries.isNotEmpty() }
        rule.runOnIdle { vm.addExample(GraphExample(R.string.graph_ex_line, listOf("y = x # 2"))) }
        waitForTag("expression_error")
        assertTrue(vm.state.value.statuses.values.any { it.error != null })
    }

    @Test
    fun showcase() {
        val vm = launch()
        val lines = listOf(
            "y = sin(x)", "y = tan(x)", "x^2/16 + y^2/9 = 1", "y > x^2 - 4", "r = 4cos(3θ)",
            "∫_(-π)^π sin(x) dx", "(t - sin(t), 1 - cos(t))", "(2, 3)",
        )
        rule.runOnIdle { vm.addExample(GraphExample(R.string.graph_ex_line, lines)) }
        rule.waitUntil(15_000) { vm.state.value.geometries.size >= lines.size }
        shot("06_showcase")
        rule.runOnIdle { vm.onFocus(vm.state.value.lines[0].id) }
        rule.waitUntil(10_000) { vm.state.value.geometries.any { it.pointsOfInterest.isNotEmpty() } }
        shot("07_points_and_keyboard")
    }

    // ── 3D ──────────────────────────────────────────────────────────────────────────────────

    /** The 3D view is OpenGL, which Compose's own capture can't see: take a real screen shot. */
    private fun screenShot(name: String) {
        rule.waitForIdle()
        Thread.sleep(1200)
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: return
        val dir = File(context.getExternalFilesDir(null), "graph_shots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun space(vararg lines: String, options: (GraphBuilderViewModel) -> Unit = {}): GraphBuilderViewModel {
        val vm = launch()
        rule.onNodeWithTag("mode_SPACE").performClick()
        rule.runOnIdle { vm.addExample(GraphExample(R.string.graph_ex_sphere, lines.toList(), space = true)) }
        rule.waitUntil(20_000) { vm.state.value.spaceGeometries.size >= lines.size }
        rule.runOnIdle { options(vm) }
        rule.waitUntil(20_000) { vm.state.value.spaceGeometries.size >= lines.size }
        return vm
    }

    @Test
    fun spaceSurfaces() {
        space("z = (x^2 - y^2)/4")
        rule.onNodeWithTag("space_view").assertIsDisplayed()
        screenShot("08_space_saddle")
    }

    @Test
    fun spaceSolidsCurvesAndVectors() {
        space("x^2 + y^2 + z^2 = 9", "(2cos(4t), 2sin(4t), t/2)", "vector((0, 0, 0), (3, 2, 4))", "(3, 2, 4)")
        screenShot("09_space_sphere_helix_vector")
    }

    @Test
    fun spaceHeightColoursAndContours() {
        space("z = 2sin(sqrt(x^2 + y^2))") { vm ->
            val line = vm.state.value.lines.last()
            vm.setOptions(line.id, line.options.copy(colorByHeight = true, showContours = true, showWireframe = false))
        }
        screenShot("10_space_ripple_height")
    }

    @Test
    fun spaceVolumes() {
        val vm = space("revolvex(sqrt(x), 0, 4)")
        rule.runOnIdle { vm.rotateCamera(20f, -8f) }
        waitForText("Volume")
        screenShot("11_space_revolution")
    }

    @Test
    fun revolveFrom2DIntegral() {
        val vm = launch()
        rule.runOnIdle { vm.addExample(GraphExample(R.string.graph_ex_integral, listOf("∫_0^1 (x - x^2) dx"))) }
        waitForText("1/6")
        rule.onNodeWithTag("expression_options_0").performClick()
        waitForTag("revolve_x")
        rule.onNodeWithTag("revolve_x").performClick()
        rule.waitUntil(20_000) { vm.state.value.isSpace && vm.state.value.spaceGeometries.isNotEmpty() }
        screenShot("12_space_washer_from_2d")
    }

    // ── Phase 3 ─────────────────────────────────────────────────────────────────────────────

    private fun template(labelRes: Int) = PhysicsTemplates.categories.flatMap { it.examples }.first { it.labelRes == labelRes }

    @Test
    fun physicsTemplateFromTheSheet() {
        val vm = launch()
        rule.onNodeWithTag("open_examples").performClick()
        waitForTag("examples_sheet")
        rule.onNodeWithTag("tab_physics").performClick()
        rule.onNodeWithTag("example_${R.string.phys_projectile}").performClick()
        rule.waitUntil(15_000) { vm.state.value.geometries.size >= 3 }
        assertTrue(vm.state.value.lines.first().text.startsWith("# "))
        shot("13_projectile_template")
    }

    @Test
    fun templateAsksBeforeReplacingWork() {
        val vm = launch()
        rule.runOnIdle { vm.addExample(GraphExample(R.string.graph_ex_line, listOf("g = 1", "y = g x"))) }
        rule.onNodeWithTag("open_examples").performClick()
        waitForTag("examples_sheet")
        rule.onNodeWithTag("tab_physics").performClick()
        rule.onNodeWithTag("example_${R.string.phys_free_fall}").performClick()
        waitForTag("template_replace")
        rule.onNodeWithTag("template_replace").performClick()
        rule.waitUntil(10_000) { vm.state.value.lines.none { it.text == "y = g x" } }
        assertTrue(vm.state.value.lines.any { it.text == "g = 9.8" })
    }

    @Test
    fun labTableShowsItsFit() {
        val vm = launch()
        rule.runOnIdle { vm.addExample(template(R.string.phys_lab_pendulum), "note", true) }
        waitForTag("fit_stats")
        rule.onNodeWithTag("expression_list").performScrollToNode(hasTestTag("fit_stats"))
        rule.onNodeWithTag("fit_equation").assertIsDisplayed()
        shot("14_lab_table_fit")
        // switching the fit family re-fits at once
        rule.onNodeWithTag("fit_LINEAR").performClick()
        rule.waitUntil(10_000) { vm.state.value.statuses.values.any { it.fitText?.contains("x +") == true || it.fitText?.contains("x -") == true } }
    }

    @Test
    fun fieldTemplates() {
        val vm = launch()
        rule.runOnIdle { vm.addExample(template(R.string.phys_dipole), "note", true) }
        rule.waitUntil(15_000) { vm.state.value.geometries.size >= 4 }
        shot("15_dipole_field")
    }

    @Test
    fun saveFromTheMenu() {
        val vm = launch()
        rule.runOnIdle { vm.addExample(GraphExample(R.string.graph_ex_line, listOf("y = x^2"))) }
        rule.onNodeWithTag("graph_more").performClick()
        rule.onNodeWithTag("menu_save").performClick()
        waitForTag("save_name")
        rule.onNodeWithTag("confirm_save").performClick()
        waitForText(context.getString(R.string.graph_saved).take(8))
        rule.waitUntil(10_000) { vm.state.value.savedGraphId != null && !vm.state.value.isDirty }
    }

    @Test
    fun shareImageIsComposed() {
        val library = FakeSavedGraphRepository()
        val vm = GraphBuilderViewModel(
            GraphCompiler(), GraphPlotter(), Dispatchers.Default, SavedStateHandle(), SpacePlotter(),
            GetSavedGraphUseCase(library), SaveGraphUseCase(library),
        )
        val capture = GraphCapture()
        rule.setContent {
            ProstutiTheme {
                val state by vm.state.collectAsStateWithLifecycle()
                GraphBuilderContent(state, GraphBuilderActions(onCanvasSize = vm::onCanvasSize), capture)
            }
        }
        rule.runOnIdle { vm.addExample(GraphExample(R.string.graph_ex_integral, listOf("y = sin(x)", "∫_0^π sin(x) dx", "# area under one arch"))) }
        rule.waitUntil(15_000) { vm.state.value.geometries.size >= 2 }
        rule.waitForIdle()
        val image = runBlocking {
            val shot = capture.capture(space = false)!!
            val rows = ShareRows.from(vm.state.value, listOf(0xFFC74440.toInt(), 0xFF2D70B3.toInt(), 0xFF388C46.toInt()), "undefined")
            GraphShareImage.compose(shot, rows, "Prostuti · Graph Builder", 0xFFFFFFFF.toInt(), 0xFF1F1F1F.toInt(), 0xFF666666.toInt(), 2.75f)
        }
        assertTrue(image.height > image.width / 2)
        val dir = File(context.getExternalFilesDir(null), "graph_shots").apply { mkdirs() }
        File(dir, "16_share_image.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun hubListsSavedGraphs() {
        val graphs = listOf(
            SavedGraphEntity(1, "Projectile", "{}", "v_0 = 20   α = 45", false, System.currentTimeMillis()),
            SavedGraphEntity(2, "Sphere", "{}", "x^2 + y^2 + z^2 = 9", true, System.currentTimeMillis() - 3_600_000),
        )
        var opened: Long? = -1
        rule.setContent {
            ProstutiTheme { PlaygroundHubContent(PlaygroundHubUiState(graphs, isLoading = false), onBack = {}, onOpenGraph = { opened = it }) }
        }
        rule.onNodeWithText("Projectile").assertIsDisplayed()
        rule.onNodeWithText("Sphere").assertIsDisplayed()
        shot("17_hub_my_graphs")
        rule.onNodeWithTag("saved_graph_2").performClick()
        rule.runOnIdle { assertEquals(2L, opened) }
        rule.onNodeWithTag("playground_graph_builder").performClick()
        rule.runOnIdle { assertEquals(null, opened) }
    }

    // ── Phone panel ─────────────────────────────────────────────────────────────────────────

    @Test
    fun equationPanelSlidesDownAndBackUp() {
        val vm = launch()
        rule.runOnIdle { vm.addExample(GraphExample(R.string.graph_ex_line, listOf("y = sin(x)", "y = x^2/4 - 2"))) }
        rule.onNodeWithTag("expression_display_0").performClick()
        rule.onNodeWithTag("math_keyboard").assertIsDisplayed()

        // Tap the handle: list and keyboard slide away, the bar counts the equations.
        rule.onNodeWithTag("panel_handle").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("expression_list").assertIsNotDisplayed()
        rule.onAllNodes(hasTestTag("math_keyboard")).assertCountEquals(0)
        rule.onNodeWithTag("panel_count", useUnmergedTree = true).assertTextContains("2", substring = true)
        shot("18_panel_collapsed")

        // Swipe it back up, then down again.
        rule.onNodeWithTag("panel_handle").performTouchInput { swipeUp(startY = centerY, endY = centerY - 600f) }
        rule.waitForIdle()
        rule.onNodeWithTag("expression_list").assertIsDisplayed()
        shot("19_panel_open")
        rule.onNodeWithTag("panel_handle").performTouchInput { swipeDown(startY = centerY, endY = centerY + 600f) }
        rule.waitForIdle()
        rule.onNodeWithTag("expression_list").assertIsNotDisplayed()
        rule.runOnIdle { assertTrue(vm.state.value.panelCollapsed) }
    }

    @Test
    fun spaceWithPanelDown() {
        space("x^2 + y^2 + z^2 = 9", "(2cos(4t), 2sin(4t), t/2)")
        rule.onNodeWithTag("panel_handle").performClick()
        screenShot("20_space_panel_down")
    }
}
