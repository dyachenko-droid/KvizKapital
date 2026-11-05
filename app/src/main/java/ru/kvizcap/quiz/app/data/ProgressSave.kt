package ru.kvizcap.quiz.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.json.*
import ru.kvizcap.quiz.app.core.*
import ru.kvizcap.quiz.app.mvi.*
import java.time.LocalDate

@Serializable
data class QuestionOrder(val id: String, val order: List<Int>)

@Serializable
data class SavedSession(
    val round: RoundState? = null,
    val questions: List<QuestionOrder> = emptyList(),
    val blitz: BlitzState? = null,
    val statements: List<Int> = emptyList(),
    val contentLanguage: String = "ru",
) {
    fun restore(content: Content, now: Long): Pair<RoundState?, BlitzState?> {
        require(contentLanguage in listOf("ru", "en", "be", "kk", "ky", "uz", "tg", "az", "hy", "ro"))
        require((round != null) xor (blitz != null))
        val r = round?.let { value ->
            val hydrated = questions.map { saved ->
                require(saved.order.sorted() == listOf(0, 1, 2, 3))
                RoundQuestion(requireNotNull(content.question(saved.id)), saved.order)
            }
            require(hydrated.isNotEmpty() && hydrated.size <= 5_000 && value.index in hydrated.indices)
            require(questions.map { it.id }.distinct().size == questions.size)
            require(value.records.size == value.index + if (value.phase == Phase.REVEALED) 1 else 0)
            require(value.records.map { it.questionId } == hydrated.take(value.records.size).map { it.question.id })
            require(value.chosen in -1..3 && value.removed.all { it in 0..3 })
            require(value.removed.size in listOf(0, 2) && hydrated[value.index].correctIndex !in value.removed)
            require(value.remainingOnPause in 0..Reducer.QUESTION_MS)
            require(value.hintsSpent in 0..1_000_000_000_000L && value.gained in 0..1_000_000_000_000L && value.delta == value.gained - value.hintsSpent)
            require(value.streak in 0..5_000 && value.bestStreak in 0..5_000)
            require(value.records.all { it.gained in 0..1_000_000_000L })
            require(value.gained == value.records.sumOf { it.gained })
            validateMode(value.mode)
            require(value.mode != RoundMode.Blitz)
            if (value.mode is RoundMode.Errors && value.mode.topic != null) require(hydrated.all { it.question.topic == value.mode.topic })
            value.copy(questions = hydrated, paused = false, deadline = now + value.remainingOnPause, explain = false)
        }
        val b = blitz?.let { value ->
            val items = statements.map { id -> requireNotNull(content.statements.firstOrNull { it.id == id }) }
            require(items.isNotEmpty() && items.size <= 5_000 && statements.distinct().size == statements.size)
            require(value.index >= 0 && value.index == value.answered && value.correct in 0..value.answered)
            require(value.records.size == value.answered && value.correct == value.records.count { it.correct })
            require(value.answered <= 5_000 && value.streak in 0..value.answered && value.bestStreak in 0..value.answered)
            require(value.records.all { it.gained in 0..1_000_000_000L })
            require(value.delta == value.records.sumOf { it.gained })
            require(value.remainingOnPause in 0..Reducer.BLITZ_MS && value.delta >= 0)
            value.copy(items = items, paused = false, endsAt = now + value.remainingOnPause)
        }
        return r to b
    }

    companion object {
        fun capture(state: AppState, language: String, now: Long): SavedSession? {
            state.pendingSession?.let { return it }
            val round = state.round
            val blitz = state.blitz
            if (round == null && blitz == null) return null
            return SavedSession(
                round = round?.copy(questions = emptyList(), paused = true, deadline = 0,
                    remainingOnPause = (if (round.paused) round.remainingOnPause else round.deadline - now).coerceIn(0, Reducer.QUESTION_MS)),
                questions = round?.questions?.map { QuestionOrder(it.question.id, it.order) }.orEmpty(),
                blitz = blitz?.copy(items = emptyList(), paused = true, endsAt = 0,
                    remainingOnPause = (if (blitz.paused) blitz.remainingOnPause else blitz.endsAt - now).coerceIn(0, Reducer.BLITZ_MS)),
                statements = blitz?.items?.map { it.id }.orEmpty(),
                contentLanguage = language,
            )
        }
    }
}

@Serializable
data class SavedGame(val profile: Profile, val session: SavedSession? = null, val language: String = "")

@Serializable
data class BackupDocument(val format: String, val schemaVersion: Int, val createdAt: Long, val appVersion: String, val save: SavedGame)

class IncompatibleBackup : IllegalArgumentException()

object ProgressCodec {
    const val MAX_BYTES = 16 * 1024 * 1024
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private const val FORMAT = "ru.kvizcap.quiz.app/progress"

