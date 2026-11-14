package com.prostuti.student.domain.code.c.run

import com.prostuti.student.domain.code.run.TraceCommand
import com.prostuti.student.domain.code.run.TraceFrame
import com.prostuti.student.domain.code.run.TraceStep
import java.util.IdentityHashMap

/**
 * Runs a checked [CProgram]. Stricter than real C where that helps a learner: reading a variable
 * that was never given a value, going outside an array, dividing by zero, using a NULL or freed
 * pointer and reading a union member other than the one stored last are runtime errors with the
 * line, not silent garbage. Call [run] on a thread with a large stack (deep recursion); it checks
 * [CIo.stopped] regularly.
 *
 * With a [tracer], the program pauses before each statement (and before each loop test) and hands
 * the tracer the line, the call stack and every variable in scope, until the tracer says
 * [TraceCommand.RUN_TO_END].
 */
class CInterpreter(private val io: CIo, private val tracer: CTracer? = null) {

    private var globals: Array<MemObj?> = emptyArray()
    private var frame: Array<MemObj?> = emptyArray()
    private var retVal: Any = 0L
    private var depth = 0
    private var ticks = 0
    private var allocated = 0L
    private val strings = IdentityHashMap<CExpr.StrLit, MemObj>()
    // A function pointer is a pointer to the function's own (empty) memory object.
    private val funcObjs = IdentityHashMap<Any, MemObj>()
    private val funcTargets = IdentityHashMap<MemObj, Any>()
    private var gotoLabel = ""

    // Tracing: the call stack (function, its frame, the line it is at).
    private var tracing = tracer != null
    private var program: CProgram? = null
    private val stackFns = ArrayList<FuncSym>()
    private val stackFrames = ArrayList<Array<MemObj?>>()
    private val stackLines = ArrayList<Int>()
    private val format = CTraceFormat { funcTargets[it]?.let { t -> (t as? FuncSym)?.name ?: (t as Builtin).cName } }
    private val input = CInput(io)
    private val library = CLibrary(this, io, input)

    // Set by ref(): the slot an lvalue expression names.
    private var refIndex = 0

    /** Runs `main` to the end (or `exit()`); throws [CProgramException] on a runtime error, [CStop] when stopped. */
    fun run(program: CProgram) {
        this.program = program
        globals = arrayOfNulls(program.globalCount)
        for (d in program.globalInits) declare(d)
        val main = program.main
        try {
            call(main, main.params.map { if (it.type is CType.Ptr) CPtr.NULL else 0L }, main.line)
        } catch (_: CExit) {
            // exit(): a normal end.
        }
    }

    // ── Statements ──

    private fun exec(s: CStmt): Int {
        if (++ticks and 0x3FF == 0 && io.stopped) throw CStop()
        if (tracing) when (s) {
            is CStmt.Block, is CStmt.Labeled, is CStmt.While, is CStmt.DoWhile, is CStmt.For -> Unit   // they pause themselves
            // `int a = 1, b = 2;` pauses once.
            is CStmt.Decl -> if (stackLines.last() != s.line) pause(s.line)
            else -> pause(s.line)
        }
        return when (s) {
            is CStmt.Expr -> { eval(s.expr); NORMAL }
            is CStmt.Decl -> { declare(s); NORMAL }
            // (`int a = 1, b = 2;` is a block of declarations too: its variables live on.)
            is CStmt.Block -> runList(s.stmts, 0).also { if (tracer != null && !s.stmts.all { it is CStmt.Decl }) forgetLocals(s.stmts) }
            is CStmt.If -> if (truthy(eval(s.cond))) exec(s.then) else s.otherwise?.let { exec(it) } ?: NORMAL
            is CStmt.While -> {
                while (true) {
                    if (tracing) pause(s.line)
                    if (!truthy(eval(s.cond))) break
                    val r = exec(s.body)
                    if (r == BREAK) break
                    if (r == RETURN || r == GOTO) return r
                }
                NORMAL
            }
            is CStmt.DoWhile -> {
                do {
                    val r = exec(s.body)
                    if (r == BREAK) break
                    if (r == RETURN || r == GOTO) return r
                    if (tracing) pause(s.cond.line)
                } while (truthy(eval(s.cond)))
                NORMAL
            }
            is CStmt.For -> {
                if (tracing) pause(s.line)
                s.init?.let { forInit(it) }
                while (s.cond == null || truthy(eval(s.cond))) {
                    val r = exec(s.body)
                    if (r == BREAK) break
                    if (r == RETURN || r == GOTO) return r
                    if (tracing) pause(s.line)
                    s.step?.let { eval(it) }
                    if (s.cond == null && ++ticks and 0x3FF == 0 && io.stopped) throw CStop()
                }
                s.init?.let { if (tracer != null) forgetLocals(listOf(it)) }
                NORMAL
            }
            is CStmt.Switch -> {
                val v = eval(s.subject) as Long
                val start = s.cases[v] ?: s.default ?: return NORMAL
                val r = runList(s.body, start)
                if (r == BREAK) NORMAL else r
            }
            is CStmt.Break -> BREAK
            is CStmt.Continue -> CONTINUE
            is CStmt.Return -> {
                retVal = s.value?.let { eval(it) } ?: 0L
                RETURN
            }
            is CStmt.Goto -> {
                gotoLabel = s.label
                GOTO
            }
            is CStmt.Labeled -> exec(s.stmt)
        }
    }

