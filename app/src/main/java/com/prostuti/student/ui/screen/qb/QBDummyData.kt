package com.prostuti.student.ui.screen.qb

// ---------------------------------------------------------------------------
// Local demo models — used by QuestionReaderScreen & ChapterBrowseScreen
// until the real "read without attempt" API is available.
// ---------------------------------------------------------------------------

data class DummyQuestion(
    val id: String,
    val text: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String = "",
    val explanationImageUrl: String? = null
)

data class ChapterItem(
    val id: String,
    val nameBn: String,
    val questionCount: Int
)

// ---------------------------------------------------------------------------
// Dummy chapters (physics-style for demo; real data comes from curriculum API)
// TODO: replace with FetchChaptersUseCase(subjectId) when API is available
// ---------------------------------------------------------------------------
fun dummyChapters(): List<ChapterItem> = listOf(
    ChapterItem("ch1", "১ম অধ্যায়: ভৌত রাশি ও পরিমাপ", 18),
    ChapterItem("ch2", "২য় অধ্যায়: গতিবিদ্যা", 24),
    ChapterItem("ch3", "৩য় অধ্যায়: গতির সূত্র", 20),
    ChapterItem("ch4", "৪র্থ অধ্যায়: কাজ, শক্তি ও ক্ষমতা", 16),
    ChapterItem("ch5", "৫ম অধ্যায়: মহাকর্ষ ও অভিকর্ষ", 22),
    ChapterItem("ch6", "৬ষ্ঠ অধ্যায়: পদার্থের গুণাগুণ", 14),
    ChapterItem("ch7", "৭ম অধ্যায়: পর্যায়বৃত্ত গতি", 19),
    ChapterItem("ch8", "৮ম অধ্যায়: তাপগতিবিদ্যা", 17)
)

// ---------------------------------------------------------------------------
// Dummy questions per chapter (keyed by chapter id)
// TODO: replace with FetchChapterQuestionsUseCase(chapterId, source) when API is available
// ---------------------------------------------------------------------------
fun dummyQuestionsForChapter(chapterId: String): List<DummyQuestion> = when (chapterId) {
    "ch1" -> listOf(
        DummyQuestion("ch1q1", "দৈর্ঘ্যের SI একক কী?", listOf("কিলোগ্রাম", "মিটার", "সেকেন্ড", "ক্যান্ডেলা"), 1,
            explanation = "আন্তর্জাতিক একক পদ্ধতিতে (SI) দৈর্ঘ্যের মূল একক হলো মিটার (m)। কিলোগ্রাম ভরের, সেকেন্ড সময়ের এবং ক্যান্ডেলা দীপন তীব্রতার একক।"),
        DummyQuestion("ch1q2", "একটি পরিমাপের আপেক্ষিক ত্রুটি ০.০৫ হলে শতকরা ত্রুটি কত?", listOf("০.০০৫%", "০.৫%", "৫%", "৫০%"), 2,
            explanation = "শতকরা ত্রুটি = আপেক্ষিক ত্রুটি × ১০০ = ০.০৫ × ১০০ = ৫%"),
        DummyQuestion("ch1q3", "ভরের মাত্রা কোনটি?", listOf("[L]", "[T]", "[M]", "[I]"), 2,
            explanation = "SI পদ্ধতিতে সাতটি মূল মাত্রা আছে। ভরের মাত্রা [M], দৈর্ঘ্যের [L], সময়ের [T] এবং তড়িৎ প্রবাহের [I]।"),
        DummyQuestion("ch1q4", "কোনটি মৌলিক রাশি নয়?", listOf("দৈর্ঘ্য", "সময়", "বেগ", "তাপমাত্রা"), 2,
            explanation = "বেগ = দূরত্ব/সময় — এটি দৈর্ঘ্য ও সময় থেকে নিঃসৃত লব্ধ রাশি, মৌলিক নয়। দৈর্ঘ্য, সময় ও তাপমাত্রা সবই মৌলিক রাশি।"),
        DummyQuestion("ch1q5", "বলের মাত্রীয় সূত্র কোনটি?", listOf("[MLT]", "[MLT⁻²]", "[ML²T⁻²]", "[ML⁻¹T⁻²]"), 1,
            explanation = "বল = ভর × ত্বরণ\n∴ [F] = [M][LT⁻²] = [MLT⁻²]"),
    )
    "ch2" -> listOf(
        DummyQuestion("ch2q1", "সমবেগে চলমান বস্তুর ত্বরণ কত?", listOf("সর্বোচ্চ", "শূন্য", "ঋণাত্মক", "ধনাত্মক"), 1),
        DummyQuestion("ch2q2", "v = u + at সমীকরণে 'a' কীসের প্রতীক?", listOf("বেগ", "সরণ", "ত্বরণ", "সময়"), 2),
        DummyQuestion("ch2q3", "মুক্তভাবে পড়ন্ত বস্তুর ত্বরণ g = কত m/s²?", listOf("8.9", "9.8", "10.8", "11.2"), 1),
        DummyQuestion("ch2q4", "স্কেলার রাশি কোনটি?", listOf("বেগ", "ত্বরণ", "দ্রুতি", "বল"), 2),
        DummyQuestion("ch2q5", "দূরত্ব একটি —", listOf("ভেক্টর রাশি", "স্কেলার রাশি", "মৌলিক রাশি", "লব্ধ রাশি"), 1),
    )
    else -> listOf(
        DummyQuestion("${chapterId}q1", "এই অধ্যায়ের নমুনা প্রশ্ন ১", listOf("বিকল্প ক", "বিকল্প খ", "বিকল্প গ", "বিকল্প ঘ"), 0),
        DummyQuestion("${chapterId}q2", "এই অধ্যায়ের নমুনা প্রশ্ন ২", listOf("বিকল্প ক", "বিকল্প খ", "বিকল্প গ", "বিকল্প ঘ"), 2),
        DummyQuestion("${chapterId}q3", "এই অধ্যায়ের নমুনা প্রশ্ন ৩", listOf("বিকল্প ক", "বিকল্প খ", "বিকল্প গ", "বিকল্প ঘ"), 1),
        DummyQuestion("${chapterId}q4", "এই অধ্যায়ের নমুনা প্রশ্ন ৪", listOf("বিকল্প ক", "বিকল্প খ", "বিকল্প গ", "বিকল্প ঘ"), 3),
    )
}

