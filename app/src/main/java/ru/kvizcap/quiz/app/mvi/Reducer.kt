package ru.kvizcap.quiz.app.mvi

import androidx.annotation.StringRes
import ru.kvizcap.quiz.app.R
import ru.kvizcap.quiz.app.core.Achievements
import ru.kvizcap.quiz.app.core.Career
import ru.kvizcap.quiz.app.core.CellState
import ru.kvizcap.quiz.app.core.Content
import ru.kvizcap.quiz.app.core.Difficulty
import ru.kvizcap.quiz.app.core.Economy
import ru.kvizcap.quiz.app.core.Picker
import ru.kvizcap.quiz.app.core.Profile
import ru.kvizcap.quiz.app.core.Quarter
import ru.kvizcap.quiz.app.core.Ranks
import ru.kvizcap.quiz.app.core.RoundQuestion
import ru.kvizcap.quiz.app.core.TopicId
import ru.kvizcap.quiz.app.core.TopicStat
import kotlin.math.max
import kotlin.math.roundToLong
import kotlin.random.Random

class Reducer(content: Content) {

    data class Next(val state: AppState, val effects: List<Effect>)

    private val picker = Picker(content)

    companion object {
        const val QUESTION_MS = 20_000L
        const val BLITZ_MS = 60_000L
        const val COST_FIFTY = 150L
        const val COST_HINT = 100L
        const val COST_SKIP = 200L
        const val BASE_POINTS = 100.0
        const val BLITZ_POINTS = 40.0
        private val extraTopics = listOf(TopicId.INSURANCE, TopicId.PENSION, TopicId.HISTORY, TopicId.ECONOMY)
    }

    private class Fx(private val profile: Profile) {
        val list = mutableListOf<Effect>()
        fun sound(s: Sfx) { if (profile.settings.sound) list += Effect.Sound(s) }
        fun buzz(b: Buzz) { if (profile.settings.haptics) list += Effect.Haptic(b) }
        operator fun plusAssign(e: Effect) { list += e }
    }

