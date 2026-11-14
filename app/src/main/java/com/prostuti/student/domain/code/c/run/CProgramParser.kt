package com.prostuti.student.domain.code.c.run

import com.prostuti.student.domain.code.c.CToken
import com.prostuti.student.domain.code.c.CTokenKind
import java.math.BigInteger

/**
 * Reads preprocessed C tokens into a typed, checked [CProgram], the way a compiler's front end
 * would: undeclared names, wrong argument counts and invalid operand types are reported here,
 * before anything runs. Every implicit conversion becomes an explicit [CExpr.Cast].
 *
 * Supported: the arithmetic types, pointers, arrays (the first size may be a variable), struct,
 * union, enum, typedef, function pointers, functions, globals, `static` locals, all C operators and
 * statements. `goto` may jump to a label in its own `{ }` block or an enclosing one (not into a
 * nested block or loop). Not supported: bit-fields, variadic functions of the program's own.
 */
internal class CProgramParser(private val toks: List<CToken>) {

    private class Spec(val base: CType, val isStatic: Boolean, val isTypedef: Boolean)

    /** The names declared in one block: variables, typedef names, enum constants and struct tags. */
    private class Scope {
        val vars = HashMap<String, VarSym>()
        val typedefs = HashMap<String, CType>()
        val enums = HashMap<String, Long>()
        val tags = HashMap<String, StructDef>()
        val enumTags = HashSet<String>()

        fun hasOrdinary(name: String) = name in vars || name in typedefs || name in enums
    }

    private class GotoUse(val label: String, val line: Int, val lists: List<Int>)

    private class Declarator(
        val name: String?,
        val type: CType,
        val line: Int,
        val sizeExpr: CExpr?,
        val params: List<Pair<String?, CType>>?,
        val paramsKnown: Boolean,
    )

    private var p = 0
    private val functions = LinkedHashMap<String, FuncSym>()
    private val globals = HashMap<String, VarSym>()
    private val globalInits = mutableListOf<CStmt.Decl>()
    private var globalCount = 0
    private val bodies = mutableListOf<Pair<FuncSym, Int>>()

    private var fn: FuncSym? = null
    private var localCount = 0
    private val scopes = ArrayDeque<Scope>()
    /** Typedefs, enum constants and tags declared outside functions (global variables are in [globals]). */
    private val fileScope = Scope()

    // goto: every statement list ({ } block, switch body) gets an id; a label belongs to the list it
    // is a direct item of, and a goto may only reach lists that enclose it.
    private var nextListId = 0
    private var currentList = -1
    private val listChain = ArrayList<Int>()
    private val labels = HashMap<String, Int>()
    private val gotos = mutableListOf<GotoUse>()
    private var loopDepth = 0
    private var switchDepth = 0

    fun parse(): CProgram {
        if (toks.isEmpty()) fail(1, "the program is empty")
        if (hasFunctionDefinition()) {
            topLevel()
            for ((f, start) in bodies) {
                p = start
                functionBody(f)
            }
        } else {
            // A snippet with no function: its statements are the body of main.
            val main = FuncSym("main", CType.INT, emptyList(), checkArgs = true, line = toks.first().line)
            functions["main"] = main
            enterFunction(main)
            val stmts = mutableListOf<CStmt>()
            val id = nextListId++
            listChain += id
            while (p < toks.size) {
                currentList = id
                stmts += statement()
            }
            leaveFunction(main, CStmt.Block(main.line, stmts))
        }
        val main = functions["main"] ?: fail(toks.last().line, "the program has no main() function")
        if (main.body == null) fail(main.line, "main() is declared but has no body")
        return CProgram(main, globalCount, globalInits, globals.values.sortedBy { it.index })
    }

    // ── Top level ──

    private fun hasFunctionDefinition(): Boolean {
        var depth = 0
        var parens = 0
        for (k in toks.indices) {
            val t = toks[k]
            when {
                t.isPunct("(") -> parens++
                t.isPunct(")") -> parens--
                t.isPunct("{") -> {
                    if (depth == 0 && parens == 0 && k > 0 && toks[k - 1].isPunct(")")) {
                        // `name ( … ) {` but not `if/for/while/switch ( … ) {`.
                        var d = 0
                        var j = k - 1
                        while (j >= 0) {
                            if (toks[j].isPunct(")")) d++
                            if (toks[j].isPunct("(")) { d--; if (d == 0) break }
                            j--
                        }
                        val before = toks.getOrNull(j - 1)
                        if (before != null && before.kind == CTokenKind.IDENT && before.text !in STATEMENT_WORDS) return true
                    }
                    depth++
                }
                t.isPunct("}") -> depth--
            }
        }
        return false
    }

    private fun topLevel() {
        while (p < toks.size) {
            if (peekPunct(";")) { p++; continue }
            val line = tok().line
            val spec = specifiers() ?: fail(line, "expected a declaration or a function, found '${tok().text}' (statements must be inside a function)")
            if (peekPunct(";")) { p++; continue }
            while (true) {
                val d = declarator(spec.base, allowFunc = true, allowVla = false, nameOptional = false)
                val name = d.name!!
                if (spec.isTypedef) {
                    defineTypedef(fileScope, name, declType(d), d.line)
                } else if (d.params != null) {
                    if (d.type.isIncomplete) fail(d.line, "${d.type} is not complete (its members are not defined)")
                    val params = d.params.mapIndexed { i, (pn, pt) -> VarSym(pn ?: "#$i", pt, d.line, isGlobal = false, index = i) }
                    val sym = FuncSym(name, d.type, params, d.paramsKnown, d.line)
                    val existing = functions[name]
                    if (existing?.body != null && peekPunct("{")) fail(d.line, "the function '$name' is defined twice")
                    if (existing == null || peekPunct("{")) functions[name] = sym
                    if (peekPunct("{")) {
                        if (d.params.any { it.first == null }) fail(d.line, "every parameter of '$name' needs a name")
                        bodies += sym to p
                        skipBraces()
                        break
                    }
                } else {
                    if (name in globals || fileScope.hasOrdinary(name)) fail(d.line, "'$name' is already declared")
                    if (d.type is CType.Func) fail(d.line, "'$name' is a function type; declare a pointer instead (e.g. *$name)")
                    if (d.type == CType.Void) fail(d.line, "a variable cannot be void")
                    val sym = VarSym(name, d.type, d.line, isGlobal = true, index = globalCount++)
                    globals[name] = sym
                    val (init, used) = if (peekPunct("=")) { p++; initializer(sym.type) } else null to 0
                    val finalSym = withInferredSize(sym, init, used)
                    if (finalSym !== sym) globals[name] = finalSym
                    checkKnownSize(finalSym)
                    globalInits += CStmt.Decl(d.line, finalSym, init, null)
                }
                if (peekPunct(",")) { p++; continue }
                expectPunct(";", "missing ; after the declaration")
                break
            }
        }
    }

    private fun skipBraces() {
        val open = tok()
        var depth = 0
        while (p < toks.size) {
            if (toks[p].isPunct("{")) depth++
            if (toks[p].isPunct("}")) { depth--; if (depth == 0) { p++; return } }
            p++
        }
        fail(open.line, "'{' is never closed")
    }

    private fun functionBody(f: FuncSym) {
        enterFunction(f)
        if (f.ret.isIncomplete) fail(f.line, "${f.ret} is not complete (its members are not defined)")
        f.params.forEach { if (it.type.isIncomplete) fail(it.line, "${it.type} is not complete (its members are not defined)") }
        val open = tok()
        p++
        val stmts = blockItems(open.line)
        leaveFunction(f, CStmt.Block(open.line, stmts))
    }

    private fun enterFunction(f: FuncSym) {
        fn = f
        localCount = f.params.size
        scopes.clear()
        scopes.addLast(Scope().apply { f.params.forEach { vars[it.name] = it } })
        labels.clear()
        gotos.clear()
        listChain.clear()
    }

    private fun leaveFunction(f: FuncSym, body: CStmt.Block) {
        for (g in gotos) {
            val list = labels[g.label] ?: fail(g.line, "the label '${g.label}' is not defined in ${f.name}()")
            if (list !in g.lists) fail(g.line, "goto cannot jump into a block, loop or if from outside it (the label '${g.label}' is inside one)")
        }
        f.body = body
        f.localCount = localCount
        fn = null
    }

