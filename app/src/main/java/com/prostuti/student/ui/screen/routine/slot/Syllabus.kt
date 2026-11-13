package com.prostuti.student.ui.screen.routine.slot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.data.model.response.RoutineSlot
import com.prostuti.student.domain.routine.TopicLabel
import com.prostuti.student.domain.routine.TopicOutline
import com.prostuti.student.ui.screen.routine.labelRes
import com.prostuti.student.ui.screen.routine.routineDateText
import com.prostuti.student.ui.screen.routine.topicLabelText
import com.prostuti.student.ui.screen.routine.weekday
import com.prostuti.student.ui.theme.ProstutiTypography
import com.prostuti.student.ui.component.localized
import com.prostuti.student.ui.component.appLanguage

const val TAG_PREP_CARD = "prep_card"
const val TAG_PREP_CONCEPTS = "prep_concepts"
const val TAG_SYLLABUS_CHAPTER_PREFIX = "syllabus_chapter_"

/**
 * An exam's syllabus, grouped by subject: a chapter the exam covers whole shows just its name,
 * a partly covered one lists the topics that are in. Without names, the short label instead.
 */
@Composable
fun SyllabusList(label: TopicLabel?, topicCount: Int, modifier: Modifier = Modifier) {
    val outline = label?.outline
    if (outline == null) {
        Text(topicLabelText(label ?: TopicLabel.Count(topicCount)), style = ProstutiTypography.bnBodyMedium,
            color = MaterialTheme.colorScheme.onSurface, modifier = modifier)
        return
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        outline.sections.groupBy { it.subject }.forEach { (subject, sections) ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                subject?.let { Text(it.localized(), style = ProstutiTypography.bnLabelLarge, color = MaterialTheme.colorScheme.primary) }
                sections.forEachIndexed { i, section -> SyllabusRow(section, Modifier.testTag(TAG_SYLLABUS_CHAPTER_PREFIX + i)) }
            }
        }
    }
}

@Composable
private fun SyllabusRow(section: TopicOutline.Section, modifier: Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.padding(top = 8.dp).size(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val chapter = section.chapter
            if (chapter != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(chapter.localized(), style = ProstutiTypography.bnBodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f, fill = false))
                    ScopeChip(stringResource(if (section.isPartial) R.string.syllabus_partial else R.string.syllabus_whole), section.isPartial)
                }
            }
            if (section.topics.isNotEmpty()) {
                val language = appLanguage()
                Text(section.topics.joinToString(", ") { it.inLanguage(language) }, style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ScopeChip(text: String, partial: Boolean) {
    val bg = if (partial) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val fg = if (partial) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
    Text(text, style = ProstutiTypography.bnLabelMedium, color = fg,
        modifier = Modifier.background(bg, RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 2.dp))
}

/**
 * "Get ready for the next exam" (GROUP_ROUTINE_PLAN.md §6.11 / D8), for every member: when, what to
 * study, and a way into concepts (not in the app yet, so [onConcepts] shows "coming soon").
 */
@Composable
fun PrepareCard(slot: RoutineSlot, label: TopicLabel?, onConcepts: () -> Unit, modifier: Modifier = Modifier, groupName: String? = null) {
    Column(
        modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(16.dp))
            .padding(16.dp).testTag(TAG_PREP_CARD),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(stringResource(R.string.prep_title), style = ProstutiTypography.bnTitleMedium, color = MaterialTheme.colorScheme.onSurface)
        val whenText = listOfNotNull(
            slot.date?.let { "${stringResource(it.weekday().labelRes())}, ${routineDateText(it)}" } ?: slot.slotDate,
            slotWindowText(slot), groupName,
        ).joinToString(" · ")
        Text(whenText, style = ProstutiTypography.bnLabelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        slot.title?.let { Text(it, style = ProstutiTypography.bnBodyLarge, color = MaterialTheme.colorScheme.onSurface) }
        Text(stringResource(R.string.prep_study), style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SyllabusList(label, slot.topicIds.size)
        FilledTonalButton(onClick = onConcepts, modifier = Modifier.testTag(TAG_PREP_CONCEPTS)) {
            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.routine_tab_concept), style = ProstutiTypography.bnLabelLarge, modifier = Modifier.padding(start = 8.dp))
        }
    }
}