    fun reduce(prev: AppState, intent: Intent, now: Long, rnd: Random): Next {
        val s = prev.copy(now = now)
        val fx = Fx(s.profile)
        val next = when (intent) {
            is Intent.Loaded -> s.copy(loaded = true, profile = intent.profile, stack = listOf(if (intent.profile.onboarded) Screen.Home else Screen.Onboarding))
            is Intent.Open -> if (intent.screen.isTab) s.copy(stack = listOf(intent.screen), sheet = null) else s.copy(stack = s.stack + intent.screen)
            is Intent.Tab -> { fx.buzz(Buzz.LIGHT); s.copy(stack = listOf(intent.screen), sheet = null) }
            Intent.Back -> back(s, fx)
            Intent.Tick -> tick(s, now, fx)
            Intent.Foreground -> s.copy(blitz = s.blitz?.let { if (it.paused) it.copy(paused = false, endsAt = now + it.remainingOnPause) else it })
            Intent.Background -> pause(s, true, now).let { paused -> paused.copy(blitz = paused.blitz?.let { if (it.paused) it else it.copy(paused = true, remainingOnPause = (it.endsAt - now).coerceAtLeast(0)) }) }
            is Intent.OfferResume -> s.copy(pendingSession = intent.session)
            is Intent.ResumeSaved -> s.copy(pendingSession = null, round = intent.round, blitz = intent.blitz, stack = listOf(Screen.Home, if (intent.round != null) Screen.Round else Screen.Blitz))
            Intent.ResumePending, Intent.RetryStorage -> s
            Intent.AbandonSaved -> s.copy(pendingSession = null, profile = s.profile.copy(capital = (s.profile.capital - (s.pendingSession?.round?.hintsSpent ?: 0)).coerceAtLeast(0)))
            is Intent.OpenHistory -> s.profile.history.firstOrNull { it.id == intent.id }?.let { s.copy(historyReview = it, stack = s.stack + Screen.HistoryReview) } ?: s
            is Intent.ImportProgress -> return Next(AppState(loaded = true, profile = intent.profile, pendingSession = intent.session, language = intent.language, stack = listOf(if (intent.profile.onboarded) Screen.Home else Screen.Onboarding), now = now), emptyList())
            is Intent.FinishOnboarding -> {
                fx.sound(Sfx.FINISH)
                s.copy(
                    profile = s.profile.copy(
                        onboarded = true,
                        name = intent.name.trim(),
                        interests = intent.interests,
                        settings = s.profile.settings.copy(defaultDifficulty = intent.difficulty),
                    ),
                    stack = listOf(Screen.Home),
                )
            }
            is Intent.StartCareer -> startCareer(s, intent.chapter, intent.level, now, rnd, fx)
            Intent.ContinueCareer -> s.profile.currentLevel().let { (c, l) -> startCareer(s, c, l, now, rnd, fx) }
            is Intent.SetTopicDifficulty -> s.copy(topicDifficulty = intent.difficulty)
            is Intent.StartTopic -> startRound(s, RoundMode.TopicPlay(intent.topic, intent.difficulty), picker.pick(listOf(intent.topic), intent.difficulty, 10, s.profile.seen, rnd), now, fx)
            is Intent.StartTraining -> {
                val difficulty = Difficulty.entries[s.profile.settings.defaultDifficulty]
                startRound(s, RoundMode.Training(intent.topic, difficulty), picker.training(intent.topic, difficulty, s.profile.seen, s.profile.mistakes, rnd), now, fx)
            }
            Intent.StartQuick -> startRound(s, RoundMode.Quick, picker.pick(openTopics(s.profile), Difficulty.entries[s.profile.settings.defaultDifficulty], 10, s.profile.seen, rnd), now, fx)
            Intent.StartMarathon -> startRound(s, RoundMode.Marathon, picker.marathon(TopicId.entries, s.profile.seen, rnd), now, fx)
            is Intent.StartErrors -> startErrors(s, RoundMode.Errors(intent.topic), now, rnd, fx)
            Intent.StartDaily -> {
                val today = Economy.today(now)
                if (s.profile.dailyLastDay == today) toast(s, R.string.toast_daily_done, now)
                else startRound(s, RoundMode.Daily, listOf(picker.daily(today)), now, fx)
            }
            Intent.StartBlitz -> {
                fx.sound(Sfx.TAP)
                s.copy(blitz = BlitzState(picker.statements(rnd), endsAt = now + BLITZ_MS), blitzResult = null, stack = s.stack.filterNot { it == Screen.BlitzResult } + Screen.Blitz)
            }
            is Intent.Answer -> answer(s, intent.index, now, fx)
            Intent.UseFifty -> useFifty(s, rnd, fx)
            Intent.UseHint -> useHint(s, fx)
            Intent.Skip -> skip(s, now, fx)
            Intent.Next -> nextQuestion(s, now, fx)
            is Intent.Explain -> s.round?.let { s.copy(round = it.copy(explain = intent.open)) } ?: s
            is Intent.Pause -> pause(s, intent.paused, now)
            Intent.Restart -> s.round?.let { r -> restart(s.copy(round = null, stack = s.stack.filterNot { it == Screen.Round }), r.mode, now, rnd, fx) } ?: s
            Intent.QuitRound -> s.copy(profile = s.profile.copy(capital = (s.profile.capital - (s.round?.hintsSpent ?: 0)).coerceAtLeast(0)), round = null, stack = s.stack.filterNot { it == Screen.Round }.ifEmpty { listOf(Screen.Home) })
            Intent.Replay -> s.result?.let { res -> restart(s.copy(stack = s.stack.filterNot { it == Screen.Result || it == Screen.Review }), res.mode, now, rnd, fx) } ?: s
            is Intent.BlitzAnswer -> blitzAnswer(s, intent.value, now, fx)
            Intent.QuitBlitz -> finishBlitz(s, fx)
            Intent.CollectAll -> collect(s, now, fx)
            is Intent.OpenCell -> openCell(s, intent.cell, now, fx)
            Intent.CloseSheet -> s.copy(sheet = null)
            is Intent.Build -> build(s, intent.cell, intent.item, now, fx)
            is Intent.Upgrade -> upgrade(s, intent.cell, now, fx)
            Intent.DismissUpgrade -> s.copy(upgradeFx = null)
            Intent.DismissCollect -> s.copy(collectFx = null)
            is Intent.DismissToast -> s.copy(toasts = s.toasts.filterNot { it.serial == intent.serial })
            is Intent.SetTheme -> settings(s) { it.copy(theme = intent.theme) }
            Intent.ToggleSound -> settings(s) { it.copy(sound = !it.sound) }
            Intent.ToggleHaptics -> settings(s) { it.copy(haptics = !it.haptics) }
            Intent.ToggleTimer -> settings(s) { it.copy(timer = !it.timer) }
            is Intent.SetDefaultDifficulty -> settings(s) { it.copy(defaultDifficulty = intent.value) }
            is Intent.SetLanguage -> {
                fx.buzz(Buzz.LIGHT)
                if (intent.tag == s.language) s else s.also { fx += Effect.ApplyLanguage(intent.tag) }
            }
            is Intent.LanguageSynced -> s.copy(language = intent.tag)
            is Intent.ConfirmReset -> s.copy(confirmReset = intent.show)
            Intent.ResetProgress -> {
                AppState(loaded = true, profile = Profile(settings = s.profile.settings), stack = listOf(Screen.Onboarding), now = now, language = s.language)
            }
            is Intent.ConfirmExit -> s.copy(confirmExit = intent.show)
            is Intent.SetAvatar -> s.copy(profile = s.profile.copy(avatar = intent.index))
            is Intent.SetName -> s.copy(profile = s.profile.copy(name = intent.name.take(24)))
        }
        val processed = if (intent is Intent.Loaded) next else postProcess(prev, next, now, fx)
        if (processed.profile != prev.profile) fx += Effect.Save
        return Next(processed, fx.list)
    }

