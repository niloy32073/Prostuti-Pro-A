package com.prostuti.student.domain.code

/** Thrown inside the parsers; [com.prostuti.student.domain.BuildCodeFlowUseCase] turns it into a result. */
class CodeParseException(val error: CodeError) : Exception("${error.kind} at line ${error.line}: ${error.detail}")
