package com.prostuti.student.ui.screen.group

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prostuti.student.data.model.response.GroupMember
import com.prostuti.student.ui.component.BackButton
import com.prostuti.student.ui.component.ObserveErrors
import com.prostuti.student.ui.component.adaptiveColumnCount
import com.prostuti.student.ui.theme.ProstutiTypography
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun GroupMembersScreen(
    groupId: String,
    onBack: () -> Unit,
    viewModel: GroupMembersViewModel = koinViewModel(parameters = { parametersOf(groupId) })
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    ObserveErrors(errors = state.errors, snackbarHostState = snackbarHostState, onConsumed = viewModel::clearError)

    Scaffold(containerColor = MaterialTheme.colorScheme.background, snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp)) {
                BackButton(onClick = onBack)
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "সদস্যরা", style = ProstutiTypography.bnTitleMedium, color = MaterialTheme.colorScheme.onBackground)
            }

            OutlinedTextField(
                value = state.query, onValueChange = viewModel::setQuery,
                placeholder = { Text("নাম দিয়ে খোঁজো", style = ProstutiTypography.bnBodyMedium) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                val columns = adaptiveColumnCount()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    state = rememberLazyGridState(),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(if (columns > 1) 8.dp else 0.dp)
                ) {
                    items(state.filtered, key = { it.id }) { member -> MemberRow(member) }
                }
            }
        }
    }
}

@Composable
private fun MemberRow(member: GroupMember) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
            Text(text = member.initial, style = ProstutiTypography.bnLabelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        val displayName = if (member.isMe) "${member.name} (তুমি)" else member.name
        Text(
            text = displayName, style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(start = 10.dp), maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        if (member.isAdmin) {
            Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.tertiaryContainer).padding(horizontal = 8.dp, vertical = 2.dp)) {
                Text(text = "অ্যাডমিন", style = ProstutiTypography.bnLabelMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
            }
        }
    }
}