    /** A `for`'s first part; the `for` already paused on its line, so this does not pause again. */
    private fun forInit(s: CStmt) {
        when (s) {
            is CStmt.Decl -> declare(s)
            is CStmt.Expr -> eval(s.expr)
            is CStmt.Block -> s.stmts.forEach { forInit(it) }
            else -> exec(s)
        }
    }

    // ── Tracing ──

    /** A block ended: its variables are gone, so the next pass through it does not show old values. */
    private fun forgetLocals(stmts: List<CStmt>) {
        for (st in stmts) when (st) {
            is CStmt.Decl -> if (!st.sym.isGlobal) frame[st.sym.index] = null
            is CStmt.Block -> if (st.stmts.all { it is CStmt.Decl }) forgetLocals(st.stmts)   // `int a, b;`
            is CStmt.Labeled -> forgetLocals(listOf(st.stmt))
            else -> Unit
        }
    }

    private fun pause(line: Int) {
        if (stackLines.isEmpty()) return
        stackLines[stackLines.lastIndex] = line
        if (tracer!!.pause(snapshot(line)) == TraceCommand.RUN_TO_END) tracing = false
    }

    private fun snapshot(line: Int): TraceStep {
        val frames = stackFns.indices.map { i ->
            val f = stackFns[i]
            val fr = stackFrames[i]
            val at = stackLines[i]
            // Later (inner) declarations hide earlier ones with the same name.
            val visible = LinkedHashMap<String, com.prostuti.student.domain.code.run.TraceVar>()
            for (v in f.params) fr.getOrNull(v.index)?.let { visible[v.name] = format.describe(v.name, v.type, it, 0) }
            for (v in f.locals) {
                if (at < v.line || at > v.scopeEnd) continue
                val obj = (if (v.isGlobal) globals.getOrNull(v.index) else fr.getOrNull(v.index)) ?: continue
                visible.remove(v.name)
                visible[v.name] = format.describe(v.name, v.type, obj, 0)
            }
            TraceFrame(f.name, at, visible.values.toList())
        }
        val globalVars = program?.globals.orEmpty().mapNotNull { v -> globals.getOrNull(v.index)?.let { format.describe(v.name, v.type, it, 0) } }
        return TraceStep(line, frames, globalVars)
    }

    /** Runs [stmts] from [from]; a goto to a label among them continues there. */
    private fun runList(stmts: List<CStmt>, from: Int): Int {
        var k = from
        while (k < stmts.size) {
            val r = exec(stmts[k])
            if (r == GOTO) {
                val target = stmts.indexOfFirst { hasLabel(it, gotoLabel) }
                if (target >= 0) { k = target; continue }
            }
            if (r != NORMAL) return r
            k++
        }
        return NORMAL
    }

    private fun hasLabel(s: CStmt, label: String): Boolean =
        s is CStmt.Labeled && (s.label == label || hasLabel(s.stmt, label))

