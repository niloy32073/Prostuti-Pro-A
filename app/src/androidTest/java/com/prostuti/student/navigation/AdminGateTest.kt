package com.prostuti.student.navigation

import androidx.compose.material3.Text
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

/**
 * Exercises [AdminGate] against the real (fake-data-backed) DI graph the app ships with — no
 * test doubles needed, since [ExamDummyData]'s 4 groups already cover admin/member/routine-
 * already-exists combinations. Each test wipes Room first so a prior manual run (or another
 * test) can never leave a stray routine/submission behind.
 */
@RunWith(AndroidJUnit4::class)
class AdminGateTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun resetDatabase() {
        GlobalContext.get().get<AppDatabase>().clearAllTables()
    }

    /** [AdminGate] resolves its group lookup asynchronously; waits for that first recomposition
     * instead of relying on `assertExists()` alone to retry. */
    private fun waitForText(text: String) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun deniesAccessForANonAdminGroup() {
        composeTestRule.setContent {
            ProstutiTheme {
                AdminGate(groupId = ExamDummyData.GROUP_CHEM, onDenied = {}) {
                    Text("গেটের ভেতরের কনটেন্ট")
                }
            }
        }

        waitForText("এই পেজ শুধু গ্রুপ অ্যাডমিনের জন্য")
        composeTestRule.onNodeWithText("গেটের ভেতরের কনটেন্ট").assertDoesNotExist()
    }

    @Test
    fun allowsAccessForTheGroupsOwnAdmin() {
        composeTestRule.setContent {
            ProstutiTheme {
                AdminGate(groupId = ExamDummyData.GROUP_NEW, onDenied = {}) {
                    Text("গেটের ভেতরের কনটেন্ট")
                }
            }
        }

        waitForText("গেটের ভেতরের কনটেন্ট")
    }

    @Test
    fun refusesReEntryOnceARoutineAlreadyExists() {
        // GROUP_HSC27 ships with a static routine already — requireRoutineNotYetCreated must
        // refuse it even though the caller IS that group's admin.
        composeTestRule.setContent {
            ProstutiTheme {
                AdminGate(groupId = ExamDummyData.GROUP_HSC27, requireRoutineNotYetCreated = true, onDenied = {}) {
                    Text("গেটের ভেতরের কনটেন্ট")
                }
            }
        }

        waitForText("এই গ্রুপের রুটিন আগেই তৈরি হয়ে গেছে — একবার তৈরি হলে তা বদলানো যায় না")
        composeTestRule.onNodeWithText("গেটের ভেতরের কনটেন্ট").assertDoesNotExist()
    }
}
