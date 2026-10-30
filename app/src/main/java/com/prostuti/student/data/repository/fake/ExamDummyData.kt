package com.prostuti.student.data.repository.fake

import com.prostuti.student.data.model.enums.GroupRole
import com.prostuti.student.data.model.response.Group
import com.prostuti.student.data.model.response.GroupMember
import com.prostuti.student.data.model.response.GroupPastExam
import com.prostuti.student.data.model.response.GroupRoutineSummary
import com.prostuti.student.data.model.response.Option
import com.prostuti.student.data.model.response.Question
import com.prostuti.student.data.model.response.RankRow
import com.prostuti.student.data.model.response.WindowRanking
import com.prostuti.student.util.toBengaliDigits

/**
 * All STATIC dummy content for group/live exams in one place — group names, members, the shared
 * MCQ bank, past-exam and ranking numbers. None of this is Room-backed: it never changes at
 * runtime. What a student actually *does* (routines they create, exams they submit, groups they
 * create/join) lives in Room instead (see `ExamDao`) so it survives the app restarting — this
 * file only seeds the baseline a fresh install starts from.
 *
 * NOTE on declaration order: every `val` below that depends on another (e.g. [pastExamsByGroupId]
 * reading [historicalScoreLabel]) is declared AFTER it — `object` properties initialize top to
 * bottom once, in source order, so a forward reference would read an unset value.
 */
object ExamDummyData {

    // ───────────────────────── the shared 25-question MCQ bank ─────────────────────────

    private data class QSeed(val stem: String, val options: List<String>, val correctIndex: Int, val explanation: String)

