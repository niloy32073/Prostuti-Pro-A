package com.prostuti.student.domain.code.c

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
 * Reads C source into flowchart blocks: one chart per function definition, `main` first. Code with
 * no function at all (a snippet such as `for (…) { … }`) is read as the body of `main`.
 * Declarations, prototypes, structs and preprocessor lines outside functions are not charted.
 * A `goto` and its label are drawn as a pair of connector circles with the label's name.
 * Throws [CodeParseException] at the first problem.
 */
class CFlowParser {

    private class Ctx(val inLoop: Boolean, val inSwitch: Boolean) {
        fun loop() = Ctx(inLoop = true, inSwitch = inSwitch)
        fun switch() = Ctx(inLoop = inLoop, inSwitch = true)
    }

    private var src = ""
    private var toks: List<CToken> = emptyList()
    private var p = 0

    fun parse(source: String): FlowProgram {
        src = source.replace("\r\n", "\n").replace('\r', '\n')
        toks = CLexer(src).tokens()
        p = 0
        if (toks.isEmpty()) return FlowProgram(emptyList())
        val functions = if (hasFunctionDefinition()) topLevel() else {
            val body = statementList(untilBrace = false, openLine = 0, ctx = Ctx(inLoop = false, inSwitch = false))
            listOf(FlowFunction("main", "main", toks.first().line, body, isEntry = true))
        }
        val names = functions.map { it.name }.toSet()
        val ordered = functions.filter { it.isEntry } + functions.filterNot { it.isEntry }
        return FlowProgram(ordered.map { it.copy(body = markCalls(it.body, names)) })
    }

    // ── Top level ──

    /** A `{` at brace depth 0 right after `name( … )` (not `for (…)`, `while (…)`…) starts a function body. */
    private fun hasFunctionDefinition(): Boolean {
        var depth = 0
        var lineStart = 0
        for (k in toks.indices) {
            val t = toks[k]
            when {
                t.isPunct("{") -> {
                    if (depth == 0 && functionNameIndex(toks.subList(lineStart, k)) >= 0) return true
                    depth++
                }
                t.isPunct("}") -> { depth--; if (depth == 0) lineStart = k + 1 }
                t.isPunct(";") && depth == 0 -> lineStart = k + 1
            }
        }
        return false
    }

    private fun topLevel(): List<FlowFunction> {
        val out = mutableListOf<FlowFunction>()
        while (p < toks.size) {
            if (toks[p].isPunct(";")) { p++; continue }
            val start = p
            var depth = 0
            while (p < toks.size) {
                val t = toks[p]
                when {
                    t.isPunct("(") || t.isPunct("[") -> depth++
                    t.isPunct(")") || t.isPunct("]") -> { depth--; if (depth < 0) fail(t.line, CodeErrorKind.UNEXPECTED_CLOSE, t.text) }
                    depth == 0 && (t.isPunct("{") || t.isPunct(";")) -> break
                    t.isPunct("}") -> fail(t.line, CodeErrorKind.UNEXPECTED_CLOSE, "}")
                }
                p++
            }
            if (p >= toks.size) {
                if (depth > 0) fail(toks[start].line, CodeErrorKind.UNCLOSED_BRACKET, "(")
                fail(toks.last().line, CodeErrorKind.MISSING_SEMICOLON)
            }
            if (toks[p].isPunct(";")) { p++; continue }                 // declaration or prototype
            val head = toks.subList(start, p)
            val nameAt = functionNameIndex(head)
            val open = toks[p]
            p++
            if (nameAt < 0) { skipBraces(open); continue }               // struct / enum / initializer
            val body = statementList(untilBrace = true, openLine = open.line, ctx = Ctx(inLoop = false, inSwitch = false))
            val name = head[nameAt].text
            out += FlowFunction(name, join(head.first(), head.last()), head.first().line, body, isEntry = name == "main")
        }
        return out
    }

    /** Index in [head] of the function name: the word before the last `( … )` group. -1 if not a function. */
    private fun functionNameIndex(head: List<CToken>): Int {
        if (head.isEmpty() || !head.last().isPunct(")")) return -1
        if (head.any { it.isPunct("=") }) return -1
        var depth = 0
        for (k in head.indices.reversed()) {
            val t = head[k]
            if (t.isPunct(")")) depth++
            if (t.isPunct("(")) { depth--; if (depth == 0) return if (k > 0 && head[k - 1].kind == CTokenKind.IDENT && head[k - 1].text !in KEYWORDS) k - 1 else -1 }
        }
        return -1
    }

