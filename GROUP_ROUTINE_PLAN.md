# স্টাডি গ্রুপ + এক্সাম রুটিন — A to Z ইমপ্লিমেন্টেশন প্ল্যান (আসল API)

> **স্ট্যাটাস (৯ নভেম্বর ২০২৬):** P0–P10 শেষ। `assembleDebug`, `testDebugUnitTest` (৭৩১টা) আর instrumented টেস্ট (রুটিন ১৩, Home ৬, নোটিফিকেশন ২, গ্রুপ ১৮) ফোন আর ট্যাবলেট দুই ইমুলেটরেই পাস (ট্যাবলেটে এক্সাম ইঞ্জিন ২৭টাও)। আসল সার্ভারে লগইন করে চালানো বাকি।
> - P10: `FeatureAccess` (`auth/me`-এর `scopes` থেকে)। `group:use` না থাকলে Home-এর "গ্রুপ" মেনু আর গ্রুপ এক্সাম কার্ড লুকানো; `routine:use` না থাকলে রুটিন ট্যাবে "তোমার অ্যাকাউন্টে রুটিন ফিচার চালু নেই"। স্কোপ লিস্ট ফাঁকা হলে দুটোই বন্ধ; প্রোফাইল জানা না থাকলে দুটোই চালু। গ্রুপ আর রুটিনের সব API কল `AUTHZ_UNAVAILABLE` এলে ১, ২, ৪ সেকেন্ড পর পর সর্বোচ্চ ৩ বার রিট্রাই করে (`retryWhileAuthzUnavailable`); `INSUFFICIENT_SCOPE` রিট্রাই হয় না। §৭-এর রিফ্রেশ টাইমার আগের ধাপেই ছিল (`RefreshAtWindowEdges`, ফিরে এলে রিফ্রেশ)।
> - P10-এর পরে (৯ নভেম্বর): সদস্যরা শুধু "N টপিক" দেখত, কারণ নাম আসত শুধু ফোনের কারিকুলাম ক্যাশ থেকে; অ্যাডমিনের ক্যাশ রুটিন বানানোর সময় ভরে যেত, সদস্যের নয়। এখন ক্যাশে না থাকলে নামগুলো সার্ভার থেকে এনে ক্যাশে রাখা হয় (`CurriculumRepository.fillNames`)। এক্সামের বিস্তারিত আর নতুন "পরের এক্সামের প্রস্তুতি" কার্ডে (সব সদস্যের জন্য, GroupDetail, ক্যালেন্ডার, রুটিন ট্যাবে): পুরো অধ্যায় হলে শুধু অধ্যায়ের নাম, আংশিক হলে তার টপিকগুলো (`TopicOutline`)। "কনসেপ্ট পড়ো" চাপলে "শীঘ্রই আসছে"।
> - অ্যাডাপ্টিভ (§১০): মিডিয়াম আর বড় স্ক্রিনে গ্রুপ লিস্ট বাঁয়ে, খোলা গ্রুপ (বা তার সদস্য, ক্যালেন্ডার, র‍্যাংকিং…) ডানে (Nav3 ListDetail)। প্রতিটা স্ক্রিন যতটুকু জায়গা পায় সেই অনুযায়ী সাজে (`PaneSized`): GroupDetail দুই কলাম, ক্যালেন্ডার ২–৩ প্যান, র‍্যাংকিং সারাংশ + তালিকা, সেটিংস দুই কলাম, বিল্ডার স্টেপ-রেল + দুই কলাম ফর্ম + প্রিভিউ গ্রিড।
> - P9: নতুন `Notifications` স্ক্রিন (সব/অপঠিত, পেজিনেশন, অপঠিত রোতে বোল্ড আর ডট, সময় ঢাকায়)। ট্যাপে সাথে সাথে পড়া হিসেবে মার্ক (ফেল করলে ফিরে যায়, `NOTIFICATION_NOT_FOUND` হলে না) আর `target_url` টেবিল-ভিত্তিক পার্সারে (`NotificationTarget`): `/groups/{id}` → GroupDetail, `/groups/{id}/join-requests` → GroupJoinRequests, `/routines…` → রুটিন ট্যাব (`MainTabRequests` দিয়ে), অজানা হলে শুধু মার্ক। Home-এর বেল এখন নোটিফিকেশন খোলে, অপঠিত থাকলে লাল ডট (`HomeNotificationsViewModel`, প্রতিবার Home-এ ফিরলে রিফ্রেশ)। P3-এর ফাঁক মেটানো: সদস্য নয় এমন কেউ GroupDetail খুললে সদস্য/এক্সামের এরর দেখায় না। FCM পুশ আলাদা কাজ (D4)।
> - P8: রুটিন ট্যাব (`RoutineTabScreen`) ComingSoon-এর জায়গায়। ব্যক্তিগত | গ্রুপ টগল। ব্যক্তিগত: "{n} দিনের প্ল্যান · {বাকি} দিন বাকি", মেয়াদের মধ্যে সপ্তাহ বদলানো, দিনের এক্সাম কার্ড (বাটন সহ, রেজাল্ট লক ছাড়া), এক্সাম-ছাড়া দিনে পরের এক্সামের টপিক (D8), মেয়াদের বাইরে লেখা, "কনসেপ্ট পড়ো" (শীঘ্রই আসছে), ⋮-এ "রুটিন মুছে নতুন বানাও", রুটিন না থাকলে "রুটিন তৈরি করো" → বিল্ডার (Personal)। গ্রুপ: সব গ্রুপের সপ্তাহের এক্সাম, গ্রুপ ফিল্টার চিপ, কার্ডে গ্রুপের নাম, অ্যাডমিন হলে তারিখ সরানো, র‍্যাংকিং, নিচে নোট; কোনো গ্রুপে না থাকলে "গ্রুপে যাও"। Compact = স্ট্রিপ + দিন; Medium = দিনের তালিকা + দিন; Expanded = + সপ্তাহের এক্সাম তালিকা (সাইজ ক্লাস দিয়ে)। সপ্তাহের স্ট্রিপ এখন `ui/screen/routine/WeekStrip.kt`-এ শেয়ার্ড (গ্রুপ ক্যালেন্ডারও এটা ব্যবহার করে)। Home কুইক মেনু "রুটিন" এখন রুটিন ট্যাব খোলে।
> - P7: নতুন `GroupRanking(groupId, examId, title, closesAt)` স্ক্রিন: "অংশ নিয়েছে X/Y" (আসল), গড় স্কোর আর স্কোর বিস্তার (ডামি, `GroupExamStatsProvider` → `DummyGroupExamStatsProvider`, examId-seeded; আসল API এলে শুধু DI বদলাবে), র‍্যাংক লিস্ট পেজিনেশন সহ, আমার র‍্যাংক (৫ পেজ পর্যন্ত খোঁজে), আমার রো হাইলাইটেড আর ট্যাপে আমার অ্যান্সার শিট, "আমার ফলাফল দেখো", 404-এ "র‍্যাংকিং তৈরি হচ্ছে", উইন্ডো খোলা থাকলে "উইন্ডো শেষে র‍্যাংকিং আসবে"। `GroupPastExams` আসল API-তে (`FetchPastSlotsUseCase`: শেষ হওয়া স্লট, নতুন আগে; রোতে দিয়েছো/অংশ নাওনি আর ডামি সাবমিট/গড়)। GroupDetail-এ শেষ ৩টা এক্সামের প্রিভিউ + "সবাই দেখো"। শেষ বা মিস হওয়া স্লট কার্ডে "র‍্যাংকিং ও অ্যান্সার শিট দেখো" (এক্সাম থাকলে) + "প্র্যাকটিস হিসেবে দাও"।
> - P6: স্লট কার্ডে অবস্থা অনুযায়ী বাটন: খোলা → "এক্সামে অংশ নিন", চলমান → "এক্সাম শেষ করো" (D9: আগের LIVE অ্যাটেম্পট জমা হয়ে লকড রেজাল্ট খোলে), সাবমিট করা → "তুমি সাবমিট করেছো — দেখো", উইন্ডো শেষ → "ফলাফল দেখো" + "প্র্যাকটিস হিসেবে দাও", মিস → "প্র্যাকটিস হিসেবে দাও"। সিদ্ধান্তটা `OpenScheduledExamUseCase`-এ। `ExamAttempt`/`ExamResult`/`ExamAnswerSheet`/`AttemptAnalysis` রাউটে `lockResultUntil`; `ResultLockGate` উইন্ডো শেষ না হওয়া পর্যন্ত `LockedExamResultScreen` দেখায় (সাবমিট ✓, কাউন্টডাউন, সময় লেগেছে, স্কোর নেই, অ্যান্সার শিট লক), শূন্যে নিজে থেকে আসল রেজাল্টে বদলায়। LIVE টাইমার উইন্ডোর শেষের বেশি চলে না (`liveRemainingSeconds`), আর এক্সাম স্ক্রিনের উপরে "গ্রুপ এক্সাম · উইন্ডো শেষ {সময়}" ব্যানার। হিস্ট্রি থেকে খোলা গ্রুপ এক্সাম এখনও লক হয় না (§১৫.৩, B4)।
> - P5: GroupDetail-এ সবার জন্য "গ্রুপ এক্সাম" অংশ: আজকের প্রতিটা স্লটের আলাদা কার্ড (টাইটেল/টপিক, কনফিগ, "একক এক্সাম" আর "N বার সরানো" চিপ, উইন্ডোর অবস্থা; খোলা উইন্ডোতে লাল কার্ড), না থাকলে "আজ এক্সাম নেই — পরের এক্সাম {তারিখ}", কিছুই না থাকলে অ্যাডমিন/সদস্যের আলাদা লেখা, আর ক্যালেন্ডার লিংক। কার্ড ট্যাপে `SlotDetailSheet` (টপিক, নিয়ম, উইন্ডো, অবস্থা; অ্যাডমিন হলে তারিখ সরাও আর একক এক্সাম মুছো)। `GroupRoutineCalendar` আসল API-তে: সোমবার-শুরু সপ্তাহ, রুটিনের মেয়াদের মধ্যে আগে-পরে যাওয়া, দিনের ডট/✓/লাল ডট, এক্সাম-ছাড়া দিনে পরের এক্সামের তারিখ ও টপিক (D8), অ্যাডমিনের ⋮-এ একক এক্সাম আর রুটিন মুছা। GroupList কার্ডে আজকের ব্যাজ, Home-এ আসল "গ্রুপ এক্সাম · আজ" কার্ড (আলাদা `HomeGroupExamViewModel`; ডেমো কার্ড আর তার স্ট্রিং সরানো)। টপিকের নাম আসে ক্যাশ করা কারিকুলাম থেকে (`DescribeTopicGroupingUseCase`, `CurriculumRepository.cachedNames`)। উইন্ডো খোলা/বন্ধের মুহূর্তে স্ক্রিন নিজে রিলোড হয় (§৭)। এক্সাম দেওয়ার বাটন (অংশ নাও / শেষ করো / প্র্যাকটিস) P6-এ, র‍্যাংকিং P7-এ।
> - P4: `ui/screen/routine/builder/`-এ একটাই রুট `RoutineBuilder(owner, mode)`। ৪টা ধাপ (একক এক্সামে ৩টা) একই স্ক্রিন আর একই ViewModel-এর ভেতরে, তাই হাজারো topic id রাউটে যায় না। সিলেকশন, ফর্ম আর ধাপ `SavedStateHandle`-এ রাখা, তাই process death-এর পরও টিকে থাকে। ধাপ ৩-এ ব্যক্তিগত রুটিনে উইন্ডো লুকানো, একক এক্সামে তারিখ আর নাম। ধাপ ৪-এ preview, সব warning, আর প্রতিটা দিনের টপিক লেবেল (`TopicLabel`, বিল্ডারের লোড করা নাম দিয়ে; P5-এর `DescribeTopicGroupingUseCase` এটাকেই কাজে লাগাতে পারে)। GroupDetail-এ শুধু অ্যাডমিনের জন্য "গ্রুপ এক্সাম" কার্ড: রুটিন না থাকলে "গ্রুপ রুটিন তৈরি করুন", আর সবসময় "একক এক্সাম নির্ধারণ করো"। সেভের পর GroupDetail-এ স্ন্যাকবার (`GroupNoticeBus`)। সদস্যের স্লট দেখা P5-এ। ব্যক্তিগত রুটিনের এন্ট্রি P8-এ (বিল্ডার তৈরি আছে)।
> - §১৫.১-এর ঝুঁকি মেটানো হয়েছে এভাবে: API `chapter_ids`/`topic_ids`-কে পুরো সিলেকশনের ওপর ফিল্টার ধরে ("narrow to these"), তাই কোনো অংশ বাদ গেলেই রাখা সব অধ্যায় (দরকারে সব টপিক) স্পষ্ট করে পাঠানো হয়, পুরো বইগুলোরও (`RoutineScope`)। সব বই পুরো হলে শুধু `subject_ids`।
> - P3: সদস্য তালিকায় অ্যাডমিনের ⋮ মেনু (অ্যাডমিন বানানো/সরানো, বাদ দেওয়া, কনফার্ম সহ) আর "সদস্য যোগ করো" শিট। GroupDetail-এ অ্যাডমিনের ⚙ আর অপেক্ষমাণ রিকোয়েস্টের ব্যানার, সদস্যের "গ্রুপ ছেড়ে দাও"। নতুন `GroupSettings` (এডিট, প্রাইভেট করার সতর্কতা, কোড রিজেনারেট, লিভ, নাম টাইপ করে ডিলিট), নতুন `GroupJoinRequests` (৩ ট্যাব, গ্রহণ/বাতিল), আর GroupList-এ "খুঁজে দেখো" ট্যাব (সার্চ, পেজিনেশন, ৪ রকম বাটন)। লিভ বা ডিলিট করলে ওই গ্রুপের সব খোলা স্ক্রিন বন্ধ হয়। `AdminGate` আসল API ব্যবহার করে না বলে নতুন অ্যাডমিন স্ক্রিনগুলো নিজেই `NOT_GROUP_ADMIN` দেখে ফিরে যায়।
> - P2: GroupList, তৈরি/জয়েন শিট (বিবরণ আর পাবলিক সুইচ সহ), GroupDetail (হেডার, বিবরণ, অ্যাডমিনের ইনভাইট কোড কপি/শেয়ার, সদস্য প্রিভিউ), GroupMembers (সব পেজ, "তুমি" চিহ্ন, খোঁজা) আসল API-তে। মাঝের সময়ের সিদ্ধান্ত অনুযায়ী GroupDetail-এর এক্সাম কার্ড, রুটিন লিংক আর আগের এক্সাম, আর GroupList-এর এক্সাম ব্যাজ **লুকানো**। এগুলো P4–P7-এ আসল ডাটা নিয়ে ফিরবে। Home-এর ডেমো গ্রুপ কার্ড আপাতত GroupList খোলে (§৬.১৯, P5)। ফেক রিপো এখনও আছে, শুধু রুটিন বিল্ডার/ক্যালেন্ডার/আগের এক্সাম স্ক্রিনের জন্য, আর সেগুলোতে এখন পৌঁছানোর কোনো পথ নেই।
> - P0: `ExamTypes.SCHEDULED` + অজানা মানে `UNKNOWN`, `util/DhakaTime.kt`, `util/ApiErrorMessages.kt` + `GroupRoutineErrorCode`।
> - P1: ৭টা enum (অজানা মানে নিরাপদ fallback), ৮টা request + ১৪টা response মডেল, `GroupApiService` / `RoutineApiService` / `NotificationApiService`, তিনটা রিপোজিটরি, `domain/group|routine|notification/`-এ ৩০টা ইউজ কেস, DI-তে wiring, ৪২টা নতুন টেস্ট।
> - নাম: পুরনো ফেক মডেলের সাথে নাম না মেলাতে নতুন মডেলগুলোর নাম `StudyGroup`, `StudyGroupMember`, `GroupJoinRequest`, `ExamRoutine`, `RoutineSlot`। পুরনো ৩টা ইউজ কেসের সাথে নাম মেলে, তাই DI-তে পুরো প্যাকেজ নাম লেখা আছে, P2-তে পুরনোগুলো সরলে ঠিক হয়ে যাবে।
> - সরানো হয়েছে P5-এ: `DescribeTopicGroupingUseCase` (লাগবে `CurriculumRepository`-তে id দিয়ে নাম খোঁজার মেথড)।
> **সোর্স:** `CHANGELOG.md` (§২ Groups, §৩ Routines, §৫ Notifications) + `openapi.json` (শুধু `/groups`, `/routines`, `/notifications`, আর এক্সাম অ্যাটেম্পট/লিডারবোর্ডের যে অংশ লাগবে) + ডিজাইন ক্যানভাস `প্রস্তুতি — স্ক্রিন ডিজাইন` (Group* ৮টি, Routine* ৪টি, Home, ExamProperties)।
> **আগের প্ল্যান:** `GROUP_LIVE_EXAM_PLAN.md`-এ গ্রুপ ফিচার ডামি ডাটায় বানানো হয়েছিল। এই প্ল্যান সেটাকে আসল API-তে নিয়ে যায়, আর যোগ করে ব্যক্তিগত রুটিন, একক (ad-hoc) এক্সাম, মেম্বার ম্যানেজমেন্ট ও নোটিফিকেশন। লাইভ এক্সাম এই প্ল্যানের বাইরে, আগের মতো ডামিতেই থাকবে।

