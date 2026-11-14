package com.prostuti.student.domain.code.run

/** The on-demand Python runtime (a Play module downloaded on first use). */
sealed interface RuntimeState {
    data object NotInstalled : RuntimeState
    /** [total] is 0 until Play knows the size. */
    data class Downloading(val downloaded: Long, val total: Long) : RuntimeState
    data object Installing : RuntimeState
    /** Play asks the user to approve the download (e.g. a large download on mobile data). */
    data object NeedsConfirmation : RuntimeState
    data object Installed : RuntimeState
    /** [code] is Play's error code, shown when the [reason] is [InstallFailure.OTHER]. */
    data class Failed(val reason: InstallFailure, val code: Int) : RuntimeState
}
