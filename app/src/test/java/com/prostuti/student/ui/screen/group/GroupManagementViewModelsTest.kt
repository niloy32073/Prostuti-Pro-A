package com.prostuti.student.ui.screen.group

import com.prostuti.student.data.model.enums.GroupRole
import com.prostuti.student.data.model.enums.JoinRequestStatus
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.repository.fake.FakeGroupRepository
import com.prostuti.student.data.repository.fake.FakeGroupRepository.Companion.group
import com.prostuti.student.data.repository.fake.FakeGroupRepository.Companion.request
import com.prostuti.student.domain.group.ApproveJoinRequestUseCase
import com.prostuti.student.domain.group.DeleteGroupUseCase
import com.prostuti.student.domain.group.ExploreGroupsUseCase
import com.prostuti.student.domain.group.FetchGroupUseCase
import com.prostuti.student.domain.group.FetchJoinRequestsUseCase
import com.prostuti.student.domain.group.LeaveGroupUseCase
import com.prostuti.student.domain.group.RegenerateGroupCodeUseCase
import com.prostuti.student.domain.group.RejectJoinRequestUseCase
import com.prostuti.student.domain.group.RequestToJoinGroupUseCase
import com.prostuti.student.domain.group.UpdateGroupUseCase
import com.prostuti.student.ui.screen.group.GroupSettingsUiState.MaxMembersProblem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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

@OptIn(ExperimentalCoroutinesApi::class)
class GroupManagementViewModelsTest {
    private val dispatcher = StandardTestDispatcher()
    private val repo = FakeGroupRepository()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun settingsVm() = GroupSettingsViewModel(
        "g1", FetchGroupUseCase(repo), UpdateGroupUseCase(repo), RegenerateGroupCodeUseCase(repo),
        LeaveGroupUseCase(repo), DeleteGroupUseCase(repo), testErrorMessages,
    )
    private fun exploreVm() = GroupExploreViewModel(ExploreGroupsUseCase(repo), RequestToJoinGroupUseCase(repo), testErrorMessages)
    private fun requestsVm() = GroupJoinRequestsViewModel(
        "g1", FetchJoinRequestsUseCase(repo), ApproveJoinRequestUseCase(repo), RejectJoinRequestUseCase(repo), testErrorMessages,
    )

    // ── GroupSettings ──

    @Test fun `settings fills the form and sends only what changed`() = runTest(dispatcher) {
        repo.groupResult = ApiResponse.Success(group(role = GroupRole.ADMIN, isPublic = true, maxMembers = 500, memberCount = 40, description = "old"))
        val vm = settingsVm()
        advanceUntilIdle()
        assertNull("nothing changed yet", vm.state.value.changes)
        assertFalse(vm.state.value.canSave)

        vm.onMaxMembersChange("300")
        vm.onDescriptionChange("  ")
        vm.save()
        advanceUntilIdle()

        val sent = repo.lastUpdate!!
        assertEquals(300, sent.maxMembers)
        assertEquals("a cleared description is sent as empty", "", sent.description)
        assertNull(sent.name)
        assertNull(sent.isPublic)
    }

    @Test fun `the member cap can't go below the member count or outside 2-5000`() = runTest(dispatcher) {
        repo.groupResult = ApiResponse.Success(group(role = GroupRole.ADMIN, memberCount = 40))
        val vm = settingsVm()
        advanceUntilIdle()

        vm.onMaxMembersChange("39")
        assertEquals(MaxMembersProblem.BELOW_MEMBER_COUNT, vm.state.value.maxMembersProblem)
        vm.onMaxMembersChange("9000")
        assertEquals(MaxMembersProblem.OUT_OF_RANGE, vm.state.value.maxMembersProblem)
        vm.onMaxMembersChange("")
        assertEquals(MaxMembersProblem.NOT_A_NUMBER, vm.state.value.maxMembersProblem)
        assertFalse(vm.state.value.canSave)
        vm.onMaxMembersChange("4a0")
        assertEquals("only digits are kept", "40", vm.state.value.form.maxMembers)
    }

    @Test fun `turning a public group private is flagged`() = runTest(dispatcher) {
        repo.groupResult = ApiResponse.Success(group(role = GroupRole.ADMIN, isPublic = true))
        val vm = settingsVm()
        advanceUntilIdle()

        vm.onPublicChange(false)

        assertTrue(vm.state.value.isTurningPrivate)
        assertEquals(false, vm.state.value.changes?.isPublic)
    }

    @Test fun `a non-admin is sent back`() = runTest(dispatcher) {
        repo.groupResult = ApiResponse.Success(group(role = GroupRole.MEMBER))
        val vm = settingsVm()
        advanceUntilIdle()
        assertTrue(vm.state.value.isNotAdmin)
    }

    @Test fun `deleting closes the group`() = runTest(dispatcher) {
        repo.groupResult = ApiResponse.Success(group(role = GroupRole.ADMIN))
        val vm = settingsVm()
        advanceUntilIdle()

        vm.delete()
        advanceUntilIdle()

        assertTrue(vm.state.value.isClosed)
        assertNull(vm.state.value.running)
    }

    @Test fun `regenerating shows the new code`() = runTest(dispatcher) {
        repo.groupResult = ApiResponse.Success(group(role = GroupRole.ADMIN, inviteCode = "OLD00001"))
        val vm = settingsVm()
        advanceUntilIdle()
        repo.regenerateResult = ApiResponse.Success(group(role = GroupRole.ADMIN, inviteCode = "NEW00001"))

        vm.regenerateCode()
        advanceUntilIdle()

        assertEquals("NEW00001", vm.state.value.group?.inviteCode)
        assertTrue(vm.state.value.notice != null)
    }