    private fun openTopics(p: Profile): List<TopicId> =
        Career.chapters.take(p.chaptersDone + 1).map { it.topic } + extraTopics

    private fun toast(s: AppState, @StringRes text: Int, now: Long, @StringRes arg: Int? = null) = s.copy(toasts = s.toasts + Toast.Message(text, arg, now + s.toasts.size))

    private fun settings(s: AppState, change: (ru.kvizcap.quiz.app.core.Settings) -> ru.kvizcap.quiz.app.core.Settings) =
        s.copy(profile = s.profile.copy(settings = change(s.profile.settings)))

    private fun back(s: AppState, fx: Fx): AppState {
        val r = s.round
        return when {
            s.confirmReset -> s.copy(confirmReset = false)
            s.confirmExit -> s.copy(confirmExit = false)
            s.upgradeFx != null -> s.copy(upgradeFx = null)
            s.sheet != null -> s.copy(sheet = null)
            s.screen == Screen.Round && r != null && r.explain -> s.copy(round = r.copy(explain = false))
            s.screen == Screen.Round && r != null -> pause(s, !r.paused, s.now)
            s.screen == Screen.Blitz -> finishBlitz(s, fx)
            s.screen == Screen.Onboarding -> s.also { fx += Effect.Exit }
            s.stack.size > 1 -> s.copy(stack = s.stack.dropLast(1))
            s.screen != Screen.Home -> s.copy(stack = listOf(Screen.Home))
            else -> s.copy(confirmExit = true)
        }
    }

    private fun startCareer(s: AppState, chapter: Int, level: Int, now: Long, rnd: Random, fx: Fx): AppState {
        if (!s.profile.isLevelOpen(chapter, level)) return toast(s, R.string.toast_level_locked, now)
        val topic = Career.chapters[chapter].topic
        val questions = if (Career.isExam(level)) picker.exam(topic, s.profile.seen, rnd)
        else picker.pick(listOf(topic), Career.difficultyFor(level), 10, s.profile.seen, rnd)
        return startRound(s, RoundMode.CareerLevel(chapter, level), questions, now, fx)
    }

