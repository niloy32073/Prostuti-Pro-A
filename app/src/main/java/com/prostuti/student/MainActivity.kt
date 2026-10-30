package com.prostuti.student

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.prostuti.student.navigation.Auth
import com.prostuti.student.navigation.Intro
import com.prostuti.student.navigation.Main
import com.prostuti.student.navigation.RootNavDisplay
import com.prostuti.student.ui.screen.SharedViewModel
import com.prostuti.student.ui.theme.ProstutiTheme
import com.prostuti.student.util.LocaleManager
import org.koin.android.ext.android.getKoin

class MainActivity : ComponentActivity() {
    private val sharedViewModel: SharedViewModel by lazy {
        getKoin().get<SharedViewModel>()
    }

    // The app's own language default (Bangla) is independent of the device's system locale.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state = sharedViewModel.state.collectAsState()
            ProstutiTheme {
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
}
