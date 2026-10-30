package com.prostuti.student.data.service.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.prostuti.student.data.model.response.Chapter
import com.prostuti.student.data.model.response.Subject
import com.prostuti.student.data.model.response.Topic

@Database(
    entities = [
        Subject::class, Chapter::class, Topic::class,
        GroupRoutineEntity::class, TimedExamAttemptEntity::class,
        DemoExamWindowEntity::class, CreatedGroupEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun chapterDao(): ChapterDao
    abstract fun topicDao(): TopicDao
    abstract fun examDao(): ExamDao
}