    @Test fun `losing admin rights mid-action sends the admin back`() = runTest(dispatcher) {
        repo.groupResult = ApiResponse.Success(group(role = GroupRole.ADMIN))
        val vm = settingsVm()
        advanceUntilIdle()
        repo.deleteResult = ApiResponse.Failure("no", "NOT_GROUP_ADMIN")

        vm.delete()
        advanceUntilIdle()

        assertTrue(vm.state.value.isNotAdmin)
        assertFalse(vm.state.value.isClosed)
    }

    // ── Explore ──

    @Test fun `explore buttons follow role, pending request and capacity`() {
        assertEquals(ExploreAction.OPEN, group(role = GroupRole.MEMBER).exploreAction())
        assertEquals(ExploreAction.REQUESTED, group(role = null, hasPendingRequest = true).exploreAction())
        assertEquals(ExploreAction.FULL, group(role = null, memberCount = 50, maxMembers = 50).exploreAction())
        assertEquals(ExploreAction.REQUEST, group(role = null).exploreAction())
    }

    @Test fun `typing searches once after the debounce`() = runTest(dispatcher) {
        val vm = exploreVm()
        advanceUntilIdle()
        assertEquals(listOf<String?>(null), repo.exploreQueries)

        vm.onQueryChange("H")
        vm.onQueryChange("HS")
        vm.onQueryChange("HSC ")
        advanceTimeBy(100)
        assertEquals(1, repo.exploreQueries.size)
        advanceUntilIdle()

        assertEquals(listOf(null, "HSC"), repo.exploreQueries)
    }

    @Test fun `a join request turns the card requested at once`() = runTest(dispatcher) {
        repo.exploreResult = { _, _ -> ApiResponse.Success(listOf(group(id = "p1", role = null, isPublic = true))) }
        val vm = exploreVm()
        advanceUntilIdle()

        vm.requestToJoin("p1")
        assertEquals("optimistic", ExploreAction.REQUESTED, vm.state.value.groups.single().exploreAction())
        advanceUntilIdle()

        assertEquals(ExploreAction.REQUESTED, vm.state.value.groups.single().exploreAction())
        assertTrue(vm.state.value.requesting.isEmpty())
    }

    @Test fun `a failed join request goes back and shows why`() = runTest(dispatcher) {
        repo.exploreResult = { _, _ -> ApiResponse.Success(listOf(group(id = "p1", role = null, isPublic = true))) }
        repo.requestToJoinResult = ApiResponse.Failure("full", "GROUP_FULL")
        val vm = exploreVm()
        advanceUntilIdle()

        vm.requestToJoin("p1")
        advanceUntilIdle()

        assertEquals(ExploreAction.REQUEST, vm.state.value.groups.single().exploreAction())
        assertEquals("mapped:GROUP_FULL", vm.state.value.errors.values.single())
    }

    @Test fun `an existing request stays requested without an error`() = runTest(dispatcher) {
        repo.exploreResult = { _, _ -> ApiResponse.Success(listOf(group(id = "p1", role = null, isPublic = true))) }
        repo.requestToJoinResult = ApiResponse.Failure("exists", "JOIN_REQUEST_EXISTS")
        val vm = exploreVm()
        advanceUntilIdle()

        vm.requestToJoin("p1")
        advanceUntilIdle()

        assertEquals(ExploreAction.REQUESTED, vm.state.value.groups.single().exploreAction())
        assertTrue(vm.state.value.errors.isEmpty())
    }

    @Test fun `explore pages until total_pages`() = runTest(dispatcher) {
        repo.exploreResult = { _, p -> ApiResponse.Success(listOf(group(id = "p$p", role = null)), metaData = FakeGroupRepository.page(totalPages = 2)) }
        val vm = exploreVm()
        advanceUntilIdle()
        assertTrue(vm.state.value.canLoadMore)

        vm.loadMore()
        advanceUntilIdle()

        assertEquals(listOf("p1", "p2"), vm.state.value.groups.map { it.id })
        assertFalse(vm.state.value.canLoadMore)
    }

    // ── Join requests ──

    @Test fun `approving removes the request from the pending list`() = runTest(dispatcher) {
        repo.joinRequestsResult = { _, _ -> ApiResponse.Success(listOf(request("r1"), request("r2"))) }
        val vm = requestsVm()
        advanceUntilIdle()

        vm.approve(vm.state.value.requests.first())
        advanceUntilIdle()

        assertEquals(listOf("r2"), vm.state.value.requests.map { it.id })
        assertTrue(vm.state.value.notice != null)
    }

    @Test fun `an already decided request reloads the list`() = runTest(dispatcher) {
        repo.joinRequestsResult = { _, _ -> ApiResponse.Success(listOf(request("r1"))) }
        repo.decideResult = ApiResponse.Failure("decided", "JOIN_REQUEST_DECIDED")
        val vm = requestsVm()
        advanceUntilIdle()
        val asked = repo.joinRequestsAsked.size

        vm.reject(vm.state.value.requests.single())
        advanceUntilIdle()

        assertTrue(repo.joinRequestsAsked.size > asked)
        assertEquals("mapped:JOIN_REQUEST_DECIDED", vm.state.value.errors.values.single())
    }

    @Test fun `switching tabs loads that status`() = runTest(dispatcher) {
        val vm = requestsVm()
        advanceUntilIdle()

        vm.selectStatus(JoinRequestStatus.APPROVED)
        advanceUntilIdle()

        assertEquals(JoinRequestStatus.APPROVED, repo.joinRequestsAsked.last().first)
    }
}
