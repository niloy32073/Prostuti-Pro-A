> **স্ট্যাটাস (৩০ অক্টো ২০২৬): P0–P5.5 ইমপ্লিমেন্ট করা হয়েছে।** `./gradlew :app:assembleDebug` ও `testDebugUnitTest` পাস করে। P7 (ট্যাবলেট লেআউট) ও P6-এর Compose UI/androidTest বাদ আছে — নিচে বিস্তারিত।

# গ্রুপ এক্সাম + লাইভ এক্সাম — ইমপ্লিমেন্টেশন প্ল্যান (ডামি ডাটা)

ডিজাইন সোর্স: `প্রস্তুতি — স্ক্রিন ডিজাইন` ক্যানভাস (Group* ৮টি + Live* ৪টি আর্টবোর্ড, সাথে Home ও Routine)।
লক্ষ্য: ব্যাকএন্ড ছাড়াই পুরো ফ্লো অ্যাপে চালানো যাবে। পরে আসল API এলে শুধু DI-তে Fake রিপোজিটরি বদলে Real রিপোজিটরি বসালেই চলবে — স্ক্রিন/ViewModel বদলাতে হবে না।

---

## ১. ডিজাইন থেকে যা পাওয়া গেল (স্ক্রিন ম্যাপ)

### গ্রুপ এক্সাম
| ডিজাইন আর্টবোর্ড | অ্যাপ স্ক্রিন | কে দেখবে |
|---|---|---|
| GroupList — "আমার গ্রুপ" | `GroupListScreen` | সবাই |
| GroupDetail — রুটিন লিংক, এক্সাম স্ট্যাটাস কার্ড, আগের এক্সাম, সদস্য | `GroupDetailScreen` | সবাই (কার্ড রোল অনুযায়ী বদলায়) |
| GroupSubjectSelect — ধাপ ১/৩ | `GroupRoutineSubjectScreen` | **শুধু অ্যাডমিন** |
| GroupChapterSelect — ধাপ ২/৩ (ট্রাই-স্টেট চেকবক্স) | `GroupRoutineChapterScreen` | **শুধু অ্যাডমিন** |
| GroupExamWindow — ধাপ ৩/৩, উইন্ডো সময় + নিয়ম + লক | `GroupRoutineWindowScreen` | **শুধু অ্যাডমিন** |
| GroupRoutineCreate — এক পেজে বিষয়/দিন/কনফিগ | (প্রশ্ন ১ দেখো — ৩-ধাপের সাথে মেলানো হবে) | **শুধু অ্যাডমিন** |
| MCQExam (GroupDetail থেকে লিংক) | `TimedExamAttemptScreen` (লাইভের সাথে শেয়ার্ড) | সবাই |
| GroupResultLocked — সাবমিটের পর, উইন্ডো চলাকালীন | `LockedResultScreen` (শেয়ার্ড) | সবাই |
| GroupRanking — উইন্ডো শেষে | `WindowRankingScreen` (শেয়ার্ড) | সবাই |
| AnswerSheet (র‍্যাংকিং-এ নিজের রো ট্যাপ করে) | `TimedAnswerSheetScreen` (শেয়ার্ড) | সবাই, শুধু নিজের শিট |

### লাইভ এক্সাম
| ডিজাইন আর্টবোর্ড | অ্যাপ স্ক্রিন |
|---|---|
| LiveExamDetail — ৪টি অবস্থা (upcoming / open / submitted / closed), অ্যাডমিন-প্যানেল থেকে লকড কনফিগ | `LiveExamDetailScreen` |
| LiveMCQExam — এক কলাম স্ক্রল, "১বারই সুযোগ" ব্যানার, টাইমার, প্রোগ্রেস | `TimedExamAttemptScreen` |
| LiveResultLocked — সঠিক সংখ্যা + সময়, মার্ক/র‍্যাংক লকড | `LockedResultScreen` |
| LiveRanking — পুরো স্ট্রিম, হিস্টোগ্রাম, টপ র‍্যাংক, "আশেপাশে দেখো" | `WindowRankingScreen` |

### এন্ট্রি পয়েন্ট (Home)
- কুইক মেনু "গ্রুপ" → `GroupList`
- আপকামিং কার্ড "লাইভ এক্সাম · আগামীকাল ৮:০০ PM" → `LiveExamList` (একাধিক লাইভ এক্সাম থাকলে তালিকা; একটাই থাকলেও তালিকা স্ক্রিনই খুলবে, সামঞ্জস্যের জন্য) → ট্যাপে `LiveExamDetail(liveExamId)`
- আপকামিং কার্ড "গ্রুপ এক্সাম · ইনভাইট" → `GroupDetail(groupId)`

### অবস্থা (state) — ডিজাইনের "ডেমো: অবস্থা পাল্টাও" বাটন থেকে
- **গ্রুপ:** `NO_ROUTINE` (অ্যাডমিন → "গ্রুপ রুটিন তৈরি করুন" CTA, সদস্য → "অ্যাডমিন এখনও রুটিন সেট করেনি") → `OPEN_NOT_SUBMITTED` ("এক্সামে অংশ নিন") → `OPEN_SUBMITTED` ("তুমি সাবমিট করেছো — দেখো") → `CLOSED` ("র‍্যাংকিং ও অ্যান্সার শিট দেখো")
- **লাইভ:** `UPCOMING` (শুরু বাটন ডিজেবল্ড) → `OPEN_NOT_SUBMITTED` → `OPEN_SUBMITTED` → `CLOSED`

---

## ২. আর্কিটেকচার (prostuti-android-arch অনুযায়ী)