    private fun restart(s: AppState, mode: RoundMode, now: Long, rnd: Random, fx: Fx): AppState = when (mode) {
        is RoundMode.CareerLevel -> startCareer(s, mode.chapter, mode.level, now, rnd, fx)
        is RoundMode.TopicPlay -> startRound(s, mode, picker.pick(listOf(mode.topic), mode.difficulty, 10, s.profile.seen, rnd), now, fx)
        is RoundMode.Training -> startRound(s, mode, picker.training(mode.topic, mode.difficulty, s.profile.seen, s.profile.mistakes, rnd), now, fx)
        RoundMode.Quick -> startRound(s, mode, picker.pick(openTopics(s.profile), Difficulty.entries[s.profile.settings.defaultDifficulty], 10, s.profile.seen, rnd), now, fx)
        RoundMode.Marathon -> startRound(s, mode, picker.marathon(TopicId.entries, s.profile.seen, rnd), now, fx)
        is RoundMode.Errors -> startErrors(s, mode, now, rnd, fx)
        RoundMode.Blitz -> s
        RoundMode.Daily -> s.copy(stack = s.stack.ifEmpty { listOf(Screen.Home) })
    }

    private fun startErrors(s: AppState, mode: RoundMode.Errors, now: Long, rnd: Random, fx: Fx): AppState {
        val questions = picker.byIds(s.profile.mistakes, rnd, mode.topic)
        return if (questions.isEmpty()) toast(s, R.string.toast_no_mistakes, now) else startRound(s, mode, questions, now, fx)
    }

    private fun startRound(s: AppState, mode: RoundMode, questions: List<RoundQuestion>, now: Long, fx: Fx): AppState {
        if (questions.isEmpty()) return toast(s, R.string.toast_no_questions, now)
        fx.sound(Sfx.TAP)
        fx.buzz(Buzz.LIGHT)
        val seen = s.profile.seen.toMutableMap()
        questions.take(if (mode == RoundMode.Marathon) 1 else questions.size).forEach { seen[it.question.id] = (seen[it.question.id] ?: 0) + 1 }
        val base = s.stack.filterNot { it == Screen.Round || it == Screen.Result || it == Screen.Review }.ifEmpty { listOf(Screen.Home) }
        return s.copy(
            profile = s.profile.copy(seen = seen),
            round = RoundState(mode = mode, questions = questions, deadline = now + QUESTION_MS),
            result = null,
            stack = base + Screen.Round,
            sheet = null,
        )
    }

    private fun pause(s: AppState, paused: Boolean, now: Long): AppState {
        val r = s.round ?: return s
        if (r.paused == paused) return s
        return if (paused) s.copy(round = r.copy(paused = true, remainingOnPause = (r.deadline - now).coerceIn(0, QUESTION_MS)))
        else s.copy(round = r.copy(paused = false, deadline = now + r.remainingOnPause))
    }

    private fun tick(s: AppState, now: Long, fx: Fx): AppState {
        val r = s.round
        if (r != null && s.screen == Screen.Round && r.phase == Phase.ASKING && !r.paused && s.profile.settings.timer) {
            val left = r.deadline - now
            if (left <= 0) return answer(s, -1, now, fx)
            val sec = ((left + 999) / 1000).toInt()
            if (sec <= 5 && sec != r.lastTickSecond) {
                fx.sound(Sfx.TICK)
                return s.copy(round = r.copy(lastTickSecond = sec))
            }
        }
        val b = s.blitz
        if (b != null && s.screen == Screen.Blitz && !b.paused && now >= b.endsAt) return finishBlitz(s, fx)
        return s
    }

