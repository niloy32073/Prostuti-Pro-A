package com.prostuti.student.domain.graph

/** What a line is, for its icon/label and which settings its card offers. */
enum class GraphLineKind {
    EMPTY, ERROR, VALUE, PARAMETER, FUNCTION_DEFINITION,
    EXPLICIT, INEQUALITY, POLAR, PARAMETRIC, IMPLICIT, REGION, POINT, INTEGRAL, VECTOR,
    SURFACE, IMPLICIT_SURFACE, PARAMETRIC_SURFACE, SPACE_CURVE, VOLUME, REVOLUTION,
    /** A `# …` line: words for the reader, never drawn. */
    NOTE,
    TABLE, VECTOR_FIELD,
}
