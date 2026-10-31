package com.prostuti.student.data.model.response

import com.prostuti.student.data.model.enums.ExamTypes
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/** Regression: GET /exams/{id} for a generated practice exam returns exam_type "GENERATED",
 *  which used to fail parsing and left Home's continue card without a title. */
class ExamSummaryParsingTest {

    // Same settings as the app's HttpClient (di/appModule.kt).
    private val json = Json { isLenient = true; ignoreUnknownKeys = true }

    private val generatedExamBody = """
        {
          "success": true, "message": "ok", "error": null, "meta_data": null,
          "data": {
            "id": "01a187ed-0cf7-7641-98da-5a107a1c3e91",
            "exam_type": "GENERATED",
            "title_bn": "প্র্যাকটিস পরীক্ষা", "title_en": "Practice exam",
            "duration": 25, "level": "ACADEMIC", "exam_year": null
          }
        }
    """.trimIndent()

    @Test fun `generated exam parses into ExamSummary`() {
        val base = json.decodeFromString<BaseResponse<ExamSummary>>(generatedExamBody)
        assertEquals("প্র্যাকটিস পরীক্ষা", base.data?.titleBn)
        assertEquals(25, base.data?.duration)
    }

    @Test fun `GENERATED is a known exam type`() {
        assertEquals(ExamTypes.GENERATED, json.decodeFromString<ExamTypes>("\"GENERATED\""))
    }
}