    private val questionSeeds = listOf(
        QSeed(
            "শূন্য প্রাথমিক বেগে স্থির অবস্থান থেকে নিক্ষিপ্ত একটি বস্তুর সরণ-সময় (s–t) লেখচিত্রের প্রকৃতি কেমন হবে?",
            listOf("মূলবিন্দু থেকে শুরু হওয়া সরলরেখা", "মূলবিন্দু থেকে শুরু হওয়া অধিবৃত্ত", "সময় অক্ষের সমান্তরাল সরলরেখা", "ঋণাত্মক ঢালবিশিষ্ট সরলরেখা"),
            1, "সমত্বরণে s = ½at² — t-এর সাথে s-এর সম্পর্ক দ্বিঘাত, তাই লেখচিত্র অধিবৃত্তাকার।"
        ),
        QSeed(
            "ভরবেগ সংরক্ষণ সূত্র প্রযোজ্য হয় কোন ক্ষেত্রে?",
            listOf("বদ্ধ জড়তা ব্যবস্থায়", "যেকোনো ঘর্ষণযুক্ত তলে", "শুধু উল্লম্ব সংঘর্ষে", "শুধু আপেক্ষিক বেগ শূন্য হলে"),
            0, "বাহ্যিক বল না থাকলে (বদ্ধ ব্যবস্থা) মোট ভরবেগ সংরক্ষিত থাকে।"
        ),
        QSeed(
            "কৌণিক ভরবেগের একক কোনটি?",
            listOf("kg·m/s", "kg·m²/s", "N·m", "J·s²"),
            1, "কৌণিক ভরবেগ L = Iω, এর একক kg·m²/s।"
        ),
        QSeed(
            "সমত্বরণে চলমান বস্তুর বেগ-সময় লেখচিত্র কীরূপ?",
            listOf("অধিবৃত্তাকার", "সরলরেখা (ধনাত্মক ঢাল)", "অনুভূমিক সরলরেখা", "সূক্ষ্ম তরঙ্গাকার"),
            1, "v = u + at — t-এর সাথে v-এর সম্পর্ক রৈখিক।"
        ),
        QSeed(
            "স্থিতিস্থাপক সংঘর্ষে কোন রাশি সংরক্ষিত থাকে?",
            listOf("শুধু ভরবেগ", "শুধু গতিশক্তি", "কোনোটিই না", "ভরবেগ ও গতিশক্তি উভয়ই"),
            3, "স্থিতিস্থাপক সংঘর্ষের সংজ্ঞা অনুযায়ী ভরবেগ ও গতিশক্তি দুটোই সংরক্ষিত থাকে।"
        ),
        QSeed(
            "ক্ষুদ্র বিস্তারের সরল দোলকের পর্যায়কাল নির্ভর করে না কোন রাশির উপর?",
            listOf("দোলকের বিস্তার", "দোলকের দৈর্ঘ্য", "অভিকর্ষজ ত্বরণ", "দোলকের অবস্থানের অক্ষাংশ"),
            0, "T = 2π√(l/g) — বিস্তার (ক্ষুদ্র হলে) পর্যায়কালে প্রভাব ফেলে না।"
        ),
        QSeed(
            "একটি বস্তু সমবেগে সরলরেখায় চললে তার উপর লব্ধি বলের মান কত?",
            listOf("সর্বোচ্চ", "শূন্য", "ত্বরণের সমান", "ভরের সমান"),
            1, "নিউটনের প্রথম সূত্র অনুযায়ী সমবেগে গতির জন্য লব্ধি বল শূন্য।"
        ),
        QSeed(
            "দুটি ভেক্টরের স্কেলার গুণফল শূন্য হলে তাদের মধ্যবর্তী কোণ কত?",
            listOf("০°", "৪৫°", "৯০°", "১৮০°"),
            2, "A·B = AB cosθ = 0 হলে cosθ = 0, অর্থাৎ θ = ৯০°।"
        ),
        QSeed(
            "কাজের মাত্রা সমীকরণ কোনটি?",
            listOf("ML²T⁻²", "MLT⁻²", "ML²T⁻³", "MLT⁻¹"),
            0, "কাজ = বল × দূরত্ব = (MLT⁻²)(L) = ML²T⁻²।"
        ),
        QSeed(
            "স্প্রিং ধ্রুবক k-এর একক কোনটি?",
            listOf("N/m", "N·m", "kg/m", "J/m²"),
            0, "হুকের সূত্র F = -kx থেকে k = F/x, একক N/m।"
        ),
        QSeed(
            "একটি বস্তুকে উলম্বভাবে ছোঁড়া হলে সর্বোচ্চ উচ্চতায় তার বেগ কত?",
            listOf("সর্বোচ্চ", "শূন্য", "প্রাথমিক বেগের অর্ধেক", "ধ্রুব"),
            1, "সর্বোচ্চ বিন্দুতে উলম্ব বেগ-উপাংশ শূন্য হয়ে যায়।"
        ),
        QSeed(
            "দুটি সমান ভরের বস্তুর স্থিতিস্থাপক সংঘর্ষে একটি স্থির থাকলে সংঘর্ষের পর কী ঘটে?",
            listOf("দুটিই একসাথে চলে", "গতিশীল বস্তুটি থেমে যায়, স্থির বস্তুটি আগের বেগে চলে", "দুটিই স্থির হয়ে যায়", "বেগ বিনিময় ছাড়া কিছুই হয় না"),
            1, "সমান ভরের স্থিতিস্থাপক সংঘর্ষে সম্পূর্ণ বেগ বিনিময় ঘটে।"
        ),
        QSeed(
            "টর্ক (ঘাত বল) নির্ভর করে কিসের উপর?",
            listOf("শুধু বলের মানের উপর", "বল ও লম্ব দূরত্বের উপর", "শুধু ভরের উপর", "শুধু কৌণিক বেগের উপর"),
            1, "τ = F × r, বল ও ঘূর্ণন অক্ষ থেকে লম্ব দূরত্বের গুণফল।"
        ),
        QSeed(
            "নিউটনের তৃতীয় সূত্র অনুযায়ী প্রতিক্রিয়া বল সম্পর্কে কোনটি সত্য?",
            listOf("ক্রিয়া বলের চেয়ে ছোট", "ক্রিয়া বলের সমান ও বিপরীত দিকে", "ক্রিয়া বলের সমান দিকে", "শূন্য"),
            1, "ক্রিয়া-প্রতিক্রিয়া জোড়া সমান মানের, বিপরীত দিকের এবং একই রেখায় কাজ করে।"
        ),
        QSeed(
            "একটি কণার সরণ সময়ের সাথে sin ফাংশন অনুসারে পরিবর্তিত হলে গতিটি কী ধরনের?",
            listOf("সমবেগ গতি", "সরল দোলন গতি", "সমত্বরণ গতি", "বৃত্তীয় গতি"),
            1, "x = A sin(ωt) আকারের গতি সরল দোলন গতির সংজ্ঞা।"
        ),
        QSeed(
            "ঘূর্ণন জড়তার (I) একক কোনটি?",
            listOf("kg·m", "kg·m²", "N·m", "kg/m²"),
            1, "I = Σmr², একক kg·m²।"
        ),
        QSeed(
            "কৌণিক ত্বরণ শূন্য হলে বস্তুর কৌণিক বেগ সম্পর্কে কী বলা যায়?",
            listOf("শূন্য হবে", "ধ্রুব থাকবে", "ক্রমাগত বাড়বে", "ক্রমাগত কমবে"),
            1, "α = 0 হলে ω পরিবর্তিত হয় না, অর্থাৎ ধ্রুব থাকে।"
        ),
        QSeed(
            "দুটি ভেক্টরের লব্ধির মান সর্বোচ্চ হয় কখন?",
            listOf("তারা পরস্পর লম্ব হলে", "তাদের মধ্যবর্তী কোণ ০° হলে", "তাদের মধ্যবর্তী কোণ ১৮০° হলে", "তাদের মান সমান হলে"),
            1, "কোণ ০° হলে R = A + B, যা সর্বোচ্চ মান।"
        ),
        QSeed(
            "শক্তির নিত্যতা সূত্র অনুযায়ী একটি বদ্ধ ব্যবস্থায় মোট শক্তি —",
            listOf("সময়ের সাথে বাড়ে", "সময়ের সাথে কমে", "অপরিবর্তিত থাকে", "শূন্য হয়ে যায়"),
            2, "শক্তি এক রূপ থেকে আরেক রূপে রূপান্তরিত হতে পারে, কিন্তু বদ্ধ ব্যবস্থায় মোট শক্তি অপরিবর্তিত থাকে।"
        ),
        QSeed(
            "একটি বস্তু অভিকর্ষের প্রভাবে মুক্তভাবে পড়লে তার ত্বরণ নির্ভর করে —",
            listOf("বস্তুর ভরের উপর", "বস্তুর আকৃতির উপর", "কোনোটির উপরেই নির্ভর করে না (বাতাসের বাধা উপেক্ষা করে)", "বস্তুর রঙের উপর"),
            2, "মুক্তভাবে পড়ন্ত সব বস্তুর ত্বরণ (g) ভর নির্বিশেষে সমান (বায়ুর বাধা উপেক্ষিত হলে)।"
        ),
        QSeed(
            "ক্ষমতার সঠিক মাত্রা সমীকরণ কোনটি?",
            listOf("ML²T⁻³", "ML²T⁻²", "MLT⁻²", "ML²T⁻¹"),
            0, "ক্ষমতা = কাজ/সময় = (ML²T⁻²)/T = ML²T⁻³।"
        ),
        QSeed(
            "একটি বস্তুর উপর একাধিক বল ক্রিয়া করে সাম্যাবস্থায় থাকলে তাদের লব্ধি ভেক্টরফল কত?",
            listOf("সর্বোচ্চ মান", "শূন্য", "প্রতিটি বলের সমষ্টির সমান", "নির্দিষ্ট করে বলা যায় না"),
            1, "সাম্যাবস্থায় সকল বলের ভেক্টর সমষ্টি শূন্য হয়।"
        ),
        QSeed(
            "কেন্দ্রমুখী ত্বরণের রাশিমালা কোনটি?",
            listOf("v/r", "v²/r", "v·r", "v²·r"),
            1, "বৃত্তীয় গতিতে কেন্দ্রমুখী ত্বরণ a = v²/r।"
        ),
        QSeed(
            "দুটি সমমানের বিপরীতমুখী ভেক্টরের লব্ধি কত?",
            listOf("দ্বিগুণ", "শূন্য", "অর্ধেক", "মূল মানের সমান"),
            1, "সমমানের বিপরীতমুখী ভেক্টরদ্বয় একে অপরকে বাতিল করে দেয়, লব্ধি শূন্য।"
        ),
        QSeed(
            "ঘাত-বেগ উপপাদ্য (Impulse-momentum theorem) অনুযায়ী ঘাত বলের মান কোনটির সমান?",
            listOf("ভরবেগের পরিবর্তনের সমান", "বেগের পরিবর্তনের সমান", "ত্বরণের সমান", "কাজের সমান"),
            0, "ঘাত বল J = Δp, অর্থাৎ ভরবেগের পরিবর্তনের সমান।"
        )
    )

