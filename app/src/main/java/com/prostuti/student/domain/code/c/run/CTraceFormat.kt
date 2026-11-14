package com.prostuti.student.domain.code.c.run

import com.prostuti.student.domain.code.run.TraceVar
import java.math.BigDecimal
import java.math.MathContext

/**
 * Turns C memory into [TraceVar]s for the variables panel: numbers as C prints them, chars with
 * their character, char arrays as text, arrays and structs with their elements/members (up to
 * [MAX_ITEMS] each, [MAX_DEPTH] levels deep), pointers as what they point at (`→ a[2]`).
 * [functionName] names the function a function pointer points to (null if it is not one).
 */
internal class CTraceFormat(private val functionName: (MemObj) -> String?) {

    fun describe(name: String, type: CType, obj: MemObj, idx: Int, depth: Int = 0): TraceVar = when (type) {
        is CType.Arr -> array(name, type, obj, idx, depth)
        is CType.Struct -> struct(name, type, obj, idx, depth)
        else -> TraceVar(name, scalar(type, obj, idx))
    }

    private fun array(name: String, type: CType.Arr, obj: MemObj, idx: Int, depth: Int): TraceVar {
        val es = maxOf(type.of.slots(), 1)
        val count = type.size ?: ((obj.slots.size - idx) / es)
        if ((type.of as? CType.Num)?.kind == NumKind.CHAR) return TraceVar(name, charText(obj, idx, count))
        val shown = minOf(count, MAX_ITEMS)
        val children = if (depth >= MAX_DEPTH) emptyList() else (0 until shown).map { describe("[$it]", type.of, obj, idx + it * es, depth + 1) }
        return TraceVar(name, summary(children, count, named = false), children)
    }

    private fun struct(name: String, type: CType.Struct, obj: MemObj, idx: Int, depth: Int): TraceVar {
        val members = type.def.members
        val children = if (depth >= MAX_DEPTH) emptyList() else members.map { m ->
            val v = describe(m.name, m.type, obj, idx + m.offset, depth + 1)
            // A union shows only the member it holds now.
            if (type.def.isUnion && !fits(m.type, obj.slots.getOrNull(idx + m.offset))) v.copy(value = null, children = emptyList()) else v
        }
        return TraceVar(name, summary(children, members.size, named = true), children)
    }

    private fun fits(t: CType, v: Any?): Boolean = when {
        v == null -> true
        t is CType.Num -> if (t.kind.isFloat) v is Double else v is Long
        t is CType.Ptr -> v is CPtr
        else -> true
    }

    /** `{1, 2, 3}` / `{x: 1, y: 2}`, cut short when long. */
    private fun summary(children: List<TraceVar>, count: Int, named: Boolean): String? {
        if (children.isNotEmpty() && children.all { it.value == null }) return null
        val sb = StringBuilder("{")
        for ((i, c) in children.withIndex()) {
            if (i > 0) sb.append(", ")
            if (named) sb.append(c.name).append(": ")
            sb.append(c.value ?: "?")
            if (sb.length > MAX_SUMMARY) { sb.append(", …"); break }
        }
        if (children.size < count && !sb.endsWith("…")) sb.append(", …")
        return sb.append('}').toString()
    }

    private fun scalar(type: CType, obj: MemObj, idx: Int): String? {
        if (obj.freed) return "(freed)"
        val v = obj.slots.getOrNull(idx) ?: return null
        return when (v) {
            is CPtr -> pointer(v, type)
            is Double -> number(v, (type as? CType.Num)?.kind == NumKind.FLOAT)
            is Long -> if ((type as? CType.Num)?.kind == NumKind.CHAR) "$v ${charLiteral(v)}" else v.toString()
            else -> v.toString()
        }
    }

    private fun pointer(p: CPtr, type: CType): String {
        val obj = p.obj ?: return "NULL"
        functionName(obj)?.let { return "→ $it()" }
        if (obj.freed) return "→ (freed memory)"
        if (obj.name == "string literal") return "→ " + charText(obj, p.index, obj.slots.size - p.index)
        val es = maxOf(obj.elemSlots, 1)
        val head = if (obj.slots.size == es && p.index in 0 until es) obj.name else "${obj.name}[${Math.floorDiv(p.index, es)}]"
        val et = obj.elemType
        val target = "→ " + head + (if (et != null && p.index >= 0) slotPath(et, p.index % es) else "")
        val pointsToChar = ((type as? CType.Ptr)?.to as? CType.Num)?.kind == NumKind.CHAR
        return if (pointsToChar && p.index in obj.slots.indices) target + " " + charText(obj, p.index, obj.slots.size - p.index) else target
    }

    /** Characters up to the terminating 0, quoted; null when the first one has no value yet. */
    private fun charText(obj: MemObj, idx: Int, count: Int): String? {
        if (obj.freed) return "(freed)"
        if (obj.slots.getOrNull(idx) == null) return null
        val sb = StringBuilder("\"")
        for (k in 0 until count) {
            val c = obj.slots.getOrNull(idx + k) as? Long ?: break
            if (c == 0L) break
            if (sb.length > MAX_SUMMARY) { sb.append("…"); break }
            sb.append(escape(c))
        }
        return sb.append('"').toString()
    }

    private fun charLiteral(c: Long) = "'" + escape(c) + "'"

    private fun escape(c: Long): String = when (c) {
        10L -> "\\n"
        9L -> "\\t"
        13L -> "\\r"
        0L -> "\\0"
        in 32L..126L -> c.toInt().toChar().toString()
        else -> "\\x" + (c and 0xFF).toString(16)
    }

    private fun number(v: Double, isFloat: Boolean): String {
        if (v.isNaN()) return "nan"
        if (v.isInfinite()) return if (v > 0) "inf" else "-inf"
        if (v == 0.0) return "0.0"
        val d = BigDecimal(v).round(MathContext(if (isFloat) 7 else 10)).stripTrailingZeros()
        val abs = kotlin.math.abs(v)
        val text = if (abs >= 1e-5 && abs < 1e15) d.toPlainString() else d.toString()
        return if ('.' in text || 'E' in text) text else "$text.0"
    }

    companion object {
        const val MAX_ITEMS = 50
        const val MAX_DEPTH = 3
        const val MAX_SUMMARY = 60
    }
}

/** `.y`, `[2]`, `.marks[1]`: where slot [off] is inside a value of [type]. */
internal fun slotPath(type: CType, off: Int): String = when (type) {
    is CType.Struct -> type.def.members.firstOrNull { off >= it.offset && off < it.offset + it.type.slots() }
        ?.let { ".${it.name}" + slotPath(it.type, off - it.offset) } ?: ""
    is CType.Arr -> {
        val es = maxOf(type.of.slots(), 1)
        "[${off / es}]" + slotPath(type.of, off % es)
    }
    else -> ""
}
