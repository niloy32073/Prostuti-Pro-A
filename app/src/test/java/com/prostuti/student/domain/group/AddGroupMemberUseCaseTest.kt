package com.prostuti.student.domain.group

import com.prostuti.student.data.repository.GroupRepository
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class AddGroupMemberUseCaseTest {
    private val useCase = AddGroupMemberUseCase(mockk<GroupRepository>())

    @Test fun `bangladeshi phone numbers get the country code`() {
        assertEquals("+8801712345678", useCase.normalizeIdentifier("01712345678"))
        assertEquals("+8801712345678", useCase.normalizeIdentifier(" 017-1234-5678 "))
        assertEquals("+8801712345678", useCase.normalizeIdentifier("8801712345678"))
        assertEquals("+8801712345678", useCase.normalizeIdentifier("+8801712345678"))
    }

    @Test fun `emails are trimmed and lower-cased`() {
        assertEquals("student@example.com", useCase.normalizeIdentifier("  Student@Example.COM "))
    }
}
