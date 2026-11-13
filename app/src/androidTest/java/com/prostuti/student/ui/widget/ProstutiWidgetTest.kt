package com.prostuti.student.ui.widget

import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasText
import androidx.glance.testing.unit.hasTextEqualTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.prostuti.student.R
import com.prostuti.student.data.model.enums.SlotKind
import com.prostuti.student.data.model.enums.SlotMyStatus
import com.prostuti.student.data.model.enums.TimedExamKind
import com.prostuti.student.data.model.enums.TopicGroupingLevel
import com.prostuti.student.data.model.response.RoutineSlot
import com.prostuti.student.data.model.response.TimedExam
import com.prostuti.student.data.model.response.TopicGrouping
import com.prostuti.student.data.model.widget.WidgetExamItem
import com.prostuti.student.data.model.widget.WidgetExamKind
import com.prostuti.student.data.model.widget.WidgetSnapshot
import com.prostuti.student.domain.FeatureAccess
import com.prostuti.student.domain.widget.UpcomingExam
import com.prostuti.student.domain.widget.WidgetData
import com.prostuti.student.navigation.AppLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.Locale

/** The widget's links, snapshot mapping and drawing. "Now" is Tue 10 Nov 2026, 20:00 Dhaka. */
@RunWith(AndroidJUnit4::class)
class ProstutiWidgetTest {
    private val now = Instant.parse("2026-11-10T14:00:00Z")
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    /** English text, whatever the device or app language. */
    private val english: Context = context.createConfigurationContext(
        Configuration(context.resources.configuration).apply { setLocale(Locale.ENGLISH) }
    )

    private fun millis(iso: String) = Instant.parse(iso).toEpochMilli()

    private fun liveExam(id: String = "live1", start: String = "2026-11-10T13:00:00Z", end: String = "2026-11-10T15:00:00Z") = TimedExam(
        examId = id, kind = TimedExamKind.LIVE, titleBn = "লাইভ মডেল টেস্ট", subtitleBn = "", windowStartEpochMillis = millis(start),
        windowEndEpochMillis = millis(end), examDurationMinutes = 60, questionCount = 25, markPerQuestion = 1f, negativeMark = 0.25f,
        totalParticipants = 10, titleEn = "Live model test",
    )

    private fun slot(id: String, groupId: String?, title: String? = null) = RoutineSlot(
        id = id, kind = SlotKind.ADHOC, groupId = groupId, title = title, slotDate = "2026-11-10",
        opensAt = "2026-11-10T13:00:00", closesAt = "2026-11-10T17:00:00", topicIds = listOf("t1", "t2"),
        topicGrouping = TopicGrouping(TopicGroupingLevel.TOPICS), questionsCount = 25, durationMinutes = 25,
        markPerQuestion = 1.0, negativeMarkValue = 0.25, myStatus = SlotMyStatus.OPEN,
    )

    private fun signedIn(upcoming: List<UpcomingExam>? = emptyList(), today: List<RoutineSlot>? = emptyList(), hasPersonal: Boolean? = false) =
        WidgetData.SignedIn(
            access = FeatureAccess(), upcoming = upcoming, today = today, hasPersonalRoutine = hasPersonal,
            groupNames = mapOf("g1" to "Physics circle"), labels = emptyMap(),
        )

    private fun item(title: String, start: String = "2026-11-10T13:00:00Z", end: String = "2026-11-10T15:00:00Z") = WidgetExamItem(
        id = title, title = title, source = "Live exam", startsAtMillis = millis(start), endsAtMillis = millis(end), link = AppLink.LiveExam(title).uri,
    )

    // ── Links ──

    @Test fun everyLinkSurvivesTheIntentRoundTrip() {
        val links = listOf(AppLink.Home, AppLink.LiveExams, AppLink.LiveExam("ex 1/2"), AppLink.GroupCalendar("g1"),
            AppLink.RoutineTab, AppLink.CreateRoutine, AppLink.LeaderboardTab)
        links.forEach { assertEquals(it, AppLink.parse(Uri.parse(it.uri))) }
        assertNull("not ours", AppLink.parse(Uri.parse("https://open/live/x")))
        assertNull(AppLink.parse(Uri.parse("prostuti://open/unknown")))
    }

    // ── Snapshot ──

