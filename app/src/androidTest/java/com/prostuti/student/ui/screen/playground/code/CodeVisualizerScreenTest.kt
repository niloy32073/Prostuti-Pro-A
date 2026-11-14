package com.prostuti.student.ui.screen.playground.code

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.prostuti.student.R
import com.prostuti.student.data.repository.CodeRunRepositoryImpl
import com.prostuti.student.data.repository.PythonRuntimeRepositoryImpl
import com.prostuti.student.domain.BuildCodeFlowUseCase
import com.prostuti.student.domain.InstallPythonRuntimeUseCase
import com.prostuti.student.domain.ObservePythonRuntimeUseCase
import com.prostuti.student.domain.RunCUseCase
import com.prostuti.student.domain.RunPythonUseCase
import com.prostuti.student.domain.SendRunInputUseCase
import com.prostuti.student.domain.SendTraceCommandUseCase
import com.prostuti.student.ui.theme.ProstutiTheme
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** The code visualizer on a device: editing, errors, language switch, examples, charts. Saves screenshots. */
@RunWith(AndroidJUnit4::class)
class CodeVisualizerScreenTest {

    @get:Rule val rule = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var vm: CodeVisualizerViewModel

    private fun launch() {
        val runtime = PythonRuntimeRepositoryImpl(context)
        val runner = CodeRunRepositoryImpl(context)
        vm = CodeVisualizerViewModel(
            BuildCodeFlowUseCase(), Dispatchers.Default, SavedStateHandle(),
            ObservePythonRuntimeUseCase(runtime), InstallPythonRuntimeUseCase(runtime), RunPythonUseCase(runner), SendRunInputUseCase(runner),
            RunCUseCase(runner), SendTraceCommandUseCase(runner),
        )
        rule.setContent { ProstutiTheme { CodeVisualizerScreen(onBack = {}, viewModel = vm) } }
        rule.waitForIdle()
    }