    private fun declare(d: CStmt.Decl) {
        val sym = d.sym
        val t = sym.type
        val elem = if (t is CType.Arr) t.of else t
        val es = elem.slots()
        val count = if (t is CType.Arr) {
            if (d.sizeExpr != null) {
                val n = eval(d.sizeExpr) as Long
                if (n <= 0) fail(d.line, "the array '${sym.name}' must have a size of at least 1 (it is $n)")
                n
            } else t.size!!.toLong()
        } else 1L
        val obj = newObj(sym.name, count * es, es, d.line, elem)
        if (sym.isGlobal || d.init != null) {
            for (k in 0 until count.toInt()) fillZero(obj.slots, k * es, elem)
        }
        when (val init = d.init) {
            null -> Unit
            is CInit.Single -> storeValue(obj, 0, eval(init.expr), d.line)
            is CInit.Placed -> for (k in init.slots.indices) {
                val v = init.values[k]
                storeValue(obj, init.slots[k], if (v is CExpr) eval(v) else v, d.line, initializing = true)
            }
        }
        if (sym.isGlobal) globals[sym.index] = obj else frame[sym.index] = obj
    }

    private fun zero(t: CType): Any = when {
        t is CType.Ptr -> CPtr.NULL
        t.isFloating -> 0.0
        else -> 0L
    }

    /** Sets the object of [type] at [start] to zero, slot by slot with each slot's own kind of zero. */
    private fun fillZero(slots: Array<Any?>, start: Int, type: CType) {
        when (type) {
            is CType.Arr -> {
                val es = type.of.slots()
                for (k in 0 until (type.size ?: 0)) fillZero(slots, start + k * es, type.of)
            }
            is CType.Struct -> if (type.def.isUnion) {
                type.def.members.firstOrNull()?.let { fillZero(slots, start, it.type) }
                for (k in start until start + type.slots()) if (slots[k] == null) slots[k] = 0L
            } else {
                type.def.members.forEach { fillZero(slots, start + it.offset, it.type) }
            }
            else -> slots[start] = zero(type)
        }
    }

    internal fun newObj(name: String, slots: Long, elemSlots: Int, line: Int, elemType: CType? = null): MemObj {
        allocated += slots
        if (slots > MAX_SLOTS || allocated > MAX_SLOTS) fail(line, "the program uses too much memory")
        return MemObj(name, arrayOfNulls(slots.toInt()), elemSlots, elemType = elemType)
    }

    // ── Calls ──

    internal fun call(f: FuncSym, args: List<Any>, line: Int): Any {
        if (++depth > MAX_DEPTH) fail(line, "too many nested function calls (does '${f.name}' call itself forever?)")
        val newFrame = arrayOfNulls<MemObj>(f.localCount)
        f.params.forEachIndexed { i, p ->
            val v = args[i]
            newFrame[i] = if (v is StructVal) MemObj(p.name, v.slots.copyOf(), v.slots.size, elemType = p.type)
            else MemObj(p.name, arrayOf(v), 1, elemType = p.type)
        }
        val saved = frame
        frame = newFrame
        retVal = 0L
        if (tracer != null) {
            stackFns += f
            stackFrames += newFrame
            stackLines += 0
        }
        try {
            val r = exec(f.body!!)
            if (r != RETURN && f.ret is CType.Struct) fail(f.line, "'${f.name}' ended without returning a ${f.ret}")
            return if (r == RETURN) retVal else 0L
        } finally {
            frame = saved
            depth--
            if (tracer != null) {
                stackFns.removeAt(stackFns.lastIndex)
                stackFrames.removeAt(stackFrames.lastIndex)
                stackLines.removeAt(stackLines.lastIndex)
            }
        }
    }

    // ── Expressions ──

