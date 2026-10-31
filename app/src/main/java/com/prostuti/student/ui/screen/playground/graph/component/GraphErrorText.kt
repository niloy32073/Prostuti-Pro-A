package com.prostuti.student.ui.screen.playground.graph.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.prostuti.student.R
import com.prostuti.student.domain.graph.GraphError
import com.prostuti.student.domain.graph.GraphErrorKind

/** A student-friendly sentence for each compiler error. */
@Composable
fun graphErrorText(error: GraphError): String {
    val names = error.names.joinToString(", ")
    return when (error.kind) {
        GraphErrorKind.SYNTAX -> stringResource(R.string.graph_error_syntax)
        GraphErrorKind.UNBALANCED_BRACKETS -> stringResource(R.string.graph_error_brackets)
        GraphErrorKind.UNKNOWN_CHARACTER -> stringResource(R.string.graph_error_character, names)
        GraphErrorKind.EMPTY_ARGUMENT -> stringResource(R.string.graph_error_empty_argument)
        GraphErrorKind.WRONG_ARGUMENT_COUNT -> stringResource(R.string.graph_error_argument_count)
        GraphErrorKind.MISSING_INTEGRAL_DIFFERENTIAL -> stringResource(R.string.graph_error_differential)
        GraphErrorKind.TUPLE_NOT_ALLOWED -> stringResource(R.string.graph_error_tuple)
        GraphErrorKind.UNDEFINED_VARIABLE -> stringResource(R.string.graph_error_undefined, names)
        GraphErrorKind.DUPLICATE_DEFINITION -> stringResource(R.string.graph_error_duplicate, names)
        GraphErrorKind.CIRCULAR_DEFINITION -> stringResource(R.string.graph_error_circular, names)
        GraphErrorKind.BROKEN_DEPENDENCY -> stringResource(R.string.graph_error_broken, names)
        GraphErrorKind.INVALID_DEFINITION -> stringResource(R.string.graph_error_invalid_definition)
        GraphErrorKind.RESERVED_NAME -> stringResource(R.string.graph_error_reserved, names)
        GraphErrorKind.UNSUPPORTED_EQUATION -> stringResource(R.string.graph_error_unsupported)
        GraphErrorKind.NEEDS_3D -> stringResource(R.string.graph_error_3d)
        GraphErrorKind.TOO_COMPLEX -> stringResource(R.string.graph_error_complex)
    }
}
