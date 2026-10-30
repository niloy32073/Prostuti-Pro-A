package com.prostuti.student.data.model.enums

import kotlinx.serialization.Serializable

@Serializable
enum class IntegrityEventType {
    TAB_SWITCH, WINDOW_BLUR, COPY_PASTE, FAST_ANSWER, RIGHT_CLICK, SCREEN_SHARE_DETECTED
}
