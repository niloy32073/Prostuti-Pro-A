package com.prostuti.student.domain.code.python

import com.prostuti.student.domain.code.CodeError
import com.prostuti.student.domain.code.CodeErrorKind
import com.prostuti.student.domain.code.CodeParseException
import com.prostuti.student.domain.code.CodeScan
import com.prostuti.student.domain.code.flow.FlowBlock
import com.prostuti.student.domain.code.flow.FlowFunction
import com.prostuti.student.domain.code.flow.FlowProgram
import com.prostuti.student.domain.code.flow.JumpKind
import com.prostuti.student.domain.code.flow.LoopKind
import com.prostuti.student.domain.code.flow.StepKind
import com.prostuti.student.domain.code.flow.SwitchCase
import com.prostuti.student.domain.code.flow.markCalls

/**
 * Reads Python source into flowchart blocks. Top-level code becomes the entry chart; every `def`
 * (methods as `Class.method`, nested functions too) gets a chart of its own. Throws
 * [CodeParseException] at the first problem, like Python's own SyntaxError.
 */
class PyFlowParser {

    private class Ctx(val inLoop: Boolean, val inFunction: Boolean, val prefix: String)

    private lateinit var lines: List<PyLine>
    private var pos = 0
    private val functions = mutableListOf<FlowFunction?>()

    fun parse(source: String): FlowProgram {
        lines = PyLineReader(source).read()
        pos = 0
        functions.clear()
        if (lines.isEmpty()) return FlowProgram(emptyList())
        val main = parseSuite(parentIndent = -1, headerLine = 1, ctx = Ctx(inLoop = false, inFunction = false, prefix = ""))
        val defs = functions.filterNotNull()
        val all = buildList {
            if (main.isNotEmpty()) add(FlowFunction("main", "main", main.first().line, main, isEntry = true))
            addAll(defs)
        }
        val names = defs.map { it.name.substringAfterLast('.') }.toSet()
        return FlowProgram(all.map { it.copy(body = markCalls(it.body, names)) })
    }

    // ── Blocks ──

    private fun parseSuite(parentIndent: Int, headerLine: Int, ctx: Ctx): List<FlowBlock> {
        val first = lines.getOrNull(pos)
        val blockIndent = if (parentIndent < 0) {
            if (first != null && first.indent != 0) fail(first.line, CodeErrorKind.UNEXPECTED_INDENT)
            0
        } else {
            if (first == null || first.indent <= parentIndent) fail(first?.line ?: headerLine, CodeErrorKind.EXPECTED_BLOCK)
            first.indent
        }
        val out = mutableListOf<FlowBlock>()
        while (pos < lines.size) {
            val l = lines[pos]
            if (l.indent < blockIndent) {
                if (l.indent > parentIndent) fail(l.line, CodeErrorKind.BAD_DEDENT)
                break
            }
            if (l.indent > blockIndent) fail(l.line, CodeErrorKind.UNEXPECTED_INDENT)
            out += statement(l, l.text, ctx)
        }
        return out
    }

    /** One statement starting at [l] (whose text may have had `async ` removed). Advances [pos]. */
    private fun statement(l: PyLine, text: String, ctx: Ctx): List<FlowBlock> {
        if (text.startsWith("@")) { pos++; return emptyList() }   // decorator
        return when (val word = PyText.firstWord(text)) {
            "if" -> listOf(ifStatement(l, text, ctx))
            "while" -> listOf(whileStatement(l, text, ctx))
            "for" -> listOf(forStatement(l, text, ctx))
            "def" -> { defStatement(l, text, ctx); emptyList() }
            "class" -> { classStatement(l, text, ctx); emptyList() }
            "try" -> tryStatement(l, text, ctx)
            "with" -> withStatement(l, text, ctx)
            "async" -> statement(l, text.removePrefix("async").trim(), ctx)
            "elif", "else", "except", "finally" -> fail(l.line, CodeErrorKind.MISPLACED, word)
            "match" -> if (isMatchStatement(l, text)) listOf(matchStatement(l, text, ctx)) else simpleLine(l, text, ctx)
            else -> simpleLine(l, text, ctx)
        }
    }

    private fun simpleLine(l: PyLine, text: String, ctx: Ctx): List<FlowBlock> {
        pos++
        return simpleStatements(text, l.line, ctx)
    }

    /** Header text before the colon and whatever follows it on the same line. */
    private fun header(l: PyLine, text: String): Pair<String, String> {
        val colon = PyText.headerColon(text)
        if (colon < 0) fail(l.line, CodeErrorKind.EXPECTED, ":")
        return text.substring(0, colon).trim() to text.substring(colon + 1).trim()
    }

