package com.prostuti.student.ui.screen.examengine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.prostuti.student.R
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.domain.GetExamAttemptUseCase
import com.prostuti.student.domain.parseServerTimeMillis
import com.prostuti.student.ui.component.BackButton
import com.prostuti.student.ui.component.PrimaryButton
import com.prostuti.student.ui.component.localizedNumber
import com.prostuti.student.ui.theme.ProstutiTypography
import com.prostuti.student.util.DhakaTime
import com.prostuti.student.util.toBengaliDigits
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.Duration
import java.time.Instant

const val TAG_LOCKED_RESULT = "locked_result"

/**
 * Shows [content] only once [lockUntil] has passed; until then [locked] gets the time left, ticking
 * every second, and swaps to [content] by itself at zero (D1: a group exam's result stays hidden
 * while its window is open). Null or unreadable [lockUntil] = not locked.
 */
@Composable
fun ResultLockGate(lockUntil: String?, locked: @Composable (remaining: Duration) -> Unit, content: @Composable () -> Unit) {
    val until = lockUntil?.let { DhakaTime.parseInstant(it) }
    if (until == null) {
        content()
        return
    }
    val now by tickingNow(until)
    if (now.isBefore(until)) locked(Duration.between(now, until)) else content()
}

@Composable
private fun tickingNow(until: Instant): State<Instant> = produceState(Instant.now(), until) {
    while (value.isBefore(until)) {
        delay(1_000)
        value = Instant.now()
    }
}

/** Seconds used on a submitted attempt; never its score, which must stay hidden while locked. */
class LockedExamResultViewModel(attemptId: String, getExamAttempt: GetExamAttemptUseCase) : ViewModel() {
    private val _timeTakenSeconds = MutableStateFlow<Long?>(null)
    val timeTakenSeconds: StateFlow<Long?> = _timeTakenSeconds.asStateFlow()

    init {
        viewModelScope.launch {
            val attempt = (getExamAttempt(attemptId) as? ApiResponse.Success)?.data ?: return@launch
            val start = parseServerTimeMillis(attempt.startAt) ?: return@launch
            val end = attempt.endAt?.let(::parseServerTimeMillis) ?: return@launch
            _timeTakenSeconds.value = ((end - start) / 1000).coerceAtLeast(0)
        }
    }
}

@Composable
fun LockedExamResultScreen(
    attemptId: String,
    titleBn: String,
    remaining: Duration,
    onBack: () -> Unit,
    viewModel: LockedExamResultViewModel = koinViewModel(key = "locked_$attemptId", parameters = { parametersOf(attemptId) }),
) {
    val timeTaken by viewModel.timeTakenSeconds.collectAsStateWithLifecycle()
    LockedExamResultContent(titleBn = titleBn, remaining = remaining, timeTakenSeconds = timeTaken, onBack = onBack)
}

/** The design's GroupResultLocked: submitted ✓, the countdown to the window's end, time taken, and no score. */
@Composable
fun LockedExamResultContent(titleBn: String, remaining: Duration, timeTakenSeconds: Long?, onBack: () -> Unit) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).testTag(TAG_LOCKED_RESULT)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                BackButton(onClick = onBack)
                Spacer(Modifier.width(12.dp))
                Text(titleBn, style = ProstutiTypography.bnTitleMedium, color = MaterialTheme.colorScheme.onBackground)
            }
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
                Text(stringResource(R.string.locked_submitted), style = ProstutiTypography.bnTitleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    stringResource(R.string.locked_countdown, clockText(remaining.seconds)),
                    style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
                )
                timeTakenSeconds?.let {
                    Text(stringResource(R.string.locked_time_taken, clockText(it)), style = ProstutiTypography.bnLabelLarge, color = MaterialTheme.colorScheme.onSurface)
                }
                OutlinedButton(onClick = {}, enabled = false) {
                    Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp).padding(end = 2.dp))
                    Text(stringResource(R.string.locked_answer_sheet), style = ProstutiTypography.bnLabelLarge)
                }
            }
            PrimaryButton(text = stringResource(R.string.locked_back), onClick = onBack, modifier = Modifier.padding(16.dp))
        }
    }
}

/** "১:০৫:০৯" / "05:09" in the app's digits. */
@Composable
private fun clockText(totalSeconds: Long): String {
    val s = totalSeconds.coerceAtLeast(0)
    val h = (s / 3600).toInt()
    val m = ((s % 3600) / 60).toInt()
    val sec = (s % 60).toInt()
    val text = if (h > 0) "%d:%02d:%02d".format(java.util.Locale.ROOT, h, m, sec) else "%02d:%02d".format(java.util.Locale.ROOT, m, sec)
    // localizedNumber tells which digits the app language uses.
    return if (localizedNumber(0) != "0") text.toBengaliDigits() else text
}
