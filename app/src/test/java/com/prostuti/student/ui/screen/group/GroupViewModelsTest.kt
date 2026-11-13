package com.prostuti.student.ui.screen.group

import com.prostuti.student.R
import com.prostuti.student.data.model.enums.GroupRole
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.repository.fake.FakeGroupRepository
import com.prostuti.student.data.repository.fake.FakeGroupRepository.Companion.group
import com.prostuti.student.data.repository.fake.FakeGroupRepository.Companion.member
import com.prostuti.student.data.repository.fake.FakeGroupRepository.Companion.page
import com.prostuti.student.data.repository.fake.FakeRoutineRepository
import com.prostuti.student.data.repository.fake.FakeUserRepository
import com.prostuti.student.data.model.response.RoutineWithSlots
import com.prostuti.student.domain.routine.FetchActiveRoutineUseCase
import com.prostuti.student.domain.routine.FetchSlotsUseCase
import com.prostuti.student.domain.routine.DescribeTopicGroupingUseCase
import com.prostuti.student.domain.routine.RescheduleSlotUseCase
import com.prostuti.student.domain.routine.DeleteAdhocExamUseCase
import com.prostuti.student.data.repository.fake.FakeQbCurriculumRepository
import com.prostuti.student.domain.GetMeUseCase
import com.prostuti.student.domain.group.AddGroupMemberUseCase
import com.prostuti.student.domain.group.CreateGroupUseCase
import com.prostuti.student.domain.group.FetchGroupMembersUseCase
import com.prostuti.student.domain.group.FetchGroupUseCase
import com.prostuti.student.domain.group.FetchJoinRequestsUseCase
import com.prostuti.student.domain.group.FetchMyGroupsUseCase
import com.prostuti.student.domain.group.JoinGroupByCodeUseCase
import com.prostuti.student.domain.group.LeaveGroupUseCase
import com.prostuti.student.domain.group.RemoveGroupMemberUseCase
import com.prostuti.student.domain.group.SetGroupMemberRoleUseCase
import com.prostuti.student.ui.screen.group.component.GroupNotice
import com.prostuti.student.util.ApiErrorMessages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Shows which path produced a message: "mapped:<code>". */
internal val testErrorMessages = object : ApiErrorMessages {
    override fun messageFor(failure: ApiResponse.Failure) = "mapped:${failure.errorCode}"
}

@OptIn(ExperimentalCoroutinesApi::class)
class GroupViewModelsTest {
    private val dispatcher = StandardTestDispatcher()
    private val repo = FakeGroupRepository()
    private val routines = FakeRoutineRepository()
    private val noticeBus = GroupNoticeBus()
    private val messages = testErrorMessages

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun listVm() = GroupListViewModel(FetchMyGroupsUseCase(repo), CreateGroupUseCase(repo), JoinGroupByCodeUseCase(repo), FetchSlotsUseCase(routines), messages)
    private fun detailVm() = GroupDetailViewModel(
        "g1", FetchGroupUseCase(repo), FetchGroupMembersUseCase(repo), FetchJoinRequestsUseCase(repo), LeaveGroupUseCase(repo),
        FetchActiveRoutineUseCase(routines), FetchSlotsUseCase(routines), DescribeTopicGroupingUseCase(FakeQbCurriculumRepository()),
        RescheduleSlotUseCase(routines), DeleteAdhocExamUseCase(routines), io.mockk.mockk(),
        com.prostuti.student.domain.routine.FetchPastSlotsUseCase(routines), com.prostuti.student.data.repository.DummyGroupExamStatsProvider(), noticeBus, messages,
    )
    private fun membersVm(me: String? = null) = GroupMembersViewModel(
        "g1", FetchGroupUseCase(repo), FetchGroupMembersUseCase(repo), AddGroupMemberUseCase(repo),
        SetGroupMemberRoleUseCase(repo), RemoveGroupMemberUseCase(repo),
        GetMeUseCase(FakeUserRepository(listOfNotNull(me?.let { ApiResponse.Success(FakeUserRepository.me().copy(userId = it)) }))),
        messages,
    )

    // ── GroupList ──

    @Test fun `list loads every page of my groups`() = runTest(dispatcher) {
        repo.myGroupsPages = mapOf(
            1 to ApiResponse.Success(listOf(group("a")), metaData = page(totalPages = 2)),
            2 to ApiResponse.Success(listOf(group("b")), metaData = page(totalPages = 2)),
        )
        val vm = listVm()
        advanceUntilIdle()

        assertEquals(listOf("a", "b"), vm.state.value.groups.map { it.id })
        assertEquals(listOf(1, 2), repo.myGroupsPagesAsked)
        assertFalse(vm.state.value.isLoading)
    }