    /** The block after a header: the rest of its line (`if x: y = 1`) or the indented lines below. */
    private fun suiteAfter(l: PyLine, rest: String, ctx: Ctx): List<FlowBlock> {
        pos++
        return if (rest.isNotEmpty()) simpleStatements(rest, l.line, ctx)
        else parseSuite(parentIndent = l.indent, headerLine = l.line, ctx = ctx)
    }

    /** The next line, if it continues the statement at [indent] with [word] (`elif`, `else`, …). */
    private fun continuation(indent: Int, vararg word: String): PyLine? =
        lines.getOrNull(pos)?.takeIf { it.indent == indent && PyText.firstWord(it.text) in word }

    private fun ifStatement(l: PyLine, text: String, ctx: Ctx): FlowBlock {
        val (head, rest) = header(l, text)
        val cond = head.substringAfter(PyText.firstWord(head)).trim()
        if (cond.isEmpty()) fail(l.line, CodeErrorKind.EXPECTED, "condition")
        val yes = suiteAfter(l, rest, ctx)
        val next = continuation(l.indent, "elif", "else")
        val no = when {
            next == null -> emptyList()
            PyText.firstWord(next.text) == "elif" -> listOf(ifStatement(next, next.text, ctx))
            else -> suiteAfter(next, header(next, next.text).second.also { checkBare(next, "else") }, ctx)
        }
        return FlowBlock.Branch(cond, yes, no, l.line)
    }

    private fun whileStatement(l: PyLine, text: String, ctx: Ctx): FlowBlock {
        val (head, rest) = header(l, text)
        val cond = head.removePrefix("while").trim()
        if (cond.isEmpty()) fail(l.line, CodeErrorKind.EXPECTED, "condition")
        val body = suiteAfter(l, rest, Ctx(inLoop = true, inFunction = ctx.inFunction, prefix = ctx.prefix))
        return FlowBlock.Loop(LoopKind.WHILE, cond, body, l.line, orElse = loopElse(l, ctx))
    }

    private fun forStatement(l: PyLine, text: String, ctx: Ctx): FlowBlock {
        val (head, rest) = header(l, text)
        val cond = head.removePrefix("for").trim()
        if (!PyText.hasTopLevelIn(cond)) fail(l.line, CodeErrorKind.EXPECTED, "in")
        val body = suiteAfter(l, rest, Ctx(inLoop = true, inFunction = ctx.inFunction, prefix = ctx.prefix))
        return FlowBlock.Loop(LoopKind.FOR_EACH, cond, body, l.line, orElse = loopElse(l, ctx))
    }

    private fun loopElse(l: PyLine, ctx: Ctx): List<FlowBlock> {
        val next = continuation(l.indent, "else") ?: return emptyList()
        checkBare(next, "else")
        return suiteAfter(next, header(next, next.text).second, ctx)
    }

    private fun defStatement(l: PyLine, text: String, ctx: Ctx) {
        val (head, rest) = header(l, text)
        val signature = head.removePrefix("def").trim()
        val name = PyText.firstWord(signature)
        if (name.isEmpty() || !signature.substring(name.length).trimStart().startsWith("(")) {
            fail(l.line, CodeErrorKind.EXPECTED, "name(")
        }
        val slot = functions.size
        functions += null   // keeps source order even though nested defs finish first
        val body = suiteAfter(l, rest, Ctx(inLoop = false, inFunction = true, prefix = ctx.prefix))
        functions[slot] = FlowFunction(ctx.prefix + name, ctx.prefix + signature, l.line, body, isEntry = false)
    }

    private fun classStatement(l: PyLine, text: String, ctx: Ctx) {
        val (head, rest) = header(l, text)
        val name = PyText.firstWord(head.removePrefix("class").trim())
        if (name.isEmpty()) fail(l.line, CodeErrorKind.EXPECTED, "name")
        // Only the methods are charted; class attributes are not steps of any flow.
        suiteAfter(l, rest, Ctx(inLoop = false, inFunction = false, prefix = "${ctx.prefix}$name."))
    }

