package ru.kvizcap.quiz.app.nativeprogress

import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import ru.kvizcap.quiz.app.core.*
import ru.kvizcap.quiz.app.data.*
import ru.kvizcap.quiz.app.mvi.*
import kotlinx.serialization.json.*
import java.io.File
import kotlin.random.Random

class ProgressTest {
    @get:Rule val folder = TemporaryFolder()
    private val content = Content(TopicId.entries.flatMap { topic -> (0..11).map { i -> Question("${topic.key}_$i", topic, Difficulty.MEDIUM, "Question $i", listOf("A", "B", "C", "D"), 2, "Hint", "Explanation") } },
        (0..3).map { Statement(it, "Statement $it", it % 2 == 0, "Explanation $it") })
    private val reducer = Reducer(content)
    private val now = 1_798_999_000_000L
    private fun initial(profile: Profile = Profile(onboarded = true)) = AppState(loaded = true, profile = profile, now = now)
    private fun act(state: AppState, intent: Intent, time: Long = now) = reducer.reduce(state, intent, time, Random(123)).state
    private fun start(profile: Profile = Profile(onboarded = true), mode: Intent = Intent.StartQuick) = act(initial(profile), mode)
    private fun snapshot(s: AppState, time: Long = now) = SavedGame(s.profile, SavedSession.capture(s, "ru", time), "ru")
    private fun resume(save: SavedGame, time: Long = now + 90_000): AppState {
        val selected = save.session!!
        val pair = selected.restore(content, time)
        return act(initial(save.profile).copy(pendingSession = selected), Intent.ResumeSaved(pair.first, pair.second), time)
    }
    private fun complete(input: AppState): AppState {
        var s = input
        repeat(100) {
            val r = s.round ?: return s
            s = if (r.phase == Phase.ASKING) act(s, Intent.Answer(r.current.correctIndex)) else act(s, Intent.Next)
        }
        error("Did not finish")
    }
    private fun roundTrip(save: SavedGame) = ProgressCodec.decodeStored(ProgressCodec.encodeStored(save))

