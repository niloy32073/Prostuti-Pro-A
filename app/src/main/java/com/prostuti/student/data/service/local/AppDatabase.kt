package com.prostuti.student.data.service.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.prostuti.student.data.model.response.Chapter
import com.prostuti.student.data.model.response.Subject
import com.prostuti.student.data.model.response.Topic

@Database(
    entities = [
        Subject::class, Chapter::class, Topic::class,
        TimedExamAttemptEntity::class,
        DemoExamWindowEntity::class,
        AttemptProgressEntity::class,
        CurrentUserEntity::class,
        SavedGraphEntity::class,
        BookmarkEntity::class,
        ReportedResourceEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(StringListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun attemptProgressDao(): AttemptProgressDao
    abstract fun subjectDao(): SubjectDao
    abstract fun chapterDao(): ChapterDao
    abstract fun topicDao(): TopicDao
    abstract fun timedExamAttemptDao(): TimedExamAttemptDao
    abstract fun demoExamWindowDao(): DemoExamWindowDao
    abstract fun currentUserDao(): CurrentUserDao
    abstract fun savedGraphDao(): SavedGraphDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun reportedResourceDao(): ReportedResourceDao
}