    @Test fun `a list failure shows the mapped message`() = runTest(dispatcher) {
        repo.myGroupsPages = mapOf(1 to ApiResponse.Failure("nope", "INSUFFICIENT_SCOPE"))
        val vm = listVm()
        advanceUntilIdle()

        assertEquals("mapped:INSUFFICIENT_SCOPE", vm.state.value.errors.values.single())
        assertTrue(vm.state.value.groups.isEmpty())
    }

    @Test fun `creating a group sends trimmed fields, closes the sheet and opens it`() = runTest(dispatcher) {
        val vm = listVm()
        advanceUntilIdle()
        vm.openCreateJoinSheet()

        vm.createGroup("  HSC 26-27 ", "   ", isPublic = true)
        advanceUntilIdle()

        val sent = repo.lastCreate!!
        assertEquals("HSC 26-27", sent.name)
        assertNull("a blank description is not sent", sent.description)
        assertTrue(sent.isPublic)
        assertFalse(vm.state.value.showCreateJoinSheet)
        assertEquals("new", vm.state.value.navigateToGroupId)

        vm.consumeNavigation()
        assertNull(vm.state.value.navigateToGroupId)
    }

    @Test fun `a join failure stays on the sheet with the mapped message`() = runTest(dispatcher) {
        repo.joinResult = ApiResponse.Failure("bad", "INVALID_GROUP_CODE")
        val vm = listVm()
        advanceUntilIdle()
        vm.openCreateJoinSheet()

        vm.joinGroup("k7m2 xq9p")
        advanceUntilIdle()

        assertEquals("K7M2 XQ9P", repo.lastJoinCode)
        assertTrue(vm.state.value.showCreateJoinSheet)
        assertEquals("mapped:INVALID_GROUP_CODE", vm.state.value.createJoinError)
        assertNull(vm.state.value.navigateToGroupId)
    }

    // ── GroupDetail ──

    @Test fun `detail loads the group and a four-member preview`() = runTest(dispatcher) {
        repo.groupResult = ApiResponse.Success(group(role = GroupRole.ADMIN, inviteCode = "K7M2XQ9P"))
        repo.membersPages = mapOf(1 to ApiResponse.Success(listOf(member("u1", role = GroupRole.ADMIN), member("u2"))))
        val vm = detailVm()
        advanceUntilIdle()

        assertEquals("K7M2XQ9P", vm.state.value.group?.inviteCode)
        assertEquals(2, vm.state.value.memberPreview.size)
        assertEquals(listOf(1 to 4), repo.membersAsked)
        assertTrue(vm.state.value.errors.isEmpty())
    }

    @Test fun `an admin of a public group sees the pending request count`() = runTest(dispatcher) {
        repo.groupResult = ApiResponse.Success(group(role = GroupRole.ADMIN, isPublic = true))
        repo.joinRequestsResult = { _, _ -> ApiResponse.Success(listOf(FakeGroupRepository.request("r1")), metaData = page(totalPages = 7, totalItems = 7)) }
        val vm = detailVm()
        advanceUntilIdle()

        assertEquals(7, vm.state.value.pendingRequestCount)
        assertEquals(1, repo.joinRequestsAsked.single().third)
    }

    @Test fun `members and private groups never ask for join requests`() = runTest(dispatcher) {
        repo.groupResult = ApiResponse.Success(group(role = GroupRole.MEMBER, isPublic = true))
        detailVm()
        advanceUntilIdle()
        repo.groupResult = ApiResponse.Success(group(role = GroupRole.ADMIN, isPublic = false))
        detailVm()
        advanceUntilIdle()

        assertTrue(repo.joinRequestsAsked.isEmpty())
    }

    @Test fun `a notice left by the builder shows once`() = runTest(dispatcher) {
        noticeBus.post("g1", GroupNotice(R.string.routine_created_group))
        val vm = detailVm()
        advanceUntilIdle()
        assertEquals(R.string.routine_created_group, vm.state.value.notice?.res)

        vm.consumeNotice()
        vm.refresh()
        advanceUntilIdle()
        assertNull(vm.state.value.notice)
    }

    @Test fun `GROUP_NOT_FOUND closes the screen`() = runTest(dispatcher) {
        repo.groupResult = ApiResponse.Failure("gone", "GROUP_NOT_FOUND")
        val vm = detailVm()
        advanceUntilIdle()

        assertTrue(vm.state.value.isGone)
        vm.consumeGone()
        assertFalse(vm.state.value.isGone)
    }

