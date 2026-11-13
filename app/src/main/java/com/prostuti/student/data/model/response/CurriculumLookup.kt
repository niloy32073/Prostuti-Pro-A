package com.prostuti.student.data.model.response

/**
 * Curriculum ids whose names a screen needs. Parents are given where known, so a whole chapter's or
 * subject's list can be fetched at once; [looseTopicIds] have no known chapter and are fetched one by one.
 */
data class CurriculumLookup(
    val subjectIds: Set<String> = emptySet(),
    /** subject id → chapter ids inside it */
    val chaptersBySubject: Map<String, Set<String>> = emptyMap(),
    /** chapter id → topic ids inside it */
    val topicsByChapter: Map<String, Set<String>> = emptyMap(),
    val looseTopicIds: Set<String> = emptySet(),
) {
    val allChapterIds: Set<String> get() = chaptersBySubject.values.flatten().toSet() + topicsByChapter.keys
    val allTopicIds: Set<String> get() = topicsByChapter.values.flatten().toSet() + looseTopicIds
}
