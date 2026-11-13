package com.prostuti.student.data.model.enums

/** `error.code` values the groups, routines and notifications APIs document (plus the shared RBAC ones). */
object GroupRoutineErrorCode {
    // RBAC
    const val INSUFFICIENT_SCOPE = "INSUFFICIENT_SCOPE"
    const val AUTHZ_UNAVAILABLE = "AUTHZ_UNAVAILABLE"

    // Groups
    const val GROUP_NOT_FOUND = "GROUP_NOT_FOUND"
    const val NOT_GROUP_ADMIN = "NOT_GROUP_ADMIN"
    const val INVALID_GROUP_CODE = "INVALID_GROUP_CODE"
    const val ALREADY_MEMBER = "ALREADY_MEMBER"
    const val GROUP_FULL = "GROUP_FULL"
    const val LAST_ADMIN = "LAST_ADMIN"
    const val MAX_MEMBERS_TOO_LOW = "MAX_MEMBERS_TOO_LOW"
    const val INVALID_IDENTIFIER = "INVALID_IDENTIFIER"
    const val USER_NOT_REGISTERED = "USER_NOT_REGISTERED"
    const val MEMBER_NOT_FOUND = "MEMBER_NOT_FOUND"
    const val JOIN_REQUEST_EXISTS = "JOIN_REQUEST_EXISTS"
    const val JOIN_REQUEST_NOT_FOUND = "JOIN_REQUEST_NOT_FOUND"
    const val JOIN_REQUEST_DECIDED = "JOIN_REQUEST_DECIDED"
    const val CODE_GENERATION_FAILED = "CODE_GENERATION_FAILED"

    // Routines and slots
    const val ROUTINE_EXISTS = "ROUTINE_EXISTS"
    const val ROUTINE_NOT_FOUND = "ROUTINE_NOT_FOUND"
    const val ROUTINE_ALREADY_DELETED = "ROUTINE_ALREADY_DELETED"
    const val START_IN_PAST = "START_IN_PAST"
    const val DATE_IN_PAST = "DATE_IN_PAST"
    const val INVALID_WINDOW = "INVALID_WINDOW"
    const val WINDOW_IN_PAST = "WINDOW_IN_PAST"
    const val NO_EXAM_DAYS = "NO_EXAM_DAYS"
    const val NO_TOPICS_TO_SCHEDULE = "NO_TOPICS_TO_SCHEDULE"
    const val CURRICULUM_MISMATCH = "CURRICULUM_MISMATCH"
    const val SUBJECT_NOT_FOUND = "SUBJECT_NOT_FOUND"
    const val SLOT_NOT_FOUND = "SLOT_NOT_FOUND"
    const val SLOT_NOT_OPEN = "SLOT_NOT_OPEN"
    const val SLOT_ALREADY_OPEN = "SLOT_ALREADY_OPEN"
    const val SLOT_HAS_ATTEMPTS = "SLOT_HAS_ATTEMPTS"
    const val NOT_AN_ADHOC_EXAM = "NOT_AN_ADHOC_EXAM"
    const val SLOT_NO_QUESTIONS = "SLOT_NO_QUESTIONS"
    const val OUTSIDE_ROUTINE_PERIOD = "OUTSIDE_ROUTINE_PERIOD"
    const val INVALID_DATE_RANGE = "INVALID_DATE_RANGE"

    // Notifications
    /** App-side, not from the API: no submitted live attempt of mine on a slot's exam. */
    const val NO_ATTEMPT = "NO_ATTEMPT"

    const val NOTIFICATION_NOT_FOUND = "NOTIFICATION_NOT_FOUND"
}
