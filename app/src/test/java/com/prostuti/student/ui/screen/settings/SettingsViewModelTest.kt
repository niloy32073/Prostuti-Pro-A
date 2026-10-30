package com.prostuti.student.ui.screen.settings

import app.cash.turbine.test
import com.prostuti.student.data.repository.fake.FakeDataStoreStorageRepository
import com.prostuti.student.domain.ClearDataStoreUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeStorage: FakeDataStoreStorageRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeStorage = FakeDataStoreStorageRepository()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = SettingsViewModel(ClearDataStoreUseCase(fakeStorage))

    @Test fun initialStateIsNotLoggedOut() {
        assertFalse(viewModel().state.value.isLoggedOut)
    }

    @Test fun logoutClearsStorageAndMarksStateLoggedOut() = runTest {
        val vm = viewModel()

        vm.state.test {
            assertFalse(awaitItem().isLoggedOut)
            vm.logout()
            assertTrue(awaitItem().isLoggedOut)
        }

        assertEquals(1, fakeStorage.clearAllCallCount)
    }
}
