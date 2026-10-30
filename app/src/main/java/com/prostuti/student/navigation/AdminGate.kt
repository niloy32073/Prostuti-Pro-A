package com.prostuti.student.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prostuti.student.data.model.enums.GroupRole
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.domain.FetchGroupDetailUseCase
import com.prostuti.student.ui.component.PrimaryButton
import com.prostuti.student.ui.theme.ProstutiTypography
import org.koin.compose.koinInject

private sealed interface AdminGateState {
    data object Loading : AdminGateState
    data object Denied : AdminGateState
    data object Allowed : AdminGateState
}

/**
 * Wraps every [GroupAdminRoute] destination: confirms the signed-in student is THIS group's
 * admin before [content] ever renders, and (when [requireRoutineNotYetCreated] is set, for the
 * flow's entry point only) that no routine exists yet — a routine can never be edited once
 * created, so re-entering the builder after that is refused the same way a non-admin is.
 */
@Composable
fun AdminGate(
    groupId: String,
    requireRoutineNotYetCreated: Boolean = false,
    onDenied: () -> Unit,
    content: @Composable () -> Unit
) {
    val fetchGroupDetailUseCase: FetchGroupDetailUseCase = koinInject()
    var state by remember(groupId) { mutableStateOf<AdminGateState>(AdminGateState.Loading) }
    var deniedReasonBn by remember(groupId) { mutableStateOf("") }

    LaunchedEffect(groupId) {
        when (val result = fetchGroupDetailUseCase(groupId)) {
            is ApiResponse.Success -> {
                val isAdmin = result.data.group.myRole == GroupRole.ADMIN
                val routineLocked = requireRoutineNotYetCreated && result.data.routine != null
                state = when {
                    !isAdmin -> {
                        deniedReasonBn = "এই পেজ শুধু গ্রুপ অ্যাডমিনের জন্য"
                        AdminGateState.Denied
                    }
                    routineLocked -> {
                        deniedReasonBn = "এই গ্রুপের রুটিন আগেই তৈরি হয়ে গেছে — একবার তৈরি হলে তা বদলানো যায় না"
                        AdminGateState.Denied
                    }
                    else -> AdminGateState.Allowed
                }
            }
            is ApiResponse.Failure -> {
                deniedReasonBn = result.message
                state = AdminGateState.Denied
            }
        }
    }

    when (state) {
        AdminGateState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        AdminGateState.Denied -> AdminDeniedScreen(reasonBn = deniedReasonBn, onBack = onDenied)
        AdminGateState.Allowed -> content()
    }
}

@Composable
private fun AdminDeniedScreen(reasonBn: String, onBack: () -> Unit) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = reasonBn,
                style = ProstutiTypography.bnBodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            PrimaryButton(
                text = "ফিরে যাও",
                onClick = onBack,
                modifier = Modifier.padding(top = 24.dp)
            )
        }
    }
}
