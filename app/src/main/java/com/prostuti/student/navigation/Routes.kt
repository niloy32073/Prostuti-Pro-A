package com.prostuti.student.navigation

import androidx.navigation3.runtime.NavKey
import com.prostuti.student.data.model.enums.TimedExamKind
import kotlinx.serialization.Serializable

// Root-level destinations. RootNavDisplay switches between these based on SharedViewModel's
// first-time/logged-in state; everything under the Auth group is reachable from Intro.
@Serializable sealed interface RootRoute : NavKey

@Serializable data object Intro : RootRoute

@Serializable data object Auth : RootRoute
@Serializable data class SignIn(val identifier: String, val firstName: String?) : RootRoute
@Serializable data class SignUp(val identifier: String) : RootRoute
@Serializable data class VerifyOtp(val identifier: String) : RootRoute
// Carries the identifier straight from AuthLogin's "forgot password?" link — no re-typing it
// here. Also handles the reset OTP inline (send -> verify) rather than a separate screen.
@Serializable data class ResetRequest(val identifier: String) : RootRoute
@Serializable data class ResetNewPassword(val resetToken: String) : RootRoute

// Post-auth home — hosts MainNavDisplay's own bottom-nav/rail switching between MainRoute tabs.
@Serializable data object Main : RootRoute

// Level 1: Main (bottom nav on compact, rail on medium/expanded — five HSC-app destinations).
@Serializable sealed interface MainRoute : NavKey
@Serializable data object Home : MainRoute
@Serializable data object QB : MainRoute
@Serializable data object Leaderboard : MainRoute
@Serializable data object Routine : MainRoute
@Serializable data object Settings : MainRoute

// Level 2: QB nested destinations.
@Serializable sealed interface QBRoute : NavKey
@Serializable data object SubjectList : QBRoute
@Serializable data class SubjectType(val subjectId: String, val nameBn: String, val nameEn: String) : QBRoute
@Serializable data class MCQExamList(val subjectId: String, val nameBn: String, val nameEn: String) : QBRoute
@Serializable data class QuestionReader(val examId: String, val titleBn: String) : QBRoute
@Serializable data class ChapterBrowse(val subjectId: String, val nameBn: String) : QBRoute

// Exam Engine — the shared attempt flow for practice/live/group exams. Root-level because it's
// reachable from several places (QB model tests, the practice builder below, and later live/group
// entry points): the caller stashes the already-fetched question set in ExamAttemptStore and
// navigates here with just the attemptId. Result/AnswerSheet/Leaderboard are common across modes
// too — the backend decides what's unlocked (show_explanation_after, leaderboard job status).
@Serializable data class ExamAttempt(val attemptId: String, val titleBn: String) : RootRoute
@Serializable data class ExamResult(val attemptId: String, val titleBn: String) : RootRoute
@Serializable data class ExamAnswerSheet(val attemptId: String, val titleBn: String) : RootRoute
@Serializable data class ExamLeaderboardRoute(val examId: String, val titleBn: String) : RootRoute

// Practice Builder — subject -> chapter/topic -> properties, then generates a practice exam and
// hands off into the Exam Engine above. Also root-level (entered from Home's quick action).
@Serializable data object PracticeSubjectSelect : RootRoute
@Serializable data class PracticeChapterSelect(val subjectIds: List<String>) : RootRoute
// chapterIds here is always derived from topicIds (every chapter owning a selected topic) — the
// backend requires chapter_ids whenever topic_ids is sent and ANDs the two together.
@Serializable data class PracticeExamProperties(
    val subjectIds: List<String>,
    val chapterIds: List<String>,
    val topicIds: List<String>
) : RootRoute

// ── Group exam ── root-level, full-screen (no bottom nav), entered from Home's quick menu and
// from its own "লাইভ এক্সাম"/"গ্রুপ এক্সাম" upcoming cards.
@Serializable data object GroupList : RootRoute
@Serializable data class GroupDetail(val groupId: String) : RootRoute
@Serializable data class GroupRoutineCalendar(val groupId: String) : RootRoute
@Serializable data class GroupPastExams(val groupId: String) : RootRoute
@Serializable data class GroupMembers(val groupId: String) : RootRoute

/**
 * The 3-step group-routine builder — admin-only. Every destination carries [groupId] so
 * `AdminGate` (navigation/AdminGate.kt) can check the caller is that specific group's admin
 * before it renders, and refuses re-entry once a routine already exists (the create flow is
 * one-shot: a routine can never be edited after it's locked).
 */
@Serializable sealed interface GroupAdminRoute : RootRoute {
    val groupId: String
}
@Serializable data class GroupRoutineSubjectSelect(override val groupId: String) : GroupAdminRoute
@Serializable data class GroupRoutineChapterSelect(
    override val groupId: String,
    val subjectIds: List<String>
) : GroupAdminRoute
@Serializable data class GroupRoutineWindowConfig(
    override val groupId: String,
    val subjectIds: List<String>,
    val chapterIds: List<String>,
    val topicIds: List<String>
) : GroupAdminRoute

// ── Live exam ──
@Serializable data object LiveExamList : RootRoute
@Serializable data class LiveExamDetail(val liveExamId: String) : RootRoute

// ── Shared timed-exam attempt/result/ranking/answer-sheet — live and group exams both finalize
// through these same four screens (see TimedExamRepository). [kind] only changes labels and the
// back-destination, never the mechanics. ──
@Serializable data class TimedExamAttempt(val examId: String, val kind: TimedExamKind) : RootRoute
@Serializable data class LockedResult(val examId: String, val kind: TimedExamKind) : RootRoute
@Serializable data class WindowRanking(val examId: String, val kind: TimedExamKind) : RootRoute
// Always my own answer sheet — no participantId, see the no-peeking-at-others'-answers decision.
@Serializable data class TimedAnswerSheet(val examId: String, val kind: TimedExamKind) : RootRoute
