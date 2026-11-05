package ru.kvizcap.quiz.app.mvi

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.kvizcap.quiz.app.core.Content
import ru.kvizcap.quiz.app.data.*
import kotlin.random.Random

/** Actions and durable snapshots are ordered; the answer is shown only after its checkpoint commits. */
class Store(app: Application) : AndroidViewModel(app) {
    private val storage = Storage(app)
    private val sounds = SoundBoard(app)
    private val haptics = Haptics(app)
    private var content: Content? = null
    private var reducer: Reducer? = null
    private val rnd = Random(System.nanoTime())
    private val actions = Channel<Intent>(Channel.UNLIMITED)
    private val _state = MutableStateFlow(AppState())
    val state = _state.asStateFlow()
    private val _signals = MutableSharedFlow<Effect>(extraBufferCapacity = 8)
    val signals = _signals.asSharedFlow()
    private var durable = AppState()
    private var revision = 0L
    private var contentLang = Lang.resolve(app)
    private var sessionLanguage = contentLang
    private var foreground = true

    init {
        viewModelScope.launch {
            loadInitial()
            launch { ticker() }
            for (requested in actions) {
                if (requested == Intent.RetryStorage) {
                    if (!_state.value.storageError) continue
                    if (!durable.loaded) loadInitial() else try {
                        if (foreground) durable = reducer!!.reduce(durable, Intent.Foreground, System.currentTimeMillis(), rnd).state
                        val save = snapshot()
                        val rev = ++revision
                        withContext(Dispatchers.IO) { storage.save(save, rev) }
                        _state.value = durable.copy(storageError = false)
                    } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                    catch (_: Exception) { _state.value = _state.value.copy(storageError = true) }
                    continue
                }
                if (requested !is Intent.ImportProgress && (!durable.loaded || _state.value.storageError)) continue
                try {
                    if (requested is Intent.LanguageSynced && Lang.resolve(app) != contentLang) {
                        contentLang = Lang.resolve(app)
                        content = withContext(Dispatchers.IO) { Content.load(app, contentLang) }
                        reducer = Reducer(content!!)
                        ru.kvizcap.quiz.app.ui.screens.TopicCounts.init(content!!)
                    }
                    val intent = if (requested == Intent.ResumePending) {
                        val session = durable.pendingSession ?: continue
                        val source = withContext(Dispatchers.IO) { Content.load(app, session.contentLanguage) }
                        val (round, blitz) = session.restore(source, System.currentTimeMillis())
                        sessionLanguage = session.contentLanguage
                        Intent.ResumeSaved(round, blitz)
                    } else requested
                    val before = durable
                    val next = reducer!!.reduce(before, intent, System.currentTimeMillis(), rnd)
                    if (before.round == null && before.blitz == null && (next.state.round != null || next.state.blitz != null) && requested != Intent.ResumePending) sessionLanguage = contentLang
                    durable = if (!foreground) reducer!!.reduce(next.state, Intent.Background, System.currentTimeMillis(), rnd).state else next.state
                    val currentRevision = ++revision
                    val clockChanged = intent == Intent.Tick && durable.now / 1000 != before.now / 1000 && (durable.round != null || durable.blitz != null)
                    if (intent is Intent.ImportProgress || durable.profile != before.profile || durable.round != before.round || durable.blitz != before.blitz || durable.pendingSession != before.pendingSession || durable.language != before.language || clockChanged) {
                        val snapshot = snapshot()
                        withContext(Dispatchers.IO) { storage.save(snapshot, currentRevision) }
                    }
                    if (intent is Intent.ImportProgress && intent.language != before.language) Lang.restoreSelection(app, intent.language)
                    // onStop can synchronously advance the checkpoint while the IO writer is suspended.
                    if (revision == currentRevision) _state.value = durable
                    next.effects.forEach(::handle)
                    if (intent is Intent.ImportProgress && intent.language != before.language) _signals.emit(Effect.ApplyLanguage(intent.language))
                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (_: Exception) {
                    durable = reducer?.reduce(durable, Intent.Background, System.currentTimeMillis(), rnd)?.state ?: durable
                    _state.value = _state.value.copy(storageError = true)
                }
            }
        }
    }

    private suspend fun loadInitial() {
        try {
            val loadedContent = withContext(Dispatchers.IO) { Content.load(getApplication(), contentLang) }
            content = loadedContent
            reducer = Reducer(loadedContent)
            ru.kvizcap.quiz.app.ui.screens.TopicCounts.init(loadedContent)
            val save = withContext(Dispatchers.IO) { storage.load() }
            save.session?.let { session ->
                val source = withContext(Dispatchers.IO) { Content.load(getApplication(), session.contentLanguage) }
                session.restore(source, System.currentTimeMillis())
            }
            durable = reducer!!.reduce(AppState(), Intent.Loaded(save.profile), System.currentTimeMillis(), rnd).state
                .copy(pendingSession = save.session, language = Lang.selected(getApplication()))
            _state.value = durable
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { _state.value = _state.value.copy(storageError = true) }
    }

    fun syncLanguage(selected: String, resolved: String) { dispatch(Intent.LanguageSynced(selected)) }

    fun dispatch(intent: Intent) {
        if (intent == Intent.Foreground) foreground = true
        actions.trySend(intent)
    }

    private fun handle(effect: Effect) {
        when (effect) {
            Effect.Save -> Unit // Profile and session were committed together before rendering.
            is Effect.Sound -> if (foreground) sounds.play(effect.sfx)
            is Effect.Haptic -> if (foreground) haptics.buzz(effect.buzz)
            is Effect.ApplyLanguage, Effect.Exit -> _signals.tryEmit(effect)
        }
    }

    fun exportSnapshot(): SavedGame = snapshot()
    private fun snapshot() = SavedGame(durable.profile, SavedSession.capture(durable, sessionLanguage, System.currentTimeMillis()), durable.language)

    suspend fun previewBackup(text: String): BackupDocument {
        val source = requireNotNull(content)
        return withContext(Dispatchers.IO) { ProgressCodec.decodeBackup(text, source) }
    }

    fun retryStorage() { actions.trySend(Intent.RetryStorage) }

    private suspend fun ticker() {
        while (viewModelScope.isActive) {
            delay(if (durable.round != null || durable.blitz != null) 200 else 1_000)
            if (foreground && durable.pendingSession == null) dispatch(Intent.Tick)
        }
    }

    fun flush() {
        foreground = false
        if (!durable.loaded || _state.value.storageError) return
        try {
            durable = reducer!!.reduce(durable, Intent.Background, System.currentTimeMillis(), rnd).state
            val rev = ++revision
            _state.value = durable
            storage.save(snapshot(), rev)
        } catch (_: Exception) { _state.value = _state.value.copy(storageError = true) }
    }

    override fun onCleared() { sounds.release(); actions.close(); super.onCleared() }
}
