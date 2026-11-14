package com.prostuti.student.domain.code.c.run

sealed class CStmt(val line: Int) {
    class Expr(line: Int, val expr: CExpr) : CStmt(line)

    /** [sizeExpr] is the run-time size of a variable-length array (`int a[n]`). */
    class Decl(line: Int, val sym: VarSym, val init: CInit?, val sizeExpr: CExpr?) : CStmt(line)
    class Block(line: Int, val stmts: List<CStmt>) : CStmt(line)
    class If(line: Int, val cond: CExpr, val then: CStmt, val otherwise: CStmt?) : CStmt(line)
    class While(line: Int, val cond: CExpr, val body: CStmt) : CStmt(line)
    class DoWhile(line: Int, val body: CStmt, val cond: CExpr) : CStmt(line)
    class For(line: Int, val init: CStmt?, val cond: CExpr?, val step: CExpr?, val body: CStmt) : CStmt(line)

    /** [body] is the switch's statement list; [cases] maps a case value to where it starts in it. */
    class Switch(line: Int, val subject: CExpr, val body: List<CStmt>, val cases: Map<Long, Int>, val default: Int?) : CStmt(line)
    class Break(line: Int) : CStmt(line)
    class Continue(line: Int) : CStmt(line)
    class Return(line: Int, val value: CExpr?) : CStmt(line)
    class Goto(line: Int, val label: String) : CStmt(line)

    /** `label: stmt`, a target of [Goto]. */
    class Labeled(line: Int, val label: String, val stmt: CStmt) : CStmt(line)
}
