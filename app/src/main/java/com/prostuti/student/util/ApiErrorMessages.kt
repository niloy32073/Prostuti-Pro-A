package com.prostuti.student.util

import android.content.Context
import androidx.annotation.StringRes
import com.prostuti.student.R
import com.prostuti.student.data.model.enums.ExamAttemptErrorCode
import com.prostuti.student.data.model.enums.GroupRoutineErrorCode as E
import com.prostuti.student.data.model.response.ApiResponse

/** The app's own message for a documented API error code, or null to fall back to the server's message. */
@StringRes
fun apiErrorMessageRes(errorCode: String): Int? = when (errorCode) {
    E.INSUFFICIENT_SCOPE -> R.string.api_err_insufficient_scope
    E.AUTHZ_UNAVAILABLE -> R.string.api_err_authz_unavailable
    E.GROUP_NOT_FOUND -> R.string.api_err_group_not_found
    E.NOT_GROUP_ADMIN -> R.string.api_err_not_group_admin
    E.INVALID_GROUP_CODE -> R.string.api_err_invalid_group_code
    E.ALREADY_MEMBER -> R.string.api_err_already_member
    E.GROUP_FULL -> R.string.api_err_group_full
    E.LAST_ADMIN -> R.string.api_err_last_admin
    E.MAX_MEMBERS_TOO_LOW -> R.string.api_err_max_members_too_low
    E.INVALID_IDENTIFIER -> R.string.api_err_invalid_identifier
    E.USER_NOT_REGISTERED -> R.string.api_err_user_not_registered
    E.MEMBER_NOT_FOUND -> R.string.api_err_member_not_found
    E.JOIN_REQUEST_EXISTS -> R.string.api_err_join_request_exists
    E.JOIN_REQUEST_NOT_FOUND, E.JOIN_REQUEST_DECIDED -> R.string.api_err_join_request_decided
    E.CODE_GENERATION_FAILED -> R.string.api_err_try_again
    E.ROUTINE_EXISTS -> R.string.api_err_routine_exists
    E.ROUTINE_NOT_FOUND, E.ROUTINE_ALREADY_DELETED -> R.string.api_err_routine_not_found
    E.START_IN_PAST, E.DATE_IN_PAST -> R.string.api_err_date_in_past
    E.INVALID_WINDOW -> R.string.api_err_invalid_window
    E.WINDOW_IN_PAST -> R.string.api_err_window_in_past
    E.NO_EXAM_DAYS -> R.string.api_err_no_exam_days
    E.NO_TOPICS_TO_SCHEDULE -> R.string.api_err_no_topics
    E.CURRICULUM_MISMATCH, E.SUBJECT_NOT_FOUND -> R.string.api_err_curriculum_mismatch
    E.SLOT_NOT_FOUND -> R.string.api_err_slot_not_found
    E.SLOT_NOT_OPEN -> R.string.api_err_slot_not_open
    E.SLOT_ALREADY_OPEN -> R.string.api_err_slot_already_open
    E.SLOT_HAS_ATTEMPTS -> R.string.api_err_slot_has_attempts
    E.NOT_AN_ADHOC_EXAM -> R.string.api_err_not_adhoc
    E.SLOT_NO_QUESTIONS -> R.string.api_err_slot_no_questions
    E.NO_ATTEMPT -> R.string.api_err_no_attempt
    E.OUTSIDE_ROUTINE_PERIOD -> R.string.api_err_outside_routine_period
    E.INVALID_DATE_RANGE -> R.string.api_err_invalid_date_range
    ExamAttemptErrorCode.EXAM_EXPIRED, ExamAttemptErrorCode.ATTEMPT_ALREADY_ENDED -> R.string.api_err_exam_time_over
    else -> null
}

/** Turns an API failure into the text a screen shows. Interface so ViewModel tests can fake it. */
interface ApiErrorMessages {
    fun messageFor(failure: ApiResponse.Failure): String
}

/**
 * Resolves [apiErrorMessageRes] in the language the user picked (see [LocaleManager]), read on every
 * call so a language switch takes effect without restarting. Holds the application [Context] only.
 */
class ResourceApiErrorMessages(private val context: Context) : ApiErrorMessages {
    override fun messageFor(failure: ApiResponse.Failure): String {
        val res = apiErrorMessageRes(failure.errorCode) ?: return failure.message
        return LocaleManager.wrapContext(context).getString(res)
    }
}
