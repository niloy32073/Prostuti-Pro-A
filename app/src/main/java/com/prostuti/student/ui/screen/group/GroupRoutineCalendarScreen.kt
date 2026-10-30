package com.prostuti.student.ui.screen.group

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.ui.component.BackButton
import com.prostuti.student.ui.component.ObserveErrors
import com.prostuti.student.ui.component.PrimaryButton
import com.prostuti.student.ui.theme.ProstutiTypography
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun GroupRoutineCalendarScreen(
    groupId: String,
    onBack: () -> Unit,
    onNavigateToGroupDetail: () -> Unit,
    viewModel: GroupRoutineCalendarViewModel = koinViewModel(parameters = { parametersOf(groupId) })
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val comingSoon = stringResource(R.string.feature_coming_soon)
    ObserveErrors(errors = state.errors, snackbarHostState = snackbarHostState, onConsumed = viewModel::clearError)

    Scaffold(containerColor = MaterialTheme.colorScheme.background, snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp)) {
                BackButton(onClick = onBack)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = "গ্রুপ রুটিন ক্যালেন্ডার", style = ProstutiTypography.bnTitleMedium, color = MaterialTheme.colorScheme.onBackground)
                    state.group?.let {
                        Text(text = it.nameBn, style = ProstutiTypography.enLabelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                return@Scaffold
            }

            if (state.routine == null) {
                Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (state.isAdmin) "এখনও কোনো গ্রুপ রুটিন তৈরি হয়নি।" else "এখনও অ্যাডমিন গ্রুপের রুটিন সেট করেনি।",
                        style = ProstutiTypography.bnBodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                return@Scaffold
            }

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.weekDays.forEachIndexed { index, day ->
                    val selected = index == state.selectedIndex
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow)
                            .clickable { viewModel.selectDay(index) }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = day.weekdayLabelBn, style = ProstutiTypography.bnLabelMedium,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = day.dayNumBn, style = ProstutiTypography.enBodyLarge,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            state.selectedDay?.let { day ->
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    if (day.isExamDay) {
                        Column(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(text = "গ্রুপ এক্সাম", style = ProstutiTypography.bnTitleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(
                                text = "রুটিন অনুযায়ী এই দিনে নির্ধারিত সময়ে গ্রুপ এক্সাম থাকবে।",
                                style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (day.isToday) {
                                PrimaryButton(text = "গ্রুপ ডিটেইলে যাও", onClick = onNavigateToGroupDetail)
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(text = "ব্যক্তিগত অধ্যয়ন", style = ProstutiTypography.bnTitleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                text = "এই দিনে কোনো গ্রুপ এক্সাম নেই — রুটিনের টপিকগুলো রেফারেন্স হিসেবে এখানে দেখানো হবে।",
                                style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            PrimaryButton(text = "কনসেপ্ট পড়ো", onClick = { scope.launch { snackbarHostState.showSnackbar(comingSoon) } })
                        }
                    }
                    Text(
                        text = "এটি অ্যাডমিনের তৈরি গ্রুপ রুটিন — সব মেম্বার একই ক্যালেন্ডার দেখবে।",
                        style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }
    }
}