    // ── Types and declarators ──

    /** A variable, typedef name or enum constant visible here (innermost first); null if none. */
    private fun resolve(name: String): Any? {
        for (scope in scopes.reversed()) {
            scope.vars[name]?.let { return it }
            scope.typedefs[name]?.let { return it }
            scope.enums[name]?.let { return it }
        }
        globals[name]?.let { return it }
        fileScope.typedefs[name]?.let { return it }
        fileScope.enums[name]?.let { return it }
        return null
    }

    private fun currentScope(): Scope = scopes.lastOrNull() ?: fileScope

    private fun isTypedefName(name: String) = resolve(name) is CType

    private fun isTypeStart(t: CToken?) =
        t != null && t.kind == CTokenKind.IDENT && (t.text in SPEC_WORDS || isTypedefName(t.text))

    private fun defineTypedef(scope: Scope, name: String, type: CType, line: Int) {
        val existing = scope.typedefs[name]
        if (existing != null && existing == type) return          // C11 allows repeating the same typedef
        if (scope.hasOrdinary(name) || (scope === fileScope && name in globals)) fail(line, "'$name' is already declared")
        scope.typedefs[name] = type
        if (type is CType.Struct && type.def.tag == null && type.def.alias == null) type.def.alias = name
    }

    private fun specifiers(): Spec? {
        if (!isTypeStart(toks.getOrNull(p))) return null
        val line = tok().line
        var unsigned = false
        var signed = false
        var short = false
        var longs = 0
        var base: String? = null
        var named: CType? = null      // struct / union / enum / typedef name
        var isStatic = false
        var isTypedef = false
        while (isTypeStart(toks.getOrNull(p))) {
            val w = tok().text
            val anySpecifier = base != null || named != null || signed || unsigned || short || longs > 0
            if (w !in SPEC_WORDS) {
                // A typedef name is a type only before any other type word: `typedef int T; long T;` is not.
                if (anySpecifier) break
                named = resolve(w) as CType
                p++
                continue
            }
            when (w) {
                "struct", "union" -> {
                    if (anySpecifier) fail(line, "'$w' cannot be combined with another type")
                    named = structSpecifier(w == "union")
                    continue
                }
                "enum" -> {
                    if (anySpecifier) fail(line, "'enum' cannot be combined with another type")
                    named = enumSpecifier()
                    continue
                }
                "typedef" -> isTypedef = true
                "static" -> isStatic = true
                "const", "volatile", "extern", "register", "auto", "inline" -> Unit
                "signed" -> signed = true
                "unsigned" -> unsigned = true
                "short" -> short = true
                "long" -> longs++
                else -> {
                    if (named != null) fail(line, "'$named' and '$w' cannot be combined")
                    if (base != null && !(base == "int" || w == "int")) fail(line, "'$base' and '$w' cannot be combined")
                    if (base == null || base == "int") base = w
                }
            }
            if (named != null && w in NUMERIC_WORDS) fail(line, "'$named' and '$w' cannot be combined")
            p++
        }
        if (signed && unsigned) fail(line, "a type cannot be both signed and unsigned")
        if (isTypedef && isStatic) fail(line, "a typedef cannot be static")
        named?.let { return Spec(it, isStatic, isTypedef) }
        val type: CType = when (base) {
            "void" -> CType.Void
            "_Bool", "bool" -> CType.Num(NumKind.BOOL)
            "char" -> CType.Num(NumKind.CHAR, unsigned)
            "float" -> CType.FLOAT
            "double" -> if (longs > 0) CType.Num(NumKind.LDOUBLE) else CType.DOUBLE
            "size_t" -> CType.ULONG
            else -> when {
                short -> CType.Num(NumKind.SHORT, unsigned)
                longs == 1 -> CType.Num(NumKind.LONG, unsigned)
                longs >= 2 -> CType.Num(NumKind.LLONG, unsigned)
                else -> CType.Num(NumKind.INT, unsigned)
            }
        }
        return Spec(type, isStatic, isTypedef)
    }

    /** `struct Tag`, `struct Tag { … }` or `struct { … }` (and the same for union). */
    private fun structSpecifier(isUnion: Boolean): CType {
        val kw = tok()
        val word = kw.text
        p++
        val tag = if (p < toks.size && tok().kind == CTokenKind.IDENT && tok().text !in KEYWORDS) tok().text.also { p++ } else null
        if (!peekPunct("{")) {
            if (tag == null) fail(kw.line, "'$word' needs a name or a { … } list of members")
            val def = findTag(tag) ?: StructDef(tag, isUnion).also { currentScope().tags[tag] = it }
            if (def.isUnion != isUnion) fail(kw.line, "'$tag' is a ${if (def.isUnion) "union" else "struct"}, not a $word")
            return CType.Struct(def)
        }
        val scope = currentScope()
        val def = if (tag == null) StructDef(null, isUnion) else {
            val existing = scope.tags[tag]
            when {
                existing == null -> StructDef(tag, isUnion).also { scope.tags[tag] = it }
                existing.complete -> fail(kw.line, "$word $tag is defined twice")
                existing.isUnion != isUnion -> fail(kw.line, "'$tag' is a ${if (existing.isUnion) "union" else "struct"}, not a $word")
                else -> existing
            }
        }
        val open = tok()
        p++
        val members = mutableListOf<Pair<String, CType>>()
        while (true) {
            if (p >= toks.size) fail(open.line, "'{' is never closed")
            if (peekPunct("}")) { p++; break }
            val mLine = tok().line
            val spec = specifiers() ?: fail(mLine, "expected a member type, found '${tok().text}'")
            if (spec.isTypedef || spec.isStatic) fail(mLine, "a struct member cannot be ${if (spec.isTypedef) "a typedef" else "static"}")
            if (peekPunct(";")) fail(mLine, "this member needs a name")
            while (true) {
                val d = declarator(spec.base, allowFunc = false, allowVla = false, nameOptional = false)
                val t = d.type
                when {
                    t == CType.Void -> fail(d.line, "a member cannot be void")
                    t is CType.Struct && t.def === def -> fail(d.line, "a struct cannot contain itself; use a pointer (${def} *${d.name})")
                    t.isIncomplete || (t is CType.Arr && t.baseScalar.isIncomplete) -> fail(d.line, "$t is not complete (its members are not defined)")
                    t is CType.Arr && t.size == null -> fail(d.line, "the size of the array member '${d.name}' is missing")
                }
                if (members.any { it.first == d.name }) fail(d.line, "the member '${d.name}' appears twice")
                if (peekPunct(":")) fail(d.line, "bit-fields (member : width) are not supported")
                members += d.name!! to t
                if (peekPunct(",")) { p++; continue }
                expectPunct(";", "missing ; after the member '${d.name}'")
                break
            }
        }
        if (members.isEmpty()) fail(open.line, "a $word needs at least one member")
        def.complete(members)
        if (def.slots > MAX_STATIC_ELEMENTS) fail(open.line, "this $word is too big")
        return CType.Struct(def)
    }

    private fun findTag(tag: String): StructDef? {
        for (scope in scopes.reversed()) scope.tags[tag]?.let { return it }
        return fileScope.tags[tag]
    }

