package com.prostuti.student.ui.screen.routine.builder

import com.prostuti.student.data.model.enums.TopicGroupingLevel
import com.prostuti.student.data.model.enums.Weekday
import com.prostuti.student.data.model.response.Chapter
import com.prostuti.student.data.model.response.ChapterGroup
import com.prostuti.student.data.model.response.Topic
import com.prostuti.student.data.model.response.TopicGrouping
import com.prostuti.student.domain.routine.TopicLabel
import com.prostuti.student.ui.screen.routine.weekday
import com.prostuti.student.ui.screen.practice.PracticeChapterGroup
import com.prostuti.student.ui.screen.practice.PracticeSubjectGroup
import com.prostuti.student.ui.screen.routine.builder.RoutineBuilderForm.Problem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import com.prostuti.student.data.model.response.CurriculumName

/** Curriculum helpers shared by the builder tests. */
internal object BuilderFixtures {
    fun topic(id: String, chapterId: String) = Topic(id = id, chapterId = chapterId, nameBn = "টপিক $id", nameEn = id, order = 0, createdAt = "", createdBy = "")
    fun chapter(id: String, subjectId: String) = Chapter(id = id, subjectId = subjectId, chapterNameBn = "অধ্যায় $id", chapterNameEn = id, order = 0, createdAt = "", createdBy = "")

    /** Subject "phy": ch1 (t1, t2), ch2 (t3). Subject "chem": ch3 (t4, t5). */
    val groups = listOf(
        PracticeSubjectGroup("phy", "পদার্থ", listOf(
            PracticeChapterGroup(chapter("ch1", "phy"), listOf(topic("t1", "ch1"), topic("t2", "ch1"))),
            PracticeChapterGroup(chapter("ch2", "phy"), listOf(topic("t3", "ch2"))),
        )),
        PracticeSubjectGroup("chem", "রসায়ন", listOf(
            PracticeChapterGroup(chapter("ch3", "chem"), listOf(topic("t4", "ch3"), topic("t5", "ch3"))),
        )),
    )
    val allTopics = setOf("t1", "t2", "t3", "t4", "t5")
}

class RoutineBuilderLogicTest {
    private val groups = BuilderFixtures.groups

    // ── Scope ──

    @Test fun `whole books send only subject ids`() {
        val scope = RoutineScope.from(groups, BuilderFixtures.allTopics)
        assertEquals(RoutineScope(listOf("phy", "chem"), emptyList(), emptyList()), scope)
    }

    @Test fun `a narrowed subject lists every kept chapter, the whole books' too`() {
        // Physics' ch2 dropped; chemistry stays whole, so its ch3 has to be in the chapter filter.
        val scope = RoutineScope.from(groups, setOf("t1", "t2", "t4", "t5"))
        assertEquals(RoutineScope(listOf("phy", "chem"), listOf("ch1", "ch3"), emptyList()), scope)
    }

    @Test fun `a part chapter lists every picked topic`() {
        val scope = RoutineScope.from(groups, setOf("t1", "t3", "t4", "t5"))
        assertEquals(listOf("ch1", "ch2", "ch3"), scope.chapterIds)
        assertEquals(listOf("t1", "t3", "t4", "t5"), scope.topicIds)
    }

    @Test fun `a subject with nothing picked is left out`() {
        val scope = RoutineScope.from(groups, setOf("t4", "t5"))
        assertEquals(RoutineScope(listOf("chem"), emptyList(), emptyList()), scope)
        assertEquals(RoutineScope.Problem.NOTHING_SELECTED, RoutineScope.from(groups, emptySet()).problem)
    }

    @Test fun `limits follow the API`() {
        val many = RoutineScope(listOf("s"), List(201) { "c$it" }, emptyList())
        assertEquals(RoutineScope.Problem.TOO_MANY_CHAPTERS, many.problem)
        assertEquals(RoutineScope.Problem.TOO_MANY_TOPICS, RoutineScope(listOf("s"), listOf("c"), List(501) { "t$it" }).problem)
    }

    // ── Form ──

    private val today = LocalDate.of(2026, 11, 8)
    private fun form() = RoutineBuilderForm.initial(RoutineOwner.Group("g1"), today)

    @Test fun `the default group form is valid`() {
        assertTrue(form().problems(RoutineBuilderMode.ROUTINE, today, nowMinute = 600).isEmpty())
        assertEquals(19 * 60 to 23 * 60, form().windowOpenMinute to form().windowCloseMinute)
    }

    @Test fun `a personal routine is open all day`() {
        val personal = RoutineBuilderForm.initial(RoutineOwner.Personal, today)
        assertEquals(0 to 1440, personal.windowOpenMinute to personal.windowCloseMinute)
    }