    val questions: List<Question> = questionSeeds.mapIndexed { qIdx, seed ->
        val mcqId = "q${qIdx + 1}"
        Question(
            mcqId = mcqId,
            options = seed.options.mapIndexed { oIdx, text ->
                Option(id = "${mcqId}_${('a' + oIdx)}", optionText = text, order = oIdx)
            },
            order = qIdx,
            questionText = seed.stem
        )
    }

    val correctAnswerByMcqId: Map<String, String> = questionSeeds.mapIndexed { qIdx, seed ->
        val mcqId = "q${qIdx + 1}"
        mcqId to "${mcqId}_${('a' + seed.correctIndex)}"
    }.toMap()

    val explanationByMcqId: Map<String, String> = questionSeeds.mapIndexed { qIdx, seed ->
        "q${qIdx + 1}" to seed.explanation
    }.toMap()

    /** A single canned "demo attempt" used as my answers for any exam that's already closed in
     * the dummy timeline and that I never actually attempted through the real UI — e.g. the past
     * exams list, or g_dhaka's/live_math_mt02's already-closed window. 18/25 correct, matching
     * the design's own sample score. */
    val historicalMockAnswers: Map<String, String> = run {
        val wrongAt = setOf(2, 6, 10, 13, 18, 21, 24) // 0-based question indices answered wrong/skipped
        questionSeeds.mapIndexed { qIdx, seed ->
            val mcqId = "q${qIdx + 1}"
            val optionIndex = if (qIdx in wrongAt) (seed.correctIndex + 1) % seed.options.size else seed.correctIndex
            mcqId to "${mcqId}_${('a' + optionIndex)}"
        }.toMap()
    }

