package com.prostuti.student.domain.routine

import com.prostuti.student.data.model.enums.TopicGroupingLevel
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.Chapter
import com.prostuti.student.data.model.response.ChapterGroup
import com.prostuti.student.data.model.response.CurriculumLookup
import com.prostuti.student.data.model.response.CurriculumName
import com.prostuti.student.data.model.response.CurriculumNames
import com.prostuti.student.data.model.response.Subject
import com.prostuti.student.data.model.response.Topic
import com.prostuti.student.data.model.response.TopicGrouping
import com.prostuti.student.data.repository.CurriculumRepository
import com.prostuti.student.data.repository.fake.FakeRoutineRepository.Companion.slot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private val phyName = CurriculumName("পদার্থবিজ্ঞান", "Physics")
private val vecName = CurriculumName("ভেক্টর", "Vector")
private val worldName = CurriculumName("ভৌত জগৎ", "Physical world")
private val motionName = CurriculumName("গতি", "Motion")
private val v1Name = CurriculumName("ভেক্টরের যোগ", "Vector addition")
private val v2Name = CurriculumName("ডট গুণন", "Dot product")
private val m1Name = CurriculumName("প্রাসের গতি", "Projectile motion")

/** A group member's phone starts with nothing cached; the names are on the "server". */
private class Curriculum(cachedAtStart: Boolean = false) : CurriculumRepository {
    private val server = CurriculumNames(
        subjects = mapOf("phy" to phyName),
        chapters = mapOf("vec" to vecName, "world" to worldName, "motion" to motionName),
        topics = mapOf("v1" to v1Name, "v2" to v2Name, "m1" to m1Name),
        chapterSubjects = mapOf("vec" to "phy", "world" to "phy", "motion" to "phy"),
        topicChapters = mapOf("v1" to "vec", "v2" to "vec", "m1" to "motion"),
    )
    private var cache = if (cachedAtStart) server else CurriculumNames()
    val fills = mutableListOf<CurriculumLookup>()
    var serverDown = false

    override fun observeSubjects(refresh: Boolean): Flow<ApiResponse<List<Subject>>> = emptyFlow()
    override fun observeChapters(subjectId: String, refresh: Boolean): Flow<ApiResponse<List<Chapter>>> = emptyFlow()
    override fun observeTopics(chapterId: String, refresh: Boolean): Flow<ApiResponse<List<Topic>>> = emptyFlow()
    override suspend fun cachedNames(subjectIds: Set<String>, chapterIds: Set<String>, topicIds: Set<String>) = CurriculumNames(
        subjects = cache.subjects.filterKeys { it in subjectIds },
        chapters = cache.chapters.filterKeys { it in chapterIds },
        topics = cache.topics.filterKeys { it in topicIds },
        chapterSubjects = cache.chapterSubjects.filterKeys { it in chapterIds },
        topicChapters = cache.topicChapters.filterKeys { it in topicIds },
    )
    override suspend fun fillNames(lookup: CurriculumLookup) {
        fills += lookup
        if (!serverDown) cache = server
    }
}

class TopicNamesTest {
    // Vector partly (2 topics), the physical world whole.
    private val twoChapters = slot(id = "a", topicIds = listOf("v1", "v2", "w1", "w2", "w3")).copy(
        topicGrouping = TopicGrouping(
            TopicGroupingLevel.CHAPTERS, "phy",
            listOf(ChapterGroup("vec", listOf("v1", "v2"), isPartial = true), ChapterGroup("world", listOf("w1", "w2", "w3"))),
        )
    )
    private val looseTopics = slot(id = "b", topicIds = listOf("v1", "m1"))

    @Test fun `a member with nothing cached gets the names fetched and sees them`() = runTest {
        val curriculum = Curriculum()
        val labels = DescribeTopicGroupingUseCase(curriculum)(listOf(twoChapters))

        assertEquals(TopicLabel.Chapters(phyName, listOf(vecName, worldName), 0, labels["a"]?.outline), labels["a"])
        val asked = curriculum.fills.single()
        assertEquals(mapOf("phy" to setOf("vec", "world")), asked.chaptersBySubject)
        assertEquals("only the partly covered chapter needs topic names", mapOf("vec" to setOf("v1", "v2")), asked.topicsByChapter)
    }

    @Test fun `a whole chapter shows only its name, a partial one its topics`() = runTest {
        val outline = DescribeTopicGroupingUseCase(Curriculum())(listOf(twoChapters))["a"]?.outline
        assertEquals(
            listOf(
                TopicOutline.Section(phyName, vecName, listOf(v1Name, v2Name), isPartial = true),
                TopicOutline.Section(phyName, worldName, emptyList(), isPartial = false),
            ),
            outline?.sections,
        )
    }

    @Test fun `loose topics are grouped under their chapters`() = runTest {
        val curriculum = Curriculum()
        val label = DescribeTopicGroupingUseCase(curriculum)(listOf(looseTopics))["b"]

        assertEquals(setOf("v1", "m1"), curriculum.fills.single().looseTopicIds)
        assertEquals(
            listOf(
                TopicOutline.Section(phyName, vecName, listOf(v1Name), isPartial = true),
                TopicOutline.Section(phyName, motionName, listOf(m1Name), isPartial = true),
            ),
            label?.outline?.sections,
        )
    }

    @Test fun `labels carry the English names too`() = runTest {
        val label = DescribeTopicGroupingUseCase(Curriculum())(listOf(twoChapters))["a"] as TopicLabel.Chapters
        assertEquals("Physics", label.subject?.inLanguage("en"))
        assertEquals(listOf("Vector", "Physical world"), label.chapters.map { it.inLanguage("en") })
        assertEquals(listOf("ভেক্টর", "ভৌত জগৎ"), label.chapters.map { it.inLanguage("bn") })
    }

    @Test fun `nothing is fetched when everything is cached`() = runTest {
        val curriculum = Curriculum(cachedAtStart = true)
        DescribeTopicGroupingUseCase(curriculum)(listOf(twoChapters, looseTopics))
        assertTrue(curriculum.fills.isEmpty())
    }

    @Test fun `a failed fetch falls back to the topic count`() = runTest {
        val curriculum = Curriculum().apply { serverDown = true }
        val labels = DescribeTopicGroupingUseCase(curriculum)(listOf(twoChapters))
        assertEquals(TopicLabel.Count(5), labels["a"])
        assertNull(labels["a"]?.outline)
        assertTrue("nothing asked for no slots", DescribeTopicGroupingUseCase(curriculum)(emptyList()).isEmpty())
    }
}
