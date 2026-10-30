package com.prostuti.student.ui.screen.qb

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.navigation.SubjectType
import com.prostuti.student.ui.component.ObserveErrors
import com.prostuti.student.ui.screen.qb.component.SubjectListItem
import com.prostuti.student.ui.theme.ProstutiTypography
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SubjectListScreen(
    onNavigate: (SubjectType) -> Unit,
    viewModel: SubjectListViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    ObserveErrors(
        errors = state.errors,
        snackbarHostState = snackbarHostState,
        onConsumed = viewModel::clearError
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
                Text(
                    text = stringResource(R.string.nav_question_bank),
                    style = ProstutiTypography.bnTitleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "বিষয় বেছে নাও — তারপর MCQ, CQ, ক-খ দেখতে পাবে",
                    style = ProstutiTypography.bnLabelMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 3.dp)
                )
            }

            if (state.isLoading && state.subjects.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val columns = when {
                        maxWidth >= 840.dp -> 3
                        maxWidth >= 600.dp -> 2
                        else -> 1
                    }
                    val hSpacing = if (columns > 1) 8.dp else 0.dp

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(hSpacing)
                    ) {
                        items(state.subjects, key = { it.id }) { subject ->
                            SubjectListItem(
                                subject = subject,
                                onClick = { onNavigate(SubjectType(subject.id, subject.nameBn, subject.nameEn)) }
                            )
                        }
                    }
                }
            }
        }
    }
}
