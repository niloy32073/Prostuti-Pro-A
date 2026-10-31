package com.prostuti.student.ui.screen.playground.graph

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.prostuti.student.R
import com.prostuti.student.domain.graph.GraphCompiler
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
        val vm = GraphBuilderViewModel(GraphCompiler(), GraphPlotter(), Dispatchers.Default, SavedStateHandle())
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
}
