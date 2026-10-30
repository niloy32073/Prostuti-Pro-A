package com.prostuti.student.data.repository.fake

import com.prostuti.student.data.model.enums.GroupRole
import com.prostuti.student.data.model.enums.TimedExamKind
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.CreateGroupRoutineRequest
import com.prostuti.student.data.model.response.Group
import com.prostuti.student.data.model.response.GroupDetail
import com.prostuti.student.data.model.response.GroupMember
import com.prostuti.student.data.model.response.GroupPastExam
import com.prostuti.student.data.model.response.GroupRoutineSummary
import com.prostuti.student.data.model.response.MyTimedExamSubmission
import com.prostuti.student.data.model.response.TimedExam
import com.prostuti.student.data.repository.GroupExamRepository
import com.prostuti.student.data.repository.TimedExamRepository
import com.prostuti.student.data.service.local.CreatedGroupDao
import com.prostuti.student.data.service.local.CreatedGroupEntity
import com.prostuti.student.data.service.local.GroupRoutineDao
import com.prostuti.student.data.service.local.GroupRoutineEntity
import com.prostuti.student.util.TimeProvider
import com.prostuti.student.util.toBengaliDigits
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeGroupExamRepository(
    private val createdGroupDao: CreatedGroupDao,
    private val groupRoutineDao: GroupRoutineDao,
    private val timedExamRepository: TimedExamRepository,
    private val time: TimeProvider
) : GroupExamRepository {

    override fun myGroups(): Flow<ApiResponse<List<Group>>> = flow {
        val created = createdGroupDao.getAll().map { it.toGroup() }
        emit(ApiResponse.Success(ExamDummyData.staticGroups + created))
    }

    override suspend fun groupDetail(groupId: String): ApiResponse<GroupDetail> {
        val group = findGroup(groupId) ?: return ApiResponse.Failure("গ্রুপ পাওয়া যায়নি", "NOT_FOUND")
        val routine = resolveRoutine(groupId)

        var currentExam: TimedExam? = null
        var mySubmission: MyTimedExamSubmission? = null
        if (routine != null) {
            val examId = ExamDummyData.currentExamIdFor(groupId)
            val (start, end) = timedExamRepository.resolveWindow(examId)
            val (titleBn, subtitleBn) = ExamDummyData.groupCurrentExamTitle[groupId] ?: (
                "${group.nameBn} — গ্রুপ এক্সাম" to
                    "${routine.questionCount.toBengaliDigits()}টি প্রশ্ন · প্রতি প্রশ্নে ${formatMark(routine.markPerQuestion)} মার্ক · নেগেটিভ ${formatMark(routine.negativeMark)}"
            )
            currentExam = TimedExam(
                examId = examId, kind = TimedExamKind.GROUP, titleBn = titleBn, subtitleBn = subtitleBn,
                windowStartEpochMillis = start, windowEndEpochMillis = end,
                examDurationMinutes = routine.examDurationMinutes, questionCount = routine.questionCount,
                markPerQuestion = routine.markPerQuestion,
                negativeMark = if (routine.negativeMarkingEnabled) routine.negativeMark else 0f,
                totalParticipants = group.memberCount
            )
            mySubmission = timedExamRepository.mySubmission(examId)
        }

        val pastExams = ExamDummyData.pastExamsByGroupId[groupId].orEmpty().take(3)
        val members = ExamDummyData.membersByGroupId[groupId]?.take(4)
            ?: ExamDummyData.membersForDynamicGroup(group.myRole).take(4)

        return ApiResponse.Success(GroupDetail(group, routine, currentExam, mySubmission, pastExams, members))
    }

    override suspend fun createRoutine(groupId: String, request: CreateGroupRoutineRequest): ApiResponse<Unit> {
        val group = findGroup(groupId) ?: return ApiResponse.Failure("গ্রুপ পাওয়া যায়নি", "NOT_FOUND")
        if (group.myRole != GroupRole.ADMIN) {
            return ApiResponse.Failure("এই অ্যাকশন শুধু গ্রুপ অ্যাডমিনের জন্য", "FORBIDDEN")
        }
        if (resolveRoutine(groupId) != null) {
            return ApiResponse.Failure("এই গ্রুপের রুটিন আগেই তৈরি হয়ে গেছে", "ROUTINE_LOCKED")
        }

        val now = time.nowEpochMillis()
        groupRoutineDao.upsert(
            GroupRoutineEntity(
                groupId = groupId,
                subjectIdsCsv = request.subjectIds.joinToString(","),
                chapterIdsCsv = request.chapterIds.joinToString(","),
                topicIdsCsv = request.topicIds.joinToString(","),
                routineDurationDays = request.routineDurationDays,
                examWeekDaysBitmask = request.examWeekDaysBitmask,
                windowStartMinuteOfDay = request.windowStartMinuteOfDay,
                windowEndMinuteOfDay = request.windowEndMinuteOfDay,
                questionCount = request.questionCount,
                markPerQuestion = request.markPerQuestion,
                negativeMarkingEnabled = request.negativeMarkingEnabled,
                negativeMark = request.negativeMark,
                examDurationMinutes = request.examDurationMinutes,
                createdAtEpochMillis = now
            )
        )

        // A real backend would wait for the routine's next scheduled exam day; this dummy build
        // opens today's window immediately on creation so the flow is demonstrable without
        // waiting for a real calendar day to turn into an "exam day".
        val windowLengthMillis = (request.windowEndMinuteOfDay - request.windowStartMinuteOfDay)
            .coerceAtLeast(10) * 60_000L
        timedExamRepository.registerWindow(ExamDummyData.currentExamIdFor(groupId), now, now + windowLengthMillis)

        return ApiResponse.Success(Unit)
    }

    override suspend fun createGroup(nameBn: String): ApiResponse<Group> {
        val id = "created_${time.nowEpochMillis()}"
        val code = generateJoinCode()
        val now = time.nowEpochMillis()
        createdGroupDao.upsert(CreatedGroupEntity(id, nameBn, GroupRole.ADMIN.name, code, now))
        return ApiResponse.Success(Group(id, nameBn, memberCount = 1, myRole = GroupRole.ADMIN, joinCode = code))
    }

    override suspend fun joinGroup(code: String): ApiResponse<Group> {
        val normalized = code.trim().uppercase()
        if (normalized == ExamDummyData.PUBLIC_JOIN_CODE) {
            val id = "joined_public"
            if (createdGroupDao.getById(id) != null) {
                return ApiResponse.Failure("তুমি আগেই এই গ্রুপে আছো", "ALREADY_MEMBER")
            }
            createdGroupDao.upsert(CreatedGroupEntity(id, ExamDummyData.PUBLIC_JOIN_GROUP_NAME, GroupRole.MEMBER.name, normalized, time.nowEpochMillis()))
            return ApiResponse.Success(Group(id, ExamDummyData.PUBLIC_JOIN_GROUP_NAME, memberCount = 24, myRole = GroupRole.MEMBER, joinCode = normalized))
        }
        if (createdGroupDao.findByJoinCode(normalized) != null || ExamDummyData.staticGroups.any { it.joinCode == normalized }) {
            return ApiResponse.Failure("তুমি আগেই এই গ্রুপে আছো", "ALREADY_MEMBER")
        }
        return ApiResponse.Failure("কোডটি সঠিক নয়", "INVALID_CODE")
    }

    override suspend fun pastExams(groupId: String): ApiResponse<List<GroupPastExam>> =
        ApiResponse.Success(ExamDummyData.pastExamsByGroupId[groupId].orEmpty())

    override suspend fun members(groupId: String): ApiResponse<List<GroupMember>> {
        ExamDummyData.membersByGroupId[groupId]?.let { return ApiResponse.Success(it) }
        val group = findGroup(groupId) ?: return ApiResponse.Failure("গ্রুপ পাওয়া যায়নি", "NOT_FOUND")
        return ApiResponse.Success(ExamDummyData.membersForDynamicGroup(group.myRole))
    }

    private suspend fun findGroup(groupId: String): Group? =
        ExamDummyData.staticGroups.find { it.id == groupId } ?: createdGroupDao.getById(groupId)?.toGroup()

    private suspend fun resolveRoutine(groupId: String): GroupRoutineSummary? {
        ExamDummyData.staticRoutineByGroupId[groupId]?.let { return it }
        val entity = groupRoutineDao.getByGroupId(groupId) ?: return null
        return GroupRoutineSummary(
            subjectCount = entity.subjectIdsCsv.splitNonBlank().size,
            chapterCount = entity.chapterIdsCsv.splitNonBlank().size,
            topicCount = entity.topicIdsCsv.splitNonBlank().size,
            routineDurationDays = entity.routineDurationDays,
            examWeekDaysBitmask = entity.examWeekDaysBitmask,
            windowStartMinuteOfDay = entity.windowStartMinuteOfDay,
            windowEndMinuteOfDay = entity.windowEndMinuteOfDay,
            questionCount = entity.questionCount,
            markPerQuestion = entity.markPerQuestion,
            negativeMarkingEnabled = entity.negativeMarkingEnabled,
            negativeMark = entity.negativeMark,
            examDurationMinutes = entity.examDurationMinutes
        )
    }

    private fun String.splitNonBlank(): List<String> = split(",").filter { it.isNotBlank() }

    private fun generateJoinCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    private fun formatMark(value: Float): String {
        val plain = if (value == value.toInt().toFloat()) value.toInt().toString()
        else "%.2f".format(value).trimEnd('0').trimEnd('.')
        return plain.toBengaliDigits()
    }

    private fun CreatedGroupEntity.toGroup(): Group {
        val role = GroupRole.valueOf(myRole)
        val memberCount = if (role == GroupRole.ADMIN) 1 else 24
        return Group(id, nameBn, memberCount, role, joinCode)
    }
}
