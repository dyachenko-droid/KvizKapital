package ru.kvizcap.quiz.app.core

import kotlin.random.Random

data class RoundQuestion(val question: Question, val order: List<Int>) {
    val options: List<String> get() = order.map { question.options[it] }
    val correctIndex: Int get() = order.indexOf(question.correct)
}

class Picker(private val content: Content) {

    fun shuffled(q: Question, rnd: Random) = RoundQuestion(q, (0..3).shuffled(rnd))

    fun pick(
        topics: Collection<TopicId>,
        preferred: Difficulty,
        count: Int,
        seen: Map<String, Int>,
        rnd: Random,
        exclude: Set<String> = emptySet(),
    ): List<RoundQuestion> {
        val pool = content.questions.filter { it.topic in topics && it.id !in exclude }
        val order = listOf(preferred) + Difficulty.entries.filter { it != preferred }.sortedBy { kotlin.math.abs(it.ordinal - preferred.ordinal) }
        val picked = mutableListOf<Question>()
        for (d in order) {
            if (picked.size >= count) break
            val candidates = pool.filter { it.difficulty == d && it !in picked }
                .shuffled(rnd)
                .sortedBy { seen[it.id] ?: 0 }
            picked += candidates.take(count - picked.size)
        }
        return picked.shuffled(rnd).map { shuffled(it, rnd) }
    }

    fun exam(topic: TopicId, seen: Map<String, Int>, rnd: Random): List<RoundQuestion> {
        val e = pick(listOf(topic), Difficulty.EASY, 3, seen, rnd)
        val m = pick(listOf(topic), Difficulty.MEDIUM, 4, seen, rnd, e.map { it.question.id }.toSet())
        val h = pick(listOf(topic), Difficulty.HARD, 3, seen, rnd, (e + m).map { it.question.id }.toSet())
        return e + m + h
    }

    fun marathon(topics: Collection<TopicId>, seen: Map<String, Int>, rnd: Random): List<RoundQuestion> {
        val pool = content.questions.filter { it.topic in topics }
        return Difficulty.entries.flatMap { d ->
            pool.filter { it.difficulty == d }.shuffled(rnd).sortedBy { seen[it.id] ?: 0 }
        }.map { shuffled(it, rnd) }
    }

    fun byIds(ids: List<String>, rnd: Random): List<RoundQuestion> =
        ids.mapNotNull { content.question(it) }.shuffled(rnd).take(10).map { shuffled(it, rnd) }

    fun daily(day: Long): RoundQuestion {
        val rnd = Random(day * 7919 + 17)
        val q = content.questions[rnd.nextInt(content.questions.size)]
        return shuffled(q, rnd)
    }

    fun statements(rnd: Random): List<Statement> = content.statements.shuffled(rnd)
}