// ---------------------------------------------------------------------------
// Dummy questions for a full exam read (no attempt)
// TODO: replace with FetchExamQuestionsUseCase(examId) when API is available
// (endpoint needed: GET /mcq/exam/{examId}/questions — no attempt required)
// ---------------------------------------------------------------------------
fun dummyExamQuestions(): List<DummyQuestion> = listOf(
    DummyQuestion("eq1", "আলোর বেগ শূন্য মাধ্যমে কত?", listOf("2×10⁸ m/s", "3×10⁸ m/s", "1×10⁸ m/s", "4×10⁸ m/s"), 1,
        explanation = "শূন্য মাধ্যমে আলোর বেগ c = 3×10⁸ m/s। এটি পদার্থবিজ্ঞানের সর্বোচ্চ সীমার একটি মৌলিক ধ্রুবক।"),
    DummyQuestion("eq2", "নিউটনের গতির কয়টি সূত্র আছে?", listOf("২টি", "৩টি", "৪টি", "৫টি"), 1,
        explanation = "নিউটনের গতিসূত্র তিনটি: (১) জড়তার সূত্র, (২) বলের সূত্র (F = ma), (৩) ক্রিয়া-প্রতিক্রিয়ার সূত্র।"),
    DummyQuestion("eq3", "বলের SI একক কী?", listOf("জুল", "নিউটন", "ওয়াট", "পাস্কাল"), 1,
        explanation = "বলের SI একক নিউটন (N)। জুল শক্তির, ওয়াট ক্ষমতার এবং পাস্কাল চাপের একক।"),
    DummyQuestion("eq4", "ভরবেগের মাত্রা কোনটি?", listOf("[MLT⁻¹]", "[MLT⁻²]", "[ML²T⁻¹]", "[ML⁻¹T]"), 0),
    DummyQuestion("eq5", "কোনটি রক্ষণশীল বল?", listOf("ঘর্ষণ বল", "মহাকর্ষীয় বল", "বায়ু প্রতিরোধ", "সান্দ্রতা বল"), 1),
    DummyQuestion("eq6", "গতিশক্তির সূত্র কোনটি?", listOf("mgh", "½mv²", "mv", "Fs"), 1),
    DummyQuestion("eq7", "তরঙ্গদৈর্ঘ্যের একক কোনটি?", listOf("Hz", "m", "m/s", "J"), 1),
    DummyQuestion("eq8", "শব্দের বেগ বায়ুতে প্রায় কত m/s?", listOf("160", "230", "340", "450"), 2),
    DummyQuestion("eq9", "তড়িৎ ক্ষেত্রের একক কী?", listOf("N/m", "N/C", "N/A", "N/kg"), 1),
    DummyQuestion("eq10", "অর্ধপরিবাহীর ব্যান্ড গ্যাপ সাধারণত কত eV?", listOf("0 – 0.5", "0.5 – 3", "3 – 8", "8 – 15"), 1),
)