---

## ০. তোমার সিদ্ধান্তগুলো (৮ নভেম্বর ২০২৬)

| # | প্রশ্ন | সিদ্ধান্ত |
|---|---|---|
| D1 | গ্রুপ/রুটিন এক্সাম কোন স্ক্রিনে চলবে | **আসল এক্সাম ইঞ্জিন** (`ExamAttemptScreen` / `ExamResult` / `ExamAnswerSheet`)। স্লটের উইন্ডো খোলা থাকা পর্যন্ত রেজাল্ট অ্যাপের দিক থেকে লক থাকবে (`GroupResultLocked` ডিজাইন)। উইন্ডো শেষ হলে প্র্যাকটিস হিসেবে আবার দেওয়া যাবে। ফেক `TimedExam*` স্ক্রিনগুলো শুধু লাইভ এক্সামের জন্য থাকবে। |
| D2 | র‍্যাংকিং | API যা দেয় তা আসল ডাটায় দেখাবে: র‍্যাংক লিস্ট, অংশগ্রহণকারী সংখ্যা, আমার র‍্যাংক। 404 এলে "র‍্যাংকিং তৈরি হচ্ছে"। **ডিজাইনের বাকি অংশ (গড় স্কোর, স্কোর বিস্তার হিস্টোগ্রাম ইত্যাদি) ডামি ডাটায় স্ক্রিনে থাকবে।** সেগুলো আসল করতে কোন API লাগবে তার লিস্ট §১৪-এ। |
| D3 | রুটিন তৈরি | ব্যক্তিগত রুটিনেও এক্সাম কনফিগ সেকশন যোগ হবে। সেভের আগে একটা **প্রিভিউ** ধাপ থাকবে (`/routines/preview`: দিন-ভিত্তিক টপিক + warnings), গ্রুপ আর ব্যক্তিগত দুই রুটিনেই। |
| D4 | নোটিফিকেশন | এখন শুধু ইন-অ্যাপ লিস্ট (Home-এর বেল আইকন থেকে)। FCM পুশ পরের আলাদা কাজ। |
| D5 | মেম্বার ম্যানেজমেন্ট | API-র সবকিছু থাকবে (অ্যাড, রিমুভ, রোল, লিভ, জয়েন রিকোয়েস্ট, Explore, এডিট/ডিলিট, কোড রিজেনারেট)। ডিজাইন নেই, তাই বিদ্যমান ডিজাইন-সিস্টেম কম্পোনেন্ট দিয়ে বানানো হবে। |
| D6 | একক (ad-hoc) গ্রুপ এক্সাম | রুটিনের ধাপগুলো রিইউজ: সাবজেক্ট → অধ্যায় → কনফিগ (এই মোডে "কত দিন/কোন বার"-এর জায়গায় একটা তারিখ + নাম) → প্রিভিউ ছাড়াই সেভ। |
| D7 | রুটিন ডিলিট / এক্সাম সরানো | দুটোই থাকবে। ব্যক্তিগত রুটিনে মালিক নিজে, গ্রুপে অ্যাডমিন। ডিজাইনের "একবার সেভ করলে আর পরিবর্তন করা যাবে না" লেখাটা বদলাবে (§৬.৮)। |
| D8 | ক্যালেন্ডারে এক্সাম-ছাড়া দিন | "আজ এক্সাম নেই — পরের এক্সাম {তারিখ}, প্রস্তুতি নাও: {টপিক}" + "কনসেপ্ট পড়ো"। ডাটা আসবে পরের স্লট থেকে, নতুন API লাগবে না। |
| D9 | অসমাপ্ত LIVE গ্রুপ এক্সাম আবার খুললে | **বিদ্যমান নিয়মই থাকবে: সাথে সাথে অটো-সাবমিট**, চালিয়ে যাওয়া যাবে না (আগের প্ল্যানের সিদ্ধান্ত ১২, আর এক্সাম ইঞ্জিনের `StartOrResumeExamAttemptUseCase`)। উইন্ডো শেষের পর শুরু করা PRACTICE অ্যাটেম্পট আগের মতোই চালিয়ে যাওয়া যায়। |

আগের প্ল্যানের যে সিদ্ধান্তগুলো এখনও খাটে: একাধিক অ্যাডমিন থাকতে পারে, অ্যাডমিনও এক্সাম দেয় ও র‍্যাংকিংয়ে থাকে। র‍্যাংকিংয়ে শুধু নিজের রো ট্যাপ করে নিজের অ্যান্সার শিট দেখা যায়। উইন্ডো চলাকালীন সাবমিটের পর কোনো স্কোর দেখায় না। ট্যাবলেটে ফোন লেআউট মাঝখানে রেখে সর্বোচ্চ ৬০০dp চওড়া।

---

## ১. মূল ধারণা (glossary)

| শব্দ | মানে |
|---|---|
| **গ্রুপ** | একটা ব্যাচ (যেমন "HSC 26-27"), সদস্যরা একই এক্সাম দেয়। পাবলিক হলে Explore-এ দেখায় আর জয়েন রিকোয়েস্ট নেয়। প্রাইভেট হলে শুধু কোড দিয়ে জয়েন করা যায়। জীবিত গ্রুপে সবসময় অন্তত একজন **ADMIN** থাকে। |
| **রোল** | গ্রুপ-ভিত্তিক `ADMIN` বা `MEMBER`। একই ইউজার এক গ্রুপে অ্যাডমিন, আরেক গ্রুপে সদস্য হতে পারে। গ্লোবাল রোল না। |
| **রুটিন** | বিষয়/অধ্যায়/টপিক, দিনসংখ্যা আর সপ্তাহের বার থেকে বানানো দিন-ভিত্তিক এক্সাম শিডিউল। একজন ইউজারের একটাই সক্রিয় ব্যক্তিগত রুটিন থাকতে পারে, আর একটা গ্রুপেরও একটাই। |
| **স্লট** | শিডিউলের একটা এক্সাম-দিন: `opens_at`–`closes_at` উইন্ডো। `kind`: রুটিন থেকে এলে `ROUTINE`, অ্যাডমিন আলাদা করে বানালে `ADHOC`। |
| **উইন্ডো** | ব্যক্তিগত রুটিনে প্রতি এক্সাম-দিনে ২৪ ঘণ্টা (ঢাকা সময়ের মধ্যরাত থেকে মধ্যরাত)। গ্রুপে অ্যাডমিন সময় ঠিক করে দেয় (মধ্যরাত থেকে কত মিনিট পর, যেমন ১৯:০০ = ১১৪০)। |
| **`my_status`** | স্লটটা এই মুহূর্তে আমার জন্য কী: `UPCOMING`, `OPEN`, `IN_PROGRESS`, `COMPLETED`, `MISSED`। **পুরো UI এটা দিয়েই চলবে**, অ্যাপে নিজে হিসাব করবে না। |
| **LIVE / PRACTICE** | উইন্ডোর ভেতরে শুরু করা অ্যাটেম্পট LIVE (লিডারবোর্ডে গোনা হয়)। উইন্ডোর পরে শুরু করলে PRACTICE। |
| **শেয়ার্ড প্রশ্নসেট** | প্রথম যে স্লট start করে, তখনই এক্সাম তৈরি হয়। পরে যারা start করে সবাই একই `exam_id` পায়। |

### ১.১ ওভারল্যাপ: ব্যক্তিগত রুটিন বনাম গ্রুপ রুটিন

দুটো একই API, একই রিকোয়েস্ট মডেল, একই স্লট মডেল আর একই এক্সাম-ফ্লো ব্যবহার করে। পার্থক্য শুধু নিচের ঘরগুলোতে, তাই কোডে **একটাই রুটিন বিল্ডার** থাকবে, যাতে একটা `RoutineOwner` (Personal / Group(groupId)) প্যারামিটার যায়।

| বিষয় | ব্যক্তিগত | গ্রুপ |
|---|---|---|
| কে বানায় | যেকোনো ইউজার (`routine:use` স্কোপ) | শুধু গ্রুপ অ্যাডমিন |
| রিকোয়েস্টে `group_id` | `null` | গ্রুপের id |
| উইন্ডো | সবসময় ২৪ ঘণ্টা; ফর্মে উইন্ডো-ফিল্ড **লুকানো** | অ্যাডমিন শুরু/শেষ সময় দেয় (ফর্মে দেখায়) |
| সীমা | একটা সক্রিয় (`ROUTINE_EXISTS` 409) | প্রতি গ্রুপে একটা সক্রিয় |
| একক এক্সাম (ad-hoc) | নেই | আছে, শুধু অ্যাডমিন |
| তারিখ সরানো | মালিক | অ্যাডমিন |
| ডিলিট | মালিক | অ্যাডমিন |
| কে দেখে | শুধু নিজে | সব সদস্য |
| নোটিফিকেশন | নেই | সদস্যরা পায় |
| কোথায় দেখায় | রুটিন ট্যাব → "ব্যক্তিগত" | রুটিন ট্যাব → "গ্রুপ" (সব গ্রুপ মিলিয়ে) + GroupDetail + GroupRoutineCalendar |

---

## ২. স্কোপে থাকা API (সব `/api/v1`-এর নিচে)

### ২.১ গ্রুপ (`group:use`)
| মেথড | পাথ | কোথায় ব্যবহার হবে |
|---|---|---|
| POST | `/groups` | গ্রুপ তৈরি (§৬.১) |
| GET | `/groups/manage?page&limit≤50` | আমার গ্রুপ (§৬.৪) |
| GET | `/groups/explore?q&page&limit≤50` | Explore ট্যাব (§৬.৩) |
| POST | `/groups/join` `{code}` | কোড দিয়ে জয়েন (§৬.২) |
| GET | `/groups/{id}` | GroupDetail হেডার, সেটিংস |
| PATCH | `/groups/{id}` | গ্রুপ এডিট (§৬.৭) |
| DELETE | `/groups/{id}` | গ্রুপ ডিলিট (§৬.৭) |
| POST | `/groups/{id}/regenerate-code` | নতুন কোড (§৬.৫) |
| GET | `/groups/{id}/members?page&limit≤100` | সদস্য তালিকা (§৬.৬) |
| POST | `/groups/{id}/members` `{identifier}` | ইমেইল/ফোন দিয়ে অ্যাড |
| PATCH | `/groups/{id}/members/{user_id}` `{role}` | অ্যাডমিন বানানো / সরানো |
| DELETE | `/groups/{id}/members/{user_id}` | রিমুভ |
| POST | `/groups/{id}/leave` | গ্রুপ ছাড়া |
| POST | `/groups/{id}/join-requests` | পাবলিক গ্রুপে রিকোয়েস্ট |
| GET | `/groups/{id}/join-requests?status=PENDING` | অ্যাডমিনের রিকোয়েস্ট তালিকা |
| POST | `/groups/{id}/join-requests/{rid}/approve` · `/reject` | রিকোয়েস্টে সিদ্ধান্ত |