    /** `enum Tag`, `enum Tag { A, B = 5 }` or `enum { … }`; an enum is an `int`. */
    private fun enumSpecifier(): CType {
        val kw = tok()
        p++
        val tag = if (p < toks.size && tok().kind == CTokenKind.IDENT && tok().text !in KEYWORDS) tok().text.also { p++ } else null
        val scope = currentScope()
        if (!peekPunct("{")) {
            if (tag == null) fail(kw.line, "'enum' needs a name or a { … } list of values")
            val known = scopes.any { tag in it.enumTags } || tag in fileScope.enumTags
            if (!known) fail(kw.line, "enum $tag is not defined")
            return CType.INT
        }
        if (tag != null) {
            if (tag in scope.enumTags) fail(kw.line, "enum $tag is defined twice")
            scope.enumTags += tag
        }
        val open = tok()
        p++
        var next = 0L
        while (true) {
            if (p >= toks.size) fail(open.line, "'{' is never closed")
            if (peekPunct("}")) break
            val t = tok()
            if (t.kind != CTokenKind.IDENT || t.text in KEYWORDS) fail(t.line, "expected a name in the enum, found '${t.text}'")
            p++
            if (peekPunct("=")) {
                p++
                val e = conditional()
                if (!e.type.isInteger) fail(t.line, "an enum value must be a whole number")
                next = constValue(e) ?: fail(t.line, "an enum value must be a constant")
            }
            if (scope.hasOrdinary(t.text) || (scope === fileScope && t.text in globals)) fail(t.line, "'${t.text}' is already declared")
            scope.enums[t.text] = CProgramParser.wrap(next, CType.INT)
            next++
            if (peekPunct(",")) p++ else break
        }
        expectPunct("}", "missing '}' at the end of the enum")
        return CType.INT
    }

    /** The declared thing's full type: a function declarator becomes a [CType.Func]. */
    private fun declType(d: Declarator): CType =
        if (d.params != null) CType.Func(d.type, d.params.map { it.second }, d.paramsKnown) else d.type

    /**
     * A declarator: pointers, a name (optional for type names), array sizes or a parameter list,
     * and parentheses for function pointers and pointers to arrays (`int (*op)(int, int)`,
     * `int (*row)[3]`). [Declarator.params] is set only when it declares a function itself.
     */
    private fun declarator(base: CType, allowFunc: Boolean, allowVla: Boolean, nameOptional: Boolean): Declarator {
        val line = tok().line
        var type = base
        while (peekPunct("*")) {
            p++
            while (peekWord("const") || peekWord("volatile")) p++
            type = CType.Ptr(type)
        }
        if (peekPunct("(") && isNestedDeclarator()) {
            // `( inner ) suffixes`: the suffixes apply first, then the inner declarator wraps them.
            val open = tok()
            p++
            val innerStart = p
            var depth = 1
            while (depth > 0) {
                if (p >= toks.size) fail(open.line, "'(' is never closed")
                if (toks[p].isPunct("(")) depth++
                if (toks[p].isPunct(")")) depth--
                p++
            }
            val outer = suffixes(type, line)
            val after = p
            p = innerStart
            val inner = declarator(outer, allowFunc = allowFunc, allowVla = false, nameOptional = nameOptional)
            expectPunct(")", "missing ')'")
            p = after
            return Declarator(inner.name, inner.type, line, null, inner.params, inner.paramsKnown)
        }
        var name: String? = null
        if (p < toks.size && tok().kind == CTokenKind.IDENT && tok().text !in KEYWORDS) {
            name = tok().text
            p++
        } else if (!nameOptional) {
            fail(line, "expected a name here, found '${toks.getOrNull(p)?.text ?: "end of code"}'")
        }
        if (peekPunct("(")) {
            if (!allowFunc) fail(line, "a function cannot be declared here")
            if (type is CType.Arr) fail(line, "a function cannot return an array")
            if (type is CType.Func) fail(line, "a function cannot return a function (return a pointer to one)")
            val (params, known) = parameters()
            return Declarator(name, type, line, null, params, known)
        }
        // Array sizes, outermost first.
        val dims = mutableListOf<Pair<Int?, CExpr?>>()
        while (peekPunct("[")) {
            val bracket = tok()
            p++
            if (peekPunct("]")) {
                dims += null to null
            } else {
                val e = assignment()
                if (!e.type.decayed.isInteger) fail(bracket.line, "an array size must be a whole number")
                val c = constValue(e)
                if (c != null) {
                    if (c <= 0) fail(bracket.line, "an array size must be more than 0")
                    if (c > MAX_STATIC_ELEMENTS) fail(bracket.line, "this array is too big")
                    dims += c.toInt() to null
                } else {
                    dims += null to conv(e, CType.LONG)
                }
            }
            expectPunct("]", "missing ']'")
        }
        var sizeExpr: CExpr? = null
        for (i in dims.indices.reversed()) {
            val (size, expr) = dims[i]
            if (expr != null) {
                if (i != 0) fail(line, "only the first array size may be a variable")
                if (!allowVla) fail(line, "an array size here must be a constant number")
                sizeExpr = expr
            } else if (size == null && i != 0) {
                fail(line, "only the first array size may be left out")
            }
            if (type is CType.Func) fail(line, "an array of functions is not allowed; use an array of function pointers")
            type = CType.Arr(type, size)
        }
        return Declarator(name, type, line, sizeExpr, null, true)
    }

    /** `(` starts a nested declarator (`(*f)`, `(*a[3])`), not a parameter list. */
    private fun isNestedDeclarator(): Boolean {
        val next = toks.getOrNull(p + 1) ?: return false
        return next.isPunct("*") || next.isPunct("(") || next.isPunct("[") ||
            (next.kind == CTokenKind.IDENT && next.text !in KEYWORDS && !isTypeStart(next))
    }

    /** Array sizes and parameter lists after a parenthesized declarator, applied to [base]. */
    private fun suffixes(base: CType, line: Int): CType {
        val ops = mutableListOf<(CType) -> CType>()
        while (true) {
            if (peekPunct("[")) {
                val bracket = tok()
                p++
                val e = assignment()
                val c = constValue(e)?.takeIf { e.type.decayed.isInteger } ?: fail(bracket.line, "an array size here must be a constant number")
                if (c <= 0) fail(bracket.line, "an array size must be more than 0")
                if (c > MAX_STATIC_ELEMENTS) fail(bracket.line, "this array is too big")
                expectPunct("]", "missing ']'")
                ops += { t -> CType.Arr(t, c.toInt()) }
            } else if (peekPunct("(")) {
                val (params, known) = parameters()
                ops += { t ->
                    if (t is CType.Arr || t is CType.Func) fail(line, "a function cannot return ${if (t is CType.Arr) "an array" else "a function"}")
                    CType.Func(t, params.map { it.second }, known)
                }
            } else break
        }
        var t = base
        for (op in ops.reversed()) t = op(t)
        return t
    }

    private fun parameters(): Pair<List<Pair<String?, CType>>, Boolean> {
        p++ // (
        if (peekPunct(")")) { p++; return emptyList<Pair<String?, CType>>() to false }
        if (peekWord("void") && toks.getOrNull(p + 1)?.isPunct(")") == true) { p += 2; return emptyList<Pair<String?, CType>>() to true }
        val params = mutableListOf<Pair<String?, CType>>()
        while (true) {
            if (peekPunct("...")) fail(tok().line, "functions with '...' are not supported")
            val spec = specifiers() ?: fail(tok().line, "expected a parameter type, found '${tok().text}'")
            if (spec.isTypedef) fail(tok().line, "'typedef' cannot be used for a parameter")
            val d = declarator(spec.base, allowFunc = false, allowVla = true, nameOptional = true)
            val type = when (val t = d.type) {
                is CType.Arr -> CType.Ptr(t.of)       // array parameters are pointers
                is CType.Func -> CType.Ptr(t)         // and function parameters are function pointers
                CType.Void -> fail(d.line, "a parameter cannot be void")
                else -> t
            }
            params += d.name to type
            if (peekPunct(",")) { p++; continue }
            expectPunct(")", "missing ')' after the parameters")
            return params to true
        }
    }

    // ── Statements ──

    private fun blockItems(openLine: Int): List<CStmt> {
        scopes.addLast(Scope())
        val id = nextListId++
        listChain += id
        val out = mutableListOf<CStmt>()
        while (true) {
            if (p >= toks.size) fail(openLine, "'{' is never closed")
            if (peekPunct("}")) { endScope(scopes.last(), tok().line); p++; break }
            currentList = id
            out += statement()
        }
        listChain.removeAt(listChain.lastIndex)
        scopes.removeLast()
        return out
    }

