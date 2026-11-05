package ru.kvizcap.quiz.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.onesignal.OneSignal
import kotlinx.coroutines.launch
import ru.kvizcap.quiz.app.data.Lang
import ru.kvizcap.quiz.app.mvi.Effect
import ru.kvizcap.quiz.app.mvi.Screen
import ru.kvizcap.quiz.app.mvi.Store
import ru.kvizcap.quiz.app.startup.LaunchRoute
import ru.kvizcap.quiz.app.startup.StartupGate
import ru.kvizcap.quiz.app.startup.StartupViewModel
import ru.kvizcap.quiz.app.ui.AppRoot
import ru.kvizcap.quiz.app.ui.theme.KvizTheme
import ru.kvizcap.quiz.app.mvi.Intent as Do

class MainActivity : ComponentActivity() {
    private val store: Store by viewModels()
    private val startup: StartupViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Lang.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        hideSystemBars()
        store.syncLanguage(Lang.selected(this), Lang.resolve(this))
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                store.signals.collect { effect ->
                    when (effect) {
                        is Effect.ApplyLanguage -> Lang.apply(this@MainActivity, effect.tag)
                        Effect.Exit -> finish()
                        else -> Unit
                    }
                }
            }
        }
        setContent {
            val state by store.state.collectAsState()
            val route by startup.route.collectAsState()
            LaunchedEffect(route) {
                val canPrompt = when (val current = route) {
                    LaunchRoute.Native -> true
                    is LaunchRoute.Web -> !current.requiresAcceptance
                    LaunchRoute.Loading -> false
                }
                val prefs = (application as KvizApplication).startupPreferences
                if (canPrompt && !prefs.notificationPermissionRequested) {
                    prefs.notificationPermissionRequested = true
                    OneSignal.Notifications.requestPermission(false)
                }
            }
            val dark = when (state.profile.settings.theme) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }
            val lightIcons = dark || state.screen == Screen.Blitz || state.upgradeFx != null
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !lightIcons
                    isAppearanceLightNavigationBars = !lightIcons
                }
            }
            KvizTheme(dark) {
                StartupGate(route, startup::acceptPrivacy) {
                    AppRoot(state, store::dispatch) { finish() }
                }
            }
        }
    }

    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onStart() {
        super.onStart()
        store.dispatch(Do.Foreground)
    }

    override fun onStop() {
        store.flush()
        super.onStop()
    }
}
