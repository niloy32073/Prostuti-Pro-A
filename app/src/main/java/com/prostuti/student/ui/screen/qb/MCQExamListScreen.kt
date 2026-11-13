package com.prostuti.student.ui.screen.qb

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import com.prostuti.student.ui.component.ProstutiSpinner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.prostuti.student.R
import com.prostuti.student.data.model.response.MCQExam
import com.prostuti.student.navigation.ChapterBrowse
import com.prostuti.student.navigation.ExamAttempt
import com.prostuti.student.navigation.QuestionReader
import com.prostuti.student.ui.component.BackButton
import com.prostuti.student.ui.component.PullToRefreshErrorOverlayWrapper
import com.prostuti.student.ui.component.adaptiveColumnCount
import com.prostuti.student.ui.screen.examengine.ExamAttemptStore
import com.prostuti.student.ui.screen.qb.component.ExamDetailDialog
import com.prostuti.student.ui.screen.qb.component.ExamItem
import com.prostuti.student.ui.theme.ProstutiTypography
import kotlinx.coroutines.flow.distinctUntilChanged
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.prostuti.student.ui.component.localized

@Composable
fun MCQExamListScreen(
    subjectId: String,
    nameBn: String,
    nameEn: String,
    onBack: () -> Unit,
    onNavigateToExamEngine: (ExamAttempt) -> Unit,
    onNavigateToReader: (QuestionReader) -> Unit,
    onNavigateToChapterBrowse: (ChapterBrowse) -> Unit,
    viewModel: MCQExamListViewModel = koinViewModel(parameters = { parametersOf(subjectId) })
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val boardListState = rememberLazyGridState()
    val collegeListState = rememberLazyGridState()

    LaunchedEffect(state.navigateToExam) {
        state.navigateToExam?.let { (attemptData, exam) ->
            ExamAttemptStore.put(attemptData)
            onNavigateToExamEngine(ExamAttempt(attemptId = attemptData.attemptId, titleBn = exam.titleBn))
            viewModel.consumeNavigation()
        }
    }

    val boardNearEnd by remember {
        derivedStateOf {
            val info = boardListState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= info.totalItemsCount - 3
        }
    }
    LaunchedEffect(boardNearEnd) {
        snapshotFlow { boardNearEnd }.distinctUntilChanged().collect { near ->
            if (near && state.selectedTab == MCQListTab.BOARD) viewModel.loadMoreBoard()
        }
    }

    val collegeNearEnd by remember {
        derivedStateOf {
            val info = collegeListState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= info.totalItemsCount - 3
        }
    }
    LaunchedEffect(collegeNearEnd) {
        snapshotFlow { collegeNearEnd }.distinctUntilChanged().collect { near ->
            if (near && state.selectedTab == MCQListTab.COLLEGE) viewModel.loadMoreCollege()
        }
    }

    state.selectedExam?.let { exam ->
        ExamDetailDialog(
            exam = exam,
            isAttempting = state.isAttempting,
            onDismiss = { viewModel.selectExam(null) },
            onRead = {
                viewModel.selectExam(null)
                onNavigateToReader(QuestionReader(examId = exam.id, titleBn = exam.titleBn))
            },
            onStart = { viewModel.startExam(exam) }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        PullToRefreshErrorOverlayWrapper(
            errors = state.errors,
            errorCleaner = viewModel::clearError,
            overlayPadding = padding
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                val examColumns = adaptiveColumnCount()
                val tabRowWidthCap = if (examColumns > 1) Modifier.widthIn(max = 480.dp) else Modifier

                Column(modifier = Modifier.fillMaxSize()) {
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
                                text = "MCQ প্রশ্ন ব্যাংক",
                                style = ProstutiTypography.enLabelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { onNavigateToChapterBrowse(ChapterBrowse(subjectId, nameBn, nameEn)) },
                        shape = RoundedCornerShape(50.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "অধ্যায়-ভিত্তিক প্রশ্ন দেখো  →",
                            style = ProstutiTypography.bnLabelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        TextField(
                            value = state.searchQuery,
                            onValueChange = viewModel::updateSearch,
                            placeholder = {
                                Text(
                                    text = "বোর্ড, কলেজ বা মডেল টেস্টের নাম খুঁজো...",
                                    style = ProstutiTypography.bnLabelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent
                            ),
                            textStyle = ProstutiTypography.bnBodyMedium,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .then(tabRowWidthCap)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(50.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            val boardSelected = state.selectedTab == MCQListTab.BOARD
                            Button(
                                onClick = { viewModel.selectTab(MCQListTab.BOARD) },
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (boardSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    contentColor = if (boardSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                elevation = null
                            ) {
                                Text(text = "বোর্ডের প্রশ্ন", style = ProstutiTypography.bnLabelLarge)
                            }
                            Button(
                                onClick = { viewModel.selectTab(MCQListTab.COLLEGE) },
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!boardSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    contentColor = if (!boardSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                elevation = null
                            ) {
                                Text(text = "কলেজের প্রশ্ন", style = ProstutiTypography.bnLabelLarge)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val filteredBoard = if (state.searchQuery.isBlank()) state.boardExams
                    else state.boardExams.filter { it.titleBn.contains(state.searchQuery, ignoreCase = true) || it.titleEn.contains(state.searchQuery, ignoreCase = true) }

                    val filteredCollege = if (state.searchQuery.isBlank()) state.collegeExams
                    else state.collegeExams.filter { it.titleBn.contains(state.searchQuery, ignoreCase = true) || it.titleEn.contains(state.searchQuery, ignoreCase = true) }

                    val examHSpacing = if (examColumns > 1) 8.dp else 0.dp

                    if (state.selectedTab == MCQListTab.BOARD) {
                        if (state.isLoading && filteredBoard.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                ProstutiSpinner()
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(examColumns),
                                state = boardListState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(examHSpacing)
                            ) {
                                items(filteredBoard, key = { it.id }) { exam ->
                                    ExamItem(
                                        exam = exam,
                                        onClick = { viewModel.selectExam(exam) }
                                    )
                                }
                            }
                        }
                    } else {
                        if (state.isLoading && filteredCollege.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                ProstutiSpinner()
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(examColumns),
                                state = collegeListState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(examHSpacing)
                            ) {
                                items(filteredCollege, key = { it.id }) { exam ->
                                    ExamItem(
                                        exam = exam,
                                        onClick = { viewModel.selectExam(exam) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
    
        }
    }
}


