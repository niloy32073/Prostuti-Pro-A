package com.prostuti.student.ui.screen.auth

import com.prostuti.student.data.repository.AuthRepository
import com.prostuti.student.data.repository.fake.FakeAuthRepository
import com.prostuti.student.di.appModule
import org.koin.core.context.GlobalContext
import org.koin.dsl.module

/**
 * Koin is already started by the real [com.prostuti.student.App] for instrumented tests, so auth
 * screen tests must override the real AuthRepository binding in place rather than starting a
 * fresh Koin instance.
 */
internal class AuthKoinOverride {
    val fakeRepository = FakeAuthRepository()
    private val overrideModule = module { single<AuthRepository>(createdAtStart = false) { fakeRepository } }

    fun install() {
        GlobalContext.get().loadModules(listOf(overrideModule), allowOverride = true)
    }

    fun restore() {
        GlobalContext.get().unloadModules(listOf(overrideModule))
        GlobalContext.get().loadModules(listOf(appModule), allowOverride = true)
    }
}
