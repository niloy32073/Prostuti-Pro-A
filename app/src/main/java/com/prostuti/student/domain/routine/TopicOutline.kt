package com.prostuti.student.domain.routine

import com.prostuti.student.data.model.enums.TopicGroupingLevel
import com.prostuti.student.data.model.response.CurriculumName
import com.prostuti.student.data.model.response.CurriculumNames
import com.prostuti.student.data.model.response.TopicGrouping

/**
 * One exam's syllabus in full, for the detail sheet and the "get ready" card: a chapter the exam
 * covers whole shows just its name; a partly covered one also lists the topics that are in.
 */
data class TopicOutline(val sections: List<Section>) {
    data class Section(val subject: CurriculumName?, val chapter: CurriculumName?, val topics: List<CurriculumName>, val isPartial: Boolean)

    companion object {
        /** Null when a needed name isn't known, so the caller falls back to the short label. */
        fun of(grouping: TopicGrouping, topicIds: List<String>, names: CurriculumNames): TopicOutline? {
            val sections = when (grouping.level) {
                TopicGroupingLevel.CHAPTER, TopicGroupingLevel.CHAPTERS -> {
                    val subject = grouping.subjectId?.let { names.subjects[it] }
                    grouping.chapters.map { group ->
                        val chapter = names.chapters[group.chapterId] ?: return null
                        val topics = if (group.isPartial) group.topicIds.map { names.topics[it] ?: return null } else emptyList()
                        Section(subject, chapter, topics, group.isPartial)
                    }
                }
                // Loose topics: grouped under their chapters, in the exam's order.
                TopicGroupingLevel.TOPICS -> topicIds.groupBy { names.topicChapters[it] }.map { (chapterId, ids) ->
                    val subject = chapterId?.let { names.chapterSubjects[it] }?.let { names.subjects[it] }
                    Section(subject, chapterId?.let { names.chapters[it] }, ids.map { names.topics[it] ?: return null }, isPartial = true)
                }
            }
            return sections.takeIf { it.isNotEmpty() }?.let(::TopicOutline)
        }
    }
}
