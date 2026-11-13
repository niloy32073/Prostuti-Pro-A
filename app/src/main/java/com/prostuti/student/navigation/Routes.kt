package com.prostuti.student.navigation

import androidx.navigation3.runtime.NavKey
import com.prostuti.student.data.model.enums.TimedExamKind
import com.prostuti.student.ui.screen.routine.builder.RoutineBuilderMode
import com.prostuti.student.ui.screen.routine.builder.RoutineOwner
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
// Carries the reset OTP: the server checks it together with the new password in one call.
@Serializable data class ResetNewPassword(val identifier: String, val code: String) : RootRoute

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
@Serializable data class ChapterBrowse(val subjectId: String, val nameBn: String, val nameEn: String = "") : QBRoute

// Exam Engine — the shared attempt flow for practice/live/group exams. Root-level because it's
// reachable from several places (QB model tests, the practice builder below, and later live/group
// entry points): the caller stashes the already-fetched question set in ExamAttemptStore and
// navigates here with just the attemptId. Result/AnswerSheet/Leaderboard are common across modes
// too — the backend decides what's unlocked (show_explanation_after, leaderboard job status).
// lockResultUntil: a group exam's window end (UTC, from the slot). Until then the result, answer sheet
// and analysis show the locked view instead (GROUP_ROUTINE_PLAN.md §6.12, D1). Null = never locked.
@Serializable data class ExamAttempt(val attemptId: String, val titleBn: String, val lockResultUntil: String? = null) : RootRoute
// fromHistory: opened from Exam History — shows Back + "retake" instead of "go home".
@Serializable data class ExamResult(
    val attemptId: String,
    val titleBn: String,
    val fromHistory: Boolean = false,
    val lockResultUntil: String? = null,
) : RootRoute
// topicFilter: a topic ID or ANSWER_SHEET_NO_TOPIC to show only that topic's questions; null = all.
@Serializable data class ExamAnswerSheet(
    val attemptId: String,
    val titleBn: String,
    val topicFilter: String? = null,
    /** The topic's name in the language shown when it was opened. */
    val topicName: String? = null,
    val lockResultUntil: String? = null,
    val besideResultOf: String? = null,
) : RootRoute
// besideResultOf: the attempt whose ExamResult opened this screen. Set = shown in the pane beside
// that result on medium and wider windows; null = a screen of its own (e.g. from group ranking).
@Serializable data class AttemptAnalysis(
    val attemptId: String,
    val titleBn: String,
    val lockResultUntil: String? = null,
    val besideResultOf: String? = null,
) : RootRoute
@Serializable data class ExamLeaderboardRoute(val examId: String, val titleBn: String, val besideResultOf: String? = null) : RootRoute

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
/** Admin only; a non-admin is sent back by the screen itself (the server answers NOT_GROUP_ADMIN). */
@Serializable data class GroupSettings(val groupId: String) : RootRoute
/** Admin only, public groups. */
@Serializable data class GroupJoinRequests(val groupId: String) : RootRoute
/** The in-app inbox, opened from Home's bell. */
@Serializable data object Notifications : RootRoute
/** One group exam's ranking; [closesAt] (the slot's window end) says whether a ranking can exist yet. */
@Serializable data class GroupRanking(val groupId: String, val examId: String, val titleBn: String, val closesAt: String? = null) : RootRoute
/**
 * The routine builder: a personal routine, a group routine or a one-off group exam (ADHOC, groups only).
 * Its steps live inside one screen, so the (possibly huge) topic picks never go into a route.
 */
@Serializable data class RoutineBuilder(val owner: RoutineOwner, val mode: RoutineBuilderMode = RoutineBuilderMode.ROUTINE) : RootRoute

// ── Profile ── root-level, full-screen, opened from Home's avatar. Shows GET auth/me.
@Serializable data object Profile : RootRoute

// ── Bookmark history ── root-level, full-screen, opened from the Settings tab.
@Serializable data object BookmarkHistory : RootRoute

// ── Exam history ── root-level, full-screen, opened from the Settings tab's "My activity" section.
@Serializable data object ExamHistory : RootRoute

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

// ── Playground ── root-level, full-screen, opened from Home's quick menu. The graph builder is
// its first tool.
@Serializable data object PlaygroundHub : RootRoute
/** [savedGraphId] opens a graph from "My graphs"; null starts a new one. */
@Serializable data class GraphBuilder(val savedGraphId: Long? = null) : RootRoute
