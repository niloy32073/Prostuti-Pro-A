package com.prostuti.student.util

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.MetaData
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FetchAllPagesTest {
    private fun meta(totalPages: Int?) = MetaData(totalPages = totalPages)

    @Test fun `follows total_pages`() = runTest {
        val asked = mutableListOf<Int>()
        val r = fetchAllPages(limit = 2) { page, _ ->
            asked += page
            ApiResponse.Success(listOf("p$page-a", "p$page-b"), metaData = meta(3))
        }
        assertEquals(listOf(1, 2, 3), asked)
        assertEquals(6, (r as ApiResponse.Success).data.size)
    }

    @Test fun `without meta a short page is the last`() = runTest {
        val r = fetchAllPages(limit = 2) { page, _ ->
            ApiResponse.Success(if (page == 1) listOf("a", "b") else listOf("c"))
        }
        assertEquals(listOf("a", "b", "c"), (r as ApiResponse.Success).data)
    }

    @Test fun `any failing page fails the whole list`() = runTest {
        val r = fetchAllPages(limit = 1) { page, _ ->
            if (page == 2) ApiResponse.Failure("down", "NETWORK_ERROR") else ApiResponse.Success(listOf("x"), metaData = meta(3))
        }
        assertEquals("NETWORK_ERROR", (r as ApiResponse.Failure).errorCode)
    }

    @Test fun `stops at maxPages`() = runTest {
        var calls = 0
        fetchAllPages(limit = 1, maxPages = 4) { _, _ -> calls++; ApiResponse.Success(listOf("x"), metaData = meta(100)) }
        assertEquals(4, calls)
    }
}
