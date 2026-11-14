package com.prostuti.student.di

import com.prostuti.student.ui.screen.FeatureAccessViewModel
import com.prostuti.student.ui.screen.home.HomeNotificationsViewModel
import com.prostuti.student.ui.screen.notification.NotificationsViewModel
import com.prostuti.student.navigation.AppLinkRequests
import com.prostuti.student.data.repository.WidgetSnapshotRepository
import com.prostuti.student.data.repository.WidgetSnapshotRepositoryImpl
import com.prostuti.student.domain.widget.LoadWidgetDataUseCase
import com.prostuti.student.ui.widget.WidgetSync
import com.prostuti.student.ui.widget.WidgetPreviewPublisher
import com.prostuti.student.ui.widget.GlanceHomeWidgetHost
import com.prostuti.student.domain.widget.HomeWidgetHost
import com.prostuti.student.data.repository.WidgetPromoRepository
import com.prostuti.student.data.repository.WidgetPromoRepositoryImpl
import com.prostuti.student.domain.widget.ShouldShowWidgetPromoUseCase
import com.prostuti.student.domain.widget.DismissWidgetPromoUseCase
import com.prostuti.student.domain.widget.AddHomeWidgetUseCase
import com.prostuti.student.ui.screen.home.HomeWidgetPromoViewModel
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.prostuti.student.BuildConfig
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.RefreshTokenResponse
import com.prostuti.student.data.repository.AuthRepository
import com.prostuti.student.data.repository.AuthRepositoryImpl
import com.prostuti.student.data.repository.UserRepository
import com.prostuti.student.data.repository.UserRepositoryImpl
import com.prostuti.student.data.repository.CurriculumRepository
import com.prostuti.student.data.repository.CurriculumRepositoryImpl
import com.prostuti.student.data.repository.LiveExamRepository
import com.prostuti.student.data.repository.AttemptProgressRepository
import com.prostuti.student.data.repository.AttemptProgressRepositoryImpl
import com.prostuti.student.data.repository.ExamRepository
import com.prostuti.student.data.repository.ExamRepositoryImpl
import com.prostuti.student.data.repository.MCQRepository
import com.prostuti.student.data.repository.MCQRepositoryImpl
import com.prostuti.student.data.repository.BookmarkRepository
import com.prostuti.student.data.repository.BookmarkRepositoryImpl
import com.prostuti.student.data.repository.ReportRepository
import com.prostuti.student.data.repository.ReportRepositoryImpl
import com.prostuti.student.data.service.remote.BookmarkApiService
import com.prostuti.student.data.repository.StorageRepository
import com.prostuti.student.data.repository.StorageRepositoryImpl
import com.prostuti.student.data.repository.TimedExamRepository
import com.prostuti.student.data.repository.fake.FakeLiveExamRepository
import com.prostuti.student.data.repository.fake.FakeTimedExamRepository
import com.prostuti.student.data.service.remote.AuthApiService
import com.prostuti.student.data.service.remote.CurriculumApiService
import com.prostuti.student.data.service.remote.ExamApiService
import com.prostuti.student.data.service.remote.MCQApiService
import com.prostuti.student.dataStore.DATA_STORE_FILE_NAME
import com.prostuti.student.dataStore.PrefKeys
import com.prostuti.student.dataStore.createDataStore
import com.prostuti.student.domain.AdvanceDemoStateUseCase
import com.prostuti.student.domain.calculator.EquationSolver
import com.prostuti.student.domain.calculator.ExpressionEvaluator
import com.prostuti.student.domain.scratchpad.StrokeEraser
import com.prostuti.student.domain.graph.GraphCompiler
import com.prostuti.student.domain.graph.plot.GraphPlotter
import com.prostuti.student.domain.graph.space.SpacePlotter
import com.prostuti.student.ui.screen.playground.graph.GraphBuilderViewModel
import com.prostuti.student.ui.screen.playground.PlaygroundHubViewModel
import com.prostuti.student.ui.screen.playground.code.CodeVisualizerViewModel
import com.prostuti.student.domain.BuildCodeFlowUseCase
import com.prostuti.student.domain.InstallPythonRuntimeUseCase
import com.prostuti.student.domain.ObservePythonRuntimeUseCase
import com.prostuti.student.domain.RunPythonUseCase
import com.prostuti.student.domain.RunCUseCase
import com.prostuti.student.domain.SendRunInputUseCase
import com.prostuti.student.domain.SendTraceCommandUseCase
import com.prostuti.student.data.repository.CodeRunRepository
import com.prostuti.student.data.repository.CodeRunRepositoryImpl
import com.prostuti.student.data.repository.PythonRuntimeRepository
import com.prostuti.student.data.repository.PythonRuntimeRepositoryImpl
import com.prostuti.student.data.service.python.PythonInstallConfirmation
import com.prostuti.student.data.repository.SavedGraphRepository
import com.prostuti.student.data.repository.SavedGraphRepositoryImpl
import com.prostuti.student.domain.ObserveSavedGraphsUseCase
import com.prostuti.student.domain.GetSavedGraphUseCase
import com.prostuti.student.domain.SaveGraphUseCase
import com.prostuti.student.domain.RenameSavedGraphUseCase
import com.prostuti.student.domain.DeleteSavedGraphUseCase
import com.prostuti.student.navigation.GraphBuilder
import com.prostuti.student.domain.StartOrResumeExamAttemptUseCase
import com.prostuti.student.data.repository.GroupRepository
import com.prostuti.student.data.repository.GroupRepositoryImpl
import com.prostuti.student.data.repository.NotificationRepository
import com.prostuti.student.data.repository.NotificationRepositoryImpl
import com.prostuti.student.data.repository.RoutineRepository
import com.prostuti.student.data.repository.RoutineRepositoryImpl
import com.prostuti.student.data.service.remote.GroupApiService
import com.prostuti.student.data.service.remote.NotificationApiService
import com.prostuti.student.data.service.remote.FirebasePushTokenSource
import com.prostuti.student.data.service.remote.PushTokenSource
import com.prostuti.student.data.service.remote.RoutineApiService
import com.prostuti.student.domain.group.AddGroupMemberUseCase
import com.prostuti.student.domain.group.FetchGroupMembersUseCase
import com.prostuti.student.domain.group.FetchMyGroupsUseCase
import com.prostuti.student.domain.group.CreateGroupUseCase
import com.prostuti.student.domain.group.ApproveJoinRequestUseCase
import com.prostuti.student.domain.group.DeleteGroupUseCase
import com.prostuti.student.domain.group.ExploreGroupsUseCase
import com.prostuti.student.domain.group.FetchGroupUseCase
import com.prostuti.student.domain.group.FetchJoinRequestsUseCase
import com.prostuti.student.domain.group.JoinGroupByCodeUseCase
import com.prostuti.student.domain.group.LeaveGroupUseCase
import com.prostuti.student.domain.group.RegenerateGroupCodeUseCase
import com.prostuti.student.domain.group.RejectJoinRequestUseCase
import com.prostuti.student.domain.group.RemoveGroupMemberUseCase
import com.prostuti.student.domain.group.RequestToJoinGroupUseCase
import com.prostuti.student.domain.group.SetGroupMemberRoleUseCase
import com.prostuti.student.domain.group.UpdateGroupUseCase
import com.prostuti.student.domain.notification.FetchNotificationsUseCase
import com.prostuti.student.domain.notification.MarkNotificationReadUseCase
import com.prostuti.student.domain.notification.ClearPushTokenUseCase
import com.prostuti.student.domain.routine.CreateAdhocExamUseCase
import com.prostuti.student.domain.routine.CreateRoutineUseCase
import com.prostuti.student.domain.routine.DeleteAdhocExamUseCase
import com.prostuti.student.domain.routine.DeleteRoutineUseCase
import com.prostuti.student.domain.routine.FetchActiveRoutineUseCase
import com.prostuti.student.domain.routine.FetchSlotsUseCase
import com.prostuti.student.domain.routine.DescribeTopicGroupingUseCase
import com.prostuti.student.domain.routine.OpenScheduledExamUseCase
import com.prostuti.student.domain.routine.FetchPastSlotsUseCase
import com.prostuti.student.data.repository.GroupExamStatsProvider
import com.prostuti.student.data.repository.DummyGroupExamStatsProvider
import com.prostuti.student.ui.screen.group.GroupRankingViewModel
import com.prostuti.student.domain.routine.FindMyAttemptForExamUseCase
import com.prostuti.student.domain.routine.PreviewRoutineUseCase
import com.prostuti.student.domain.routine.RescheduleSlotUseCase
import com.prostuti.student.domain.routine.StartScheduledExamUseCase
import com.prostuti.student.domain.routine.StartSlotUseCase
import com.prostuti.student.domain.AutoSubmitAbandonedAttemptUseCase
import com.prostuti.student.domain.CheckUserUseCase
import com.prostuti.student.domain.ClearCachedMeUseCase
import com.prostuti.student.domain.ClearDataStoreUseCase
import com.prostuti.student.domain.GetMeUseCase
import com.prostuti.student.domain.ObserveThemeModeUseCase
import com.prostuti.student.domain.SetThemeModeUseCase
import com.prostuti.student.data.repository.ThemeRepository
import com.prostuti.student.data.repository.ThemeRepositoryImpl
import com.prostuti.student.domain.FetchChaptersUseCase
import com.prostuti.student.domain.FetchInProgressAnswersUseCase
import com.prostuti.student.domain.FetchLiveExamDetailUseCase
import com.prostuti.student.domain.FetchLiveExamListUseCase
import com.prostuti.student.domain.FetchMCQExamsUseCase
import com.prostuti.student.domain.AddBookmarkUseCase
import com.prostuti.student.domain.ClearBookmarksUseCase
import com.prostuti.student.domain.FetchMcqUseCase
import com.prostuti.student.domain.ObserveBookmarkedIdsUseCase
import com.prostuti.student.domain.ObserveBookmarksUseCase
import com.prostuti.student.domain.RemoveBookmarkUseCase
import com.prostuti.student.domain.FetchMyAnswerSheetUseCase
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
import com.prostuti.student.domain.ClearLocalAttemptElapsedUseCase
import com.prostuti.student.domain.GetAttemptResponsesUseCase
import com.prostuti.student.domain.GetLocalAttemptElapsedUseCase
import com.prostuti.student.domain.SaveLocalAttemptElapsedUseCase
import com.prostuti.student.domain.GetResumablePracticeAttemptUseCase
import com.prostuti.student.domain.GetAttemptAnalysisUseCase
import com.prostuti.student.domain.GetAttemptAnswerSheetUseCase
import com.prostuti.student.domain.GetExamHistoryUseCase
import com.prostuti.student.domain.GetBooleanFromDataStoreUseCase
import com.prostuti.student.domain.GetExamAttemptUseCase
import com.prostuti.student.domain.GetExamLeaderboardUseCase
import com.prostuti.student.domain.GetExamQuestionsWithOptionsUseCase
import com.prostuti.student.domain.GetStringFromDataStoreUseCase
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
import com.prostuti.student.domain.SyncBookmarksIfNeededUseCase
import com.prostuti.student.domain.ObserveReportedIdsUseCase
import com.prostuti.student.domain.ClearReportsUseCase
import com.prostuti.student.domain.SubmitIntegrityEventsUseCase
import com.prostuti.student.domain.SubmitTimedExamUseCase
import com.prostuti.student.domain.VerifyEmailUseCase
import com.prostuti.student.domain.VerifyPhoneUseCase
import com.prostuti.student.ui.screen.SharedViewModel
import com.prostuti.student.ui.screen.group.GroupDetailViewModel
import com.prostuti.student.ui.screen.group.GroupNoticeBus
import com.prostuti.student.ui.screen.routine.builder.RoutineBuilderViewModel
import com.prostuti.student.ui.screen.group.GroupExploreViewModel
import com.prostuti.student.ui.screen.group.GroupJoinRequestsViewModel
import com.prostuti.student.ui.screen.group.GroupSettingsViewModel
import com.prostuti.student.ui.screen.group.GroupListViewModel
import com.prostuti.student.ui.screen.group.GroupMembersViewModel
import com.prostuti.student.ui.screen.group.GroupPastExamsViewModel
import com.prostuti.student.ui.screen.group.GroupRoutineCalendarViewModel
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
import com.prostuti.student.ui.screen.home.HomeGroupExamViewModel
import com.prostuti.student.ui.screen.routine.RoutineTabViewModel
import com.prostuti.student.ui.screen.profile.ProfileViewModel
import com.prostuti.student.ui.screen.intro.OnboardingViewModel
import com.prostuti.student.ui.screen.examengine.ExamAnswerSheetViewModel
import com.prostuti.student.ui.screen.examengine.ExamAttemptViewModel
import com.prostuti.student.ui.screen.examengine.ExamLeaderboardViewModel
import com.prostuti.student.ui.screen.examengine.ExamResultViewModel
import com.prostuti.student.ui.screen.examengine.LockedExamResultViewModel
import com.prostuti.student.ui.screen.examengine.calculator.CalculatorViewModel
import com.prostuti.student.ui.screen.examengine.scratchpad.ScratchpadViewModel
import com.prostuti.student.ui.screen.practice.PracticeChapterSelectViewModel
import com.prostuti.student.ui.screen.practice.PracticeExamPropertiesViewModel
import com.prostuti.student.ui.screen.practice.PracticeFlowViewModel
import com.prostuti.student.ui.screen.practice.PracticeSubjectChapterViewModel
import com.prostuti.student.ui.screen.practice.PracticeSubjectSelectViewModel
import com.prostuti.student.ui.screen.qb.ChapterBrowseViewModel
import com.prostuti.student.ui.screen.qb.MCQExamListViewModel
import com.prostuti.student.ui.screen.qb.QuestionReaderViewModel
import com.prostuti.student.ui.screen.qb.SubjectListViewModel
import com.prostuti.student.ui.screen.settings.SettingsViewModel
import com.prostuti.student.ui.screen.history.ExamHistoryViewModel
import com.prostuti.student.ui.screen.analysis.AttemptAnalysisViewModel
import com.prostuti.student.ui.screen.bookmark.BookmarkHistoryViewModel
import com.prostuti.student.util.ApiErrorMessages
import com.prostuti.student.util.RequestLogging
import com.prostuti.student.util.ResourceApiErrorMessages
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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.prostuti.student.util.CrashlyticsUserSync
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.binds
import org.koin.dsl.module
import timber.log.Timber

