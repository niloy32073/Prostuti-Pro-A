package com.prostuti.student.domain.code.c.run

/**
 * A variable. Locals have an [index] into their function's frame; globals (and `static` locals)
 * into the program's global table.
 */
class VarSym(val name: String, val type: CType, val line: Int, val isGlobal: Boolean, val index: Int) {
    /** The last line of the block it is declared in (a trace shows it only from [line] to here). */
    var scopeEnd = Int.MAX_VALUE
}

/** A function written in the program. [body] is set once its definition is read. */
class FuncSym(val name: String, val ret: CType, val params: List<VarSym>, val checkArgs: Boolean, val line: Int) {
    var body: CStmt.Block? = null
    var localCount = 0
    /** Every variable declared in the body (static ones too), in order; for traces. */
    val locals = mutableListOf<VarSym>()
}