    private fun statement(): CStmt {
        val t = tok()
        // Only a direct item of a { } list (or a switch body) may carry a label.
        val list = currentList
        currentList = -1
        if (t.isPunct("{")) { p++; return CStmt.Block(t.line, blockItems(t.line)) }
        if (t.isPunct(";")) { p++; return CStmt.Block(t.line, emptyList()) }
        if (isTypeStart(t)) return declaration()
        if (t.kind == CTokenKind.IDENT) when (t.text) {
            "if" -> {
                p++
                val cond = condition()
                val then = statement()
                val otherwise = if (peekWord("else")) { p++; statement() } else null
                return CStmt.If(t.line, cond, then, otherwise)
            }
            "else" -> fail(t.line, "'else' without a matching 'if'")
            "while" -> {
                p++
                val cond = condition()
                return CStmt.While(t.line, cond, loopBody())
            }
            "do" -> {
                p++
                val body = loopBody()
                if (!peekWord("while")) fail(tok().line, "expected 'while' after the do { … } block")
                p++
                val cond = condition()
                expectPunct(";", "missing ; after do … while (…)")
                return CStmt.DoWhile(t.line, body, cond)
            }
            "for" -> return forStatement()
            "switch" -> return switchStatement()
            "case", "default" -> fail(t.line, "'${t.text}' outside a switch")
            "break" -> {
                p++
                if (loopDepth == 0 && switchDepth == 0) fail(t.line, "'break' outside a loop or switch")
                expectPunct(";", "missing ; after break")
                return CStmt.Break(t.line)
            }
            "continue" -> {
                p++
                if (loopDepth == 0) fail(t.line, "'continue' outside a loop")
                expectPunct(";", "missing ; after continue")
                return CStmt.Continue(t.line)
            }
            "return" -> return returnStatement()
            "goto" -> {
                p++
                val target = toks.getOrNull(p)
                if (target == null || target.kind != CTokenKind.IDENT || target.text in KEYWORDS) fail(t.line, "goto needs a label name")
                p++
                expectPunct(";", "missing ; after goto ${target.text}")
                gotos += GotoUse(target.text, t.line, listChain.toList())
                return CStmt.Goto(t.line, target.text)
            }
        }
        if (t.kind == CTokenKind.IDENT && t.text !in KEYWORDS && toks.getOrNull(p + 1)?.isPunct(":") == true) {
            p += 2
            if (list < 0) fail(t.line, "put the label '${t.text}:' on a statement directly inside { } (here it is the body of an if or loop)")
            if (t.text in labels) fail(t.line, "the label '${t.text}' is defined twice")
            labels[t.text] = list
            if (p >= toks.size || peekPunct("}")) fail(t.line, "the label '${t.text}:' needs a statement after it (an empty ';' is fine)")
            currentList = list
            return CStmt.Labeled(t.line, t.text, statement())
        }
        val e = expression()
        expectStatementEnd(e)
        return CStmt.Expr(t.line, e)
    }

    private fun expectStatementEnd(e: CExpr) {
        if (peekPunct(";")) { p++; return }
        val line = toks.getOrNull(p - 1)?.line ?: e.line
        fail(line, "missing ; at the end of this statement")
    }

    private fun condition(): CExpr {
        expectPunct("(", "expected '('")
        val e = expression()
        expectPunct(")", "missing ')'")
        return truth(e)
    }

    private fun loopBody(): CStmt {
        loopDepth++
        try { return statement() } finally { loopDepth-- }
    }

    private fun forStatement(): CStmt {
        val line = tok().line
        p++
        expectPunct("(", "expected '(' after for")
        scopes.addLast(Scope())
        val init: CStmt? = when {
            peekPunct(";") -> { p++; null }
            isTypeStart(toks.getOrNull(p)) -> declaration()
            else -> expression().let { e -> expectPunct(";", "missing ; in for ( … ; … ; … )"); CStmt.Expr(e.line, e) }
        }
        val cond = if (peekPunct(";")) null else truth(expression())
        expectPunct(";", "missing ; in for ( … ; … ; … )")
        val step = if (peekPunct(")")) null else expression()
        expectPunct(")", "missing ')' after for ( … )")
        val body = loopBody()
        endScope(scopes.last(), toks[p - 1].line)
        scopes.removeLast()
        return CStmt.For(line, init, cond, step, body)
    }

    private fun switchStatement(): CStmt {
        val line = tok().line
        p++
        expectPunct("(", "expected '(' after switch")
        val subject = expression()
        expectPunct(")", "missing ')'")
        val st = subject.type.decayed
        if (!st.isInteger) fail(line, "switch needs a whole number (int or char), not $st")
        val subjectType = promote(st)
        val open = tok()
        expectPunct("{", "expected '{' after switch ( … )")
        scopes.addLast(Scope())
        val listId = nextListId++
        listChain += listId
        switchDepth++
        val body = mutableListOf<CStmt>()
        val cases = LinkedHashMap<Long, Int>()
        var default: Int? = null
        while (true) {
            if (p >= toks.size) fail(open.line, "'{' is never closed")
            val t = tok()
            when {
                t.isPunct("}") -> { p++; break }
                t.isWord("case") -> {
                    p++
                    val e = conditional()
                    val v = constValue(e) ?: fail(t.line, "a case needs a constant value")
                    expectPunct(":", "missing ':' after case")
                    val key = wrap(v, subjectType as CType.Num)
                    if (key in cases) fail(t.line, "the case value $v appears twice")
                    cases[key] = body.size
                }
                t.isWord("default") -> {
                    p++
                    expectPunct(":", "missing ':' after default")
                    if (default != null) fail(t.line, "a switch can have only one default")
                    default = body.size
                }
                else -> {
                    currentList = listId
                    body += statement()
                }
            }
        }
        listChain.removeAt(listChain.lastIndex)
        endScope(scopes.last(), toks[p - 1].line)
        switchDepth--
        scopes.removeLast()
        return CStmt.Switch(line, conv(subject, subjectType), body, cases, default)
    }

    private fun returnStatement(): CStmt {
        val line = tok().line
        p++
        val f = fn!!
        if (peekPunct(";")) {
            p++
            return CStmt.Return(line, null)
        }
        val e = expression()
        expectStatementEnd(e)
        if (f.ret == CType.Void) fail(line, "'${f.name}' is void, so it cannot return a value")
        return CStmt.Return(line, assignConv(e, f.ret, "the return value"))
    }

    private fun declaration(): CStmt {
        val line = tok().line
        val spec = specifiers()!!
        val decls = mutableListOf<CStmt>()
        if (peekPunct(";")) { p++; return CStmt.Block(line, emptyList()) }
        while (true) {
            val d = declarator(spec.base, allowFunc = true, allowVla = !spec.isStatic && !spec.isTypedef, nameOptional = false)
            val name = d.name!!
            val scope = scopes.last()
            if (spec.isTypedef) {
                defineTypedef(scope, name, declType(d), d.line)
                if (peekPunct(",")) { p++; continue }
                expectPunct(";", "missing ; after the typedef")
                break
            }
            if (d.params != null) fail(d.line, "declare functions outside other functions")
            if (d.type == CType.Void) fail(d.line, "a variable cannot be void")
            if (d.type is CType.Func) fail(d.line, "'$name' is a function type; declare a pointer instead (e.g. *$name)")
            if (scope.hasOrdinary(name)) fail(d.line, "'$name' is already declared in this block")
            val sym = if (spec.isStatic) VarSym(name, d.type, d.line, isGlobal = true, index = globalCount++)
            else VarSym(name, d.type, d.line, isGlobal = false, index = localCount++)
            scope.vars[name] = sym
            val (init, used) = if (peekPunct("=")) { p++; initializer(sym.type) } else null to 0
            val finalSym = withInferredSize(sym, init, used)
            if (finalSym !== sym) scope.vars[name] = finalSym
            fn?.locals?.add(finalSym)
            if (d.sizeExpr == null) checkKnownSize(finalSym)
            if (d.sizeExpr != null && init != null) fail(d.line, "an array with a variable size cannot have an initializer")
            if (spec.isStatic) globalInits += CStmt.Decl(d.line, finalSym, init, null)
            else decls += CStmt.Decl(d.line, finalSym, init, d.sizeExpr)
            if (peekPunct(",")) { p++; continue }
            expectPunct(";", "missing ; after the declaration")
            break
        }
        return if (decls.size == 1) decls[0] else CStmt.Block(decls.firstOrNull()?.line ?: 0, decls)
    }

