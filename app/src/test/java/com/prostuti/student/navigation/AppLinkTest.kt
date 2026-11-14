package com.prostuti.student.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppLinkTest {

    private fun app(vararg path: String) = AppLink.parse("prostuti", "open", path.toList())
    private fun web(host: String, vararg path: String) = AppLink.parse("https", host, path.toList())

    @Test
    fun `the app scheme opens each place`() {
        assertEquals(AppLink.Home, app("home"))
        assertEquals(AppLink.LiveExams, app("live"))
        assertEquals(AppLink.LiveExam("e1"), app("live", "e1"))
        assertEquals(AppLink.GroupCalendar("g1"), app("group", "g1"))
        assertEquals(AppLink.RoutineTab, app("routine"))
        assertEquals(AppLink.CreateRoutine, app("routine", "create"))
        assertEquals(AppLink.LeaderboardTab, app("leaderboard"))
    }

    @Test
    fun `website links under app open the same places`() {
        assertEquals(AppLink.LiveExam("e1"), web("prostuti.com.bd", "app", "live", "e1"))
        assertEquals(AppLink.GroupCalendar("g1"), web("www.prostuti.com.bd", "app", "group", "g1"))
        assertEquals(AppLink.RoutineTab, web("Prostuti.com.bd", "app", "routine"))
    }

    @Test
    fun `the rest of the website and other links are not ours`() {
        assertNull(web("prostuti.com.bd", "live", "e1"))          // no /app/ prefix: a website page
        assertNull(web("prostuti.com.bd", "app"))                  // /app alone: nothing to open
        assertNull(web("evil.example", "app", "live"))
        assertNull(AppLink.parse("http", "prostuti.com.bd", listOf("app", "live")))
        assertNull(AppLink.parse("prostuti", "other", listOf("live")))
        assertNull(app("group"))                                   // a group link needs its id
        assertNull(app("unknown"))
    }
}
