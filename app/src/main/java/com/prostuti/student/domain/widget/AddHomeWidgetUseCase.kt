package com.prostuti.student.domain.widget

/** Opens the launcher's "add to home screen" dialog for the widget; false when it can't. */
class AddHomeWidgetUseCase(private val host: HomeWidgetHost) {
    suspend operator fun invoke(): Boolean = host.requestPin()
}