    /** The variables of a block that just closed at [line] stop being shown by a trace after it. */
    private fun endScope(scope: Scope, line: Int) {
        for (v in scope.vars.values) if (v.scopeEnd == Int.MAX_VALUE) v.scopeEnd = line
    }

    private fun checkKnownSize(sym: VarSym) {
        val t = sym.type
        if (t.baseScalar.isIncomplete) fail(sym.line, "${t.baseScalar} is not complete (its members are not defined), so '${sym.name}' cannot be made")
        if (t is CType.Arr && t.size == null) fail(sym.line, "the size of the array '${sym.name}' is missing")
        if (t.slots() > MAX_STATIC_ELEMENTS) fail(sym.line, "the array '${sym.name}' is too big")
    }

    /** `int a[] = {…}` / `char s[] = "…"` take their size from the initializer ([used] slots). */
    private fun withInferredSize(sym: VarSym, init: CInit?, used: Int): VarSym {
        val t = sym.type
        if (t !is CType.Arr || t.size != null || init == null) return sym
        val es = maxOf(t.of.slots(), 1)
        val elems = (used + es - 1) / es
        return VarSym(sym.name, t.copy(size = maxOf(elems, 1)), sym.line, sym.isGlobal, sym.index)
    }

    /** Reads `= …` (after the `=`) for an object of [type]; returns it and the slots it covers. */
    private fun initializer(type: CType): Pair<CInit, Int> {
        val raw = rawInitializer()
        return CInitLayout { e, to -> assignConv(e, to, "this value") }.layout(type, raw)
    }

    private fun rawInitializer(): RawInit {
        val t = tok()
        if (t.isPunct("{")) {
            p++
            val items = mutableListOf<RawInit>()
            while (!peekPunct("}")) {
                items += if (peekPunct(".") && toks.getOrNull(p + 1)?.kind == CTokenKind.IDENT && toks.getOrNull(p + 2)?.isPunct("=") == true) {
                    val d = tok()
                    val member = toks[p + 1].text
                    p += 3
                    RawInit.Designated(member, rawInitializer(), d.line)
                } else {
                    if (peekPunct("[")) fail(tok().line, "array designators ([2] = …) are not supported; list the values in order")
                    rawInitializer()
                }
                if (peekPunct(",")) p++ else break
            }
            expectPunct("}", "missing '}' at the end of the initializer list")
            return RawInit.Braced(items, t.line)
        }
        if (t.kind == CTokenKind.STRING) {
            // A string alone fills a char array; followed by more (`"a" + …`) it is an ordinary value.
            val start = p
            val lit = CExpr.StrLit(t.line, stringLiteral())
            if (peekPunct(",") || peekPunct("}") || peekPunct(";")) return RawInit.Str(lit)
            p = start
        }
        return RawInit.Expr(assignment())
    }

    // ── Expressions ──

    private fun expression(): CExpr {
        var e = assignment()
        while (peekPunct(",")) {
            val line = tok().line
            p++
            e = CExpr.Comma(line, e, assignment())
        }
        return e
    }

    private fun assignment(): CExpr {
        val left = conditional()
        val t = toks.getOrNull(p) ?: return left
        if (t.kind != CTokenKind.PUNCT || t.text !in ASSIGN_OPS) return left
        p++
        val right = assignment()
        val line = t.line
        requireLvalue(left, line)
        val target = left.type
        if (target is CType.Arr) fail(line, "an array cannot be assigned; set its elements one by one (or use strcpy)")
        if (t.text == "=") return CExpr.Assign(line, left, assignConv(right, target, "the assigned value"), null, target)
        val op = t.text.dropLast(1)
        if ((op == "+" || op == "-") && target is CType.Ptr) {
            if (!right.type.decayed.isInteger) fail(line, "a pointer can only move by a whole number")
            scale(target, line)
            return CExpr.Assign(line, left, conv(right, CType.LONG), op, target)
        }
        val opType = binaryOpType(op, target, right.type.decayed, line)
        return CExpr.Assign(line, left, conv(right, if (op == "<<" || op == ">>") opType else opType), op, opType)
    }

    private fun conditional(): CExpr {
        val c = binary(0)
        if (!peekPunct("?")) return c
        val line = tok().line
        p++
        val a = expression()
        expectPunct(":", "missing ':' in a ? b : c")
        val b = conditional()
        val at = a.type.decayed
        val bt = b.type.decayed
        val type = when {
            at.isArith && bt.isArith -> common(at, bt)
            at is CType.Struct && at == bt -> at
            at is CType.Ptr && (bt is CType.Ptr || isNull(b)) -> at
            bt is CType.Ptr && isNull(a) -> bt
            at == CType.Void && bt == CType.Void -> CType.Void
            else -> fail(line, "the two results of ?: do not match ($at and $bt)")
        }
        return CExpr.Cond(line, truth(c), if (type == CType.Void) a else conv(a, type), if (type == CType.Void) b else conv(b, type), type)
    }

    private fun binary(minPrec: Int): CExpr {
        var left = unary()
        while (true) {
            val t = toks.getOrNull(p) ?: break
            val prec = if (t.kind == CTokenKind.PUNCT) PRECEDENCE[t.text] else null
            if (prec == null || prec < minPrec) break
            p++
            val right = binary(prec + 1)
            left = buildBinary(t.text, left, right, t.line)
        }
        return left
    }

    private fun buildBinary(op: String, l: CExpr, r: CExpr, line: Int): CExpr {
        val lt = l.type.decayed
        val rt = r.type.decayed
        if (lt == CType.Void || rt == CType.Void) fail(line, "a void value cannot be used with '$op'")
        when (op) {
            "&&", "||" -> return CExpr.Logical(line, op, truth(l), truth(r))
            "+" -> {
                if (lt is CType.Ptr && rt.isInteger) return CExpr.PtrAdd(line, l, conv(r, CType.LONG), 1, scale(lt, line), lt)
                if (rt is CType.Ptr && lt.isInteger) return CExpr.PtrAdd(line, r, conv(l, CType.LONG), 1, scale(rt, line), rt)
            }
            "-" -> {
                if (lt is CType.Ptr && rt.isInteger) return CExpr.PtrAdd(line, l, conv(r, CType.LONG), -1, scale(lt, line), lt)
                if (lt is CType.Ptr && rt is CType.Ptr) return CExpr.PtrDiff(line, l, r, scale(lt, line))
            }
            "==", "!=", "<", ">", "<=", ">=" -> {
                if ((lt is CType.Ptr || isNull(l)) && (rt is CType.Ptr || isNull(r)) && (lt is CType.Ptr || rt is CType.Ptr)) {
                    return CExpr.Binary(line, op, conv(l, CType.VOID_PTR), conv(r, CType.VOID_PTR), CType.VOID_PTR, CType.INT)
                }
            }
        }
        val opType = binaryOpType(op, lt, rt, line)
        val resultType = if (op in COMPARISONS) CType.INT else opType
        return CExpr.Binary(line, op, conv(l, opType), conv(r, opType), opType, resultType)
    }

    /** The type both operands of an arithmetic/bitwise/comparison operator are converted to. */
    private fun binaryOpType(op: String, lt: CType, rt: CType, line: Int): CType {
        if (!lt.isArith || !rt.isArith) fail(line, "'$op' cannot be used with $lt and $rt")
        return when (op) {
            "%", "&", "|", "^" -> {
                if (!lt.isInteger || !rt.isInteger) fail(line, "'$op' needs whole numbers (int, long, char), not $lt and $rt")
                common(lt, rt)
            }
            "<<", ">>" -> {
                if (!lt.isInteger || !rt.isInteger) fail(line, "'$op' needs whole numbers")
                promote(lt)
            }
            else -> common(lt, rt)
        }
    }

