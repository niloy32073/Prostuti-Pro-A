package com.prostuti.student.ui.screen.playground.graph.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.ui.theme.ProstutiTypography
import com.prostuti.student.ui.screen.playground.graph.GraphExample
import com.prostuti.student.ui.screen.playground.graph.GraphExamples
import com.prostuti.student.ui.theme.MonoFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamplesSheet(onPick: (GraphExample) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.testTag("examples_sheet")) {
        Text(
            stringResource(R.string.graph_examples_title),
            style = ProstutiTypography.bnTitleMedium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        LazyColumn(state = rememberLazyListState(), contentPadding = PaddingValues(bottom = 24.dp)) {
            for (category in GraphExamples.categories) {
                item(key = category.titleRes) {
                    Text(
                        stringResource(category.titleRes),
                        style = ProstutiTypography.bnLabelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
                    )
                }
                items(category.examples, key = { it.labelRes }) { example ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(example) }
                            .testTag("example_${example.labelRes}")
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                    ) {
                        Text(stringResource(example.labelRes), style = ProstutiTypography.bnBodyLarge)
                        Text(
                            example.lines.joinToString("    "),
                            fontFamily = MonoFontFamily,
                            style = ProstutiTypography.bnBodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                }
            }
        }
    }
}