    internal fun eval(e: CExpr): Any = when (e) {
        is CExpr.IntLit -> e.value
        is CExpr.FloatLit -> e.value
        is CExpr.StrLit -> CPtr(stringObj(e), 0)
        is CExpr.Var -> valueAt(varObj(e.sym, e.line), 0, e.type, e.line)
        is CExpr.Unary -> unary(e)
        is CExpr.AddrOf -> if (e.operand.type is CType.Func) eval(e.operand) else {
            val obj = ref(e.operand)
            CPtr(obj, refIndex)
        }
        is CExpr.Deref -> if (e.type is CType.Func) eval(e.operand) else {
            val obj = ref(e)
            valueAt(obj, refIndex, e.type, e.line)
        }
        is CExpr.Index -> {
            val obj = ref(e)
            valueAt(obj, refIndex, e.type, e.line)
        }
        is CExpr.Member -> member(e)
        is CExpr.FuncRef -> funcPtr(e.fn ?: e.builtin!!, e.fn?.name ?: e.builtin!!.cName)
        is CExpr.CallPtr -> callPtr(e)
        is CExpr.IncDec -> incDec(e)
        is CExpr.Binary -> binary(e.op, eval(e.left), eval(e.right), e.opType, e.line)
        is CExpr.PtrAdd -> {
            val p = eval(e.ptr) as CPtr
            val off = eval(e.offset) as Long
            if (p.obj == null) fail(e.line, "arithmetic on a NULL pointer")
            movePtr(p, e.sign * off * e.scale, e.line)
        }
        is CExpr.PtrDiff -> {
            val a = eval(e.left) as CPtr
            val b = eval(e.right) as CPtr
            if (a.obj !== b.obj) fail(e.line, "subtracting pointers into different arrays")
            ((a.index - b.index) / e.scale).toLong()
        }
        is CExpr.Logical -> if (e.op == "&&") {
            if (truthy(eval(e.left)) && truthy(eval(e.right))) 1L else 0L
        } else {
            if (truthy(eval(e.left)) || truthy(eval(e.right))) 1L else 0L
        }
        is CExpr.Assign -> assign(e)
        is CExpr.Cond -> if (truthy(eval(e.cond))) eval(e.ifTrue) else eval(e.ifFalse)
        is CExpr.Call -> {
            val args = e.args.map { eval(it) }
            if (e.fn != null) call(e.fn, args, e.line) else library.call(e.builtin!!, args, e.args, e.line)
        }
        is CExpr.Cast -> convert(eval(e.operand), e.operand.type.decayed, e.type, e.line)
        is CExpr.Comma -> { eval(e.left); eval(e.right) }
    }

    /** A value of [type] stored at [idx]: arrays give a pointer, structs a copy, scalars the slot. */
    private fun valueAt(obj: MemObj, idx: Int, type: CType, line: Int): Any = when (type) {
        is CType.Arr -> CPtr(obj, idx)
        is CType.Struct -> {
            val n = type.slots()
            if (obj.freed) fail(line, "using memory after free()")
            if (obj.untypedBytes >= 0) fail(line, "malloc() memory must be used through a typed pointer")
            if (idx < 0 || idx + n > obj.slots.size) outOfBounds(obj, idx, line)
            StructVal(obj.slots.copyOfRange(idx, idx + n))
        }
        else -> load(obj, idx, line)
    }

    private fun member(e: CExpr.Member): Any {
        val t = e.type
        val v = if (e.baseIsLvalue) {
            val obj = ref(e)
            valueAt(obj, refIndex, t, e.line)
        } else {
            // A member of a struct value that is not stored anywhere (`make().x`).
            val sv = eval(e.base) as StructVal
            val off = e.member.offset
            when (t) {
                is CType.Arr -> fail(e.line, "store the returned struct in a variable before using its array '${e.member.name}'")
                is CType.Struct -> StructVal(sv.slots.copyOfRange(off, off + t.slots()))
                else -> sv.slots[off] ?: fail(e.line, "the member '${e.member.name}' of this struct was never given a value")
            }
        }
        if (!e.isUnion || t !is CType.Num && t !is CType.Ptr) return v
        // A union holds one member at a time.
        val fits = when {
            t is CType.Ptr -> v is CPtr
            (t as CType.Num).kind.isFloat -> v is Double
            else -> v is Long
        }
        if (!fits) fail(e.line, "the union member '${e.member.name}' does not hold a value now: another member was stored in it last")
        return if (v is Long) CProgramParser.wrap(v, t as CType.Num) else v
    }

    private fun funcPtr(target: Any, name: String): CPtr = CPtr(
        funcObjs.getOrPut(target) { MemObj(name, emptyArray(), 1, readOnly = true).also { funcTargets[it] = target } },
        0,
    )