    private fun unary(): CExpr {
        val t = tok()
        if (t.kind == CTokenKind.PUNCT) when (t.text) {
            "++", "--" -> {
                p++
                val target = unary()
                return incDec(target, if (t.text == "++") 1 else -1, prefix = true, line = t.line)
            }
            "-", "+", "~" -> {
                p++
                val e = unary()
                val et = e.type.decayed
                if (t.text == "~" && !et.isInteger) fail(t.line, "'~' needs a whole number")
                if (!et.isArith) fail(t.line, "'${t.text}' needs a number, not $et")
                val pt = promote(et)
                return CExpr.Unary(t.line, t.text, conv(e, pt), pt)
            }
            "!" -> {
                p++
                return CExpr.Unary(t.line, "!", truth(unary()), CType.INT)
            }
            "*" -> {
                p++
                val e = unary()
                val et = e.type.decayed
                if (et !is CType.Ptr) fail(t.line, "'*' needs a pointer, but this is $et")
                if (et.to == CType.Void) fail(t.line, "a void * pointer cannot be used with '*'; cast it first")
                return CExpr.Deref(t.line, e, et.to)
            }
            "&" -> {
                p++
                val e = unary()
                if (e.type !is CType.Arr && e.type !is CType.Func) requireLvalue(e, t.line)
                return CExpr.AddrOf(t.line, e, CType.Ptr(e.type))
            }
            "(" -> if (isTypeStart(toks.getOrNull(p + 1))) {
                p++
                val type = typeName()
                expectPunct(")", "missing ')' after the type")
                val e = unary()
                return explicitCast(e, type, t.line)
            }
        }
        if (t.isWord("sizeof")) {
            p++
            val type = if (peekPunct("(") && isTypeStart(toks.getOrNull(p + 1))) {
                p++
                typeName().also { expectPunct(")", "missing ')' after sizeof(type") }
            } else {
                unary().type
            }
            if (type is CType.Arr && type.size == null) fail(t.line, "sizeof cannot measure an array whose size is a variable")
            if (type is CType.Func) fail(t.line, "sizeof cannot measure a function")
            if (type.baseScalar.isIncomplete) fail(t.line, "sizeof cannot measure ${type.baseScalar}: its members are not defined")
            return CExpr.IntLit(t.line, type.bytes(), CType.ULONG)
        }
        return postfix()
    }

    private fun typeName(): CType {
        val line = tok().line
        val spec = specifiers() ?: fail(line, "expected a type")
        if (spec.isTypedef || spec.isStatic) fail(line, "'${if (spec.isTypedef) "typedef" else "static"}' cannot be used here")
        val d = declarator(spec.base, allowFunc = false, allowVla = false, nameOptional = true)
        if (d.name != null) fail(d.line, "unexpected name '${d.name}' in a type")
        return d.type
    }

    private fun postfix(): CExpr {
        var e = primary()
        while (p < toks.size) {
            val t = tok()
            when {
                t.isPunct("[") -> {
                    p++
                    val idx = expression()
                    expectPunct("]", "missing ']'")
                    e = index(e, idx, t.line)
                }
                t.isPunct("++") || t.isPunct("--") -> {
                    p++
                    e = incDec(e, if (t.text == "++") 1 else -1, prefix = false, line = t.line)
                }
                t.isPunct(".") -> {
                    p++
                    e = member(e, memberName(t), t.line)
                }
                t.isPunct("->") -> {
                    p++
                    val bt = e.type.decayed
                    val to = (bt as? CType.Ptr)?.to
                    if (to !is CType.Struct) {
                        val hint = if (e.type is CType.Struct) " (it is a struct, so use '.')" else ""
                        fail(t.line, "'->' needs a pointer to a struct, but this is ${e.type}$hint")
                    }
                    e = member(CExpr.Deref(t.line, e, to), memberName(t), t.line)
                }
                t.isPunct("(") -> {
                    if ((e.type.decayed as? CType.Ptr)?.to !is CType.Func) fail(t.line, "only a function or a function pointer can be called, not ${e.type}")
                    e = callPtr(e, t.line)
                }
                else -> return e
            }
        }
        return e
    }

    private fun primary(): CExpr {
        val t = toks.getOrNull(p) ?: fail(toks.last().line, "the code ends in the middle of an expression")
        when (t.kind) {
            CTokenKind.NUMBER -> { p++; return number(t) }
            CTokenKind.CHAR -> {
                p++
                val chars = decodeEscapes(t.text.substring(t.text.indexOf('\'') + 1, t.text.length - 1), t.line)
                if (chars.size != 1) fail(t.line, "a character literal holds exactly one character")
                return CExpr.IntLit(t.line, chars[0].toLong(), CType.INT)
            }
            CTokenKind.STRING -> return CExpr.StrLit(t.line, stringLiteral())
            CTokenKind.IDENT -> {
                if (t.text in KEYWORDS) fail(t.line, "'${t.text}' cannot be used here")
                p++
                if (peekPunct("(")) return call(t)
                when (val r = resolve(t.text)) {
                    is VarSym -> return CExpr.Var(t.line, r)
                    is Long -> return CExpr.IntLit(t.line, r, CType.INT)          // enum constant
                    is CType -> fail(t.line, "'${t.text}' is a type name, not a value")
                }
                Builtin.STREAMS[t.text]?.let { return CExpr.IntLit(t.line, it, CType.LONG) }
                functions[t.text]?.let { return CExpr.FuncRef(t.line, it, null, funcType(it)) }
                Builtin.find(t.text)?.let { b ->
                    if (b.variadic) fail(t.line, "'${t.text}' cannot be used as a function pointer; call it with ( )")
                    return CExpr.FuncRef(t.line, null, b, CType.Func(b.ret, b.params, checkArgs = true))
                }
                fail(t.line, "'${t.text}' is not declared")
            }
            CTokenKind.PUNCT -> if (t.isPunct("(")) {
                p++
                val e = expression()
                expectPunct(")", "missing ')'")
                return e
            }
            CTokenKind.DIRECTIVE -> Unit
        }
        fail(t.line, "unexpected '${t.text}'")
    }

    private fun funcType(f: FuncSym) = CType.Func(f.ret, f.params.map { it.type }, f.checkArgs)

    private fun memberName(dot: CToken): String {
        val m = toks.getOrNull(p)
        if (m == null || m.kind != CTokenKind.IDENT || m.text in KEYWORDS) fail(dot.line, "expected a member name after '${dot.text}'")
        p++
        return m.text
    }

    private fun member(base: CExpr, name: String, line: Int): CExpr {
        val st = base.type as? CType.Struct ?: run {
            val hint = if ((base.type.decayed as? CType.Ptr)?.to is CType.Struct) " (it is a pointer, so use '->')" else ""
            fail(line, "'.' needs a struct, but this is ${base.type}$hint")
        }
        if (!st.def.complete) fail(line, "$st is not complete (its members are not defined)")
        val m = st.def.member(name) ?: fail(line, "$st has no member '$name'")
        return CExpr.Member(line, base, m, isLvalue(base))
    }

    /** Arguments `( … )` of a call; [p] is at the `(`. */
    private fun arguments(name: String): List<CExpr> {
        p++ // (
        val args = mutableListOf<CExpr>()
        if (!peekPunct(")")) {
            while (true) {
                args += assignment()
                if (peekPunct(",")) { p++; continue }
                break
            }
        }
        expectPunct(")", "missing ')' after the arguments of $name")
        return args
    }

    /** A call through [callee], whose type is a function or a pointer to one. */
    private fun callPtr(callee: CExpr, line: Int): CExpr {
        val ft = (callee.type.decayed as CType.Ptr).to as CType.Func
        val name = (callee as? CExpr.Var)?.sym?.name ?: "the function"
        val args = arguments(name)
        if (ft.checkArgs && args.size != ft.params.size) fail(line, "$name needs ${ft.params.size} argument(s) but got ${args.size}")
        val converted = args.mapIndexed { i, a ->
            ft.params.getOrNull(i)?.let { assignConv(a, it, "argument ${i + 1} of $name") } ?: promoteArg(a)
        }
        return CExpr.CallPtr(line, callee, converted, ft.ret)
    }

