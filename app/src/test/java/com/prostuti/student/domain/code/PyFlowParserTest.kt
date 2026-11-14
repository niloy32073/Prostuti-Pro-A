package com.prostuti.student.domain.code

import com.prostuti.student.domain.code.flow.FlowBlock
import com.prostuti.student.domain.code.flow.FlowBlock.Branch
import com.prostuti.student.domain.code.flow.FlowBlock.Jump
import com.prostuti.student.domain.code.flow.FlowBlock.Loop
import com.prostuti.student.domain.code.flow.FlowBlock.Step
import com.prostuti.student.domain.code.flow.FlowBlock.Switch
import com.prostuti.student.domain.code.flow.JumpKind
import com.prostuti.student.domain.code.flow.LoopKind
import com.prostuti.student.domain.code.flow.StepKind
import com.prostuti.student.domain.code.python.PyFlowParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PyFlowParserTest {

    private fun parse(code: String) = PyFlowParser().parse(code.trimIndent())
    private fun main(code: String): List<FlowBlock> = parse(code).functions.single { it.isEntry }.body
    private fun error(code: String): CodeError {
        val r = com.prostuti.student.domain.BuildCodeFlowUseCase()(CodeLanguage.PYTHON, code.trimIndent())
        assertNull(r.program)
        return r.error!!
    }

    @Test fun stepsAreClassifiedAsProcessOrIo() {
        val b = main(
            """
            n = int(input("n: "))
            s = n * 2
            print(s)
            """
        )
        assertEquals(listOf(StepKind.IO, StepKind.PROCESS, StepKind.IO), b.map { (it as Step).kind })
        assertEquals(listOf(1, 2, 3), b.map { it.line })
        assertEquals("""n = int(input("n: "))""", (b[0] as Step).text)
    }

    @Test fun ifElifElseNestsInTheNoBranch() {
        val b = main(
            """
            if x > 0:
                print("pos")
            elif x < 0:
                print("neg")
            else:
                print("zero")
            """
        ).single() as Branch
        assertEquals("x > 0", b.condition)
        val elif = b.no.single() as Branch
        assertEquals("x < 0", elif.condition)
        assertEquals(1, elif.no.size)
        assertEquals(6, elif.no.single().line)
    }

    @Test fun loopsBreaksAndElse() {
        val loop = main(
            """
            for i in range(10):
                if i == 5:
                    break
                continue
            else:
                print("done")
            """
        ).single() as Loop
        assertEquals(LoopKind.FOR_EACH, loop.kind)
        assertEquals("i in range(10)", loop.condition)
        val inner = loop.body[0] as Branch
        assertEquals(JumpKind.BREAK, (inner.yes.single() as Jump).kind)
        assertEquals(JumpKind.CONTINUE, (loop.body[1] as Jump).kind)
        assertEquals(1, loop.orElse.size)
    }

    @Test fun functionsGetTheirOwnChartsAndCallsAreMarked() {
        val p = parse(
            """
            def fact(n):
                '''Factorial.'''
                if n <= 1:
                    return 1
                return n * fact(n - 1)

            x = fact(5)
            print(x)
            """
        )
        assertEquals(listOf("main", "fact"), p.functions.map { it.name })
        val main = p.functions[0].body
        assertEquals(StepKind.CALL, (main[0] as Step).kind)
        assertEquals("fact", (main[0] as Step).callee)
        val fact = p.functions[1]
        assertEquals("fact(n)", fact.header)
        assertEquals(2, fact.body.size)   // the docstring is not a step
        assertEquals("fact", (fact.body[1] as Jump).callee)
    }

    @Test fun oneLinersSemicolonsCommentsAndBrackets() {
        val b = main(
            """
            a = 1; b = 2  # two steps
            total = (a +
                     b)
            if a: print(a)
            while a < 3: a += 1
            """
        )
        assertEquals(5, b.size)
        assertEquals("total = (a + b)", (b[2] as Step).text)
        assertEquals(1, (b[3] as Branch).yes.size)
        assertEquals(LoopKind.WHILE, (b[4] as Loop).kind)
    }

    @Test fun matchTryWithAndClasses() {
        val p = parse(
            """
            class Box:
                size = 3
                def area(self):
                    return self.size ** 2

            match cmd:
                case 1 | 2:
                    print("low")
                case _:
                    print("other")
            try:
                v = int(s)
            except ValueError:
                v = 0
            with open("f") as fh:
                data = fh.read()
            """
        )
        assertEquals(listOf("main", "Box.area"), p.functions.map { it.name })
        val main = p.functions[0].body
        val sw = main[0] as Switch
        assertEquals(listOf("cmd == 1 | 2", null), sw.cases.map { it.test })
        assertTrue(main[1] is Step && (main[1] as Step).text == "try")
        assertTrue(main.any { it is Branch && it.condition == "except ValueError" })
        assertTrue(main.any { it is Step && it.text == "with open(\"f\") as fh" })
    }

    @Test fun matchAsAVariableIsAStep() {
        assertTrue(main("match = 3").single() is Step)
    }

    @Test fun errorsPointAtTheRightLine() {
        assertEquals(CodeError(1, CodeErrorKind.EXPECTED, ":"), error("if x > 0\n    y = 1"))
        assertEquals(CodeError(2, CodeErrorKind.EXPECTED_BLOCK), error("if x:\ny = 1"))
        assertEquals(CodeError(2, CodeErrorKind.UNEXPECTED_INDENT), error("x = 1\n    y = 2"))
        assertEquals(CodeError(3, CodeErrorKind.BAD_DEDENT), error("if x:\n        y = 1\n    z = 2"))
        assertEquals(CodeError(1, CodeErrorKind.UNCLOSED_BRACKET, "("), error("print((1)\nx = 2"))
        assertEquals(CodeError(1, CodeErrorKind.UNEXPECTED_CLOSE, "]"), error("x = (1]"))
        assertEquals(CodeError(1, CodeErrorKind.UNTERMINATED_STRING), error("print(\"hi)"))
        assertEquals(CodeError(1, CodeErrorKind.MISPLACED, "break"), error("break"))
        assertEquals(CodeError(1, CodeErrorKind.MISPLACED, "return"), error("return 3"))
        assertEquals(CodeError(2, CodeErrorKind.MISPLACED, "else"), error("x = 1\nelse:\n    y = 2"))
        assertEquals(CodeError(1, CodeErrorKind.EXPECTED, "in"), error("for i range(3):\n    pass"))
    }

    @Test fun tripleQuotedStringsSpanLines() {
        val b = main("s = '''a\nb'''\nprint(s)")
        assertEquals(2, b.size)
        assertEquals(3, b[1].line)
    }

    @Test fun emptyCodeHasNoCharts() {
        assertTrue(parse("# only a comment\n\n").functions.isEmpty())
    }
}
