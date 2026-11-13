package com.prostuti.student.data.repository.fake

import com.prostuti.student.data.model.response.Option
import com.prostuti.student.data.model.response.Question
import com.prostuti.student.data.model.response.RankRow
import com.prostuti.student.data.model.response.WindowRanking
import com.prostuti.student.util.toBengaliDigits

/**
 * All STATIC dummy content for the live-exam demo in one place: the shared MCQ bank and the
 * ranking numbers. None of this is Room-backed; what a student does (exams they submit) lives in
 * Room (`TimedExamAttemptDao` / `DemoExamWindowDao`) so it survives the app restarting.
 *
 * NOTE on declaration order: `object` properties initialize top to bottom once, in source order,
 * so a `val` that reads another must be declared after it.
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

    data class WindowBaseline(val startOffsetMillis: Long, val endOffsetMillis: Long)

    private const val HOUR = 60 * 60 * 1000L
    private const val MINUTE = 60 * 1000L

    /** First-ever-resolved baseline offsets, relative to "now" the first time each exam is looked
     * up — see `DemoExamWindowEntity`. Group-current ids here cover only the 2 statically-seeded
     * groups; a freshly created routine computes its own baseline from the admin's chosen window. */
    val windowBaselineByExamId: Map<String, WindowBaseline> = mapOf(
        "live_phy_mt04" to WindowBaseline(-10 * MINUTE, 110 * MINUTE),
        "live_chem_mt03" to WindowBaseline(5 * MINUTE, 125 * MINUTE),
        "live_math_mt02" to WindowBaseline(-48 * HOUR, -46 * HOUR)
    )

    data class LiveExamSeed(
        val examId: String,
        val titleBn: String,
        val titleEn: String,
        val totalParticipants: Int,
        val questionCount: Int = 25,
        val markPerQuestion: Float = 1f,
        val negativeMark: Float = 0.25f,
        val examDurationMinutes: Int = 30
    )

    val liveExams = listOf(
        LiveExamSeed("live_phy_mt04", "পদার্থবিজ্ঞান — মডেল টেস্ট ০৪", "Physics — Model Test 04", totalParticipants = 2410),
        LiveExamSeed("live_chem_mt03", "রসায়ন — মডেল টেস্ট ০৩", "Chemistry — Model Test 03", totalParticipants = 1840),
        LiveExamSeed("live_math_mt02", "উচ্চতর গণিত — মডেল টেস্ট ০২", "Higher Mathematics — Model Test 02", totalParticipants = 1520)
    )

    // ───────────────────────── ranking numbers, keyed by exam id ─────────────────────────

    data class RankingSeed(
        val submittedCount: Int,
        val totalParticipants: Int,
        val averagePercent: Int,
        val myRank: Int?,
        val histogramBars: List<Int>,
        val myHistogramBarIndex: Int,
        val extraRows: List<RankRow>
    )

    private val liveHistogram = listOf(30, 46, 62, 74, 68, 54, 40, 32, 24, 18)

    val rankingByExamId: Map<String, RankingSeed> = mapOf(
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
    )
}
