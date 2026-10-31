package com.prostuti.student.ui.screen.examengine

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.os.PowerManager

/**
 * Called when the exam screen goes to the background: true when the student switched away on
 * purpose (home button, recent apps, another app). False for things that are not "leaving":
 * a rotation, the screen turning off / locking, or a phone call (ringing or in progress — read
 * from the audio mode, which needs no phone permission).
 */
internal fun isLeavingExam(context: Context, activity: Activity?): Boolean {
    if (activity?.isChangingConfigurations == true) return false
    val power = context.getSystemService(PowerManager::class.java)
    if (power != null && !power.isInteractive) return false
    val audioMode = context.getSystemService(AudioManager::class.java)?.mode
    val inCall = audioMode == AudioManager.MODE_RINGTONE ||
        audioMode == AudioManager.MODE_IN_CALL ||
        audioMode == AudioManager.MODE_IN_COMMUNICATION
    return !inCall
}