    /** Skips to the `}` matching [open] (already consumed). */
    private fun skipBraces(open: CToken) {
        var depth = 1
        while (p < toks.size && depth > 0) {
            if (toks[p].isPunct("{")) depth++
            if (toks[p].isPunct("}")) depth--
            p++
        }
        if (depth > 0) fail(open.line, CodeErrorKind.UNCLOSED_BRACKET, "{")
    }

    // ── Statements ──

    /** Statements up to the `}` that closes a block (consumed), or to the end of the source. */
    private fun statementList(untilBrace: Boolean, openLine: Int, ctx: Ctx): List<FlowBlock> {
        val out = mutableListOf<FlowBlock>()
        while (true) {
            if (p >= toks.size) {
                if (untilBrace) fail(openLine, CodeErrorKind.UNCLOSED_BRACKET, "{")
                return out
            }
            if (untilBrace && toks[p].isPunct("}")) { p++; return out }
            out += statement(ctx)
        }
    }

    private fun statement(ctx: Ctx): List<FlowBlock> {
        val t = toks[p]
        if (t.kind == CTokenKind.PUNCT) when (t.text) {
            "{" -> { p++; return statementList(untilBrace = true, openLine = t.line, ctx = ctx) }
            ";" -> { p++; return emptyList() }
            "}" -> fail(t.line, CodeErrorKind.UNEXPECTED_CLOSE, "}")
        }
        if (t.kind == CTokenKind.IDENT) when (t.text) {
            "if" -> return listOf(ifStatement(ctx))
            "else" -> fail(t.line, CodeErrorKind.MISPLACED, "else")
            "while" -> {
                p++
                val cond = parenText()
                return listOf(FlowBlock.Loop(LoopKind.WHILE, cond, body(ctx.loop()), t.line))
            }
            "do" -> return listOf(doWhile(ctx))
            "for" -> return listOf(forStatement(ctx))
            "switch" -> return listOf(switchStatement(ctx))
            "case", "default" -> fail(t.line, CodeErrorKind.MISPLACED, t.text)
            "break" -> {
                if (!ctx.inLoop && !ctx.inSwitch) fail(t.line, CodeErrorKind.MISPLACED, "break")
                return listOf(FlowBlock.Jump(JumpKind.BREAK, simpleText(), t.line))
            }
            "continue" -> {
                if (!ctx.inLoop) fail(t.line, CodeErrorKind.MISPLACED, "continue")
                return listOf(FlowBlock.Jump(JumpKind.CONTINUE, simpleText(), t.line))
            }
            "return" -> return listOf(FlowBlock.Jump(JumpKind.RETURN, simpleText(), t.line))
            "goto" -> {
                p++
                val target = toks.getOrNull(p)?.takeIf { it.kind == CTokenKind.IDENT } ?: fail(t.line, CodeErrorKind.EXPECTED, "label")
                p++
                expectSemicolon()
                return listOf(FlowBlock.Connector(target.text, isGoto = true, line = t.line))
            }
        }
        if (isLabel()) {
            // `label:` before a statement: a connector circle, then the statement.
            p += 2
            val here = FlowBlock.Connector(t.text, isGoto = false, line = t.line)
            return listOf(here) + if (p < toks.size && !toks[p].isPunct("}")) statement(ctx) else emptyList()
        }
        val text = simpleText()
        return listOf(FlowBlock.Step(classify(text), text, t.line))
    }

    /** `name:` (not `default:`, not `a ? b : c`). */
    private fun isLabel(): Boolean {
        val t = toks[p]
        val next = toks.getOrNull(p + 1) ?: return false
        return t.kind == CTokenKind.IDENT && t.text !in KEYWORDS && next.isPunct(":")
    }

    /** A statement used as a body (`if (x) y;` or `if (x) { … }`). */
    private fun body(ctx: Ctx): List<FlowBlock> {
        if (p >= toks.size) fail(toks.last().line, CodeErrorKind.EXPECTED, "statement")
        return statement(ctx)
    }

    private fun ifStatement(ctx: Ctx): FlowBlock {
        val line = toks[p].line
        p++
        val cond = parenText()
        val yes = body(ctx)
        val no = if (p < toks.size && toks[p].isWord("else")) { p++; body(ctx) } else emptyList()
        return FlowBlock.Branch(cond, yes, no, line)
    }

