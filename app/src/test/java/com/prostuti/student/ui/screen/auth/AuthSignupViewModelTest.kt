package com.prostuti.student.ui.screen.auth

import app.cash.turbine.test
import com.prostuti.student.data.model.enums.UserStatus
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.PasswordBasedSignUpResponse
import com.prostuti.student.data.repository.fake.FakeAuthRepository
import com.prostuti.student.domain.PasswordBasedSignUpUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthSignupViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeRepository = FakeAuthRepository()
    private fun viewModel(identifier: String = "test@example.com") =
        AuthSignupViewModel(PasswordBasedSignUpUseCase(fakeRepository), identifier)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateIsEmpty() {
        val state = viewModel().state.value
        assertEquals("", state.firstName)
        assertEquals("", state.lastName)
        assertEquals("", state.password)
        assertFalse(state.isSignedUp)
    }

    @Test
    fun signUpWithShortNamesSetsErrorsAndDoesNotCallUseCase() = runTest {
        val viewModel = viewModel()
        viewModel.updateFirstName("A")
        viewModel.updateLastName("B")
        viewModel.updatePassword("password1")
        viewModel.signUp()
        assertNotNull(viewModel.state.value.firstNameError)
        assertNotNull(viewModel.state.value.lastNameError)
        assertNull(fakeRepository.lastSignUpRequest)
    }

    @Test
    fun signUpWithPasswordMissingDigitSetsPasswordError() = runTest {
        val viewModel = viewModel()
        viewModel.updateFirstName("Rahim")
        viewModel.updateLastName("Islam")
        viewModel.updatePassword("passwordonly")
        viewModel.signUp()
        assertNotNull(viewModel.state.value.passwordError)
        assertNull(fakeRepository.lastSignUpRequest)
    }

    @Test
    fun signUpWithShortPasswordSetsPasswordError() = runTest {
        val viewModel = viewModel()
        viewModel.updateFirstName("Rahim")
        viewModel.updateLastName("Islam")
        viewModel.updatePassword("pass1")
        viewModel.signUp()
        assertNotNull(viewModel.state.value.passwordError)
        assertNull(fakeRepository.lastSignUpRequest)
    }

    @Test
    fun signUpSuccessUpdatesIsSignedUpAndDerivesUserName() = runTest {
        fakeRepository.signUpResult = ApiResponse.Success(
            PasswordBasedSignUpResponse(userId = "018f3c7e-007b-7000-8000-000000000001", userName = "rahimislam", status = UserStatus.PENDING)
        )
        val viewModel = viewModel(identifier = "test@example.com")
        viewModel.updateFirstName("Rahim")
        viewModel.updateLastName("Islam")
        viewModel.updatePassword("password1")

        viewModel.state.test {
            assertFalse(awaitItem().isSignedUp)
            viewModel.signUp()
            assertTrue(awaitItem().isLoading)
            val result = awaitItem()
            assertFalse(result.isLoading)
            assertTrue(result.isSignedUp)
        }
        assertEquals("rahimislam", fakeRepository.lastSignUpRequest?.userName)
        assertEquals("Rahim", fakeRepository.lastSignUpRequest?.firstName)
        assertEquals("Islam", fakeRepository.lastSignUpRequest?.lastName)
    }

    @Test
    fun signUpFailurePopulatesErrorsMap() = runTest {
        fakeRepository.signUpResult = ApiResponse.Failure(message = "Username taken", errorCode = "DUPLICATE")
        val viewModel = viewModel()
        viewModel.updateFirstName("Rahim")
        viewModel.updateLastName("Islam")
        viewModel.updatePassword("password1")

        viewModel.state.test {
            assertFalse(awaitItem().isSignedUp)
            viewModel.signUp()
            assertTrue(awaitItem().isLoading)
            val result = awaitItem()
            assertEquals("Username taken", result.errors["PASSWORD_BASED_SIGN_UP"])
        }
    }
}