    val historicalScoreLabel: String by lazy {
        val correct = correctAnswerByMcqId.count { (id, correct) -> historicalMockAnswers[id] == correct }
        "${correct.toBengaliDigits()}/২৫"
    }

    // ───────────────────────── groups ─────────────────────────

    const val GROUP_HSC27 = "g_hsc27"
    const val GROUP_CHEM = "g_chem"
    const val GROUP_DHAKA = "g_dhaka"
    const val GROUP_NEW = "g_new"

    val staticGroups = listOf(
        Group(id = GROUP_HSC27, nameBn = "HSC ব্যাচ ২৭", memberCount = 38, myRole = GroupRole.ADMIN, joinCode = "HSC27X"),
        Group(id = GROUP_CHEM, nameBn = "রসায়ন স্টাডি সার্কেল", memberCount = 12, myRole = GroupRole.MEMBER, joinCode = "CHEM12Y"),
        Group(id = GROUP_DHAKA, nameBn = "ঢাকা কলেজ — একদল বন্ধু", memberCount = 6, myRole = GroupRole.MEMBER, joinCode = "DHK06Z"),
        Group(id = GROUP_NEW, nameBn = "ফিজিক্স ক্র্যাক টিম", memberCount = 4, myRole = GroupRole.ADMIN, joinCode = "PHY04W")
    )

    /** A 6-letter code that joins the student into a brand new demo group (not one of the 4 above). */
    const val PUBLIC_JOIN_CODE = "PHY27X"
    const val PUBLIC_JOIN_GROUP_NAME = "প্রিমিয়াম ব্যাচ গ্রুপ"

    private fun member(name: String, initial: String, isAdmin: Boolean = false, isMe: Boolean = false) =
        GroupMember(id = "m_$name", name = name, initial = initial, isAdmin = isAdmin, isMe = isMe)

    private val namedHsc27Members = listOf(
        member("সাদিয়া ইসলাম", "সা"),
        member("রাফি হাসান", "র", isAdmin = true),
        member("ইমন চৌধুরী", "ই"),
        member("তানিয়া আক্তার", "তা"),
        member("মাহির রহমান", "মা"),
        member("নিফা রহমান", "নি", isAdmin = true, isMe = true)
    )