    private fun answer(s: AppState, index: Int, now: Long, fx: Fx): AppState {
        val r = s.round ?: return s
        if (r.phase != Phase.ASKING || r.paused || index in r.removed) return s
        val rq = r.current
        val correct = index == rq.correctIndex
        var p = s.profile
        val streak = if (correct) r.streak + 1 else 0
        var gain = 0L
        var base = 0L
        if (r.mode == RoundMode.Daily) {
            val today = Economy.today(now)
            val newStreak = if (p.dailyLastDay == today - 1) p.dailyStreak + 1 else 1
            p = p.copy(dailyLastDay = today, dailyStreak = newStreak, dailyBest = max(p.dailyBest, newStreak), dailyTotal = p.dailyTotal + 1)
            if (correct) { base = 500; gain = 500L + 50L * newStreak.coerceAtMost(10) }
        } else if (correct) {
            base = (BASE_POINTS * rq.question.difficulty.mult).roundToLong()
            gain = (base * r.multiplierFor(streak)).roundToLong()
        }
        val topicKey = rq.question.topic.key
        val ts = p.stats.perTopic[topicKey] ?: TopicStat()
        p = p.copy(
            stats = p.stats.copy(
                answered = p.stats.answered + 1,
                correct = p.stats.correct + if (correct) 1 else 0,
                bestStreak = max(p.stats.bestStreak, streak),
                perTopic = p.stats.perTopic + (topicKey to ts.copy(answered = ts.answered + 1, correct = ts.correct + if (correct) 1 else 0)),
            ),
            mistakes = when {
                correct && (r.mode is RoundMode.Errors || r.mode is RoundMode.Training) -> p.mistakes - rq.question.id
                !correct && rq.question.id !in p.mistakes -> (p.mistakes + rq.question.id).takeLast(60)
                else -> p.mistakes
            },
        )
        if (correct) { fx.sound(Sfx.CORRECT); fx.buzz(Buzz.CONFIRM) } else { fx.sound(Sfx.WRONG); fx.buzz(Buzz.REJECT) }
        val record = AnswerRecord(
            questionId = rq.question.id,
            question = rq.question.text,
            chosen = rq.options.getOrNull(index),
            correctAnswer = rq.options[rq.correctIndex],
            correct = correct,
            skipped = false,
            gained = gain,
            explanation = rq.question.explanation,
        )
        return s.copy(
            profile = p,
            round = r.copy(
                phase = Phase.REVEALED,
                chosen = index,
                streak = streak,
                bestStreak = max(r.bestStreak, streak),
                delta = r.delta + gain,
                base = r.base + base,
                gained = r.gained + gain,
                lastGain = gain,
                records = r.records + record,
                serial = r.serial + 1,
            ),
        )
    }

    private fun canAfford(s: AppState, cost: Long) = s.liveCapital >= cost

    private fun useFifty(s: AppState, rnd: Random, fx: Fx): AppState {
        val r = s.round ?: return s
        if (r.phase != Phase.ASKING || r.removed.isNotEmpty() || !canAfford(s, COST_FIFTY)) return s
        fx.sound(Sfx.TAP); fx.buzz(Buzz.LIGHT)
        val wrong = (0..3).filter { it != r.current.correctIndex }.shuffled(rnd).take(2).toSet()
        return s.copy(round = r.copy(removed = wrong, delta = r.delta - COST_FIFTY, hintsSpent = r.hintsSpent + COST_FIFTY, usedAnyHint = true))
    }

    private fun useHint(s: AppState, fx: Fx): AppState {
        val r = s.round ?: return s
        if (r.phase != Phase.ASKING || r.hintShown || !canAfford(s, COST_HINT)) return s
        fx.sound(Sfx.TAP); fx.buzz(Buzz.LIGHT)
        return s.copy(round = r.copy(hintShown = true, delta = r.delta - COST_HINT, hintsSpent = r.hintsSpent + COST_HINT, usedAnyHint = true))
    }

    private fun skip(s: AppState, now: Long, fx: Fx): AppState {
        val r = s.round ?: return s
        if (r.phase != Phase.ASKING || r.mode == RoundMode.Daily || !canAfford(s, COST_SKIP)) return s
        fx.sound(Sfx.TAP); fx.buzz(Buzz.LIGHT)
        val rq = r.current
        val record = AnswerRecord(rq.question.id, rq.question.text, null, rq.options[rq.correctIndex], correct = false, skipped = true, gained = 0, explanation = rq.question.explanation)
        val skipped = r.copy(records = r.records + record, delta = r.delta - COST_SKIP, hintsSpent = r.hintsSpent + COST_SKIP, usedAnyHint = true)
        return if (r.isLast) finish(s.copy(round = skipped), now, fx) else advance(s.copy(round = skipped), now)
    }

