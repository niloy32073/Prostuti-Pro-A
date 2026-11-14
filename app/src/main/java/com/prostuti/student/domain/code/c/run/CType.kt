package com.prostuti.student.domain.code.c.run

/** A C type, as far as the runner supports them. Enums are plain `int`. */
sealed interface CType {
    data class Num(val kind: NumKind, val unsigned: Boolean = false) : CType {
        override fun toString() = (if (unsigned) "unsigned " else "") + kind.cName
    }

    data object Void : CType {
        override fun toString() = "void"
    }

    data class Ptr(val to: CType) : CType {
        override fun toString() = if (to is Func) "${to.ret} (*)(${to.paramList()})" else "$to *"
    }

    /** [size] is null when only known while running (`int a[n]`) or unknown (`extern int a[]`). */
    data class Arr(val of: CType, val size: Int?) : CType {
        override fun toString() = "$of[${size ?: ""}]"
    }

    /** A struct or union; two struct types are the same only when they share one [def]. */
    data class Struct(val def: StructDef) : CType {
        override fun toString() = def.toString()
    }

    /** A function's type. [checkArgs] is false for an old-style `int f()` (any arguments). */
    data class Func(val ret: CType, val params: List<CType>, val checkArgs: Boolean) : CType {
        internal fun paramList() = if (!checkArgs) "" else if (params.isEmpty()) "void" else params.joinToString(", ")
        override fun toString() = "$ret (${paramList()})"
    }

    companion object {
        val INT = Num(NumKind.INT)
        val UINT = Num(NumKind.INT, unsigned = true)
        val LONG = Num(NumKind.LONG)
        val ULONG = Num(NumKind.LONG, unsigned = true)
        val CHAR = Num(NumKind.CHAR)
        val DOUBLE = Num(NumKind.DOUBLE)
        val FLOAT = Num(NumKind.FLOAT)
        val CHAR_PTR = Ptr(CHAR)
        val VOID_PTR = Ptr(Void)
    }
}

/** One member of a struct or union; [offset] is in memory slots (0 for every union member). */
class StructMember(val name: String, val type: CType, val offset: Int)

/**
 * A struct or union definition. It is created when its tag is first seen, so a struct can point
 * to itself (`struct Node *next`); [members] are filled in when its `{ … }` is read.
 */
class StructDef(val tag: String?, val isUnion: Boolean) {
    var members: List<StructMember> = emptyList()
        private set
    var complete = false
        private set
    /** Set by `typedef struct { … } Name;` so messages can name an untagged struct. */
    var alias: String? = null
    var slots = 0
        private set
    var bytes = 0L
        private set
    var align = 1L
        private set

    fun complete(members: List<Pair<String, CType>>) {
        var off = 0
        var b = 0L
        var a = 1L
        val placed = members.map { (n, t) ->
            val m = StructMember(n, t, if (isUnion) 0 else off)
            val ma = t.align()
            a = maxOf(a, ma)
            if (isUnion) {
                off = maxOf(off, t.slots())
                b = maxOf(b, t.bytes())
            } else {
                off += t.slots()
                b = (b + ma - 1) / ma * ma + t.bytes()
            }
            m
        }
        this.members = placed
        slots = off
        align = a
        bytes = maxOf((b + a - 1) / a * a, 1L)
        complete = true
    }

    fun member(name: String): StructMember? = members.firstOrNull { it.name == name }

    override fun toString() = (if (isUnion) "union " else "struct ") + (tag ?: alias ?: "(unnamed)")
}

internal val CType.isArith get() = this is CType.Num
internal val CType.isInteger get() = this is CType.Num && !kind.isFloat
internal val CType.isFloating get() = this is CType.Num && kind.isFloat
internal val CType.isPtrLike get() = this is CType.Ptr || this is CType.Arr
internal val CType.isScalar get() = this is CType.Num || this is CType.Ptr

/** What an expression of this type is worth as a value: arrays and functions decay to pointers. */
internal val CType.decayed: CType
    get() = when (this) {
        is CType.Arr -> CType.Ptr(of)
        is CType.Func -> CType.Ptr(this)
        else -> this
    }

/** The element a pointer or array refers to. */
internal val CType.element: CType
    get() = when (this) {
        is CType.Ptr -> to
        is CType.Arr -> of
        else -> error("not a pointer: $this")
    }

/** Memory slots one value of this type takes (scalars take one; arrays need a known size). */
internal fun CType.slots(): Int = when (this) {
    is CType.Arr -> (size ?: 0) * of.slots()
    is CType.Struct -> def.slots
    else -> 1
}

/** `sizeof` in bytes, as on a 64-bit Android phone. */
internal fun CType.bytes(): Long = when (this) {
    is CType.Num -> kind.bytes.toLong()
    is CType.Ptr -> 8
    is CType.Arr -> (size ?: 0) * of.bytes()
    is CType.Struct -> def.bytes
    CType.Void, is CType.Func -> 1
}

/** Alignment in bytes (for struct padding, so `sizeof` matches gcc). */
internal fun CType.align(): Long = when (this) {
    is CType.Num -> kind.bytes.toLong()
    is CType.Ptr -> 8
    is CType.Arr -> of.align()
    is CType.Struct -> def.align
    CType.Void, is CType.Func -> 1
}

/** The scalar type at the bottom of (possibly nested) arrays. */
internal val CType.baseScalar: CType get() = if (this is CType.Arr) of.baseScalar else this

/** A struct or union whose members are not known yet (`struct Node;` or a self-reference). */
internal val CType.isIncomplete get() = this is CType.Struct && !def.complete