### ২.২ রুটিন ও স্লট (`routine:use`)
| মেথড | পাথ | কোথায় |
|---|---|---|
| POST | `/routines/preview` | রুটিন প্রিভিউ ধাপ (§৬.৯) |
| POST | `/routines` | রুটিন সেভ |
| GET | `/routines/active?group_id=` | রুটিন আছে কিনা + তার স্লট (`group_id` না দিলে ব্যক্তিগত) |
| DELETE | `/routines/{routine_id}` | রুটিন ডিলিট |
| GET | `/routines/slots?date_from&date_to&group_id&page&limit≤100` | ক্যালেন্ডার, GroupDetail-এর আজকের কার্ড, GroupList-এর ব্যাজ, আগের এক্সাম |
| POST | `/routines/slots` | একক এক্সাম তৈরি (§৬.১০) |
| POST | `/routines/slots/{slot_id}/start` | এক্সাম শুরু (§৬.১২) |
| PATCH | `/routines/slots/{slot_id}/reschedule` `{slot_date}` | তারিখ সরানো (§৬.১৫) |
| DELETE | `/routines/slots/{slot_id}` | একক এক্সাম ডিলিট |

### ২.৩ এক্সাম (আগে থেকেই `ExamRepository`-তে আছে, শুধু রিইউজ)
`POST /exams/{exam_id}/attempts` (`attemptMCQExam`), `GET /exams/attempts/manage?exam_id=` (`listMyAttempts`, আমার অ্যাটেম্পট id খুঁজতে), submit / result / answer-sheet / analysis, আর `GET /exams/{exam_id}/leaderboard?page` (`getExamLeaderboard`)।

### ২.৪ নোটিফিকেশন (যেকোনো লগইন করা ইউজার)
`GET /notifications?unread_only&page&limit≤50`, `POST /notifications/{id}/read`। `PUT /notifications/fcm-token` এখন বাদ (D4)।

### ২.৫ বাইরে রাখা
সাবস্ক্রিপশন, পেমেন্ট, `/admin`, লাইভ এক্সাম, FCM।

---

## ৩. বর্তমান কোডের অবস্থা আর কী বদলাবে

ডামি রাউন্ডে (`GROUP_LIVE_EXAM_PLAN.md`) এগুলো বানানো হয়েছিল: `ui/screen/group/`-এ ৮টা স্ক্রিন (List, Detail, Members, PastExams, RoutineCalendar, RoutineSubject, RoutineChapter, RoutineWindow) আর `GroupCreateJoinSheet`। এগুলোর পেছনে আছে `GroupExamRepository` → `FakeGroupExamRepository`, যেটা Room-এর `created_group`, `group_routine`, `demo_exam_window`, `timed_exam_attempt` টেবিল আর `TimedExamRepository`-র ওপর চলে। Routine ট্যাব এখন `ComingSoonScreen`।

| জিনিস | কী হবে |
|---|---|
| `ui/screen/group/*` স্ক্রিনগুলো | **রাখা হবে, ডাটা-সোর্স বদলাবে।** UI-এর বেশিরভাগ থাকবে। স্টেট আর মডেল নতুন API মডেলে যাবে। |
| `GroupRoutineSubject/Chapter/Window` | একটা সাধারণ **রুটিন বিল্ডারে** রূপান্তর, যেটা ব্যক্তিগত রুটিন, গ্রুপ রুটিন আর একক এক্সাম তিনটাই চালাবে (§৬.৯)। ফোল্ডার: `ui/screen/routine/builder/`। |
| `GroupCreateJoinSheet` | রাখা হবে। লেবেল "৬-অক্ষরের কোড" বদলে হবে "গ্রুপ কোড" (API-তে কোড ৪–২০ অক্ষর)। বিবরণ ও পাবলিক/প্রাইভেট ফিল্ড যোগ হবে। |
| `GroupExamRepository` + `FakeGroupExamRepository` | **বাদ।** নতুন `GroupRepository` + `RoutineRepository` আসবে। |
| Room: `CreatedGroupEntity/Dao`, `GroupRoutineEntity/Dao` | **বাদ** (গ্রুপ ডাটা অনলাইন-অনলি, ক্যাশ হবে না)। স্কিমা বদলালে অ্যাপ আনইন্সটল করে আবার ইন্সটল করতে হবে (CLAUDE.md অনুযায়ী মাইগ্রেশন নেই)। |
| `TimedExamRepository`, `TimedExamKind.GROUP`, `demo_exam_window`, `timed_exam_attempt` | লাইভ এক্সামের জন্য থাকবে। `TimedExamKind.GROUP` আর গ্রুপ-সংক্রান্ত শাখাগুলো অব্যবহৃত হয়ে যাবে, ইমপ্লিমেন্টেশনের সময় **ডেড কোড হিসেবে রিপোর্ট করে অনুমতি নিয়ে** সরানো হবে (work-rules)। |
| `GroupRoutineSummary`, `CreateGroupRoutineRequest`, `GroupDetail`, `Group` মডেল | নতুন API মডেল দিয়ে প্রতিস্থাপন (§৪)। |
| `GroupRoutinePlanner` (ক্লায়েন্টে টপিক ভাগ) | **বাদ।** টপিক ভাগ এখন সার্ভারের কাজ (preview/slots)। |
| `ExamTypes` enum | `SCHEDULED` যোগ হবে। অজানা মান যেন ক্র্যাশ না করে সেই ব্যবস্থা থাকবে (CHANGELOG §১.২)। **এটা আগে করতে হবে**, নাহলে হিস্ট্রি বা এক্সাম লিস্টে SCHEDULED এক্সাম এলেই পার্স ফেল করতে পারে। |
| Routine ট্যাব (`ComingSoonScreen`) | আসল `RoutineScreen` (§৬.১৬)। |
| Home: বেল আইকন | নোটিফিকেশন স্ক্রিনে যাবে (§৬.১৮)। |
| Home: "গ্রুপ এক্সাম · ইনভাইট" কার্ড (এখন ডেমো group id) | আসল ডাটা: আজকের প্রথম খোলা গ্রুপ স্লট (§৬.১৯)। |
| DI | প্রজেক্টে একটা বড় classic-DSL `appModule` আছে। নতুন definition-গুলোও **একই স্টাইলে** সেখানে যাবে (android-master §১৪.৭)। ফিচার-ভিত্তিক মডিউলে ভাগ করা আলাদা প্রস্তাব, এই কাজে নয়। |

---

## ৪. ডাটা লেয়ার

### ৪.১ মডেল (`data/model/…`, সব `@Serializable`, `@SerialName` snake_case)