    private fun advance(s: AppState, now: Long): AppState {
        val r = s.round ?: return s
        val nextIndex = r.index + 1
        var p = s.profile
        if (r.mode == RoundMode.Marathon) {
            val id = r.questions[nextIndex].question.id
            p = p.copy(seen = p.seen + (id to ((p.seen[id] ?: 0) + 1)))
        }
        return s.copy(
            profile = p,
            round = r.copy(
                index = nextIndex,
                phase = Phase.ASKING,
                chosen = -1,
                removed = emptySet(),
                hintShown = false,
                explain = false,
                deadline = now + QUESTION_MS,
                lastTickSecond = -1,
                lastGain = 0,
            ),
        )
    }

    private fun nextQuestion(s: AppState, now: Long, fx: Fx): AppState {
        val r = s.round ?: return s
        if (r.phase != Phase.REVEALED) return s
        val marathonOver = r.mode == RoundMode.Marathon && r.records.lastOrNull()?.correct == false
        return if (r.isLast || marathonOver) finish(s, now, fx) else advance(s, now)
    }

    private fun stars(correct: Int, total: Int): Int {
        if (total == 0) return 0
        val ratio = correct.toDouble() / total
        return when {
            ratio >= 0.9 -> 3
            ratio >= 0.7 -> 2
            ratio >= 0.5 -> 1
            else -> 0
        }
    }

    private fun finish(s: AppState, now: Long, fx: Fx): AppState {
        val r = s.round ?: return s
        val correct = r.correctCount
        val total = r.records.size.coerceAtLeast(1)
        var p = s.profile
        var capital = (p.capital + r.delta).coerceAtLeast(0)
        val starCount = if (r.mode == RoundMode.Marathon || r.mode == RoundMode.Daily) 0 else stars(correct, total)
        var chapterDone: ru.kvizcap.quiz.app.core.Chapter? = null
        val passed: Boolean
        when (val m = r.mode) {
            is RoundMode.CareerLevel -> {
                passed = starCount >= 1
                if (passed) {
                    val wasDone = p.career[m.chapter][m.level] >= 1
                    val career = p.career.mapIndexed { ci, ch -> if (ci != m.chapter) ch else ch.mapIndexed { li, st -> if (li == m.level) max(st, starCount) else st } }
                    p = p.copy(career = career)
                    if (Career.isExam(m.level) && !wasDone) {
                        chapterDone = Career.chapters[m.chapter]
                        capital += chapterDone.reward
                    }
                }
            }
            is RoundMode.TopicPlay -> passed = starCount >= 1
            RoundMode.Quick, RoundMode.Marathon, RoundMode.Blitz, is RoundMode.Errors, is RoundMode.Training -> passed = true
            RoundMode.Daily -> passed = correct > 0
        }
        if (r.mode == RoundMode.Marathon) p = p.copy(stats = p.stats.copy(marathonBest = max(p.stats.marathonBest, correct)))
        p = p.copy(capital = capital, stats = p.stats.copy(earned = p.stats.earned + r.gained + (chapterDone?.reward ?: 0)))
        fx.sound(Sfx.FINISH); fx.buzz(Buzz.HEAVY)
        val result = RoundResult(
            mode = r.mode, correct = correct, total = total,
            base = r.base, streakBonus = (r.gained - r.base).coerceAtLeast(0), hintsSpent = r.hintsSpent, delta = r.delta,
            stars = starCount, passed = passed, bestStreak = r.bestStreak, records = r.records,
            chapterCompleted = chapterDone, usedAnyHint = r.usedAnyHint,
        )
        return s.copy(profile = p.copy(history = (listOf(RoundHistory(java.util.UUID.randomUUID().toString(), now, result)) + p.history).take(20)), round = null, result = result, stack = s.stack.filterNot { it == Screen.Round } + Screen.Result)
    }

