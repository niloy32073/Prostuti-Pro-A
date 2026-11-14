package com.prostuti.student.domain.code.c.run

/**
 * A checked program, ready to run. [globalInits] run once, in order, before `main`. [globals] are
 * the global variables (not `static` locals), for traces.
 */
class CProgram(val main: FuncSym, val globalCount: Int, val globalInits: List<CStmt.Decl>, val globals: List<VarSym> = emptyList())
