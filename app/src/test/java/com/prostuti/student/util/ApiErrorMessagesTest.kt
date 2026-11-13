package com.prostuti.student.util

import com.prostuti.student.data.model.enums.GroupRoutineErrorCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ApiErrorMessagesTest {
    /** Every documented code gets its own message except the ones the app handles silently. */
    @Test fun `every documented code has a message`() {
        val silent = setOf(GroupRoutineErrorCode.NOTIFICATION_NOT_FOUND)
        val codes = GroupRoutineErrorCode::class.java.declaredFields
            .filter { it.type == String::class.java }
            .map { it.get(null) as String }
        assertEquals(true, codes.isNotEmpty())
        codes.filterNot { it in silent }.forEach { code ->
            assertNotNull("no message for $code", apiErrorMessageRes(code))
        }
    }

    @Test fun `unknown codes fall back to the server message`() {
        assertNull(apiErrorMessageRes("SOMETHING_NEW"))
        assertNull(apiErrorMessageRes(GroupRoutineErrorCode.NOTIFICATION_NOT_FOUND))
    }
}
