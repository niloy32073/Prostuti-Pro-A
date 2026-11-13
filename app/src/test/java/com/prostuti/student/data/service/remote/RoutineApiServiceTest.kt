package com.prostuti.student.data.service.remote

import com.prostuti.student.data.model.enums.SlotKind
import com.prostuti.student.data.model.enums.SlotMyStatus
import com.prostuti.student.data.model.enums.TopicGroupingLevel
import com.prostuti.student.data.model.enums.Weekday
import com.prostuti.student.data.model.request.AdhocExamRequest
import com.prostuti.student.data.model.request.RoutineCreateRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.service.remote.MockApi.Companion.error
import com.prostuti.student.data.service.remote.MockApi.Companion.ok
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineApiServiceTest {

    private fun slotJson(status: String = "OPEN", grouping: String = "CHAPTER") = """{
        "id": "s1", "kind": "ADHOC", "routine_id": null, "group_id": "g1", "title": "Friday mock test",
        "slot_date": "2026-11-15", "opens_at": "2026-11-15T00:00:00", "closes_at": "2026-11-15T12:00:00",
        "topic_ids": ["t1", "t2"],
        "topic_grouping": { "level": "$grouping", "subject_id": "sub1",
            "chapters": [ { "chapter_id": "c1", "topic_ids": ["t1", "t2"], "is_partial": true } ] },
        "questions_count": 20, "duration_minutes": 20, "mark_per_question": 1.0, "negative_mark_value": 0.25,
        "level": "ACADEMIC", "difficulty": null, "source_id": null, "postponed_count": 1,
        "exam_id": null, "my_status": "$status" }"""

    private val routineJson = """{ "routine": { "id": "r1", "group_id": null, "status": "ACTIVE", "start_date": "2026-11-10",
        "duration_days": 30, "weekdays": ["MON", "WED", "FRI"], "subject_ids": ["sub1"], "chapter_ids": [], "topic_ids": [],
        "window_open_minute": 0, "window_close_minute": 1440, "questions_count": 20, "duration_minutes": 20,
        "mark_per_question": 1.0, "negative_mark_value": 0.0, "level": null, "difficulty": null, "source_id": null,
        "created_at": "2026-11-08T10:00:00" }, "slots": [ ${slotJson()} ], "warnings": [] }"""

    private val personalRequest = RoutineCreateRequest(
        subjectIds = listOf("sub1"), questionsCount = 20, durationMinutes = 20,
        durationDays = 30, weekdays = listOf(Weekday.MON, Weekday.WED, Weekday.FRI), startDate = "2026-11-10",
    )

    @Test fun `preview of a personal routine omits group and window fields`() = runTest {
        val mock = MockApi(ok("""{ "exam_days": 13, "warnings": ["Dropped 1 subject: fewer exam days than subjects."],
            "slots": [ { "slot_date": "2026-11-10", "opens_at": "2026-11-09T18:00:00", "closes_at": "2026-11-10T18:00:00",
            "topic_ids": ["t1"], "topic_grouping": { "level": "TOPICS", "subject_id": null, "chapters": [] } } ] }"""))

        val r = RoutineApiService(mock.client).preview(personalRequest)

        assertEquals(HttpMethod.Post, mock.method)
        assertEquals("/api/v1/routines/preview", mock.path)
        val body = mock.jsonBody()
        assertFalse(body.containsKey("group_id"))
        assertFalse(body.containsKey("window_open_minute"))
        assertFalse(body.containsKey("window_close_minute"))
        assertEquals(listOf("MON", "WED", "FRI"), body["weekdays"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertEquals("2026-11-10", body["start_date"]!!.jsonPrimitive.content)
        r as ApiResponse.Success
        assertEquals(13, r.data.examDays)
        assertEquals(1, r.data.warnings.size)
        assertEquals(TopicGroupingLevel.TOPICS, r.data.slots[0].topicGrouping.level)
    }

    @Test fun `a group routine sends the group and its window`() = runTest {
        val mock = MockApi(ok(routineJson), HttpStatusCode.Created)

        RoutineApiService(mock.client).create(
            personalRequest.copy(groupId = "g1", windowOpenMinute = 1140, windowCloseMinute = 1380)
        )

        assertEquals("/api/v1/routines", mock.path)
        val body = mock.jsonBody()
        assertEquals("g1", body["group_id"]!!.jsonPrimitive.content)
        assertEquals("1140", body["window_open_minute"]!!.jsonPrimitive.content)
        assertEquals("1380", body["window_close_minute"]!!.jsonPrimitive.content)
    }

    @Test fun `ROUTINE_EXISTS surfaces as the error code`() = runTest {
        val mock = MockApi(error("ROUTINE_EXISTS"), HttpStatusCode.Conflict)
        val r = RoutineApiService(mock.client).create(personalRequest)
        assertEquals("ROUTINE_EXISTS", (r as ApiResponse.Failure).errorCode)
    }

    @Test fun `no active routine is a success with null data`() = runTest {
        val mock = MockApi(ok("null"))

        val r = RoutineApiService(mock.client).active(groupId = null)

        assertEquals("/api/v1/routines/active", mock.path)
        assertNull(mock.param("group_id"))
        assertTrue(r is ApiResponse.Success)
        assertNull((r as ApiResponse.Success).data)
    }

    @Test fun `an active group routine parses with its slots`() = runTest {
        val mock = MockApi(ok(routineJson))

        val r = RoutineApiService(mock.client).active(groupId = "g1")

        assertEquals("g1", mock.param("group_id"))
        val data = (r as ApiResponse.Success).data!!
        assertEquals(listOf(Weekday.MON, Weekday.WED, Weekday.FRI), data.routine.weekdays)
        assertEquals(1440, data.routine.windowCloseMinute)
        assertEquals(1, data.slots.size)
    }

    @Test fun `an active-routine error is still a failure`() = runTest {
        val mock = MockApi(error("GROUP_NOT_FOUND"), HttpStatusCode.NotFound)
        val r = RoutineApiService(mock.client).active(groupId = "g1")
        assertEquals("GROUP_NOT_FOUND", (r as ApiResponse.Failure).errorCode)
    }

    @Test fun `slots send the filters and parse every field`() = runTest {
        val mock = MockApi(ok("[ ${slotJson()} ]"))

        val r = RoutineApiService(mock.client).slots("2026-11-10", "2026-11-16", "g1", page = 1, limit = 100)

        assertEquals("/api/v1/routines/slots", mock.path)
        assertEquals("2026-11-10", mock.param("date_from"))
        assertEquals("2026-11-16", mock.param("date_to"))
        assertEquals("g1", mock.param("group_id"))
        assertEquals("100", mock.param("limit"))
        val slot = (r as ApiResponse.Success).data.single()
        assertEquals(SlotKind.ADHOC, slot.kind)
        assertEquals(SlotMyStatus.OPEN, slot.myStatus)
        assertEquals(TopicGroupingLevel.CHAPTER, slot.topicGrouping.level)
        assertTrue(slot.topicGrouping.chapters.single().isPartial)
        assertEquals(0.25, slot.negativeMarkValue, 0.0)
        assertEquals(1, slot.postponedCount)
        assertNull(slot.examId)
    }

    @Test fun `slots without filters send none`() = runTest {
        val mock = MockApi(ok("[]"))
        RoutineApiService(mock.client).slots(null, null, null, page = 1, limit = 50)
        assertNull(mock.param("date_from"))
        assertNull(mock.param("date_to"))
        assertNull(mock.param("group_id"))
    }

    @Test fun `unknown status and grouping values do not break parsing`() = runTest {
        val mock = MockApi(ok("[ ${slotJson(status = "PAUSED", grouping = "SECTIONS")} ]"))
        val slot = (RoutineApiService(mock.client).slots(null, null, null, 1, 50) as ApiResponse.Success).data.single()
        assertEquals(SlotMyStatus.UPCOMING, slot.myStatus)
        assertEquals(TopicGroupingLevel.TOPICS, slot.topicGrouping.level)
    }

    @Test fun `adhoc exam posts the date window and title`() = runTest {
        val mock = MockApi(ok(slotJson("UPCOMING")), HttpStatusCode.Created)

        RoutineApiService(mock.client).createAdhoc(
            AdhocExamRequest(
                subjectIds = listOf("sub1"), questionsCount = 20, durationMinutes = 20, groupId = "g1",
                examDate = "2026-11-15", windowOpenMinute = 360, windowCloseMinute = 1080, title = "Friday mock test",
            )
        )

        assertEquals("/api/v1/routines/slots", mock.path)
        val body = mock.jsonBody()
        assertEquals("2026-11-15", body["exam_date"]!!.jsonPrimitive.content)
        assertEquals("360", body["window_open_minute"]!!.jsonPrimitive.content)
        assertEquals("Friday mock test", body["title"]!!.jsonPrimitive.content)
    }

    @Test fun `start returns the shared exam`() = runTest {
        val mock = MockApi(ok("""{ "slot_id": "s1", "exam_id": "e1", "is_live": true, "closes_at": "2026-11-15T12:00:00" }"""))

        val r = RoutineApiService(mock.client).start("s1")

        assertEquals("/api/v1/routines/slots/s1/start", mock.path)
        r as ApiResponse.Success
        assertEquals("e1", r.data.examId)
        assertTrue(r.data.isLive)
    }

    @Test fun `SLOT_NOT_OPEN surfaces as the error code`() = runTest {
        val mock = MockApi(error("SLOT_NOT_OPEN"), HttpStatusCode.Conflict)
        assertEquals("SLOT_NOT_OPEN", (RoutineApiService(mock.client).start("s1") as ApiResponse.Failure).errorCode)
    }

    @Test fun `reschedule patches the new date`() = runTest {
        val mock = MockApi(ok(slotJson("UPCOMING")))
        RoutineApiService(mock.client).reschedule("s1", "2026-11-17")
        assertEquals(HttpMethod.Patch, mock.method)
        assertEquals("/api/v1/routines/slots/s1/reschedule", mock.path)
        assertEquals("2026-11-17", mock.jsonBody()["slot_date"]!!.jsonPrimitive.content)
    }

    @Test fun `delete adhoc accepts a null-data success`() = runTest {
        val mock = MockApi(ok("null"))
        val r = RoutineApiService(mock.client).deleteAdhoc("s1")
        assertEquals(HttpMethod.Delete, mock.method)
        assertTrue(r is ApiResponse.Success)
    }

    @Test fun `delete routine reports removed slots`() = runTest {
        val mock = MockApi(ok("""{ "routine_id": "r1", "removed_slots": 8 }"""))
        val r = RoutineApiService(mock.client).delete("r1")
        assertEquals("/api/v1/routines/r1", mock.path)
        assertEquals(8, (r as ApiResponse.Success).data.removedSlots)
    }
}
