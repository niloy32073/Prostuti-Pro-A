package com.prostuti.student.domain.code

import com.prostuti.student.domain.BuildCodeFlowUseCase
import com.prostuti.student.domain.code.flow.FlowMetrics
import com.prostuti.student.domain.code.flow.Flowchart
import com.prostuti.student.domain.code.flow.FlowchartLayout
import com.prostuti.student.domain.code.flow.NodeShape
import com.prostuti.student.ui.screen.playground.code.CodeExamples
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Geometry checks over many shapes of program: symbols never overlap, lines are horizontal or
 * vertical, no line runs through a symbol, everything is inside the chart, and every symbol but
 * the start (and code after a jump) is reached by a line.
 */
class FlowchartLayoutTest {

    private val metrics = FlowMetrics(lineHeight = 20f, charWidth = 8f, startLabel = "Start", endLabel = "End")
    private val layout = FlowchartLayout(metrics) { it.length * 8f }

    private fun charts(language: CodeLanguage, code: String): List<Flowchart> {
        val result = BuildCodeFlowUseCase()(language, code.trimIndent())
        val program = result.program ?: run { fail("parse error ${result.error}"); error("") }
        return program.functions.map { layout.layout(it) }
    }

    private fun check(language: CodeLanguage, code: String, expectUnreached: Int = 0) =
        charts(language, code).forEach { verify(it, expectUnreached) }

    private fun verify(chart: Flowchart, expectUnreached: Int) {
        val eps = 0.5f
        chart.nodes.forEachIndexed { i, a ->
            assertTrue("node inside", a.x - a.width / 2 >= -eps && a.x + a.width / 2 <= chart.width + eps && a.y >= -eps && a.y + a.height <= chart.height + eps)
            for (b in chart.nodes.drop(i + 1)) {
                val overlapX = min(a.x + a.width / 2, b.x + b.width / 2) - max(a.x - a.width / 2, b.x - b.width / 2)
                val overlapY = min(a.y + a.height, b.y + b.height) - max(a.y, b.y)
                assertTrue("'${a.lines}' overlaps '${b.lines}'", overlapX <= eps || overlapY <= eps)
            }
        }
        for (e in chart.edges) {
            e.points.zipWithNext { p, q ->
                assertTrue("segment $p→$q is not straight", abs(p.x - q.x) < eps || abs(p.y - q.y) < eps)
                assertTrue("point outside", p.x in -eps..chart.width + eps && p.y in -eps..chart.height + eps)
                for (n in chart.nodes) {
                    val l = n.x - n.width / 2 + 1f
                    val r = n.x + n.width / 2 - 1f
                    val t = n.y + 1f
                    val bt = n.y + n.height - 1f
                    val crosses = if (abs(p.x - q.x) < eps) {
                        p.x in l..r && max(p.y, q.y) > t && min(p.y, q.y) < bt
                    } else {
                        p.y in t..bt && max(p.x, q.x) > l && min(p.x, q.x) < r
                    }
                    assertTrue("segment $p→$q runs through '${n.lines}'", !crosses)
                }
            }
        }
        // Every symbol except the start is reached: some line ends on (or runs into) its outline.
        val unreached = chart.nodes.drop(1).count { n ->
            chart.edges.none { e ->
                val end = e.points.last()
                end.x in n.x - n.width / 2 - eps..n.x + n.width / 2 + eps && end.y in n.y - eps..n.y + n.height + eps
            }
        }
        assertEquals("unreached symbols", expectUnreached, unreached)
    }

    @Test fun straightLineProgram() {
        val chart = charts(CodeLanguage.C, "int main(){ int a; scanf(\"%d\", &a); a = a * 2; printf(\"%d\", a); }").single()
        verify(chart, 0)
        assertEquals(listOf(NodeShape.TERMINAL, NodeShape.PROCESS, NodeShape.IO, NodeShape.PROCESS, NodeShape.IO, NodeShape.TERMINAL), chart.nodes.map { it.shape })
        // One axis: every symbol is centred on the same x.
        assertEquals(1, chart.nodes.map { it.x }.distinct().size)
        // Top to bottom in code order.
        assertEquals(chart.nodes.sortedBy { it.y }, chart.nodes)
    }

    @Test fun branchesAndElifChains() = check(
        CodeLanguage.PYTHON,
        """
        x = int(input())
        if x > 100000000000:
            print("very very big number indeed, more than you can count")
            y = 1
        elif x > 0:
            print("pos")
        elif x < 0:
            if x < -10:
                print("very neg")
        else:
            pass
        print("end")
        """,
    )

    @Test fun loopsWithBreakContinueAndElse() = check(
        CodeLanguage.PYTHON,
        """
        for i in range(10):
            if i % 2 == 0:
                continue
            if i > 7:
                break
            print(i)
        else:
            print("no break")
        while True:
            n = int(input())
            if n == 0:
                break
        print("bye")
        """,
    )

    @Test fun nestedLoopsInFunctionsWithReturns() = check(
        CodeLanguage.PYTHON,
        """
        def is_prime(n):
            if n < 2:
                return False
            i = 2
            while i * i <= n:
                if n % i == 0:
                    return False
                i += 1
            return True

        def find(grid, target):
            for row in grid:
                for v in row:
                    if v == target:
                        return True
                    if v < 0:
                        raise ValueError("negative")
            return False
        """,
    )

    @Test fun cForDoWhileAndSwitch() = check(
        CodeLanguage.C,
        """
        int main() {
            int s = 0, op;
            for (int i = 1; i <= 10; i++) {
                if (i == 5) continue;
                if (i == 9) break;
                s += i;
            }
            do {
                scanf("%d", &op);
                switch (op) {
                    case 1: puts("one"); break;
                    case 2:
                    case 3: puts("two or three");
                    case 4: puts("four"); break;
                    default: puts("other");
                }
                if (op == 9) return 1;
            } while (op != 0);
            switch (s) { case 1: s = 2; }
            return 0;
        }
        """,
    )

    @Test fun pythonMatchAndTry() = check(
        CodeLanguage.PYTHON,
        """
        while True:
            cmd = input()
            match cmd:
                case "q":
                    break
                case "a" | "b":
                    print("letter")
                case _:
                    print("?")
            try:
                v = int(cmd)
            except ValueError:
                v = 0
            finally:
                print("tried")
        """,
    )

    @Test fun deadCodeAfterAJumpIsDrawnButNotReached() = check(
        CodeLanguage.C,
        "int f(int x) { return x; x = 2; }",
        expectUnreached = 1,
    )

    @Test fun gotoConnectorsEndTheFlowAndLabelsContinueIt() = check(
        CodeLanguage.C,
        """
        int main() {
            int i = 0;
        again:
            i++;
            if (i < 3) goto again;
            goto end;
            printf("skipped");
        end:
            return 0;
        }
        """,
        expectUnreached = 1,
    )

    @Test fun emptyBodies() = check(
        CodeLanguage.C,
        "int main() { while (x) {} if (y) {} else {} for (;;) ; do ; while (z); switch (q) {} }",
    )

    @Test fun everyExampleReadsAndLaysOutInBothLanguages() {
        for (example in CodeExamples.all) for (language in CodeLanguage.entries) {
            check(language, CodeExamples.code(example, language))
        }
    }

    @Test fun longTextWrapsInsideSymbols() {
        val chart = charts(CodeLanguage.PYTHON, "result = some_function_name(alpha_value, beta_value, gamma_value, delta_value)").single()
        verify(chart, 0)
        val step = chart.nodes[1]
        assertTrue(step.lines.size > 1)
        assertTrue(step.lines.all { it.length <= metrics.maxChars })
    }
}
