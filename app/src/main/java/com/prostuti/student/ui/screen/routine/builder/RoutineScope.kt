package com.prostuti.student.ui.screen.routine.builder

import com.prostuti.student.data.model.response.CurriculumName
import com.prostuti.student.data.model.response.CurriculumNames
import com.prostuti.student.ui.screen.practice.PracticeSubjectGroup
import com.prostuti.student.ui.screen.practice.topicIds

/** Names of everything step 2 loaded, for labelling the preview's days. */
internal fun List<PracticeSubjectGroup>.curriculumNames() = CurriculumNames(
    subjects = associate { it.subjectId to CurriculumName(it.subjectNameBn, it.subjectNameEn) },
    chapters = flatMap { it.chapters }.associate { it.chapter.id to CurriculumName(it.chapter.chapterNameBn, it.chapter.chapterNameEn) },
    topics = flatMap { g -> g.chapters.flatMap { it.topics } }.associate { it.id to CurriculumName(it.nameBn, it.nameEn) },
)

/**
 * What a routine covers, as the API wants it. The API reads `chapter_ids` and `topic_ids` as
 * filters over everything picked ("narrow to these"), so once any part is narrowed, every chapter
 * (or topic) that should stay in has to be listed — not only the partly picked ones.
 */
data class RoutineScope(
    val subjectIds: List<String>,
    val chapterIds: List<String>,
    val topicIds: List<String>,
) {
    val problem: Problem?
        get() = when {
            subjectIds.isEmpty() -> Problem.NOTHING_SELECTED
            subjectIds.size > MAX_SUBJECTS -> Problem.TOO_MANY_SUBJECTS
            chapterIds.size > MAX_CHAPTERS -> Problem.TOO_MANY_CHAPTERS
            topicIds.size > MAX_TOPICS -> Problem.TOO_MANY_TOPICS
            else -> null
        }

    enum class Problem { NOTHING_SELECTED, TOO_MANY_SUBJECTS, TOO_MANY_CHAPTERS, TOO_MANY_TOPICS }

    companion object {
        const val MAX_SUBJECTS = 20
        const val MAX_CHAPTERS = 200
        const val MAX_TOPICS = 500

        /**
         * - every picked subject whole → only `subject_ids`
         * - some subject partly picked → `chapter_ids` = every chapter with a picked topic
         * - and some of those chapters partly picked → `topic_ids` = every picked topic too
         *
         * A subject with no topic picked is left out. A subject with no topics at all (nothing to
         * narrow) counts as whole, and so does a chapter with no topics.
         */
        fun from(groups: List<PracticeSubjectGroup>, selectedTopicIds: Set<String>): RoutineScope {
            val kept = groups.filter { g -> g.topicIds.isEmpty() || g.topicIds.any { it in selectedTopicIds } }
            val subjectIds = kept.map { it.subjectId }
            val everySubjectWhole = kept.all { g -> g.topicIds.all { it in selectedTopicIds } }
            if (everySubjectWhole) return RoutineScope(subjectIds, emptyList(), emptyList())

            // A chapter without topics can't be ticked, so it goes with its subject.
            val chapters = kept.flatMap { it.chapters }.filter { c -> c.topics.isEmpty() || c.topics.any { it.id in selectedTopicIds } }
            val everyChapterWhole = chapters.all { c -> c.topics.all { it.id in selectedTopicIds } }
            val topicIds = if (everyChapterWhole) emptyList() else chapters.flatMap { c -> c.topics.map { it.id }.filter { it in selectedTopicIds } }
            return RoutineScope(subjectIds, chapters.map { it.chapter.id }, topicIds)
        }
    }
}
