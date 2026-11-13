package com.prostuti.student.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProstutiSpinnerTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun setSpinners(darkTheme: Boolean) {
        composeRule.setContent {
            ProstutiTheme(darkTheme = darkTheme) {
                Row(Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)) {
                    ProstutiSpinner(Modifier.size(96.dp).testTag(TAG_BRAND))
                    ProstutiSpinner(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary)
                            .size(48.dp)
                            .testTag(TAG_TINTED),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }

    @Test
    fun lightTheme_showsBrandAndTintedSpinners_asIndeterminateProgress() {
        setSpinners(darkTheme = false)
        composeRule.onNodeWithTag(TAG_BRAND).assertIsDisplayed()
        composeRule.onNodeWithTag(TAG_TINTED).assertIsDisplayed()
        composeRule.onAllNodes(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .fetchSemanticsNodes().let { check(it.size == 2) { "expected 2 spinners, got ${it.size}" } }
    }

    @Test
    fun darkTheme_inflatesWithDarkMarkTheme_andShows() {
        setSpinners(darkTheme = true)
        composeRule.onNodeWithTag(TAG_BRAND).assertIsDisplayed()
        composeRule.onNodeWithTag(TAG_TINTED).assertIsDisplayed()
    }

    private companion object {
        const val TAG_BRAND = "spinner_brand"
        const val TAG_TINTED = "spinner_tinted"
    }
}
