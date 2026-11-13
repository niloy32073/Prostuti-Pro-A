---
name: "android-master"
description: "MANDATORY for ANY Android/Kotlin/Jetpack Compose work in any project (new app, feature, screen, ViewModel, repository, API call, Room, DataStore, Koin, Navigation 3, adaptive layout, bug fix, refactor, review). Defines the one architecture, stack, folder layout, DI, navigation and adaptive-UI rules every Android project must follow."
---

# Android Master Blueprint — the ONE way we build Android apps

This is the single source of truth for every Android project. It merges:
the Prostuti architecture (Kotlin · Compose · Navigation 3 · Koin · Ktor · Room · DataStore · MVVM),
Google's official `navigation-3` + `adaptive` skills with the developer.android.com docs/recipes they link,
and the official Koin skills (Koin 4.x + Koin Compiler Plugin). Docs state as of Oct 2026.

## How to use this skill (read first — non-negotiable)

1. Load and follow this skill for **every** Android task, even a one-line fix. Do not mix in patterns from memory,
   Stack Overflow, older Android docs, or other architectures (no Hilt, no Navigation 2, no XML layouts, no LiveData, no Retrofit).
2. Follow it **literally**. A rule written as MUST / NEVER is not a suggestion. If the user asks for something that breaks a rule,
   say which rule, explain the cost in one or two lines, propose the compliant way, and only do the non-compliant thing after an explicit OK
   — then flag it in your summary.
3. Existing project that already deviates (older Koin style, nested NavDisplays, etc.): do the task in the project's current style,
   do not rewrite unrelated code, and propose the compliant change **once** as a separate step.
4. If the project contradicts an assumption here (not Compose, not Nav3, multi-Gradle-module, KMP…), STOP and ask.
5. Before finishing ANY task, run the Final checklist (§22) and report what you verified and what you could not.
6. Testing details live in the companion skill `prostuti-android-e2e-tests`; use it whenever writing tests.

**Project assumptions:** every project is already on Navigation 3 or is new and built on Navigation 3 — no Navigation 2 migration work.
DI is **Koin** — never Hilt/Dagger. Single Gradle `:app` module unless the user says otherwise.

---

## A1. Stack (fixed — do not substitute)

| Layer | Technology | Notes |
|---|---|---|
| Language | Kotlin (K2, ≥ 2.3.20) | |
| UI | Jetpack Compose, Material 3 | No XML layouts, no Fragments for new screens |
| Navigation | Navigation 3 (`androidx.navigation3`) | §1–§13 |
| Adaptive | `material3-adaptive`, `adaptive-navigation3`, `material3-adaptive-navigation-suite` | §4, §6, §15 |
| DI | Koin 4.x (BOM) + Koin Compiler Plugin | §14 |
| Network | Ktor 3 (BOM) client + kotlinx.serialization | via `safeApiCall` (A5) |
| Local DB | Room (with **KSP** — KSP is correct for Room; it is only banned for Koin) | |
| Preferences | DataStore Preferences | |
| Images | Coil 3 | |
| Math/LaTeX (if needed) | `io.github.huarangmeng:latex-*` | |
| Logging | Timber (planted only in debug) | Never `println`/`Log.d` in committed code |
| Async | Coroutines + Flow / StateFlow | No LiveData, no RxJava |
| Splash | `androidx.core:core-splashscreen` | |

Build: `minSdk = 28` (default; Nav3 needs ≥ 23), `compileSdk`/`targetSdk = 36`, `buildConfig = true` with
`buildConfigField("String", "BASE_URL", ...)`, release `isMinifyEnabled = true`, `isShrinkResources = true`.
Version catalog (`libs.versions.toml`) for every dependency; BOMs for Compose, Koin, Ktor.

---

## A2. Package structure (create exactly this; `<app>` = app package)

```
com.<company>.<app>
├── App.kt                         # Application: Timber + startKoin
├── MainActivity.kt                # splash, edge-to-edge, theme, root navigation
│
├── data/
│   ├── model/
│   │   ├── enums/                 # enum classes
│   │   ├── request/               # @Serializable request DTOs
│   │   └── response/              # @Serializable response DTOs (+ Room @Entity when cached); ApiResponse, BaseResponse
│   ├── repository/
│   │   ├── XxxRepository.kt       # interface
│   │   └── XxxRepositoryImpl.kt   # implementation
│   └── service/
│       ├── local/                 # AppDatabase.kt, XxxDao.kt
│       └── remote/                # XxxApiService.kt (Ktor wrappers)
│
├── domain/                        # XxxUseCase.kt — one operation per class
│
├── dataStore/                     # PrefKeys.kt, createDataStore.kt
│
├── navigation/
│   ├── Routes.kt                  # ALL @Serializable NavKey routes (sealed per level)
│   ├── NavigationState.kt         # multi back stack state (§4)
│   ├── Navigator.kt               # Navigator + LocalNavigator (§4)
│   ├── RootNavDisplay.kt          # Intro / Auth / Main switch (A10)
│   └── MainNavDisplay.kt          # NavigationSuiteScaffold + tab NavDisplay (§4)
│
├── ui/
│   ├── component/                 # reusable composables (incl. PullToRefreshErrorOverlayWrapper, AdaptiveColumns.kt §15.3)
│   ├── screen/
│   │   ├── SharedViewModel.kt     # app-wide session state (A9)
│   │   └── <feature>/             # XxxScreen.kt + XxxViewModel.kt (+ XxxUiState)
│   └── theme/                     # Color.kt, Theme.kt, Type.kt
│
├── di/
│   ├── appModule.kt               # ONE root module: includes(...) all modules below
│   ├── networkModule.kt           # HttpClient
│   ├── databaseModule.kt          # Room + DAOs
│   ├── dataStoreModule.kt         # DataStore
│   └── <feature>Module.kt         # api service + repo + use cases + ViewModels + navigation<> entries
│
└── util/                          # safeApiCall.kt, extensions
```
Rules: one public top-level class per file, file name = class name. Feature screens grouped by feature folder.
`startKoin` loads only `appModule`; every other module is reached through `appModule.includes(...)`.

---

## A3. Layer rules (dependency direction: ui → domain → data)

| Layer | May depend on | Must NOT |
|---|---|---|
| `ui` (Screen, ViewModel) | `domain` use cases, models | Call ApiService/DAO/DataStore directly; hold Context/Activity in VM; import Ktor/Room |
| `domain` (UseCase) | Repository **interfaces**, models | Know Compose, Koin, Android UI |
| `data` (Repository impl, services) | Ktor, Room, DataStore | Expose `HttpResponse`, exceptions or Room cursors upward — only `ApiResponse<T>` / `Flow<ApiResponse<T>>` / models |

- No Koin imports in `data` or `domain` classes — constructor injection only.
- Every network call returns `ApiResponse<T>`; nothing throws out of the data layer.

---

## A4. App.kt and MainActivity.kt

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.INFO else Level.NONE)
            androidContext(this@App)
            modules(appModule)                       // single entry module (§14.4)
        }
    }
}
```
```kotlin
class MainActivity : ComponentActivity() {
    private val sharedViewModel: SharedViewModel by viewModel()   // Koin Android API — NOT getKoin().get()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        splash.setKeepOnScreenCondition { !sharedViewModel.state.value.isChecked }
        setContent {
            val session by sharedViewModel.state.collectAsStateWithLifecycle()
            AppTheme {
                if (session.isChecked) RootNavDisplay(session = session)   // A10
            }
        }
    }
}
```
- No `calculateWindowSizeClass(this)` passed down the tree: screens read `currentWindowAdaptiveInfoV2()` themselves (§15.1).
- Insets: each screen's `Scaffold` handles its own insets; `NavigationSuiteScaffold` handles nav bar/rail insets.

---

## A5. Network: ApiResponse, BaseResponse, safeApiCall, ApiService

```kotlin
// data/model/response/ApiResponse.kt
@Serializable
sealed class ApiResponse<out T> {
    @Serializable data class Success<T>(val data: T, val message: String = "", val metaData: MetaData? = null) : ApiResponse<T>()
    @Serializable data class Failure(val message: String, val errorCode: String, val details: String? = null) : ApiResponse<Nothing>()
}

@Serializable
data class BaseResponse<T>(
    val success: Boolean = false,
    val message: String? = null,
    val data: T? = null,
    val error: ErrorDetail? = null,
    @SerialName("meta_data") val metaData: MetaData? = null,
)
```
```kotlin
// util/safeApiCall.kt — EVERY Ktor call goes through this
suspend inline fun <reified T> safeApiCall(crossinline execute: suspend () -> HttpResponse): ApiResponse<T> =
    try {
        val response = execute()
        if (response.status.isSuccess()) {
            val base = response.body<BaseResponse<T>>()
            if (base.success && base.data != null)
                ApiResponse.Success(base.data, base.message.orEmpty(), base.metaData)
            else
                ApiResponse.Failure(base.message ?: "Operation failed", base.error?.code ?: response.status.value.toString(), base.error?.details)
        } else {
            val err = runCatching { response.body<BaseResponse<Unit>>() }.getOrNull()
            ApiResponse.Failure(
                err?.message ?: "Unexpected server response (${response.status.value})",
                err?.error?.code ?: response.status.value.toString(),
                err?.error?.details,
            )
        }
    } catch (e: CancellationException) {
        throw e                                          // never swallow coroutine cancellation
    } catch (e: Exception) {
        val (msg, code) = when (e) {
            is SocketTimeoutException, is HttpRequestTimeoutException -> "Timeout: server is too slow" to "TIMEOUT_ERROR"
            is UnresolvedAddressException, is java.net.UnknownHostException -> "No internet connection" to "NETWORK_ERROR"
            is SerializationException -> "Mapping error: check data classes" to "SERIALIZATION_ERROR"
            else -> (e.message ?: "Unknown error") to "UNKNOWN_ERROR"
        }
        Timber.e(e, "safeApiCall failed: %s", code)
        ApiResponse.Failure(msg, code, e.localizedMessage)
    }
```
```kotlin
// data/service/remote/XxxApiService.kt
class XxxApiService(private val client: HttpClient) {
    suspend fun create(body: XxxRequest): ApiResponse<XxxResponse> = safeApiCall {
        client.post("xxx/endpoint") { contentType(ContentType.Application.Json); setBody(body) }
    }
    suspend fun list(): ApiResponse<List<XxxItem>> = safeApiCall { client.get("xxx/list") }
}
```
HttpClient (built once, in `networkModule`, §14.7): `defaultRequest { url(BuildConfig.BASE_URL + "/api/v1/") }`,
`ContentNegotiation { json(Json { ignoreUnknownKeys = true; explicitNulls = false }) }`, `HttpTimeout`,
`Auth { bearer { loadTokens { ... from DataStore ... }; refreshTokens { ... } } }`, `Logging` only in debug (via Timber).
Never log tokens or personal data.

---

## A6. Repository + Use case

```kotlin
interface XxxRepository {
    suspend fun create(body: XxxRequest): ApiResponse<XxxResponse>
    fun observeItems(refresh: Boolean): Flow<ApiResponse<List<XxxItem>>>
}

