package ru.kvizcap.quiz.app.mvi

import ru.kvizcap.quiz.app.core.Difficulty
import ru.kvizcap.quiz.app.core.Profile
import ru.kvizcap.quiz.app.core.TopicId

sealed interface Intent {
    data class Loaded(val profile: Profile) : Intent
    data class Open(val screen: Screen) : Intent
    data class Tab(val screen: Screen) : Intent
    data object Back : Intent
    data object Tick : Intent
    data object Foreground : Intent

    data class FinishOnboarding(val name: String, val difficulty: Int, val interests: List<String>) : Intent

    data class StartCareer(val chapter: Int, val level: Int) : Intent
    data class StartTopic(val topic: TopicId, val difficulty: Difficulty) : Intent
    data class SetTopicDifficulty(val difficulty: Difficulty) : Intent
    data object StartQuick : Intent
    data object StartMarathon : Intent
    data object StartErrors : Intent
    data object StartDaily : Intent
    data object StartBlitz : Intent
    data object ContinueCareer : Intent

    data class Answer(val index: Int) : Intent
    data object UseFifty : Intent
    data object UseHint : Intent
    data object Skip : Intent
    data object Next : Intent
    data class Explain(val open: Boolean) : Intent
    data class Pause(val paused: Boolean) : Intent
    data object Restart : Intent
    data object QuitRound : Intent
    data object Replay : Intent

    data class BlitzAnswer(val value: Boolean) : Intent
    data object QuitBlitz : Intent

    data object CollectAll : Intent
    data class OpenCell(val cell: Int) : Intent
    data object CloseSheet : Intent
    data class Build(val cell: Int, val item: String) : Intent
    data class Upgrade(val cell: Int) : Intent
    data object DismissUpgrade : Intent
    data object DismissCollect : Intent
    data class DismissToast(val serial: Long) : Intent

    data class SetTheme(val theme: Int) : Intent
    data object ToggleSound : Intent
    data object ToggleHaptics : Intent
    data object ToggleTimer : Intent
    data class SetDefaultDifficulty(val value: Int) : Intent
    data class ConfirmReset(val show: Boolean) : Intent
    data object ResetProgress : Intent
    data class ConfirmExit(val show: Boolean) : Intent
    data class SetAvatar(val index: Int) : Intent
    data class SetName(val name: String) : Intent
    data class SetLanguage(val tag: String) : Intent
    data class LanguageSynced(val tag: String) : Intent
}

enum class Sfx { TAP, CORRECT, WRONG, COIN, COLLECT, UPGRADE, RANK, TICK, FINISH }

enum class Buzz { LIGHT, CONFIRM, REJECT, HEAVY }

sealed interface Effect {
    data object Save : Effect
    data class Sound(val sfx: Sfx) : Effect
    data class Haptic(val buzz: Buzz) : Effect
    data class ApplyLanguage(val tag: String) : Effect
    data object Exit : Effect
}