    /**
     * `try` is drawn as the guarded steps, then one diamond per `except` (does this error happen?),
     * the `else` block when none does, and `finally` after them.
     */
    private fun tryStatement(l: PyLine, text: String, ctx: Ctx): List<FlowBlock> {
        val (_, rest) = header(l, text).also { checkBare(l, "try") }
        val body = suiteAfter(l, rest, ctx)
        val handlers = mutableListOf<Triple<String, List<FlowBlock>, Int>>()
        var orElse = emptyList<FlowBlock>()
        var finally = emptyList<FlowBlock>()
        var finallyLine = 0
        while (true) {
            val next = continuation(l.indent, "except", "else", "finally") ?: break
            val (head, nextRest) = header(next, next.text)
            when (PyText.firstWord(next.text)) {
                "except" -> handlers += Triple(head, suiteAfter(next, nextRest, ctx), next.line)
                "else" -> orElse = suiteAfter(next, nextRest, ctx)
                else -> { finallyLine = next.line; finally = suiteAfter(next, nextRest, ctx) }
            }
        }
        if (handlers.isEmpty() && finallyLine == 0) fail(l.line, CodeErrorKind.EXPECTED, "except")
        var chain: List<FlowBlock> = orElse
        for ((head, handler, line) in handlers.asReversed()) chain = listOf(FlowBlock.Branch(head, handler, chain, line))
        return buildList {
            add(FlowBlock.Step(StepKind.PROCESS, "try", l.line))
            addAll(body)
            addAll(chain)
            if (finallyLine != 0) { add(FlowBlock.Step(StepKind.PROCESS, "finally", finallyLine)); addAll(finally) }
        }
    }

    private fun withStatement(l: PyLine, text: String, ctx: Ctx): List<FlowBlock> {
        val (head, rest) = header(l, text)
        return listOf(FlowBlock.Step(classify(head), head, l.line)) + suiteAfter(l, rest, ctx)
    }

    /** `match` is a soft keyword: `match x:` followed by an indented `case` block. */
    private fun isMatchStatement(l: PyLine, text: String): Boolean {
        val colon = PyText.headerColon(text)
        if (colon < 0 || text.substring(colon + 1).isNotBlank()) return false
        val next = lines.getOrNull(pos + 1) ?: return false
        return next.indent > l.indent && PyText.firstWord(next.text) == "case"
    }

    private fun matchStatement(l: PyLine, text: String, ctx: Ctx): FlowBlock {
        val subject = header(l, text).first.removePrefix("match").trim()
        pos++
        val caseIndent = lines[pos].indent
        val cases = mutableListOf<SwitchCase>()
        while (pos < lines.size && lines[pos].indent == caseIndent) {
            val c = lines[pos]
            if (PyText.firstWord(c.text) != "case") fail(c.line, CodeErrorKind.EXPECTED, "case")
            val (head, rest) = header(c, c.text)
            val pattern = head.removePrefix("case").trim()
            val body = suiteAfter(c, rest, ctx)
            val test = if (pattern == "_") null else "$subject == $pattern"
            cases += SwitchCase(test, body, c.line)
        }
        lines.getOrNull(pos)?.let { if (it.indent > l.indent) fail(it.line, CodeErrorKind.BAD_DEDENT) }
        return FlowBlock.Switch(subject, cases, fallsThrough = false, line = l.line)
    }

    // ── Simple statements ──

    private fun simpleStatements(text: String, line: Int, ctx: Ctx): List<FlowBlock> =
        PyText.splitTopLevel(text, ';').mapNotNull { simple(it, line, ctx) }

    private fun simple(text: String, line: Int, ctx: Ctx): FlowBlock? = when (val word = PyText.firstWord(text)) {
        "pass" -> null
        "break" -> if (ctx.inLoop) FlowBlock.Jump(JumpKind.BREAK, text, line) else fail(line, CodeErrorKind.MISPLACED, word)
        "continue" -> if (ctx.inLoop) FlowBlock.Jump(JumpKind.CONTINUE, text, line) else fail(line, CodeErrorKind.MISPLACED, word)
        "return" -> if (ctx.inFunction) FlowBlock.Jump(JumpKind.RETURN, text, line) else fail(line, CodeErrorKind.MISPLACED, word)
        "raise" -> FlowBlock.Jump(JumpKind.RAISE, text, line)
        in COMPOUND -> fail(line, CodeErrorKind.MISPLACED, word)   // a compound statement after `x:` on one line
        else -> if (PyText.isStringOnly(text)) null else FlowBlock.Step(classify(text), text, line)
    }

    private fun classify(text: String): StepKind =
        if (CodeScan.calledNames(text).any { it in IO_NAMES }) StepKind.IO else StepKind.PROCESS

    /** `else:` / `try:` must have nothing between the keyword and the colon. */
    private fun checkBare(l: PyLine, word: String) {
        if (PyText.headerColon(l.text).let { it < 0 || l.text.substring(0, it).trim() != word }) {
            fail(l.line, CodeErrorKind.EXPECTED, ":")
        }
    }

    private fun fail(line: Int, kind: CodeErrorKind, detail: String = ""): Nothing =
        throw CodeParseException(CodeError(line, kind, detail))

    private companion object {
        val IO_NAMES = setOf("input", "print")
        val COMPOUND = setOf("if", "elif", "else", "while", "for", "def", "class", "try", "except", "finally", "with")
    }
}