// cache-first for Room-backed data: Room is the single source of truth for the UI
class XxxRepositoryImpl(
    private val api: XxxApiService,
    private val dao: XxxDao,
) : XxxRepository {
    override suspend fun create(body: XxxRequest) = api.create(body)

    override fun observeItems(refresh: Boolean): Flow<ApiResponse<List<XxxItem>>> = flow {
        if (refresh || dao.getAll().isEmpty()) {
            when (val r = api.list()) {
                is ApiResponse.Success -> dao.replaceAll(r.data)          // @Transaction delete + insert
                is ApiResponse.Failure -> { emit(r); if (dao.getAll().isEmpty()) return@flow }  // show error, keep cached data if any
            }
        }
        emitAll(dao.observeAll().map { ApiResponse.Success(it) })
    }
}
```
```kotlin
// domain — one class = one operation, no interfaces, operator invoke
class CreateXxxUseCase(private val repo: XxxRepository) {
    suspend operator fun invoke(body: XxxRequest): ApiResponse<XxxResponse> = repo.create(body)
}
class ObserveXxxItemsUseCase(private val repo: XxxRepository) {
    operator fun invoke(refresh: Boolean = false): Flow<ApiResponse<List<XxxItem>>> = repo.observeItems(refresh)
}
```
- Ktor and Room suspend APIs are main-safe — no `withContext(Dispatchers.IO)` needed around them. For real CPU/blocking work,
  inject a dispatcher (`class Foo(private val io: CoroutineDispatcher)` with `single<CoroutineDispatcher>(named("io")) { Dispatchers.IO }`)
  so tests can replace it. Never hard-code `Dispatchers.IO` inside ViewModels.
- Use cases are `factory`; repositories and services `single` (§14.2).

---

## A7. ViewModel + UiState (the only allowed shape)

```kotlin
data class XxxUiState(
    val isLoading: Boolean = false,
    val items: List<XxxItem> = emptyList(),
    val errors: Map<String, String> = emptyMap(),   // tag -> user message (shown by PullToRefreshErrorOverlayWrapper)
    val isSuccess: Boolean = false,                 // one-shot outcome; consume it (see below)
)

class XxxViewModel(
    private val createXxx: CreateXxxUseCase,
    private val observeItems: ObserveXxxItemsUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(XxxUiState())
    val state: StateFlow<XxxUiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    init { load() }
    fun refresh() = load(refresh = true)

    private fun load(refresh: Boolean = false) {
        loadJob?.cancel()                                   // never stack collectors
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            observeItems(refresh).collect { r ->
                when (r) {
                    is ApiResponse.Success -> _state.update { it.copy(isLoading = false, items = r.data) }
                    is ApiResponse.Failure -> _state.update { it.copy(isLoading = false, errors = it.errors + (TAG_LOAD to r.message)) }
                }
            }
        }
    }

    fun create(body: XxxRequest) = viewModelScope.launch {
        _state.update { it.copy(isLoading = true) }
        when (val r = createXxx(body)) {
            is ApiResponse.Success -> _state.update { it.copy(isLoading = false, isSuccess = true) }
            is ApiResponse.Failure -> _state.update { it.copy(isLoading = false, errors = it.errors + (TAG_CREATE to r.message)) }
        }
    }

    fun clearError(tag: String) = _state.update { it.copy(errors = it.errors - tag) }
    fun onSuccessConsumed() = _state.update { it.copy(isSuccess = false) }

    private companion object { const val TAG_LOAD = "LOAD"; const val TAG_CREATE = "CREATE" }
}
```
Rules:
- One `MutableStateFlow` private, one public `StateFlow`, updated only with `_state.update { }`. Immutable UiState (`List`, not `MutableList`).
- Error tags are `const val` in the ViewModel — never inline string literals scattered around.
- Parameterized ViewModel (route args): `@InjectedParam` + `parametersOf(route)` (§7). Never read args from a global.
- No Android `Context`, `Activity`, `NavBackStack` or `Navigator` in a ViewModel. Navigation happens in the UI from state/lambdas.
- Long-lived observable work: either `init`-launched collect with a cancellable `Job` (as above) or `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)`.

---

## A8. Screen (Compose) contract

```kotlin
// Stateful entry — gets the VM, collects state, handles one-shot outcomes
@Composable
fun XxxScreen(
    viewModel: XxxViewModel,                // passed from the navigation entry (koinViewModel()) — §14.3
    onDone: () -> Unit,                     // navigation as lambdas, never NavBackStack/Navigator
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) { viewModel.onSuccessConsumed(); onDone() }   // consume → no re-navigation on return
    }

    XxxContent(
        state = state,
        onRefresh = viewModel::refresh,
        onClearError = viewModel::clearError,
        onCreate = viewModel::create,
    )
}

// Stateless content — previewable, testable; receives only state + lambdas
@Composable
fun XxxContent(state: XxxUiState, onRefresh: () -> Unit, onClearError: (String) -> Unit, onCreate: (XxxRequest) -> Unit) {
    PullToRefreshErrorOverlayWrapper(
        errors = state.errors,
        errorCleaner = onClearError,
        pullToRefreshEnabled = true,
        isRefreshing = state.isLoading,
        onRefresh = onRefresh,
    ) {
        LazyVerticalGrid(columns = GridCells.Fixed(adaptiveColumnCount()), state = rememberLazyGridState()) {   // size-class columns (§15.3)
            items(state.items, key = { it.id }) { item -> XxxItemCard(item) }
        }
    }
}
```
Rules:
- Every screen = stateful `XxxScreen` (VM + effects) + stateless `XxxContent` (state + lambdas). Previews target `XxxContent` with `@FormFactorPreviews` (§20).
- `collectAsStateWithLifecycle()` — never `collectAsState()` for ViewModel state.
- Every data screen wraps content in `PullToRefreshErrorOverlayWrapper`; errors flow only through `errors: Map<String,String>`.
- Forms/auth screens on wide windows: center and cap width — `Modifier.widthIn(max = 480.dp).fillMaxWidth()` — instead of
  `widthFraction` by size class (fractions break on very wide/very narrow windows).
- Strings in `strings.xml` (`stringResource`), Bangla + English if the app is bilingual; no hard-coded user-visible text.
- Every clickable icon has a `contentDescription`; test tags per the e2e skill.

---

## A9. Local data: Room, DataStore, SharedViewModel

```kotlin
// Dual-annotated entity: same class for API response and Room row (when shapes match)
@Serializable
@Entity(tableName = "xxx_item", indices = [Index("parent_id")])
data class XxxItem(
    @PrimaryKey @ColumnInfo(name = "id") @SerialName("id") val id: String,
    @ColumnInfo(name = "parent_id") @SerialName("parent_id") val parentId: String? = null,
    @ColumnInfo(name = "name_en") @SerialName("name_en") val nameEn: String,
    @ColumnInfo(name = "name_bn") @SerialName("name_bn") val nameBn: String,
)

@Dao
interface XxxDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(items: List<XxxItem>)
    @Query("SELECT * FROM xxx_item") suspend fun getAll(): List<XxxItem>
    @Query("SELECT * FROM xxx_item") fun observeAll(): Flow<List<XxxItem>>
    @Query("SELECT * FROM xxx_item WHERE id = :id") suspend fun getById(id: String): XxxItem?
    @Query("SELECT * FROM xxx_item WHERE parent_id = :parentId") suspend fun getByParentId(parentId: String): List<XxxItem>
    @Query("DELETE FROM xxx_item") suspend fun deleteAll()
    @Transaction suspend fun replaceAll(items: List<XxxItem>) { deleteAll(); insertAll(items) }
}

