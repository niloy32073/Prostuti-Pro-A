package com.prostuti.student.data.model.response

/** Names (Bangla and English) of subjects, chapters and topics by id, from the cached curriculum, plus each one's parent. */
data class CurriculumNames(
    val subjects: Map<String, CurriculumName> = emptyMap(),
    val chapters: Map<String, CurriculumName> = emptyMap(),
    val topics: Map<String, CurriculumName> = emptyMap(),
    /** chapter id → its subject id */
    val chapterSubjects: Map<String, String> = emptyMap(),
    /** topic id → its chapter id */
    val topicChapters: Map<String, String> = emptyMap(),
)
