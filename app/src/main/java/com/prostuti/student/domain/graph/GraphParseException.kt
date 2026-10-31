package com.prostuti.student.domain.graph

/** Thrown inside the tokenizer/parser only; [MathParser.parse] turns it into a [GraphError]. */
internal class GraphParseException(val error: GraphError) : Exception(error.kind.name)