    private fun shot(name: String) {
        rule.waitForIdle()
        Thread.sleep(400)
        rule.waitForIdle()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            ?: rule.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(context.getExternalFilesDir(null), "code_shots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** On a phone the chart is a tab of its own; on wider windows it is always beside the code. */
    private fun showChart() {
        val tab = rule.onAllNodes(hasTestTag("tab_flowchart")).fetchSemanticsNodes()
        if (tab.isNotEmpty()) rule.onNodeWithTag("tab_flowchart").performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasTestTag("flowchart_canvas")).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun showCode() {
        val tab = rule.onAllNodes(hasTestTag("tab_code")).fetchSemanticsNodes()
        if (tab.isNotEmpty()) rule.onNodeWithTag("tab_code").performClick()
    }

    private fun loadExample(titleRes: Int) {
        rule.onNodeWithTag("code_examples").performClick()
        rule.onNodeWithTag("code_examples_sheet").performScrollToNode(hasTestTag("code_example_$titleRes"))
        rule.onNodeWithTag("code_example_$titleRes").performClick()
        rule.waitForIdle()
    }

    @Test fun starterCodeShowsItsFlowchart() {
        launch()
        shot("01_code")
        showChart()
        rule.onNodeWithTag("flowchart_canvas").assertIsDisplayed()
        shot("02_chart_c_sum")
    }

    @Test fun aSyntaxErrorShowsTheBannerAndKeepsTheChart() {
        launch()
        showCode()
        rule.onNodeWithTag("code_editor").performTextReplacement("int main() {\n    int a = 5\n    printf(\"%d\", a);\n}")
        rule.waitUntil(5_000) { rule.onAllNodes(hasTestTag("code_error")).fetchSemanticsNodes().isNotEmpty() }
        shot("03_error")
        showChart()
        rule.onNodeWithTag("code_stale_note").assertIsDisplayed()
    }

    @Test fun examplesInBothLanguages() {
        launch()
        val picks = listOf(
            R.string.code_ex_grade to "grade",
            R.string.code_ex_prime to "prime",
            R.string.code_ex_calculator to "calculator",
            R.string.code_ex_reverse to "reverse",
            R.string.code_ex_bubble to "bubble",
        )
        for ((res, name) in picks) {
            showCode()
            loadExample(res)
            showChart()
            shot("10_c_$name")
        }
        showCode()
        rule.onNodeWithTag("language_PYTHON").performClick()
        for ((res, name) in picks) {
            showCode()
            loadExample(res)
            showChart()
            shot("20_py_$name")
        }
    }

    /** Needs the Python module installed natively (see CodeRunRepositoryTest); otherwise shows the download dialog. */
    @Test fun runningPythonWithInput() {
        launch()
        showCode()
        rule.onNodeWithTag("language_PYTHON").performClick()
        loadExample(R.string.code_ex_factorial)
        rule.onNodeWithTag("code_run").performClick()
        rule.waitForIdle()
        if (rule.onAllNodes(hasTestTag("download_python")).fetchSemanticsNodes().isNotEmpty()) {
            shot("40_download_dialog")
            rule.onNodeWithTag("download_python").performClick()
            rule.waitUntil(15_000) { vm.state.value.runStatus == RunStatus.IDLE }
            shot("41_download_failed")
            return
        }
        rule.waitUntil(20_000) { vm.state.value.runStatus == RunStatus.WAITING_INPUT }
        rule.onNodeWithTag("console_input").performTextInput("5")
        rule.onNodeWithTag("console_send").performClick()
        rule.waitUntil(10_000) { vm.state.value.runStatus == RunStatus.DONE }
        shot("42_python_output")
        rule.onNodeWithTag("console_output").assertIsDisplayed()

        showCode()
        rule.onNodeWithTag("code_editor").performTextReplacement("x = [1, 2, 3]\nprint(x[1])\nprint(x[5])\n")
        rule.onNodeWithTag("code_run").performClick()
        rule.waitUntil(20_000) { vm.state.value.runStatus == RunStatus.ERROR }
        shot("43_python_error")
        showCode()
        shot("44_error_line_in_editor")

        rule.onNodeWithTag("code_editor").performTextReplacement("n = int(input(\"n = \"))\nfor i in range(1, n + 1):\n    print(i, \"x\", i, \"=\", i * i)\n")
        rule.onNodeWithTag("code_run").performClick()
        rule.waitUntil(20_000) { vm.state.value.runStatus == RunStatus.WAITING_INPUT }
        rule.onNodeWithTag("console_input").performTextInput("4")
        rule.onNodeWithTag("console_send").performClick()
        rule.waitUntil(10_000) { vm.state.value.runStatus == RunStatus.DONE }
        shot("45_prompt_and_answer")
    }

    @Test fun runningCWithScanf() {
        launch()
        showCode()
        loadExample(R.string.code_ex_largest)
        rule.onNodeWithTag("code_run").performClick()
        rule.waitUntil(10_000) { vm.state.value.runStatus == RunStatus.WAITING_INPUT }
        rule.onNodeWithTag("console_input").performTextInput("12 45 7")
        rule.onNodeWithTag("console_send").performClick()
        rule.waitUntil(10_000) { vm.state.value.runStatus == RunStatus.DONE }
        shot("50_c_scanf_output")

        showCode()
        rule.onNodeWithTag("code_editor").performTextReplacement("#include <stdio.h>\nint main() {\n    int a[3] = {1, 2, 3};\n    for (int i = 0; i <= 3; i++)\n        printf(\"%d\\n\", a[i]);\n    return 0;\n}\n")
        rule.onNodeWithTag("code_run").performClick()
        rule.waitUntil(10_000) { vm.state.value.runStatus == RunStatus.ERROR }
        shot("51_c_runtime_error")
        showCode()
        shot("52_c_error_line")

        rule.onNodeWithTag("code_editor").performTextReplacement("int main() {\n    while (1) { }\n}\n")
        rule.onNodeWithTag("code_run").performClick()
        rule.waitUntil(5_000) { vm.state.value.runStatus == RunStatus.RUNNING || vm.state.value.runStatus == RunStatus.STARTING }
        Thread.sleep(500)
        rule.onNodeWithTag("console_stop").performClick()
        rule.waitUntil(5_000) { vm.state.value.runStatus == RunStatus.STOPPED }
    }

    @Test fun steppingThroughAC_Program() {
        launch()
        showCode()
        rule.onNodeWithTag("code_editor").performTextReplacement(
            "#include <stdio.h>\nint square(int x) {\n    return x * x;\n}\nint main() {\n    int total = 0;\n" +
                "    for (int i = 1; i <= 3; i++) {\n        total += square(i);\n    }\n    printf(\"%d\\n\", total);\n    return 0;\n}\n",
        )
        rule.waitUntil(5_000) { vm.state.value.program?.functions?.size == 2 }
        rule.onNodeWithTag("code_trace").performClick()
        rule.waitUntil(10_000) { vm.state.value.runStatus == RunStatus.PAUSED }
        assertEquals(6, vm.state.value.traceLine)
        rule.onNodeWithTag("trace_controls").assertIsDisplayed()
        shot("60_trace_first_line")

        fun step() {
            rule.onNodeWithTag("trace_step").performClick()
            rule.waitUntil(10_000) { vm.state.value.runStatus == RunStatus.PAUSED }
        }
        repeat(3) { step() }   // 7 (for), 8, then inside square at 3
        assertEquals(3, vm.state.value.traceLine)
        assertEquals(listOf("main", "square"), vm.state.value.trace!!.frames.map { it.function })
        assertEquals("square", vm.state.value.chartName)
        showChart()
        shot("61_trace_in_square_chart")
        if (rule.onAllNodes(hasTestTag("tab_variables")).fetchSemanticsNodes().isNotEmpty()) rule.onNodeWithTag("tab_variables").performClick()
        rule.onNodeWithTag("variables_panel").assertIsDisplayed()
        shot("62_trace_variables")

        rule.onNodeWithTag("trace_play").performClick()
        rule.waitUntil(10_000) { (vm.state.value.trace?.frames?.first()?.vars?.firstOrNull { it.name == "total" }?.value ?: "0") != "0" }
        rule.onNodeWithTag("trace_pause").performClick()
        rule.onNodeWithTag("trace_to_end").performClick()
        rule.waitUntil(10_000) { vm.state.value.runStatus == RunStatus.DONE }
        assertEquals("14\n", vm.state.value.console.joinToString("") { it.text })
        shot("63_trace_done")
    }

    /** Needs the Python module installed natively (see CodeRunRepositoryTest); skipped otherwise. */
    @Test fun steppingThroughPython() {
        org.junit.Assume.assumeTrue(PythonRuntimeRepositoryImpl(context).state.value == com.prostuti.student.domain.code.run.RuntimeState.Installed)
        launch()
        showCode()
        rule.onNodeWithTag("language_PYTHON").performClick()
        rule.onNodeWithTag("code_editor").performTextReplacement("marks = [70, 85]\ntotal = 0\nfor m in marks:\n    total += m\nprint(total)\n")
        rule.waitUntil(5_000) { vm.state.value.program != null && vm.state.value.error == null }
        rule.onNodeWithTag("code_trace").performClick()
        rule.waitUntil(20_000) { vm.state.value.runStatus == RunStatus.PAUSED }
        repeat(3) {
            rule.onNodeWithTag("trace_step").performClick()
            rule.waitUntil(10_000) { vm.state.value.runStatus == RunStatus.PAUSED }
        }
        assertEquals(4, vm.state.value.traceLine)
        val vars = vm.state.value.trace!!.frames.single().vars.associate { it.name to it.value }
        assertEquals(mapOf("marks" to "[70, 85]", "total" to "0", "m" to "70"), vars)
        if (rule.onAllNodes(hasTestTag("tab_variables")).fetchSemanticsNodes().isNotEmpty()) rule.onNodeWithTag("tab_variables").performClick()
        shot("64_python_trace_variables")
        rule.onNodeWithTag("trace_to_end").performClick()
        rule.waitUntil(10_000) { vm.state.value.runStatus == RunStatus.DONE }
        assertEquals("155\n", vm.state.value.console.joinToString("") { it.text })
    }

    @Test fun runningCWithStructsAndGoto() {
        launch()
        showCode()
        rule.onNodeWithTag("code_editor").performTextReplacement(
            "#include <stdio.h>\ntypedef struct { char name[10]; int marks; } Student;\nint main() {\n" +
                "    Student s[2] = { {\"Rina\", 80}, {\"Tom\", 65} };\n    int i = 0;\nnext:\n" +
                "    printf(\"%s %d\\n\", s[i].name, s[i].marks);\n    i++;\n    if (i < 2) goto next;\n    return 0;\n}\n",
        )
        rule.onNodeWithTag("code_run").performClick()
        rule.waitUntil(10_000) { vm.state.value.runStatus == RunStatus.DONE }
        val out = vm.state.value.console.joinToString("") { it.text }
        org.junit.Assert.assertEquals("Rina 80\nTom 65\n", out)
        shot("54_c_struct_output")
        showChart()
        shot("55_c_goto_chart")
    }

    @Test fun functionsGetTheirOwnCharts() {
        launch()
        loadExample(R.string.code_ex_factorial_rec)
        showChart()
        rule.waitUntil(5_000) { rule.onAllNodes(hasTestTag("chart_factorial")).fetchSemanticsNodes().isNotEmpty() }
        shot("30_main_with_call")
        rule.onNodeWithTag("chart_factorial").performClick()
        shot("31_factorial_chart")
    }
}
