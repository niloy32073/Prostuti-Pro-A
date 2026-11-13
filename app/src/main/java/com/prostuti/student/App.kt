package com.prostuti.student

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.prostuti.student.di.appModule
import com.prostuti.student.util.CrashlyticsTree
import com.prostuti.student.util.CrashlyticsUserSync
import com.prostuti.student.ui.widget.WidgetSync
import com.prostuti.student.ui.widget.WidgetPreviewPublisher
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.android.ext.android.get
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import timber.log.Timber

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Debug: logs go to Logcat. Release: nothing is logged on the device; errors and crashes
        // go to Crashlytics only.
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashlyticsTree(FirebaseCrashlytics.getInstance()))
        }
        startKoin {
            androidContext(this@App)
            androidLogger(if (BuildConfig.DEBUG) Level.INFO else Level.NONE)
            modules(appModule)
        }
        if (!BuildConfig.DEBUG) get<CrashlyticsUserSync>().start()
        // Sign-in and sign-out redraw the home screen widget (it never shows another account's data).
        get<WidgetSync>().start()
        // Android 15+ widget picker: a preview with sample data (published once per app version).
        get<WidgetPreviewPublisher>().start()
    }
}