    private val fillerNamePool = listOf(
        "অর্পা দাস" to "অ", "সাকিব আহমেদ" to "সা", "নুসরাত জাহান" to "নু", "ফাহিম হোসেন" to "ফা",
        "প্রিয়া রায়" to "প্রি", "তাহসিন ইকবাল" to "তা", "মিম খান" to "মি", "রাকিব উদ্দিন" to "রা",
        "সুমাইয়া বিনতে" to "সু", "জুনায়েদ করিম" to "জু", "অনিক মণ্ডল" to "অনি", "লামিয়া ফেরদৌস" to "লা"
    )

    private fun fillerMembers(count: Int, seedOffset: Int = 0): List<GroupMember> =
        (0 until count).map { i ->
            val (name, initial) = fillerNamePool[(i + seedOffset) % fillerNamePool.size]
            member("$name ${i + 1}".takeIf { (i + seedOffset) >= fillerNamePool.size } ?: name, initial)
        }

    /** For a group created/joined at runtime (not one of the 4 fixed demo groups), which has no
     * entry in [membersByGroupId]. */
    fun membersForDynamicGroup(role: GroupRole): List<GroupMember> {
        val me = member("নিফা রহমান", "নি", isAdmin = role == GroupRole.ADMIN, isMe = true)
        return if (role == GroupRole.ADMIN) listOf(me) else listOf(me) + fillerMembers(23, seedOffset = 5)
    }

    val membersByGroupId: Map<String, List<GroupMember>> = mapOf(
        GROUP_HSC27 to namedHsc27Members + fillerMembers(38 - namedHsc27Members.size),
        GROUP_CHEM to fillerMembers(12, seedOffset = 3),
        GROUP_DHAKA to fillerMembers(6, seedOffset = 7),
        GROUP_NEW to listOf(member("নিফা রহমান", "নি", isAdmin = true, isMe = true)) + fillerMembers(3, seedOffset = 1)
    )

    // ───────────────────────── static routines (the 2 groups that already have one) ─────────────────────────

    /** সন্ধ্যা ৭:০০ */
    private const val WINDOW_START_MINUTE = 19 * 60

    /** রাত ১১:০০ */
    private const val WINDOW_END_MINUTE = 23 * 60

    /** বুধ ও শনি — bit 2 (বুধ) ও bit 5 (শনি), Mon=0..Sun=6 */
    private const val WEEK_DAYS_WED_SAT = (1 shl 2) or (1 shl 5)

    val staticRoutineByGroupId: Map<String, GroupRoutineSummary> = mapOf(
        GROUP_HSC27 to GroupRoutineSummary(
            subjectCount = 3, chapterCount = 33, topicCount = 162, routineDurationDays = 30,
            examWeekDaysBitmask = WEEK_DAYS_WED_SAT,
            windowStartMinuteOfDay = WINDOW_START_MINUTE, windowEndMinuteOfDay = WINDOW_END_MINUTE,
            questionCount = 25, markPerQuestion = 1f, negativeMarkingEnabled = true, negativeMark = 0.25f,
            examDurationMinutes = 30
        ),
        GROUP_DHAKA to GroupRoutineSummary(
            subjectCount = 1, chapterCount = 11, topicCount = 48, routineDurationDays = 21,
            examWeekDaysBitmask = WEEK_DAYS_WED_SAT,
            windowStartMinuteOfDay = WINDOW_START_MINUTE, windowEndMinuteOfDay = WINDOW_END_MINUTE,
            questionCount = 25, markPerQuestion = 1f, negativeMarkingEnabled = true, negativeMark = 0.25f,
            examDurationMinutes = 30
        )
    )

    // ───────────────────────── current exam occurrence per group (windows are Room-anchored) ─────────────────────────

    /** examId used for "today's" group-exam occurrence. Works for the 2 static-routine groups and
     * for any group whose admin just created a routine (brand new or user-created). */
    fun currentExamIdFor(groupId: String) = "grp_$groupId"

    data class WindowBaseline(val startOffsetMillis: Long, val endOffsetMillis: Long)

    private const val HOUR = 60 * 60 * 1000L
    private const val MINUTE = 60 * 1000L