    private fun callPtr(e: CExpr.CallPtr): Any {
        val p = eval(e.callee) as CPtr
        val obj = p.obj ?: fail(e.line, "calling a NULL function pointer (it points to no function)")
        val target = funcTargets[obj] ?: fail(e.line, "this pointer does not point to a function")
        val args = e.args.map { eval(it) }
        return when (target) {
            is FuncSym -> {
                if (args.size != target.params.size) fail(e.line, "'${target.name}' needs ${target.params.size} argument(s) but got ${args.size}")
                val ft = (e.callee.type.decayed as CType.Ptr).to as CType.Func
                // Through an `int (*f)()` pointer the arguments were not converted to the parameter types yet.
                val converted = if (ft.checkArgs) args else args.mapIndexed { i, v ->
                    val to = target.params[i].type
                    if (v is StructVal || to is CType.Struct) v else convert(v, e.args[i].type.decayed, to, e.line)
                }
                call(target, converted, e.line)
            }
            else -> library.call(target as Builtin, args, e.args, e.line)
        }
    }

    /** The memory an lvalue names; its slot is left in [refIndex]. */
    private fun ref(e: CExpr): MemObj = when (e) {
        is CExpr.Var -> { refIndex = 0; varObj(e.sym, e.line) }
        is CExpr.Member -> {
            val obj = ref(e.base)
            refIndex += e.member.offset
            obj
        }
        is CExpr.Deref -> {
            val p = eval(e.operand) as CPtr
            val obj = p.obj ?: fail(e.line, "using a NULL pointer (it points nowhere)")
            refIndex = p.index
            obj
        }
        is CExpr.Index -> {
            val p = eval(e.base) as CPtr
            val i = eval(e.index) as Long
            val obj = p.obj ?: fail(e.line, "indexing a NULL pointer")
            if (e.bound != null && (i < 0 || i >= e.bound)) {
                fail(e.line, "index $i is outside '${e.boundName}', which has ${e.bound} element(s) (valid: 0 to ${e.bound - 1})")
            }
            val idx = p.index + i * e.scale
            if (idx < Int.MIN_VALUE || idx > Int.MAX_VALUE) outOfBounds(obj, Int.MAX_VALUE, e.line)
            refIndex = idx.toInt()
            obj
        }
        else -> error("not an lvalue")
    }

    private fun varObj(sym: VarSym, line: Int): MemObj =
        (if (sym.isGlobal) globals[sym.index] else frame[sym.index])
            ?: fail(line, "'${sym.name}' is used before its declaration runs (a goto jumped over it)")

    private fun stringObj(e: CExpr.StrLit): MemObj = strings.getOrPut(e) {
        MemObj("string literal", Array(e.chars.size) { e.chars[it].toLong() }, 1, readOnly = true)
    }

    internal fun load(obj: MemObj, idx: Int, line: Int): Any {
        if (obj.freed) fail(line, "using memory after free()")
        if (obj.untypedBytes >= 0) fail(line, "malloc() memory must be used through a typed pointer")
        if (idx < 0 || idx >= obj.slots.size) outOfBounds(obj, idx, line)
        return obj.slots[idx] ?: fail(line, uninitialized(obj, idx))
    }

    /** Stores a scalar, or copies a whole struct value into the slots from [idx]. */
    private fun storeValue(obj: MemObj, idx: Int, value: Any, line: Int, initializing: Boolean = false) {
        if (value !is StructVal) {
            if (initializing) obj.slots[idx] = value else store(obj, idx, value, line)
            return
        }
        if (obj.readOnly) fail(line, "a string literal cannot be changed (copy it into a char array first)")
        if (obj.freed) fail(line, "using memory after free()")
        if (idx < 0 || idx + value.slots.size > obj.slots.size) outOfBounds(obj, idx, line)
        System.arraycopy(value.slots, 0, obj.slots, idx, value.slots.size)
    }

    internal fun store(obj: MemObj, idx: Int, value: Any, line: Int) {
        if (obj.readOnly) fail(line, "a string literal cannot be changed (copy it into a char array first)")
        if (obj.freed) fail(line, "using memory after free()")
        if (idx < 0 || idx >= obj.slots.size) outOfBounds(obj, idx, line)
        obj.slots[idx] = value
    }