    private fun call(nameTok: CToken): CExpr {
        val line = nameTok.line
        val name = nameTok.text
        when (val r = resolve(name)) {
            is VarSym -> {
                if ((r.type.decayed as? CType.Ptr)?.to !is CType.Func) fail(line, "'$name' is a variable, not a function")
                return callPtr(CExpr.Var(line, r), line)
            }
            is Long -> fail(line, "'$name' is an enum value, not a function")
            is CType -> fail(line, "'$name' is a type name, not a function")
        }
        val args = arguments(name)
        val user = functions[name]
        if (user != null) {
            if (user.checkArgs && args.size != user.params.size) {
                fail(line, "'$name' needs ${user.params.size} argument(s) but got ${args.size}")
            }
            val converted = args.mapIndexed { i, a ->
                user.params.getOrNull(i)?.let { assignConv(a, it.type, "argument ${i + 1} of $name") } ?: promoteArg(a)
            }
            return CExpr.Call(line, name, user, null, converted, user.ret)
        }
        val builtin = Builtin.find(name) ?: fail(line, "the function '$name' is not declared (misspelled, or a missing #include?)")
        if (builtin == Builtin.SCANF) {
            args.drop(1).forEach { a ->
                if (a.type.decayed !is CType.Ptr) fail(a.line, "scanf needs the address of each variable: write &${(a as? CExpr.Var)?.sym?.name ?: "x"}")
            }
        }
        if (args.size < builtin.params.size || (!builtin.variadic && args.size > builtin.params.size)) {
            val needs = if (builtin.variadic) "at least ${builtin.params.size}" else "${builtin.params.size}"
            fail(line, "$name needs $needs argument(s) but got ${args.size}")
        }
        val converted = args.mapIndexed { i, a ->
            builtin.params.getOrNull(i)?.let { assignConv(a, it, "argument ${i + 1} of $name") } ?: promoteArg(a)
        }
        return CExpr.Call(line, name, null, builtin, converted, builtin.ret)
    }

    private fun index(base: CExpr, idx: CExpr, line: Int): CExpr {
        val bt = base.type.decayed
        val it = idx.type.decayed
        val (arr, i) = when {
            bt is CType.Ptr && it.isInteger -> base to idx
            it is CType.Ptr && bt.isInteger -> idx to base
            else -> fail(line, "'[ ]' needs an array or pointer and a whole-number index")
        }
        val ptrType = arr.type.decayed as CType.Ptr
        val bound = (arr.type as? CType.Arr)?.size
        return CExpr.Index(line, arr, conv(i, CType.LONG), scale(ptrType, line), ptrType.to, bound, bound?.let { exprName(arr) })
    }

    /** A short name for an array expression, for messages (`a`, `s.marks`, `p->name`, `m[…]`). */
    private fun exprName(e: CExpr): String = when (e) {
        is CExpr.Var -> e.sym.name
        is CExpr.Member -> (e.base as? CExpr.Deref)?.let { "${exprName(it.operand)}->${e.member.name}" } ?: "${exprName(e.base)}.${e.member.name}"
        is CExpr.Index -> "${exprName(e.base)}[…]"
        is CExpr.Deref -> "*${exprName(e.operand)}"
        else -> "the array"
    }

    private fun incDec(target: CExpr, delta: Int, prefix: Boolean, line: Int): CExpr {
        requireLvalue(target, line)
        val t = target.type
        if (t is CType.Ptr) scale(t, line) else if (!t.isArith) fail(line, "'++'/'--' need a number or a pointer")
        return CExpr.IncDec(line, target, delta, prefix, t)
    }

    // ── Types of expressions ──

    private fun isLvalue(e: CExpr): Boolean = when (e) {
        is CExpr.Var, is CExpr.Index -> true
        is CExpr.Deref -> e.type !is CType.Func
        is CExpr.Member -> e.baseIsLvalue
        else -> false
    }

    private fun requireLvalue(e: CExpr, line: Int) {
        val ok = isLvalue(e) && !(e is CExpr.Var && e.type is CType.Arr)
        if (!ok) fail(line, "only a variable, an array element, a struct member or *pointer can be changed here")
    }

    private fun truth(e: CExpr): CExpr {
        val t = e.type.decayed
        if (!t.isScalar) fail(e.line, "a condition must be a number or a pointer, not $t")
        return e
    }

    private fun scale(ptr: CType.Ptr, line: Int): Int {
        if (ptr.to == CType.Void) fail(line, "a void * pointer cannot be moved; cast it first")
        val to = ptr.to
        if (to is CType.Arr && to.size == null) fail(line, "this pointer's element size is unknown")
        return to.slots()
    }

    private fun isNull(e: CExpr): Boolean = when (e) {
        is CExpr.IntLit -> e.value == 0L && e.type.isInteger
        is CExpr.Cast -> e.type is CType.Ptr && isNull(e.operand)
        else -> false
    }

    /** Implicit conversion; only meaningful ones are allowed. */
    private fun conv(e: CExpr, to: CType): CExpr {
        val from = e.type.decayed
        if (from == to) return e
        if (from == CType.Void) fail(e.line, "a void value cannot be used here")
        if (from is CType.Struct || to is CType.Struct) fail(e.line, "cannot convert $from to $to")
        val ok = (from.isArith && to.isArith) || (from is CType.Ptr && to is CType.Ptr) || (isNull(e) && to is CType.Ptr)
        if (!ok) fail(e.line, "cannot convert $from to $to")
        return CExpr.Cast(e.line, e, to)
    }

    private fun assignConv(e: CExpr, to: CType, what: String): CExpr {
        val from = e.type.decayed
        if (from == CType.Void) fail(e.line, "$what is a void value")
        if (to is CType.Ptr && from.isInteger && !isNull(e)) fail(e.line, "$what should be a pointer (an address, e.g. &x), not $from")
        if (to.isArith && from is CType.Ptr) fail(e.line, "$what should be a number, not a pointer ($from)")
        if (from is CType.Struct && to is CType.Struct && from != to) fail(e.line, "$what is a $from, but a $to is needed")
        if (from is CType.Struct && to !is CType.Struct) fail(e.line, "$what is a whole $from; use one of its members (like .x)")
        if (to is CType.Struct && from !is CType.Struct) fail(e.line, "$what should be a $to, not $from")
        if (to is CType.Ptr && from is CType.Ptr && !isNull(e)) {
            val want = to.to
            val have = from.to
            if (want is CType.Func && have !is CType.Func) fail(e.line, "$what should be a function (like a function's name), not $from")
            if (have is CType.Func && want !is CType.Func && want != CType.Void) fail(e.line, "$what is a function, but $to is needed")
            if (want is CType.Func && have is CType.Func && !sameSignature(want, have)) {
                fail(e.line, "$what is a function of type $from, but the pointer needs $to")
            }
        }
        return conv(e, to)
    }

    private fun sameSignature(a: CType.Func, b: CType.Func) =
        a.ret == b.ret && (!a.checkArgs || !b.checkArgs || a.params == b.params)

    private fun explicitCast(e: CExpr, to: CType, line: Int): CExpr {
        val from = e.type.decayed
        if (to == CType.Void) return CExpr.Cast(line, e, CType.Void)
        if (from == CType.Void) fail(line, "a void value cannot be cast")
        if (to is CType.Struct || from is CType.Struct) fail(line, "a struct cannot be cast")
        if (to is CType.Ptr && from.isInteger && !isNull(e)) fail(line, "a number cannot be turned into a pointer")
        if (to.isArith && from is CType.Ptr) fail(line, "a pointer cannot be turned into a number")
        return if (from == to) e else CExpr.Cast(line, e, to)
    }

    /** Arguments of `printf`-like calls: float → double, small integers → int, arrays → pointers. */
    private fun promoteArg(e: CExpr): CExpr {
        val t = e.type.decayed
        return when {
            t == CType.Void -> fail(e.line, "a void value cannot be passed")
            t is CType.Struct -> fail(e.line, "a whole $t cannot be passed here; use its members (like .x)")
            t.isFloating -> conv(e, CType.DOUBLE)
            t.isInteger -> conv(e, promote(t))
            else -> conv(e, t)
        }
    }

    // ── Literals ──