    @Test fun legacyProfileLoadsWithoutLosingCareerQuarterOrStatistics() {
        val p = Profile(onboarded = true, capital = 9000, cells = Profile().cells.toMutableList().also { it[0] = CellState("books", 1, accruedAt = now) }, stats = Stats(answered = 6, correct = 4))
        val legacy = Json.encodeToString(Profile.serializer(), p)
        assertEquals(SavedGame(p), ProgressCodec.decodeStored(legacy))
    }
    @Test fun storedRootRemainsReadableByALegacyProfileReader() {
        val s = start()
        val json = Json { ignoreUnknownKeys = true }
        val decoded = json.decodeFromString(Profile.serializer(), ProgressCodec.encodeStored(snapshot(s)))
        assertEquals(s.profile, decoded)
    }
    @Test fun askingCheckpointRetainsQuestionOrderHintsAndRemainingTimeAfterProcessDeath() {
        var s = start()
        s = act(s, Intent.UseFifty)
        s = act(s, Intent.UseHint)
        val restored = resume(roundTrip(snapshot(s, now + 7_500)))
        assertEquals(s.round!!.questions.map { it.question.id to it.order }, restored.round!!.questions.map { it.question.id to it.order })
        assertEquals(s.round!!.removed, restored.round!!.removed)
        assertTrue(restored.round!!.hintShown)
        assertEquals(12_500, restored.round!!.deadline - restored.now)
        assertEquals(-250, restored.round!!.delta)
    }
    @Test fun revealedAnswerRestoresAndCannotAwardTheSameAnswerTwice() {
        val s = start().let { act(it, Intent.Answer(it.round!!.current.correctIndex)) }
        val resumed = resume(roundTrip(snapshot(s)))
        assertEquals(Phase.REVEALED, resumed.round!!.phase)
        assertEquals(s.round!!.records, resumed.round!!.records)
        val duplicate = act(resumed, Intent.Answer(resumed.round!!.current.correctIndex), resumed.now)
        assertEquals(resumed.profile, duplicate.profile)
        assertEquals(resumed.round, duplicate.round)
    }
    @Test fun finishingAfterRestoreCreditsOnceAndClearsCheckpoint() {
        val before = start()
        val restored = resume(roundTrip(snapshot(before)))
        val done = complete(restored)
        assertNull(done.round)
        assertNull(snapshot(done).session)
        assertEquals(1, done.profile.history.size)
        val again = act(done, Intent.Next)
        assertEquals(done.profile, again.profile)
        assertEquals(done.result!!.delta, done.profile.stats.earned) // no achievement threshold reached
    }
    @Test fun expiryDuringClosedProcessDoesNotConsumeTheRemainingTime() {
        val s = start()
        val save = roundTrip(snapshot(s, now + 19_999))
        val restored = resume(save, now + 50_000_000)
        assertEquals(1, restored.round!!.deadline - restored.now)
        val expired = act(restored, Intent.Tick, restored.now + 1)
        assertEquals(1, expired.round!!.records.size)
        assertFalse(expired.round!!.records.single().correct)
    }
    @Test fun backgroundPausesRoundAndBlitzClocks() {
        val s = act(start(), Intent.Background, now + 3_000)
        assertTrue(s.round!!.paused)
        assertEquals(17_000, s.round!!.remainingOnPause)
        assertEquals(s.round, act(s, Intent.Tick, now + 100_000).round)
        val b = act(act(initial(), Intent.StartBlitz), Intent.Background, now + 5_000)
        assertTrue(b.blitz!!.paused)
        val foreground = act(b, Intent.Foreground, now + 100_000)
        assertEquals(55_000, foreground.blitz!!.endsAt - foreground.now)
    }
    @Test fun abandoningPendingRoundChargesHintsWithoutCreditingUnfinishedReward() {
        var s = act(start(), Intent.UseHint)
        s = act(s, Intent.Answer(s.round!!.current.correctIndex))
        val pending = initial(s.profile).copy(pendingSession = snapshot(s).session)
        val abandoned = act(pending, Intent.AbandonSaved)
        assertEquals(900, abandoned.profile.capital)
        assertNull(abandoned.pendingSession)
        assertTrue(abandoned.profile.history.isEmpty())
    }
    @Test fun topicErrorsNeverIncludeOtherTopicsAndReplayKeepsTopic() {
        val p = Profile(onboarded = true, mistakes = listOf("budget_0", "banks_1", "budget_2"))
        val s = start(p, Intent.StartErrors(TopicId.BUDGET))
        assertEquals(setOf("budget_0", "budget_2"), s.round!!.questions.map { it.question.id }.toSet())
        val replay = act(complete(s), Intent.Replay)
        // All budget errors have been repaired; bank error is not selected as a fallback.
        assertNull(replay.round)
        assertEquals(listOf("banks_1"), replay.profile.mistakes)
    }
    @Test fun restartErrorRoundPreservesItsTopicAndUnansweredMistakes() {
        val p = Profile(onboarded = true, mistakes = listOf("budget_0", "banks_1", "budget_2"))
        val s = act(start(p, Intent.StartErrors(TopicId.BUDGET)), Intent.Restart)
        assertEquals(RoundMode.Errors(TopicId.BUDGET), s.round!!.mode)
        assertTrue(s.round!!.questions.all { it.question.topic == TopicId.BUDGET })
    }
    @Test fun generalErrorTrainingStillUsesAllTopics() {
        val s = start(Profile(onboarded = true, mistakes = listOf("budget_0", "banks_1")), Intent.StartErrors())
        assertEquals(setOf(TopicId.BUDGET, TopicId.BANKS), s.round!!.questions.map { it.question.topic }.toSet())
    }
    @Test fun recommendationsNeedEnoughDataAndUseWeakestTopic() {
        assertNull(weakestTopic(Profile(stats = Stats(perTopic = mapOf("budget" to TopicStat(4, 0))))))
        assertEquals(TopicId.BANKS, weakestTopic(Profile(stats = Stats(perTopic = mapOf("budget" to TopicStat(10, 8), "banks" to TopicStat(5, 1))))))
        assertNull(weakestTopic(Profile(stats = Stats(perTopic = mapOf("budget" to TopicStat(10, 9))))))
    }
    @Test fun trainingPrioritizesErrorsAndFillsWithDistinctQuestionsFromSameTopic() {
        val s = start(Profile(onboarded = true, mistakes = listOf("budget_9", "banks_2")), Intent.StartTraining(TopicId.BUDGET))
        assertEquals("budget_9", s.round!!.questions.first().question.id)
        assertEquals(10, s.round!!.questions.map { it.question.id }.distinct().size)
        assertTrue(s.round!!.questions.all { it.question.topic == TopicId.BUDGET })
    }
    @Test fun historyKeepsOnlyTwentyMostRecentRoundsAndReviewChangesNoRewards() {
        var s = initial()
        repeat(23) { i -> s = complete(act(s, Intent.StartQuick, now + i)) }
        assertEquals(20, s.profile.history.size)
        val reloaded = roundTrip(SavedGame(s.profile)).profile
        assertEquals(s.profile.history, reloaded.history)
        val reviewed = act(s, Intent.OpenHistory(s.profile.history.last().id))
        assertEquals(Screen.HistoryReview, reviewed.screen)
        assertEquals(s.profile, reviewed.profile)
        assertSame(s.result, reviewed.result)
        assertEquals(s.profile, act(reviewed, Intent.Back).profile)
    }
    @Test fun blitzRestoresAnsweredStatementsAndRetainsBestStreakBeyondEleven() {
        var s = act(initial(), Intent.StartBlitz)
        repeat(15) { s = act(s, Intent.BlitzAnswer(s.blitz!!.current.isTrue)) }
        val restored = resume(roundTrip(snapshot(s, now + 10_000)))
        assertEquals(50_000, restored.blitz!!.endsAt - restored.now)
        assertEquals(15, restored.blitz!!.records.size)
        val done = act(restored, Intent.QuitBlitz, restored.now)
        assertEquals(15, done.blitzResult!!.bestStreak)
        assertEquals(RoundMode.Blitz, done.profile.history.single().result.mode)
        assertTrue(done.profile.history.single().result.records.all { it.chosenTruth == it.correctTruth })
    }
    @Test fun dailyAnswerIsNotRepeatedAfterRestoreAndDailyHistoryIsSaved() {
        val s = start(mode = Intent.StartDaily).let { act(it, Intent.Answer(it.round!!.current.correctIndex)) }
        val restored = resume(roundTrip(snapshot(s)))
        assertEquals(1, restored.profile.dailyTotal)
        val done = act(restored, Intent.Next, restored.now)
        assertEquals(1, done.profile.dailyTotal)
        assertEquals(RoundMode.Daily, done.profile.history.single().result.mode)
    }
    @Test fun importRestoresExactlyAndDoesNotGenerateAchievementsOrMoney() {
        val p = complete(start()).profile.copy(achievements = emptyList(), capital = 1234)
        val imported = act(initial(), Intent.ImportProgress(p, null, "en"))
        assertEquals(p, imported.profile)
        assertEquals("en", imported.language)
        assertTrue(imported.toasts.isEmpty())
    }
    @Test fun backupRoundTripPreservesProfileSessionLanguageAndExplainsIncompatibleSchema() {
        val save = snapshot(act(start(), Intent.UseHint), now + 10_000)
        val text = ProgressCodec.encodeBackup(save, now, "1.1.0")
        assertEquals(save, ProgressCodec.decodeBackup(text, content).save)
        val bad = text.replace("\"schemaVersion\":1", "\"schemaVersion\":999")
        assertThrows(IncompatibleBackup::class.java) { ProgressCodec.decodeBackup(bad, content) }
    }
    @Test fun corruptForeignAndIncompleteBackupsAreRejected() {
        val text = ProgressCodec.encodeBackup(SavedGame(Profile()), now, "1.1.0")
        for (bad in listOf("{}", "[]", "bad", text.take(20), text.replace("ru.kvizcap.quiz.app/progress", "other-app"), text.replace("\"cells\":[", "\"cells\":[] , \"invalid\":["))) {
            assertThrows(bad.take(30), Exception::class.java) { ProgressCodec.decodeBackup(bad, content) }
        }
    }
    @Test fun unknownQuestionAndInvalidAnswerPermutationAreRejectedBeforeImport() {
        val save = snapshot(start())
        val session = save.session!!
        for (q in listOf(QuestionOrder("absent", listOf(0, 1, 2, 3)), QuestionOrder("budget_0", listOf(0, 0, 2, 3)))) {
            val bad = save.copy(session = session.copy(questions = listOf(q) + session.questions.drop(1)))
            assertThrows(Exception::class.java) { ProgressCodec.decodeBackup(ProgressCodec.encodeBackup(bad, now, "1.1.0"), content) }
        }
    }
    @Test fun staleWriterCannotOverwriteNewerAtomicCheckpoint() {
        val file = File(folder.root, "profile.json")
        val storage = ProgressFile(file)
        storage.save(SavedGame(Profile(capital = 5000)), 2)
        storage.save(SavedGame(Profile(capital = 1000)), 1)
        assertEquals(5000, storage.load().profile.capital)
    }
    @Test fun invalidImportLeavesCurrentFileByteIdenticalAndBackupSurvivesCorruption() {
        val file = File(folder.root, "profile.json")
        val storage = ProgressFile(file)
        storage.save(SavedGame(Profile(capital = 2000)), 1)
        val bytes = file.readBytes()
        assertThrows(Exception::class.java) { ProgressCodec.decodeBackup("bad", content) }
        assertArrayEquals(bytes, file.readBytes())
        storage.save(SavedGame(Profile(capital = 3000)), 2)
        file.writeText("corrupt")
        assertEquals(2000, storage.load().profile.capital)
    }
    @Test fun completedCareerChapterSurvivesBackupUsingCanonicalCatalogData() {
        val career = Profile().career.toMutableList().also { it[0] = List(Career.LEVELS) { if (it < 7) 1 else -1 } }
        val done = complete(start(Profile(onboarded = true, career = career), Intent.StartCareer(0, 7)))
        assertEquals(Career.chapters[0], done.profile.history.single().result.chapterCompleted)
        val save = SavedGame(done.profile)
        assertEquals(save, ProgressCodec.decodeBackup(ProgressCodec.encodeBackup(save, now, "1.1.0"), content).save)
    }
    @Test fun accuracyDoesNotOverflowForLargeValidCounters() {
        assertEquals(50, Stats(answered = 1_000_000_000, correct = 500_000_000).accuracy)
        assertEquals(50, TopicStat(1_000_000_000, 500_000_000).accuracy)
    }

}
