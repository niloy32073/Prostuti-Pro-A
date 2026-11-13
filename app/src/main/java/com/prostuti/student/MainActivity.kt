package com.prostuti.student

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import android.graphics.Color
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.content.Intent
import com.prostuti.student.navigation.AppLink
import com.prostuti.student.navigation.AppLinkRequests
import com.prostuti.student.navigation.Auth
import com.prostuti.student.ui.widget.WidgetSync
import org.koin.android.ext.android.inject
import com.prostuti.student.navigation.Intro
import com.prostuti.student.navigation.Main
import com.prostuti.student.navigation.RootNavDisplay
import com.prostuti.student.ui.screen.SharedViewModel
import com.prostuti.student.ui.theme.ProstutiTheme
import com.prostuti.student.data.model.enums.ThemeMode
import androidx.compose.foundation.isSystemInDarkTheme
import com.prostuti.student.util.LocaleManager
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val sharedViewModel: SharedViewModel by viewModel()
    private val appLinks: AppLinkRequests by inject()
    private val widgetSync: WidgetSync by inject()

    // The app's own language default (Bangla) is independent of the device's system locale.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // API 31+ plays the animated mark (avd_prostuti_splash); hold the splash until it has
        // finished, not just until the session is read. Only on a fresh launch — a recreated
        // activity shows no system splash, and holding it would only delay the first frame.
        val holdUntil = if (savedInstanceState == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SystemClock.uptimeMillis() + SPLASH_ANIMATION_MS
        } else {
            0L
        }
        splash.setKeepOnScreenCondition {
            !sharedViewModel.state.value.isChecked || SystemClock.uptimeMillis() < holdUntil
        }
        enableEdgeToEdge()
        // A widget tap: open its screen. Not again after a rotation (the stack already has it).
        if (savedInstanceState == null) openLink(intent)
        setContent {
            val state = sharedViewModel.state.collectAsStateWithLifecycle()
            val darkTheme = when (state.value.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            // Status/navigation bar icons follow the app's theme choice, not only the device's.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme }
                )
                onDispose {}
            }
            ProstutiTheme(darkTheme = darkTheme) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { innerPadding ->
                    if (state.value.isChecked) {
                        val startDestination = when {
                            state.value.isFirstTime -> Intro
                            !state.value.isLoggedIn -> Auth
                            else -> Main
                        }
                        RootNavDisplay(startDestination = startDestination, innerPaddingValues = innerPadding)
                    }
                }
            }
        }
    }

    /** A widget tap while the app is open (single top, see the widget's launch intent). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openLink(intent)
    }

    override fun onStop() {
        super.onStop()
        // Left the app (not a rotation): the home screen widget shows what changed in here.
        if (!isChangingConfigurations) widgetSync.onAppLeft()
    }

    private fun openLink(intent: Intent?) {
        AppLink.parse(intent?.data)?.let(appLinks::request)
    }

    private companion object {
        /** Length of avd_prostuti_splash; keep in sync with windowSplashScreenAnimationDuration. */
        const val SPLASH_ANIMATION_MS = 2_200L
    }
}
