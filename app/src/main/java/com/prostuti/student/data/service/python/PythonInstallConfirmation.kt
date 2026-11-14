package com.prostuti.student.data.service.python

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest

/** Shows Play's own "allow this download?" screen when the runtime state asks for it. */
interface PythonInstallConfirmation {
    /** False when there is nothing to confirm. */
    fun confirm(launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean
}
