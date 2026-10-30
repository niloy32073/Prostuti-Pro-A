package com.prostuti.student.ui.screen.group

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prostuti.student.data.repository.fake.ExamDummyData
import com.prostuti.student.data.service.local.AppDatabase
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/** GroupDetailScreen's "no routine yet" card must read differently for an admin (a CTA to
 * create one) than for a member (a waiting message, no CTA) — the same role-based branch the
 * design calls out. Both [ExamDummyData.GROUP_CHEM] (member) and [ExamDummyData.GROUP_NEW]
 * (admin) start with no routine, by construction. */
@RunWith(AndroidJUnit4::class)
class GroupDetailScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun resetDatabase() {
        GlobalContext.get().get<AppDatabase>().clearAllTables()
    }

    private fun setGroupDetail(groupId: String) {
        composeTestRule.setContent {
            ProstutiTheme {
                GroupDetailScreen(
                    groupId = groupId,
                    onBack = {},
                    onNavigateToRoutineCreate = {},
                    onNavigateToCalendar = {},
                    onNavigateToPastExams = {},
                    onNavigateToMembers = {},
                    onNavigateToPastExam = {},
                    onNavigateToAttempt = {},
                    onNavigateToLockedResult = {},
                    onNavigateToRanking = {}
                )
            }
        }
    }

    private fun waitForText(text: String) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun memberWithNoRoutineSeesWaitingMessageNotTheCreateButton() {
        setGroupDetail(ExamDummyData.GROUP_CHEM)

        waitForText("এখনও অ্যাডমিন গ্রুপের রুটিন সেট করেনি — রুটিন সেট হলে নির্দিষ্ট দিনে এক্সাম এখানে অটো দেখাবে।")
        composeTestRule.onNodeWithText("গ্রুপ রুটিন তৈরি করুন").assertDoesNotExist()
    }

    @Test
    fun adminWithNoRoutineSeesTheCreateButton() {
        setGroupDetail(ExamDummyData.GROUP_NEW)

        waitForText("গ্রুপ রুটিন তৈরি করুন")
        composeTestRule.onNodeWithText("এখনও অ্যাডমিন গ্রুপের রুটিন সেট করেনি — রুটিন সেট হলে নির্দিষ্ট দিনে এক্সাম এখানে অটো দেখাবে।").assertDoesNotExist()
    }
}
