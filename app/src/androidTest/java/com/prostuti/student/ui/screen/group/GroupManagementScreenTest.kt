package com.prostuti.student.ui.screen.group

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.prostuti.student.R
import com.prostuti.student.data.model.enums.GroupRole
import com.prostuti.student.data.model.response.StudyGroup
import com.prostuti.student.data.model.response.StudyGroupMember
import com.prostuti.student.ui.screen.group.component.TAG_CONFIRM
import com.prostuti.student.ui.screen.group.component.TAG_TYPE_TO_CONFIRM
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Admin-only controls on the member list, Explore's per-card button, and the type-the-name delete guard. */
@RunWith(AndroidJUnit4::class)
class GroupManagementScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun group(role: GroupRole?, memberCount: Int = 2, maxMembers: Int = 500, pending: Boolean = false) = StudyGroup(
        id = "g1", name = "HSC 26-27", description = null, isPublic = true, maxMembers = maxMembers, memberCount = memberCount,
        createdAt = "2026-11-02T10:00:00", myRole = role, inviteCode = null, hasPendingRequest = pending,
    )

    private val members = listOf(
        StudyGroupMember(userId = "me", username = "muwy", role = GroupRole.ADMIN, joinedAt = "2026-11-02T10:05:00"),
        StudyGroupMember(userId = "u2", username = "rafi", role = GroupRole.MEMBER, joinedAt = "2026-11-02T10:06:00"),
    )

    private fun showMembers(role: GroupRole, onMakeAdmin: (StudyGroupMember) -> Unit = {}) {
        composeTestRule.setContent {
            ProstutiTheme {
                GroupMembersContent(
                    state = GroupMembersUiState(isLoading = false, group = group(role), members = members, myUserId = "me"),
                    onBack = {}, onQueryChange = {}, onAddMember = {}, onMakeAdmin = onMakeAdmin, onRemoveAdmin = {}, onRemove = {},
                    onRefresh = {}, onClearError = {},
                )
            }
        }
    }

    @Test
    fun adminCanAddMembersAndOpenAMembersMenu() {
        var promoted: String? = null
        showMembers(GroupRole.ADMIN, onMakeAdmin = { promoted = it.userId })

        composeTestRule.onNodeWithTag(TAG_ADD_MEMBER_FAB).assertIsDisplayed()
        composeTestRule.onNodeWithTag("${TAG_MEMBER_MENU_PREFIX}u2").performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.group_make_admin)).performClick()
        assertEquals("u2", promoted)
    }

    @Test
    fun memberSeesNoManagementControls() {
        showMembers(GroupRole.MEMBER)

        composeTestRule.onNodeWithTag(TAG_ADD_MEMBER_FAB).assertDoesNotExist()
        composeTestRule.onNodeWithTag("${TAG_MEMBER_MENU_PREFIX}u2").assertDoesNotExist()
        composeTestRule.onNodeWithText(context.getString(R.string.group_member_you, "muwy")).assertIsDisplayed()
    }

    @Test
    fun exploreCardsShowTheRightButton() {
        val groups = listOf(
            group(GroupRole.MEMBER).copy(id = "mine"),
            group(null, pending = true).copy(id = "pending"),
            group(null, memberCount = 50, maxMembers = 50).copy(id = "full"),
            group(null).copy(id = "open"),
        )
        var requested: String? = null
        composeTestRule.setContent {
            ProstutiTheme {
                GroupExploreTab(
                    state = GroupExploreUiState(isLoading = false, groups = groups),
                    onQueryChange = {}, onLoadMore = {}, onOpenGroup = {}, onRequestToJoin = { requested = it },
                )
            }
        }

        composeTestRule.onNodeWithTag("${TAG_EXPLORE_ACTION_PREFIX}mine").assertIsEnabled()
        composeTestRule.onNodeWithTag("${TAG_EXPLORE_ACTION_PREFIX}pending").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("${TAG_EXPLORE_ACTION_PREFIX}full").performScrollTo().assertIsNotEnabled()
        composeTestRule.onNodeWithTag("${TAG_EXPLORE_ACTION_PREFIX}open").performScrollTo().performClick()
        assertEquals("open", requested)
    }

    @Test
    fun deletingNeedsTheGroupNameTyped() {
        var deleted = false
        composeTestRule.setContent {
            ProstutiTheme {
                val g = group(GroupRole.ADMIN)
                GroupSettingsContent(
                    state = GroupSettingsUiState(isLoading = false, group = g, form = GroupSettingsForm(g.name, "", g.isPublic, g.maxMembers.toString())),
                    onBack = {}, onNameChange = {}, onDescriptionChange = {}, onPublicChange = {}, onMaxMembersChange = {},
                    onSave = {}, onRegenerateCode = {}, onOpenJoinRequests = {}, onOpenMembers = {}, onLeave = {},
                    onDelete = { deleted = true }, onRefresh = {}, onClearError = {},
                )
            }
        }

        composeTestRule.onNodeWithTag(TAG_SETTINGS_DELETE).performScrollTo().performClick()
        composeTestRule.onNodeWithTag(TAG_CONFIRM).assertIsNotEnabled()
        composeTestRule.onNodeWithTag(TAG_TYPE_TO_CONFIRM).performTextInput("HSC 26-27")
        composeTestRule.onNodeWithTag(TAG_CONFIRM).assertIsEnabled().performClick()
        assertEquals(true, deleted)
    }
}
