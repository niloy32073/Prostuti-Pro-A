package com.prostuti.student.domain.code

import com.prostuti.student.domain.BuildCodeFlowUseCase
import com.prostuti.student.domain.code.flow.functionAt
import com.prostuti.student.domain.code.run.TraceChanges
import com.prostuti.student.domain.code.run.TraceFrame
import com.prostuti.student.domain.code.run.TraceStep
import com.prostuti.student.domain.code.run.TraceVar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TraceHelpersTest {

    @Test fun changedValuesAreFoundByPath() {
        val before = TraceStep(
            3,
            listOf(TraceFrame("main", 3, listOf(TraceVar("n", "1"), TraceVar("a", "{1, 2}", listOf(TraceVar("[0]", "1"), TraceVar("[1]", "2")))))),
        )
        val after = TraceStep(
            4,
            listOf(
                TraceFrame("main", 4, listOf(TraceVar("n", "1"), TraceVar("a", "{1, 5}", listOf(TraceVar("[0]", "1"), TraceVar("[1]", "5"))))),
                TraceFrame("f", 9, listOf(TraceVar("x", "5"))),
            ),
            globals = listOf(TraceVar("g", "0")),
        )
        val changed = TraceChanges.changed(before, after)
        assertEquals(setOf("0/main/a", "0/main/a/[1]", "1/f/x", "globals/g"), changed)
        assertTrue(TraceChanges.changed(null, after).isEmpty())
        assertEquals("1/f/x", TraceChanges.path(1, after.frames[1], "x"))
    }

    @Test fun theChartOfTheFunctionAroundALine() {
        val c = BuildCodeFlowUseCase()(CodeLanguage.C, "int sq(int x) {\n  return x * x;\n}\nint main() {\n  int y = sq(3);\n  return y;\n}").program!!
        assertEquals("sq", c.functionAt(2)?.name)
        assertEquals("main", c.functionAt(5)?.name)
        val py = BuildCodeFlowUseCase()(
            CodeLanguage.PYTHON,
            "def outer(n):\n    def inner(k):\n        return k + 1\n    return inner(n)\nprint(outer(2))",
        ).program!!
        assertEquals("inner", py.functionAt(3)?.name)
        assertEquals("outer", py.functionAt(4)?.name)
        assertEquals("main", py.functionAt(5)?.name)
    }
}