    /** First-ever-resolved baseline offsets, relative to "now" the first time each exam is looked
     * up — see `DemoExamWindowEntity`. Group-current ids here cover only the 2 statically-seeded
     * groups; a freshly created routine computes its own baseline from the admin's chosen window. */
    val windowBaselineByExamId: Map<String, WindowBaseline> = mapOf(
        currentExamIdFor(GROUP_HSC27) to WindowBaseline(-HOUR, 3 * HOUR),
        currentExamIdFor(GROUP_DHAKA) to WindowBaseline(-50 * HOUR, -46 * HOUR),
        "live_phy_mt04" to WindowBaseline(-10 * MINUTE, 110 * MINUTE),
        "live_chem_mt03" to WindowBaseline(5 * MINUTE, 125 * MINUTE),
        "live_math_mt02" to WindowBaseline(-48 * HOUR, -46 * HOUR)
    )

    val groupCurrentExamTitle: Map<String, Pair<String, String>> = mapOf(
        GROUP_HSC27 to ("সাপ্তাহিক টেস্ট — পদার্থবিজ্ঞান ১ম পত্র" to "২৫টি প্রশ্ন · প্রতি প্রশ্নে ১ মার্ক · নেগেটিভ ০.২৫"),
        GROUP_DHAKA to ("সাপ্তাহিক টেস্ট — পদার্থবিজ্ঞান ২য় পত্র" to "২৫টি প্রশ্ন · প্রতি প্রশ্নে ১ মার্ক · নেগেটিভ ০.২৫")
    )

    data class LiveExamSeed(
        val examId: String,
        val titleBn: String,
        val totalParticipants: Int,
        val questionCount: Int = 25,
        val markPerQuestion: Float = 1f,
        val negativeMark: Float = 0.25f,
        val examDurationMinutes: Int = 30
    )

    val liveExams = listOf(
        LiveExamSeed("live_phy_mt04", "পদার্থবিজ্ঞান — মডেল টেস্ট ০৪", totalParticipants = 2410),
        LiveExamSeed("live_chem_mt03", "রসায়ন — মডেল টেস্ট ০৩", totalParticipants = 1840),
        LiveExamSeed("live_math_mt02", "উচ্চতর গণিত — মডেল টেস্ট ০২", totalParticipants = 1520)
    )

    // ───────────────────────── past exams (always closed, purely static) ─────────────────────────

    val pastExamsByGroupId: Map<String, List<GroupPastExam>> = mapOf(
        GROUP_HSC27 to listOf(
            GroupPastExam("past_hsc27_1", "সাপ্তাহিক টেস্ট — রসায়ন ১ম পত্র", "১২ অক্টোবর", 36, 38, 72, historicalScoreLabel),
            GroupPastExam("past_hsc27_2", "সাপ্তাহিক টেস্ট — উচ্চতর গণিত ১ম পত্র", "৫ অক্টোবর", 33, 38, 65, historicalScoreLabel),
            GroupPastExam("past_hsc27_3", "সাপ্তাহিক টেস্ট — পদার্থবিজ্ঞান ২য় পত্র", "২৮ সেপ্টেম্বর", 38, 38, 58, historicalScoreLabel),
            GroupPastExam("past_hsc27_4", "সাপ্তাহিক টেস্ট — জীববিজ্ঞান ১ম পত্র", "২১ সেপ্টেম্বর", 35, 38, 61, historicalScoreLabel),
            GroupPastExam("past_hsc27_5", "সাপ্তাহিক টেস্ট — রসায়ন ২য় পত্র", "১৪ সেপ্টেম্বর", 31, 38, 55, null),
            GroupPastExam("past_hsc27_6", "সাপ্তাহিক টেস্ট — উচ্চতর গণিত ২য় পত্র", "৭ সেপ্টেম্বর", 34, 38, 70, historicalScoreLabel),
            GroupPastExam("past_hsc27_7", "সাপ্তাহিক টেস্ট — পদার্থবিজ্ঞান ১ম পত্র", "৩১ আগস্ট", 30, 38, 63, null),
            GroupPastExam("past_hsc27_8", "সাপ্তাহিক টেস্ট — জীববিজ্ঞান ২য় পত্র", "২৪ আগস্ট", 32, 38, 59, historicalScoreLabel),
            GroupPastExam("past_hsc27_9", "সাপ্তাহিক টেস্ট — রসায়ন ১ম পত্র", "১৭ আগস্ট", 29, 38, 52, historicalScoreLabel),
            GroupPastExam("past_hsc27_10", "সাপ্তাহিক টেস্ট — উচ্চতর গণিত ১ম পত্র", "১০ আগস্ট", 27, 38, 49, historicalScoreLabel)
        ),
        GROUP_DHAKA to listOf(
            GroupPastExam("past_dhaka_1", "সাপ্তাহিক টেস্ট — পদার্থবিজ্ঞান ১ম পত্র", "১৮ অক্টোবর", 5, 6, 58, historicalScoreLabel)
        )
    )

