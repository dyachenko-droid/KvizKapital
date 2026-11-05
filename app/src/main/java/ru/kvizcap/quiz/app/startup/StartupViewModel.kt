package ru.kvizcap.quiz.app.startup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.kvizcap.quiz.app.KvizApplication

class StartupViewModel(application: Application) : AndroidViewModel(application) {
    private val coordinator = (application as KvizApplication).launchCoordinator
    private val mutableRoute = MutableStateFlow<LaunchRoute>(LaunchRoute.Loading)
    val route = mutableRoute.asStateFlow()

    init {
        viewModelScope.launch { mutableRoute.value = coordinator.resolve() }
    }

    fun acceptPrivacy() {
        val current = mutableRoute.value as? LaunchRoute.Web ?: return
        if (!current.requiresAcceptance) return
        coordinator.acceptPrivacy()
        mutableRoute.value = LaunchRoute.Native
    }
}