    private fun doWhile(ctx: Ctx): FlowBlock {
        val line = toks[p].line
        p++
        val loopBody = body(ctx.loop())
        if (p >= toks.size || !toks[p].isWord("while")) fail(toks[(p - 1).coerceAtLeast(0)].line, CodeErrorKind.EXPECTED, "while")
        p++
        val cond = parenText()
        expectSemicolon()
        return FlowBlock.Loop(LoopKind.DO_WHILE, cond, loopBody, line)
    }

    private fun forStatement(ctx: Ctx): FlowBlock {
        val line = toks[p].line
        p++
        val open = expect("(")
        val parts = mutableListOf(mutableListOf<CToken>())
        var depth = 0
        while (true) {
            if (p >= toks.size) fail(open.line, CodeErrorKind.UNCLOSED_BRACKET, "(")
            val t = toks[p]
            if (t.isPunct("(") || t.isPunct("[") || t.isPunct("{")) depth++
            if (t.isPunct(")") || t.isPunct("]") || t.isPunct("}")) { if (depth == 0) break; depth-- }
            if (t.isPunct(";") && depth == 0) parts += mutableListOf<CToken>() else parts.last() += t
            p++
        }
        p++   // ')'
        if (parts.size != 3) fail(line, CodeErrorKind.EXPECTED, ";")
        val (init, cond, update) = parts.map { if (it.isEmpty()) "" else join(it.first(), it.last()) }
        return FlowBlock.Loop(
            kind = LoopKind.WHILE,
            condition = cond.ifEmpty { "true" },
            body = body(ctx.loop()),
            line = line,
            init = init.takeIf { it.isNotEmpty() }?.let { FlowBlock.Step(classify(it), it, line) },
            update = update.takeIf { it.isNotEmpty() }?.let { FlowBlock.Step(classify(it), it, line) },
        )
    }

    /**
     * `switch` body: statements split by `case`/`default` labels. Labels with nothing between them
     * share one diamond (`x == 1 || x == 2`). Statements before the first label never run and are dropped.
     */
    private fun switchStatement(ctx: Ctx): FlowBlock {
        val line = toks[p].line
        p++
        val subject = parenText()
        val open = expect("{")
        class Draft(val line: Int) { val values = mutableListOf<String>(); var isDefault = false; val body = mutableListOf<FlowBlock>() }
        val drafts = mutableListOf<Draft>()
        val inner = ctx.switch()
        while (true) {
            if (p >= toks.size) fail(open.line, CodeErrorKind.UNCLOSED_BRACKET, "{")
            val t = toks[p]
            when {
                t.isPunct("}") -> { p++; break }
                t.isWord("case") || t.isWord("default") -> {
                    p++
                    val value = if (t.isWord("case")) caseValue(t) else { expect(":"); null }
                    val current = drafts.lastOrNull()?.takeIf { it.body.isEmpty() } ?: Draft(t.line).also { drafts += it }
                    if (value == null) current.isDefault = true else current.values += value
                }
                else -> {
                    val blocks = statement(inner)
                    drafts.lastOrNull()?.body?.addAll(blocks)
                }
            }
        }
        val cases = drafts.map { d ->
            SwitchCase(if (d.isDefault) null else d.values.joinToString(" || ") { "$subject == $it" }, d.body, d.line)
        }
        return FlowBlock.Switch(subject, cases, fallsThrough = true, line = line)
    }

    /** The value of `case 3:` up to its `:` (a `?:` inside it is skipped). */
    private fun caseValue(caseTok: CToken): String {
        val start = p
        var ternary = 0
        while (p < toks.size) {
            val t = toks[p]
            if (t.isPunct("?")) ternary++
            if (t.isPunct(":")) { if (ternary == 0) break; ternary-- }
            if (t.isPunct(";") || t.isPunct("{") || t.isPunct("}")) break
            p++
        }
        if (p >= toks.size || !toks[p].isPunct(":") || p == start) fail(caseTok.line, CodeErrorKind.EXPECTED, ":")
        val text = join(toks[start], toks[p - 1])
        p++
        return text
    }

    // ── Pieces ──

    /** `( … )` → the text inside. */
    private fun parenText(): String {
        val open = expect("(")
        val start = p
        var depth = 0
        while (true) {
            if (p >= toks.size) fail(open.line, CodeErrorKind.UNCLOSED_BRACKET, "(")
            val t = toks[p]
            if (t.isPunct("(")) depth++
            if (t.isPunct(")")) { if (depth == 0) break; depth-- }
            p++
        }
        if (p == start) fail(open.line, CodeErrorKind.EXPECTED, "condition")
        val text = join(toks[start], toks[p - 1])
        p++
        return text
    }