    @Test fun `a member-preview failure keeps the group on screen`() = runTest(dispatcher) {
        repo.membersPages = mapOf(1 to ApiResponse.Failure("down", "NETWORK_ERROR"))
        val vm = detailVm()
        advanceUntilIdle()

        assertEquals("g1", vm.state.value.group?.id)
        assertFalse(vm.state.value.isGone)
        assertEquals("mapped:NETWORK_ERROR", vm.state.value.errors.values.single())
    }

    @Test fun `leaving closes the group's screens`() = runTest(dispatcher) {
        val vm = detailVm()
        advanceUntilIdle()

        vm.leave()
        advanceUntilIdle()

        assertTrue(vm.state.value.isGone)
    }

    @Test fun `the last admin can't leave`() = runTest(dispatcher) {
        repo.leaveResult = ApiResponse.Failure("last", "LAST_ADMIN")
        val vm = detailVm()
        advanceUntilIdle()

        vm.leave()
        advanceUntilIdle()

        assertFalse(vm.state.value.isGone)
        assertFalse(vm.state.value.isLeaving)
        assertEquals("mapped:LAST_ADMIN", vm.state.value.errors.values.single())
    }

    // ── GroupMembers ──

    @Test fun `members marks me and filters by username`() = runTest(dispatcher) {
        repo.membersPages = mapOf(1 to ApiResponse.Success(listOf(member("me", username = "muwy"), member("u2", username = "rafi"))))
        val vm = membersVm(me = "me")
        advanceUntilIdle()

        assertEquals("me", vm.state.value.myUserId)
        assertEquals(2, vm.state.value.filtered.size)
        vm.setQuery(" RAF ")
        assertEquals(listOf("rafi"), vm.state.value.filtered.map { it.username })
    }

    @Test fun `adding a member normalizes the phone, closes the sheet and reloads`() = runTest(dispatcher) {
        repo.groupResult = ApiResponse.Success(group(role = GroupRole.ADMIN))
        val vm = membersVm()
        advanceUntilIdle()
        val loadsBefore = repo.groupCalls
        vm.openAddSheet()

        vm.addMember("01712345678")
        advanceUntilIdle()

        assertEquals("+8801712345678", repo.lastAddIdentifier)
        assertFalse(vm.state.value.showAddSheet)
        assertEquals(GroupNotice(R.string.group_member_added, "added_user"), vm.state.value.notice)
        assertTrue(repo.groupCalls > loadsBefore)
    }

    @Test fun `an add failure stays on the sheet`() = runTest(dispatcher) {
        repo.addMemberResult = ApiResponse.Failure("no", "USER_NOT_REGISTERED")
        val vm = membersVm()
        advanceUntilIdle()
        vm.openAddSheet()

        vm.addMember("someone@example.com")
        advanceUntilIdle()

        assertTrue(vm.state.value.showAddSheet)
        assertEquals("mapped:USER_NOT_REGISTERED", vm.state.value.addError)
    }

    @Test fun `making an admin sends the role and shows a notice`() = runTest(dispatcher) {
        val target = member("u2", username = "rafi")
        repo.membersPages = mapOf(1 to ApiResponse.Success(listOf(target)))
        val vm = membersVm()
        advanceUntilIdle()

        vm.makeAdmin(target)
        advanceUntilIdle()

        assertEquals("u2" to GroupRole.ADMIN, repo.lastRole)
        assertEquals(GroupNotice(R.string.group_member_now_admin, "rafi"), vm.state.value.notice)
        assertNull(vm.state.value.busyUserId)
    }

    @Test fun `LAST_ADMIN on demote shows the mapped message without reloading`() = runTest(dispatcher) {
        repo.setRoleResult = ApiResponse.Failure("last", "LAST_ADMIN")
        val vm = membersVm()
        advanceUntilIdle()
        val loadsBefore = repo.groupCalls

        vm.removeAdmin(member("me", role = GroupRole.ADMIN))
        advanceUntilIdle()

        assertEquals("mapped:LAST_ADMIN", vm.state.value.errors.values.single())
        assertEquals(loadsBefore, repo.groupCalls)
    }

    @Test fun `MEMBER_NOT_FOUND on remove reloads the stale list`() = runTest(dispatcher) {
        repo.removeResult = ApiResponse.Failure("gone", "MEMBER_NOT_FOUND")
        val vm = membersVm()
        advanceUntilIdle()
        val loadsBefore = repo.groupCalls

        vm.remove(member("u9"))
        advanceUntilIdle()

        assertTrue(repo.groupCalls > loadsBefore)
    }
}
