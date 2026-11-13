package com.prostuti.student.ui.screen.examengine

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prostuti.student.data.model.response.FetchPassageData
import com.prostuti.student.ui.screen.examengine.component.PASSAGE_IMAGE_TAG
import com.prostuti.student.ui.screen.examengine.component.PassageCard
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PassageCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun show(passage: FetchPassageData) {
        composeTestRule.setContent {
            ProstutiTheme { PassageCard(passage = passage) { Text("MCQ body") } }
        }
    }

    @Test
    fun imageOnlyPassageShowsTheImage() {
        show(FetchPassageData(id = "p1", passageText = null, passageImage = "https://cdn.prostuti.com/passage.png"))

        composeTestRule.onNodeWithText("অনুচ্ছেদ").assertIsDisplayed()
        composeTestRule.onNodeWithTag(PASSAGE_IMAGE_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithText("MCQ body").assertIsDisplayed()
    }

    @Test
    fun passageWithTextAndImageShowsBoth() {
        show(FetchPassageData(id = "p2", passageText = "একটি উদ্দীপক", passageImage = "https://cdn.prostuti.com/passage.png"))

        composeTestRule.onNodeWithTag(PASSAGE_IMAGE_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithText("একটি উদ্দীপক", substring = true).assertIsDisplayed()
    }

    @Test
    fun blankImageIsNotShown() {
        show(FetchPassageData(id = "p3", passageText = "শুধু লেখা", passageImage = " "))

        composeTestRule.onNodeWithTag(PASSAGE_IMAGE_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithText("শুধু লেখা", substring = true).assertIsDisplayed()
    }
}