    @Test fun `form problems`() {
        val routine = RoutineBuilderMode.ROUTINE
        assertEquals(setOf(Problem.NO_WEEKDAY), form().copy(weekdays = emptySet()).problems(routine, today, 0))
        assertEquals(setOf(Problem.WINDOW_ORDER), form().copy(windowOpenMinute = 600, windowCloseMinute = 600).problems(routine, today, 0))
        assertEquals(setOf(Problem.DURATION_OVER_WINDOW), form().copy(windowOpenMinute = 600, windowCloseMinute = 620).problems(routine, today, 0))
        assertEquals(setOf(Problem.DATE_IN_PAST), form().copy(startDate = "2026-11-07").problems(routine, today, 0))
    }

    @Test fun `a one-off exam today needs a window that hasn't closed`() {
        val adhoc = RoutineBuilderMode.ADHOC
        val todayExam = form().copy(examDate = "2026-11-08", windowOpenMinute = 600, windowCloseMinute = 720)
        assertEquals(setOf(Problem.WINDOW_ALREADY_OVER), todayExam.problems(adhoc, today, nowMinute = 720))
        assertTrue(todayExam.problems(adhoc, today, nowMinute = 700).isEmpty())
        assertTrue("tomorrow is fine", todayExam.copy(examDate = "2026-11-09").problems(adhoc, today, 900).isEmpty())
        assertEquals(setOf(Problem.TITLE_TOO_LONG), form().copy(title = "x".repeat(256)).problems(adhoc, today, 0))
        assertTrue("weekdays don't matter", form().copy(weekdays = emptySet()).problems(adhoc, today, 0).isEmpty())
    }

    @Test fun `marks stay on the quarter grid`() {
        assertEquals(0.75, RoutineBuilderForm.quarter(0.25 + 0.25 + 0.25), 0.0)
        assertEquals(1.25, RoutineBuilderForm.quarter(1.2500000001), 0.0)
    }

    @Test fun `weekday of a date`() {
        assertEquals(Weekday.SUN, LocalDate.of(2026, 11, 8).weekday())
        assertEquals(Weekday.MON, LocalDate.of(2026, 11, 9).weekday())
    }

    // ── Topic label ──

    private val names = groups.curriculumNames()

    @Test fun `one chapter, whole or in part`() {
        val whole = TopicGrouping(TopicGroupingLevel.CHAPTER, "phy", listOf(ChapterGroup("ch1", listOf("t1", "t2"), isPartial = false)))
        assertEquals(TopicLabel.Chapter(CurriculumName("পদার্থ", ""), CurriculumName("অধ্যায় ch1", "ch1"), emptyList(), 0), TopicLabel.of(whole, listOf("t1", "t2"), names).short())
        val part = whole.copy(chapters = listOf(ChapterGroup("ch1", listOf("t2"), isPartial = true)))
        assertEquals(TopicLabel.Chapter(CurriculumName("পদার্থ", ""), CurriculumName("অধ্যায় ch1", "ch1"), listOf(CurriculumName("টপিক t2", "t2")), 0), TopicLabel.of(part, listOf("t2"), names).short())
    }

    @Test fun `several chapters and plain topics`() {
        val chapters = TopicGrouping(TopicGroupingLevel.CHAPTERS, "phy", listOf(ChapterGroup("ch1"), ChapterGroup("ch2")))
        assertEquals(TopicLabel.Chapters(CurriculumName("পদার্থ", ""), listOf(CurriculumName("অধ্যায় ch1", "ch1"), CurriculumName("অধ্যায় ch2", "ch2")), 0), TopicLabel.of(chapters, emptyList(), names).short())
        val topics = TopicGrouping(TopicGroupingLevel.TOPICS)
        assertEquals(TopicLabel.Topics(listOf(CurriculumName("টপিক t1", "t1"), CurriculumName("টপিক t3", "t3"), CurriculumName("টপিক t4", "t4")), 1), TopicLabel.of(topics, listOf("t1", "t3", "t4", "t5"), names).short())
    }

    @Test fun `unknown names fall back to a count`() {
        val topics = TopicGrouping(TopicGroupingLevel.TOPICS)
        assertEquals(TopicLabel.Count(2), TopicLabel.of(topics, listOf("t1", "new"), names))
        val chapter = TopicGrouping(TopicGroupingLevel.CHAPTER, "phy", listOf(ChapterGroup("ch9")))
        assertEquals(TopicLabel.Count(1), TopicLabel.of(chapter, listOf("x"), names))
    }

    /** The short label alone; its full outline is covered by TopicNamesTest. */
    private fun TopicLabel.short(): TopicLabel = when (this) {
        is TopicLabel.Chapter -> copy(outline = null)
        is TopicLabel.Chapters -> copy(outline = null)
        is TopicLabel.Topics -> copy(outline = null)
        is TopicLabel.Count -> this
    }
}
