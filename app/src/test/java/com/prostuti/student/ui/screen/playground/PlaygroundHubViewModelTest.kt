package com.prostuti.student.ui.screen.playground

import com.prostuti.student.data.repository.fake.FakeSavedGraphRepository
import com.prostuti.student.domain.DeleteSavedGraphUseCase
import com.prostuti.student.domain.ObserveSavedGraphsUseCase
import com.prostuti.student.domain.RenameSavedGraphUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaygroundHubViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repo = FakeSavedGraphRepository()

    @Before fun setup() = Dispatchers.setMain(dispatcher)

    @After fun teardown() = Dispatchers.resetMain()

    private fun viewModel() =
        PlaygroundHubViewModel(ObserveSavedGraphsUseCase(repo), RenameSavedGraphUseCase(repo), DeleteSavedGraphUseCase(repo))

    @Test fun listsSavedGraphsNewestFirst() = runTest(dispatcher) {
        repo.save(null, "Old", "{}", "y = x", false)
        repo.save(null, "New", "{}", "z = x", true)
        val vm = viewModel()
        assertTrue(vm.state.value.isLoading)
        advanceUntilIdle()
        assertFalse(vm.state.value.isLoading)
        assertEquals(listOf("New", "Old"), vm.state.value.graphs.map { it.name })
    }

    @Test fun renameAndDeleteGoThroughDialogs() = runTest(dispatcher) {
        repo.save(null, "Graph", "{}", "y = x", false)
        val vm = viewModel()
        advanceUntilIdle()
        val graph = vm.state.value.graphs.single()
        vm.startRename(graph)
        assertEquals(graph, vm.state.value.renaming)
        vm.rename("  Parabola ")
        advanceUntilIdle()
        assertNull(vm.state.value.renaming)
        assertEquals("Parabola", vm.state.value.graphs.single().name)
        vm.startDelete(vm.state.value.graphs.single())
        vm.confirmDelete()
        advanceUntilIdle()
        assertTrue(vm.state.value.graphs.isEmpty())
    }

    @Test fun blankRenameIsIgnored() = runTest(dispatcher) {
        repo.save(null, "Keep", "{}", "", false)
        val vm = viewModel()
        advanceUntilIdle()
        vm.startRename(vm.state.value.graphs.single())
        vm.rename("   ")
        advanceUntilIdle()
        assertEquals("Keep", vm.state.value.graphs.single().name)
    }
}
