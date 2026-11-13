package com.prostuti.student.ui.screen.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.prostuti.student.R
import com.prostuti.student.ui.screen.home.component.WIDGET_PROMO_ADD_TAG
import com.prostuti.student.ui.screen.home.component.WIDGET_PROMO_CLOSE_TAG
import com.prostuti.student.ui.screen.home.component.WidgetPromoCard
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetPromoCardTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun addAndCloseCallBack() {
        var adds = 0
        var closes = 0
        composeTestRule.setContent {
            ProstutiTheme { WidgetPromoCard(onAdd = { adds++ }, onClose = { closes++ }) }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.widget_promo_title)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(WIDGET_PROMO_ADD_TAG).performClick()
        composeTestRule.onNodeWithTag(WIDGET_PROMO_CLOSE_TAG).performClick()

        assertEquals(1, adds)
        assertEquals(1, closes)
    }
}