@Database(entities = [XxxItem::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() { abstract fun xxxDao(): XxxDao }
```
- Room via KSP. `exportSchema = true` + `room { schemaDirectory("$projectDir/schemas") }`. Bump `version` with every schema change.
- `fallbackToDestructiveMigration(true)` ONLY for pure cache tables (data re-downloadable). Any user-generated data → write a `Migration`.
- DataStore: one instance (`single`), keys in `PrefKeys`:
```kotlin
object PrefKeys {
    val JWT_TOKEN = stringPreferencesKey("jwt_token")
    val JWT_REFRESH_TOKEN = stringPreferencesKey("jwt_refresh_token")
    val IS_FIRST_TIME = booleanPreferencesKey("is_first_time")
}
```
  (Typed `Preferences.Key<T>` — if an existing project stores plain `String` names, keep its style.)
  Access only through a `StorageRepository` + small use cases (`GetStringFromDataStoreUseCase`, `SaveStringToDataStoreUseCase`, …).
- `SharedViewModel` = app-wide session state derived from DataStore flows:
```kotlin
data class SharedState(val isChecked: Boolean = false, val isLogIn: Boolean = false, val isFirstTime: Boolean = true)

class SharedViewModel(getString: GetStringFromDataStoreUseCase, getBoolean: GetBooleanFromDataStoreUseCase) : ViewModel() {
    val state: StateFlow<SharedState> = combine(
        getString(PrefKeys.JWT_TOKEN), getBoolean(PrefKeys.IS_FIRST_TIME)
    ) { token, firstTime -> SharedState(isChecked = true, isLogIn = !token.isNullOrBlank(), isFirstTime = firstTime ?: true) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SharedState())
}
```
  It drives the splash screen and the root route. Feature screens do not read it for their own data.

---

## A10. Root navigation (Intro / Auth / Main) + routes

Routes — all in `navigation/Routes.kt`, sealed per level:
```kotlin
@Serializable sealed interface RootRoute : NavKey
@Serializable data object Intro : RootRoute
@Serializable data object Auth : RootRoute
@Serializable data class SignIn(val identifier: String, val firstName: String? = null) : RootRoute
@Serializable data object Main : RootRoute

@Serializable sealed interface MainRoute : NavKey               // top-level tabs + everything reachable inside them
@Serializable data object Home : MainRoute
@Serializable data object QB : MainRoute
@Serializable data object Profile : MainRoute
@Serializable data class McqQb(val subjectId: String, val nameBn: String, val nameEn: String) : MainRoute
```
Exactly **two** NavDisplay levels:
1. `RootNavDisplay` — back stack of `RootRoute` (Intro → Auth → SignIn → Main). Start chosen from `SharedViewModel`
   (`isFirstTime → Intro`, `!isLogIn → Auth`, else `Main`). On login/logout **replace** the root stack
   (`backStack.clear(); backStack.add(Main)`) — never push Main on top of Auth (Back would return to login).
   Re-key the root back stack when the session flips: `key(session.isLogIn) { val backStack = rememberNavBackStack(start) ... }`.
2. `MainNavDisplay` (the `Main` entry) — `NavigationSuiteScaffold` + `NavigationState` with one back stack per tab (§4).
   Every feature screen (e.g. `McqQb`) is a route pushed onto the current tab's stack.

NEVER create a third, per-feature nested NavDisplay (the old `QBNavDisplay` pattern): nested displays break list-detail scenes,
deep links, predictive back and per-tab state. Existing projects that have one: keep it working, propose flattening once.

---

## 0. Golden rules — navigation, DI, adaptive (obey always)

**MUST**
1. Use Jetpack **Navigation 3** (`androidx.navigation3`). Compose-only destinations.
2. Every route is a `@Serializable` class/object that implements `NavKey`.
3. The app owns the back stack. Create it with `rememberNavBackStack(...)` (or `NavigationState` for tabs, §4). Never hold it in a plain `remember { mutableStateListOf() }` in production — that loses state on process death.
4. Navigate by **mutating the back stack** (`add`, `removeLastOrNull`, `removeIf`) — through a `Navigator` class when the app has tabs or rules.
5. Wrap click-to-navigate lambdas in `dropUnlessResumed { ... }` to prevent double navigation.
6. Pass arguments **inside the key** (`data class Detail(val id: String) : NavKey`); read them from the `key` parameter of `entry<Detail> { key -> }`.
7. Multi-pane layouts MUST be built with Navigation 3 **`SceneStrategy`** (`ListDetailSceneStrategy`, `SupportingPaneSceneStrategy`, or a custom one). **NEVER** use `ListDetailPaneScaffold` / `SupportingPaneScaffold`.
8. The top-level navigation container MUST be `NavigationSuiteScaffold` (auto bar ↔ rail). Never a hard-coded bottom `NavigationBar` only.
9. All user/interactive state MUST survive configuration changes: `rememberSaveable`, `rememberLazyListState()`, etc. (§16).
10. When you use `entryDecorators`, ALWAYS include `rememberSaveableStateHolderNavEntryDecorator()` **first**, then add others.
11. DI is **Koin 4.x** with the **Koin Compiler Plugin** (compile-time graph check). Constructor injection everywhere; Koin APIs only at entry points (`koinViewModel()`, `koinInject()`, `by inject()` in Android components). Details: §14.
12. Screens get their ViewModel via `koinViewModel()` **inside the Nav3 entry** (or as a default parameter of the screen), with `rememberViewModelStoreNavEntryDecorator()` installed so each entry owns its ViewModel.
13. Architecture: ui → domain → data (A3). Every network call via `safeApiCall` → `ApiResponse<T>`. One UiState data class + one `StateFlow` per ViewModel; errors as `Map<String,String>` shown by `PullToRefreshErrorOverlayWrapper`.
14. Every screen = stateful `XxxScreen` + stateless `XxxContent`; state collected with `collectAsStateWithLifecycle()`.
15. Decide compact / medium / expanded (/ large / extra-large) **only through the window size class**:
    `currentWindowAdaptiveInfoV2().windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_…_LOWER_BOUND)`
    (and `isHeightAtLeastBreakpoint(HEIGHT_DP_…)` when height matters). List column counts come from `adaptiveColumnCount()` (§15.3).

**NEVER**
- NEVER pick a layout from a hand-written dp breakpoint (`maxWidth < 800.dp`, `constraints.maxWidth.toDp() >= 840.dp`,
  `windowWidth < 600.dp`, `BoxWithConstraints { if (maxWidth > …) }`). dp values are only for **sizes** (padding, gaps,
  `widthIn(max = …)` caps, item min widths, touch targets), never for choosing a layout (§15.1).
- NEVER use Retrofit, LiveData, RxJava, XML layouts, Fragments for new screens, `GlobalScope`, `runBlocking` in app code, or `!!` on data from network/DB.
- NEVER let a repository/data source throw to the UI, or swallow `CancellationException`.
- NEVER use Hilt, Dagger, `@HiltViewModel`, `hiltViewModel()`, `@AndroidEntryPoint`, `kapt`, or Koin KSP (`koin-ksp-compiler`, `com.google.devtools.ksp` for Koin, `org.koin.ksp.generated.*`, `koin-annotations:2.x`).
- NEVER declare a ViewModel as `single`/`factory` — always `viewModel<T>()` / `@KoinViewModel`.
- NEVER make a class `KoinComponent` in domain/data/presentation layers (service locator) unless no Koin entry-point API fits AND the user explicitly OKs it.
- NEVER pass dependencies through `parametersOf(...)` — only runtime values (route key, id).
- NEVER introduce Navigation 2 APIs (`NavController`, `NavHost`, `composable<>`, `rememberNavController`, `koin-compose-viewmodel-navigation`, `sharedKoinViewModel`).
- NEVER use string routes.
- NEVER add `android:configChanges` or `android:screenOrientation` to the manifest to "fix" state loss or lock orientation.
- NEVER pass `NavBackStack`/`Navigator` deep into leaf composables; pass lambdas (`onItemClick: (Id) -> Unit`).
- NEVER replace `rememberSerializable` with `rememberSaveable` in `NavigationState`.
- NEVER look for a "back stack entry lifecycle"; use `LocalLifecycleOwner.current` inside the destination.
- NEVER rely on `ResultEventBus` results surviving process death.
- NEVER show a back arrow on a detail pane while it is displayed beside the list.
- NEVER put a `LazyColumn` in a plain `1.fr` Grid track (crash) — use `GridTrackSize.MinMax(0.dp, 1.fr)`.
- NEVER use `FlowRow`/`FlowColumn` for wrapping items — use `FlexBox`.
- NEVER use experimental APIs (Grid, FlexBox, mediaQuery) without telling the user they are experimental and getting a yes.

---

## 1. Navigation theory (principles) → how Nav3 implements them

| Principle | What it means | Nav3 implementation |
|---|---|---|
| Fixed start destination | The first screen from the launcher, and the last before exiting with Back. Login/onboarding are *conditional* screens, NOT the start destination. | `startRoute` in `rememberNavigationState` / first element of `rememberNavBackStack(Home)`. Gate login via `RootNavDisplay` (A10) / conditional navigation (§9). |
| Navigation state is a stack | Bottom = start destination, top = current screen. Ops only push/pop at the top. | `NavBackStack` list. `add()` = push, `removeLastOrNull()` = pop. |
| Up and Back are identical within your task | Up arrow and system Back go to the same place. | Toolbar Up calls the same `navigator.goBack()` as `NavDisplay(onBack=)`. |
| Up never exits the app | No Up arrow on the start destination; Back there exits. | Hide the Up icon when `backStack.size == 1` or current key is a top-level route. |
| Deep linking simulates manual navigation | A deep link replaces the task's back stack with a realistic **synthetic** back stack (start destination + parents + target). | `UriDeepLinkMatcher(...).withBackStack { listOf(Home, it.key) }` (§10). |
| Multiple back stacks (tabs) | Each tab keeps its own history; user exits through the start tab. | `NavigationState` + `Navigator` (§4). |

---

## 2. Setup

Prerequisites: `compileSdk` ≥ 36, `minSdk` 28 (project default; Nav3 minimum is 23), Kotlin ≥ 2.3.20 (K2), Kotlin serialization plugin,
all screens in Compose (Fragments/Views → use view interop or rewrite in Compose).
Koin Gradle setup is in §14.1.

```toml
# gradle/libs.versions.toml
[versions]
nav3Core = "1.2.0"
lifecycleViewmodelNav3 = "2.11.0"
compose = "1.13.0-beta02"          # Grid / FlexBox (foundation-layout), only if user accepts experimental
# material3-adaptive: use the latest stable/beta that ships adaptive-navigation3

[libraries]
androidx-navigation3-runtime = { module = "androidx.navigation3:navigation3-runtime", version.ref = "nav3Core" }
androidx-navigation3-ui = { module = "androidx.navigation3:navigation3-ui", version.ref = "nav3Core" }
androidx-lifecycle-viewmodel-navigation3 = { module = "androidx.lifecycle:lifecycle-viewmodel-navigation3", version.ref = "lifecycleViewmodelNav3" }
androidx-compose-foundation-layout = { group = "androidx.compose.foundation", name = "foundation-layout", version.ref = "compose" }
androidx-material3-adaptive = { module = "androidx.compose.material3.adaptive:adaptive" }
androidx-material3-adaptive-navigation3 = { module = "androidx.compose.material3.adaptive:adaptive-navigation3" }
androidx-material3-adaptive-navigation-suite = { module = "androidx.compose.material3:material3-adaptive-navigation-suite" }
kotlinx-serialization-core = { module = "org.jetbrains.kotlinx:kotlinx-serialization-core" }
```

```kotlin
// app/build.gradle.kts
plugins { alias(libs.plugins.kotlin.serialization) }
dependencies {
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)      // if screens use ViewModels
    implementation(libs.androidx.material3.adaptive)
    implementation(libs.androidx.material3.adaptive.navigation3)       // ListDetail / SupportingPane scenes
    implementation(libs.androidx.material3.adaptive.navigation.suite)  // NavigationSuiteScaffold
    implementation(libs.kotlinx.serialization.core)
}
```
Before adding versions, check the project's existing BOM/catalog and match it; do not downgrade.

---

## 3. Core building blocks

```kotlin
@Serializable data object Home : NavKey
@Serializable data class ProductDetail(val id: String) : NavKey

@Composable
fun AppNav() {
    val backStack = rememberNavBackStack(Home)       // survives config change + process death
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(), // ALWAYS first
            rememberViewModelStoreNavEntryDecorator(),       // per-entry ViewModels
        ),
        entryProvider = entryProvider {
            entry<Home> {
                HomeScreen(onProductClick = { id -> backStack.add(ProductDetail(id)) })
            }
            entry<ProductDetail> { key -> ProductDetailScreen(id = key.id) }
        }
    )
}

// Inside the screen, guard the click site (dropUnlessResumed takes a no-arg block):
// Card(onClick = dropUnlessResumed { onProductClick(product.id) }) { ... }
```

Typed back stack for a sealed key hierarchy:
```kotlin
@Serializable sealed interface AppKey : NavKey
@Composable
fun rememberAppBackStack(vararg elements: AppKey): NavBackStack<AppKey> =
    rememberSerializable(serializer = serializer()) { NavBackStack(*elements) }
```

> In real projects entries are NOT written inline like this — they are declared per feature in Koin modules with
> `navigation<Key>(metadata) { }` and collected with `koinEntryProvider<NavKey>()` (§14.3). Inline `entry<>` examples in
> §3/§6 show the Nav3 API; translate `entry<X>(metadata = M) { }` → `navigation<X>(metadata = M) { }` in a Koin module.

Key concepts:
- `NavEntry` = key + content + `metadata` (used by scenes/animations). `contentKey` uniquely identifies it.
- `entryProvider { entry<T>(metadata = ...) { key -> } }` maps keys → content.
- Decorators wrap each entry: saveable state, ViewModel store, result bus.
- Metadata is built with `metadata { put(SomeKey, value) }` where `object SomeKey : NavMetadataKey<T>`.

---

## 4. Standard app shell: tabs with multiple back stacks (COPY THIS)

Use for any app with ≥2 top-level destinations. Assumptions: each tab has its own stack, switching tabs keeps state, user exits via the start tab. If more than one level of nested navigation or shared destinations moving between stacks exist → ask the user.

```kotlin
// NavigationState.kt
@Composable
fun rememberNavigationState(startRoute: NavKey, topLevelRoutes: Set<NavKey>): NavigationState {
    val topLevelRoute = rememberSerializable(
        startRoute, topLevelRoutes,
        serializer = MutableStateSerializer(NavKeySerializer())
    ) { mutableStateOf(startRoute) }
    val backStacks = topLevelRoutes.associateWith { key -> rememberNavBackStack(key) }
    return remember(startRoute, topLevelRoutes) {
        NavigationState(startRoute, topLevelRoute, backStacks)
    }
}

class NavigationState(
    val startRoute: NavKey,
    topLevelRoute: MutableState<NavKey>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>,
) {
    var topLevelRoute: NavKey by topLevelRoute
    val stacksInUse: List<NavKey>
        get() = if (topLevelRoute == startRoute) listOf(startRoute) else listOf(startRoute, topLevelRoute)
}

@Composable
fun NavigationState.toEntries(entryProvider: (NavKey) -> NavEntry<NavKey>): SnapshotStateList<NavEntry<NavKey>> {
    val decorated = backStacks.mapValues { (_, stack) ->
        rememberDecoratedNavEntries(
            backStack = stack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
                rememberViewModelStoreNavEntryDecorator(),   // add if ViewModels are used
                // rememberResultEventBusNavEntryDecorator(), // add if results are used (§8)
            ),
            entryProvider = entryProvider,
        )
    }
    return stacksInUse.flatMap { decorated[it] ?: emptyList() }.toMutableStateList()
}
```
Imports: `androidx.compose.runtime.saveable.rememberSerializable`, `androidx.navigation3.runtime.{NavBackStack, NavEntry, NavKey, rememberDecoratedNavEntries, rememberNavBackStack, rememberSaveableStateHolderNavEntryDecorator}`, `androidx.navigation3.runtime.serialization.NavKeySerializer`, `androidx.savedstate.compose.serialization.serializers.MutableStateSerializer`.

```kotlin
// Navigator.kt
class Navigator(val state: NavigationState) {
    fun navigate(route: NavKey) {
        if (route in state.backStacks.keys) state.topLevelRoute = route          // tab switch
        else state.backStacks[state.topLevelRoute]?.add(route)                 // push in current tab
    }
    fun goBack() {
        val stack = state.backStacks[state.topLevelRoute] ?: error("Stack for ${state.topLevelRoute} not found")
        if (stack.last() == state.topLevelRoute) state.topLevelRoute = state.startRoute  // tab root → start tab
        else stack.removeLastOrNull()
    }
}
```

```kotlin
// navigation/MainNavDisplay.kt — adaptive shell (the `Main` root entry renders this)
// Routes come from navigation/Routes.kt (A10): Home, Search, Profile, Detail(id) : MainRoute
data class TopLevelItem(val key: NavKey, val label: String, val icon: ImageVector)
val TOP_LEVEL = listOf(
    TopLevelItem(Home, "Home", Icons.Default.Home),
    TopLevelItem(Search, "Search", Icons.Default.Search),
    TopLevelItem(Profile, "Profile", Icons.Default.Person),
)

