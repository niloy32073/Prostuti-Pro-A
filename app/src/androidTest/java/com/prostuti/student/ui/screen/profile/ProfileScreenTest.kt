package com.prostuti.student.ui.screen.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.prostuti.student.R
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.repository.fake.FakeUserRepository
import com.prostuti.student.domain.GetMeUseCase
import com.prostuti.student.ui.component.errorCardCloseTag
import com.prostuti.student.ui.component.errorCardTag
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun setContent(state: ProfileUiState, onBack: () -> Unit = {}, onRefresh: () -> Unit = {}) {
        composeTestRule.setContent {
            ProstutiTheme {
                ProfileContent(state = state, onBack = onBack, onRefresh = onRefresh, onClearError = {})
            }
        }
    }

    private fun assertBadge(badgeTag: String, labelRes: Int) {
        composeTestRule.onNode(
            hasAnyAncestor(hasTestTag(badgeTag)) and hasText(context.getString(labelRes)),
            useUnmergedTree = true
        ).assertExists()
    }

    @Test
    fun showsAllProfileFieldsWithVerifiedBadges() {
        setContent(ProfileUiState(profile = FakeUserRepository.me()))

        composeTestRule.onNodeWithTag(PROFILE_NAME_TAG).assertIsDisplayed().assertTextEquals("Muwaz Ahmed")
        composeTestRule.onNodeWithTag(PROFILE_USERNAME_TAG).assertTextEquals("@muwy")
        composeTestRule.onNodeWithTag(PROFILE_ROLE_TAG).assertTextEquals(context.getString(R.string.role_student))
        composeTestRule.onNodeWithTag(PROFILE_EMAIL_TAG).performScrollTo().assertTextEquals("muwy@proton.me")
        assertBadge(PROFILE_EMAIL_BADGE_TAG, R.string.profile_verified)
        composeTestRule.onNodeWithTag(PROFILE_PHONE_TAG).performScrollTo().assertTextEquals("+8801712345678")
        assertBadge(PROFILE_PHONE_BADGE_TAG, R.string.profile_not_verified)
        composeTestRule.onNodeWithTag(PROFILE_PROVIDER_TAG).performScrollTo()
            .assertTextEquals(context.getString(R.string.provider_email))
        composeTestRule.onNodeWithTag(PROFILE_CREATED_TAG).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag(PROFILE_LAST_LOGIN_TAG).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun missingPhoneShowsNotSetAndNoBadge() {
        setContent(ProfileUiState(profile = FakeUserRepository.me(phone = null, lastLoginAt = null)))

        composeTestRule.onNodeWithTag(PROFILE_PHONE_TAG).performScrollTo()
            .assertTextEquals(context.getString(R.string.profile_not_set))
        composeTestRule.onNodeWithTag(PROFILE_PHONE_BADGE_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(PROFILE_LAST_LOGIN_TAG).performScrollTo()
            .assertTextEquals(context.getString(R.string.profile_not_set))
    }

    @Test
    fun loadFailureShowsRetryThatRefreshes() {
        var refreshed = 0
        setContent(ProfileUiState(isLoading = false, profile = null), onRefresh = { refreshed++ })

        composeTestRule.onNodeWithText(context.getString(R.string.profile_load_failed)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PROFILE_RETRY_TAG).performClick()
        assertEquals(1, refreshed)
    }

    @Test
    fun loadErrorShowsInTheErrorOverlayAndCanBeClosed() {
        var cleared: String? = null
        composeTestRule.setContent {
            ProstutiTheme {
                ProfileContent(
                    state = ProfileUiState(
                        profile = FakeUserRepository.me(),
                        errors = mapOf(ProfileViewModel.ERR_LOAD to "No internet connection")
                    ),
                    onBack = {}, onRefresh = {}, onClearError = { cleared = it }
                )
            }
        }

        composeTestRule.onNodeWithTag(errorCardTag(ProfileViewModel.ERR_LOAD)).assertIsDisplayed()
        composeTestRule.onNodeWithText("No internet connection").assertIsDisplayed()
        composeTestRule.onNodeWithTag(errorCardCloseTag(ProfileViewModel.ERR_LOAD)).performClick()
        assertEquals(ProfileViewModel.ERR_LOAD, cleared)
    }

    @Test
    fun backButtonCallsOnBack() {
        var backs = 0
        setContent(ProfileUiState(profile = FakeUserRepository.me()), onBack = { backs++ })

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.back)).performClick()
        assertEquals(1, backs)
    }

    @Test
    fun screenLoadsProfileThroughViewModel() {
        val repo = FakeUserRepository(listOf(ApiResponse.Success(FakeUserRepository.me(firstName = "Rahim"))))
        composeTestRule.setContent {
            ProstutiTheme {
                ProfileScreen(onBack = {}, viewModel = ProfileViewModel(GetMeUseCase(repo)))
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag(PROFILE_NAME_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(PROFILE_NAME_TAG).assertTextEquals("Rahim Ahmed")
    }
}
