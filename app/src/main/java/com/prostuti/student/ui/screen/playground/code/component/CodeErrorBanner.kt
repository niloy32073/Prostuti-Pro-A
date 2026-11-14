package com.prostuti.student.ui.screen.playground.code.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.domain.code.CodeError
import com.prostuti.student.domain.code.CodeErrorKind
import com.prostuti.student.ui.theme.ProstutiTypography

/** The first syntax problem, in words; a tap jumps to its line. */
@Composable
fun CodeErrorBanner(error: CodeError, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .clickable(onClick = onClick)
            .testTag("code_error")
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
        Spacer(Modifier.width(10.dp))
        Text(codeErrorMessage(error), style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

@Composable
fun codeErrorMessage(error: CodeError): String {
    val line = error.line
    return when (error.kind) {
        CodeErrorKind.UNCLOSED_BRACKET -> stringResource(R.string.code_err_unclosed_bracket, line, error.detail)
        CodeErrorKind.UNEXPECTED_CLOSE -> stringResource(R.string.code_err_unexpected_close, line, error.detail)
        CodeErrorKind.UNTERMINATED_STRING -> stringResource(R.string.code_err_unterminated_string, line)
        CodeErrorKind.UNTERMINATED_COMMENT -> stringResource(R.string.code_err_unterminated_comment, line)
        CodeErrorKind.MISSING_SEMICOLON -> stringResource(R.string.code_err_missing_semicolon, line)
        CodeErrorKind.UNEXPECTED_INDENT -> stringResource(R.string.code_err_unexpected_indent, line)
        CodeErrorKind.BAD_DEDENT -> stringResource(R.string.code_err_bad_dedent, line)
        CodeErrorKind.EXPECTED_BLOCK -> stringResource(R.string.code_err_expected_block, line)
        CodeErrorKind.EXPECTED -> {
            val what = when (error.detail) {
                "condition" -> stringResource(R.string.code_err_word_condition)
                "statement" -> stringResource(R.string.code_err_word_statement)
                "name", "name(" -> stringResource(R.string.code_err_word_name)
                else -> "‘${error.detail}’"
            }
            stringResource(R.string.code_err_expected, line, what)
        }
        CodeErrorKind.MISPLACED -> when (error.detail) {
            "break", "continue" -> stringResource(R.string.code_err_misplaced_jump, line, error.detail)
            "return" -> stringResource(R.string.code_err_misplaced_return, line)
            else -> stringResource(R.string.code_err_misplaced, line, error.detail)
        }
    }
}
