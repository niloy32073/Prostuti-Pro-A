package com.prostuti.student.domain.code

import com.prostuti.student.domain.code.c.run.CCompiler
import com.prostuti.student.domain.code.c.run.CInterpreter
import com.prostuti.student.domain.code.c.run.CIo
import com.prostuti.student.domain.code.c.run.CTracer
import com.prostuti.student.domain.code.run.TraceCommand
import com.prostuti.student.domain.code.run.TraceFrame
import com.prostuti.student.domain.code.run.TraceStep
import com.prostuti.student.domain.code.run.TraceVar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Step-by-step C runs: where they pause, the call stack, and how variables read. */
class CTraceTest {

    private class Traced(val steps: List<TraceStep>, val out: String)

    private fun trace(code: String, stopAfter: Int = Int.MAX_VALUE): Traced {
        val steps = mutableListOf<TraceStep>()
        val out = StringBuilder()
        val io = object : CIo {
            override fun write(text: String) { out.append(text) }
            override fun readLine(): String? = null
            override val stopped = false
        }
        val tracer = CTracer { step ->
            steps += step
            if (steps.size >= stopAfter) TraceCommand.RUN_TO_END else TraceCommand.STEP
        }
        var error: Throwable? = null
        val t = Thread(null, {
            try { CInterpreter(io, tracer).run(CCompiler.compile(code.trimIndent())) } catch (e: Throwable) { error = e }
        }, "c", 32L * 1024 * 1024)
        t.start()
        t.join()
        error?.let { throw it }
        return Traced(steps, out.toString())
    }

    private fun TraceFrame.vars(): Map<String, String?> = vars.associate { it.name to it.value }
    private fun TraceStep.variable(name: String): TraceVar = frames.last().vars.single { it.name == name }

    private val loopAndCall = """
        #include <stdio.h>
        int square(int x) {
            int r = x * x;
            return r;
        }
        int main() {
            int total = 0;
            for (int i = 1; i <= 2; i++) {
                int s = square(i);
                total += s;
            }
            printf("%d\n", total);
            return 0;
        }
    """

    @Test fun pausesBeforeEachStatementAndLoopTest() {
        val t = trace(loopAndCall)
        assertEquals(listOf(7, 8, 9, 3, 4, 10, 8, 9, 3, 4, 10, 8, 12, 13), t.steps.map { it.line })
        assertEquals("5\n", t.out)
    }

    @Test fun showsTheCallStackAndOnlyVariablesInScope() {
        val steps = trace(loopAndCall).steps
        // Inside square(1), called from line 9 of main; r is not declared yet.
        val inSquare = steps[3]
        assertEquals(listOf("main" to 9, "square" to 3), inSquare.frames.map { it.function to it.line })
        assertEquals(mapOf("x" to "1"), inSquare.frames[1].vars())
        assertEquals(mapOf("total" to "0", "i" to "1"), inSquare.frames[0].vars())
        assertEquals(mapOf("x" to "1", "r" to "1"), steps[4].frames[1].vars())
        assertEquals(mapOf("total" to "0", "i" to "1", "s" to "1"), steps[5].frames[0].vars())
        // Second time round, s is declared afresh: it is not shown with the old value before line 9 runs.
        assertEquals(mapOf("total" to "1", "i" to "2"), steps[7].frames[0].vars())
        // After the loop, i and s are out of scope.
        assertEquals(mapOf("total" to "5"), steps[12].frames.single().vars())
    }

    @Test fun runToEndStopsPausingButFinishes() {
        val t = trace(loopAndCall, stopAfter = 3)
        assertEquals(3, t.steps.size)
        assertEquals("5\n", t.out)
    }

    @Test fun valuesReadLikeC() {
        val code = """
            #include <stdio.h>
            struct P { int x; int y; };
            int counter = 7;
            int twice(int v) { return 2 * v; }
            int main() {
                int a[3] = {1, 2, 3};
                char name[10] = "Rina";
                struct P p = {1, 2};
                int *q = &a[1];
                char *s = name;
                double d = 2.5;
                float f = 80;
                char c = 'A';
                int u;
                int *none = NULL;
                int (*op)(int) = twice;
                return 0;
            }
        """
        val last = trace(code).steps.last()
        val a = last.variable("a")
        assertEquals("{1, 2, 3}", a.value)
        assertEquals(listOf("[0]" to "1", "[1]" to "2", "[2]" to "3"), a.children.map { it.name to it.value })
        assertEquals("\"Rina\"", last.variable("name").value)
        assertEquals("{x: 1, y: 2}", last.variable("p").value)
        assertEquals("→ a[1]", last.variable("q").value)
        assertEquals("→ name[0] \"Rina\"", last.variable("s").value)
        assertEquals("2.5", last.variable("d").value)
        assertEquals("80.0", last.variable("f").value)
        assertEquals("65 'A'", last.variable("c").value)
        assertNull(last.variable("u").value)
        assertEquals("NULL", last.variable("none").value)
        assertEquals("→ twice()", last.variable("op").value)
        assertEquals(listOf("counter" to "7"), last.globals.map { it.name to it.value })
    }
}
