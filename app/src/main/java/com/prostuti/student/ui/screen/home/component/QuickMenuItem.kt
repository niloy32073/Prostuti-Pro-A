package com.prostuti.student.ui.screen.home.component

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.prostuti.student.R

data class QuickMenuItem(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val containerColor: @Composable () -> Color,
    val enabled: Boolean = true
)

@Composable
fun quickMenuItems(): List<QuickMenuItem> {
    val primary = MaterialTheme.colorScheme.primaryContainer
    val secondary = MaterialTheme.colorScheme.secondaryContainer
    return listOf(
        QuickMenuItem(R.string.qm_practice, Icons.Filled.Edit, { primary }),
        QuickMenuItem(R.string.qm_group, Icons.Filled.Group, { secondary }),
        QuickMenuItem(R.string.qm_survival, Icons.Filled.Bolt, { secondary }),
        QuickMenuItem(R.string.qm_battle, Icons.Filled.EmojiEvents, { secondary }),
        QuickMenuItem(R.string.qm_question_bank, Icons.AutoMirrored.Filled.MenuBook, { secondary }),
        QuickMenuItem(R.string.qm_math, Icons.Filled.Functions, { secondary }),
        QuickMenuItem(R.string.qm_concepts, Icons.Filled.Lightbulb, { secondary }),
        QuickMenuItem(R.string.qm_routine, Icons.Filled.CalendarMonth, { secondary }),
        QuickMenuItem(R.string.qm_admission, Icons.Filled.School, { secondary }),
        QuickMenuItem(R.string.qm_playground, Icons.Filled.SportsEsports, { secondary }),
        QuickMenuItem(R.string.qm_magazine, Icons.AutoMirrored.Filled.Article, { secondary }, enabled = false),
        QuickMenuItem(R.string.qm_ebook, Icons.AutoMirrored.Filled.LibraryBooks, { secondary }, enabled = false)
    )
}