    // ───────────────────────── ranking numbers, keyed by any exam id (current or past) ─────────────────────────

    data class RankingSeed(
        val submittedCount: Int,
        val totalParticipants: Int,
        val averagePercent: Int,
        val myRank: Int?,
        val histogramBars: List<Int>,
        val myHistogramBarIndex: Int,
        val extraRows: List<RankRow>
    )

    private val smallHistogram = listOf(38, 52, 68, 44, 30, 60, 48, 36)
    private val liveHistogram = listOf(30, 46, 62, 74, 68, 54, 40, 32, 24, 18)

    val rankingByExamId: Map<String, RankingSeed> = mapOf(
        currentExamIdFor(GROUP_HSC27) to RankingSeed(
            submittedCount = 34, totalParticipants = 38, averagePercent = 68, myRank = 4,
            histogramBars = smallHistogram, myHistogramBarIndex = 5,
            extraRows = listOf(
                RankRow(1, "সাদিয়া ইসলাম", "সা", "২৩/২৫", false),
                RankRow(2, "রাফি হাসান", "র", "২২/২৫", false),
                RankRow(3, "ইমন চৌধুরী", "ই", "২০/২৫", false),
                RankRow(5, "তানিয়া আক্তার", "তা", "১৭/২৫", false),
                RankRow(6, "মাহির রহমান", "মা", "১৫/২৫", false)
            )
        ),
        currentExamIdFor(GROUP_DHAKA) to RankingSeed(
            submittedCount = 5, totalParticipants = 6, averagePercent = 58, myRank = 2,
            histogramBars = smallHistogram.takeLast(5), myHistogramBarIndex = 1,
            extraRows = listOf(
                RankRow(1, "অর্পা দাস", "অ", "২১/২৫", false),
                RankRow(3, "সাকিব আহমেদ", "সা", "১৫/২৫", false),
                RankRow(4, "নুসরাত জাহান", "নু", "১৩/২৫", false),
                RankRow(5, "ফাহিম হোসেন", "ফা", "১১/২৫", false)
            )
        ),
        "live_phy_mt04" to RankingSeed(
            submittedCount = 2318, totalParticipants = 2410, averagePercent = 64, myRank = 142,
            histogramBars = liveHistogram, myHistogramBarIndex = 3,
            extraRows = listOf(
                RankRow(1, "সাদিয়া ইসলাম", "সা", "২৫/২৫", false),
                RankRow(2, "রাফি হাসান", "র", "২৪/২৫", false),
                RankRow(3, "ইমন চৌধুরী", "ই", "২৪/২৫", false),
                RankRow(5, "তানিয়া আক্তার", "তা", "২৩/২৫", false),
                RankRow(6, "মাহির রহমান", "মা", "২৩/২৫", false)
            )
        ),
        "live_chem_mt03" to RankingSeed(
            submittedCount = 1690, totalParticipants = 1840, averagePercent = 59, myRank = 310,
            histogramBars = liveHistogram.shuffled(kotlin.random.Random(3)), myHistogramBarIndex = 4,
            extraRows = emptyList()
        ),
        "live_math_mt02" to RankingSeed(
            submittedCount = 1390, totalParticipants = 1520, averagePercent = 55, myRank = 88,
            histogramBars = liveHistogram.shuffled(kotlin.random.Random(7)), myHistogramBarIndex = 2,
            extraRows = emptyList()
        )
    ) + ExamDummyDataPastRankings.build()
}
