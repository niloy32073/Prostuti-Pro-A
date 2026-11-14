package com.prostuti.student.domain.code

import com.prostuti.student.domain.BuildCodeFlowUseCase
import com.prostuti.student.domain.code.c.CFlowParser
import com.prostuti.student.domain.code.flow.FlowBlock
import com.prostuti.student.domain.code.flow.FlowBlock.Branch
import com.prostuti.student.domain.code.flow.FlowBlock.Connector
import com.prostuti.student.domain.code.flow.FlowBlock.Jump
import com.prostuti.student.domain.code.flow.FlowBlock.Loop
import com.prostuti.student.domain.code.flow.FlowBlock.Step
import com.prostuti.student.domain.code.flow.FlowBlock.Switch
import com.prostuti.student.domain.code.flow.JumpKind
import com.prostuti.student.domain.code.flow.LoopKind
import com.prostuti.student.domain.code.flow.StepKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CFlowParserTest {

    private fun parse(code: String) = CFlowParser().parse(code.trimIndent())
    private fun main(code: String): List<FlowBlock> = parse(code).functions.single { it.isEntry }.body
    private fun error(code: String): CodeError {
        val r = BuildCodeFlowUseCase()(CodeLanguage.C, code.trimIndent())
        assertNull(r.program)
        return r.error!!
    }

    @Test fun mainWithIoAndProcessSteps() {
        val b = main(
            """
            #include <stdio.h>
            /* sum of two numbers */
            int main() {
                int a, b, sum;
                scanf("%d %d", &a, &b);
                sum = a + b;   // add
                printf("%d\n", sum);
                return 0;
            }
            """
        )
        assertEquals(listOf(StepKind.PROCESS, StepKind.IO, StepKind.PROCESS, StepKind.IO), b.dropLast(1).map { (it as Step).kind })
        assertEquals("""scanf("%d %d", &a, &b)""", (b[1] as Step).text)
        assertEquals(5, b[1].line)
        assertEquals(JumpKind.RETURN, (b.last() as Jump).kind)
        assertEquals("return 0", (b.last() as Jump).text)
    }

    @Test fun forLoopKeepsInitAndUpdate() {
        val loop = main("int main(){ for (int i = 0; i < n; i++) s += i; }").single() as Loop
        assertEquals(LoopKind.WHILE, loop.kind)
        assertEquals("i < n", loop.condition)
        assertEquals("int i = 0", loop.init!!.text)
        assertEquals("i++", loop.update!!.text)
        assertEquals(1, loop.body.size)
        val forever = main("int main(){ for (;;) { break; } }").single() as Loop
        assertEquals("true", forever.condition)
        assertNull(forever.init)
    }

    @Test fun ifElseChainsDoWhileAndContinue() {
        val b = main(
            """
            int main() {
                if (x > 0) puts("pos");
                else if (x < 0) { puts("neg"); }
                else puts("zero");
                do {
                    x--;
                    if (x == 3) continue;
                } while (x > 0);
            }
            """
        )
        val branch = b[0] as Branch
        assertEquals("x > 0", branch.condition)
        assertEquals("x < 0", (branch.no.single() as Branch).condition)
        val loop = b[1] as Loop
        assertEquals(LoopKind.DO_WHILE, loop.kind)
        assertEquals("x > 0", loop.condition)
        assertEquals(JumpKind.CONTINUE, ((loop.body[1] as Branch).yes.single() as Jump).kind)
    }

    @Test fun switchMergesEmptyLabelsAndKeepsDefault() {
        val sw = main(
            """
            int main() {
                switch (op) {
                    case 1:
                    case 2: puts("low"); break;
                    case 3: puts("three");
                    default: puts("other");
                }
            }
            """
        ).single() as Switch
        assertTrue(sw.fallsThrough)
        assertEquals(listOf("op == 1 || op == 2", "op == 3", null), sw.cases.map { it.test })
        assertEquals(JumpKind.BREAK, (sw.cases[0].body.last() as Jump).kind)
    }

    @Test fun functionsAndCalls() {
        val p = parse(
            """
            int fact(int n);
            struct point { int x; int y; };
            int fact(int n) {
                if (n <= 1) return 1;
                return n * fact(n - 1);
            }
            int main(void) {
                int r = fact(5);
                printf("%d", fact(3));
            }
            """
        )
        assertEquals(listOf("main", "fact"), p.functions.map { it.name })
        assertEquals("int fact(int n)", p.functions[1].header)
        val main = p.functions[0].body
        assertEquals(StepKind.CALL, (main[0] as Step).kind)
        assertEquals(StepKind.IO, (main[1] as Step).kind)   // output wins over the call
    }

    @Test fun gotoAndLabelsBecomeConnectors() {
        val b = main(
            """
            typedef struct { int x; } P;
            int main() {
                P p = { 1 };
            again:
                p.x++;
                if (p.x < 3) goto again;
                return 0;
            }
            """
        )
        assertEquals(Connector("again", isGoto = false, line = 4), b[1])
        assertEquals("p.x++", (b[2] as Step).text)
        assertEquals(listOf(Connector("again", isGoto = true, line = 6)), (b[3] as Branch).yes)
    }

    @Test fun snippetWithoutFunctionsIsMain() {
        val b = main("int s = 0;\nfor (int i = 1; i <= 10; i++) { s += i; }\nprintf(\"%d\", s);")
        assertEquals(3, b.size)
        assertTrue(b[1] is Loop)
    }

    @Test fun errors() {
        assertEquals(CodeError(3, CodeErrorKind.MISSING_SEMICOLON), error("int main() {\n  int a = 5;\n  a = a + 1\n  printf(\"%d\", a);\n}"))
        assertEquals(CodeError(2, CodeErrorKind.MISSING_SEMICOLON), error("int main() {\n  x = 1\n  if (x) y = 2;\n}"))
        assertEquals(CodeError(1, CodeErrorKind.UNCLOSED_BRACKET, "{"), error("int main() {\n  x = 1;\n"))
        assertEquals(CodeError(2, CodeErrorKind.MISPLACED, "else"), error("int main() {\n  else x = 1;\n}"))
        assertEquals(CodeError(2, CodeErrorKind.MISPLACED, "break"), error("int main() {\n  break;\n}"))
        assertEquals(CodeError(2, CodeErrorKind.UNTERMINATED_STRING), error("int main() {\n  printf(\"hi);\n}"))
        assertEquals(CodeError(1, CodeErrorKind.UNTERMINATED_COMMENT), error("/* open\nint main() {}"))
        assertEquals(CodeError(2, CodeErrorKind.EXPECTED, "while"), error("int main() {\n  do { x++; }\n}"))
        assertEquals(CodeError(2, CodeErrorKind.EXPECTED, ";"), error("int main() {\n  for (i = 0; i < 3) x++;\n}"))
    }

    @Test fun multiLineStatementIsNotAMissingSemicolon() {
        val b = main("int main() {\n  int total = a +\n    b;\n  return\n    total;\n}")
        assertEquals("int total = a + b", (b[0] as Step).text)
        assertEquals(2, b.size)
    }
}
