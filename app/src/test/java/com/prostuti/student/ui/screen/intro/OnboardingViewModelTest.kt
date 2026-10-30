package com.prostuti.student.ui.screen.intro

import app.cash.turbine.test
import com.prostuti.student.data.repository.fake.FakeDataStoreStorageRepository
import com.prostuti.student.dataStore.PrefKeys
import com.prostuti.student.domain.SaveBooleanToDataStoreUseCase
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
class OnboardingViewModelTest {

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

    private fun viewModel() = OnboardingViewModel(SaveBooleanToDataStoreUseCase(fakeStorage))

    @Test fun exposesExactlyFiveOnboardingCards() {
        assertEquals(5, viewModel().currentCards.size)
    }

    @Test fun initialStateIsNotFinished() {
        assertFalse(viewModel().state.value.isFinished)
    }

    @Test fun finishPersistsIsFirstTimeFalseAndMarksStateFinished() = runTest {
        val vm = viewModel()

        vm.state.test {
            assertFalse(awaitItem().isFinished)
            vm.finish()
            assertTrue(awaitItem().isFinished)
        }

        assertEquals(listOf(PrefKeys.IS_FIRST_TIME to false), fakeStorage.putBooleanCalls)
    }
}
