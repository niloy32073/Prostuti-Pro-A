package com.prostuti.student.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.prostuti.student.BuildConfig
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.RefreshTokenResponse
import com.prostuti.student.data.repository.AuthRepository
import com.prostuti.student.data.repository.AuthRepositoryImpl
import com.prostuti.student.data.repository.CurriculumRepository
import com.prostuti.student.data.repository.CurriculumRepositoryImpl
import com.prostuti.student.data.repository.GroupExamRepository
import com.prostuti.student.data.repository.LiveExamRepository
import com.prostuti.student.data.repository.MCQExamRepository
import com.prostuti.student.data.repository.MCQExamRepositoryImpl
import com.prostuti.student.data.repository.StorageRepository
import com.prostuti.student.data.repository.StorageRepositoryImpl
import com.prostuti.student.data.repository.TimedExamRepository
import com.prostuti.student.data.repository.fake.FakeGroupExamRepository
import com.prostuti.student.data.repository.fake.FakeLiveExamRepository
import com.prostuti.student.data.repository.fake.FakeTimedExamRepository
import com.prostuti.student.data.service.remote.AuthApiService
import com.prostuti.student.data.service.remote.CurriculumApiService
import com.prostuti.student.data.service.remote.MCQExamApiService
import com.prostuti.student.dataStore.DATA_STORE_FILE_NAME
import com.prostuti.student.dataStore.PrefKeys
import com.prostuti.student.dataStore.createDataStore
import com.prostuti.student.domain.AdvanceDemoStateUseCase
import com.prostuti.student.domain.AttemptMCQExamUseCase
import com.prostuti.student.domain.AutoSubmitAbandonedAttemptUseCase
import com.prostuti.student.domain.CheckUserUseCase
import com.prostuti.student.domain.ClearDataStoreUseCase
import com.prostuti.student.domain.CreateGroupRoutineUseCase
import com.prostuti.student.domain.CreateGroupUseCase
import com.prostuti.student.domain.FetchChaptersUseCase
import com.prostuti.student.domain.FetchGroupDetailUseCase
import com.prostuti.student.domain.FetchGroupMembersUseCase
import com.prostuti.student.domain.FetchGroupPastExamsUseCase
import com.prostuti.student.domain.FetchInProgressAnswersUseCase
import com.prostuti.student.domain.FetchLiveExamDetailUseCase
import com.prostuti.student.domain.FetchLiveExamListUseCase
import com.prostuti.student.domain.FetchMCQExamsUseCase
import com.prostuti.student.domain.FetchMyAnswerSheetUseCase
import com.prostuti.student.domain.FetchMyGroupsUseCase
import com.prostuti.student.domain.FetchMyTimedSubmissionUseCase
import com.prostuti.student.domain.FetchPassageUseCase
import com.prostuti.student.domain.FetchSubjectsUseCase
import com.prostuti.student.domain.FetchTimedExamDurationUseCase
import com.prostuti.student.domain.FetchTimedExamQuestionsUseCase
import com.prostuti.student.domain.FetchTimedExamWindowUseCase
import com.prostuti.student.domain.FetchTopicsUseCase
import com.prostuti.student.domain.FetchWindowRankingUseCase
import com.prostuti.student.domain.GenerateExamUseCase
import com.prostuti.student.domain.GetAttemptQuestionsUseCase
import com.prostuti.student.domain.GetAttemptReviewUseCase
import com.prostuti.student.domain.GetBooleanFromDataStoreUseCase
import com.prostuti.student.domain.GetExamAttemptUseCase
import com.prostuti.student.domain.GetExamLeaderboardUseCase
import com.prostuti.student.domain.GetStringFromDataStoreUseCase
import com.prostuti.student.domain.JoinGroupUseCase
import com.prostuti.student.domain.ListMyExamAttemptsUseCase
import com.prostuti.student.domain.PasswordBasedSignInUseCase
import com.prostuti.student.domain.PasswordBasedSignUpUseCase
import com.prostuti.student.domain.ResetPasswordUseCase
import com.prostuti.student.domain.SaveBooleanToDataStoreUseCase
import com.prostuti.student.domain.SaveTimedExamAnswerUseCase
import com.prostuti.student.domain.SendOTPUseCase
import com.prostuti.student.domain.SocialSignInUseCase
import com.prostuti.student.domain.SubmitExamAttemptUseCase
import com.prostuti.student.domain.SubmitExamResponsesUseCase
import com.prostuti.student.domain.SubmitMcqReportUseCase
import com.prostuti.student.domain.SubmitIntegrityEventsUseCase
import com.prostuti.student.domain.SubmitTimedExamUseCase
import com.prostuti.student.domain.VerifyEmailUseCase
import com.prostuti.student.domain.VerifyPhoneUseCase
import com.prostuti.student.domain.VerifyResetOtpUseCase
import com.prostuti.student.ui.screen.SharedViewModel
import com.prostuti.student.ui.screen.group.GroupDetailViewModel
import com.prostuti.student.ui.screen.group.GroupListViewModel
import com.prostuti.student.ui.screen.group.GroupMembersViewModel
import com.prostuti.student.ui.screen.group.GroupPastExamsViewModel
import com.prostuti.student.ui.screen.group.GroupRoutineCalendarViewModel
import com.prostuti.student.ui.screen.group.GroupRoutineChapterViewModel
import com.prostuti.student.ui.screen.group.GroupRoutineSubjectViewModel
import com.prostuti.student.ui.screen.group.GroupRoutineWindowViewModel
import com.prostuti.student.ui.screen.live.LiveExamDetailViewModel
import com.prostuti.student.ui.screen.live.LiveExamListViewModel
import com.prostuti.student.ui.screen.timedexam.LockedResultViewModel
import com.prostuti.student.ui.screen.timedexam.TimedAnswerSheetViewModel
import com.prostuti.student.ui.screen.timedexam.TimedExamAttemptViewModel
import com.prostuti.student.ui.screen.timedexam.WindowRankingViewModel
import com.prostuti.student.ui.screen.auth.AuthLoginViewModel
import com.prostuti.student.ui.screen.auth.AuthResetNewPasswordViewModel
import com.prostuti.student.ui.screen.auth.AuthResetRequestViewModel
import com.prostuti.student.ui.screen.auth.AuthSignupViewModel
import com.prostuti.student.ui.screen.auth.AuthStartViewModel
import com.prostuti.student.ui.screen.auth.AuthVerifyOtpViewModel
import com.prostuti.student.ui.screen.home.HomeViewModel
import com.prostuti.student.ui.screen.intro.OnboardingViewModel
import com.prostuti.student.ui.screen.examengine.ExamAnswerSheetViewModel
import com.prostuti.student.ui.screen.examengine.ExamAttemptViewModel
import com.prostuti.student.ui.screen.examengine.ExamLeaderboardViewModel
import com.prostuti.student.ui.screen.examengine.ExamResultViewModel
import com.prostuti.student.ui.screen.practice.PracticeChapterSelectViewModel
import com.prostuti.student.ui.screen.practice.PracticeExamPropertiesViewModel
import com.prostuti.student.ui.screen.practice.PracticeSubjectChapterViewModel
import com.prostuti.student.ui.screen.practice.PracticeSubjectSelectViewModel
import com.prostuti.student.ui.screen.qb.ChapterBrowseViewModel
import com.prostuti.student.ui.screen.qb.MCQExamListViewModel
import com.prostuti.student.ui.screen.qb.QuestionReaderViewModel
import com.prostuti.student.ui.screen.qb.SubjectListViewModel
import com.prostuti.student.ui.screen.settings.SettingsViewModel
import com.prostuti.student.util.RequestLogging
import com.prostuti.student.util.SystemTimeProvider
import com.prostuti.student.util.TimeProvider
import com.prostuti.student.util.safeApiCall
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CancellationException
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import timber.log.Timber