/** Koin name of the dispatcher the graph builder plots on. */
const val GRAPH_COMPUTE_DISPATCHER = "graphCompute"

/** App-lifetime scope for work that must not die with a screen (push-token upload). */
const val APP_SCOPE = "appScope"

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
    single<CoroutineScope>(named(APP_SCOPE)) { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get(), get(), get(named(APP_SCOPE))) }
    // Release only (started from App): FirebaseCrashlytics is resolved when this is first created.
    single { CrashlyticsUserSync(FirebaseCrashlytics.getInstance(), get(), get(named(APP_SCOPE))) }

    single { CurriculumApiService(get()) }
    single<CurriculumRepository> { CurriculumRepositoryImpl(get(), get(), get(), get()) }

    single { ExamApiService(get()) }
    single<ExamRepository> { ExamRepositoryImpl(get()) }

    single { MCQApiService(get()) }
    single<MCQRepository> { MCQRepositoryImpl(get()) }
    single { BookmarkApiService(get()) }
    single<BookmarkRepository> { BookmarkRepositoryImpl(get(), get(), get()) }
    single<ReportRepository> { ReportRepositoryImpl(get(), get(), get()) }
    single<AttemptProgressRepository> { AttemptProgressRepositoryImpl(get(), get()) }

    // Study groups, exam routines, notifications — real API (GROUP_ROUTINE_PLAN.md).
    single { GroupApiService(get()) }
    single<GroupRepository> { GroupRepositoryImpl(get()) }
    single { RoutineApiService(get()) }
    single<RoutineRepository> { RoutineRepositoryImpl(get()) }
    single { NotificationApiService(get()) }
    single<PushTokenSource> { FirebasePushTokenSource() }
    single<NotificationRepository> { NotificationRepositoryImpl(get(), get()) }
    single<ApiErrorMessages> { ResourceApiErrorMessages(androidContext()) }
    single { GroupNoticeBus() }
    single { AppLinkRequests() }

    // Home screen widget (ui/widget).
    single<WidgetSnapshotRepository> { WidgetSnapshotRepositoryImpl(get()) }
    factory { LoadWidgetDataUseCase(get(), get(), get(), get(), get(), get(), get()) }
    single { WidgetSync(androidContext(), get(), get(), get(), get(named(APP_SCOPE))) }
    single { WidgetPreviewPublisher(androidContext(), get(), get(named(APP_SCOPE))) }
    single<HomeWidgetHost> { GlanceHomeWidgetHost(androidContext()) }
    single<WidgetPromoRepository> { WidgetPromoRepositoryImpl(get()) }
    factory { ShouldShowWidgetPromoUseCase(get(), get()) }
    factory { DismissWidgetPromoUseCase(get()) }
    factory { AddHomeWidgetUseCase(get()) }

    // Group/live exam — fake, Room-backed where the real backend doesn't exist yet. Swapping in
    // the real API later is a single-line change here, nothing above or below it.
    single<TimedExamRepository> { FakeTimedExamRepository(get(), get(), get()) }
    single<LiveExamRepository> { FakeLiveExamRepository(get()) }

    factory { CheckUserUseCase(get()) }
    factory { PasswordBasedSignInUseCase(get()) }
    factory { PasswordBasedSignUpUseCase(get()) }
    factory { SendOTPUseCase(get()) }
    factory { SocialSignInUseCase(get()) }
    factory { VerifyEmailUseCase(get()) }
    factory { VerifyPhoneUseCase(get()) }
    factory { ResetPasswordUseCase(get()) }

    single<UserRepository> { UserRepositoryImpl(get(), get()) }
    factory { GetMeUseCase(get()) }
    single<ThemeRepository> { ThemeRepositoryImpl(androidContext()) }
    factory { ObserveThemeModeUseCase(get()) }
    factory { SetThemeModeUseCase(get()) }
    factory { ClearCachedMeUseCase(get()) }

    factory { GetStringFromDataStoreUseCase(get()) }
    factory { GetBooleanFromDataStoreUseCase(get()) }
    factory { SaveBooleanToDataStoreUseCase(get()) }
    factory { ClearDataStoreUseCase(get()) }

    factory { FetchSubjectsUseCase(get()) }
    factory { FetchChaptersUseCase(get()) }
    factory { FetchTopicsUseCase(get()) }
    factory { FetchMCQExamsUseCase(get()) }
    factory { StartOrResumeExamAttemptUseCase(get()) }
    factory { FetchPassageUseCase(get()) }
    factory { GenerateExamUseCase(get()) }
    factory { GetAttemptQuestionsUseCase(get()) }
    factory { GetAttemptResponsesUseCase(get()) }
    factory { GetLocalAttemptElapsedUseCase(get()) }
    factory { SaveLocalAttemptElapsedUseCase(get()) }
    factory { ClearLocalAttemptElapsedUseCase(get()) }
    factory { GetResumablePracticeAttemptUseCase(get(), get()) }
    factory { SubmitExamResponsesUseCase(get()) }
    factory { SubmitExamAttemptUseCase(get()) }
    factory { GetExamAttemptUseCase(get()) }
    factory { GetExamQuestionsWithOptionsUseCase(get()) }
    factory { ListMyExamAttemptsUseCase(get()) }
    factory { SubmitIntegrityEventsUseCase(get()) }
    factory { GetAttemptAnswerSheetUseCase(get(), get()) }
    factory { GetExamHistoryUseCase(get()) }
    factory { GetAttemptAnalysisUseCase(get(), get()) }
    factory { GetExamLeaderboardUseCase(get()) }
    factory { SubmitMcqReportUseCase(get()) }
    factory { FetchMcqUseCase(get()) }
    factory { SyncBookmarksIfNeededUseCase(get()) }
    factory { ObserveBookmarkedIdsUseCase(get()) }
    factory { ObserveBookmarksUseCase(get()) }
    factory { AddBookmarkUseCase(get()) }
    factory { RemoveBookmarkUseCase(get()) }
    factory { ClearBookmarksUseCase(get()) }
    factory { ObserveReportedIdsUseCase(get()) }
    factory { ClearReportsUseCase(get()) }

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

    // Study groups (real API)
    factory { CreateGroupUseCase(get()) }
    factory { FetchMyGroupsUseCase(get()) }
    factory { ExploreGroupsUseCase(get()) }
    factory { JoinGroupByCodeUseCase(get()) }
    factory { FetchGroupUseCase(get()) }
    factory { UpdateGroupUseCase(get()) }
    factory { DeleteGroupUseCase(get()) }
    factory { RegenerateGroupCodeUseCase(get()) }
    factory { FetchGroupMembersUseCase(get()) }
    factory { AddGroupMemberUseCase(get()) }
    factory { SetGroupMemberRoleUseCase(get()) }
    factory { RemoveGroupMemberUseCase(get()) }
    factory { LeaveGroupUseCase(get()) }
    factory { RequestToJoinGroupUseCase(get()) }
    factory { FetchJoinRequestsUseCase(get()) }
    factory { ApproveJoinRequestUseCase(get()) }
    factory { RejectJoinRequestUseCase(get()) }
    // Exam routines and scheduled exams
    factory { PreviewRoutineUseCase(get()) }
    factory { CreateRoutineUseCase(get()) }
    factory { FetchActiveRoutineUseCase(get()) }
    factory { DeleteRoutineUseCase(get()) }
    factory { FetchSlotsUseCase(get()) }
    factory { DescribeTopicGroupingUseCase(get()) }
    factory { OpenScheduledExamUseCase(get(), get()) }
    factory { FetchPastSlotsUseCase(get()) }
    // DUMMY until the API has exam stats (GROUP_ROUTINE_PLAN.md §14 B1): swap this binding only.
    single<GroupExamStatsProvider> { DummyGroupExamStatsProvider() }
    factory { CreateAdhocExamUseCase(get()) }
    factory { StartSlotUseCase(get()) }
    factory { RescheduleSlotUseCase(get()) }
    factory { DeleteAdhocExamUseCase(get()) }
    factory { StartScheduledExamUseCase(get(), get()) }
    factory { FindMyAttemptForExamUseCase(get()) }
    // Notifications
    factory { FetchNotificationsUseCase(get()) }
    factory { MarkNotificationReadUseCase(get()) }
    factory { ClearPushTokenUseCase(get()) }
    factory { FetchMyAnswerSheetUseCase(get()) }
    factory { AdvanceDemoStateUseCase(get()) }

    viewModel { SharedViewModel(get(), get(), get(), get()) }
    viewModel { OnboardingViewModel(get()) }
    viewModel { AuthStartViewModel(get()) }
    viewModel { params -> AuthLoginViewModel(get(), params.get(), params.getOrNull()) }
    viewModel { params -> AuthSignupViewModel(get(), params.get()) }
    viewModel { params -> AuthVerifyOtpViewModel(get(), get(), get(), params.get()) }
    viewModel { params -> AuthResetRequestViewModel(get(), params.get()) }
    viewModel { params -> AuthResetNewPasswordViewModel(get(), identifier = params[0], code = params[1]) }
    viewModel { HomeViewModel(get(), get()) }
    viewModel { HomeGroupExamViewModel(get(), get()) }
    viewModel { HomeNotificationsViewModel(get()) }
    viewModel { HomeWidgetPromoViewModel(get(), get(), get()) }
    viewModel { FeatureAccessViewModel(get()) }
    viewModel { NotificationsViewModel(get(), get(), get()) }
    viewModel { RoutineTabViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { BookmarkHistoryViewModel(get(), get(), get()) }
    viewModel { ProfileViewModel(get()) }

    viewModel { SubjectListViewModel(get()) }
    viewModel { params -> MCQExamListViewModel(params.get(), get(), get()) }
    // Both route values are Strings, so read them by position: attemptId, then the group window's end (may be null).
    viewModel { params ->
        ExamAttemptViewModel(params.values[0] as String, get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), params.values.getOrNull(1) as String?)
    }
    viewModel { params -> ExamResultViewModel(params.get(), get(), get()) }
    viewModel { params -> LockedExamResultViewModel(params.get(), get()) }
    // By position: both parameters are Strings, and the topic filter may be null.
    viewModel { params -> ExamAnswerSheetViewModel(params[0], params.values.getOrNull(1) as String?, get()) }
    viewModel { ExamHistoryViewModel(get(), get(), get()) }
    viewModel { params -> AttemptAnalysisViewModel(params.get(), get()) }
    viewModel { params -> ExamLeaderboardViewModel(params.get(), get()) }

    // ── Exam tools: calculator + scratch canvas (own ViewModels, independent of the exam VM) ──
    factory { ExpressionEvaluator() }
    factory { EquationSolver() }
    factory { StrokeEraser() }
    viewModel { CalculatorViewModel(get(), get()) }

    // ── Playground · graph builder ── plotting runs on its own CPU dispatcher (replaceable in tests).
    single<CoroutineDispatcher>(named(GRAPH_COMPUTE_DISPATCHER)) { Dispatchers.Default }
    factory { GraphCompiler() }
    factory { GraphPlotter() }
    factory { SpacePlotter() }
    single<SavedGraphRepository> { SavedGraphRepositoryImpl(get(), get()) }
    factory { ObserveSavedGraphsUseCase(get()) }
    factory { GetSavedGraphUseCase(get()) }
    factory { SaveGraphUseCase(get()) }
    factory { RenameSavedGraphUseCase(get()) }
    factory { DeleteSavedGraphUseCase(get()) }
    viewModel { params ->
        GraphBuilderViewModel(
            get(), get(), get(named(GRAPH_COMPUTE_DISPATCHER)), get(), get(), get(), get(),
            savedGraphId = params.getOrNull<GraphBuilder>()?.savedGraphId,
        )
    }
    viewModel { PlaygroundHubViewModel(get(), get(), get()) }
    // ── Playground · code visualizer ── parsing runs on the same CPU dispatcher as plotting.
    factory { BuildCodeFlowUseCase() }
    // Python runs in the `:py` process; its runtime is an on-demand Play module.
    single { PythonRuntimeRepositoryImpl(androidContext()) } binds arrayOf(PythonRuntimeRepository::class, PythonInstallConfirmation::class)
    single<CodeRunRepository> { CodeRunRepositoryImpl(androidContext()) }
    factory { ObservePythonRuntimeUseCase(get()) }
    factory { InstallPythonRuntimeUseCase(get()) }
    factory { RunPythonUseCase(get()) }
    factory { SendRunInputUseCase(get()) }
    factory { SendTraceCommandUseCase(get()) }
    factory { RunCUseCase(get()) }
    viewModel { CodeVisualizerViewModel(get(), get(named(GRAPH_COMPUTE_DISPATCHER)), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { ScratchpadViewModel(get()) }
    viewModel { PracticeSubjectSelectViewModel(get()) }
    viewModel { PracticeSubjectChapterViewModel(get(), get(), get()) }
    viewModel { params -> PracticeChapterSelectViewModel(params.get(), get(), get(), get()) }
    viewModel { PracticeExamPropertiesViewModel(get()) }
    viewModel { PracticeFlowViewModel() }
    viewModel { params -> QuestionReaderViewModel(params.get(), get(), get(), get(), get(), get(), get()) }
    viewModel { params -> ChapterBrowseViewModel(params.get(), get(), get(), get()) }

    // ── Group exam ──
    viewModel { GroupListViewModel(get(), get(), get(), get(), get()) }
    viewModel { params -> GroupDetailViewModel(params.get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { params -> GroupRankingViewModel(params.values[0] as String, params.values[1] as String, params.values.getOrNull(2) as String?, get(), get(), get(), get(), get(), get()) }
    viewModel { params ->
        RoutineBuilderViewModel(params.get(), params.get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get())
    }
    viewModel { params -> GroupSettingsViewModel(params.get(), get(), get(), get(), get(), get(), get()) }
    viewModel { params -> GroupJoinRequestsViewModel(params.get(), get(), get(), get(), get()) }
    viewModel { GroupExploreViewModel(get(), get(), get()) }
    viewModel { params -> GroupRoutineCalendarViewModel(params.get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { params -> GroupPastExamsViewModel(params.get(), get(), get(), get(), get(), get()) }
    viewModel { params -> GroupMembersViewModel(params.get(), get(), get(), get(), get(), get(), get(), get()) }

    // ── Live exam ──
    viewModel { LiveExamListViewModel(get(), get(), get()) }
    viewModel { params -> LiveExamDetailViewModel(params.get(), get(), get(), get(), get()) }

    // ── Shared timed-exam attempt/result/ranking/answer-sheet (live + group) ──
    viewModel { params -> TimedExamAttemptViewModel(params.get(), params.get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { params -> LockedResultViewModel(params.get(), params.get(), get(), get(), get()) }
    viewModel { params -> WindowRankingViewModel(params.get(), params.get(), get()) }
    viewModel { params -> TimedAnswerSheetViewModel(params.get(), get()) }
}
