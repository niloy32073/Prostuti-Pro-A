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
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.ui.screen.playground.graph.GraphExample
import com.prostuti.student.ui.screen.playground.graph.GraphExampleCategory
import com.prostuti.student.ui.screen.playground.graph.GraphExamples
import com.prostuti.student.ui.screen.playground.graph.PhysicsTemplates
import com.prostuti.student.ui.theme.MonoFontFamily
import com.prostuti.student.ui.theme.ProstutiTypography

/**
 * Two tabs: maths examples (added to the list) and physics templates (which bring their own
 * sliders and note). [onPick] gets the example, its translated note, and whether it is a template.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamplesSheet(
    onPick: (example: GraphExample, note: String?, isTemplate: Boolean) -> Unit,
    onDismiss: () -> Unit,
    categories: List<GraphExampleCategory> = GraphExamples.categories,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.testTag("examples_sheet")) {
        Text(
            stringResource(R.string.graph_examples_title),
            style = ProstutiTypography.bnTitleMedium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        PrimaryTabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, modifier = Modifier.testTag("tab_math"), text = { Text(stringResource(R.string.graph_tab_math)) })
            Tab(selected = tab == 1, onClick = { tab = 1 }, modifier = Modifier.testTag("tab_physics"), text = { Text(stringResource(R.string.graph_tab_physics)) })
        }
        val shown = if (tab == 0) categories else PhysicsTemplates.categories
        LazyColumn(state = rememberLazyListState(), contentPadding = PaddingValues(bottom = 24.dp)) {
            for (category in shown) {
                item(key = "c${category.titleRes}") {
                    Text(
                        stringResource(category.titleRes),
                        style = ProstutiTypography.bnLabelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
                    )
                }
                items(category.examples, key = { it.labelRes }) { example ->
                    val note = example.noteRes?.let { stringResource(it) }
                    val isTemplate = tab == 1
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(example, note, isTemplate) }
                            .testTag("example_${example.labelRes}")
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                    ) {
                        Text(stringResource(example.labelRes), style = ProstutiTypography.bnBodyLarge)
                        if (note != null) {
                            Text(
                                note, style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2, overflow = TextOverflow.Ellipsis,
                            )
                        } else {
                            Text(
                                example.lines.joinToString("    "),
                                fontFamily = MonoFontFamily,
                                style = ProstutiTypography.bnBodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                }
            }
        }
    }
}
