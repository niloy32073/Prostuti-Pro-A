package com.prostuti.student.util

import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.serialization.SerializationException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import timber.log.Timber
import java.net.ConnectException
import java.net.UnknownHostException

class CrashlyticsTreeTest {

    private val crashlytics: FirebaseCrashlytics = mockk(relaxed = true)

    @Before fun plant() = Timber.plant(CrashlyticsTree(crashlytics))
    @After fun uproot() = Timber.uprootAll()

    @Test fun `error with throwable is recorded as non-fatal with a breadcrumb`() {
        val boom = IllegalStateException("boom")
        Timber.tag("T").e(boom, "it failed")

        verify { crashlytics.log("E/T: it failed") }
        verify { crashlytics.recordException(boom) }
    }

    @Test fun `error without throwable records LoggedError starting at the caller`() {
        val recorded = slot<Throwable>()
        Timber.tag("T").e("bad state %d", 3)

        verify { crashlytics.recordException(capture(recorded)) }
        val error = recorded.captured as LoggedError
        assertEquals("[T] bad state 3", error.message)
        assertEquals(CrashlyticsTreeTest::class.java.name, error.stackTrace.first().className)
    }

    @Test fun `warning is only a breadcrumb`() {
        Timber.tag("T").w("slow")

        verify { crashlytics.log("W/T: slow") }
        verify(exactly = 0) { crashlytics.recordException(any()) }
    }

    @Test fun `debug logs are dropped`() {
        Timber.d("secret body")

        verify(exactly = 0) { crashlytics.log(any()) }
        verify(exactly = 0) { crashlytics.recordException(any()) }
    }

    @Test fun `serialization failure is reported as non-fatal`() {
        val e = SerializationException("missing field")
        logApiFailure("safeApiCall", "SERIALIZATION_ERROR", e)

        verify { crashlytics.recordException(e) }
    }

    @Test fun `network failures are breadcrumbs, not reports`() {
        logApiFailure("safeApiCall", NETWORK_ERROR, UnknownHostException())
        logApiFailure("safeApiCall", "UNKNOWN_ERROR", ConnectException("refused"))

        verify(exactly = 0) { crashlytics.recordException(any()) }
        val crumbs = mutableListOf<String>()
        verify(exactly = 2) { crashlytics.log(capture(crumbs)) }
        assertTrue(crumbs.all { it.startsWith("W/") })
    }
}