    private fun blitzAnswer(s: AppState, value: Boolean, now: Long, fx: Fx): AppState {
        val b = s.blitz ?: return s
        if (b.paused || now >= b.endsAt) return s
        val correct = value == b.current.isTrue
        val streak = if (correct) b.streak + 1 else 0
        val gain = if (correct) (BLITZ_POINTS * (1.0 + 0.1 * (streak - 1)).coerceAtMost(2.0)).roundToLong() else 0
        if (correct) { fx.sound(Sfx.CORRECT); fx.buzz(Buzz.LIGHT) } else { fx.sound(Sfx.WRONG); fx.buzz(Buzz.REJECT) }
        return s.copy(
            blitz = b.copy(
                records = b.records + AnswerRecord(b.current.id.toString(), b.current.text, null, "", correct, false, gain, b.current.explanation, value, b.current.isTrue),
                bestStreak = max(b.bestStreak, streak),
                index = b.index + 1, streak = streak, correct = b.correct + if (correct) 1 else 0, answered = b.answered + 1,
                delta = b.delta + gain, lastCorrect = correct, serial = b.serial + 1, history = (b.history + correct).takeLast(11),
            ),
            profile = s.profile.copy(stats = s.profile.stats.copy(bestStreak = max(s.profile.stats.bestStreak, streak))),
        )
    }

    private fun finishBlitz(s: AppState, fx: Fx): AppState {
        val b = s.blitz ?: return s
        val record = b.correct > s.profile.stats.blitzBest
        val p = s.profile.copy(
            capital = s.profile.capital + b.delta,
            stats = s.profile.stats.copy(blitzBest = max(s.profile.stats.blitzBest, b.correct), earned = s.profile.stats.earned + b.delta),
        )
        fx.sound(Sfx.FINISH); fx.buzz(Buzz.HEAVY)
        val best = b.bestStreak
        val history = if (b.answered == 0) p.history else (listOf(RoundHistory(java.util.UUID.randomUUID().toString(), s.now,
            RoundResult(RoundMode.Blitz, b.correct, b.answered, b.delta, 0, 0, b.delta, 0, true, best, b.records))) + p.history).take(20)
        return s.copy(
            profile = p.copy(history = history), blitz = null,
            blitzResult = BlitzResult(b.correct, b.answered, b.delta, best, record),
            stack = s.stack.filterNot { it == Screen.Blitz } + Screen.BlitzResult,
        )
    }

    private fun collect(s: AppState, now: Long, fx: Fx): AppState {
        val p = s.profile
        val amount = Economy.storedTotal(p, now)
        if (amount <= 0) return s
        fx.sound(Sfx.COLLECT); fx.buzz(Buzz.CONFIRM)
        val cells = p.cells.map { if (it.building != null) it.copy(accruedAt = now) else it }
        return s.copy(
            profile = p.copy(capital = p.capital + amount, cells = cells, stats = p.stats.copy(quarterIncome = p.stats.quarterIncome + amount)),
            collectFx = CollectFx(amount, now),
        )
    }

    private fun openCell(s: AppState, cell: Int, now: Long, fx: Fx): AppState {
        val unlocked = Quarter.unlockedCells(s.profile.chaptersDone)
        if (cell !in unlocked) {
            val ch = Quarter.chapterUnlockingCell(cell)
            fx.buzz(Buzz.REJECT)
            return if (ch >= 0) toast(s, R.string.toast_plot_after, now, Career.chapters[ch].topic.title) else toast(s, R.string.toast_plot_closed, now)
        }
        fx.buzz(Buzz.LIGHT)
        return s.copy(sheet = if (s.profile.cells[cell].isEmpty) QuarterSheet.Shop(cell) else QuarterSheet.Building(cell))
    }

    private fun build(s: AppState, cell: Int, item: String, now: Long, fx: Fx): AppState {
        val p = s.profile
        if (!p.cells[cell].isEmpty || item !in Quarter.unlockedKinds(p.chaptersDone)) return s
        val isBuilding = Quarter.buildings.any { it.id == item }
        val cost = if (isBuilding) Quarter.building(item).buildCost.toLong() else Quarter.decorKind(item).cost.toLong()
        if (p.capital < cost) return s
        val newCell = if (isBuilding) CellState(building = item, level = 1, accruedAt = now) else CellState(decor = item)
        val bonusBefore = Economy.bonusPercent(p)
        val cells = p.cells.toMutableList().also { it[cell] = newCell }
        val np = p.copy(capital = p.capital - cost, cells = cells)
        fx.sound(Sfx.UPGRADE); fx.buzz(Buzz.HEAVY)
        val incomeAfter = if (isBuilding) Economy.incomePerDay(newCell, Economy.bonusPercent(np)) else Economy.bonusPercent(np).toLong()
        return s.copy(profile = np, sheet = null, upgradeFx = UpgradeFx(cell, item, 0, 1, if (isBuilding) 0 else bonusBefore.toLong(), incomeAfter, built = true))
    }