val appModule = module {
    includes(databaseModule)

    val baseUrl: String = BuildConfig.BASE_URL + "/api/v1/"

    single<HttpClient> {
        HttpClient {
            defaultRequest {
                url { takeFrom(baseUrl) }
            }
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
            install(HttpTimeout)
            if (BuildConfig.DEBUG) {
                install(RequestLogging)
            }
            install(Auth) {
                bearer {
                    loadTokens {
                        val storageRepository: StorageRepository = get()
                        val token = storageRepository.getJWTToken()
                        val refreshToken = storageRepository.getRefreshToken()

                        if (token != null && refreshToken != null) {
                            BearerTokens(accessToken = token, refreshToken = refreshToken)
                        } else {
                            null
                        }
                    }

                    sendWithoutRequest { request ->
                        val path = request.url.encodedPath
                        !path.endsWith("/login") && !path.endsWith("/refresh")
                    }

                    refreshTokens {
                        val storageRepository: StorageRepository = get()

                        try {
                            val refreshToken = oldTokens?.refreshToken
                            if (refreshToken.isNullOrBlank()) {
                                Timber.w("Token refresh skipped: no refresh token in cache")
                                storageRepository.remove(PrefKeys.JWT_TOKEN)
                                storageRepository.remove(PrefKeys.JWT_REFRESH_TOKEN)
                                return@refreshTokens null
                            }

                            val response: ApiResponse<RefreshTokenResponse> = safeApiCall {
                                client.post("auth/refresh") {
                                    contentType(ContentType.Application.Json)
                                    setBody(mapOf("refresh_token" to refreshToken))
                                }
                            }

                            when (response) {
                                is ApiResponse.Success -> {
                                    val newData = response.data
                                    Timber.d("Token refreshed successfully")
                                    storageRepository.putString(key = PrefKeys.JWT_TOKEN, value = newData.accessToken)
                                    storageRepository.putString(key = PrefKeys.JWT_REFRESH_TOKEN, value = newData.refreshToken)
                                    BearerTokens(
                                        accessToken = newData.accessToken,
                                        refreshToken = newData.refreshToken
                                    )
                                }

                                is ApiResponse.Failure -> {
                                    Timber.w("Token refresh failed: ${response.message} (${response.errorCode})")
                                    // Only clear tokens — do NOT clearAll() so IS_FIRST_TIME is preserved
                                    storageRepository.remove(PrefKeys.JWT_TOKEN)
                                    storageRepository.remove(PrefKeys.JWT_REFRESH_TOKEN)
                                    null
                                }
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Timber.e(e, "Token refresh threw unexpectedly")
                            // Network/timeout errors should NOT log the user out
                            null
                        }
                    }
                }
            }
        }
    }

    single<TimeProvider> { SystemTimeProvider() }

    single<DataStore<Preferences>> {
        createDataStore(
            producePath = {
                androidContext().filesDir.resolve(DATA_STORE_FILE_NAME).absolutePath
            }
        )
    }

    single<StorageRepository> { StorageRepositoryImpl(get()) }

    single { AuthApiService(get()) }
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }

    single { CurriculumApiService(get()) }
    single<CurriculumRepository> { CurriculumRepositoryImpl(get(), get(), get(), get()) }

    single { MCQExamApiService(get()) }
    single<MCQExamRepository> { MCQExamRepositoryImpl(get()) }

    // Group/live exam — fake, Room-backed where the real backend doesn't exist yet. Swapping in
    // the real API later is a single-line change here, nothing above or below it.
    single<TimedExamRepository> { FakeTimedExamRepository(get(), get(), get(), get()) }
    single<GroupExamRepository> { FakeGroupExamRepository(get(), get(), get(), get()) }
    single<LiveExamRepository> { FakeLiveExamRepository(get()) }

    factory { CheckUserUseCase(get()) }
    factory { PasswordBasedSignInUseCase(get()) }
    factory { PasswordBasedSignUpUseCase(get()) }
    factory { SendOTPUseCase(get()) }
    factory { SocialSignInUseCase(get()) }
    factory { VerifyEmailUseCase(get()) }
    factory { VerifyPhoneUseCase(get()) }
    factory { VerifyResetOtpUseCase(get()) }
    factory { ResetPasswordUseCase(get()) }

    factory { GetStringFromDataStoreUseCase(get()) }
    factory { GetBooleanFromDataStoreUseCase(get()) }
    factory { SaveBooleanToDataStoreUseCase(get()) }
    factory { ClearDataStoreUseCase(get()) }

    factory { FetchSubjectsUseCase(get()) }
    factory { FetchChaptersUseCase(get()) }
    factory { FetchTopicsUseCase(get()) }
    factory { FetchMCQExamsUseCase(get()) }
    factory { AttemptMCQExamUseCase(get()) }
    factory { FetchPassageUseCase(get()) }
    factory { GenerateExamUseCase(get()) }
    factory { GetAttemptQuestionsUseCase(get()) }
    factory { SubmitExamResponsesUseCase(get()) }
    factory { SubmitExamAttemptUseCase(get()) }
    factory { GetExamAttemptUseCase(get()) }
    factory { ListMyExamAttemptsUseCase(get()) }
    factory { SubmitIntegrityEventsUseCase(get()) }
    factory { GetAttemptReviewUseCase(get()) }
    factory { GetExamLeaderboardUseCase(get()) }
    factory { SubmitMcqReportUseCase(get()) }

    factory { FetchMyGroupsUseCase(get()) }
    factory { FetchGroupDetailUseCase(get()) }
    factory { CreateGroupRoutineUseCase(get()) }
    factory { CreateGroupUseCase(get()) }
    factory { JoinGroupUseCase(get()) }
    factory { FetchGroupPastExamsUseCase(get()) }
    factory { FetchGroupMembersUseCase(get()) }
    factory { FetchLiveExamListUseCase(get()) }
    factory { FetchLiveExamDetailUseCase(get()) }
    factory { FetchTimedExamQuestionsUseCase(get()) }
    factory { FetchTimedExamWindowUseCase(get()) }
    factory { FetchTimedExamDurationUseCase(get()) }
    factory { SaveTimedExamAnswerUseCase(get()) }
    factory { SubmitTimedExamUseCase(get()) }
    factory { AutoSubmitAbandonedAttemptUseCase(get()) }
    factory { FetchMyTimedSubmissionUseCase(get()) }
    factory { FetchInProgressAnswersUseCase(get()) }
    factory { FetchWindowRankingUseCase(get()) }
    factory { FetchMyAnswerSheetUseCase(get()) }
    factory { AdvanceDemoStateUseCase(get()) }

    viewModel { SharedViewModel(get(), get()) }
    viewModel { OnboardingViewModel(get()) }
    viewModel { AuthStartViewModel(get()) }
    viewModel { params -> AuthLoginViewModel(get(), params.get(), params.getOrNull()) }
    viewModel { params -> AuthSignupViewModel(get(), params.get()) }
    viewModel { params -> AuthVerifyOtpViewModel(get(), get(), get(), params.get()) }
    viewModel { params -> AuthResetRequestViewModel(get(), get(), params.get()) }
    viewModel { params -> AuthResetNewPasswordViewModel(get(), params.get()) }
    viewModel { HomeViewModel() }
    viewModel { SettingsViewModel(get()) }

    viewModel { SubjectListViewModel(get()) }
    viewModel { params -> MCQExamListViewModel(params.get(), get(), get()) }
    viewModel { params -> ExamAttemptViewModel(params.get(), get(), get(), get(), get(), get()) }
    viewModel { params -> ExamResultViewModel(params.get(), get()) }
    viewModel { params -> ExamAnswerSheetViewModel(params.get(), get()) }
    viewModel { params -> ExamLeaderboardViewModel(params.get(), get()) }
    viewModel { PracticeSubjectSelectViewModel(get()) }
    viewModel { PracticeSubjectChapterViewModel(get(), get(), get()) }
    viewModel { params -> PracticeChapterSelectViewModel(params.get(), get(), get(), get()) }
    viewModel { params -> PracticeExamPropertiesViewModel(params.get(), params.get(), params.get(), get()) }
    viewModel { QuestionReaderViewModel() }
    viewModel { params -> ChapterBrowseViewModel(params.get()) }

    // ── Group exam ──
    viewModel { GroupListViewModel(get(), get(), get(), get(), get()) }
    viewModel { params -> GroupDetailViewModel(params.get(), get(), get(), get()) }
    viewModel { params -> GroupRoutineSubjectViewModel(params.get(), get()) }
    viewModel { params -> GroupRoutineChapterViewModel(params.get(), params.get(), get(), get(), get()) }
    viewModel { params -> GroupRoutineWindowViewModel(params.get(), params.get(), params.get(), params.get(), get()) }
    viewModel { params -> GroupRoutineCalendarViewModel(params.get(), get()) }
    viewModel { params -> GroupPastExamsViewModel(params.get(), get()) }
    viewModel { params -> GroupMembersViewModel(params.get(), get()) }

    // ── Live exam ──
    viewModel { LiveExamListViewModel(get(), get(), get()) }
    viewModel { params -> LiveExamDetailViewModel(params.get(), get(), get(), get(), get()) }

    // ── Shared timed-exam attempt/result/ranking/answer-sheet (live + group) ──
    viewModel { params -> TimedExamAttemptViewModel(params.get(), params.get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { params -> LockedResultViewModel(params.get(), params.get(), get(), get(), get()) }
    viewModel { params -> WindowRankingViewModel(params.get(), params.get(), get()) }
    viewModel { params -> TimedAnswerSheetViewModel(params.get(), get()) }
}
