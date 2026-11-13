package com.prostuti.student.ui.screen.routine.slot

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.prostuti.student.R
import com.prostuti.student.data.model.enums.SlotKind
import com.prostuti.student.data.model.enums.SlotMyStatus
import com.prostuti.student.data.model.enums.TopicGroupingLevel
import com.prostuti.student.data.model.response.RoutineSlot
import com.prostuti.student.data.model.response.TopicGrouping
import com.prostuti.student.domain.routine.TopicLabel
import com.prostuti.student.domain.routine.TopicOutline
import com.prostuti.student.ui.component.localizedNumber
import com.prostuti.student.ui.theme.ProstutiTheme
import androidx.compose.ui.res.stringResource
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.prostuti.student.data.model.response.CurriculumName

/** The next exam's syllabus for every member: whole chapters by name, partial ones with their topics. */
@RunWith(AndroidJUnit4::class)
class PrepareCardTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val slot = RoutineSlot(
        id = "s1", kind = SlotKind.ROUTINE, groupId = "g1", slotDate = "2026-11-12", opensAt = "2026-11-12T13:00:00",
        closesAt = "2026-11-12T17:00:00", topicIds = listOf("v1", "v2", "w1"),
        topicGrouping = TopicGrouping(TopicGroupingLevel.CHAPTERS, "phy"),
        questionsCount = 25, durationMinutes = 25, markPerQuestion = 1.0, negativeMarkValue = 0.25, myStatus = SlotMyStatus.UPCOMING,
    )
    private val phyName = CurriculumName("পদার্থবিজ্ঞান", "Physics")
    private val vecName = CurriculumName("ভেক্টর", "Vector")
    private val worldName = CurriculumName("ভৌত জগৎ", "Physical world")
    private val v1Name = CurriculumName("ভেক্টরের যোগ", "Vector addition")
    private val v2Name = CurriculumName("ডট গুণন", "Dot product")
    private fun CurriculumName.shown() = inLanguage(context.resources.configuration.locales[0].language)

    private val label = TopicLabel.Chapters(
        phyName, listOf(vecName, worldName), 0,
        TopicOutline(listOf(
            TopicOutline.Section(phyName, vecName, listOf(v1Name, v2Name), isPartial = true),
            TopicOutline.Section(phyName, worldName, emptyList(), isPartial = false),
        )),
    )

    @Test
    fun showsChaptersWholeOrWithTopicsAndConceptsSaysComingSoon() {
        var concepts = false
        composeTestRule.setContent { ProstutiTheme { PrepareCard(slot, label, onConcepts = { concepts = true }) } }

        composeTestRule.onNodeWithText(context.getString(R.string.prep_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(vecName.shown()).assertIsDisplayed()
        composeTestRule.onNodeWithText("${v1Name.shown()}, ${v2Name.shown()}").assertIsDisplayed()
        composeTestRule.onNodeWithText(worldName.shown()).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.syllabus_whole)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.syllabus_partial)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_PREP_CONCEPTS).performClick()
        assertTrue(concepts)
    }

    @Test
    fun withoutNamesItFallsBackToTheCount() {
        var expected = ""
        composeTestRule.setContent {
            ProstutiTheme {
                expected = stringResource(R.string.routine_topic_count, localizedNumber(3))
                SyllabusList(TopicLabel.Count(3), topicCount = 3)
            }
        }
        composeTestRule.onNodeWithText(expected).assertExists()
    }
}
