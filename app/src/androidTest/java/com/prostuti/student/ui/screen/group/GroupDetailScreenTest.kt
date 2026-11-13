package com.prostuti.student.ui.screen.group

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.prostuti.student.R
import com.prostuti.student.data.model.enums.GroupRole
import com.prostuti.student.data.model.response.StudyGroup
import com.prostuti.student.data.model.response.StudyGroupMember
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** GroupDetail shows the invite code (with copy and share) to admins only — the server only sends
 * `invite_code` to admins, and the card must not appear for a member even if one slipped through. */
@RunWith(AndroidJUnit4::class)
class GroupDetailScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun group(role: GroupRole, code: String?) = StudyGroup(
        id = "g1", name = "HSC ব্যাচ ২৭", description = "সাপ্তাহিক প্র্যাকটিস", isPublic = false, maxMembers = 500,
        memberCount = 38, createdAt = "2026-11-02T10:00:00", myRole = role, inviteCode = code,
    )

    private val members = listOf(
        StudyGroupMember(userId = "u1", username = "rafi", role = GroupRole.ADMIN, joinedAt = "2026-11-02T10:05:00"),
        StudyGroupMember(userId = "u2", username = "sadia", role = GroupRole.MEMBER, joinedAt = "2026-11-02T10:06:00"),
    )

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private var copied: String? = null
    private var seeAllClicks = 0

    private fun show(state: GroupDetailUiState) {
        composeTestRule.setContent {
            ProstutiTheme {
                GroupDetailContent(
                    state = state,
                    onBack = {},
                    onSeeAllMembers = { seeAllClicks++ },
                    onCopyCode = { copied = it },
                    onShareCode = { _, _ -> },
                    onRefresh = {},
                    onClearError = {},
                )
            }
        }
    }

    @Test
    fun adminSeesTheInviteCodeAndCanCopyIt() {
        show(GroupDetailUiState(isLoading = false, group = group(GroupRole.ADMIN, "K7M2XQ9P"), memberPreview = members))

        composeTestRule.onNodeWithTag(TAG_INVITE_CODE).assertIsDisplayed()
        composeTestRule.onNodeWithText("K7M2XQ9P").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.group_copy_code_cd)).performClick()
        assertEquals("K7M2XQ9P", copied)
    }

    @Test
    fun memberDoesNotSeeAnInviteCode() {
        show(GroupDetailUiState(isLoading = false, group = group(GroupRole.MEMBER, "LEAKED01"), memberPreview = members))

        composeTestRule.onNodeWithTag(TAG_INVITE_CODE).assertDoesNotExist()
        composeTestRule.onNodeWithText("LEAKED01").assertDoesNotExist()
    }

    @Test
    fun memberPreviewShowsUsernamesAndAdminBadge() {
        show(GroupDetailUiState(isLoading = false, group = group(GroupRole.MEMBER, null), memberPreview = members))

        composeTestRule.onNodeWithText("HSC ব্যাচ ২৭").assertIsDisplayed()
        composeTestRule.onNodeWithText("rafi").assertIsDisplayed()
        composeTestRule.onNodeWithText("sadia").assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.group_admin_badge)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.group_see_all)).performClick()
        assertEquals(1, seeAllClicks)
    }
}
