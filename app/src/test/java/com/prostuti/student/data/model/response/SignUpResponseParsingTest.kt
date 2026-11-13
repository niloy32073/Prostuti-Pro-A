package com.prostuti.student.data.model.response

import com.prostuti.student.data.model.enums.UserStatus
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/** Regression: POST /auth/register returns user_id as a UUID string; it used to be read as an Int,
 *  so a successful sign-up failed to parse. */
class SignUpResponseParsingTest {

    // Same settings as the app's HttpClient (di/appModule.kt).
    private val json = Json { isLenient = true; ignoreUnknownKeys = true }

    private val registerBody = """
        {
          "success": true, "message": "Registered", "error": null, "meta_data": null,
          "data": {
            "user_id": "018f3c7e-007b-7000-8000-000000000001",
            "username": "rahimislam",
            "status": "PENDING"
          }
        }
    """.trimIndent()

    @Test fun `register response with a UUID user id parses`() {
        val base = json.decodeFromString<BaseResponse<PasswordBasedSignUpResponse>>(registerBody)
        assertEquals("018f3c7e-007b-7000-8000-000000000001", base.data?.userId)
        assertEquals("rahimislam", base.data?.userName)
        assertEquals(UserStatus.PENDING, base.data?.status)
    }
}
