package com.prostuti.student.di

import android.content.Context
import androidx.room.Room
import com.prostuti.student.data.service.local.AppDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

private const val DATABASE_NAME = "prostuti-db"

// No migrations until the app ships: on a schema change, uninstall/reinstall locally.
// After launch, bump AppDatabase.version and add a Migration for every change.
fun buildDatabase(context: Context): AppDatabase =
    Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME).build()

val databaseModule = module {
    single { buildDatabase(androidContext()) }
    single { get<AppDatabase>().subjectDao() }
    single { get<AppDatabase>().chapterDao() }
    single { get<AppDatabase>().topicDao() }
    single { get<AppDatabase>().groupRoutineDao() }
    single { get<AppDatabase>().timedExamAttemptDao() }
    single { get<AppDatabase>().demoExamWindowDao() }
    single { get<AppDatabase>().createdGroupDao() }
}
