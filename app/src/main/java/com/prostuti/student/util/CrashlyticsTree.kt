package com.prostuti.student.util

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

/**
 * Release-build Timber tree: sends logs to Crashlytics instead of Logcat, so nothing is written on
 * the user's device.
 * - ERROR (`Timber.e`) → a non-fatal report (the throwable, or [LoggedError] built from the message).
 * - INFO / WARN → a breadcrumb attached to the next crash or non-fatal report.
 * - DEBUG / VERBOSE → dropped (debug dumps can hold personal data).
 */
class CrashlyticsTree(private val crashlytics: FirebaseCrashlytics) : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= Log.INFO

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // Timber appends the stack trace to `message` when there is a throwable; the report has it already.
        val firstLine = message.substringBefore('\n')
        crashlytics.log("${levelLetter(priority)}/${tag ?: "-"}: $firstLine")
        if (priority >= Log.ERROR) {
            crashlytics.recordException(t ?: LoggedError(tag, firstLine))
        }
    }

    private fun levelLetter(priority: Int) = when (priority) {
        Log.INFO -> "I"
        Log.WARN -> "W"
        Log.ERROR -> "E"
        else -> "A"
    }
}

/**
 * Stands in for the throwable of a `Timber.e("…")` call that had none. Its stack trace starts at
 * the caller (Timber's own frames are dropped), so Crashlytics groups reports by where they were logged.
 */
class LoggedError(tag: String?, message: String) : Exception(if (tag == null) message else "[$tag] $message") {
    init {
        val loggingFrames = setOf(
            LoggedError::class.java.name,
            CrashlyticsTree::class.java.name,
            Timber::class.java.name,
            Timber.Tree::class.java.name,
            Timber.Forest::class.java.name,
        )
        val trace = stackTrace
        val firstCaller = trace.indexOfFirst { it.className !in loggingFrames }
        if (firstCaller > 0) stackTrace = trace.copyOfRange(firstCaller, trace.size)
    }
}