    /**
     * Tokens up to the `;` that ends a simple statement (consumed). A missing `;` is reported when the
     * statement runs into a `}`, the end, a statement keyword, or a new line that clearly starts a new statement.
     */
    private fun simpleText(): String {
        val start = p
        var depth = 0
        while (true) {
            if (p >= toks.size) fail(toks.last().line, CodeErrorKind.MISSING_SEMICOLON)
            val t = toks[p]
            if (depth == 0) {
                if (t.isPunct(";")) break
                if (t.isPunct("}")) fail(toks[(p - 1).coerceAtLeast(start)].line, CodeErrorKind.MISSING_SEMICOLON)
                if (p > start && looksLikeNewStatement(toks[p - 1], t)) fail(toks[p - 1].line, CodeErrorKind.MISSING_SEMICOLON)
            }
            when {
                t.isPunct("(") || t.isPunct("[") || t.isPunct("{") -> depth++
                t.isPunct(")") || t.isPunct("]") || t.isPunct("}") -> {
                    depth--
                    if (depth < 0) fail(t.line, CodeErrorKind.UNEXPECTED_CLOSE, t.text)
                }
            }
            p++
        }
        val text = if (p == start) "" else join(toks[start], toks[p - 1])
        p++
        return text
    }

    private fun looksLikeNewStatement(prev: CToken, next: CToken): Boolean {
        if (next.kind == CTokenKind.IDENT && next.text in STATEMENT_KEYWORDS) return true
        if (next.line == prev.line || next.kind != CTokenKind.IDENT) return false
        val ends = prev.kind in VALUE_KINDS || prev.text == ")" || prev.text == "]" || prev.text == "++" || prev.text == "--" ||
            (prev.kind == CTokenKind.IDENT && prev.text !in TYPE_WORDS && prev.text !in KEYWORDS)
        return ends
    }

    private fun expect(punct: String): CToken {
        val t = toks.getOrNull(p)
        if (t == null || !t.isPunct(punct)) fail((t ?: toks.last()).line, CodeErrorKind.EXPECTED, punct)
        p++
        return t
    }

    private fun expectSemicolon() {
        val t = toks.getOrNull(p)
        if (t == null || !t.isPunct(";")) fail(toks[p - 1].line, CodeErrorKind.MISSING_SEMICOLON)
        p++
    }

    /** Source text from [first] to [last], comments and line breaks squeezed to single spaces. */
    private fun join(first: CToken, last: CToken): String {
        val a = toks.indexOf(first)
        val b = toks.indexOf(last)
        val sb = StringBuilder(first.text)
        for (k in a + 1..b) {
            val gap = src.substring(toks[k - 1].end, toks[k].start)
            if (gap.isNotEmpty()) sb.append(if (gap.all { it == ' ' || it == '\t' }) gap.replace('\t', ' ') else " ")
            sb.append(toks[k].text)
        }
        return sb.toString()
    }

    private fun classify(text: String): StepKind {
        val calls = CodeScan.calledNames(text)
        val io = calls.any { it in IO_NAMES } || CodeScan.words(text).any { it == "cin" || it == "cout" }
        return if (io) StepKind.IO else StepKind.PROCESS
    }

    private fun fail(line: Int, kind: CodeErrorKind, detail: String = ""): Nothing =
        throw CodeParseException(CodeError(line, kind, detail))

    private companion object {
        val IO_NAMES = setOf(
            "printf", "scanf", "puts", "gets", "fgets", "getchar", "putchar", "fputs", "fprintf", "fscanf",
            "getch", "getche", "scanf_s", "gets_s",
        )
        val STATEMENT_KEYWORDS = setOf("if", "else", "for", "while", "do", "switch", "case", "default", "break", "continue", "return", "goto")
        val KEYWORDS = STATEMENT_KEYWORDS + setOf("sizeof", "int", "char", "float", "double", "void", "long", "short", "unsigned", "signed")
        val TYPE_WORDS = setOf(
            "int", "char", "float", "double", "void", "long", "short", "unsigned", "signed", "const", "static",
            "struct", "enum", "union", "volatile", "register", "extern", "auto", "inline", "bool", "_Bool",
        )
        val VALUE_KINDS = setOf(CTokenKind.NUMBER, CTokenKind.STRING, CTokenKind.CHAR)
    }
}