    private fun uninitialized(obj: MemObj, idx: Int): String {
        val et = obj.elemType
        val es = maxOf(obj.elemSlots, 1)
        return when {
            et != null -> {
                val head = if (obj.slots.size == es) obj.name else "${obj.name}[${idx / es}]"
                "'$head${slotPath(et, idx % es)}' is used before it is given a value"
            }
            obj.slots.size == 1 -> "'${obj.name}' is used before it is given a value"
            obj.elemSlots == 1 -> "'${obj.name}[$idx]' is used before it is given a value"
            else -> "an element of '${obj.name}' is used before it is given a value"
        }
    }

    private fun outOfBounds(obj: MemObj, idx: Int, line: Int): Nothing {
        val es = maxOf(obj.elemSlots, 1)
        val count = obj.slots.size / es
        val element = Math.floorDiv(idx, es)
        if (obj.slots.size == 1 && obj.name != "malloc") fail(line, "going outside the variable '${obj.name}' through a pointer")
        fail(line, "index $element is outside '${obj.name}', which has $count element(s) (valid: 0 to ${count - 1})")
    }

    internal fun movePtr(p: CPtr, delta: Long, line: Int): CPtr {
        val idx = p.index + delta
        if (idx < Int.MIN_VALUE || idx > Int.MAX_VALUE) fail(line, "the pointer moved far outside its array")
        return CPtr(p.obj, idx.toInt())
    }

    private fun unary(e: CExpr.Unary): Any {
        val v = eval(e.operand)
        return when (e.op) {
            "!" -> if (truthy(v)) 0L else 1L
            "+" -> v
            "-" -> if (v is Double) round(-v, e.type) else CProgramParser.wrap(-(v as Long), e.type as CType.Num)
            else -> CProgramParser.wrap((v as Long).inv(), e.type as CType.Num)
        }
    }

    private fun incDec(e: CExpr.IncDec): Any {
        val obj = ref(e.target)
        val idx = refIndex
        val cur = load(obj, idx, e.line)
        val t = e.target.type
        val next: Any = when {
            t is CType.Ptr -> {
                val p = cur as CPtr
                if (p.obj == null) fail(e.line, "moving a NULL pointer")
                movePtr(p, e.delta.toLong() * t.to.slots(), e.line)
            }
            cur is Double -> round(cur + e.delta, t)
            else -> CProgramParser.wrap((cur as Long) + e.delta, t as CType.Num)
        }
        store(obj, idx, next, e.line)
        return if (e.prefix) next else cur
    }

    private fun assign(e: CExpr.Assign): Any {
        val obj = ref(e.target)
        val idx = refIndex
        if (e.op == null) {
            val v = eval(e.value)
            storeValue(obj, idx, v, e.line)
            return v
        }
        val t = e.target.type
        val cur = load(obj, idx, e.line)
        val rhs = eval(e.value)
        val result: Any = if (t is CType.Ptr) {
            val p = cur as CPtr
            if (p.obj == null) fail(e.line, "moving a NULL pointer")
            val sign = if (e.op == "+") 1 else -1
            movePtr(p, sign * (rhs as Long) * t.to.slots(), e.line)
        } else {
            val a = convert(cur, t, e.opType, e.line)
            convert(binary(e.op, a, rhs, e.opType, e.line), e.opType, t, e.line)
        }
        store(obj, idx, result, e.line)
        return result
    }

