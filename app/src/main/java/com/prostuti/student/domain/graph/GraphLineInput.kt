package com.prostuti.student.domain.graph

/** One line of the list: an expression, or a data [table] (then [text] is ignored). */
data class GraphLineInput(val id: Long, val text: String, val table: TableInput? = null)
