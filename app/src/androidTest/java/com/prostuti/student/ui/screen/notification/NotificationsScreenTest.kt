package com.prostuti.student.ui.screen.notification

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.prostuti.student.R
import com.prostuti.student.data.model.response.NotificationItem
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The inbox: unread rows carry a dot, a tap reaches the ViewModel, and the filter and empty states. */
@RunWith(AndroidJUnit4::class)
class NotificationsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun item(id: String, read: Boolean) = NotificationItem(
        id = id, title = "New group exam $id", message = "Physics weekly test", type = "GROUP_ACTIVITY",
        targetUrl = "/groups/g1", isRead = read, createdAt = "2026-11-08T10:00:00",
    )

    @Test
    fun unreadRowsHaveADotAndTapsOpenThem() {
        var opened: String? = null
        composeTestRule.setContent {
            ProstutiTheme {
                NotificationsContent(
                    NotificationsUiState(isLoading = false, items = listOf(item("a", read = false), item("b", read = true))),
                    onBack = {}, onOpen = { opened = it.id },
                )
            }
        }
        composeTestRule.onNodeWithText("New group exam a").assertIsDisplayed()
        assertEquals("only the unread row", 1, composeTestRule.onAllNodesWithTag(TAG_NOTIF_UNREAD_DOT, useUnmergedTree = true).fetchSemanticsNodes().size)
        composeTestRule.onNodeWithTag(TAG_NOTIF_ROW_PREFIX + "b").performClick()
        assertEquals("b", opened)
    }

    @Test
    fun theUnreadFilterAndItsEmptyState() {
        var unreadOnly: Boolean? = null
        composeTestRule.setContent {
            ProstutiTheme {
                NotificationsContent(NotificationsUiState(isLoading = false, unreadOnly = true), onBack = {}, onUnreadOnly = { unreadOnly = it })
            }
        }
        composeTestRule.onNodeWithTag(TAG_NOTIF_EMPTY).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.notif_empty_unread)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.notif_all)).performClick()
        assertEquals(false, unreadOnly)
    }
}
