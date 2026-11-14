package com.prostuti.student.ui.screen.playground.code.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.ui.screen.playground.code.CodeExample
import com.prostuti.student.ui.screen.playground.code.CodeExamples
import com.prostuti.student.ui.theme.ProstutiTypography

/** The example programs; picking one loads it in the current language. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeExamplesSheet(onPick: (CodeExample) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.testTag("code_examples_sheet")) {
        Text(
            stringResource(R.string.code_examples_title),
            style = ProstutiTypography.bnTitleMedium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        LazyColumn(state = rememberLazyListState(), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(CodeExamples.all, key = { it.titleRes }) { example ->
                Text(
                    stringResource(example.titleRes),
                    style = ProstutiTypography.bnBodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(example) }
                        .testTag("code_example_${example.titleRes}")
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                )
            }
        }
    }
}
