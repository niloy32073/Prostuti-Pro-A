package com.prostuti.student.domain.code.c.run

/**
 * A typed C expression. The parser inserts every implicit conversion as a [Cast], so the
 * interpreter never has to work out types: a [Binary]'s operands already have its [Binary.opType].
 */
sealed class CExpr(val line: Int) {
    abstract val type: CType

    class IntLit(line: Int, val value: Long, override val type: CType) : CExpr(line)
    class FloatLit(line: Int, val value: Double, override val type: CType) : CExpr(line)

    /** [chars] ends with the terminating 0. */
    class StrLit(line: Int, val chars: IntArray) : CExpr(line) {
        override val type: CType = CType.Arr(CType.CHAR, chars.size)
    }

    class Var(line: Int, val sym: VarSym) : CExpr(line) {
        override val type: CType get() = sym.type
    }

    /** `-x`, `+x`, `!x`, `~x`. */
    class Unary(line: Int, val op: String, val operand: CExpr, override val type: CType) : CExpr(line)

    class AddrOf(line: Int, val operand: CExpr, override val type: CType) : CExpr(line)
    class Deref(line: Int, val operand: CExpr, override val type: CType) : CExpr(line)

    /** `++x`, `x--`, … on a number or a pointer. */
    class IncDec(line: Int, val target: CExpr, val delta: Int, val prefix: Boolean, override val type: CType) : CExpr(line)

    /** Arithmetic, bitwise and comparison operators; both operands are already of [opType]. */
    class Binary(line: Int, val op: String, val left: CExpr, val right: CExpr, val opType: CType, override val type: CType) : CExpr(line)

    /** `ptr + n` / `ptr - n` ([sign] -1); [scale] is the element size in memory slots. */
    class PtrAdd(line: Int, val ptr: CExpr, val offset: CExpr, val sign: Int, val scale: Int, override val type: CType) : CExpr(line)

    /** `p - q` in elements. */
    class PtrDiff(line: Int, val left: CExpr, val right: CExpr, val scale: Int) : CExpr(line) {
        override val type: CType = CType.LONG
    }

    /** `&&` / `||`. */
    class Logical(line: Int, val op: String, val left: CExpr, val right: CExpr) : CExpr(line) {
        override val type: CType = CType.INT
    }

    /** `=` ([op] null) or a compound assignment; for compound ones [value] is the right operand converted to [opType]. */
    class Assign(line: Int, val target: CExpr, val value: CExpr, val op: String?, val opType: CType) : CExpr(line) {
        override val type: CType get() = target.type
    }

    class Cond(line: Int, val cond: CExpr, val ifTrue: CExpr, val ifFalse: CExpr, override val type: CType) : CExpr(line)

    /** A call to the program's own function ([fn]) or a library function ([builtin]). */
    class Call(line: Int, val name: String, val fn: FuncSym?, val builtin: Builtin?, val args: List<CExpr>, override val type: CType) : CExpr(line)

    /**
     * `base[index]`. When [base] is an array of known size, [bound] is that size and [boundName]
     * names it, so `m[0][7]` or `s.marks[5]` is caught even inside one piece of memory.
     */
    class Index(
        line: Int,
        val base: CExpr,
        val index: CExpr,
        val scale: Int,
        override val type: CType,
        val bound: Int? = null,
        val boundName: String? = null,
    ) : CExpr(line)

    /** `s.m`, or `p->m` as `(*p).m`. [baseIsLvalue] false for a returned struct (`make().x`). */
    class Member(line: Int, val base: CExpr, val member: StructMember, val baseIsLvalue: Boolean) : CExpr(line) {
        override val type: CType get() = member.type
        val isUnion: Boolean get() = (base.type as CType.Struct).def.isUnion
    }

    /** A function used as a value (`op = add;`); it decays to a pointer to the function. */
    class FuncRef(line: Int, val fn: FuncSym?, val builtin: Builtin?, override val type: CType.Func) : CExpr(line)

    /** A call through a function pointer (`op(a, b)`, `(*op)(a, b)`, `ops[i](a, b)`). */
    class CallPtr(line: Int, val callee: CExpr, val args: List<CExpr>, override val type: CType) : CExpr(line)
    class Cast(line: Int, val operand: CExpr, override val type: CType) : CExpr(line)
    class Comma(line: Int, val left: CExpr, val right: CExpr) : CExpr(line) {
        override val type: CType get() = right.type
    }
}