    fun encodeStored(save: SavedGame): String {
        val profile = json.encodeToJsonElement(Profile.serializer(), save.profile).jsonObject
        return JsonObject(profile + mapOf(
            "native_session" to json.encodeToJsonElement(SavedSession.serializer().nullable, save.session),
            "native_language" to JsonPrimitive(save.language),
        )).toString()
    }

    fun decodeStored(text: String): SavedGame {
        require(text.toByteArray().size <= MAX_BYTES)
        val root = json.parseToJsonElement(text).jsonObject
        val profile = json.decodeFromJsonElement(Profile.serializer(), root)
        validateProfile(profile)
        return SavedGame(profile, root["native_session"]?.takeUnless { it == JsonNull }?.let { json.decodeFromJsonElement(SavedSession.serializer(), it) },
            root["native_language"]?.jsonPrimitive?.contentOrNull.orEmpty()).also { validateLanguage(it.language) }
    }

    fun encodeBackup(save: SavedGame, now: Long, version: String): String =
        json.encodeToString(BackupDocument.serializer(), BackupDocument(FORMAT, 1, now, version, save))

    fun decodeBackup(text: String, content: Content): BackupDocument {
        require(text.toByteArray().size <= MAX_BYTES)
        val doc = json.decodeFromString(BackupDocument.serializer(), text)
        require(doc.format == FORMAT)
        if (doc.schemaVersion != 1) throw IncompatibleBackup()
        require(doc.createdAt >= 0 && doc.appVersion.length in 1..64)
        validateProfile(doc.save.profile)
        validateLanguage(doc.save.language)
        doc.save.session?.restore(content, doc.createdAt)
        return doc
    }

    private fun validateLanguage(tag: String) { require(tag in listOf("", "ru", "en", "be", "kk", "ky", "uz", "tg", "az", "hy", "ro")) }

    fun validateProfile(p: Profile) {
        val max = 1_000_000_000_000_000L
        require(p.capital in 0..max && p.name.length <= 100 && p.avatar in Avatars.all.indices)
        require(p.cells.size == Quarter.SIZE * Quarter.SIZE)
        p.cells.forEach { c ->
            require(c.accruedAt >= 0)
            require(c.building == null || Quarter.buildings.any { it.id == c.building })
            require(c.decor == null || Quarter.decor.any { it.id == c.decor })
            require(c.building == null || c.decor == null)
            require(if (c.building == null) c.level == 0 else c.level in 1..Quarter.MAX_LEVEL)
        }
        require(p.career.size == Career.chapters.size && p.career.all { it.size == Career.LEVELS && it.all { star -> star in -1..3 } })
        require(p.settings.theme in 0..2 && p.settings.defaultDifficulty in 0..2)
        require(p.stats.answered in 0..1_000_000_000 && p.stats.correct in 0..p.stats.answered)
        require(p.stats.earned in 0..max && p.stats.quarterIncome in 0..max && p.stats.bestStreak >= 0 && p.stats.blitzBest >= 0 && p.stats.marathonBest >= 0)
        require(p.dailyStreak in 0..1_000_000_000 && p.dailyBest in 0..1_000_000_000 && p.dailyTotal in 0..1_000_000_000 && p.dailyLastDay >= -1)
        require(p.rankSeen in Ranks.all.indices && p.interests.all { key -> TopicId.entries.any { it.key == key } })
        require(p.achievements.all { id -> Achievements.all.any { it.id == id } })
        require(p.stats.perTopic.values.all { it.answered in 0..1_000_000_000 && it.correct in 0..it.answered })
        require(p.seen.size <= 20_000 && p.seen.values.all { it in 0..1_000_000_000 } && p.mistakes.size <= 60 && p.mistakes.distinct().size == p.mistakes.size)
        require(p.assetsHistory.size <= 90 && p.assetsHistory.all { (day, amount) -> runCatching { LocalDate.ofEpochDay(day) }.isSuccess && amount in 0..max })
        require(p.history.size <= 20 && p.history.map { it.id }.distinct().size == p.history.size)
        p.history.forEach { h ->
            require(h.id.isNotBlank() && h.finishedAt >= 0)
            val r = h.result
            validateMode(r.mode)
            require(r.records.size in 1..5_000 && r.total == r.records.size && r.correct == r.records.count { it.correct } && r.stars in 0..3)
            require(r.records.all { it.gained >= 0 && it.question.length <= 20_000 && it.explanation.length <= 20_000 && (it.chosen?.length ?: 0) <= 20_000 && it.correctAnswer.length <= 20_000 })
        }
    }
}

private fun validateMode(mode: RoundMode) {
    if (mode is RoundMode.CareerLevel) require(mode.chapter in Career.chapters.indices && mode.level in 0 until Career.LEVELS)
}
