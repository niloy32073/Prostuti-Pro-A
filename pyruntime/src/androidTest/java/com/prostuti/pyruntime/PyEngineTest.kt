package com.prostuti.pyruntime

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.function.BiConsumer
import java.util.function.Function

/** Runs real Python 3.11 on the device. */
@RunWith(AndroidJUnit4::class)
class PyEngineTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private class Result(val out: String, val err: String, val error: String?, val prompts: List<String>)

    private fun run(code: String, vararg inputs: String): Result {
        val out = StringBuilder()
        val err = StringBuilder()
        val prompts = mutableListOf<String>()
        val queue = ArrayDeque(inputs.toList())
        val error = PyEngine.run(
            context, code.trimIndent(),
            BiConsumer { text, isError -> (if (isError) err else out).append(text) },
            Function { prompt -> prompts += prompt; queue.removeFirstOrNull() },
            null,
        )
        return Result(out.toString(), err.toString(), error, prompts)
    }

    @Test fun printsAndComputes() {
        val r = run(
            """
            import sys
            total = sum(range(1, 11))
            print("sum =", total)
            print(sys.version_info[:2])
            """
        )
        assertNull(r.error)
        assertEquals("sum = 55\n(3, 11)\n", r.out)
    }

    @Test fun inputReadsLinesWithPrompts() {
        val r = run(
            """
            a = int(input("a = "))
            b = int(input())
            print(a + b)
            """,
            "2", "40",
        )
        assertNull(r.error)
        assertEquals(listOf("a = ", ""), r.prompts)
        assertEquals("a = 42\n", r.out)
    }

    @Test fun inputPastTheEndIsEof() {
        val r = run("x = input()")
        assertTrue(r.error!!, r.error.contains("EOFError"))
    }

    @Test fun errorsShowOnlyTheStudentsLines() {
        val r = run(
            """
            def f(n):
                return 10 / n
            print("before")
            f(0)
            """
        )
        assertEquals("before\n", r.out)
        val error = r.error!!
        assertTrue(error, error.contains("File \"main.py\", line 4"))
        assertTrue(error, error.contains("File \"main.py\", line 2, in f"))
        assertTrue(error, error.contains("ZeroDivisionError"))
        assertTrue(error, !error.contains("prostuti_runner"))
    }

    @Test fun syntaxErrorsAndStderr() {
        val bad = run("print(\"hi\"")
        assertTrue(bad.error!!, bad.error.contains("SyntaxError") && bad.error.contains("line 1"))
        val err = run("import sys\nsys.stderr.write('oops')\nprint('ok')")
        assertNull(err.error)
        assertEquals("oops", err.err)
        assertEquals("ok\n", err.out)
    }

    @Test fun exitCodes() {
        assertNull(run("import sys\nprint(1)\nsys.exit()").error)
        assertEquals("SystemExit: 3\n", run("raise SystemExit(3)").error)
    }

    @Test fun eachRunStartsWithFreshGlobals() {
        run("leftover = 1")
        val r = run("print('leftover' in globals())")
        assertEquals("False\n", r.out)
    }

    @Test fun aTracedRunPausesBeforeEachLineWithTheStack() {
        val steps = mutableListOf<String>()
        val out = StringBuilder()
        val error = PyEngine.run(
            context,
            "def sq(x):\n    return x * x\nn = sq(3)\nprint(n)\n",
            BiConsumer { text, _ -> out.append(text) },
            Function { _ -> null },
            Function { json -> steps += json; if (steps.size >= 4) "run" else "step" },
        )
        assertNull(error)
        assertEquals("9\n", out.toString())
        assertEquals(4, steps.size)
        // line 1 (def), line 3 (call), then inside sq at line 2 with x = 3.
        assertTrue(steps[0], steps[0].contains("\"line\": 1"))
        assertTrue(steps[2], steps[2].contains("\"function\": \"sq\"") && steps[2].contains("\"name\": \"x\", \"value\": \"3\""))
    }
}