    private fun binary(op: String, a: Any, b: Any, opType: CType, line: Int): Any {
        if (opType is CType.Ptr) return comparePtr(op, a as CPtr, b as CPtr, line)
        val t = opType as CType.Num
        if (t.kind.isFloat) {
            val x = a as Double
            val y = b as Double
            return when (op) {
                "+" -> round(x + y, t)
                "-" -> round(x - y, t)
                "*" -> round(x * y, t)
                "/" -> round(x / y, t)
                "==" -> bool(x == y); "!=" -> bool(x != y)
                "<" -> bool(x < y); ">" -> bool(x > y)
                "<=" -> bool(x <= y); ">=" -> bool(x >= y)
                else -> error(op)
            }
        }
        val x = a as Long
        val y = b as Long
        val u64 = t.unsigned && t.kind.bytes == 8
        return when (op) {
            "+" -> CProgramParser.wrap(x + y, t)
            "-" -> CProgramParser.wrap(x - y, t)
            "*" -> CProgramParser.wrap(x * y, t)
            "/" -> {
                if (y == 0L) fail(line, "division by zero")
                CProgramParser.wrap(if (u64) java.lang.Long.divideUnsigned(x, y) else x / y, t)
            }
            "%" -> {
                if (y == 0L) fail(line, "division by zero (% by 0)")
                CProgramParser.wrap(if (u64) java.lang.Long.remainderUnsigned(x, y) else x % y, t)
            }
            "<<" -> CProgramParser.wrap(x shl (y and 63).toInt(), t)
            ">>" -> CProgramParser.wrap(if (u64) x ushr (y and 63).toInt() else x shr (y and 63).toInt(), t)
            "&" -> CProgramParser.wrap(x and y, t)
            "|" -> CProgramParser.wrap(x or y, t)
            "^" -> CProgramParser.wrap(x xor y, t)
            "==" -> bool(x == y)
            "!=" -> bool(x != y)
            else -> {
                val c = if (u64) java.lang.Long.compareUnsigned(x, y) else x.compareTo(y)
                when (op) { "<" -> bool(c < 0); ">" -> bool(c > 0); "<=" -> bool(c <= 0); else -> bool(c >= 0) }
            }
        }
    }

    private fun comparePtr(op: String, a: CPtr, b: CPtr, line: Int): Long {
        val same = a.obj === b.obj
        return when (op) {
            "==" -> bool(same && a.index == b.index)
            "!=" -> bool(!(same && a.index == b.index))
            else -> {
                if (!same) fail(line, "comparing pointers into different arrays")
                val c = a.index.compareTo(b.index)
                when (op) { "<" -> bool(c < 0); ">" -> bool(c > 0); "<=" -> bool(c <= 0); else -> bool(c >= 0) }
            }
        }
    }

    internal fun convert(v: Any, from: CType, to: CType, line: Int): Any {
        if (to == CType.Void) return 0L
        if (to is CType.Ptr) {
            val p = if (v is CPtr) v else CPtr.NULL
            val obj = p.obj
            if (obj != null && obj.untypedBytes >= 0 && to.to != CType.Void && to.to !is CType.Func) typeMalloc(obj, to.to)
            return p
        }
        val t = to as CType.Num
        return when (v) {
            is Long -> if (t.kind.isFloat) {
                val d = if (from is CType.Num && from.unsigned && from.kind.bytes == 8 && v < 0) v.toULong().toDouble() else v.toDouble()
                round(d, t)
            } else CProgramParser.wrap(v, t)
            is Double -> if (t.kind.isFloat) round(v, t) else when {
                t.kind == NumKind.BOOL -> bool(v != 0.0)
                v.isNaN() -> 0L
                else -> CProgramParser.wrap(v.toLong(), t)
            }
            else -> fail(line, "a pointer cannot be used as a number")
        }
    }

    /** A malloc() block gets its element type from the first typed pointer that uses it. */
    private fun typeMalloc(obj: MemObj, elem: CType) {
        val count = (obj.untypedBytes / elem.bytes()).toInt()
        val es = elem.slots()
        obj.slots = arrayOfNulls(count * es)
        if (obj.zeroWhenTyped) for (k in 0 until count) fillZero(obj.slots, k * es, elem)
        obj.elemSlots = es
        obj.elemType = elem
        obj.untypedBytes = -1
    }

    private fun round(v: Double, t: CType): Double = if (t is CType.Num && t.kind == NumKind.FLOAT) v.toFloat().toDouble() else v

    private fun bool(b: Boolean) = if (b) 1L else 0L

    internal fun truthy(v: Any): Boolean = when (v) {
        is Long -> v != 0L
        is Double -> v != 0.0
        is CPtr -> v.obj != null
        else -> false
    }

    internal fun fail(line: Int, reason: String): Nothing = throw CProgramException(line, reason, isCompile = false)

    private companion object {
        const val NORMAL = 0
        const val BREAK = 1
        const val CONTINUE = 2
        const val RETURN = 3
        const val GOTO = 4
        const val MAX_DEPTH = 3000
        const val MAX_SLOTS = 8_000_000L
    }
}
