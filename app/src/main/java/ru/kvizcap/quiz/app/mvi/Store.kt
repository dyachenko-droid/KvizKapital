package ru.kvizcap.quiz.app.mvi

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.kvizcap.quiz.app.core.Content
import ru.kvizcap.quiz.app.data.Haptics
import ru.kvizcap.quiz.app.data.Lang
import ru.kvizcap.quiz.app.data.SoundBoard
import ru.kvizcap.quiz.app.data.Storage
import kotlin.random.Random

class Store(app: Application) : AndroidViewModel(app) {
    private val storage = Storage(app)
    private val sounds = SoundBoard(app)
    private val haptics = Haptics(app)
    private var reducer: Reducer? = null
    private val rnd = Random(System.nanoTime())

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private val _signals = MutableSharedFlow<Effect>(extraBufferCapacity = 8)
    val signals: SharedFlow<Effect> = _signals.asSharedFlow()

    private var saveJob: Job? = null
    private var contentLang = Lang.resolve(app)

    init {
        viewModelScope.launch {
            val (content, profile) = withContext(Dispatchers.IO) { Content.load(app, contentLang) to storage.load() }
            ru.kvizcap.quiz.app.ui.screens.TopicCounts.init(content)
            reducer = Reducer(content)
            dispatch(Intent.Loaded(profile))
            dispatch(Intent.LanguageSynced(Lang.selected(app)))
            ticker()
        }
    }

    fun syncLanguage(selected: String, resolved: String) {
        dispatch(Intent.LanguageSynced(selected))
        if (resolved == contentLang) return
        contentLang = resolved
        viewModelScope.launch {
            val content = withContext(Dispatchers.IO) { Content.load(getApplication(), resolved) }
            ru.kvizcap.quiz.app.ui.screens.TopicCounts.init(content)
            reducer = Reducer(content)
        }
    }

    fun dispatch(intent: Intent) {
        val r = reducer ?: return
        val next = r.reduce(_state.value, intent, System.currentTimeMillis(), rnd)
        _state.value = next.state
        next.effects.forEach { handle(it) }
    }

    private fun handle(effect: Effect) {
        when (effect) {
            Effect.Save -> scheduleSave()
            is Effect.Sound -> sounds.play(effect.sfx)
            is Effect.Haptic -> haptics.buzz(effect.buzz)
            is Effect.ApplyLanguage, Effect.Exit -> _signals.tryEmit(effect)
        }
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(300)
            val p = _state.value.profile
            withContext(Dispatchers.IO) { storage.save(p) }
        }
    }

    private suspend fun ticker() {
        while (viewModelScope.isActive) {
            val s = _state.value
            val active = (s.round != null && s.screen == Screen.Round) || (s.blitz != null && s.screen == Screen.Blitz)
            delay(if (active) 200 else 1_000)
            dispatch(Intent.Tick)
        }
    }

    fun flush() {
        if (_state.value.loaded) storage.save(_state.value.profile)
    }

    override fun onCleared() {
        sounds.release()
        super.onCleared()
    }
}
