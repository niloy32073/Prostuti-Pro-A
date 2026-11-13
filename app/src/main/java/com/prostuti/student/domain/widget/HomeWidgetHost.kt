package com.prostuti.student.domain.widget

/** The phone's launcher, as far as the Prostuti home screen widget goes. */
interface HomeWidgetHost {
    /** The launcher can add the widget for us, from inside the app. */
    fun canPin(): Boolean

    /** At least one Prostuti widget is on a home screen. */
    suspend fun isPlaced(): Boolean

    /** Asks the launcher to add the widget (it shows its own confirm dialog); false when it can't. */
    suspend fun requestPin(): Boolean
}