    @Test fun signedInTurnsIntoTextWithLinks() {
        val data = signedIn(
            upcoming = listOf(UpcomingExam.Live(liveExam())),
            today = listOf(slot("mine", groupId = null), slot("grp", groupId = "g1", title = "Mock 3")),
            hasPersonal = true,
        )

        val snapshot = data.toSnapshot(english, previous = null, now = now)

        assertEquals("Live model test", snapshot.exams?.single()?.title)
        assertEquals(AppLink.LiveExam("live1").uri, snapshot.exams?.single()?.link)
        assertEquals(listOf("Personal routine", "Physics circle"), snapshot.today?.map { it.source })
        assertEquals("Mock 3", snapshot.today?.last()?.title)
        assertEquals("no names cached: a topic count", english.getString(R.string.routine_topic_count, "2"), snapshot.today?.first()?.title)
        assertEquals(AppLink.GroupCalendar("g1").uri, snapshot.today?.last()?.link)
        assertTrue(snapshot.hasPersonalRoutine)
        assertEquals(listOf(WidgetExamKind.LIVE), snapshot.exams?.map { it.kind })
        assertEquals(listOf(WidgetExamKind.PERSONAL, WidgetExamKind.GROUP), snapshot.today?.map { it.kind })
    }

    @Test fun thePreviewShowsARunningExamFirst() {
        val sample = sampleWidgetSnapshot(english, now)

        assertTrue(sample.signedIn)
        assertEquals(3, sample.exams?.size)
        val first = sample.exams!!.first()
        assertTrue("running now", first.startsAtMillis <= now.toEpochMilli() && now.toEpochMilli() < first.endsAtMillis)
    }

    @Test fun aFailedCallKeepsTheLastDataAndItsTime() {
        val previous = WidgetSnapshot(signedIn = true, updatedAtMillis = 5L, checkedAtMillis = 5L, exams = listOf(item("Old")), today = emptyList(), hasPersonalRoutine = true)

        val snapshot = signedIn(upcoming = null, today = null, hasPersonal = null).toSnapshot(english, previous, now)

        assertEquals(listOf("Old"), snapshot.exams?.map { it.title })
        assertEquals(emptyList<WidgetExamItem>(), snapshot.today)
        assertTrue(snapshot.hasPersonalRoutine)
        assertEquals("nothing new came in", 5L, snapshot.updatedAtMillis)
        assertEquals("but it was tried", now.toEpochMilli(), snapshot.checkedAtMillis)
    }

    @Test fun signingOutDropsEverything() {
        val previous = WidgetSnapshot(signedIn = true, exams = listOf(item("Old")))

        val snapshot = WidgetData.SignedOut.toSnapshot(english, previous, now)

        assertEquals(false, snapshot.signedIn)
        assertNull(snapshot.exams)
    }

    // ── Drawing ──

    @Test fun signedOutAsksToSignIn() = runGlanceAppWidgetUnitTest {
        setContext(context)
        provideComposable { WidgetContent(WidgetSnapshot(signedIn = false), WidgetTab.EXAMS, english, now) }

        onNode(hasText(english.getString(R.string.widget_sign_in_body))).assertExists()
        onNode(hasTextEqualTo(english.getString(R.string.widget_sign_in))).assertExists()
    }

    @Test fun examsTabMarksTheRunningExam() = runGlanceAppWidgetUnitTest {
        setContext(context)
        val ended = item("Ended one", start = "2026-11-10T08:00:00Z", end = "2026-11-10T09:00:00Z")
        provideComposable {
            WidgetContent(WidgetSnapshot(signedIn = true, updatedAtMillis = 1L, exams = listOf(item("Running one"), ended)), WidgetTab.EXAMS, english, now)
        }

        onNode(hasText("Running one")).assertExists()
        onNode(hasText(english.getString(R.string.widget_live_now, "9:00 PM"))).assertExists()
        onNode(hasText("Ended one")).assertDoesNotExist()
    }

    @Test fun routineTabOffersToCreateARoutine() = runGlanceAppWidgetUnitTest {
        setContext(context)
        provideComposable { WidgetContent(WidgetSnapshot(signedIn = true, today = emptyList(), hasPersonalRoutine = false), WidgetTab.ROUTINE, english, now) }

        onNode(hasText(english.getString(R.string.widget_no_routine))).assertExists()
        onNode(hasText(english.getString(R.string.widget_create_routine))).assertExists()
    }

    @Test fun routineTabWithARoutineButNoExamToday() = runGlanceAppWidgetUnitTest {
        setContext(context)
        provideComposable { WidgetContent(WidgetSnapshot(signedIn = true, today = emptyList(), hasPersonalRoutine = true), WidgetTab.ROUTINE, english, now) }

        onNode(hasText(english.getString(R.string.widget_no_exam_today))).assertExists()
        onNode(hasText(english.getString(R.string.widget_create_routine))).assertDoesNotExist()
    }

    @Test fun progressTabSaysItsNumbersAreDemo() = runGlanceAppWidgetUnitTest {
        setContext(context)
        provideComposable { WidgetContent(WidgetSnapshot(signedIn = true), WidgetTab.PROGRESS, english, now) }

        onNode(hasText(english.getString(R.string.widget_demo))).assertExists()
        onNode(hasText("1,240")).assertExists()
    }
}