### মূল সিদ্ধান্ত: বিদ্যমান Exam Engine ছোঁয়া হবে না
`ExamAttemptViewModel` প্রতিটা উত্তর বাছাই ও সাবমিটে আসল API কল করে (`SubmitExamResponsesUseCase`, `SubmitExamAttemptUseCase`)। ডামি attemptId দিলে সব কল ফেইল করবে। তাই:
- প্র্যাকটিস এক্সামের কাজ করা ইঞ্জিন যেমন আছে তেমনই থাকবে।
- লাইভ ও গ্রুপের জন্য নতুন `TimedExamAttemptScreen` + `TimedExamAttemptViewModel`, যেটা Fake রিপোজিটরি থেকে প্রশ্ন নেয়, উত্তর মেমরিতে রাখে, সাবমিট করে।
- UI রিইউজ: প্রশ্ন কার্ডে বিদ্যমান `McqCard` (`Question` মডেল নেয় — ডামি `Question` বানিয়ে দেওয়া যাবে)। অ্যান্সার শিটে `ReadOnlyMcqCard`-এর প্যাটার্ন।

### Fake ডাটা লেয়ার — Room-ব্যাকড (প্রশ্ন ১১: রুটিন/সাবমিশন অ্যাপ বন্ধেও থাকবে)
সাবমিশন ও অ্যাডমিনের তৈরি রুটিন Room-এ সেভ হবে, যাতে অ্যাপ বন্ধ-খোলা করলেও হারিয়ে না যায় (সত্যিকার ইউজারের কাছে এটাই আসল API-র মতো লাগবে)। শুধু *স্ট্যাটিক* ডামি কনটেন্ট (গ্রুপের তালিকা, সদস্য, প্রশ্ন, র‍্যাংকিং) কোডে হার্ডকোড — ওগুলো বদলায় না।
```
data/
├── model/enums/
│   ├── GroupRole.kt                 // ADMIN, MEMBER
│   ├── GroupExamState.kt            // NO_ROUTINE, OPEN_NOT_SUBMITTED, OPEN_SUBMITTED, CLOSED
│   └── LiveExamState.kt             // UPCOMING, OPEN_NOT_SUBMITTED, OPEN_SUBMITTED, CLOSED
├── model/response/
│   ├── Group.kt                     // Group, GroupMember, GroupPastExam
│   ├── GroupRoutine.kt              // GroupRoutine, ExamRules   (@Entity — Room-এ সেভ)
│   ├── TimedExam.kt                 // TimedExam (live+group common), ExamWindow, MySubmission
│   └── WindowRanking.kt             // RankingSummary, RankRow, histogram
├── model/request/
│   └── CreateGroupRoutineRequest.kt
├── service/local/
│   ├── GroupRoutineEntity.kt        // @Entity — গ্রুপ-ভিত্তিক তৈরি হওয়া রুটিন (subjectIds/chapterIds/topicIds JSON, window, qCount, mark, neg, durationMin)
│   ├── TimedExamSubmissionEntity.kt // @Entity — examId + kind(LIVE/GROUP) + answers(JSON) + submittedAt + correctCount + marks (primary key: examId+userId সিমুলেটেড)
│   ├── CreatedGroupEntity.kt        // @Entity — + বাটনে তৈরি/জয়েন করা গ্রুপ (ডামি-৪টার বাইরে)
│   ├── ExamDao.kt                   // routine/submission/group-এর জন্য তিনটা @Dao, একই ফাইলে বা আলাদা
│   └── (AppDatabase-এ নতুন এন্টিটি + DAO যোগ, migration: fallbackToDestructiveMigration থাকায় আলাদা Migration লাগবে না)
├── repository/
│   ├── GroupExamRepository.kt       // interface
│   ├── LiveExamRepository.kt        // interface
│   └── fake/
│       ├── FakeGroupExamRepository.kt   // স্ট্যাটিক ডামি + ExamDao (Room) মিলিয়ে, delay(400) নেটওয়ার্ক সিমুলেশন
│       ├── FakeLiveExamRepository.kt
│       └── ExamDummyData.kt             // সব ডামি গ্রুপ/সদস্য/প্রশ্ন/র‍্যাংকিং এক জায়গায় (স্ট্যাটিক, Room-ে যায় না)
└── util/
    └── TimeProvider.kt              // now() — টেস্টে ও ডেমোতে সময় নিয়ন্ত্রণের জন্য
```
- সব মডেল `@Serializable` + `@SerialName` সহ, Room-ব্যাকড মডেলে `@Entity`/`@ColumnInfo` দ্বৈত-অ্যানোটেশন (আর্কিটেকচার স্কিলের প্যাটার্ন #12) — আসল API এলে সরাসরি কাজে লাগে।
- রিপোজিটরি `ApiResponse<T>` রিটার্ন করবে (আসল API-র মতোই) — ViewModel-এ এরর হ্যান্ডলিং একই প্যাটার্নে।
- অবস্থা (state) কখনও আলাদা স্টোর হবে না, সবসময় হিসাব হবে: `deriveGroupExamState(routine, exam, mySubmission, now)` — একটা pure ফাংশন, ইউনিট টেস্ট করা সহজ। `routine`/`mySubmission` আসে Room থেকে, বাকি স্ট্যাটিক ডামি থেকে।
- উইন্ডোর সময় অ্যাপ প্রথম চালুর সময় `now` থেকে আপেক্ষিকভাবে বসানো হবে (যেমন HSC ব্যাচ ২৭-এর উইন্ডো `now − 1h` থেকে `now + 3h`), এবং সেই অ্যাবসোলিউট টাইমস্ট্যাম্পই Room-এ সেভ হবে — যাতে অ্যাপ বন্ধ করে পরদিন খুললেও উইন্ডো ঠিক নিয়মেই বন্ধ হয়ে যায় (ডেমো শুধু প্রথমবার আপেক্ষিক, তারপর বাস্তব)।

### Use case (domain/)
`FetchMyGroupsUseCase`, `FetchGroupDetailUseCase`, `CreateGroupRoutineUseCase`, `FetchLiveExamListUseCase`, `FetchTimedExamUseCase`, `StartTimedExamUseCase`, `SaveInProgressAnswersUseCase` (প্রতি উত্তরে Room-এ আপসার্ট — প্রশ্ন ১২-এর অটো-সাবমিটের ভিত্তি), `SubmitTimedExamUseCase`, `FetchMySubmissionUseCase`, `FetchWindowRankingUseCase`, `FetchMyAnswerSheetUseCase` (শুধু নিজের), `FetchLiveExamUseCase`, `CreateGroupUseCase`, `JoinGroupUseCase`

### DI (appModule.kt)
```kotlin
single<TimeProvider> { SystemTimeProvider() }
single<ExamDao> { get<AppDatabase>().examDao() }                 // নতুন DAO, AppDatabase-এ entities যোগ
single<GroupExamRepository> { FakeGroupExamRepository(get(), get()) }   // পরে: GroupExamRepositoryImpl(get())
single<LiveExamRepository>  { FakeLiveExamRepository(get(), get()) }
factory { ... use cases ... }
viewModel { params -> GroupDetailViewModel(params.get(), get(), get()) }
...
```

---

## ৩. অ্যাডমিন-ভিত্তিক রাউটিং (প্রতি গ্রুপ আলাদা)

রোল ইউজারের উপর নয়, **গ্রুপ মেম্বারশিপের উপর**: `Group.myRole: GroupRole`। একই ইউজার HSC ব্যাচ ২৭-এ অ্যাডমিন, রসায়ন সার্কেলে সাধারণ সদস্য।

তিন স্তরে চেক:
1. **UI স্তর** — `GroupDetailScreen` শুধু `myRole == ADMIN` হলে "গ্রুপ রুটিন তৈরি করুন" CTA দেখায়; সদস্য দেখে অপেক্ষার মেসেজ। ডিজাইনের "সাধারণ সদস্য ভিউ / অ্যাডমিন ভিউ" টগল অ্যাপে **থাকবে না** — রোল ডাটা থেকে আসবে।
2. **রাউট স্তর** — অ্যাডমিন রাউটগুলো একটা মার্কার ইন্টারফেস ইমপ্লিমেন্ট করবে:
   ```kotlin
   @Serializable sealed interface GroupAdminRoute : RootRoute { val groupId: String }
   @Serializable data class GroupRoutineSubject(override val groupId: String) : GroupAdminRoute
   @Serializable data class GroupRoutineChapter(override val groupId: String, val subjectIds: List<String>) : GroupAdminRoute
   @Serializable data class GroupRoutineWindow(override val groupId: String, val subjectIds: List<String>,
                                               val chapterIds: List<String>, val topicIds: List<String>) : GroupAdminRoute
   ```
   NavDisplay-তে এগুলো একটা `AdminGate(groupId) { ... }` composable দিয়ে মোড়ানো — রোল লোড হয়, অ্যাডমিন না হলে "এই পেজ শুধু গ্রুপ অ্যাডমিনের জন্য" দেখিয়ে back করে দেয়। রুটিন আগেই তৈরি থাকলে রুটিন-তৈরি রাউটেও ঢুকতে দেয় না (লকড)।
3. **ডাটা স্তর** — `FakeGroupExamRepository.createRoutine()` অ্যাডমিন না হলে `ApiResponse.Failure(errorCode = "FORBIDDEN")` দেয়, রুটিন থাকলে `"ROUTINE_LOCKED"` — আসল ব্যাকএন্ডের আচরণ নকল করে।

### সব রাউট (Routes.kt-এ যোগ হবে, root-level — বটম ন্যাভ ছাড়া ফুল-স্ক্রিন, Practice-এর মতো)
```kotlin
// Group
@Serializable data object GroupList : RootRoute
@Serializable data class GroupDetail(val groupId: String) : RootRoute
// + উপরের তিনটা GroupAdminRoute
// Shared timed exam (live + group)
@Serializable data class TimedExamAttempt(val examId: String, val kind: TimedExamKind) : RootRoute
@Serializable data class LockedResult(val examId: String, val kind: TimedExamKind) : RootRoute
@Serializable data class WindowRanking(val examId: String, val kind: TimedExamKind) : RootRoute
@Serializable data class TimedAnswerSheet(val examId: String) : RootRoute   // শুধু নিজের শিট — participantId লাগে না (প্রশ্ন ৫)
// Live
@Serializable data object LiveExamList : RootRoute
@Serializable data class LiveExamDetail(val liveExamId: String) : RootRoute
```
`TimedExamKind { LIVE, GROUP }` — টেক্সট/ব্যাক-ডেস্টিনেশন বদলায় (যেমন "গ্রুপ ডিটেইলে ফিরে যাও" বনাম "এক্সাম ডিটেইলে ফিরে যাও")।

### ফ্লো ডায়াগ্রাম
```
Home ─┬─ কুইক মেনু "গ্রুপ" ─→ GroupList ─→ GroupDetail(id)
      │                                    ├─ [ADMIN + NO_ROUTINE] → Subject → Chapter → Window ─(লক)→ GroupDetail
      │                                    ├─ [OPEN_NOT_SUBMITTED] → TimedExamAttempt ─(সাবমিট)→ LockedResult
      │                                    ├─ [OPEN_SUBMITTED] → LockedResult
      │                                    ├─ [CLOSED] / আগের এক্সাম → WindowRanking → TimedAnswerSheet (শুধু নিজের)
      │                                    ├─ "রুটিন ক্যালেন্ডার দেখো" → GroupRoutineCalendar(id)
      │                                    └─ "সবাই দেখো" → GroupPastExams(id) / GroupMembers(id)
      │                        GroupList + বাটন → তৈরি (তুমি ADMIN) / কোডে জয়েন (তুমি MEMBER) → GroupDetail
      └─ লাইভ কার্ড ─→ LiveExamList ─→ LiveExamDetail(id)
                        ├─ [UPCOMING] শুরু বাটন ডিজেবল্ড
                        ├─ [OPEN_NOT_SUBMITTED] → TimedExamAttempt ─(সাবমিট)→ LockedResult
                        ├─ [OPEN_SUBMITTED] → LockedResult
                        └─ [CLOSED] → WindowRanking → TimedAnswerSheet (শুধু নিজের)
```
এক্সাম সাবমিটের পর `TimedExamAttempt` ব্যাকস্ট্যাক থেকে সরিয়ে `LockedResult` বসানো হবে — back চাপলে আর এক্সামে ফেরা যাবে না ("১বারই সুযোগ")। এক্সাম চলাকালীন অ্যাপ বন্ধ করে ফিরলে (প্রশ্ন ১২) `TimedExamAttemptViewModel`-এর `init{}`-এ Room-এ এই examId-এর জন্য ইন-প্রগ্রেস রেকর্ড পেলে, যতটুকু উত্তর সেভ ছিল তা নিয়েই সাথে সাথে অটো-সাবমিট করে সরাসরি `LockedResult`-এ পাঠাবে (এক্সাম স্ক্রিন দেখাবেই না) — ঠিক টাইমার-শেষের অটো-সাবমিটের মতো আচরণ।

---

## ৪. ডামি ডাটা (ExamDummyData.kt)

ডিজাইনের টেক্সট হুবহু ব্যবহার হবে।

**গ্রুপ (৪টি — প্রতিটা আলাদা অবস্থা দেখায়, ডেমো টগল ছাড়াই পুরো ফ্লো ঘোরা যায়):**
| id | নাম | সদস্য | আমার রোল | অবস্থা |
|---|---|---|---|---|
| `g_hsc27` | HSC ব্যাচ ২৭ | ৩৮ | ADMIN | রুটিন আছে, উইন্ডো খোলা আরও ৩ ঘণ্টা, সাবমিট করিনি |
| `g_chem` | রসায়ন স্টাডি সার্কেল | ১২ | MEMBER | রুটিন নেই → অপেক্ষার মেসেজ |
| `g_dhaka` | ঢাকা কলেজ — একদল বন্ধু | ৬ | MEMBER | উইন্ডো শেষ → র‍্যাংকিং |
| `g_new` | ফিজিক্স ক্র্যাক টিম *(ডিজাইনে নেই, প্রস্তাবিত)* | ৪ | ADMIN | রুটিন নেই → অ্যাডমিন রুটিন-তৈরি ফ্লো |

- সদস্য: সাদিয়া ইসলাম, রাফি হাসান (অ্যাডমিন), ইমন চৌধুরী, তানিয়া আক্তার, মাহির রহমান, নিফা রহমান (তুমি) + বাকি জেনারেটেড নাম।
- আগের এক্সাম: রসায়ন ১ম পত্র (১২ অক্টো, ৩৬ জন, ৭২%), উচ্চতর গণিত ১ম পত্র (৫ অক্টো, ৩৩ জন, ৬৫%), পদার্থবিজ্ঞান ২য় পত্র (২৮ সেপ্টে, ৩৮ জন, ৫৮%)।
- সাবজেক্ট/অধ্যায়/টপিক: পদার্থ ১ম/২য়, রসায়ন ১ম/২য়, জীববিজ্ঞান ১ম, উচ্চতর গণিত ১ম — "ভৌত জগৎ ও পরিমাপ", "ভেক্টর" (৪ টপিক), "নিউটনিয়ান বলবিদ্যা" ইত্যাদি। *(অথবা আসল `FetchSubjectsUseCase`/`FetchChaptersUseCase` — প্রশ্ন ৯)*
- রুটিন ডিফল্ট: ২৫ প্রশ্ন, প্রতি প্রশ্নে ১, নেগেটিভ ০.২৫, উইন্ডো সন্ধ্যা ৭:০০–রাত ১১:০০।

**লাইভ এক্সাম (৩টি — প্রশ্ন ১৪: Home থেকে `LiveExamList`-এ যায়):**
| id | নাম | অবস্থা | উইন্ডো |
|---|---|---|---|
| `live_phy_mt04` | পদার্থবিজ্ঞান — মডেল টেস্ট ০৪ | OPEN_NOT_SUBMITTED | `now − 10m` থেকে ২ ঘণ্টা |
| `live_chem_mt03` | রসায়ন — মডেল টেস্ট ০৩ | UPCOMING | `now + 5m` থেকে ২ ঘণ্টা (একটু পরে নিজে থেকে OPEN হয়, টিকার দিয়ে) |
| `live_math_mt02` | উচ্চতর গণিত — মডেল টেস্ট ০২ | CLOSED | `now − 2d` |

সবগুলো: সম্পূর্ণ স্ট্রিম, ২৫ প্রশ্ন, ১ / −০.২৫, ১বারই সুযোগ, সময়সীমা ৩০ মিনিট। `live_phy_mt04`-এর র‍্যাংকিং: ২৪১০ জন, গড় ৬৪%, তোমার র‍্যাংক #১৪২ (CLOSED হওয়ার পর)।

**প্রশ্ন:** ২৫টি পদার্থবিজ্ঞান MCQ (ডিজাইনের ৬টি + ১৯টি নতুন), প্রতিটায় সঠিক উত্তর ও ব্যাখ্যা। অপশন লেবেল ক/খ/গ/ঘ। একই সেট তিনটা লাইভ এক্সাম ও গ্রুপ এক্সামে পুনর্ব্যবহার হবে (ডামি ডাটায় সমস্যা নেই)।

**ডেমো কন্ট্রোল (শুধু `BuildConfig.DEBUG`):** GroupDetail ও LiveExamDetail-এ ছোট "ডেমো: অবস্থা পাল্টাও" চিপ — Fake রিপোজিটরিতে ঐ এক্সামের উইন্ডো-টাইম শিফট করে পরের অবস্থায় নিয়ে যায় (অ্যাবসোলিউট টাইমস্ট্যাম্প Room-এ আপডেট হয়)। রিলিজ বিল্ডে দেখাবে না।

**পার্সিস্টেন্স (প্রশ্ন ১১):** রুটিন তৈরি, এক্সাম সাবমিশন, নতুন গ্রুপ তৈরি/জয়েন — সব Room-এ সেভ থাকবে, অ্যাপ বন্ধ করে খুললেও (এমনকি দিন পরেও) ঠিক সেই অবস্থাতেই দেখাবে, আসল API-র মতোই আচরণ করবে।

---

## ৫. স্ক্রিন-বাই-স্ক্রিন বিস্তারিত

**GroupListScreen** — হেডার "আমার গ্রুপ / Nটি গ্রুপে আছো", + বাটন (প্রশ্ন ৭)। প্রতি কার্ডে আদ্যক্ষর-ব্যাজ, নাম, "N সদস্য · তুমি অ্যাডমিন/সাধারণ সদস্য", ডানপাশে স্ট্যাটাস চিপ (খোলা উইন্ডো হলে লাল ব্যানার "… উইন্ডো খোলা আছে আরও X", নেই হলে "কোনো চলমান এক্সাম নেই", শেষ হলে "সম্পন্ন")। লোডিং/খালি অবস্থা।

**GroupDetailScreen** — রুটিন ক্যালেন্ডার লিংক, এক্সাম স্ট্যাটাস কার্ড (৫ রকম: অ্যাডমিন-NO_ROUTINE, সদস্য-NO_ROUTINE, OPEN_NOT_SUBMITTED, OPEN_SUBMITTED, CLOSED), "আগের এক্সামগুলো" (ট্যাপ → র‍্যাংকিং), "সদস্যরা (N)" প্রথম ৪ জন + অ্যাডমিন ব্যাজ। কাউন্টডাউন প্রতি মিনিটে আপডেট।

**রুটিন তৈরি (অ্যাডমিন, ৩ ধাপ)** — বিদ্যমান `PracticeSelection` ট্রাই-স্টেট লজিক রিইউজ।
- ধাপ ১: সাবজেক্ট মাল্টি-সিলেক্ট, নিচে "Nটি সাবজেক্ট নির্বাচিত · পরবর্তী" (০টি হলে ডিজেবল্ড)।
- ধাপ ২: সাবজেক্ট-ভিত্তিক অধ্যায় → টপিক ট্রি, "পুরো বই" শর্টকাট, "+ আরও Nটি অধ্যায় দেখাও", নিচে "Nটি অধ্যায়, Nটি টপিক নির্বাচিত"।
- ধাপ ৩: সতর্কবার্তা (সেভের পর পরিবর্তন নয়), উইন্ডো শুরু/শেষ টাইম পিকার, প্রশ্ন সংখ্যা (±৫, কমপক্ষে ৫), প্রতি প্রশ্নের মান (±০.২৫), নেগেটিভ টগল + মান, মোট মার্ক লাইভ হিসাব। "রুটিন সেভ করো ও সক্রিয় করো" → কনফার্ম ডায়ালগ → লকড অবস্থা → "গ্রুপ ডিটেইলে যাও" (ব্যাকস্ট্যাক GroupDetail পর্যন্ত পরিষ্কার)।

**TimedExamAttemptScreen** — উপরে "N/25 উত্তর দেওয়া হয়েছে" + টাইমার + প্রোগ্রেস বার, "১বারই সুযোগ — সাবমিটের পর ফেরা যাবে না" ব্যানার, এক কলামে সব প্রশ্ন (`McqCard`), নিচে "এক্সাম সাবমিট করো"। ক্যালকুলেটর/স্ক্র্যাচ-ক্যানভাস বাটন থাকবে, ট্যাপে "শীঘ্রই আসছে" স্ন্যাকবার (প্রশ্ন ১৩ — ফাংশনাল ক্যালকুলেটর এই রাউন্ডে নেই)। প্রতিটা উত্তর বাছাইয়ে `SaveInProgressAnswersUseCase` Room-এ আপসার্ট করে (ডিবাউন্স করে, প্রতি কিস্ট্রোকে না)। সাবমিট কনফার্ম ডায়ালগে উত্তর-না-দেওয়া প্রশ্নের সংখ্যা। টাইমার শেষ হলে অটো-সাবমিট। back চাপলে "এক্সাম ছেড়ে যাবে?" ডায়ালগ — কিন্তু অ্যাপ nav-এর বাইরে (হোম বাটন/টাস্ক সুইচ/ক্র্যাশ) বন্ধ হয়ে ফিরলে প্রশ্ন ১২ অনুযায়ী Room-এর সেভ করা উত্তর দিয়েই সাথে সাথে অটো-সাবমিট হয়ে যাবে, স্ক্রিন খোলেই না।

**LockedResultScreen** — "সাবমিট হয়েছে ✅" + সাবমিট করার সময়, "উইন্ডো শেষ হবে আরও X পর — তারপর ফলাফল আনলক হবে"। **প্রশ্ন ৪ অনুযায়ী সঠিক উত্তরের সংখ্যাও দেখাবে না** — সঠিক/মার্ক/র‍্যাংক সবই "উইন্ডো শেষে দেখা যাবে" লেখা লকড রো; শুধু "সময় লেগেছে" (যেটা ফলাফল প্রকাশ করে না) দেখাবে। ডিজেবল্ড "অ্যান্সার শিট (লকড)", ফিরে যাওয়ার বাটন। উইন্ডো শেষ হলে স্ক্রিন নিজেই র‍্যাংকিং বাটন দেখায়।

**WindowRankingScreen** — ৩টি স্ট্যাট (সাবমিট করেছে, গড় স্কোর, তোমার র‍্যাংক), স্কোর বিস্তার হিস্টোগ্রাম (Canvas দিয়ে বার, তোমার বার হাইলাইট), র‍্যাংক তালিকা (টপ ৩ লাল, নিজের রো হাইলাইট)। **প্রশ্ন ৫ অনুযায়ী শুধু নিজের রো ট্যাপযোগ্য** (→ `TimedAnswerSheet`), অন্য সবার রো প্লেইন টেক্সট — "সঠিক/ভুল দেখতে ট্যাপ করো" হিন্টটা শুধু নিজের রো-র নিচে দেখাবে। লাইভে "#১৪২-এ তোমার আশেপাশে দেখো" — তালিকা নিজের র‍্যাংকে স্ক্রল/লোড করে।

**TimedAnswerSheetScreen** — শুধু নিজের শিট (participantId নেয় না)। প্রতি প্রশ্নে নিজের উত্তর, সঠিক উত্তর, ব্যাখ্যা; উপরে সঠিক/ভুল/উত্তর-দেওয়া-হয়নি সামারি, চূড়ান্ত মার্ক (নেগেটিভ সহ)।

**LiveExamListScreen** *(নতুন, প্রশ্ন ১৪)* — "লাইভ এক্সাম" হেডার, প্রতি কার্ডে নাম, অবস্থা চিপ (আসন্ন/খোলা/সাবমিট করেছো/শেষ), উইন্ডো সময়; ট্যাপ → `LiveExamDetail`। খালি অবস্থা "এখন কোনো লাইভ এক্সাম নেই"।

**LiveExamDetailScreen** — শিরোনাম, "লাইভ এক্সাম · সম্পূর্ণ স্ট্রিম একসাথে", লক নোট (অ্যাডমিন প্যানেল থেকে কনফিগার), ইনফো টেবিল (উইন্ডো শুরু/শেষ, প্রশ্ন, মার্কিং, অংশগ্রহণ), সতর্কবার্তা, অবস্থা-অনুযায়ী নিচের বাটন।

সব স্ট্রিং `res/values/strings.xml` (বাংলা) + `values-en/strings.xml`-এ — HomeViewModel-এর মতো। ডামি কনটেন্ট (নাম, প্রশ্ন) `ExamDummyData.kt`-এ হার্ডকোড, `QBDummyData.kt`-এর মতো।

---

## ৬. কাজের ধাপ (ক্রম অনুযায়ী)

| ফেজ | কাজ | হয়ে গেলে যা দেখা যাবে |
|---|---|---|
| **P0 — ভিত্তি** | enums, মডেল, Room entities + DAO + AppDatabase আপডেট, `TimeProvider`, দুটো রিপোজিটরি ইন্টারফেস + Fake impl (Room+স্ট্যাটিক মিলিয়ে), `ExamDummyData`, state-derivation ফাংশন, use cases, DI | ইউনিট টেস্ট পাস (state derivation, অ্যাডমিন-চেক, মার্ক হিসাব নেগেটিভ সহ), Room schema কম্পাইল হয় |
| **P1 — নেভিগেশন** | Routes.kt, RootNavDisplay entry, `AdminGate`, Home কুইক মেনু + আপকামিং কার্ড ওয়্যারিং (`MainNavDisplay`-এ নতুন কলব্যাক) | Home থেকে খালি স্ক্রিনগুলোতে যাওয়া যায়, নন-অ্যাডমিন অ্যাডমিন রাউটে আটকায় |
| **P2 — গ্রুপ লিস্ট + ডিটেইল** | দুটো স্ক্রিন + ViewModel, সব ৫ রকম কার্ড | ৪টা গ্রুপে ঢুকে চার রকম অবস্থা দেখা যায় |
| **P3 — রুটিন তৈরি (অ্যাডমিন)** | ৩ ধাপ, লক, Room-এ সেভ | `g_new`-এ রুটিন বানালে GroupDetail OPEN দেখায়, অ্যাপ রিস্টার্টেও থাকে |
| **P4 — শেয়ার্ড এক্সাম অংশ** | Attempt (ইন-প্রগ্রেস Room সেভ + অটো-সাবমিট), LockedResult (সবকিছু লকড), Ranking (শুধু নিজের রো ক্লিকেবল), AnswerSheet (শুধু নিজের) | HSC ব্যাচ ২৭-এ এক্সাম দিয়ে সাবমিট → সবকিছু লকড দেখা যায় → (ডেমো চিপ) → র‍্যাংকিং → নিজের অ্যান্সার শিট; মাঝপথে অ্যাপ বন্ধ করে ফিরলে অটো-সাবমিট হয় |
| **P5 — লাইভ** | LiveExamList (৩টা কার্ড), LiveExamDetail, শেয়ার্ড স্ক্রিনে `LIVE` টেক্সট | Home → তালিকা → পুরো ফ্লো, তিনটা ভিন্ন অবস্থা |
| **P5.5 — ডিজাইন-ছাড়া অংশ** | গ্রুপ তৈরি/জয়েন, গ্রুপ রুটিন ক্যালেন্ডার, দুটো "সবাই দেখো" লিস্ট (নিচে সেকশন ৮) | + বাটনে নতুন গ্রুপ বানালে তুমি অ্যাডমিন; কোড দিয়ে জয়েন করলে সদস্য; ক্যালেন্ডারে এক্সাম-দিন দেখা যায় |
| **P6 — পালিশ ও টেস্ট** | ডেমো চিপ (DEBUG), en স্ট্রিং, ViewModel টেস্ট (Turbine), Compose UI টেস্ট: অ্যাডমিন বনাম সদস্য GroupDetail, AdminGate রিডাইরেক্ট, এক্সাম back-press, অ্যাপ-বন্ধ-মাঝপথে অটো-সাবমিট (process-death সিমুলেশন) | `./gradlew test connectedAndroidTest` পাস |
| **P7 — অ্যাডাপ্টিভ** | Medium/Expanded (প্রশ্ন ১০) | ট্যাবলেটে ঠিক দেখায় |

নতুন ফাইল আনুমানিক ৪৫টি (Room entity/DAO + LiveExamList যোগ হওয়ায়); বিদ্যমান ফাইলে বদল: `Routes.kt`, `RootNavDisplay.kt`, `MainNavDisplay.kt`, `HomeScreen.kt`, `HomeViewModel.kt` (কার্ডে id যোগ), `AppDatabase.kt`, `appModule.kt`, `strings.xml` ×২। প্র্যাকটিস এক্সাম ইঞ্জিন অপরিবর্তিত।

---

## ৭. প্রশ্নোত্তর (ডিজাইন দেখে) — সবগুলোর উত্তর পাওয়া গেছে (৩০ অক্টো ২০২৬)

1. **রুটিন তৈরির ফ্লো** → ৩-ধাপ `Subject → Chapter → Window`, আর ধাপ ৩-এ "কত দিনের রুটিন" + "সপ্তাহের কোন দিন" + "সময়সীমা (মিনিট)" যোগ। এক-পেজের `GroupRoutineCreate` ডিজাইন বানানো হবে না।
2. **টাইমার কিসের ভিত্তিতে** → নির্দিষ্ট সময়সীমা (লাইভ: অ্যাডমিন প্যানেল থেকে প্রি-সেট, গ্রুপ: রুটিনের ধাপ ৩-এ অ্যাডমিন সেট করে), উইন্ডো শেষের বেশি কখনও যাবে না।
3. **গ্রুপ এক্সাম কোন স্ক্রিনে** → লাইভের মতোই এক-সুযোগের `TimedExamAttemptScreen`-এ, প্র্যাকটিসের MCQExam-এ নয়।
4. **লকড রেজাল্টে সঠিক সংখ্যা** → **সবই লকড** — সাবমিটের পর "সাবমিট হয়েছে ✅" ছাড়া কোনো স্কোর/সঠিক-সংখ্যা/মার্ক/র‍্যাংক দেখাবে না, উইন্ডো শেষ হওয়া পর্যন্ত। (ডিজাইনের "১৮/২৫ সঠিক উত্তর" লাইনটা বানাবো না।)
5. **উইন্ডো শেষের পর অন্যের অ্যান্সার শিট** → **না, শুধু নিজের।** র‍্যাংকিং-এ কেবল নিজের রো ট্যাপ করে নিজের অ্যান্সার শিট দেখা যাবে; অন্যদের রো প্লেইন (ক্লিকেবল নয়)।
6. **অ্যাডমিনের নিয়ম** → একাধিক অ্যাডমিন সম্ভব, অ্যাডমিনও নিজে এক্সাম দেয় ও র‍্যাংকিংয়ে থাকে (দুটোই হ্যাঁ)। রুটিনের মেয়াদ শেষ হলে নতুন রুটিন বানানোর CTA আবার দেখাবে (এই অংশে অতিরিক্ত প্রশ্ন ছিল না, ডিফল্ট রাখা হলো)।
7. **ডিজাইন নেই এমন অংশ (গ্রুপ তৈরি/জয়েন, রুটিন ক্যালেন্ডার, "সবাই দেখো" লিস্ট)** → **এই রাউন্ডেই থাকবে** (বিস্তারিত সেকশন ৮-এ)। ইনভাইট অ্যাকসেপ্ট **থাকবে না** — Home-এর গ্রুপ কার্ড সরাসরি GroupDetail খুলবে। সদস্য ম্যানেজ (অ্যাডমিন বানানো/বাদ দেওয়া) এই রাউন্ডে নেই।
8. **গ্রুপ রুটিন ক্যালেন্ডার** → এই রাউন্ডেই থাকবে, `GroupRoutineCalendar(groupId)` আলাদা স্ক্রিন (সেকশন ৮)। ব্যক্তিগত Routine ট্যাব এখনও ComingSoon।
9. **সাবজেক্ট/অধ্যায়/টপিক সোর্স** → আসল `FetchSubjects/Chapters/TopicsUseCase` (Practice-এ যেমন কাজ করে) — শুধু গ্রুপ/এক্সাম-নির্দিষ্ট অংশ (রুটিন, সাবমিশন, র‍্যাংকিং) ডামি/Room।
10. **ট্যাবলেট লেআউট** → Group/Live-এর জন্য আলাদা Medium/Expanded ডিজাইন নেই বলে ফোন লেআউট মাঝখানে, সর্বোচ্চ ৬০০dp চওড়া রাখা হবে।
11. **ডামি ডাটা পার্সিস্টেন্স** → **Room-এ সেভ থাকবে।** রুটিন তৈরি, এক্সাম সাবমিশন, গ্রুপ তৈরি/জয়েন — অ্যাপ বন্ধ করে খুললেও (দিন পরেও) একই অবস্থা দেখাবে। স্ট্যাটিক কনটেন্ট (গ্রুপের তালিকা, প্রশ্ন, সদস্য, র‍্যাংকিং) কোডেই হার্ডকোড থাকবে।
12. **এক্সামের মাঝে অ্যাপ বন্ধ করে ফিরলে** → **অটো-সাবমিট।** প্রতিটা উত্তর বাছাইয়ে Room-এ সেভ হয়; অ্যাপ বন্ধ করে ফিরলে যতটুকু উত্তর দেওয়া ছিল তা দিয়েই সাথে সাথে সাবমিট হয়ে যায়, এক্সাম স্ক্রিন আর খুলবে না।
13. **ক্যালকুলেটর/স্ক্র্যাচ ক্যানভাস বাটন** → থাকবে, ট্যাপে "শীঘ্রই আসছে" স্ন্যাকবার। ফাংশনাল ক্যালকুলেটর এই রাউন্ডে বানানো হবে না।
14. **একাধিক লাইভ এক্সাম** → **হ্যাঁ, তালিকা স্ক্রিন বানাবো।** Home-এর লাইভ কার্ড এখন `LiveExamList`-এ যাবে (৩টা ডামি লাইভ এক্সাম, ভিন্ন অবস্থায়), তারপর ট্যাপে ডিটেইল।
15. **প্রস্তাবিত ৪র্থ ডামি গ্রুপ** (ফিজিক্স ক্র্যাক টিম, অ্যাডমিন + রুটিন নেই) → **হ্যাঁ, যোগ হবে।**

---

## ৮. ডিজাইন-ছাড়া অংশ (P5.5) — ডিজাইন সিস্টেমের বিদ্যমান কম্পোনেন্ট দিয়ে

**গ্রুপ তৈরি/জয়েন** — GroupList-এর + বাটন → বটম শিট, দুটো অপশন:
- *নতুন গ্রুপ তৈরি করো:* নাম (৩–৪০ অক্ষর), ঐচ্ছিক বিবরণ → Fake রিপোতে গ্রুপ তৈরি, `myRole = ADMIN`, ৬-অক্ষরের জয়েন কোড জেনারেট → সরাসরি নতুন GroupDetail (অবস্থা NO_ROUTINE, অ্যাডমিন CTA দেখায়)। GroupDetail-এ অ্যাডমিন "জয়েন কোড শেয়ার করো" চিপ দেখবে (Android share sheet)।
- *কোড দিয়ে জয়েন করো:* ৬-অক্ষরের কোড → ডামি কোড `PHY27X` ঠিক হলে একটা ডামি গ্রুপে `MEMBER` হিসেবে যোগ; ভুল কোডে এরর "কোডটি সঠিক নয়"; আগে থেকে সদস্য হলে "তুমি আগেই এই গ্রুপে আছো"।
- রাউট: `GroupCreate`, `GroupJoin` (root-level) বা একটা `ModalBottomSheet` — শিট দিয়েই করা হবে, আলাদা রাউট লাগবে না।
- Use cases: `CreateGroupUseCase`, `JoinGroupUseCase`।

**গ্রুপ রুটিন ক্যালেন্ডার** — Routine.dc.html-এর "গ্রুপ" মোড অনুসরণ করে:
- রাউট `GroupRoutineCalendar(groupId)` — GroupDetail-এর "গ্রুপ রুটিন ক্যালেন্ডার দেখো" থেকে। Routine ট্যাব নিজে এই রাউন্ডেও ComingSoon থাকবে (ব্যক্তিগত রুটিন আলাদা কাজ)।
- উপরে সপ্তাহের ৭ দিনের স্ট্রিপ (আগের/পরের সপ্তাহে সোয়াইপ), নির্বাচিত দিনের কার্ড: এক্সাম-দিনে "গ্রুপ এক্সাম — {বিষয়/অধ্যায়}" + অবস্থা চিপ (আসন্ন/খোলা/শেষ, ট্যাপ → সঠিক স্ক্রিন); বাকি দিনে "ব্যক্তিগত অধ্যয়ন — {টপিক}" + "কনসেপ্ট পড়ো" (কনসেপ্ট ফিচার এখনও নেই → "শীঘ্রই আসছে")।
- নিচে নোট: "এটি অ্যাডমিনের তৈরি গ্রুপ রুটিন — সব মেম্বার একই ক্যালেন্ডার দেখবে।"
- রুটিন না থাকলে খালি অবস্থা (অ্যাডমিন → রুটিন তৈরির CTA, সদস্য → অপেক্ষার মেসেজ) — GroupDetail-এর কার্ডের মতোই।
- দিন-ভিত্তিক টপিক বণ্টন: নির্বাচিত টপিকগুলো রুটিনের দিনসংখ্যায় ক্রমানুসারে ভাগ — `GroupRoutinePlanner` pure ফাংশন, ইউনিট টেস্ট সহ।

**"সবাই দেখো" লিস্ট**
- `GroupPastExams(groupId)` — সব আগের এক্সাম, নতুন থেকে পুরনো, প্রতিটায় তারিখ · সাবমিট সংখ্যা · গড় · *তোমার স্কোর* (দাওনি হলে "অংশ নাওনি"); ট্যাপ → WindowRanking। ডামিতে ১০টি এক্সাম।
- `GroupMembers(groupId)` — সব সদস্য, সার্চ বক্স, অ্যাডমিনরা উপরে ব্যাজ সহ, "তুমি" চিহ্নিত। ডামিতে HSC ব্যাচ ২৭-এর ৩৮ জন। সদস্য ম্যানেজ (অ্যাডমিন বানানো/বাদ দেওয়া) এই রাউন্ডে নেই।
