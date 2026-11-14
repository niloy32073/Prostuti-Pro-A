package com.prostuti.student.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class NavigatorTest {

    private lateinit var state: NavigationState
    private lateinit var navigator: Navigator

    @Before
    fun setUp() {
        val tabs: List<NavKey> = listOf(Home, QB, Routine)
        state = NavigationState(
            startRoute = Home,
            topLevelRoute = mutableStateOf(Home),
            backStacks = tabs.associateWith { NavBackStack(it) },
        )
        navigator = Navigator(state)
    }

    private val subject = SubjectType("s1", "পদার্থ", "Physics")
    private val exams = MCQExamList("s1", "পদার্থ", "Physics")

    @Test
    fun `navigating to a tab shows it without touching any stack`() {
        navigator.navigate(QB)

        assertEquals(QB, state.topLevelRoute)
        assertEquals(listOf<NavKey>(QB), state.backStacks.getValue(QB).toList())
        assertEquals(listOf<NavKey>(Home, QB), state.stacksInUse)
    }

    @Test
    fun `a screen is pushed onto the shown tab only`() {
        navigator.navigate(QB)
        navigator.navigate(subject)

        assertEquals(listOf<NavKey>(QB, subject), state.backStacks.getValue(QB).toList())
        assertEquals(listOf<NavKey>(Home), state.backStacks.getValue(Home).toList())
    }

    @Test
    fun `the same screen is not pushed twice in a row`() {
        navigator.navigate(QB)
        navigator.navigate(subject)
        navigator.navigate(subject)

        assertEquals(listOf<NavKey>(QB, subject), state.currentStack.toList())
    }

    @Test
    fun `a tab keeps its history while another tab is shown`() {
        navigator.navigate(QB)
        navigator.navigate(subject)
        navigator.navigate(exams)
        navigator.navigate(Routine)
        navigator.navigate(QB)

        assertEquals(listOf<NavKey>(QB, subject, exams), state.currentStack.toList())
    }

    @Test
    fun `back pops the shown tab`() {
        navigator.navigate(QB)
        navigator.navigate(subject)
        navigator.navigate(exams)

        navigator.goBack()

        assertEquals(QB, state.topLevelRoute)
        assertEquals(listOf<NavKey>(QB, subject), state.currentStack.toList())
    }

    @Test
    fun `back from another tab's root goes to the start tab`() {
        navigator.navigate(Routine)

        navigator.goBack()

        assertEquals(Home, state.topLevelRoute)
        assertEquals(listOf<NavKey>(Home), state.stacksInUse)
    }

    @Test
    fun `replaceTop swaps the top screen`() {
        navigator.navigate(QB)
        navigator.navigate(subject)
        navigator.replaceTop(exams)

        assertEquals(listOf<NavKey>(QB, exams), state.currentStack.toList())
    }

    @Test
    fun `popToRoot and popUntil never remove the tab root`() {
        navigator.navigate(QB)
        navigator.navigate(subject)
        navigator.navigate(exams)

        navigator.popUntil { it == subject }
        assertEquals(listOf<NavKey>(QB, subject), state.currentStack.toList())

        navigator.popUntil { false }
        assertEquals(listOf<NavKey>(QB), state.currentStack.toList())

        navigator.navigate(subject)
        navigator.popToRoot()
        assertEquals(listOf<NavKey>(QB), state.currentStack.toList())
    }

    @Test
    fun `removeEverywhere clears matching screens from every tab but keeps roots`() {
        navigator.navigate(subject)          // on Home
        navigator.navigate(QB)
        navigator.navigate(subject)
        navigator.navigate(exams)

        navigator.removeEverywhere { it is SubjectType || it == QB }

        assertEquals(listOf<NavKey>(Home), state.backStacks.getValue(Home).toList())
        assertEquals(listOf<NavKey>(QB, exams), state.backStacks.getValue(QB).toList())
    }

    @Test
    fun `openInTab replaces that tab's history and shows it`() {
        navigator.navigate(QB)
        navigator.navigate(subject)
        navigator.navigate(Routine)

        navigator.openInTab(QB, listOf(exams))

        assertEquals(QB, state.topLevelRoute)
        assertEquals(listOf<NavKey>(QB, exams), state.currentStack.toList())
        assertEquals(true, navigator.anyOpen { it is MCQExamList })
    }

    @Test
    fun `back to the start tab keeps the left tab's history`() {
        navigator.navigate(QB)
        navigator.navigate(subject)
        navigator.goBack()
        navigator.goBack()

        assertEquals(Home, state.topLevelRoute)
        assertEquals(listOf<NavKey>(QB), state.backStacks.getValue(QB).toList())
    }
}