    private fun upgrade(s: AppState, cell: Int, now: Long, fx: Fx): AppState {
        val p = s.profile
        val c = p.cells[cell]
        val id = c.building ?: return s
        if (c.level >= Quarter.MAX_LEVEL) return s
        val kind = Quarter.building(id)
        val cost = Quarter.upgradeCost(kind, c.level + 1).toLong()
        val bonus = Economy.bonusPercent(p)
        val stored = Economy.stored(c, bonus, now)
        if (p.capital + stored < cost) return s
        val upgraded = c.copy(level = c.level + 1, accruedAt = now)
        val cells = p.cells.toMutableList().also { it[cell] = upgraded }
        fx.sound(Sfx.UPGRADE); fx.buzz(Buzz.HEAVY)
        return s.copy(
            profile = p.copy(capital = p.capital + stored - cost, cells = cells, stats = p.stats.copy(quarterIncome = p.stats.quarterIncome + stored)),
            sheet = null,
            upgradeFx = UpgradeFx(cell, id, c.level, c.level + 1, Economy.incomePerDay(c, bonus), Economy.incomePerDay(upgraded, bonus), built = false),
        )
    }

    private fun achievementsMet(p: Profile, result: RoundResult?): Set<String> {
        val met = mutableSetOf<String>()
        if (p.stats.correct >= 100) met += "first100"
        if (p.stats.bestStreak >= 10) met += "streak10"
        if (p.cells.any { it.building != null }) met += "first_business"
        if (p.dailyBest >= 7) met += "week"
        if (p.career[5][Career.LEVELS - 1] >= 1) met += "antifraud"
        if (p.career[4][Career.LEVELS - 1] >= 1) met += "taxes"
        if (result != null && result.total >= 10 && result.correct == result.total && !result.usedAnyHint) met += "no_hints"
        if (p.cells.any { it.level >= Quarter.MAX_LEVEL }) met += "quarter_max"
        if (Ranks.of(Economy.assets(p)).index >= 4) met += "magnate"
        if (p.stats.blitzBest >= 20) met += "blitz"
        if (p.stats.marathonBest >= 15) met += "marathon"
        if (p.dailyTotal >= 30) met += "daily30"
        return met
    }

    private fun postProcess(prev: AppState, next: AppState, now: Long, fx: Fx): AppState {
        if (!next.loaded || !next.profile.onboarded || next.profile === prev.profile && next.result === prev.result) return next
        var p = next.profile
        val toasts = next.toasts.toMutableList()
        val freshResult = next.result?.takeIf { it !== prev.result }
        val newOnes = achievementsMet(p, freshResult) - p.achievements.toSet()
        if (newOnes.isNotEmpty()) {
            p = p.copy(achievements = p.achievements + newOnes, capital = p.capital + Achievements.REWARD.toLong() * newOnes.size)
            newOnes.forEachIndexed { i, id -> toasts += Toast.AchievementUnlocked(id, now + 100 + i) }
            fx.sound(Sfx.RANK)
        }
        val rank = Ranks.of(Economy.assets(p)).index
        if (rank > p.rankSeen) {
            p = p.copy(rankSeen = rank)
            toasts += Toast.RankUp(rank, now + 200)
            fx.sound(Sfx.RANK); fx.buzz(Buzz.HEAVY)
        }
        val today = Economy.today(now)
        val assets = Economy.assets(p)
        if (p.assetsHistory[today] != assets) {
            p = p.copy(assetsHistory = (p.assetsHistory + (today to assets)).toSortedMap().entries.toList().takeLast(90).associate { it.key to it.value })
        }
        return next.copy(profile = p, toasts = toasts)
    }
}