    private fun number(t: CToken): CExpr {
        val raw = t.text.lowercase()
        val hex = raw.startsWith("0x")
        val isFloat = !hex && (raw.contains('.') || raw.contains('e')) || (hex && raw.contains('p'))
        if (isFloat) {
            val suffix = raw.takeLastWhile { it == 'f' || it == 'l' }
            val v = raw.dropLast(suffix.length).toDoubleOrNull() ?: fail(t.line, "'${t.text}' is not a valid number")
            val type = when (suffix) { "f" -> CType.FLOAT; "l" -> CType.Num(NumKind.LDOUBLE); else -> CType.DOUBLE }
            return CExpr.FloatLit(t.line, if (type == CType.FLOAT) v.toFloat().toDouble() else v, type)
        }
        val suffix = raw.takeLastWhile { it == 'u' || it == 'l' }
        val digits = raw.dropLast(suffix.length)
        val value = try {
            when {
                hex -> BigInteger(digits.substring(2), 16)
                digits.startsWith("0b") -> BigInteger(digits.substring(2), 2)
                digits.length > 1 && digits.startsWith("0") -> BigInteger(digits.substring(1), 8)
                else -> BigInteger(digits)
            }
        } catch (_: NumberFormatException) {
            fail(t.line, "'${t.text}' is not a valid number")
        }
        if (value.bitLength() > 64) fail(t.line, "the number ${t.text} is too big")
        val unsigned = 'u' in suffix
        val longs = suffix.count { it == 'l' }
        val type = when {
            longs == 0 && !unsigned && value <= BigInteger.valueOf(Int.MAX_VALUE.toLong()) -> CType.INT
            longs == 0 && unsigned && value.bitLength() <= 32 -> CType.UINT
            unsigned || value.bitLength() > 63 -> CType.ULONG
            else -> CType.LONG
        }
        return CExpr.IntLit(t.line, value.toLong(), type)
    }

    /** One or more adjacent string literals ("ab" "cd"), with the terminating 0. */
    private fun stringLiteral(): IntArray {
        val out = ArrayList<Int>()
        while (p < toks.size && tok().kind == CTokenKind.STRING) {
            val t = tok()
            val body = t.text.substring(t.text.indexOf('"') + 1, t.text.length - 1)
            decodeEscapes(body, t.line).forEach { out += it }
            p++
        }
        out += 0
        return out.toIntArray()
    }

    private fun decodeEscapes(body: String, line: Int): IntArray {
        val out = ArrayList<Int>()
        var i = 0
        while (i < body.length) {
            val c = body[i]
            if (c != '\\') { out += c.code; i++; continue }
            i++
            if (i >= body.length) fail(line, "a '\\' at the end of a literal")
            when (val e = body[i]) {
                'n' -> out += 10
                't' -> out += 9
                'r' -> out += 13
                '0', '1', '2', '3', '4', '5', '6', '7' -> {
                    var v = 0
                    var n = 0
                    while (n < 3 && i < body.length && body[i] in '0'..'7') { v = v * 8 + (body[i] - '0'); i++; n++ }
                    out += v
                    continue
                }
                'x' -> {
                    i++
                    val start = i
                    while (i < body.length && body[i].isLetterOrDigit() && body[i].lowercaseChar() in "0123456789abcdef") i++
                    if (start == i) fail(line, "\\x needs hex digits")
                    out += body.substring(start, i).toInt(16)
                    continue
                }
                'a' -> out += 7
                'b' -> out += 8
                'f' -> out += 12
                'v' -> out += 11
                '\\', '\'', '"', '?' -> out += e.code
                else -> fail(line, "unknown escape '\\$e'")
            }
            i++
        }
        return out.toIntArray()
    }

    // ── Helpers ──

    private fun constValue(e: CExpr): Long? = when (e) {
        is CExpr.IntLit -> e.value
        is CExpr.Cast -> if (e.type.isInteger) constValue(e.operand)?.let { wrap(it, e.type as CType.Num) } else null
        is CExpr.Unary -> constValue(e.operand)?.let { v ->
            when (e.op) { "-" -> -v; "+" -> v; "~" -> v.inv(); "!" -> if (v == 0L) 1 else 0; else -> null }
        }
        is CExpr.Binary -> {
            val a = constValue(e.left)
            val b = constValue(e.right)
            if (a == null || b == null) null else when (e.op) {
                "+" -> a + b; "-" -> a - b; "*" -> a * b
                "/" -> if (b == 0L) null else a / b
                "%" -> if (b == 0L) null else a % b
                "<<" -> a shl b.toInt(); ">>" -> a shr b.toInt()
                "&" -> a and b; "|" -> a or b; "^" -> a xor b
                else -> null
            }?.let { if (e.opType is CType.Num && e.type.isInteger) wrap(it, e.opType) else it }
        }
        else -> null
    }

    private fun tok(): CToken = toks.getOrNull(p) ?: fail(toks.last().line, "the code ends too early (a '}' or ';' may be missing)")
    private fun peekPunct(s: String) = p < toks.size && toks[p].isPunct(s)
    private fun peekWord(s: String) = p < toks.size && toks[p].isWord(s)

    private fun expectPunct(s: String, message: String) {
        if (peekPunct(s)) { p++; return }
        val line = if (s == ";" || s == ")") toks.getOrNull(p - 1)?.line ?: 1 else toks.getOrNull(p)?.line ?: toks.last().line
        fail(line, message)
    }

    private fun fail(line: Int, reason: String): Nothing = throw CProgramException(line, reason, isCompile = true)

    companion object {
        /** Integer promotion: small types become int. */
        fun promote(t: CType): CType =
            if (t is CType.Num && !t.kind.isFloat && t.kind.rank < NumKind.INT.rank) CType.INT else t

        /** The usual arithmetic conversions. */
        fun common(a: CType, b: CType): CType {
            val x = promote(a) as CType.Num
            val y = promote(b) as CType.Num
            if (x.kind.isFloat || y.kind.isFloat) {
                val k = maxOf(if (x.kind.isFloat) x.kind.rank else 0, if (y.kind.isFloat) y.kind.rank else 0)
                return CType.Num(NumKind.entries.first { it.rank == k })
            }
            if (x.kind.rank != y.kind.rank) {
                val big = if (x.kind.rank > y.kind.rank) x else y
                return big
            }
            return CType.Num(x.kind, x.unsigned || y.unsigned)
        }

        /** Wraps an integer to the range of [t], as storing it in a variable of that type would. */
        fun wrap(v: Long, t: CType.Num): Long = when (t.kind) {
            NumKind.BOOL -> if (v != 0L) 1 else 0
            NumKind.CHAR -> if (t.unsigned) v and 0xFF else v.toByte().toLong()
            NumKind.SHORT -> if (t.unsigned) v and 0xFFFF else v.toShort().toLong()
            NumKind.INT -> if (t.unsigned) v and 0xFFFFFFFFL else v.toInt().toLong()
            else -> v
        }

        const val MAX_STATIC_ELEMENTS = 4_000_000L

        private val SPEC_WORDS = setOf(
            "void", "char", "short", "int", "long", "float", "double", "signed", "unsigned", "_Bool", "bool",
            "size_t", "const", "volatile", "static", "extern", "register", "auto", "inline",
            "struct", "union", "enum", "typedef",
        )
        private val NUMERIC_WORDS = setOf("void", "char", "short", "int", "long", "float", "double", "signed", "unsigned", "_Bool", "bool", "size_t")
        private val STATEMENT_WORDS = setOf("if", "for", "while", "switch", "return", "sizeof", "do", "else")
        private val KEYWORDS = SPEC_WORDS + STATEMENT_WORDS + setOf("case", "default", "break", "continue", "goto")
        private val ASSIGN_OPS = setOf("=", "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<=", ">>=")
        private val COMPARISONS = setOf("==", "!=", "<", ">", "<=", ">=")
        private val PRECEDENCE = mapOf(
            "||" to 1, "&&" to 2, "|" to 3, "^" to 4, "&" to 5,
            "==" to 6, "!=" to 6, "<" to 7, ">" to 7, "<=" to 7, ">=" to 7,
            "<<" to 8, ">>" to 8, "+" to 9, "-" to 9, "*" to 10, "/" to 10, "%" to 10,
        )
    }
}
