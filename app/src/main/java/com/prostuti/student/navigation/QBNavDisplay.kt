package com.prostuti.student.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.prostuti.student.ui.screen.qb.ChapterBrowseScreen
import com.prostuti.student.ui.screen.qb.MCQExamListScreen
import com.prostuti.student.ui.screen.qb.QuestionReaderScreen
import com.prostuti.student.ui.screen.qb.SubjectListScreen
import com.prostuti.student.ui.screen.qb.SubjectTypeScreen

@Composable
fun QBNavDisplay(
    backStack: NavBackStack<NavKey> = rememberNavBackStack(SubjectList),
    onNavigateToExamEngine: (ExamAttempt) -> Unit
) {
    NavDisplay(
        backStack = backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<SubjectList> {
                SubjectListScreen(onNavigate = { backStack.add(it) })
            }
            entry<SubjectType> { route ->
                SubjectTypeScreen(
                    subjectId = route.subjectId,
                    nameBn = route.nameBn,
                    nameEn = route.nameEn,
                    onBack = { backStack.removeLastOrNull() },
                    onNavigateToMCQ = { backStack.add(it) }
                )
            }
            entry<MCQExamList> { route ->
                MCQExamListScreen(
                    subjectId = route.subjectId,
                    nameBn = route.nameBn,
                    nameEn = route.nameEn,
                    onBack = { backStack.removeLastOrNull() },
                    onNavigateToExamEngine = onNavigateToExamEngine,
                    onNavigateToReader = { backStack.add(it) },
                    onNavigateToChapterBrowse = { backStack.add(it) }
                )
            }
            entry<QuestionReader> { route ->
                QuestionReaderScreen(
                    titleBn = route.titleBn,
                    onBack = { backStack.removeLastOrNull() }
                )
            }
            entry<ChapterBrowse> { route ->
                ChapterBrowseScreen(
                    subjectId = route.subjectId,
                    nameBn = route.nameBn,
                    onBack = { backStack.removeLastOrNull() }
                )
            }
        }
    )
}