**request/**
- `GroupCreateRequest(name, description?, isPublic=false, maxMembers=500)`: নাম ২–২৫৫, বিবরণ ≤১০২৪, সদস্য-সীমা ২–৫০০০।
- `GroupUpdateRequest(name?, description?, isPublic?, maxMembers?)`: শুধু বদলানো ফিল্ড পাঠানো হবে (`explicitNulls = false` থাকায় null ফিল্ড বাদ যায়)।
- `JoinByCodeRequest(code)` (৪–২০ অক্ষর), `DirectAddRequest(identifier)` (৫–২৫৫), `MemberRoleRequest(role)`।
- `RoutineCreateRequest(subjectIds, chapterIds, topicIds, questionsCount, durationMinutes, markPerQuestion, negativeMarking, negativeMarkValue, level?, difficulty?, sourceId?, groupId?, durationDays, weekdays, startDate?, windowOpenMinute, windowCloseMinute)`: preview আর create দুটোতেই একই মডেল।
- `AdhocExamRequest(… একই কনফিগ …, groupId, examDate, windowOpenMinute, windowCloseMinute, title?)`।
- `RescheduleRequest(slotDate)`।

**response/**
- `GroupResponse(id, name, description?, isPublic, maxMembers, memberCount, createdAt, myRole: GroupRole?, inviteCode?, hasPendingRequest)`
- `GroupMember(userId, username, role, joinedAt)`: পুরনো `GroupMember` বদলে যাবে। "তুমি" চিহ্ন দিতে `CurrentUserDao` থেকে নিজের id মেলানো হবে।
- `JoinRequest(id, groupId, userId, username?, status, createdAt, decidedAt?)`
- `GroupActionAck(groupId)` (API-র `MessageData`)
- `RoutineResponse(...)`, `RoutineWithSlots(routine, slots, warnings)`, `RoutinePlan(examDays, slots: List<PlannedSlot>, warnings)`, `PlannedSlot(slotDate, opensAt, closesAt, topicIds, topicGrouping)`, `RoutineDeleteResult(routineId, removedSlots)`
- `Slot(id, kind, routineId?, groupId?, title?, slotDate, opensAt, closesAt, topicIds, topicGrouping, questionsCount, durationMinutes, markPerQuestion, negativeMarkValue, level?, difficulty?, sourceId?, postponedCount, examId?, myStatus)`
- `TopicGrouping(level, subjectId?, chapters: List<ChapterGroup(chapterId, topicIds, isPartial)>)`
- `SlotStart(slotId, examId, isLive, closesAt)`
- `NotificationItem(id, title, message, type, targetUrl?, isRead, createdAt)`
- `MetaData`: এখনকার মডেলে `total_items / current_page / total_pages / page_limit` আছে কিনা মিলিয়ে নিতে হবে, পেজিনেশন এগুলো দিয়েই চলবে।

**enums/** (string-মানের; অজানা মানে ক্র্যাশ নয়)
- `GroupRole` (আছে), `JoinRequestStatus`, `SlotKind {ROUTINE, ADHOC}`, `SlotMyStatus`, `TopicGroupingLevel`, `QuestionDifficulty`, `Weekday {MON…SUN}`, `ExamTypes` + `SCHEDULED`।
- `NotificationItem.type` আর `JoinRequest.status` String-ই থাকবে, কারণ API এগুলোকে String হিসেবে ডকুমেন্ট করেছে আর অজানা মান উপেক্ষা করতে বলেছে।

### ৪.২ সময় (`util/DhakaTime.kt`)
- API-র `opens_at`/`closes_at`/`created_at` আসে `"2026-11-10T18:00:00"` আকারে, কোনো zone ছাড়া, অথচ এগুলো **UTC**। তাই পার্স হবে `LocalDateTime.parse(s).toInstant(ZoneOffset.UTC)` দিয়ে। শেষে `Z` থাকলেও যেন চলে।
- `slot_date`, `start_date`, `exam_date` আসে `LocalDate` (ঢাকা)। "আজ" মানে `LocalDate.now(ZoneId.of("Asia/Dhaka"))`, ডিভাইসের টাইমজোন না।
- মিনিট ↔ সময়: `360 ↔ ০৬:০০`। ১৪৪০ মানে "রাত ১২:০০ (পরের দিন)"।
- বাংলা সংখ্যা ও তারিখ ফরম্যাটার: প্রজেক্টে যেটা আছে সেটাই রিইউজ হবে।
- "উইন্ডো খোলা আছে আরও ৩ ঘণ্টা" কাউন্টডাউন: `closesAt - now`, প্রতি মিনিটে রিফ্রেশ (`TimeProvider` ইনজেক্টেড, যাতে টেস্ট করা যায়)।

### ৪.৩ ApiService (`data/service/remote/`, প্রতিটা কল `safeApiCall` দিয়ে)
- `GroupApiService`: §২.১-এর ১৭টা কল।
- `RoutineApiService`: §২.২-এর ৯টা কল।
- `NotificationApiService`: list, markRead।
- লিস্ট কলগুলো `ApiResponse.Success.metaData` ফেরত দেবে, তা দিয়ে পেজিনেশন হবে।

### ৪.৪ রিপোজিটরি (interface + Impl, `single`)
- `GroupRepository`: গ্রুপ, মেম্বার, জয়েন রিকোয়েস্টের সব অপারেশন।
- `RoutineRepository`: preview, create, active(owner), delete, slots(filter), createAdhoc, start, reschedule, deleteAdhoc।
- `NotificationRepository`।
- কোনো Room ক্যাশ নেই, সরাসরি নেটওয়ার্ক। সাবজেক্ট/অধ্যায়/টপিকের **নাম** আসবে বিদ্যমান `CurriculumRepository` (Room ক্যাশ) থেকে, কারণ স্লট শুধু id দেয়।

### ৪.৫ ইউজ কেস (`domain/`, প্রতি অপারেশনে একটা, `factory`)
গ্রুপ: `CreateGroup`, `FetchMyGroups`, `ExploreGroups`, `JoinGroupByCode`, `FetchGroup`, `UpdateGroup`, `DeleteGroup`, `RegenerateGroupCode`, `FetchGroupMembers`, `AddGroupMember`, `SetMemberRole`, `RemoveGroupMember`, `LeaveGroup`, `RequestToJoinGroup`, `FetchJoinRequests`, `ApproveJoinRequest`, `RejectJoinRequest`।
রুটিন: `PreviewRoutine`, `CreateRoutine`, `FetchActiveRoutine`, `DeleteRoutine`, `FetchSlots`, `CreateAdhocExam`, `StartSlot`, `RescheduleSlot`, `DeleteAdhocExam`।
কম্পোজিট:
- `StartScheduledExamUseCase`: `StartSlot` → `attemptMCQExam(examId)`। `ATTEMPT_ALREADY_ACTIVE` এলে `listMyAttempts(examId, unfinished=true)` থেকে চলমান অ্যাটেম্পট খুঁজে সেটাই খুলবে।
- `FindMyAttemptForSlotUseCase`: আগের এক্সামের রেজাল্ট খুলতে `listMyAttempts(examId)` থেকে সর্বশেষ সাবমিট করা অ্যাটেম্পট।
- `DescribeTopicGroupingUseCase`: `TopicGrouping` + কারিকুলাম ক্যাশ থেকে বাংলা লেবেল বানায় (§৬.১১)।
- নোটিফিকেশন: `FetchNotifications`, `MarkNotificationRead`।

### ৪.৬ এরর কোড → বাংলা মেসেজ
একটা জায়গায় (`util/ApiErrorMessages.kt` বা `strings.xml` + ম্যাপার) `errorCode → @StringRes`। কোড না মিললে সার্ভারের `message` দেখাবে।

| কোড | বাংলা মেসেজ / আচরণ |
|---|---|
| `INSUFFICIENT_SCOPE` (403) | "তোমার অ্যাকাউন্টে এই ফিচার চালু নেই"। রিট্রাই নয় (§৬.২০)। |
| `AUTHZ_UNAVAILABLE` (503) | "সার্ভার ব্যস্ত, একটু পরে চেষ্টা করো"। ব্যাকঅফ দিয়ে রিট্রাই। |
| `GROUP_NOT_FOUND` | "গ্রুপটি পাওয়া যায়নি বা তুমি আর সদস্য নও"। স্ক্রিন বন্ধ হয়ে GroupList রিফ্রেশ। |
| `NOT_GROUP_ADMIN` | "শুধু অ্যাডমিন এটা করতে পারবে"। গ্রুপ রিলোড, কারণ রোল বদলে গিয়ে থাকতে পারে। |
| `INVALID_GROUP_CODE` | "কোডটি সঠিক নয়" |
| `ALREADY_MEMBER` | "তুমি আগেই এই গ্রুপে আছো" (অ্যাডে: "উনি আগেই সদস্য") |
| `GROUP_FULL` | "গ্রুপে আর জায়গা নেই" |
| `LAST_ADMIN` | "গ্রুপে অন্তত একজন অ্যাডমিন থাকতে হবে — আগে আরেকজনকে অ্যাডমিন বানাও" |
| `MAX_MEMBERS_TOO_LOW` | "সদস্য-সীমা বর্তমান সদস্য সংখ্যার কম হতে পারবে না" |
| `INVALID_IDENTIFIER` | "সঠিক ইমেইল বা ফোন নম্বর দাও" |
| `USER_NOT_REGISTERED` | "এই ইমেইল/ফোনে কোনো অ্যাকাউন্ট নেই" |
| `MEMBER_NOT_FOUND` | "এই সদস্য আর গ্রুপে নেই"। লিস্ট রিফ্রেশ। |
| `JOIN_REQUEST_EXISTS` | "রিকোয়েস্ট আগেই পাঠানো হয়েছে" |
| `JOIN_REQUEST_NOT_FOUND` / `JOIN_REQUEST_DECIDED` | "রিকোয়েস্টটির সিদ্ধান্ত আগেই হয়ে গেছে"। লিস্ট রিফ্রেশ। |
| `CODE_GENERATION_FAILED` (503) | "আবার চেষ্টা করো"। একবার নিজে থেকে রিট্রাই। |
| `ROUTINE_EXISTS` | "আগে থেকে একটা রুটিন চালু আছে"। সক্রিয় রুটিন দেখাবে। |
| `START_IN_PAST` / `DATE_IN_PAST` | "অতীতের তারিখ দেওয়া যাবে না" |
| `INVALID_WINDOW` | "শেষের সময় শুরুর পরে হতে হবে" |
| `WINDOW_IN_PAST` | "এই সময়ের উইন্ডো পার হয়ে গেছে" |
| `NO_EXAM_DAYS` | "এই মেয়াদে নির্বাচিত বারগুলোর কোনো দিন পড়েনি" |
| `NO_TOPICS_TO_SCHEDULE` | "নির্বাচিত অংশে কোনো টপিক নেই" |
| `CURRICULUM_MISMATCH` / `SUBJECT_NOT_FOUND` | "নির্বাচনে সমস্যা আছে, আবার বাছাই করো" |
| `ROUTINE_NOT_FOUND` / `ROUTINE_ALREADY_DELETED` | "রুটিনটি আর নেই"। রিফ্রেশ। |
| `SLOT_NOT_FOUND` | "এক্সামটি আর নেই"। রিফ্রেশ। |
| `SLOT_NOT_OPEN` | "উইন্ডো এখনও খোলেনি — {সময়}-এ খুলবে" |
| `SLOT_ALREADY_OPEN` | "উইন্ডো খুলে গেছে, এখন আর তারিখ সরানো যাবে না" |
| `SLOT_HAS_ATTEMPTS` | "কেউ এক্সামটি দিয়ে ফেলেছে, তাই মুছা যাবে না" |
| `NOT_AN_ADHOC_EXAM` | "রুটিনের এক্সাম আলাদা করে মুছা যায় না — পুরো রুটিন মুছতে হবে" |
| `SLOT_NO_QUESTIONS` | "এই টপিকগুলোতে এখনও প্রশ্ন নেই" |
| `OUTSIDE_ROUTINE_PERIOD` | "রুটিনের মেয়াদের বাইরে সরানো যাবে না" |
| `INVALID_DATE_RANGE` | ক্লায়েন্টেই আটকানো হবে, তাই সাধারণত দেখা যাবে না |
| `ATTEMPT_ALREADY_ACTIVE` | এরর না দেখিয়ে চলমান অ্যাটেম্পট খুলবে |
| `EXAM_EXPIRED` / `ATTEMPT_ALREADY_ENDED` | "এক্সামের সময় শেষ"। রেজাল্টে যাবে। |
| `NOTIFICATION_NOT_FOUND` | চুপচাপ উপেক্ষা |

---

## ৫. রাউট (`navigation/Routes.kt`)

বিদ্যমান স্টাইল অনুযায়ী গ্রুপ স্ক্রিনগুলো `RootRoute` (বটম-ন্যাভ ছাড়া ফুল-স্ক্রিন)। রুটিন ট্যাব `MainRoute.Routine`।

| রাউট | অবস্থা | কোথা থেকে |
|---|---|---|
| `GroupList` | আছে | Home কুইক মেনু "গ্রুপ" |
| `GroupDetail(groupId)` | আছে | GroupList, Home কার্ড, নোটিফিকেশন |
| `GroupMembers(groupId)` | আছে, অ্যাকশন যোগ হবে | GroupDetail "সবাই দেখো" |
| `GroupPastExams(groupId)` | আছে | GroupDetail "সবাই দেখো" |
| `GroupRoutineCalendar(groupId)` | আছে | GroupDetail "গ্রুপ রুটিন ক্যালেন্ডার দেখো" |
| `GroupSettings(groupId)` | **নতুন**, শুধু অ্যাডমিন | GroupDetail টপবার ⚙ |
| `GroupJoinRequests(groupId)` | **নতুন**, শুধু অ্যাডমিন | GroupDetail ব্যানার / Settings |
| `GroupExplore` | নতুন নয়, GroupList-এর দ্বিতীয় ট্যাব | — |
| `RoutineBuilderSubjects(owner, mode)` | **নতুন** (পুরনো `GroupRoutineSubjectSelect`-এর জায়গায়) | রুটিন ট্যাব / GroupDetail |
| `RoutineBuilderChapters(owner, mode, subjectIds)` | **নতুন** | ধাপ ১ |
| `RoutineBuilderConfig(owner, mode, selection)` | **নতুন** | ধাপ ২ |
| `RoutinePreview(owner, request)` | **নতুন** | ধাপ ৩ (শুধু রুটিন মোডে) |
| `SlotDetail(slotId)` | **নতুন**, ছোট বটম শিট | ক্যালেন্ডার/কার্ড ট্যাপ: টপিক, কনফিগ, তারিখ সরানো, ডিলিট |
| `Notifications` | **নতুন** | Home বেল |
| `ExamAttempt(attemptId, titleBn, lockResultUntil: String? = null)` | **প্যারাম যোগ** | স্লট start |
| `ExamResult(attemptId, titleBn, fromHistory, lockResultUntil: String? = null)` | **প্যারাম যোগ** | অ্যাটেম্পট সাবমিট |
| `ExamLeaderboardRoute(examId, titleBn)` | আছে, গ্রুপ ভ্যারিয়েন্টে রিইউজ | §৬.১৩ |

- `owner` = `@Serializable sealed interface RoutineOwner { data object Personal; data class Group(groupId) }`
- `mode` = `ROUTINE | ADHOC`। ADHOC শুধু `Group` owner-এর সাথে বৈধ।
- `GroupAdminRoute` সিল্ড ইন্টারফেস আর `AdminGate` থাকবে। অ্যাডমিন-অনলি রাউটে ঢোকার আগে `my_role` যাচাই হবে। সার্ভার `NOT_GROUP_ADMIN` দিলে স্ক্রিন বন্ধ হবে।
- বড় সিলেকশন (শত শত topic id) রাউটের ভেতরে না রেখে বিল্ডারের একটা শেয়ার্ড স্টেট-হোল্ডারে রাখা হবে, যেমন Practice-এ `PracticeSelection` আছে। ইমপ্লিমেন্টেশনের শুরুতে ঠিক হবে কীভাবে, যাতে process death-এর পরও সিলেকশন টিকে থাকে।

---

## ৬. ফ্লো: A থেকে Z

প্রতিটা স্ক্রিন = `XxxScreen` + `XxxContent`, `PullToRefreshErrorOverlayWrapper`, স্টেট `collectAsStateWithLifecycle()` দিয়ে। নেভিগেশন ক্লিক `dropUnlessResumed`-এ মোড়ানো। সব লেখা `strings.xml`-এ (বাংলা + `values-en`)।

### ৬.১ গ্রুপ তৈরি
1. GroupList টপবার **＋** → `GroupCreateJoinSheet` → "নতুন গ্রুপ তৈরি করো" ট্যাব।
2. ফিল্ড: নাম (২–২৫৫, বাধ্যতামূলক), বিবরণ (ঐচ্ছিক, ≤১০২৪), "পাবলিক গ্রুপ (Explore-এ দেখাবে, জয়েন রিকোয়েস্ট নেবে)" সুইচ, যার ডিফল্ট **বন্ধ**। সদস্য-সীমা ফর্মে থাকবে না, ডিফল্ট ৫০০, পরে Settings থেকে বদলানো যায়।
3. `POST /groups` → 201। রেসপন্সে `my_role = ADMIN` আর `invite_code` আসে।
4. শিট বন্ধ হয়ে সরাসরি `GroupDetail(newId)` খোলে। রুটিন নেই, তাই অ্যাডমিনের CTA দেখায়। উপরে একবার "কোড শেয়ার করো: K7M2XQ9P" কার্ড দেখাবে।
5. এরর: `CODE_GENERATION_FAILED` হলে একবার অটো-রিট্রাই, তারপর মেসেজ। ভ্যালিডেশন এরর ফিল্ডের নিচে দেখাবে।

### ৬.২ কোড দিয়ে জয়েন (পাবলিক বা প্রাইভেট, সাথে সাথে সদস্য)
1. শিটের "কোড দিয়ে জয়েন করো" ট্যাব। ইনপুট বড়-হাতের অক্ষরে নেওয়া হবে, স্পেস ট্রিম হবে, ৪–২০ অক্ষর।
2. `POST /groups/join {code}` → 200 `GroupResponse` → সরাসরি `GroupDetail`।
3. এরর: `INVALID_GROUP_CODE`, `ALREADY_MEMBER` (সাথে "গ্রুপ খোলো" বাটন, কারণ তুমি তো সদস্যই), `GROUP_FULL`।

### ৬.৩ Explore + জয়েন রিকোয়েস্ট (শুধু পাবলিক গ্রুপ)
1. GroupList-এ দুটো ট্যাব: **আমার গ্রুপ** | **খুঁজে দেখো**। ডিজাইনের "আমার গ্রুপ" লেআউট প্রথম ট্যাবে থাকবে।
2. খুঁজে দেখো: সার্চবক্স (৩০০ms debounce, `q` ১–১০০ অক্ষর) → `GET /groups/explore` পেজিনেটেড (নিচে পৌঁছালে পরের পেজ)।
3. প্রতিটা কার্ডে নাম, বিবরণ, "{n}/{max} সদস্য", আর একটা বাটন:
   - `my_role != null` → **খোলো** (GroupDetail)
   - `has_pending_request` → **রিকোয়েস্ট পাঠানো হয়েছে** (নিষ্ক্রিয়)
   - `member_count >= max_members` → **গ্রুপ পূর্ণ** (নিষ্ক্রিয়)
   - অন্যথায় → **জয়েন রিকোয়েস্ট পাঠাও** → `POST /groups/{id}/join-requests` → 201 হলে বাটন বদলে "রিকোয়েস্ট পাঠানো হয়েছে" (optimistic, ফেল করলে আগের অবস্থায় ফেরে)।
4. ফাঁকা অবস্থা: "কোনো পাবলিক গ্রুপ পাওয়া যায়নি"।
5. অ্যাপ্রুভ হলে ইউজার নোটিফিকেশন পায়। "আমার গ্রুপ" ট্যাব রিফ্রেশ করলে গ্রুপটা দেখাবে।

### ৬.৪ আমার গ্রুপ (GroupList)
1. `GET /groups/manage` (সবশেষে জয়েন করা আগে) + `GET /routines/slots?date_from=আজ&date_to=আজ` (সব গ্রুপের আজকের স্লট, একবারে)। দুটো একসাথে চলে, `group_id` দিয়ে মেলানো হয়।
2. কার্ড (ডিজাইন GroupList): অ্যাভাটারে নামের প্রথম ২ অক্ষর, নাম, "{n} সদস্য · তুমি অ্যাডমিন/সাধারণ সদস্য"। ডান পাশে/নিচে আজকের স্লটের অবস্থা:
   - কোনো স্লট `OPEN` বা `IN_PROGRESS` → লাল ব্যানার "{title} — উইন্ডো খোলা আছে আরও {X}"
   - সব স্লট `COMPLETED` → সবুজ চিপ "সম্পন্ন"
   - `UPCOMING` → "আজ {সময়}-এ খুলবে"
   - কিছু নেই → "কোনো চলমান এক্সাম নেই"
3. ফাঁকা অবস্থা (কোনো গ্রুপ নেই): ইলাস্ট্রেশন + "গ্রুপ তৈরি করো" / "কোড দিয়ে জয়েন করো" দুটো বাটন।
4. পেজিনেশন: সাধারণত ২০টার কম গ্রুপ থাকবে, তবু meta দেখে পরের পেজ লোড হবে।

### ৬.৫ GroupDetail (রোল-ভিত্তিক)
ডিজাইনের "সাধারণ সদস্য ভিউ / অ্যাডমিন ভিউ" টগলটা ডেমোর জন্য ছিল, **অ্যাপে থাকবে না।** ভিউ ঠিক হবে `my_role` দিয়ে।

একসাথে চলা লোড:
- `GET /groups/{id}` (হেডার, রোল, `invite_code`)
- `GET /routines/active?group_id=` (রুটিন আছে কিনা)
- `GET /routines/slots?group_id=&date_from=আজ&date_to=আজ` (আজকের কার্ড)
- `GET /routines/slots?group_id=&date_to=গতকাল` → শেষ ৩টা (আগের এক্সাম প্রিভিউ)
- `GET /groups/{id}/members?limit=4` (সদস্য প্রিভিউ)
- অ্যাডমিন হলে `GET /groups/{id}/join-requests?limit=1`। পেন্ডিং থাকলে "{n}টি জয়েন রিকোয়েস্ট" ব্যানার দেখায়। সংখ্যাটা আসে `meta_data.total_items` থেকে।

সেকশনগুলো উপর থেকে নিচে:
1. **টপবার:** ব্যাক, নাম, "{n} সদস্য"। অ্যাডমিনের জন্য ⚙ (Settings) আর শেয়ার আইকন।
2. **ইনভাইট কোড চিপ (শুধু অ্যাডমিন):** "কোড: K7M2XQ9P"। ট্যাপে কপি, লং-প্রেস বা শেয়ারে Android share sheet ("প্রস্তুতি অ্যাপে আমার গ্রুপ '{নাম}'-এ জয়েন করো, কোড: …")।
3. **"গ্রুপ রুটিন ক্যালেন্ডার দেখো"** লিংক (রুটিন বা কোনো স্লট থাকলে)।
4. **এক্সাম স্ট্যাটাস** (আজকের স্লটগুলো, একাধিক থাকলে প্রতিটার আলাদা কার্ড):
   | অবস্থা | অ্যাডমিন | সদস্য |
   |---|---|---|
   | রুটিন নেই, আজ স্লট নেই | ড্যাশড কার্ড + **"গ্রুপ রুটিন তৈরি করুন"** + **"একক এক্সাম নির্ধারণ করো"** | "এখনও অ্যাডমিন গ্রুপের রুটিন সেট করেনি…" |
   | রুটিন আছে, আজ এক্সাম নেই | "আজ এক্সাম নেই — পরের এক্সাম {তারিখ}" + "একক এক্সাম নির্ধারণ করো" | একই, বাটন ছাড়া |
   | `UPCOMING` | "{সময়}-এ উইন্ডো খুলবে" + কনফিগ লাইন | একই |
   | `OPEN` | লাল কার্ড "উইন্ডো খোলা আছে আরও {X}" + টাইটেল + "{q}টি প্রশ্ন · প্রতি প্রশ্নে {m} মার্ক · নেগেটিভ {n}" + **"এক্সামে অংশ নিন"** | একই |
   | `IN_PROGRESS` | একই কার্ড + **"এক্সাম শেষ করো"**। ট্যাপ করলে আগের LIVE অ্যাটেম্পট সাথে সাথে জমা হয়ে লকড রেজাল্ট খোলে (D9)। কার্ডে লেখা: "তুমি এক্সাম শুরু করেছিলে — আবার খুললে যতটুকু দিয়েছো তা জমা হয়ে যাবে।" | একই |
   | `COMPLETED` আর উইন্ডো খোলা | **"তুমি সাবমিট করেছো — দেখো"** → লকড রেজাল্ট | একই |
   | উইন্ডো শেষ (`COMPLETED` বা `MISSED`) | "উইন্ডো শেষ" চিপ + "{total_participants} জন অংশ নিয়েছে · গড় স্কোর {ডামি}" + **"র‍্যাংকিং ও অ্যান্সার শিট দেখো"** | একই। `MISSED` হলে "তুমি অংশ নাওনি" চিপ, তবে "প্র্যাকটিস হিসেবে দাও" বাটন থাকবে। |
5. **আগের এক্সামগুলো** (৩টা) + "সবাই দেখো" → GroupPastExams।
6. **সদস্যরা ({n})** (৪ জন) + "সবাই দেখো" → GroupMembers।
7. **নিচে (সদস্য):** "গ্রুপ ছেড়ে দাও" (কনফার্ম ডায়ালগ)। অ্যাডমিনের এই অপশন Settings-এ।

### ৬.৬ সদস্য তালিকা ও ম্যানেজমেন্ট (GroupMembers)
1. `GET /groups/{id}/members` (অ্যাডমিন আগে, পেজ ৫০)। সার্চবক্স লোড হওয়া তালিকায় username দিয়ে ফিল্টার করে (সার্ভারে সার্চ API নেই)।
2. রো: অ্যাভাটার (username-এর প্রথম অক্ষর), username, "অ্যাডমিন" ব্যাজ, নিজের রোতে "তুমি"। ডিজাইনের পূর্ণ নাম (যেমন "সাদিয়া ইসলাম") API দেয় না, তাই আপাতত username (§১৪-এ রিকোয়েস্ট)।
3. **অ্যাডমিন:** প্রতিটা রোতে ⋮ মেনু:
   - "অ্যাডমিন বানাও" / "অ্যাডমিন থেকে সরাও" → `PATCH …/members/{user_id} {role}`
   - "গ্রুপ থেকে বাদ দাও" → কনফার্ম ডায়ালগ → `DELETE …/members/{user_id}`
   - নিজের রোতে: "অ্যাডমিন থেকে সরাও" (`LAST_ADMIN` হলে মেসেজ)
4. **অ্যাডমিন: সদস্য অ্যাড** → FAB "সদস্য যোগ করো" → বটম শিট:
   - ইনপুট "ইমেইল বা ফোন নম্বর"। ক্লায়েন্টে নরমালাইজ হবে: `01XXXXXXXXX` → `+8801XXXXXXXXX`, `8801…` → `+8801…`, ইমেইল lowercase-trim।
   - `POST …/members {identifier}` → 201 → তালিকায় যোগ হয়, স্ন্যাকবার "{username} যোগ হয়েছে"।
   - এরর: `INVALID_IDENTIFIER`, `USER_NOT_REGISTERED`, `ALREADY_MEMBER`, `GROUP_FULL`।
5. অ্যাকশন শেষে লিস্ট রিফ্রেশ হয়। নিজের রোল বদলালে (নিজেকে ডিমোট) GroupDetail-এ ফিরে সদস্য ভিউ দেখাবে।

### ৬.৭ গ্রুপ সেটিংস (`GroupSettings`, শুধু অ্যাডমিন)
- **এডিট:** নাম, বিবরণ, পাবলিক সুইচ, সদস্য-সীমা (২–৫০০০) → `PATCH /groups/{id}` (শুধু বদলানো ফিল্ড)।
  - পাবলিক থেকে প্রাইভেটে গেলে আগে সতর্কতা: "প্রাইভেট করলে অপেক্ষমাণ সব জয়েন রিকোয়েস্ট বাতিল হয়ে যাবে।"
  - `MAX_MEMBERS_TOO_LOW` হলে ফিল্ডে এরর।
- **ইনভাইট কোড:** বর্তমান কোড + "নতুন কোড বানাও" → কনফার্ম ("পুরনো কোড সাথে সাথে বন্ধ হয়ে যাবে") → `POST …/regenerate-code`।
- **জয়েন রিকোয়েস্ট** (পাবলিক গ্রুপে) → `GroupJoinRequests`।
- **রুটিন:** রুটিন থাকলে "রুটিন মুছে দাও" (§৬.১৫)।
- **গ্রুপ ছেড়ে দাও** → `POST …/leave` (শেষ অ্যাডমিন হলে `LAST_ADMIN` মেসেজ + "সদস্য তালিকায় যাও")।
- **গ্রুপ মুছে দাও** (লাল) → নাম টাইপ করে কনফার্ম → `DELETE /groups/{id}` → GroupList-এ ফিরে রিফ্রেশ। লেখা: "সব সদস্য নোটিফিকেশন পাবে; সবার এক্সাম হিস্ট্রি থেকে যাবে।"

### ৬.৮ জয়েন রিকোয়েস্ট (`GroupJoinRequests`, অ্যাডমিন)
- ট্যাব: **অপেক্ষমাণ** (ডিফল্ট) | **গৃহীত** | **বাতিল** → `GET …/join-requests?status=` (পুরনো আগে, পেজিনেটেড)।
- অপেক্ষমাণ রো: username, "{কত আগে}", দুটো বাটন **গ্রহণ** / **বাতিল** → approve/reject → রো সরে যায়।
- এরর: `JOIN_REQUEST_DECIDED` (অন্য অ্যাডমিন আগেই সিদ্ধান্ত নিয়েছে) হলে রিফ্রেশ। Approve-এ `GROUP_FULL` হলে মেসেজ + "সদস্য-সীমা বাড়াও" লিংক (Settings)।

### ৬.৯ রুটিন বিল্ডার: গ্রুপ রুটিন ও ব্যক্তিগত রুটিন (৪ ধাপ)
একটাই বিল্ডার, `owner` ভিন্ন। ডিজাইন: GroupSubjectSelect → GroupChapterSelect → GroupExamWindow (+ GroupRoutineCreate/RoutineCreate-এর ফিল্ডগুলো এখানে একীভূত, আগের প্ল্যানের সিদ্ধান্ত ১)।

**ধাপ ১/৪: সাবজেক্ট** (`GroupSubjectSelect` ডিজাইন)
- বিদ্যমান কারিকুলাম ক্যাশ থেকে সাবজেক্ট লিস্ট, চেকবক্স কার্ড, "{n}টি অধ্যায় · {q}+ প্রশ্ন"। প্রশ্ন সংখ্যা পাওয়া না গেলে শুধু অধ্যায়।
- ১–২০টা সাবজেক্ট (API সীমা)। নিচের বারে "{n}টি সাবজেক্ট নির্বাচিত" + **পরবর্তী**।

**ধাপ ২/৪: অধ্যায় ও টপিক** (`GroupChapterSelect` ডিজাইন, ট্রাই-স্টেট চেকবক্স)
- সাবজেক্ট টিক মানে পুরো বই। অধ্যায় খুললে টপিক।
- রিকোয়েস্ট বানানোর নিয়ম (API: খালি মানে পুরো সাবজেক্ট/অধ্যায়):
  - পুরো সাবজেক্ট নির্বাচিত → শুধু `subject_ids`-এ, তার কোনো chapter/topic পাঠানো হবে না
  - সাবজেক্টের কিছু অধ্যায় পুরো নির্বাচিত → `chapter_ids`
  - অধ্যায়ের কিছু টপিক → `topic_ids` (+ সেই অধ্যায় `chapter_ids`-এ)
  - সার্ভার এর ব্যাখ্যা ঠিক কীভাবে করে, সেটা প্রথম preview কলে যাচাই করতে হবে (§১৫ ঝুঁকি)
- সীমা: chapter ≤ ২০০, topic ≤ ৫০০। বেশি হলে "পুরো অধ্যায়/বই বেছে নাও" হিন্ট।
- নিচে: "{c}টি অধ্যায়, {t}টি টপিক নির্বাচিত" + **পরবর্তী**।

**ধাপ ৩/৪: সময় ও নিয়ম** (`GroupExamWindow` ডিজাইন + GroupRoutineCreate-এর দিন/বার অংশ)
| ফিল্ড | ব্যক্তিগত | গ্রুপ | API ফিল্ড / সীমা |
|---|---|---|---|
| শুরুর তারিখ | হ্যাঁ, ডিফল্ট আজ | হ্যাঁ | `start_date` (অতীত নয়) |
| কত দিনের রুটিন | ১–৯০ (ডিজাইন) | ৭–৯০ (ডিজাইন) | `duration_days` ১–৩৬৬ |
| সপ্তাহের কোন বার | চিপ সোম…রবি, অন্তত ১টা | একই | `weekdays` MON…SUN |
| উইন্ডো শুরু / শেষ | **লুকানো** (২৪ ঘণ্টা) | টাইম পিকার, ডিফল্ট সন্ধ্যা ৭:০০ – রাত ১১:০০ | `window_open_minute` / `window_close_minute` |
| প্রশ্ন সংখ্যা | ±৫ স্টেপার, ডিফল্ট ২৫ | একই | ১–২০০ |
| সময়সীমা (মিনিট) | স্টেপার, ডিফল্ট ২৫ | একই | ১–৪৮০, উইন্ডোর দৈর্ঘ্যের বেশি নয় (ক্লায়েন্ট চেক) |
| প্রতি প্রশ্নের মান | ±০.২৫, ডিফল্ট ১ | একই | >০ – ১০০ |
| নেগেটিভ মার্কিং | সুইচ + ±০.২৫, ডিফল্ট চালু ০.২৫ | একই | `negative_marking`, `negative_mark_value` |
| মোট মার্ক | গণনা করে দেখায় | একই | — |
| level / difficulty / source | প্রথম রাউন্ডে পাঠানো হবে না (`null`) | একই | §১৫ |
- ক্লায়েন্টে ভ্যালিডেশন: শেষ > শুরু (`INVALID_WINDOW`), শুরুর তারিখ অতীত নয়, অন্তত একটা বার।
- ডিজাইনের ⚠️ ব্যানারের লেখা (D7 অনুযায়ী) বদলে হবে: "সেভ করার পর এই নিয়মগুলো এডিট করা যাবে না। দরকার হলে রুটিন মুছে নতুন বানাতে হবে, অথবা কোনো একটা এক্সামের তারিখ সরাতে পারবে।" ইমোজির জায়গায় ডিজাইন সিস্টেমের ওয়ার্নিং আইকন।
- **পরবর্তী** → ধাপ ৪।

**ধাপ ৪/৪: প্রিভিউ** (নতুন, D3)
- `POST /routines/preview` (একই বডি)।
- উপরে সারাংশ: "{exam_days}টি এক্সাম-দিন · {start} – {end} · প্রতিদিন {window}"।
- `warnings[]` থাকলে টারশিয়ারি-কন্টেইনার কার্ডে **প্রতিটা দেখানো বাধ্যতামূলক** (CHANGELOG বলছে "Always show")। সার্ভারের ইংরেজি লেখা হুবহু দেখাবে, কারণ বাংলা অনুবাদ API দেয় না (§১৪)।
- নিচে দিন-ভিত্তিক তালিকা: "{বার}, {তারিখ}" + টপিক লেবেল (`topic_grouping` থেকে, §৬.১১)।
- বাটন: **"রুটিন সেভ করো ও সক্রিয় করো"** → `POST /routines` → 201।
  - গ্রুপ: GroupDetail-এ ফেরে, স্ন্যাকবার "রুটিন চালু হয়েছে — সব সদস্য নোটিফিকেশন পেয়েছে"।
  - ব্যক্তিগত: রুটিন ট্যাবে ফেরে।
  - `ROUTINE_EXISTS` → "আগে থেকে একটা রুটিন চালু আছে" + "দেখো" বাটন।
- **"পেছনে গিয়ে বদলাও"** → ধাপ ৩।
- preview ফেল করলে (`NO_EXAM_DAYS`, `NO_TOPICS_TO_SCHEDULE`, `START_IN_PAST`, `CURRICULUM_MISMATCH`) এরর দেখিয়ে সংশ্লিষ্ট ধাপে ফেরার বাটন।

### ৬.১০ একক (ad-hoc) গ্রুপ এক্সাম (D6)
- এন্ট্রি: GroupDetail (অ্যাডমিন) → "একক এক্সাম নির্ধারণ করো" → `RoutineBuilderSubjects(Group(id), ADHOC)`।
- ধাপ ১ ও ২ একই।
- ধাপ ৩ (ADHOC মোড): "কত দিন/কোন বার"-এর জায়গায় **এক্সামের তারিখ** (ডেট পিকার, আজ বা পরে) + **নাম** (ঐচ্ছিক, ≤২৫৫, প্লেসহোল্ডার "যেমন: শুক্রবারের মডেল টেস্ট")। উইন্ডো আর কনফিগ ফিল্ড একই। ⚠️ ব্যানার: "কেউ এক্সাম দেওয়ার আগ পর্যন্ত এটা মুছে ফেলা যাবে।"
- প্রিভিউ ধাপ নেই (API-তে ad-hoc preview নেই)। বাটন **"এক্সাম নির্ধারণ করো"** → `POST /routines/slots` → 201 `Slot` → GroupDetail, স্ন্যাকবার।
- এরর: `WINDOW_IN_PAST` (আজকের তারিখে পার হয়ে যাওয়া সময়), `INVALID_WINDOW`, `NO_TOPICS_TO_SCHEDULE`।
- একই দিনে রুটিনের এক্সাম আর একাধিক একক এক্সাম আলাদা কার্ডে দেখাবে।

### ৬.১১ স্লট দেখানো: টাইটেল ও টপিক লেবেল
`DescribeTopicGroupingUseCase` (কারিকুলাম ক্যাশ থেকে নাম):
- `title` থাকলে সেটাই প্রধান লাইন।
- `CHAPTER` → "{সাবজেক্ট} — {অধ্যায়}"। `is_partial` হলে "(আংশিক: {টপিক১}, {টপিক২}…)"।
- `CHAPTERS` → "{সাবজেক্ট} — {অধ্যায়১}, {অধ্যায়২}…"। ৩টার বেশি হলে "+{n}টি"।
- `TOPICS` → "{টপিক১}, {টপিক২}…"
- নাম ক্যাশে না পেলে (নতুন অধ্যায়) "{n}টি টপিক"। কারিকুলাম রিফ্রেশ ব্যাকগ্রাউন্ডে।
- কনফিগ লাইন: "{q}টি প্রশ্ন · {d} মিনিট · প্রতি প্রশ্নে {m} মার্ক · নেগেটিভ {n}" (`negative_mark_value == 0` হলে "নেগেটিভ নেই")।
- `postponed_count > 0` হলে ছোট চিপ "{n} বার সরানো হয়েছে"।
- `kind == ADHOC` হলে "একক এক্সাম" চিপ।

### ৬.১২ শিডিউলড এক্সাম দেওয়া (D1: আসল ইঞ্জিন)
1. কার্ডের **"এক্সামে অংশ নিন" / "এক্সাম শেষ করো" / "প্র্যাকটিস হিসেবে দাও"** → `StartScheduledExamUseCase`:
   - D9: অসমাপ্ত **LIVE** অ্যাটেম্পট থাকলে `StartOrResumeExamAttemptUseCase` সেটা জমা দিয়ে `LIVE_ATTEMPT_SUBMITTED` ফেরত দেয়। তখন এক্সাম স্ক্রিন না খুলে সরাসরি লকড রেজাল্ট (`FindMyAttemptForExamUseCase`, `liveOnly = true`) খুলবে। অসমাপ্ত PRACTICE অ্যাটেম্পট আগের জায়গা থেকে চলবে।
   - `POST /routines/slots/{slot_id}/start` → `{exam_id, is_live, closes_at}`
   - `POST /exams/{exam_id}/attempts` → `ExamAttemptData`। `ATTEMPT_ALREADY_ACTIVE` হলে চলমান অ্যাটেম্পট খুঁজে সেটা খোলে।
   - এরর: `SLOT_NOT_OPEN` → "{সময়}-এ খুলবে"। `SLOT_NO_QUESTIONS` → মেসেজ। `EXAM_EMPTY` → একই মেসেজ।
2. নেভিগেট: `ExamAttempt(attemptId, titleBn, lockResultUntil = if (is_live) closes_at else null)`।
3. **ExamAttemptScreen**-এ বদলাবে শুধু এটুকু: `lockResultUntil` থাকলে উপরে ছোট ব্যানার "গ্রুপ এক্সাম · উইন্ডো শেষ {সময়}"। টাইমার সার্ভারের `duration` অনুযায়ী, তবে **উইন্ডো শেষের বেশি দেখাবে না** (`min(startAt + duration, closesAt)`)। সময় শেষে বিদ্যমান অটো-সাবমিট চলবে। অ্যাপ বন্ধ থাকলে সার্ভারের worker অটো-সাবমিট করে।
4. সাবমিটের পর `ExamResult(attemptId, titleBn, lockResultUntil)`:
   - `lockResultUntil > now` → **লকড ভিউ** (`GroupResultLocked` ডিজাইন): "সাবমিট হয়েছে ✅", গ্রুপ · এক্সাম নাম, "উইন্ডো শেষ হবে আরও {X} পর — তারপর ফলাফল ও অ্যান্সার শিট আনলক হবে", সময় লেগেছে {mm:ss}, অ্যান্সার শিট বাটন (লকড), "গ্রুপ ডিটেইলে ফিরে যাও"। আগের প্ল্যানের সিদ্ধান্ত ৪ অনুযায়ী সঠিক-সংখ্যা বা স্কোর দেখাবে না। কাউন্টডাউন শূন্য হলে স্ক্রিন নিজে থেকেই সাধারণ রেজাল্টে বদলে যায়।
   - অন্যথায় → বিদ্যমান সাধারণ ExamResult (স্কোর, অ্যান্সার শিট, অ্যানালাইসিস)।
   - লক শুধু অ্যাপের দিক থেকে। `ExamAnswerSheet`/`AttemptAnalysis`-এর রাউটেও একই `lockResultUntil` চেক থাকবে, যাতে অন্য পথে (হিস্ট্রি থেকে) ঢুকে লক এড়ানো না যায়। হিস্ট্রি থেকে খুললে স্লটের `closes_at` জানা থাকে না, তাই ঝুঁকি: §১৫।
5. ব্যক্তিগত রুটিনেও একই ফ্লো। এখানে লক দরকার নেই (একাই দিচ্ছ), তাই `lockResultUntil = null`।
6. উইন্ডো শেষে আবার দিলে PRACTICE অ্যাটেম্পট হয়। হিস্ট্রিতে আলাদা এন্ট্রি, লিডারবোর্ডে গোনা হয় না।

### ৬.১৩ লিডারবোর্ড / র‍্যাংকিং (D2)
- এন্ট্রি: উইন্ডো শেষের কার্ড "র‍্যাংকিং ও অ্যান্সার শিট দেখো", GroupPastExams রো, ক্যালেন্ডারের শেষ হওয়া দিনের কার্ড।
- স্ক্রিন: `GroupRanking` ডিজাইন অনুযায়ী `ExamLeaderboardScreen`-এর একটা গ্রুপ ভ্যারিয়েন্ট (বিদ্যমান ভিউমডেল রিইউজ, `GET /exams/{exam_id}/leaderboard?page`)।
- **আসল ডাটা:**
  - "অংশ নিয়েছে {total_participants}/{member_count}"। `member_count` আসে GroupResponse থেকে, তাই রাউটে `groupId` লাগবে।
  - র‍্যাংক লিস্ট: rank, username, score, time_taken। পেজিনেশন: নিচে পৌঁছালে পরের পেজ।
  - "তোমার র‍্যাংক #{n}": entries থেকে নিজের `user_id` খুঁজে বের করা হয়। এখনকার পেজে না থাকলে পরের পেজগুলো খোঁজা হবে যতক্ষণ না পাওয়া যায় (সর্বোচ্চ ৫ পেজ), না পেলে "—"। তুমি LIVE অ্যাটেম্পট না দিলে "তুমি র‍্যাংকিংয়ে নেই"।
  - নিজের রো হাইলাইটেড, ট্যাপ করলে নিজের `ExamAnswerSheet` (অ্যাটেম্পট id আসে `FindMyAttemptForSlotUseCase` থেকে)। অন্যদের রো ক্লিকেবল না।
  - **404** → "র‍্যাংকিং তৈরি হচ্ছে — উইন্ডো শেষ হওয়ার কিছুক্ষণ পরে দেখা যাবে" + রিফ্রেশ বাটন। উইন্ডো এখনও খোলা থাকলে লেখা হবে "উইন্ডো শেষে র‍্যাংকিং আসবে"।
- **ডামি (UI-তে থাকবে, ডাটা ফেক):** "গড় স্কোর" টাইল, "স্কোর বিস্তার" হিস্টোগ্রাম।
  - ডামি ডাটা আসে একটা `GroupExamStatsProvider` interface থেকে। এখন তার ইমপ্লিমেন্টেশন `DummyGroupExamStatsProvider` (examId থেকে নির্দিষ্ট seed, যাতে প্রতিবার একই সংখ্যা দেখায়)। আসল API এলে শুধু DI-তে বদলালেই চলবে।
  - ডেভেলপারদের জন্য কোডে `// DUMMY: replace when GET …/stats exists (GROUP_ROUTINE_PLAN.md §14)` কমেন্ট। ইউজার কোনো "ডেমো" লেবেল দেখবে না।
- গ্রুপের সামগ্রিক (সব এক্সাম মিলিয়ে) লিডারবোর্ড API-তে নেই। বটম ট্যাবের "লিডারবোর্ড" এই কাজের বাইরে, এখনও ComingSoon। §১৪-এ রিকোয়েস্ট আছে।

### ৬.১৪ আগের এক্সাম (GroupPastExams)
- `GET /routines/slots?group_id=&date_to=গতকাল` (নতুন থেকে পুরনো সাজানো হবে ক্লায়েন্টে, কারণ API `opens_at` অনুযায়ী পুরনো থেকে নতুন দেয়) + আজকের যেগুলোর উইন্ডো শেষ। পেজিনেটেড।
- রো: ট্রফি আইকন, টাইটেল, "{তারিখ} · {আমার অবস্থা}"। অবস্থা: `COMPLETED` → "দিয়েছো", `MISSED` → "অংশ নাওনি"। ডিজাইনের "৩৬ জন সাবমিট · গড় ৭২%" অংশে সাবমিট আর গড় দুটোই **ডামি** (`GroupExamStatsProvider`), কারণ স্লট লিস্টে এগুলো আসে না।
- ট্যাপ করলে র‍্যাংকিং (§৬.১৩)।
- ফাঁকা: "এখনও কোনো এক্সাম শেষ হয়নি"।

### ৬.১৫ তারিখ সরানো, একক এক্সাম মুছা, রুটিন মুছা (D7)
`SlotDetail` বটম শিট (যেকোনো স্লট কার্ড ট্যাপ বা লং-প্রেসে): টপিক তালিকা, কনফিগ, উইন্ডো, অবস্থা, আর অনুমতি থাকলে অ্যাকশন।

- **তারিখ সরাও** (দেখাবে যদি `my_status == UPCOMING` আর `opens_at > now`, এবং ব্যক্তিগত স্লটের মালিক অথবা গ্রুপের অ্যাডমিন হও):
  - ডেট পিকার: আজ থেকে। রুটিন স্লটে রুটিনের মেয়াদ (`start_date` … `start_date + duration_days - 1`) পর্যন্ত, ad-hoc স্লটে সীমা নেই।
  - `PATCH …/reschedule {slot_date}` → আপডেট হওয়া Slot। উইন্ডোর সময় একই থাকে।
  - এরর: `SLOT_ALREADY_OPEN`, `DATE_IN_PAST`, `WINDOW_IN_PAST`, `OUTSIDE_ROUTINE_PERIOD`।
- **এক্সাম মুছে দাও** (শুধু `kind == ADHOC`, গ্রুপ অ্যাডমিন): কনফার্ম → `DELETE /routines/slots/{id}`। `SLOT_HAS_ATTEMPTS` হলে মেসেজ।
- **রুটিন মুছে দাও** (রুটিন ট্যাবের ⋮ / GroupSettings / ক্যালেন্ডারের ⋮):
  - কনফার্ম ডায়ালগ: "আগামী যেসব এক্সাম কেউ দেয়নি সেগুলো মুছে যাবে। দেওয়া এক্সাম ও মিস করা এন্ট্রি হিস্ট্রিতে থাকবে।"
  - `routine_id` আসে `GET /routines/active` থেকে → `DELETE /routines/{id}` → স্ন্যাকবার "{removed_slots}টি আসন্ন এক্সাম মুছে গেছে"।
  - এরপর "নতুন রুটিন বানাও" CTA আবার দেখায়।
- ডিজাইনের রুটিন ট্যাবের ✏ "রুটিন এডিট করো" আইকন বদলে হবে ⋮ মেনু: "রুটিন মুছে নতুন বানাও", "রুটিনের বিস্তারিত"।

### ৬.১৬ রুটিন ট্যাব (`MainRoute.Routine`, ডিজাইন Routine / RoutineMedium / RoutineExpanded)
হেডার: "রুটিন" + "{duration_days} দিনের প্ল্যান · {বাকি} দিন বাকি" (বাকি = শেষ তারিখ − আজ) + ⋮। সেগমেন্ট টগল: **ব্যক্তিগত | গ্রুপ** (সিলেক্ট করা ট্যাব `rememberSaveable`)।

**ব্যক্তিগত মোড**
- `GET /routines/active` (ব্যক্তিগত)। `null` হলে ফাঁকা অবস্থা: "এখনও কোনো রুটিন নেই — বিষয় আর দিন বেছে দিলে প্রতিদিনের এক্সাম অটো তৈরি হবে" + **"রুটিন তৈরি করো"** → বিল্ডার (Personal, ROUTINE)।
- আছে: সপ্তাহের স্ট্রিপ (সোম…রবি, তারিখ সংখ্যা)। আগের/পরের সপ্তাহে সোয়াইপ, তবে রুটিনের মেয়াদের মধ্যে। ডিফল্ট আজ সিলেক্টেড। ডট মানে: এক্সাম-দিন। ✓ মানে: `COMPLETED`। লাল ডট মানে: `MISSED`।
- নির্বাচিত দিনের কার্ড ("{বার} — আজকের পরিকল্পনা"):
  - এক্সাম-দিন: সাবজেক্ট + টপিক লেবেল (§৬.১১) + অবস্থা চিপ + দুটো বাটন **"এক্সাম দাও"** (§৬.১২ ফ্লো; ডিজাইনে এটা SubjectSelect-এ যেত, এখন সরাসরি স্লট এক্সাম। অবস্থা অনুযায়ী লেবেল বদলায়: "এক্সাম শেষ করো" / "রেজাল্ট দেখো" / "আবার দাও") আর **"কনসেপ্ট পড়ো"**।
  - এক্সাম-ছাড়া দিন (D8): "আজ এক্সাম নেই — পরের এক্সাম {বার, তারিখ}। প্রস্তুতি নাও: {পরের স্লটের টপিক লেবেল}" + "কনসেপ্ট পড়ো"।
  - মেয়াদের বাইরের দিন: "রুটিনের মেয়াদের বাইরে"।
- "কনসেপ্ট পড়ো": কনসেপ্টস ফিচার কোডে এখনও নেই, তাই আপাতত "শীঘ্রই আসছে" স্ন্যাকবার। পরে সেই অধ্যায়/টপিকের কনসেপ্ট স্ক্রিনে ডিপ লিংক হবে।

**গ্রুপ মোড**
- `GET /routines/slots?date_from={সপ্তাহের শুরু}&date_to={সপ্তাহের শেষ}` (আমার সব গ্রুপ মিলিয়ে, ব্যক্তিগত বাদে। `group_id != null` দিয়ে ফিল্টার করা হবে)। সপ্তাহ বদলালে নতুন কল।
- একই স্ট্রিপ। কার্ডে "{গ্রুপের নাম} · গ্রুপ এক্সাম" + টাইটেল/টপিক + উইন্ডো + অবস্থা + **"এক্সামে অংশ নিন"** ইত্যাদি (GroupDetail-এর কার্ডের মতো)।
- উপরে গ্রুপ ফিল্টার চিপ ("সব গ্রুপ" / প্রতিটা গ্রুপ), যা `group_id` প্যারাম দিয়ে চলে।
- নিচে ডিজাইনের নোট: "এটি অ্যাডমিনের তৈরি গ্রুপ রুটিন — নির্ধারিত সময়ে গ্রুপ এক্সাম অটো শুরু হবে। সব মেম্বার একই ক্যালেন্ডার দেখবে।"
- কোনো গ্রুপে নেই: "তুমি কোনো গ্রুপে নেই" + "গ্রুপে যাও" → GroupList।

**অ্যাডাপ্টিভ:** Compact = ডিজাইন Routine (স্ট্রিপ উপরে, কার্ড নিচে)। Medium = RoutineMedium (২ কলাম: বাঁয়ে দিন-তালিকা, ডানে কার্ড)। Expanded = RoutineExpanded (৩ কলাম)। সিদ্ধান্ত হবে `currentWindowAdaptiveInfoV2().windowSizeClass` দিয়ে।

### ৬.১৭ গ্রুপ রুটিন ক্যালেন্ডার (`GroupRoutineCalendar(groupId)`)
রুটিন ট্যাবের গ্রুপ মোডের মতোই, শুধু একটা গ্রুপের (`group_id` ফিক্সড)। ডাটা: `GET /routines/active?group_id=` (রুটিনের মেয়াদ আর স্লট) + সপ্তাহের ad-hoc স্লট (`/routines/slots?group_id=`)। অ্যাডমিনের ⋮ মেনুতে "একক এক্সাম নির্ধারণ করো" আর "রুটিন মুছে দাও"। রুটিন না থাকলে ফাঁকা অবস্থা, GroupDetail-এর মতোই।

### ৬.১৮ নোটিফিকেশন (D4: ইন-অ্যাপ)
- Home-এর বেল → `Notifications` স্ক্রিন। বেলের লাল ডট দেখায় `GET /notifications?unread_only=true&limit=1`-এর `meta_data.total_items > 0` হলে (Home খোলার সময় লোড হয়)।
- লিস্ট: নতুন আগে, পেজিনেটেড। অপঠিত রো বোল্ড আর বাঁয়ে ডট। ফিল্টার চিপ: "সব" / "অপঠিত"।
- ট্যাপ: `POST /notifications/{id}/read` (optimistic) → `target_url` পার্স করে নেভিগেট।
  - `/groups/{id}` → `GroupDetail(id)`
  - `/groups/{id}/join-requests` → `GroupJoinRequests(id)`
  - `/routines…` → রুটিন ট্যাব
  - অজানা বা null → শুধু পড়া হিসেবে মার্ক। অজানা `type`-ও উপেক্ষা।
  - পার্সারটা টেবিল-ভিত্তিক হবে, যাতে নতুন ফরম্যাট সহজে যোগ করা যায়। সার্ভার ঠিক কোন কোন `target_url` পাঠায় তার লিস্ট চাওয়া হয়েছে (§১৪)।
- ফাঁকা: "কোনো নোটিফিকেশন নেই"।
- পুশ (FCM, লগআউটে `token: null`) পরের আলাদা কাজ।

### ৬.১৯ Home-এর এন্ট্রি পয়েন্ট
- কুইক মেনু "গ্রুপ" → GroupList (আছে)।
- "গ্রুপ এক্সাম" কার্ড: `GET /routines/slots?date_from=আজ&date_to=আজ` থেকে প্রথম `OPEN`/`IN_PROGRESS` গ্রুপ স্লট (না পেলে প্রথম `UPCOMING`) → "{গ্রুপ} · {টাইটেল}" + "উইন্ডো খোলা আছে আরও {X}" / "{সময়}-এ খুলবে" → `GroupDetail(groupId)`। কোনো স্লট না থাকলে কার্ড লুকানো থাকবে। ডিজাইনের লেবেল "গ্রুপ এক্সাম · ইনভাইট"-এর বদলে "গ্রুপ এক্সাম · আজ", কারণ ইনভাইট ফ্লো API-তে নেই।
- কুইক মেনু "রুটিন" ও বটম-ন্যাভ "রুটিন" → রুটিন ট্যাব।

### ৬.২০ স্কোপ (`group:use`, `routine:use`) ও RBAC
- `MeData.scopes` আগে থেকেই আসে। স্কোপ না থাকলে: Home-এর "গ্রুপ" মেনু আর গ্রুপ কার্ড লুকানো থাকবে, রুটিন ট্যাবে "তোমার অ্যাকাউন্টে রুটিন ফিচার চালু নেই" দেখাবে। স্কোপ লিস্ট পুরো ফাঁকা হলে (সার্ভারে RBAC সেটআপ না থাকলে) একই আচরণ, অ্যাপ ক্র্যাশ করবে না।
- রানটাইমে `INSUFFICIENT_SCOPE` (403) এলে একই মেসেজ, রিট্রাই নেই। `AUTHZ_UNAVAILABLE` (503) এলে এক্সপোনেনশিয়াল ব্যাকঅফে সর্বোচ্চ ৩ বার রিট্রাই।

---

## ৭. স্টেট আর রিফ্রেশের নিয়ম
- `my_status` সার্ভার-সত্য। তবে উইন্ডো খোলা বা বন্ধ হওয়ার মুহূর্তে স্ক্রিন যেন পুরনো না থাকে, তাই GroupDetail, রুটিন ট্যাব আর Home কার্ড পরের `opens_at`/`closes_at`-এ একবার নিজে থেকে রিলোড করবে (`LaunchedEffect` + delay, `LifecycleResumeEffect`-এ বাতিল/পুনরায় চালু)।
- এক্সাম থেকে ফিরলে (`ON_RESUME`) সংশ্লিষ্ট স্ক্রিন রিফ্রেশ।
- worker বন্ধ থাকলে `MISSED` দেরিতে আসতে পারে। তাই `closes_at` পার হয়ে গেছে কিন্তু স্ট্যাটাস এখনও `OPEN`, এমন স্লটকে UI "উইন্ডো শেষ" হিসেবে দেখাবে (শুধু দেখানোর জন্য, `my_status` বদলানো হবে না)।
- সব রিস্ট্রাকচার-যোগ্য UI স্টেট (সিলেক্ট করা দিন, ট্যাব, সার্চ, ফর্ম ইনপুট) `rememberSaveable` বা `SavedStateHandle`-এ।

---

## ৮. DI (`di/appModule.kt`, বিদ্যমান classic-DSL স্টাইল)
```
single { GroupApiService(get()) }; single { RoutineApiService(get()) }; single { NotificationApiService(get()) }
single<GroupRepository> { GroupRepositoryImpl(get()) }
single<RoutineRepository> { RoutineRepositoryImpl(get()) }
single<NotificationRepository> { NotificationRepositoryImpl(get()) }
single<GroupExamStatsProvider> { DummyGroupExamStatsProvider() }   // DUMMY (§6.13)
factory { … সব ইউজ কেস … }
viewModel { … } / viewModel { params -> …(params.get(), …) }
```
সরবে: `FakeGroupExamRepository` binding, `CreatedGroupDao`, `GroupRoutineDao`, আর পুরনো গ্রুপ ইউজ কেস (`FetchGroupDetail`, `CreateGroupRoutine` ইত্যাদি)। নেভিগেশন এন্ট্রিগুলো বিদ্যমান `RootNavDisplay`/`MainNavDisplay`-এর স্টাইলে।

---

## ৯. স্ট্রিং ও লোকালাইজেশন
- প্রতিটা লেখা `values/strings.xml` (বাংলা) + `values-en/strings.xml`-এ। প্রিফিক্স: `group_*`, `routine_*`, `slot_*`, `notif_*`, `err_*`।
- সংখ্যা ও তারিখ বাংলা ফরম্যাটারে। বার: সোম, মঙ্গল, বুধ, বৃহ, শুক্র, শনি, রবি (ডিজাইন অনুযায়ী সোমবার থেকে শুরু)।
- সার্ভারের `warnings[]`, নোটিফিকেশনের `title`/`message` সার্ভারের ভাষাতেই দেখাবে।

---

## ১০. অ্যাডাপ্টিভ লেআউট
- লিস্ট স্ক্রিন (GroupList, Explore, Members, PastExams, JoinRequests, Notifications): `GridCells.Fixed(adaptiveColumnCount())`।
- ফর্ম ও বিল্ডার (Create/Join শিট, Settings, বিল্ডারের ৪ ধাপ): `widthIn(max = 600.dp)` দিয়ে মাঝখানে (আগের প্ল্যানের সিদ্ধান্ত ১০)।
- GroupDetail ও র‍্যাংকিং: Compact-এ এক কলাম। Medium বা তার বেশিতে ২ কলাম (বাঁয়ে এক্সাম স্ট্যাটাস + আগের এক্সাম, ডানে সদস্য), সাইজ ক্লাস দিয়ে। এর ডিজাইন নেই, তাই ডিজাইন যোগ না হওয়া পর্যন্ত ৬০০dp-তে মাঝখানে রাখাই ডিফল্ট।
- রুটিন ট্যাব: §৬.১৬-এর Compact/Medium/Expanded ডিজাইন।
- কোনো হাতে লেখা dp ব্রেকপয়েন্ট নয়।

---

## ১১. টেস্ট (prostuti-android-e2e-tests অনুযায়ী)
- **ApiService (MockEngine):** প্রতিটা এন্ডপয়েন্টের সফল রেসপন্স পার্স। প্রতিটা ডকুমেন্টেড এরর কোড `ApiResponse.Failure.errorCode`-এ আসে কিনা। UTC সময় পার্স। অজানা enum মান (`exam_type: "SCHEDULED"`, নতুন `type`) ক্র্যাশ না করা।
- **ইউজ কেস / ইউটিল:** রুটিন রিকোয়েস্ট বিল্ডার (সাবজেক্ট/অধ্যায়/টপিক সিলেকশন → id লিস্ট), ফোন নরমালাইজেশন, `DescribeTopicGrouping`, `DhakaTime` (মধ্যরাত, ১৪৪০, ডিভাইস টাইমজোন ঢাকা না হলে), নোটিফিকেশন `target_url` পার্সার, `StartScheduledExam`-এর `ATTEMPT_ALREADY_ACTIVE` শাখা, লিডারবোর্ডে নিজের র‍্যাংক খোঁজা।
- **ViewModel (Turbine, ফেক রিপো):** প্রতিটা স্ক্রিনের লোড/এরর/সাকসেস। GroupDetail-এর প্রতিটা কার্ড অবস্থা (অ্যাডমিন বনাম সদস্য × রুটিন আছে/নেই × স্লট অবস্থা)। বিল্ডারের ধাপ-ভ্যালিডেশন। লকড রেজাল্টের কাউন্টডাউন শেষে আনলক।
- **Compose UI:** GroupDetail-এর অবস্থাগুলো, Explore বাটনের ৪টা অবস্থা, মেম্বার মেনু (অ্যাডমিন দেখে, সদস্য দেখে না), বিল্ডার ধাপ ৩-এ ব্যক্তিগত মোডে উইন্ডো লুকানো আর ADHOC মোডে তারিখ ফিল্ড, প্রিভিউর warnings, লকড রেজাল্ট।
- গ্রুপ স্ক্রিনের যেসব পুরনো androidTest `FakeGroupExamRepository`-র ওপর চলে, সেগুলো নতুন ফেক রিপো দিয়ে আপডেট হবে।
- ইমুলেটরে ম্যানুয়াল টেস্ট: একসাথে একটাই ইমুলেটর (work-rules)। দুই অ্যাকাউন্ট (অ্যাডমিন + সদস্য) দিয়ে পুরো ফ্লো চালানো হবে, পালা করে।

---

## ১২. কাজের ধাপ (ক্রম অনুযায়ী)

| ধাপ | কাজ | কী শেষ হলে ধাপ শেষ |
|---|---|---|
| **P0** | `ExamTypes.SCHEDULED` + অজানা মানে নিরাপদ পার্স। `DhakaTime`। এরর-ম্যাপার। | হিস্ট্রি/লিস্টে SCHEDULED এক্সাম এলেও ক্র্যাশ নেই, টেস্ট পাস |
| **P1** | মডেল, enum, ৩টা ApiService, ৩টা রিপোজিটরি, সব ইউজ কেস + MockEngine টেস্ট | বিল্ড + ইউনিট টেস্ট পাস |
| **P2** | গ্রুপ কোর: GroupList (আমার গ্রুপ), Create/Join শিট, GroupDetail হেডার/সদস্য প্রিভিউ/ইনভাইট কোড, GroupMembers (শুধু দেখা) আসল API-তে। ফেক রিপো আর Room টেবিল সরানো (অনুমতি নিয়ে)। | দুই অ্যাকাউন্টে গ্রুপ তৈরি ও জয়েন কাজ করে |
| **P3** | মেম্বার ম্যানেজমেন্ট: অ্যাড, রোল, রিমুভ, লিভ। GroupSettings (এডিট, কোড রিজেনারেট, ডিলিট)। Explore + জয়েন রিকোয়েস্ট + GroupJoinRequests। | §৬.৩, ৬.৬–৬.৮ সব এরর কোড সহ |
| **P4** | রুটিন বিল্ডার (৪ ধাপ, `owner`/`mode`) + প্রিভিউ। গ্রুপ রুটিন তৈরি। একক এক্সাম তৈরি। | অ্যাডমিন রুটিন ও একক এক্সাম বানাতে পারে, সদস্য স্লট দেখে |
| **P5** | স্লট দেখানো: GroupDetail এক্সাম কার্ড, GroupList ব্যাজ, Home কার্ড, GroupRoutineCalendar, SlotDetail শিট (তারিখ সরানো, মুছা, রুটিন মুছা) | §৬.৪, ৬.৫, ৬.১৫, ৬.১৭, ৬.১৯ |
| **P6** | এক্সাম দেওয়া: `StartScheduledExam`, `lockResultUntil` (Attempt/Result/AnswerSheet/Analysis), লকড ভিউ | লাইভ অ্যাটেম্পট → লকড রেজাল্ট → উইন্ডো শেষে আনলক |
| **P7** | র‍্যাংকিং (আসল + ডামি স্ট্যাটস প্রোভাইডার), GroupPastExams | §৬.১৩–৬.১৪ |
| **P8** | ব্যক্তিগত রুটিন: রুটিন ট্যাব (ব্যক্তিগত + গ্রুপ মোড, Compact/Medium/Expanded), বিল্ডারের Personal মোড | ComingSoon সরে গেছে |
| **P9** | নোটিফিকেশন লিস্ট + বেলের ডট + ডিপ লিংক | §৬.১৮ |
| **P10** | স্কোপ গেটিং, রিফ্রেশ টাইমার (§৭), Compose UI টেস্ট, ইমুলেটরে এন্ড-টু-এন্ড, android-master §২২ চেকলিস্ট | রিপোর্ট সহ শেষ |

P3, P8, P9 একে অপরের ওপর নির্ভর করে না (P1-এর পরে), তাই চাইলে আলাদা সেশনে সমান্তরালে চালানো যায়। কিন্তু সবগুলো `appModule.kt`, `Routes.kt` আর `strings.xml` ছোঁয়, তাই মার্জ কনফ্লিক্ট হবে। ক্রম ধরে করাই নিরাপদ।

---

## ১৩. ডেড কোড / সরানোর তালিকা (ইমপ্লিমেন্টেশনের সময় অনুমতি নিয়ে)
`FakeGroupExamRepository`, `GroupExamRepository`, `CreatedGroupEntity/Dao`, `GroupRoutineEntity/Dao`, পুরনো `Group`/`GroupDetail`/`GroupRoutineSummary`/`CreateGroupRoutineRequest`/`GroupPastExam` মডেল, `GroupRoutinePlanner`, গ্রুপের পুরনো ইউজ কেস, `TimedExamKind.GROUP` আর `TimedExam*`-এর গ্রুপ শাখা, `ExamDummyData`-র গ্রুপ অংশ, `GroupDetailScreen`-এর ডেবাগ "অবস্থা পাল্টাও" চিপ।

---

## ১৪. ব্যাকএন্ডের কাছে API রিকোয়েস্ট (ডামি অংশ আসল করতে)

| # | যা দরকার | কেন / কোন স্ক্রিন | প্রস্তাবিত রূপ |
|---|---|---|---|
| B1 | **এক্সামের গ্রুপ স্ট্যাটস**: জমা দেওয়ার সংখ্যা, গড় স্কোর (%), স্কোর বিস্তার (যেমন ১০টা ১০%-এর বাকেট) | GroupRanking-এর "গড় স্কোর" টাইল আর হিস্টোগ্রাম, GroupDetail-এর উইন্ডো-শেষ কার্ড, GroupPastExams রো। এখন ডামি। | `GET /routines/slots/{slot_id}/stats` → `{submitted_count, member_count, average_score_percent, histogram: [{from, to, count}]}`; অথবা `SlotResponse`-এ `submitted_count`, `average_score_percent` |
| B2 | **লিডারবোর্ডে আমার এন্ট্রি** | আমি প্রথম পেজে না থাকলে "তোমার র‍্যাংক" বের করতে অনেক পেজ আনতে হয় | `ExamLeaderboardData`-এ `my_entry: LeaderboardEntry?` |
| B3 | **স্লটে আমার অ্যাটেম্পট id** | রেজাল্ট/অ্যান্সার শিট খুলতে আলাদা `/exams/attempts/manage` কল লাগে | `SlotResponse`-এ `my_attempt_id` (সর্বশেষ সাবমিট করা) আর `my_live_attempt_id` |
| B4 | **হিস্ট্রিতে স্লটের উইন্ডো** | হিস্ট্রি থেকে খোলা গ্রুপ এক্সামের রেজাল্ট উইন্ডো চলাকালীন লক রাখতে | `ExamHistoryItem`-এ `slot_id`, `closes_at` (SCHEDULED হলে) |
| B5 | **উইন্ডো চলাকালীন রেজাল্ট সার্ভারে লক** (ঐচ্ছিক) | এখন লক শুধু অ্যাপের দিক থেকে। API সরাসরি ডাকলে আগেই স্কোর দেখা যায়। | স্লটের উইন্ডো খোলা থাকলে LIVE অ্যাটেম্পটের result/answer-sheet-এ `409 RESULT_LOCKED_UNTIL_WINDOW_CLOSES` |
| B6 | **গ্রুপের সামগ্রিক লিডারবোর্ড** | সব গ্রুপ এক্সাম মিলিয়ে র‍্যাংক (ডিজাইনে ইঙ্গিত, বটম-ন্যাভ লিডারবোর্ড) | `GET /groups/{id}/leaderboard?page` → `{rank, user_id, username, total_score, exams_taken}` |
| B7 | **মেম্বারের পূর্ণ নাম ও ছবি** | ডিজাইনে "সাদিয়া ইসলাম" আর অ্যাভাটার, API শুধু `username` দেয় | `MemberResponse`, `JoinRequestResponse`, `LeaderboardEntry`-তে `full_name`, `avatar_url` |
| B8 | **অপঠিত নোটিফিকেশন সংখ্যা** | বেলের ব্যাজ। এখন `unread_only&limit=1`-এর `total_items` দিয়ে চালানো হচ্ছে। | `GET /notifications/unread-count` → `{count}` (ঐচ্ছিক) |
| B9 | **`target_url`-এর পূর্ণ তালিকা** | নোটিফিকেশন থেকে সঠিক স্ক্রিনে যাওয়া | ডকুমেন্টেশন: প্রতিটা নোটিফিকেশন ইভেন্টের `target_url` প্যাটার্ন |
| B10 | **লিডারবোর্ড কখন তৈরি হয়** | 404-এর সময় "কতক্ষণ পরে দেখা যাবে" বলতে | ডকুমেন্টেশন (জব কত মিনিট পর পর চলে) অথবা 404-এর `details`-এ `expected_at` |
| B11 | **planner `warnings`-এর বাংলা বা কোড** | এখন ইংরেজি লেখা হুবহু দেখাতে হয় | `warnings: [{code, message, params}]` অথবা `Accept-Language: bn` সাপোর্ট |
| B12 | **অপেক্ষমাণ জয়েন রিকোয়েস্টের সংখ্যা** | GroupDetail-এ অ্যাডমিনের ব্যানার | `GroupResponse`-এ `pending_request_count` (শুধু অ্যাডমিনের জন্য) |
| B13 | **এক্সাম-ছাড়া দিনের পড়ার প্ল্যান** (ঐচ্ছিক) | ডিজাইনে এক্সাম-ছাড়া দিনেও "ব্যক্তিগত অধ্যয়ন — টপিক" ছিল। এখন পরের এক্সামের টপিক দেখানো হচ্ছে (D8)। | রুটিনে `study_days: [{date, topic_ids}]` |
| B14 | **সাবজেক্টে প্রশ্ন সংখ্যা** | বিল্ডার ধাপ ১-এর "৪৫০+ প্রশ্ন" লেখা | কারিকুলাম সাবজেক্টে `approved_mcq_count` (যদি এখনো না থাকে) |

---

## ১৫. ঝুঁকি ও ধরে নেওয়া বিষয় (দরকার হলে বদলাতে বলো)
1. **সিলেকশন → `subject_ids/chapter_ids/topic_ids` ম্যাপিং:** API বলে "খালি মানে পুরো"। কিন্তু একটা সাবজেক্ট পুরো, আর আরেকটার কিছু অধ্যায়, এমন মিশ্র সিলেকশনে সার্ভার পুরো সাবজেক্টের সব অধ্যায় নেয় কিনা, সেটা ডকুমেন্টে স্পষ্ট নয়। P4-এর শুরুতে preview কল দিয়ে যাচাই হবে। না মিললে সার্ভারে সব chapter id স্পষ্ট করে পাঠানো হবে।
2. **level / difficulty / source** রুটিনে পাঠানো হচ্ছে না (`null`), মানে সব লেভেল ও উৎসের প্রশ্ন আসবে। একাডেমিক/ভর্তি ফিল্টার চাইলে ধাপ ৩-এ "আরও অপশন" যোগ করা যাবে।
3. **লকড রেজাল্ট শুধু অ্যাপের দিক থেকে** (B5 না আসা পর্যন্ত)। আর হিস্ট্রি থেকে খুললে B4 না আসা পর্যন্ত লক করা সম্ভব নয়। প্রস্তাব: হিস্ট্রির SCHEDULED আইটেম আজকের হলে `/routines/slots`-এ `exam_id` মিলিয়ে `closes_at` বের করা হবে।
4. **দিনসংখ্যার সীমা** ডিজাইন অনুযায়ী (ব্যক্তিগত ১–৯০, গ্রুপ ৭–৯০), যদিও API ৩৬৬ পর্যন্ত দেয়।
5. **সপ্তাহ শুরু সোমবার** (ডিজাইন অনুযায়ী)। বাংলাদেশে অনেকে শনিবার থেকে সপ্তাহ ধরে, চাইলে বদলানো যাবে।
6. **রুটিন ট্যাবের গ্রুপ মোডে** শুধু গ্রুপের স্লট দেখায়, ব্যক্তিগত বাদ।
7. **worker বন্ধ থাকলে** (সার্ভারে) `MISSED` আর অটো-সাবমিট দেরিতে হয়। অ্যাপ শুধু দেখানোর দিক সামলায় (§৭)।
8. **ডেভ সার্ভারে নতুন স্কোপ seed** (`make rbac-sync args=--grant-new`) না থাকলে সব কলে 403 আসবে। টেস্টের আগে ব্যাকএন্ডকে জানাতে হবে।
9. **পুরনো গ্রুপ রোল** (`MODERATOR`/`TEACHER`) সার্ভারে থেকে গেলে enum পার্স ফেল করতে পারে। তাই `GroupRole` পার্সে অজানা মানকে `MEMBER` ধরা হবে।