@Composable
fun MainNavDisplay(pendingDeepLink: MutableStateFlow<List<NavKey>?>) {   // deep-link plumbing: §14.5
    val navigationState = rememberNavigationState(startRoute = Home, topLevelRoutes = TOP_LEVEL.map { it.key }.toSet())
    val navigator = remember { Navigator(navigationState) }
    var isNavBarVisible by rememberSaveable { mutableStateOf(true) }
    val scaffoldState = rememberNavigationSuiteScaffoldState()
    LaunchedEffect(isNavBarVisible) { if (isNavBarVisible) scaffoldState.show() else scaffoldState.hide() }

    // Entries are declared in Koin modules with navigation<Key> { } (§14.3) and aggregated here.
    val entryProvider = koinEntryProvider<NavKey>()

    CompositionLocalProvider(LocalNavigator provides navigator) {   // entries read the navigator from here
    NavigationSuiteScaffold(
        state = scaffoldState,
        navigationItems = {
            TOP_LEVEL.forEach { item ->
                NavigationSuiteItem(
                    selected = item.key == navigationState.topLevelRoute,   // selection rule
                    onClick = { navigator.navigate(item.key) },
                    icon = { Icon(item.icon, contentDescription = null) },
                    label = { Text(item.label) },
                )
            }
        },
    ) {
        NavDisplay(
            entries = navigationState.toEntries(entryProvider),
            onBack = { navigator.goBack() },
            sceneStrategies = remember { listOf(DialogSceneStrategy()) }, // + list-detail etc. (§6)
        )
    }
    }
}

// Navigator.kt (next to Navigator)
val LocalNavigator = staticCompositionLocalOf<Navigator> { error("Navigator not provided") }
```
Rules:
- Selected tab = `key == navigationState.topLevelRoute`. Never walk a hierarchy.
- Current screen = `navigationState.backStacks[topLevelRoute]!!.last()`.
- Navigation area visibility: keep a `isNavBarVisible`/`shouldShowNavBar` boolean, drive `show()/hide()` via `LaunchedEffect`. Hide for immersive content (camera, full-screen photo) or on scroll-down; show on scroll-up.
- If the nav bar was previously wrapped in `AnimatedVisibility/AnimatedContent`, port that logic to `NavigationSuiteScaffoldState`.
- A detail that is full-screen (bars hidden) on phones MUST NOT be full-screen on large screens / when shown in a multi-pane scene.
- Simple single-stack app (no tabs): just `rememberNavBackStack(Home)` + `NavDisplay(backStack=...)`.

---

## 5. Choosing the right navigation container / layout (decision table)

| Situation | Use |
|---|---|
| ≥2 top-level destinations | `NavigationSuiteScaffold` + `NavigationState`/`Navigator` |
| Item list → item detail (email, notes, chat, settings) | Material `ListDetailSceneStrategy` (§6.3) |
| Main content + supplementary content (video + related, doc + comments) | Material `SupportingPaneSceneStrategy` (§6.4) |
| Detail needs big media/full-screen | Do NOT use list-detail unless user asks; single pane |
| Modal choice / confirmation | `DialogSceneStrategy` (§6.1) |
| Modal sheet destination | `BottomSheetSceneStrategy` copied into project (§6.2) |
| Two arbitrary screens side by side | Custom two-pane `SceneStrategy` (§6.5) |
| Login / onboarding gate | `RootNavDisplay` (A10) or conditional navigation (§9), not start destination |

---

## 6. Scenes (multi-pane, dialogs, sheets)

How it works: `NavDisplay` passes current entries to each strategy in `sceneStrategies` **in order**; the first non-null `Scene` wins; fallback is `SinglePaneSceneStrategy`. `Scene` has `key`, `entries`, `previousEntries` (what Back would show), `content`, `metadata`. Overlay scenes (dialog, sheet) also have `overlaidEntries`.

**Order rule:** overlay strategies (bottom sheet, dialog) go **before** non-overlay strategies:
```kotlin
sceneStrategies = listOf(bottomSheetStrategy, dialogStrategy, listDetailStrategy)
```
Create strategies with `remember { }` / their `remember...` function — never instantiate per recomposition.

### 6.1 Dialog (built-in)
```kotlin
entry<ConfirmDelete>(metadata = DialogSceneStrategy.dialog()) { key -> ConfirmDeleteDialog(key.id) }
// NavDisplay(..., sceneStrategies = remember { listOf(DialogSceneStrategy()) })
```

### 6.2 Bottom sheet (NOT in core library — copy this file)
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
internal data class BottomSheetScene<T : Any>(
    override val key: T,
    override val previousEntries: List<NavEntry<T>>,
    override val overlaidEntries: List<NavEntry<T>>,
    private val entry: NavEntry<T>,
    private val modalBottomSheetProperties: ModalBottomSheetProperties,
    private val onBack: () -> Unit,
) : OverlayScene<T> {
    override val entries: List<NavEntry<T>> = listOf(entry)
    lateinit var sheetState: SheetState
    override val content: @Composable (() -> Unit) = {
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ModalBottomSheet(onDismissRequest = onBack, properties = modalBottomSheetProperties, sheetState = sheetState) {
            entry.Content()
        }
    }
    override suspend fun onRemove() { sheetState.hide() } // hide animation when popped
}

@OptIn(ExperimentalMaterial3Api::class)
class BottomSheetSceneStrategy<T : Any> : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val lastEntry = entries.lastOrNull() ?: return null
        val properties = lastEntry.metadata[BottomSheetKey] ?: return null
        @Suppress("UNCHECKED_CAST")
        return BottomSheetScene(
            key = lastEntry.contentKey as T,
            previousEntries = entries.dropLast(1),
            overlaidEntries = entries.dropLast(1),
            entry = lastEntry,
            modalBottomSheetProperties = properties,
            onBack = onBack,
        )
    }
    companion object {
        fun bottomSheet(p: ModalBottomSheetProperties = ModalBottomSheetProperties()) = metadata { put(BottomSheetKey, p) }
        object BottomSheetKey : NavMetadataKey<ModalBottomSheetProperties>
    }
}
// usage: entry<Filters>(metadata = BottomSheetSceneStrategy.bottomSheet()) { FiltersSheet() }
// val bottomSheetStrategy = remember { BottomSheetSceneStrategy<NavKey>() }
```

### 6.3 Material list-detail (preferred for list → detail)
```kotlin
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun MailNav() {
    val backStack = rememberNavBackStack(ConversationList)
    val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
    val directive = remember(windowAdaptiveInfo) {
        calculatePaneScaffoldDirective(windowAdaptiveInfo).copy(horizontalPartitionSpacerSize = 0.dp)
    }
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        sceneStrategies = listOf(listDetailStrategy),
        entryProvider = entryProvider {
            entry<ConversationList>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = { Text("Choose a conversation") }   // shown when nothing selected on wide screens
                )
            ) { ConversationListScreen(onClick = dropUnlessResumed { backStack.add(ConversationDetail("ABC")) }) }
            entry<ConversationDetail>(metadata = ListDetailSceneStrategy.detailPane()) { key -> ConversationDetailScreen(key.id) }
            entry<Profile>(metadata = ListDetailSceneStrategy.extraPane()) { ProfileScreen() }
        }
    )
}
```
Behaviour: 1 pane on compact, list+detail side by side on wider, list+detail+extra on very wide.
Rules:
- When selecting a new detail, **replace** the old detail instead of stacking: `backStack.removeIf { it is ConversationDetail }; backStack.add(newDetail)` (unless multiple details should stay).
- Detail screen must hide its back arrow when shown beside the list. With the custom scene use `LocalBackButtonVisibility`; with Material,
  read the scene itself, never guess from the window size (the directive decides the pane count, and it can differ per flow):
  ```kotlin
  @OptIn(ExperimentalMaterial3AdaptiveApi::class)
  @Composable
  fun isShownBesideList(): Boolean {
      val scope = LocalListDetailSceneScope.current ?: return false          // null = single pane
      return scope.scaffoldTransitionScope.scaffoldStateTransition.targetState[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded
  }
  ```
  Supporting pane: same with `LocalSupportingPaneSceneScope` and `SupportingPaneScaffoldRole.Main`. Hide the arrow only when Back
  would land on the pane beside it; a deeper detail (Back goes to the previous detail) keeps its arrow.
- Full-screen detail mode MUST be disabled inside list-detail.

### 6.4 Material supporting pane
```kotlin
val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
val directive = remember(windowAdaptiveInfo) {
    calculatePaneScaffoldDirective(windowAdaptiveInfo)
        .copy(horizontalPartitionSpacerSize = 0.dp, verticalPartitionSpacerSize = 0.dp)
}
val supportingPaneStrategy = rememberSupportingPaneSceneStrategy<NavKey>(
    backNavigationBehavior = BackNavigationBehavior.PopUntilCurrentDestinationChange, // Back dismisses supporting pane
    directive = directive,
)
// entry<MainVideo>(metadata = SupportingPaneSceneStrategy.mainPane()) { ... }
// entry<RelatedVideos>(metadata = SupportingPaneSceneStrategy.supportingPane()) { ... }
// entry<Profile>(metadata = SupportingPaneSceneStrategy.extraPane()) { ... }
```

