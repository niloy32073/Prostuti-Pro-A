package com.prostuti.student.ui.screen.qb

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.navigation.MCQExamList
import com.prostuti.student.ui.component.BackButton
import com.prostuti.student.ui.screen.qb.component.SubjectTypeCard
import com.prostuti.student.ui.theme.ProstutiTypography
import kotlinx.coroutines.launch
import com.prostuti.student.ui.component.localized
import androidx.lifecycle.compose.dropUnlessResumed

@Composable
fun SubjectTypeScreen(
    subjectId: String,
    nameBn: String,
    nameEn: String,
    onBack: () -> Unit,
    onNavigateToMCQ: (MCQExamList) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val comingSoon = stringResource(R.string.feature_coming_soon)

    fun showComingSoon() { scope.launch { snackbarHostState.showSnackbar(comingSoon) } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButton(onClick = onBack)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = localized(nameBn, nameEn),
                        style = ProstutiTypography.bnTitleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "কোন ধরনের প্রশ্ন দেখতে চাও?",
                        style = ProstutiTypography.bnLabelMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                SubjectTypeCard(
                    icon = Icons.Outlined.CheckBox,
                    iconBg = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                    title = "MCQ — বহুনির্বাচনী",
                    subtitle = "বোর্ড ও কলেজের মডেল টেস্ট",
                    modifier = Modifier.padding(bottom = 12.dp),
                    onClick = dropUnlessResumed { onNavigateToMCQ(MCQExamList(subjectId, nameBn, nameEn)) }
                )
                SubjectTypeCard(
                    icon = Icons.Outlined.Description,
                    iconBg = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                    title = "CQ — সৃজনশীল",
                    subtitle = "বোর্ড ও কলেজের সৃজনশীল প্রশ্ন",
                    modifier = Modifier.padding(bottom = 12.dp),
                    onClick = ::showComingSoon
                )
                SubjectTypeCard(
                    icon = Icons.AutoMirrored.Outlined.Article,
                    iconBg = MaterialTheme.colorScheme.tertiaryContainer,
                    iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                    title = "ক-খ সংক্ষিপ্ত প্রশ্ন",
                    subtitle = "অধ্যায় ধরে জ্ঞানমূলক ও অনুধাবনমূলক প্রশ্ন",
                    modifier = Modifier.padding(bottom = 12.dp),
                    onClick = ::showComingSoon
                )
                SubjectTypeCard(
                    icon = Icons.Outlined.Calculate,
                    iconBg = MaterialTheme.colorScheme.surfaceContainerHighest,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    title = "ম্যাথ প্রবলেম ব্যাংক",
                    subtitle = "হিন্ট → ফুল সমাধান স্টাইলে",
                    modifier = Modifier.padding(bottom = 12.dp),
                    onClick = ::showComingSoon
                )
            }
        }
    }
}
