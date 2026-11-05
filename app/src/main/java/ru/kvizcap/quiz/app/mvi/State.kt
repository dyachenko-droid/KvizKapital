package ru.kvizcap.quiz.app.mvi

import androidx.annotation.StringRes
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import ru.kvizcap.quiz.app.data.SavedSession
import ru.kvizcap.quiz.app.core.Chapter
import ru.kvizcap.quiz.app.core.Difficulty
import ru.kvizcap.quiz.app.core.Profile
import ru.kvizcap.quiz.app.core.RoundQuestion
import ru.kvizcap.quiz.app.core.Statement
import ru.kvizcap.quiz.app.core.TopicId

sealed interface Screen {
    data object Onboarding : Screen
    data object Home : Screen
    data object Topics : Screen
    data object Quarter : Screen
    data object Progress : Screen
    data object Profile : Screen
    data object Career : Screen
    data object Settings : Screen
    data object Round : Screen
    data object Result : Screen
    data object Review : Screen
    data object History : Screen
    data object HistoryReview : Screen
    data object Backup : Screen
    data object Blitz : Screen
    data object BlitzResult : Screen
    data class Topic(val topic: TopicId) : Screen

    val isTab: Boolean get() = this == Home || this == Topics || this == Quarter || this == Progress || this == Profile
}

@Serializable
sealed interface RoundMode {
    @Serializable data class CareerLevel(val chapter: Int, val level: Int) : RoundMode
    @Serializable data class TopicPlay(val topic: TopicId, val difficulty: Difficulty) : RoundMode
    @Serializable data class Training(val topic: TopicId, val difficulty: Difficulty) : RoundMode
    @Serializable data object Quick : RoundMode
    @Serializable data object Marathon : RoundMode
    @Serializable data class Errors(val topic: TopicId? = null) : RoundMode
    @Serializable data object Blitz : RoundMode
    @Serializable data object Daily : RoundMode
}

enum class Phase { ASKING, REVEALED }

@Serializable
data class AnswerRecord(
    val questionId: String,
    val question: String,
    val chosen: String?,
    val correctAnswer: String,
    val correct: Boolean,
    val skipped: Boolean,
    val gained: Long,
    val explanation: String,
    val chosenTruth: Boolean? = null,
    val correctTruth: Boolean? = null,
)

@Serializable
data class RoundState(
    val mode: RoundMode,
    @Transient val questions: List<RoundQuestion> = emptyList(),
    val index: Int = 0,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val delta: Long = 0,
    val base: Long = 0,
    val gained: Long = 0,
    val hintsSpent: Long = 0,
    val usedAnyHint: Boolean = false,
    val records: List<AnswerRecord> = emptyList(),
    val phase: Phase = Phase.ASKING,
    val chosen: Int = -1,
    val lastGain: Long = 0,
    val removed: Set<Int> = emptySet(),
    val hintShown: Boolean = false,
    val explain: Boolean = false,
    val paused: Boolean = false,
    val deadline: Long = 0,
    val remainingOnPause: Long = 0,
    val lastTickSecond: Int = -1,
    val serial: Int = 0,
    val startStreakMultiplier: Double = 1.0,
) {
    val current: RoundQuestion get() = questions[index]
    val isLast: Boolean get() = index >= questions.lastIndex
    val correctCount: Int get() = records.count { it.correct }
    val wasCorrect: Boolean get() = phase == Phase.REVEALED && chosen == current.correctIndex
    val total: Int get() = if (mode == RoundMode.Marathon) records.size else questions.size

    fun multiplierFor(streak: Int): Double = (1.0 + 0.1 * (streak - 1).coerceAtLeast(0)).coerceAtMost(2.0)
    val nextMultiplier: Double get() = multiplierFor(streak + 1)
}

@Serializable
data class RoundResult(
    val mode: RoundMode,
    val correct: Int,
    val total: Int,
    val base: Long,
    val streakBonus: Long,
    val hintsSpent: Long,
    val delta: Long,
    val stars: Int,
    val passed: Boolean,
    val bestStreak: Int,
    val records: List<AnswerRecord>,
    val chapterCompleted: Chapter? = null,
    val usedAnyHint: Boolean = false,
)

@Serializable
data class BlitzState(
    @Transient val items: List<Statement> = emptyList(),
    val records: List<AnswerRecord> = emptyList(),
    val bestStreak: Int = 0,
    val index: Int = 0,
    val streak: Int = 0,
    val correct: Int = 0,
    val answered: Int = 0,
    val delta: Long = 0,
    val endsAt: Long = 0,
    val lastCorrect: Boolean? = null,
    val serial: Int = 0,
    val history: List<Boolean> = emptyList(),
    val paused: Boolean = false,
    val remainingOnPause: Long = 0,
) {
    val current: Statement get() = items[index % items.size]
    val combo: Double get() = (1.0 + 0.1 * (streak - 1).coerceAtLeast(0)).coerceAtMost(2.0)
}

data class BlitzResult(val correct: Int, val answered: Int, val delta: Long, val bestStreak: Int, val record: Boolean)

sealed interface QuarterSheet {
    data class Building(val cell: Int) : QuarterSheet
    data class Shop(val cell: Int) : QuarterSheet
}

data class UpgradeFx(val cell: Int, val kind: String, val from: Int, val to: Int, val incomeBefore: Long, val incomeAfter: Long, val built: Boolean)

sealed interface Toast {
    val serial: Long

    data class AchievementUnlocked(val id: String, override val serial: Long) : Toast
    data class RankUp(val rank: Int, override val serial: Long) : Toast
    data class Message(@StringRes val text: Int, @StringRes val arg: Int?, override val serial: Long) : Toast
}

data class CollectFx(val amount: Long, val serial: Long)

data class AppState(
    val loaded: Boolean = false,
    val profile: Profile = Profile(),
    val stack: List<Screen> = listOf(Screen.Home),
    val round: RoundState? = null,
    val blitz: BlitzState? = null,
    val result: RoundResult? = null,
    val blitzResult: BlitzResult? = null,
    val sheet: QuarterSheet? = null,
    val upgradeFx: UpgradeFx? = null,
    val toasts: List<Toast> = emptyList(),
    val collectFx: CollectFx? = null,
    val now: Long = System.currentTimeMillis(),
    val topicDifficulty: Difficulty = Difficulty.MEDIUM,
    val confirmExit: Boolean = false,
    val confirmReset: Boolean = false,
    val language: String = "",
    val pendingSession: SavedSession? = null,
    val historyReview: RoundHistory? = null,
    val storageError: Boolean = false,
) {
    val screen: Screen get() = stack.last()
    val liveCapital: Long get() = profile.capital + (round?.delta ?: 0) + (blitz?.delta ?: 0)
}

@Serializable
data class RoundHistory(val id: String, val finishedAt: Long, val result: RoundResult)
