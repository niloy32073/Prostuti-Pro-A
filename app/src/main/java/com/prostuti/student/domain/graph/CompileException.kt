package com.prostuti.student.domain.graph

/** Raised while compiling a line; [GraphCompiler] turns it into the line's [GraphError]. */
internal class CompileException(val error: GraphError) : Exception(error.kind.name)
