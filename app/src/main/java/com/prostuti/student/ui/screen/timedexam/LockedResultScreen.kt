package com.prostuti.student.ui.screen.timedexam

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import com.prostuti.student.ui.component.ProstutiSpinner
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prostuti.student.data.model.enums.TimedExamKind
import com.prostuti.student.ui.component.PrimaryButton
import com.prostuti.student.ui.theme.ProstutiTypography
import com.prostuti.student.util.formatRemainingBn
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import androidx.lifecycle.compose.dropUnlessResumed

@Composable
fun LockedResultScreen(
    examId: String,
    kind: TimedExamKind,
    onBack: () -> Unit,
    onNavigateToRanking: (examId: String) -> Unit,
    viewModel: LockedResultViewModel = koinViewModel(parameters = { parametersOf(examId, kind) })
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                ProstutiSpinner()
            }
            return@Scaffold
        }

        val kindLabel = "লাইভ এক্সাম"
        val timeTakenText = state.submission?.timeTakenSeconds?.let { "%d:%02d".format(it / 60, it % 60) } ?: "—"

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "সাবমিট হয়েছে ✅", style = ProstutiTypography.bnHeadlineMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(text = kindLabel, style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))

            if (state.isWindowClosed) {
                Text(
                    text = "উইন্ডো শেষ হয়েছে — ফলাফল আনলক হয়েছে", style = ProstutiTypography.bnBodyLarge,
                    color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 24.dp)
                )
                PrimaryButton(text = "র‍্যাংকিং ও ফলাফল দেখো", onClick = dropUnlessResumed { onNavigateToRanking(examId) }, modifier = Modifier.padding(top = 20.dp))
            } else {
                Text(
                    text = "উইন্ডো শেষ হবে আরও ${formatRemainingBn(state.remainingMillis)} পর — তারপর ফলাফল আনলক হবে",
                    style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(top = 24.dp)
                )

                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(16.dp).padding(top = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LockedRow(label = "সময় লেগেছে", value = timeTakenText)
                    LockedRow(label = "চূড়ান্ত মার্ক", value = "উইন্ডো শেষে দেখা যাবে")
                    LockedRow(label = "র‍্যাংক", value = "উইন্ডো শেষে দেখা যাবে")
                }

                PrimaryButton(text = "অ্যান্সার শিট (লকড)", enabled = false, onClick = {}, modifier = Modifier.padding(top = 20.dp))
            }

            Text(
                text = "এক্সাম ডিটেইলে ফিরে যাও",
                style = ProstutiTypography.bnLabelLarge, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp).clickable(onClick = dropUnlessResumed { onBack() })
            )
        }
    }
}

@Composable
private fun LockedRow(label: String, value: String) {
    androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(text = value, style = ProstutiTypography.enBodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