### 6.5 Custom scene (when Material doesn't fit)
```kotlin
data class ListDetailScene<T : Any>(
    override val key: Any,
    override val previousEntries: List<NavEntry<T>>,
    val listEntry: NavEntry<T>,
    val detailEntry: NavEntry<T>,
) : Scene<T> {
    override val entries = listOf(listEntry, detailEntry)
    override val content: @Composable () -> Unit = {
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.weight(0.4f)) { listEntry.Content() }
            CompositionLocalProvider(LocalBackButtonVisibility provides false) {
                Column(Modifier.weight(0.6f)) {
                    AnimatedContent(targetState = detailEntry, contentKey = { it.contentKey }) { it.Content() }
                }
            }
        }
    }
    companion object {
        fun listPane() = metadata { put(ListKey, true) }
        fun detailPane() = metadata { put(DetailKey, true) }
    }
    object ListKey : NavMetadataKey<Boolean>
    object DetailKey : NavMetadataKey<Boolean>
}
val LocalBackButtonVisibility = compositionLocalOf { true }

@Composable
fun <T : Any> rememberListDetailSceneStrategy(): ListDetailSceneStrategy<T> {
    val wsc = currentWindowAdaptiveInfoV2().windowSizeClass
    return remember(wsc) { ListDetailSceneStrategy(wsc) }
}
class ListDetailSceneStrategy<T : Any>(val windowSizeClass: WindowSizeClass) : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        if (!windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)) return null
        val detail = entries.lastOrNull()?.takeIf { it.metadata.contains(ListDetailScene.DetailKey) } ?: return null
        val list = entries.findLast { it.metadata.contains(ListDetailScene.ListKey) } ?: return null
        return ListDetailScene(key = list.contentKey, previousEntries = entries.dropLast(1), listEntry = list, detailEntry = detail)
    }
}
```
Custom scene rules:
- Return `null` when you can't build the scene (too narrow, wrong metadata) — let the next strategy try.
- `equals/hashCode` MUST include `key`, `entries`, `previousEntries`; MUST NOT include callbacks like `onBack`. A `data class` without callback properties satisfies this.
- Use a **stable scene key** (e.g. list entry's `contentKey`) so changing the detail animates inside the scene instead of replacing the whole scene.
- `previousEntries = entries.dropLast(1)` unless you have a reason.

---

## 7. ViewModels and arguments (Koin)

- Install `rememberViewModelStoreNavEntryDecorator()` (after the saveable decorator). Then `koinViewModel()`
  called inside an entry is scoped to that entry: created when pushed, cleared when popped, one instance per key
  instance (keyed by `contentKey`) — no manual `key =` needed.
- Without this decorator `koinViewModel()` uses the Activity's store → every entry of the same route shares ONE
  ViewModel (bug for per-item screens).
- Declare the ViewModel with `viewModel<T>()` (DSL) or `@KoinViewModel` (Annotations). Never `single`/`factory`.
- Pass the route (or its id) as a **runtime parameter**: `@InjectedParam` on the constructor + `parametersOf(route)`
  at the call site. Nav3 restores the key from the saved back stack after process death, so the ViewModel is rebuilt
  with the correct key. Other state that must survive process death → `SavedStateHandle` (injected automatically,
  no declaration).

```kotlin
// feature/detail — ViewModel: constructor injection, route as runtime param
class DetailViewModel(
    @InjectedParam private val route: DetailRoute,
    private val repository: ItemRepository,        // from the graph
    private val savedStateHandle: SavedStateHandle, // auto-injected
) : ViewModel()

// DSL module
val detailModule = module {
    viewModel<DetailViewModel>()
    navigation<DetailRoute> { route ->
        DetailScreen(viewModel = koinViewModel { parametersOf(route) }, onDone = LocalNavigator.current::goBack)
    }
}
// Annotations alternative for the VM:  @KoinViewModel class DetailViewModel(@InjectedParam val route: DetailRoute, ...)
```
- Several ViewModels of the same type on ONE screen (per list item): `koinViewModel<ItemVm>(key = id) { parametersOf(id) }`.
- Shared ViewModel between a list and its detail (list-detail flows): prefer passing data via keys/repository; if a
  shared instance is really needed, scope it (ask the user first — §14.6), never by making it `single`.
- Known issue (Compiler Plugin 1.2.1): `@InjectedParam` on a definition in a **different Gradle module** than
  `startKoin` fails with `[KOIN-D001]`. On 1.2.1 use the lambda form there:
  `viewModel { (route: DetailRoute) -> DetailViewModel(route, get(), get()) }`. Newer plugin → reified form.
- ViewModels never hold the back stack or `Navigator`; UI calls lambdas. Collect flows with
  `collectAsStateWithLifecycle()` inside the destination (entry-scoped lifecycle).

---

## 8. Returning results

Add `rememberResultEventBusNavEntryDecorator()` to the decorators (package `androidx.navigation3.runtime.result`).

```kotlin
// Sender
entry<ContactPicker> {
    val bus = LocalResultEventBus.current
    ContactPickerScreen(onPicked = { contact -> bus.sendResult<Contact>(result = contact); navigator.goBack() })
}
// Receiver — event style (one-shot, e.g. forward to ViewModel)
@Composable fun ComposeScreen(vm: ComposeViewModel = koinViewModel()) {
    ResultEffect<Contact> { contact -> vm.onRecipientSelected(contact) }
}
// Receiver — state style (only latest value matters)
entry<Home> {
    val person = LocalResultEventBus.current.conflateAsState<Person?>(null).value
    HomeScreen(person)
}
```
Process death: results are in memory only. If the result must persist, store it in the receiver's `rememberSaveable`, a `SavedStateHandle` ViewModel, or put the data in a `NavKey`.

---

## 9. Conditional navigation (login, onboarding)

App-level Intro/Auth/Main gating is done by `RootNavDisplay` (A10). Use the pattern below for gates *inside* Main (e.g. a premium-only screen).

```kotlin
@Serializable sealed class ConditionalNavKey(val requiresLogin: Boolean = false) : NavKey
@Serializable data object Home : ConditionalNavKey()
@Serializable data object Profile : ConditionalNavKey(requiresLogin = true)
@Serializable data class Login(val redirectToKey: ConditionalNavKey? = null) : ConditionalNavKey()

class Navigator(
    private val backStack: NavBackStack<ConditionalNavKey>,
    private val onNavigateToRestrictedKey: (ConditionalNavKey?) -> ConditionalNavKey,
    private val isLoggedIn: () -> Boolean,
) {
    fun navigate(key: ConditionalNavKey) {
        if (key.requiresLogin && !isLoggedIn()) backStack.add(onNavigateToRestrictedKey(key)) else backStack.add(key)
    }
    fun goBack() = backStack.removeLastOrNull()
}
// Login success: isLoggedIn = true; key.redirectToKey?.let { backStack.remove(key); navigator.navigate(it) }
// Logout: isLoggedIn = false; backStack.removeIf { it.requiresLogin }; ensure start destination remains.
```
Typed back stack overload for a `NavKey` subtype:
```kotlin
@Composable
fun <T : NavKey> rememberNavBackStack(vararg elements: T): NavBackStack<T> =
    rememberSerializable(serializer = NavBackStackSerializer(elementSerializer = NavKeySerializer())) { NavBackStack(*elements) }
```
Use a sealed class/interface for keys — kotlinx.serialization handles sealed polymorphism automatically.
Login state comes from your auth repository (DataStore etc.), not `rememberSaveable`, in real apps.

---

## 10. Deep links (Nav3 ≥ 1.2.0)

Steps: (1) intent-filters in `AndroidManifest.xml`; (2) create `DeepLinkMatcher`s; (3) match in `onCreate` **and** `onNewIntent`; (4) build the back stack.

```kotlin
val homeMatcher = StaticKeyDeepLinkMatcher(HomeKey, listOf(DeepLinkMatcher.actionFilter(Intent.ACTION_VIEW)))
val userMatcher = UriDeepLinkMatcher(DeepLinkUri("www.example.com/users/{id}"), serializer<UserProfileKey>())
    .withBackStack { match -> listOf(HomeKey, match.key) }      // synthetic back stack (principle!)
val deepLinkMatchers: List<DeepLinkMatcher<*, *>> = listOf(homeMatcher, userMatcher)

fun backStackFor(intent: Intent): List<NavKey> {
    val result = deepLinkMatchers.mapNotNull { it.match(DeepLinkRequest(intent = intent)) }.maxOrNull()
    return when (result) {
        null -> listOf(HomeKey)
        is BackStackMatchResult<*, *> -> @Suppress("UNCHECKED_CAST") (result.backStack as List<NavKey>)
        else -> listOf(result.key as NavKey)
    }
}
// onCreate: seed rememberNavBackStack / NavigationState with backStackFor(intent) (only when savedInstanceState == null)
// onNewIntent: compute backStackFor(intent) and REPLACE the current stack (clear + addAll)
```
Rules:
- Every deep-linked target gets a realistic synthetic back stack (start destination + parents) — never a lone detail screen.
- Path `{id}` and query args map to the key's properties via its serializer. Custom arg types (e.g. `LocalDateTime`) need a `DeepLinkSerializer`.
- Each feature contributes its matcher to Koin with a distinct qualifier, collected with `getAll()` (§14.5).
- From another app's task: Up goes through your synthetic stack; Back returns to the caller app. Use `FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK` when restarting in your own task.

---

## 11. Animations

```kotlin
NavDisplay(
    ...,
    transitionSpec = { slideInHorizontally(initialOffsetX = { it }) togetherWith slideOutHorizontally(targetOffsetX = { -it }) },
    popTransitionSpec = { slideInHorizontally(initialOffsetX = { -it }) togetherWith slideOutHorizontally(targetOffsetX = { it }) },
    predictivePopTransitionSpec = { slideInHorizontally(initialOffsetX = { -it }) togetherWith slideOutHorizontally(targetOffsetX = { it }) },
)
// Per destination override via metadata:
entry<ScreenC>(metadata = metadata {
    put(NavDisplay.TransitionKey) { slideInVertically(initialOffsetY = { it }) togetherWith ExitTransition.KeepUntilTransitionsFinished }
    put(NavDisplay.PopTransitionKey) { EnterTransition.None togetherWith slideOutVertically(targetOffsetY = { it }) }
    put(NavDisplay.PredictivePopTransitionKey) { EnterTransition.None togetherWith slideOutVertically(targetOffsetY = { it }) }
}) { ScreenC() }
```
Always set `predictivePopTransitionSpec` when you customise pop. Shared elements: wrap in `SharedTransitionLayout { NavDisplay(..., sharedTransitionScope = this) }`.

---

## 12. Modularization (Koin) — only for multi-Gradle-module projects

Default is a single `:app` module with per-feature Koin modules in `di/` (A2, §14.7). Use the layout below only when the user asks for Gradle modules.

- `:core:navigation`: `NavigationState`, `Navigator`, `LocalNavigator`, shared top-level keys.
- `:feature:x:api`: only the feature's `@Serializable` `NavKey`s (+ deep-link URI constants). No Koin, no UI.
- `:feature:x:impl`: screens, ViewModels, and the feature's Koin module(s) — definitions + `navigation<Key> { }`
  entries + its `BackStackMatcher`. Module lives in a `.di` package (e.g. `com.acme.feature.home.di`).
- `:app`: `appModule` that `includes(...)` every feature module; starts Koin; hosts `NavDisplay(entryProvider = koinEntryProvider())`.
- Features navigate to other features only through **api** keys, never impl classes.
- Make sure every feature module is on `:app`'s compile classpath (direct dependency or `api`), otherwise the
  Compiler Plugin reports `[KOIN-D001]` for definitions that exist.

---

## 13. Lifecycle & interop

- `LocalLifecycleOwner.current` inside an entry is entry-scoped. Use `collectAsStateWithLifecycle`, `LifecycleStartEffect`, `LifecycleResumeEffect` there.
- Legacy Fragment/View screens: host them with `AndroidView` / `AndroidFragment` inside an `entry`. Prefer Compose.

---

## 14. Dependency injection with Koin (MANDATORY DI for every project)

Source of truth: official Koin skills (Koin 4.x + Koin Compiler Plugin) and insert-koin.io Navigation 3 docs.
Never answer Koin API questions from memory — when unsure, check https://insert-koin.io or the project's existing Koin code.

### 14.0 Read the project first
1. Read the version catalog / build files: Koin version, Compiler Plugin present?, style used (Annotations or DSL).
2. Koin not set up yet → set it up (§14.1–14.4). Already set up → **follow its style, packages and module conventions**; never rewrite existing modules into another style unasked.
3. Project on Koin 3.x / Koin KSP / no Compiler Plugin → do the task in the current setup, then propose ONCE to add the Compiler Plugin (compile-time graph check). Don't change it unasked.

### 14.1 Gradle
Versions: defaults Kotlin `2.3.20`, Koin `4.2.2`, Koin Compiler Plugin `1.2.1` — these are minimums; check Maven Central
(`io.insert-koin:koin-bom`, `io.insert-koin:koin-compiler-gradle-plugin`) for newer releases and say which version you used.

```toml
[versions]
koin = "4.2.2"
koinCompiler = "1.2.1"
[libraries]
koin-bom = { module = "io.insert-koin:koin-bom", version.ref = "koin" }
koin-core = { module = "io.insert-koin:koin-core" }
koin-android = { module = "io.insert-koin:koin-android" }
koin-compose = { module = "io.insert-koin:koin-compose" }
koin-compose-viewmodel = { module = "io.insert-koin:koin-compose-viewmodel" }
koin-compose-navigation3 = { module = "io.insert-koin:koin-compose-navigation3" }
koin-annotations = { module = "io.insert-koin:koin-annotations" }          # only for Annotations style / @InjectedParam, @Named...
koin-androidx-workmanager = { module = "io.insert-koin:koin-androidx-workmanager" }  # only if Workers
koin-test = { module = "io.insert-koin:koin-test" }
koin-test-junit4 = { module = "io.insert-koin:koin-test-junit4" }
[plugins]
koin-compiler = { id = "io.insert-koin.compiler.plugin", version.ref = "koinCompiler" }
```
```kotlin
// module build.gradle.kts
plugins { alias(libs.plugins.koin.compiler) }
dependencies {
    implementation(platform(libs.koin.bom))      // BOM resolves every Koin artifact version — never pin them one by one
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.koin.compose.navigation3)
    implementation(libs.koin.annotations)        // needed for @InjectedParam / @Named even in DSL style
    testImplementation(libs.koin.test); testImplementation(libs.koin.test.junit4)
}
```
Never add KSP / `koin-ksp-compiler` for Koin. Never pin `koin-annotations:2.x`. Apply the plugin in every Gradle module
that declares definitions (convention plugin: depend on the Koin Gradle plugin with `implementation`, not `compileOnly`).

### 14.2 Definition style — one per project, chosen explicitly
- Default for our projects: **Koin DSL with the Compiler Plugin** (`single<T>()`, `factory<T>()`, `viewModel<T>()`). Existing project → detect and keep its style. State the style before writing code.
- **Navigation 3 entries (`navigation<Key> { }`) are DSL-only for now.** In an Annotations project, each feature keeps
  its nav entries in one small DSL module — tell the user it is the one DSL module (one style per module, never both in one module).
- DSL form preference with the Compiler Plugin: `single<T>()`, `factory<T>()`, `viewModel<T>()`, `worker<T>()` (reified, constructor
  auto-resolved). Third-party types → function builder `single { create(::buildX) }`. Classic `single { X(get()) }` only as last resort.
  Imports: Compiler Plugin DSL `org.koin.plugin.module.dsl.*`; `module { }` from `org.koin.dsl.module`.

| Lifetime | DSL | Annotations | Use for |
|---|---|---|---|
| App-wide shared | `single<T>()` | `@Singleton` | HTTP client, database, ApiServices, repositories |
| New each time | `factory<T>()` | `@Factory` | Use cases, mappers, cheap stateless objects |
| ViewModel | `viewModel<T>()` | `@KoinViewModel` | Every ViewModel — always |
| Scoped | `scoped<T>()` in a scope | `@Scoped`/archetypes | Advanced only (§14.6) |

### 14.3 Declaring Nav3 entries in Koin modules
Package `org.koin.dsl.navigation3.navigation`; API is `@KoinExperimentalAPI` → `@OptIn(KoinExperimentalAPI::class)` on the module.
`navigation<Key>(metadata = ...)` accepts the same Nav3 metadata as `entry<Key>` → scenes, dialogs, sheets, animations all work.

```kotlin
// di/conversationsModule.kt
@OptIn(KoinExperimentalAPI::class)
val conversationsModule = module {
    // data + domain
    single<ConversationApiService>()
    single<ConversationRepositoryImpl>() bind ConversationRepository::class
    factory<GetConversationsUseCase>()
    viewModel<ConversationListViewModel>()
    viewModel<ConversationDetailViewModel>()   // has @InjectedParam route

    // navigation entries (metadata = adaptive scenes, §6)
    navigation<ConversationList>(
        metadata = ListDetailSceneStrategy.listPane(detailPlaceholder = { Text("Select a conversation") })
    ) {
        val navigator = LocalNavigator.current
        ConversationListScreen(
            viewModel = koinViewModel(),
            onOpen = { id -> navigator.navigate(ConversationDetail(id)) },
        )
    }
    navigation<ConversationDetail>(metadata = ListDetailSceneStrategy.detailPane()) { route ->
        ConversationDetailScreen(viewModel = koinViewModel { parametersOf(route) })
    }
    navigation<DeleteConversation>(metadata = DialogSceneStrategy.dialog()) { route ->
        DeleteConversationDialog(route.id, onDone = LocalNavigator.current::goBack)
    }
}
```
Host (root composable):
```kotlin
NavDisplay(
    entries = navigationState.toEntries(koinEntryProvider<NavKey>()),   // tabs (§4); single stack: backStack = ..., entryProvider = koinEntryProvider<NavKey>()
    onBack = { navigator.goBack() },
    sceneStrategies = listOf(bottomSheetStrategy, dialogStrategy, listDetailStrategy),
)
```
- `koinEntryProvider<NavKey>()` (`org.koin.compose.navigation3`) aggregates every `navigation<>` from loaded modules. Type it to the back-stack element type (`NavKey`). `RootNavDisplay` and `MainNavDisplay` may share it — each renders only the keys in its own stack.
- Android Activity alternative: `val entryProvider by entryProvider<NavKey>()` / `getEntryProvider<NavKey>()` (`org.koin.androidx.compose.navigation3`).
- Entries in a scope: `koinEntryProvider(scope = myScope)`; entries declared inside a scope are visible only from that scope.
- Always install the decorators (`rememberSaveableStateHolderNavEntryDecorator()`, `rememberViewModelStoreNavEntryDecorator()`) — in `toEntries()` for tabs, in `NavDisplay(entryDecorators=)` for a single stack.
- A route with no `navigation<>` declaration in any loaded module crashes at runtime — add the feature module to `appModule.includes(...)`.

### 14.4 Start Koin (Android app)
```kotlin
// DSL style (default) — see A4 for the full App.kt
val appModule = module { includes(networkModule, databaseModule, dataStoreModule, homeModule, conversationsModule) }
startKoin {                                   // org.koin.core.context.startKoin
    androidLogger(if (BuildConfig.DEBUG) Level.INFO else Level.NONE)
    androidContext(this@App)
    modules(appModule)                        // ONE entry module, children via includes — never modules(*list)
}

// Annotations style (only if the project already uses it)
@KoinApplication
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin<App> {                      // org.koin.plugin.module.dsl.startKoin — triggers discovery
            androidContext(this@App)
            modules(navigationModules)        // the DSL nav-entry modules (mixing allowed, §14.2)
        }
    }
}
@Module @Configuration @ComponentScan("com.acme.feature.home") class HomeModule   // triad on each root module
```
- Start Koin once, in `Application.onCreate()` (register it in the manifest). No `KoinApplication {}` composable in an Android app; `KoinContext` is deprecated.
- Annotations: missing any of `@Module @Configuration @ComponentScan` on a root module → runtime `NoDefinitionFoundException`, not a compile error. `@ComponentScan` the feature's own package, never a broad root.
- Values known at startup (BuildConfig URL, flags): `koin.declare(value, qualifier<ApiUrl>())` in `startKoin` + `@Provided @ApiUrl` parameter, or `properties(...)` + `@Property("api.url")`.

### 14.5 Deep links with Koin
```kotlin
// each feature module: contribute its matcher with a DISTINCT qualifier (same type twice without qualifier = only the last survives)
single<BackStackMatcher<*, NavKey>>(named<ConversationDetail>()) {
    UriDeepLinkMatcher(DeepLinkUri("$CONVERSATION_URI/{id}"), ConversationDetail.serializer())
        .withBackStack { match -> listOf(ConversationList, match.key) }
}

// MainActivity — entry point uses Koin's Android API, no KoinComponent needed
class MainActivity : ComponentActivity() {
    private val matchers: List<BackStackMatcher<*, NavKey>> by lazy { getKoin().getAll() }
    private val pendingDeepLink = MutableStateFlow<List<NavKey>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) pendingDeepLink.value = match(intent)
        // ... splash / session as in A4 ...
        setContent { AppTheme { RootNavDisplay(session, pendingDeepLink) } }   // Main entry passes it on to MainNavDisplay
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); pendingDeepLink.value = match(intent) }
    private fun match(intent: Intent): List<NavKey>? =
        if (intent.data == null) null
        else matchers.firstNotNullOfOrNull { it.match(DeepLinkRequest(intent)) }?.backStack
}

// In MainNavDisplay(): consume once
val link by pendingDeepLink.collectAsState()
LaunchedEffect(link) { link?.let { navigator.openDeepLink(it); pendingDeepLink.value = null } }

// Navigator: put the synthetic stack into the right tab
fun openDeepLink(stack: List<NavKey>) {
    val root = stack.firstOrNull { it in state.backStacks.keys } ?: state.startRoute
    state.topLevelRoute = root
    state.backStacks.getValue(root).apply { clear(); add(root); addAll(stack.dropWhile { it != root }.drop(1)) }
}
```
Deep links into Main while logged out: keep the pending link, let `RootNavDisplay` route to Auth, apply it after login.
Why `NavigationState` in composition (and not a Koin `single` Navigator with `mutableStateListOf`): `rememberNavBackStack`
survives **process death**; a Koin-held list does not. The nav3 `modular-koin` recipe keeps `Navigator` in
`activityRetainedScope { scoped { Navigator(...) } }` — survives rotation only. Use that recipe form only if the user accepts losing the stack on process death.

### 14.6 Scopes — advanced, ask first
Order: no scope (`single`/`factory`/`viewModel`) → archetype (`viewModelScope { scoped<X>(); viewModel<VM>() }` + `options(viewModelScopeFactory())` in `startKoin`; `activityScope`, `activityRetainedScope`, `fragmentScope`) → custom `scope<SessionScope> { }` (code opens/closes it). Use a scope only when several objects of the same lifetime must share one instance.

### 14.7 Project Koin modules (A2 `di/` package)
```kotlin
// di/networkModule.kt
fun buildJson(): Json = Json { ignoreUnknownKeys = true; explicitNulls = false }
fun buildHttpClient(json: Json, storage: StorageRepository): HttpClient = HttpClient(OkHttp) {
    defaultRequest { url(BuildConfig.BASE_URL + "/api/v1/") }
    install(ContentNegotiation) { json(json) }
    install(HttpTimeout) { requestTimeoutMillis = 30_000 }
    install(Auth) { bearer { loadTokens { storage.bearerTokens() }; refreshTokens { storage.refresh(client) } } }
    if (BuildConfig.DEBUG) install(Logging) { logger = object : Logger { override fun log(message: String) = Timber.tag("Ktor").d(message) }; level = LogLevel.BODY }
}
val networkModule = module {
    single { create(::buildJson) }
    single { create(::buildHttpClient) }          // function builder: params injected like a constructor
}

// di/databaseModule.kt
fun buildDatabase(context: Context): AppDatabase =
    Room.databaseBuilder(context, AppDatabase::class.java, "app-db").build()   // add migrations / cache-only destructive fallback (A9)
fun buildXxxDao(db: AppDatabase): XxxDao = db.xxxDao()
val databaseModule = module {
    single { create(::buildDatabase) }            // Context = androidContext()
    single { create(::buildXxxDao) }
}

// di/dataStoreModule.kt
fun buildDataStore(context: Context): DataStore<Preferences> =
    createDataStore { context.filesDir.resolve(DATA_STORE_FILE_NAME).absolutePath }
val dataStoreModule = module {
    single { create(::buildDataStore) }
    single<StorageRepositoryImpl>() bind StorageRepository::class
    factory<GetStringFromDataStoreUseCase>(); factory<SaveStringToDataStoreUseCase>(); factory<GetBooleanFromDataStoreUseCase>()
    viewModel<SharedViewModel>()
}

// di/xxxModule.kt — one per feature
@OptIn(KoinExperimentalAPI::class)
val xxxModule = module {
    single<XxxApiService>()
    single<XxxRepositoryImpl>() bind XxxRepository::class
    factory<CreateXxxUseCase>(); factory<ObserveXxxItemsUseCase>()
    viewModel<XxxViewModel>()
    navigation<XxxList> { XxxScreen(viewModel = koinViewModel(), onDone = LocalNavigator.current::goBack) }
}

// di/appModule.kt
val appModule = module { includes(networkModule, databaseModule, dataStoreModule, xxxModule /*, every feature */) }
```
Existing project with one big classic-DSL `appModule` (`single { X(get()) }`): add new definitions in the same style there,
and propose ONCE to split per feature + add the Compiler Plugin. Don't rewrite unasked.
- Constructor injection; domain/data classes have **no Koin imports**.
- Constructors are cheap: no I/O in `init`. Not needed at startup → `Lazy<T>` parameter.
- Never hold an `Activity`/`Fragment`/View in a `single`; singletons get the application `Context` only.
- Two implementations of one interface → distinct types first (`AuthHttpClient`, `PublicHttpClient`); otherwise `@Named` / custom `@Qualifier` on the class, parameter or function builder.
- WorkManager: `worker<SyncWorker>()` + `workManagerFactory()` in `startKoin` + remove the default `WorkManagerInitializer` from the manifest.
- Compose state holders tied to a composable: `koinInject<T>()`.

### 14.8 Verify & test
- The Compiler Plugin checks the whole graph at build time (`[KOIN-…]` errors) — fix what it reports. **No `checkModules()` / `verify()` tests.**
- After first setup, propose to the user (their step): remove one needed definition → build must fail with `[KOIN-…]` → restore. A green build means the plugin doesn't see the modules (e.g. `modules(*list)`).
- Unit tests: build classes directly with fakes — no Koin. Need the real graph → `KoinTestRule.create { modules(featureModule) }` + `by inject()`; override with `declare<T> { Fake() }`.
- Compose previews/tests: target `XxxContent` with fake state instead of starting Koin.
- Runtime Koin exception (`NoDefinitionFoundException`, `InstanceCreationException`) → check the module is included and the route's `navigation<>` exists; deeper runtime analysis (startup, duplicates) → Koin's `koin-diagnose` skill / Kotzilla (optional, propose once).

### 14.9 Koin anti-patterns (push back before writing them)
ViewModel as `single` · `getKoin().get<SomeViewModel>()` · Activity/Context-of-Activity inside a singleton · `KoinComponent`/`get()` in repositories, use cases or ViewModels · dependencies passed via `parametersOf` · I/O in constructors · `createdAtStart` on everything · `named("x")` strings everywhere instead of distinct types · KSP for Koin · Hilt annotations.
When the user asks for one of these: name the principle and its cost, propose the right form, write the requested form only after an explicit OK and flag it.

---

## 15. Adaptive layouts inside screens

### 15.1 Window size classes — the ONLY way to pick a layout
Width: Compact < 600 · Medium 600–839 · Expanded 840–1199 · Large 1200–1599 · Extra-large ≥ 1600.
Height: Compact < 480 · Medium 480–899 · Expanded ≥ 900.
Constants (`androidx.window.core.layout.WindowSizeClass`): `WIDTH_DP_MEDIUM_LOWER_BOUND`, `WIDTH_DP_EXPANDED_LOWER_BOUND`,
`WIDTH_DP_LARGE_LOWER_BOUND`, `WIDTH_DP_EXTRA_LARGE_LOWER_BOUND`, `HEIGHT_DP_MEDIUM_LOWER_BOUND`, `HEIGHT_DP_EXPANDED_LOWER_BOUND`.

Rules:
- Every screen/layout decision (columns, one vs two panes, side-by-side vs stacked, which composable to show) MUST read
  `currentWindowAdaptiveInfoV2().windowSizeClass` and test it with `isWidthAtLeastBreakpoint(...)` /
  `isHeightAtLeastBreakpoint(...)`. Check from the largest class down:
```kotlin
val wsc = currentWindowAdaptiveInfoV2().windowSizeClass
when {
    wsc.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> ExpandedLayout()
    wsc.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> MediumLayout()
    else -> CompactLayout()
}
// height matters (landscape phone, short window): wsc.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)
```
- Read it where it is needed (screen or component) — do not pass a size class down from `MainActivity` (A4).
- NEVER compare raw dp values to choose a layout: no `maxWidth < 800.dp`, no `constraints.maxWidth.toDp() >= 840.dp`,
  no `windowWidth < 600.dp`, no `LocalConfiguration.current.screenWidthDp > …`. `BoxWithConstraints` / Grid `constraints`
  may only be used to **size** things (e.g. split the available width into equal cells), never as a breakpoint.
- dp values that are **sizes, not breakpoints** stay as dp: padding, gaps, `widthIn(max = …)` caps, item min widths,
  touch targets (≥ 48dp), fixed track widths like a 280dp sidebar.
- Never base decisions on device type, orientation, or physical screen size.

### 15.2 Which layout API?

| Need | API |
|---|---|
| Large/unbounded homogeneous data, lazy loading | `LazyVerticalGrid(GridCells.Fixed(adaptiveColumnCount()))`, `LazyVerticalStaggeredGrid(StaggeredGridCells.Fixed(adaptiveColumnCount()))` (§15.3) |
| Screen/component **structure** in 2D (dashboard, header+sidebar+content, fixed set of cards) | `Grid` (experimental, no lazy loading) |
| Small number of items in one direction that wrap/grow/shrink (chips, buttons, tags, toolbar) | `FlexBox` (experimental, no lazy loading) |
| Simple 1D | `Row` / `Column` |
| Device capability/posture/input decisions | `mediaQuery` / `derivedMediaQuery` (experimental) |

### 15.3 Make vertical lists adaptive (MANDATORY step for list screens)
Column counts come from the window size class through ONE shared helper, `ui/component/AdaptiveColumns.kt`
(create it if the project has none; keep the project's numbers if it already has one):
```kotlin
// ui/component/AdaptiveColumns.kt
/** List column count from the window width size class: compact 1, medium 2, expanded and up 3. */
@Composable
fun adaptiveColumnCount(): Int {
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    return when {
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> 3
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> 2
        else -> 1
    }
}
```
- `LazyColumn` of uniform items → `LazyVerticalGrid(columns = GridCells.Fixed(adaptiveColumnCount()))`.
- `LazyVerticalGrid(GridCells.Fixed(n))` with a hard-coded `n` → `GridCells.Fixed(adaptiveColumnCount())`.
- `LazyVerticalStaggeredGrid` → `StaggeredGridCells.Fixed(adaptiveColumnCount())`.
- A grid that needs other counts (dense thumbnails, chips) adds its own helper in `AdaptiveColumns.kt` with the same
  `when` on the size class (e.g. `thumbnailColumnCount()` 3 / 4 / 6) — never a count computed from measured dp.
- Never build column counts inline in a screen; never call `GridCells.Adaptive(...)` to pick the count for list screens.
- Use `rememberLazyGridState()` (saveable). Give items stable `key = { it.id }`.
- Full-width headers: `item(span = { GridItemSpan(maxLineSpan) }) { Header() }`.
- Text-heavy reading content: cap width (`Modifier.widthIn(max = 840.dp)` — a size cap, not a breakpoint) and center it instead of stretching.

### 15.4 Grid (experimental — confirm with user first)
Dependency `androidx.compose.foundation:foundation-layout` (Compose 1.11+; docs use 1.13.0-beta02). Add `@OptIn(ExperimentalGridApi::class)` where the compiler asks.
Replace a non-lazy `Column` of same-type items **completely** with `Grid` (don't nest Grid inside the old Column, don't swap to a lazy grid).

```kotlin
@OptIn(ExperimentalGridApi::class)
@Composable
fun StatsGrid(items: List<Stat>) {
    // layout choice: from the window size class (read outside `config`, which is not @Composable)
    val wsc = currentWindowAdaptiveInfoV2().windowSizeClass
    val (cols, rows) = if (wsc.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)) 4 to 2 else 2 to 4
    Grid(
        config = {
            val gap = 8.dp
            // sizing only (not a breakpoint): split the available width into equal square cells
            val cell = ((constraints.maxWidth.toDp() - gap * (cols - 1)) / cols).coerceAtLeast(0.dp)
            repeat(cols) { column(cell) }
            repeat(rows) { row(cell) }
            gap(gap)
        }
    ) { items.forEach { StatCard(it) } }
}
```
Inside `config`, `constraints` may size tracks; the number of columns/rows and which areas exist come from the size class (§15.1).
Track sizes: `column(180.dp)` fixed · `row(0.2f)` percentage 0–1 · `row(1.fr)` flexible share of remaining · `GridTrackSize.Auto / MinContent / MaxContent` intrinsic · `GridTrackSize.MinMax(100.dp, 1.fr)` min + flexible.
Gaps: `gap(8.dp)` or `rowGap()` / `columnGap()`. Fill order: `flow = GridFlow.Row` (default) or `GridFlow.Column`.
Item placement (1-based, negative counts from end): `Modifier.gridItem(row = 2, column = 2)`, `Modifier.gridItem(rowSpan = 2, columnSpan = 2, alignment = Alignment.Center)`, `Modifier.gridItem(columnSpan = 3)`.
Named areas (preferred for complex screen structure):
```kotlin
enum class Area { Header, Sidebar, Content }
val wide = currentWindowAdaptiveInfoV2().windowSizeClass
    .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)   // size class, never constraints vs dp
Grid(config = {
    if (wide) {
        column(280.dp); column(1.fr); row(GridTrackSize.Auto); row(GridTrackSize.MinMax(0.dp, 1.fr))
        area(Area.Header, row = 1, column = 1, columnSpan = 2)
        area(Area.Sidebar, row = 2, column = 1)
        area(Area.Content, row = 2, column = 2)
    } else {
        column(1f); row(GridTrackSize.Auto); row(GridTrackSize.Auto); row(GridTrackSize.MinMax(0.dp, 1.fr))
        area(Area.Header, row = 1, column = 1); area(Area.Sidebar, row = 2, column = 1); area(Area.Content, row = 3, column = 1)
    }
    gap(16.dp)
}) {
    Header(Modifier.gridItem(areaId = Area.Header))
    Filters(Modifier.gridItem(areaId = Area.Sidebar))
    LazyColumn(Modifier.gridItem(areaId = Area.Content)) { /* scrolls: track uses MinMax(0.dp, 1.fr) */ }
}
```

### 15.5 FlexBox (experimental — confirm with user first)
Same dependency as Grid. **Function-call syntax** (`direction(...)`, `grow(1f)`), not assignment.
```kotlin
FlexBox(
    config = {
        direction(FlexDirection.Row)          // Row | RowReverse | Column | ColumnReverse
        wrap(FlexWrap.Wrap)                   // NoWrap (default) | Wrap | WrapReverse
        justifyContent(FlexJustifyContent.Start) // Start | Center | End | SpaceBetween | SpaceAround | SpaceEvenly
        alignItems(FlexAlignItems.Center)     // Start | End | Center | Stretch | Baseline
        alignContent(FlexAlignContent.Start)  // only with wrap: Start | End | Center | Stretch | SpaceBetween | SpaceAround
        gap(8.dp)                             // or rowGap() / columnGap()
    }
) {
    Chip("All")
    SearchField(Modifier.flex { basis(240.dp); grow(1f); shrink(1f) })  // takes remaining space, min-ish 240dp
    Button(onClick = {}, modifier = Modifier.flex { shrink(0f) }) { Text("Go") } // never shrinks
}
```
Item props in `Modifier.flex { }`: `basis(FlexBasis.Auto | 200.dp | 0.7f)`, `grow(weight)` (default 0), `shrink(weight)` (default 1), `alignSelf(FlexAlignSelf.Auto|Center|End|Stretch|Baseline|Start)`, `order(int)` (default 0, ascending).
Adaptive tricks: action row that wraps on narrow screens (`wrap(Wrap)` + `grow`); form fields side-by-side when wide via `basis(0.5f)` + wrap; reorder with `order(-1)`.

### 15.6 mediaQuery (experimental)
Enable once:
```kotlin
class App : Application() {
    override fun onCreate() { ComposeUiFlags.isMediaQueryIntegrationEnabled = true; super.onCreate() /* then Timber + Koin as in A4 */ }
}
```
Parameters in `UiMediaScope`: `windowWidth`, `windowHeight` (Dp), `windowPosture` (Tabletop/Book/Flat), `pointerPrecision` (Fine > Coarse > Blunt, None), `keyboardKind` (Physical/Virtual/None), `hasCamera`, `hasMicrophone`, `viewingDistance` (Near/Medium/Far).
Use `mediaQuery` for posture, pointer, keyboard, camera/mic and viewing distance. **Do NOT use `windowWidth` / `windowHeight`
for layout decisions** — width and height decisions go through the window size class (§15.1):
```kotlin
// width → size class, not mediaQuery { windowWidth < … }
val wsc = currentWindowAdaptiveInfoV2().windowSizeClass
when {
    wsc.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> ThreePane()
    wsc.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> TwoPane()
    else -> SinglePane()
}

// capability / input decisions → mediaQuery
if (mediaQuery { pointerPrecision == UiMediaScope.PointerPrecision.Blunt }) LargeButton() else NormalButton()  // target size
if (mediaQuery { hasMicrophone }) MicButton()
val fontSize = when { mediaQuery { viewingDistance == UiMediaScope.ViewingDistance.Far } -> 20.sp
                      mediaQuery { viewingDistance == UiMediaScope.ViewingDistance.Medium } -> 18.sp; else -> 16.sp }
```
Preview override: wrap content in `CompositionLocalProvider(LocalUiMediaScope provides object : UiMediaScope by LocalUiMediaScope.current { override val windowPosture = UiMediaScope.Posture.Tabletop })`.
If the user declines experimental APIs, fall back to `currentWindowAdaptiveInfoV2()` (`windowSizeClass`, `windowPosture`).

### 15.7 Adaptive components
- Touch targets ≥ 48dp; bigger for `Blunt` pointers, can be denser for `Fine` (mouse).
- Buttons/dialogs/forms: don't stretch full width on wide windows (`widthIn(max = …)` — a size cap, not a breakpoint).
- Images: `ContentScale.Crop` + aspect ratio, not fixed heights.
- Use `WindowInsets`/`safeDrawingPadding` (edge-to-edge); avoid the hinge for interactive content.

---

## 16. Preserve state across configuration changes (audit EVERY `remember`)

- User input / interactive UI state: `remember { mutableStateOf(..) }` → `rememberSaveable { mutableStateOf(..) }` (text drafts, search query, toggles, selected tab, expanded flags). Key it when per item: `rememberSaveable(conversationId) { mutableStateOf("") }`.
- Custom types: `var s by rememberSaveable(stateSaver = MySaver) { mutableStateOf(MyState()) }` (use **`stateSaver`**, not `saver`, when returning `MutableState`). Or `@Serializable` + `rememberSerializable`.
- Scroll: `remember { LazyListState() }` → `rememberLazyListState()`; `LazyGridState` → `rememberLazyGridState()`; `LazyStaggeredGridState` → `rememberLazyStaggeredGridState()`; `ScrollState(0)` → `rememberScrollState()`. Text fields: `rememberTextFieldState()`.
- Navigation: `rememberNavBackStack` / `NavigationState` (already saveable). Entry UI state saved by `rememberSaveableStateHolderNavEntryDecorator`.
- Screen data: ViewModel + `SavedStateHandle` for process death.
- Media (ExoPlayer): save `positionMs` + `playWhenReady` in `rememberSaveable(videoId)`; update them continuously (listener + periodic loop), restore before `prepare()`, and in `LifecycleStartEffect` only `play()` if `savedPlayWhenReady`; in `onStopOrDispose` capture `wasPlaying` before `pause()` then restore it.

---

## 17. App bars on scroll

Each top-level screen owns its own `TopAppBarScrollBehavior`:
- `TopAppBarDefaults.enterAlwaysScrollBehavior()` — hides on scroll down, returns on any scroll up (feeds, lists).
- `TopAppBarDefaults.exitUntilCollapsedScrollBehavior()` — returns only at the top (large/medium headers, detail).
Connect: `Scaffold(Modifier.nestedScroll(scrollBehavior.nestedScrollConnection), topBar = { TopAppBar(..., scrollBehavior = scrollBehavior) })`.

---

## 18. Desktop / laptop / ChromeOS experiences

First list ALL targets across the app, implement all, re-check the list.
- **Keyboard shortcuts**: handle with `Modifier.onPreviewKeyEvent` / `onKeyEvent` at screen root (e.g. Ctrl+F search, Ctrl+N new, Esc close, Enter submit, arrow keys in lists); publish them via `Activity.onProvideKeyboardShortcuts()` for Keyboard Shortcuts Helper. Ensure focus order and visible focus indication.
- **Tooltips**: every icon-only button gets a Material 3 tooltip (`TooltipBox` + `PlainTooltip`, positioned with the tooltip position provider defaults).
- **Cursors**: clickable/draggable/text elements set `Modifier.pointerHoverIcon(PointerIcon.Hand | Text | Crosshair …)`; add hover states.
- Right-click context menus and drag-and-drop where users expect them.
(Exact tooltip/cursor API names change between Material versions — verify against the project's version.)

---

## 19. Foldables: tabletop & book posture

Detect: `currentWindowAdaptiveInfoV2().windowPosture.isTabletop` (or `mediaQuery { windowPosture == UiMediaScope.Posture.Tabletop }`), or `collectFoldingFeaturesAsState()` + `FoldingFeature.State.HALF_OPENED` + orientation (horizontal = tabletop, vertical = book). Use the fold `bounds` to split content.
Tabletop layouts (top = view, bottom = controls): media playback (video top / scrubber, comments bottom), video calls (participants top / mic, camera, chat bottom), camera (viewfinder top / shutter, zoom bottom), photo editing (canvas top / sliders bottom), audio mixing, cooking (recipe top / timers bottom), slideshows (slide top / notes bottom).
Book posture: two-page layout across the vertical fold. Never place buttons/text across the hinge.

---

## 20. Verify: previews, screenshot tests, build

```kotlin
@Preview(name = "Phone", device = Devices.PHONE, showBackground = true)
@Preview(name = "Foldable", device = Devices.FOLDABLE, showBackground = true)
@Preview(name = "Tablet", device = Devices.TABLET, showBackground = true)
@Preview(name = "Desktop", device = Devices.DESKTOP, showBackground = true)
annotation class FormFactorPreviews
```
- Ensure Compose Preview Screenshot Testing exists (`@PreviewTest @FormFactorPreviews`). Add it before changing UI if missing.
- After changes: build, run unit tests, run screenshot tests. **Do NOT update reference images yourself** — ask the user to review diffs and record.
- Manually test: rotate, fold/unfold, resize window, process death ("Don't keep activities"), Back + predictive back, deep link cold & warm start.

---

## 21. Workflows (follow in order)

### 21.1 New project
1. Gradle: version catalog, BOMs, plugins (Kotlin serialization, KSP for Room, Koin Compiler Plugin), buildConfig `BASE_URL`, minify on release (A1, §2, §14.1).
2. Create the A2 package structure, `App.kt`, `MainActivity.kt`, theme.
3. `ApiResponse`/`BaseResponse`/`safeApiCall`, `networkModule`, `databaseModule`, `dataStoreModule`, `StorageRepository`, `SharedViewModel` (A5, A9, §14.7).
4. `Routes.kt`, `NavigationState`, `Navigator`, `LocalNavigator`, `RootNavDisplay`, `MainNavDisplay` (A10, §4).
5. `PullToRefreshErrorOverlayWrapper` + `@FormFactorPreviews` + screenshot test setup (A8, §20).
6. Add the §23 block to the repo's `CLAUDE.md`.
7. Then features with 21.2.

### 21.2 New feature (every time, in this order)
1. **Models** — `data/model/request|response/` (`@Serializable`; `@Entity` if cached).
2. **DAO** (if cached) — add entity to `AppDatabase`, bump version + migration/cache fallback.
3. **ApiService** — `data/service/remote/`, every call via `safeApiCall`.
4. **Repository** — interface + Impl; Flow + cache-first for Room data.
5. **Use cases** — `domain/`, one per operation.
6. **ViewModel** — `XxxUiState` + `XxxViewModel` (A7) in `ui/screen/<feature>/`.
7. **Screen** — `XxxScreen` + `XxxContent` (A8); adaptive list/grid (§15); previews.
8. **Route** — `@Serializable` key in `Routes.kt` under the right sealed level.
9. **Scene metadata** — list-detail / supporting pane / dialog / sheet if applicable (§5, §6).
10. **Koin** — `di/<feature>Module.kt`: service, repo, use cases, `viewModel<T>()`, `navigation<Key>(metadata) { }`; add to `appModule.includes` (§14).
11. **Tests** — per `prostuti-android-e2e-tests` (VM with Turbine, repo with MockEngine, DAO, Compose UI).
12. **Verify** — build (no `[KOIN-…]` errors), tests, previews on 4 form factors, rotate/fold/process-death check, §22 checklist.

### 21.3 Bug fix / change in existing code
Read the surrounding feature first, keep its style, fix minimally, add/adjust a test that fails before the fix, run §22 for the touched area.

## 22. Final review checklist

Architecture
- [ ] Files in the A2 package locations; one class per file; ui → domain → data only.
- [ ] Network only via `safeApiCall` → `ApiResponse`; `CancellationException` rethrown; no secrets/tokens logged.
- [ ] ViewModel: private `MutableStateFlow` + public `StateFlow`, `update {}`, error tags as constants, one-shot flags consumed, no Context/Navigator.
- [ ] Screen = `XxxScreen` + `XxxContent`; `collectAsStateWithLifecycle`; `PullToRefreshErrorOverlayWrapper`; strings in resources; previews.
- [ ] Room schema versioned; destructive fallback only for cache tables; DataStore keys in `PrefKeys`.

Navigation · DI · Adaptive
- [ ] No `NavController`/`NavHost`/string routes; no Hilt/Dagger/KSP-for-Koin anywhere.
- [ ] Exactly two NavDisplay levels (Root + Main); login/logout replaces the root stack.
- [ ] Koin: one style per module, Compiler Plugin applied, build green with no `[KOIN-…]` errors, every feature module included in `appModule`.
- [ ] ViewModels via `viewModel<T>()`/`@KoinViewModel` + `koinViewModel()` inside entries; route passed with `@InjectedParam` + `parametersOf(route)`.
- [ ] No `KoinComponent` in normal layers; constructor injection only.
- [ ] All keys `@Serializable` + `NavKey`; args in keys.
- [ ] Back stack via `rememberNavBackStack`/`NavigationState` (`rememberSerializable` for top-level route).
- [ ] `rememberSaveableStateHolderNavEntryDecorator()` first in every decorator list.
- [ ] Per-entry ViewModels via `rememberViewModelStoreNavEntryDecorator()`.
- [ ] Clicks that navigate use `dropUnlessResumed`.
- [ ] `NavigationSuiteScaffold` with selection = `key == topLevelRoute`; visibility via state `show()/hide()`.
- [ ] Back from a tab root goes to start tab; Back from start tab exits; no Up on start destination.
- [ ] Multi-pane only via SceneStrategy; detail hides back arrow & full-screen mode in multi-pane; overlay strategies listed first.
- [ ] Deep links produce synthetic back stacks; handled in `onCreate` and `onNewIntent`.
- [ ] Every compact/medium/expanded decision uses `currentWindowAdaptiveInfoV2().windowSizeClass` + `isWidthAtLeastBreakpoint` / `isHeightAtLeastBreakpoint`; no hand-written dp breakpoints (`maxWidth < …dp`, `constraints.maxWidth.toDp() >= …dp`, `windowWidth < …dp`, `screenWidthDp`).
- [ ] Lists use `GridCells.Fixed(adaptiveColumnCount())` from `ui/component/AdaptiveColumns.kt`; Grid/FlexBox only with consent; no LazyColumn in plain `fr` track.
- [ ] Every `remember` audited; scroll states use `remember*State()`; no `configChanges` hacks.
- [ ] Previews for phone/foldable/tablet/desktop; screenshot tests run, references not auto-updated.

## 23. Enforcement: CLAUDE.md in every project

When creating a new project, or when the project has no such line yet, add this to the repo-root `CLAUDE.md`
(create it if missing) so every Claude session follows this skill:

```markdown
## Android rules
- ALWAYS load and strictly follow the `android-master` skill for every Android/Kotlin/Compose task — no exceptions.
- Stack: Compose · Navigation 3 · Koin (Compiler Plugin) · Ktor · Room · DataStore · MVVM. No Hilt, no Navigation 2, no Retrofit, no LiveData.
- Tests: follow the `prostuti-android-e2e-tests` skill.
- Before finishing, run the android-master §22 checklist and report what was verified.
```

## References
- Nav3 docs: https://developer.android.com/guide/navigation/navigation-3
- Recipes: https://github.com/android/nav3-recipes (main branch may target alpha/snapshot; check releases for stable)
- Principles: https://developer.android.com/guide/navigation/principles
- Grid / FlexBox / MediaQuery: https://developer.android.com/develop/ui/compose/layouts/adaptive/grid · …/flexbox · …/mediaquery
- Foldables: https://developer.android.com/develop/ui/compose/layouts/adaptive/foldables/make-your-app-fold-aware
- Koin Navigation 3: https://insert-koin.io/docs/reference/koin-compose/navigation3
- Koin skills: https://github.com/InsertKoinIO/koin-skills · nav3 recipe `modular/koin`