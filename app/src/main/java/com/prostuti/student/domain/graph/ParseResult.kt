package com.prostuti.student.domain.graph

sealed interface ParseResult {
    data class Success(val statement: MathStatement) : ParseResult
    data class Failure(val error: GraphError) : ParseResult
}
