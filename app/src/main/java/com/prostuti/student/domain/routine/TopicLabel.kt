package com.prostuti.student.domain.routine

import com.prostuti.student.data.model.enums.TopicGroupingLevel
import com.prostuti.student.data.model.response.CurriculumName
import com.prostuti.student.data.model.response.CurriculumNames
import com.prostuti.student.data.model.response.TopicGrouping

/**
 * How one exam's topics read (GROUP_ROUTINE_PLAN.md §6.11), before turning into text:
 * - [Chapter]: "{subject} — {chapter}", plus "(partly: …)" when only some of its topics are in
 * - [Chapters]: "{subject} — {chapter1}, {chapter2}…" with "+n" past [MAX_NAMES]
 * - [Topics]: "{topic1}, {topic2}…" with "+n"
 * - [Count]: names not known (e.g. the lookup failed) → "{n} topics"
 *
 * [outline] is the full list behind the short label, when every name is known.
 */
sealed interface TopicLabel {
    val outline: TopicOutline?

    data class Chapter(val subject: CurriculumName?, val chapter: CurriculumName, val partialTopics: List<CurriculumName>, val moreTopics: Int, override val outline: TopicOutline? = null) : TopicLabel
    data class Chapters(val subject: CurriculumName?, val chapters: List<CurriculumName>, val more: Int, override val outline: TopicOutline? = null) : TopicLabel
    data class Topics(val topics: List<CurriculumName>, val more: Int, override val outline: TopicOutline? = null) : TopicLabel
    data class Count(val topicCount: Int) : TopicLabel {
        override val outline: TopicOutline? get() = null
    }

    companion object {
        const val MAX_NAMES = 3

        fun of(grouping: TopicGrouping, topicIds: List<String>, names: CurriculumNames): TopicLabel {
            val subject = grouping.subjectId?.let { names.subjects[it] }
            val fallback = Count(topicIds.size)
            val outline = TopicOutline.of(grouping, topicIds, names)
            return when (grouping.level) {
                TopicGroupingLevel.CHAPTER -> {
                    val group = grouping.chapters.firstOrNull() ?: return fallback
                    val chapter = names.chapters[group.chapterId] ?: return fallback
                    val partial = if (group.isPartial) group.topicIds.mapNotNull { names.topics[it] } else emptyList()
                    Chapter(subject, chapter, partial.take(MAX_NAMES), (partial.size - MAX_NAMES).coerceAtLeast(0), outline)
                }
                TopicGroupingLevel.CHAPTERS -> {
                    val chapters = grouping.chapters.map { names.chapters[it.chapterId] ?: return fallback }
                    if (chapters.isEmpty()) fallback
                    else Chapters(subject, chapters.take(MAX_NAMES), (chapters.size - MAX_NAMES).coerceAtLeast(0), outline)
                }
                TopicGroupingLevel.TOPICS -> {
                    val topics = topicIds.map { names.topics[it] ?: return fallback }
                    if (topics.isEmpty()) fallback else Topics(topics.take(MAX_NAMES), (topics.size - MAX_NAMES).coerceAtLeast(0), outline)
                }
            }
        }
    }
}
