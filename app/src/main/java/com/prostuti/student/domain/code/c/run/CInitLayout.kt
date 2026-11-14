package com.prostuti.student.domain.code.c.run

/** An initializer as written, before it is matched to the object's type. */
internal sealed interface RawInit {
    val line: Int

    class Expr(val expr: CExpr) : RawInit {
        override val line get() = expr.line
    }

    /** A string literal: fills a `char` array, or is a `char *` value anywhere else. */
    class Str(val lit: CExpr.StrLit) : RawInit {
        override val line get() = lit.line
    }

    class Braced(val items: List<RawInit>, override val line: Int) : RawInit

    /** `.name = value` inside a struct's braces. */
    class Designated(val member: String, val init: RawInit, override val line: Int) : RawInit
}

/**
 * Matches an initializer to the memory slots of an object, following C's rules: braces start a
 * new sub-object (array element or struct member), plain values fill the next scalar (brace
 * elision), a string fills a `char` array, `.m =` picks a struct member, a union takes its first
 * member, and anything not given is zero. [convert] converts a value to the scalar (or struct)
 * type of the slot it lands in, reporting a mismatch as a compile error.
 */
internal class CInitLayout(private val convert: (CExpr, CType) -> CExpr) {

    private val slots = ArrayList<Int>()
    private val values = ArrayList<Any>()

    /** The laid-out initializer and how many slots it covers (to size `int a[] = {…}`). */
    fun layout(type: CType, raw: RawInit): Pair<CInit, Int> {
        when (raw) {
            is RawInit.Braced -> {
                val used = braced(type, raw, 0)
                return placed() to used
            }
            is RawInit.Str -> if (isCharArray(type)) {
                val used = string(type as CType.Arr, raw, 0)
                return placed() to used
            }
            is RawInit.Designated -> fail(raw.line, "'.${raw.member} =' only works inside a struct's { }")
            is RawInit.Expr -> Unit
        }
        if (type is CType.Arr) fail(raw.line, "an array is given its values in { … }, e.g. {1, 2, 3}")
        val expr = if (raw is RawInit.Str) raw.lit else (raw as RawInit.Expr).expr
        return CInit.Single(convert(expr, type)) to type.slots()
    }

    private fun placed() = CInit.Placed(slots.toIntArray(), values.toList())

    private fun place(slot: Int, value: Any) {
        slots += slot
        values += value
    }

    /** `{ … }` for an object of [type] at [start]; returns the slots it covers. */
    private fun braced(type: CType, b: RawInit.Braced, start: Int): Int {
        val items = b.items
        when (type) {
            is CType.Arr -> {
                if (isCharArray(type) && items.size == 1 && items[0] is RawInit.Str) return string(type, items[0] as RawInit.Str, start)
                val es = type.of.slots()
                var i = 0
                var k = 0
                while (i < items.size) {
                    if (type.size != null && k >= type.size) tooMany(items[i].line, "an array of ${type.size}")
                    i = consume(type.of, items, i, start + k * es)
                    k++
                }
                return k * es
            }
            is CType.Struct -> {
                val def = type.def
                if (!def.complete) fail(b.line, "$type has no members yet, so it cannot be initialized")
                var i = 0
                var m = 0
                while (i < items.size) {
                    var item = items[i]
                    if (item is RawInit.Designated) {
                        m = def.members.indexOfFirst { it.name == item.member }
                        if (m < 0) fail(item.line, "$type has no member '${item.member}'")
                        item = item.init
                    } else if (m >= def.members.size || (def.isUnion && m >= 1)) {
                        tooMany(item.line, type.toString())
                    }
                    val member = def.members[m]
                    i = consume(member.type, items, i, start + member.offset, first = item)
                    m++
                }
                return def.slots
            }
            else -> {
                // `int x = {5};` is allowed for a scalar.
                if (items.size > 1) tooMany(items[1].line, "one value")
                if (items.isNotEmpty()) consume(type, items, 0, start)
                return 1
            }
        }
    }

    /**
     * Fills the object of [type] at [start] from `items[i…]` and returns the index after what it
     * used. [first] replaces `items[i]` (a designator's value).
     */
    private fun consume(type: CType, items: List<RawInit>, i: Int, start: Int, first: RawInit = items[i]): Int {
        val item = first
        if (item is RawInit.Designated) fail(item.line, "'.${item.member} =' only works for a struct member")
        if (item is RawInit.Braced) { braced(type, item, start); return i + 1 }
        if (item is RawInit.Str && isCharArray(type)) { string(type as CType.Arr, item, start); return i + 1 }
        val expr = if (item is RawInit.Str) item.lit else (item as RawInit.Expr).expr
        if (type is CType.Struct && expr.type == type) { place(start, convert(expr, type)); return i + 1 }
        // A plain value for an array or struct fills its first scalars (brace elision).
        var k = i
        when (type) {
            is CType.Arr -> {
                val es = type.of.slots()
                for (e in 0 until (type.size ?: 0)) {
                    if (k >= items.size || (k > i && items[k] is RawInit.Designated)) break
                    k = consume(type.of, items, k, start + e * es, first = if (k == i) item else items[k])
                }
                return k
            }
            is CType.Struct -> {
                val members = if (type.def.isUnion) type.def.members.take(1) else type.def.members
                for (m in members) {
                    if (k >= items.size || (k > i && items[k] is RawInit.Designated)) break
                    k = consume(m.type, items, k, start + m.offset, first = if (k == i) item else items[k])
                }
                return k
            }
            else -> {
                place(start, convert(expr, type))
                return i + 1
            }
        }
    }

    private fun string(type: CType.Arr, s: RawInit.Str, start: Int): Int {
        val chars = s.lit.chars
        if (type.size != null && chars.size - 1 > type.size) fail(s.line, "the text is longer than the array (${type.size} characters)")
        val count = if (type.size == null) chars.size else minOf(type.size, chars.size)
        for (k in 0 until count) place(start + k, chars[k].toLong())
        return count
    }

    private fun isCharArray(t: CType) = t is CType.Arr && (t.of as? CType.Num)?.kind == NumKind.CHAR

    private fun tooMany(line: Int, what: String): Nothing = fail(line, "too many values for $what")

    private fun fail(line: Int, reason: String): Nothing = throw CProgramException(line, reason, isCompile = true)
}
