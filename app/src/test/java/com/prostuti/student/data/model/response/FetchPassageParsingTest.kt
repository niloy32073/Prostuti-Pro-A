package com.prostuti.student.data.model.response

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Regression: GET /questions/passages/{id} sends the image as "passage_image_link",
 *  which the model used to miss, so image-only passages showed nothing. */
class FetchPassageParsingTest {

    // Same settings as the app's HttpClient (di/appModule.kt).
    private val json = Json { isLenient = true; ignoreUnknownKeys = true }

    private fun body(dataJson: String) = """
        {"success": true, "message": "Passage retrieved successfully.", "error": null, "meta_data": null, "data": $dataJson}
    """.trimIndent()

    @Test fun `passage_image_link from the API is read`() {
        val base = json.decodeFromString<BaseResponse<FetchPassageData>>(
            body(
                """
                {
                  "id": "019f5f49-1aef-776b-9a28-cbe2611510e0",
                  "subject_id": "s1", "chapter_id": "c1",
                  "passage_text": null,
                  "passage_image_link": "https://cdn.prostuti.com.bd/images/79c1bfecb10b4a6abf338771f5222a80.jpeg",
                  "created_at": "2026-07-14T06:20:54.383505"
                }
                """.trimIndent()
            )
        )
        assertEquals("https://cdn.prostuti.com.bd/images/79c1bfecb10b4a6abf338771f5222a80.jpeg", base.data?.passageImage)
        assertNull(base.data?.passageText)
    }

    @Test fun `older passage_image name is still accepted`() {
        val base = json.decodeFromString<BaseResponse<FetchPassageData>>(
            body("""{"id": "p1", "passage_text": "লেখা", "passage_image": "https://cdn/x.png"}""")
        )
        assertEquals("https://cdn/x.png", base.data?.passageImage)
    }

    @Test fun `text-only passage has no image`() {
        val base = json.decodeFromString<BaseResponse<FetchPassageData>>(
            body("""{"id": "p2", "passage_text": "উদ্দীপক", "passage_image_link": null}""")
        )
        assertNull(base.data?.passageImage)
        assertEquals("উদ্দীপক", base.data?.passageText)
    }
}
