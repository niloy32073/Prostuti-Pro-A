package com.prostuti.student.domain.routine

import com.prostuti.student.data.model.enums.TopicGroupingLevel
import com.prostuti.student.data.model.response.CurriculumLookup
import com.prostuti.student.data.model.response.CurriculumNames
import com.prostuti.student.data.model.response.RoutineSlot
import com.prostuti.student.data.repository.CurriculumRepository

/**
 * A [TopicLabel] per slot id. Names come from the cached curriculum; whatever isn't cached yet is
 * fetched first. Slots only carry ids, and a group member (unlike the admin who built the routine)
 * usually hasn't opened those subjects, so without the fetch they'd only see a topic count.
 */
class DescribeTopicGroupingUseCase(private val curriculum: CurriculumRepository) {
    suspend operator fun invoke(slots: List<RoutineSlot>): Map<String, TopicLabel> {
        if (slots.isEmpty()) return emptyMap()
        val lookup = lookupFor(slots)
        var names = cached(lookup)
        if (names.isMissingAny(lookup)) {
            curriculum.fillNames(lookup)
            names = cached(lookup)
        }
        return slots.associate { it.id to TopicLabel.of(it.topicGrouping, it.topicIds, names) }
    }

    private fun lookupFor(slots: List<RoutineSlot>): CurriculumLookup {
        val grouped = slots.filter { it.topicGrouping.level != TopicGroupingLevel.TOPICS }
        return CurriculumLookup(
            subjectIds = grouped.mapNotNull { it.topicGrouping.subjectId }.toSet(),
            chaptersBySubject = grouped.filter { it.topicGrouping.subjectId != null }
                .groupBy { it.topicGrouping.subjectId!! }
                .mapValues { (_, s) -> s.flatMap { slot -> slot.topicGrouping.chapters.map { it.chapterId } }.toSet() },
            // A whole chapter shows only its name, so only partly covered chapters need topic names.
            topicsByChapter = grouped.flatMap { it.topicGrouping.chapters }.filter { it.isPartial }
                .groupBy { it.chapterId }.mapValues { (_, g) -> g.flatMap { it.topicIds }.toSet() },
            looseTopicIds = slots.filter { it.topicGrouping.level == TopicGroupingLevel.TOPICS }.flatMap { it.topicIds }.toSet(),
        )
    }

    /** Cached names for [lookup], following loose topics up to their chapters and subjects. */
    private suspend fun cached(lookup: CurriculumLookup): CurriculumNames {
        val direct = curriculum.cachedNames(lookup.subjectIds, lookup.allChapterIds, lookup.allTopicIds)
        val looseChapters = lookup.looseTopicIds.mapNotNull { direct.topicChapters[it] }.toSet()
        if (looseChapters.isEmpty()) return direct
        val chapters = curriculum.cachedNames(emptySet(), looseChapters - direct.chapters.keys, emptySet())
        val chapterSubjects = direct.chapterSubjects + chapters.chapterSubjects
        val subjectIds = looseChapters.mapNotNull { chapterSubjects[it] }.toSet() - direct.subjects.keys
        val subjects = curriculum.cachedNames(subjectIds, emptySet(), emptySet())
        return direct.copy(
            subjects = direct.subjects + subjects.subjects,
            chapters = direct.chapters + chapters.chapters,
            chapterSubjects = chapterSubjects,
        )
    }

    private fun CurriculumNames.isMissingAny(lookup: CurriculumLookup): Boolean {
        val looseChapters = lookup.looseTopicIds.mapNotNull { topicChapters[it] }
        return !subjects.keys.containsAll(lookup.subjectIds + looseChapters.mapNotNull { chapterSubjects[it] }) ||
            !chapters.keys.containsAll(lookup.allChapterIds + looseChapters) ||
            !topics.keys.containsAll(lookup.allTopicIds)
    }
}
