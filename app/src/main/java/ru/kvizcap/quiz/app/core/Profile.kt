package ru.kvizcap.quiz.app.core

import kotlinx.serialization.Serializable

@Serializable
data class CellState(
    val building: String? = null,
    val level: Int = 0,
    val decor: String? = null,
    val accruedAt: Long = 0,
) {
    val isEmpty: Boolean get() = building == null && decor == null
}

@Serializable
data class TopicStat(val answered: Int = 0, val correct: Int = 0) {
    val accuracy: Int get() = if (answered == 0) 0 else correct * 100 / answered
}

@Serializable
data class Stats(
    val answered: Int = 0,
    val correct: Int = 0,
    val bestStreak: Int = 0,
    val earned: Long = 0,
    val quarterIncome: Long = 0,
    val blitzBest: Int = 0,
    val marathonBest: Int = 0,
    val perTopic: Map<String, TopicStat> = emptyMap(),
) {
    val accuracy: Int get() = if (answered == 0) 0 else correct * 100 / answered
}

@Serializable
data class Settings(
    val theme: Int = 0,
    val sound: Boolean = true,
    val haptics: Boolean = true,
    val timer: Boolean = true,
    val defaultDifficulty: Int = 1,
)

@Serializable
data class Profile(
    val onboarded: Boolean = false,
    val name: String = "",
    val avatar: Int = 0,
    val interests: List<String> = emptyList(),
    val capital: Long = 1_000,
    val cells: List<CellState> = List(Quarter.SIZE * Quarter.SIZE) { CellState() },
    val career: List<List<Int>> = List(Career.chapters.size) { List(Career.LEVELS) { -1 } },
    val stats: Stats = Stats(),
    val seen: Map<String, Int> = emptyMap(),
    val mistakes: List<String> = emptyList(),
    val achievements: List<String> = emptyList(),
    val dailyLastDay: Long = -1,
    val dailyStreak: Int = 0,
    val dailyBest: Int = 0,
    val dailyTotal: Int = 0,
    val settings: Settings = Settings(),
    val assetsHistory: Map<Long, Long> = emptyMap(),
    val rankSeen: Int = 0,
) {
    val chaptersDone: Int get() = career.count { it[Career.LEVELS - 1] >= 1 }
    val totalStars: Int get() = career.sumOf { ch -> ch.sumOf { it.coerceAtLeast(0) } }

    fun starsIn(chapter: Int): Int = career[chapter].sumOf { it.coerceAtLeast(0) }

    fun isLevelOpen(chapter: Int, level: Int): Boolean = when {
        level > 0 -> career[chapter][level - 1] >= 1
        chapter == 0 -> true
        else -> career[chapter - 1][Career.LEVELS - 1] >= 1
    }

    fun currentLevel(): Pair<Int, Int> {
        for (c in career.indices) for (l in 0 until Career.LEVELS) {
            if (career[c][l] < 1 && isLevelOpen(c, l)) return c to l
        }
        return career.lastIndex to Career.LEVELS - 1
    }
}
