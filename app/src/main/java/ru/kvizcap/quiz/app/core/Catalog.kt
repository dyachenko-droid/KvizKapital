package ru.kvizcap.quiz.app.core

import androidx.annotation.StringRes
import ru.kvizcap.quiz.app.R
import java.text.NumberFormat

enum class TopicId(val key: String, @StringRes val title: Int, @StringRes val description: Int, val icon: String, val tint: Long, val tintDark: Long, val ink: Long, val inkDark: Long) {
    BUDGET("budget", R.string.topic_budget, R.string.topic_budget_desc, Glyphs.PIGGY, 0xFFD5EEDF, 0xFF16392A, 0xFF0A4F34, 0xFFA6EBC9),
    BANKS("banks", R.string.topic_banks, R.string.topic_banks_desc, Glyphs.CARD, 0xFFDCEBFA, 0xFF16283D, 0xFF1D4F86, 0xFFA9CBF2),
    CREDIT("credit", R.string.topic_credit, R.string.topic_credit_desc, Glyphs.PERCENT, 0xFFFCE4DF, 0xFF3D1D18, 0xFF9A2F1F, 0xFFFFC4B8),
    INVEST("invest", R.string.topic_invest, R.string.topic_invest_desc, Glyphs.TREND, 0xFFFBEFD3, 0xFF3A2D12, 0xFF8A5A08, 0xFFF3D08A),
    TAX("tax", R.string.topic_tax, R.string.topic_tax_desc, Glyphs.DOC, 0xFFECE4F7, 0xFF2C2240, 0xFF5B3A8A, 0xFFD3C2F0),
    FRAUD("fraud", R.string.topic_fraud, R.string.topic_fraud_desc, Glyphs.SHIELD_CHECK, 0xFFDCF5E8, 0xFF12372A, 0xFF0B5A35, 0xFFA9F0CB),
    INSURANCE("insurance", R.string.topic_insurance, R.string.topic_insurance_desc, Glyphs.SHIELD, 0xFFDDF2F2, 0xFF123534, 0xFF16605F, 0xFFA8E3E1),
    PENSION("pension", R.string.topic_pension, R.string.topic_pension_desc, Glyphs.CLOCK, 0xFFF3EBDD, 0xFF33291A, 0xFF6B4A1E, 0xFFE8CFA8),
    HISTORY("history", R.string.topic_history, R.string.topic_history_desc, Glyphs.COINS, 0xFFFDE9D6, 0xFF3A2414, 0xFF8A3D0A, 0xFFFFC58F),
    ECONOMY("economy", R.string.topic_economy, R.string.topic_economy_desc, Glyphs.CITY, 0xFFE3ECF1, 0xFF1C2830, 0xFF2E4A5C, 0xFFB8CEDC);

    companion object {
        fun of(key: String) = entries.first { it.key == key }
    }
}

enum class Difficulty(val code: Char, @StringRes val title: Int, val mult: Double) {
    EASY('e', R.string.diff_easy, 1.0), MEDIUM('m', R.string.diff_medium, 1.5), HARD('h', R.string.diff_hard, 2.0);

    val multLabel: String get() = "×" + NumberFormat.getInstance().format(mult)

    companion object {
        fun of(c: Char) = entries.first { it.code == c }
    }
}

@kotlinx.serialization.Serializable(with = ChapterSerializer::class)
data class Chapter(
    val index: Int,
    val topic: TopicId,
    @StringRes val subtitle: Int,
    val unlocks: List<String>,
    val reward: Int,
) {
    @get:StringRes
    val title: Int get() = topic.title
}

object Career {
    const val LEVELS = 8
    val chapters = listOf(
        Chapter(0, TopicId.BUDGET, R.string.chapter_1_sub, listOf("books"), 2_000),
        Chapter(1, TopicId.BANKS, R.string.chapter_2_sub, listOf("garage", "lamps"), 4_000),
        Chapter(2, TopicId.CREDIT, R.string.chapter_3_sub, listOf("bank"), 6_000),
        Chapter(3, TopicId.INVEST, R.string.chapter_4_sub, listOf("hotel", "fountain"), 8_000),
        Chapter(4, TopicId.TAX, R.string.chapter_5_sub, listOf("office", "plaza"), 10_000),
        Chapter(5, TopicId.FRAUD, R.string.chapter_6_sub, listOf("tech"), 12_000),
    )

    fun difficultyFor(level: Int): Difficulty = when (level) {
        in 0..2 -> Difficulty.EASY
        in 3..5 -> Difficulty.MEDIUM
        else -> Difficulty.HARD
    }

    fun isExam(level: Int) = level == LEVELS - 1

    fun levelNumber(chapter: Int, level: Int): Int = chapter * LEVELS + level + 1
}

data class BuildingKind(
    val id: String,
    @StringRes val title: Int,
    val buildCost: Int,
    val baseIncome: Int,
    val art: List<String>,
)

data class DecorKind(
    val id: String,
    @StringRes val title: Int,
    val cost: Int,
    val bonusPercent: Int,
    val art: String,
)

object Quarter {
    const val SIZE = 4
    const val MAX_LEVEL = 5
    const val DAY_MS = 24L * 60 * 60 * 1000
    private val incomeMult = doubleArrayOf(1.0, 2.0, 3.4, 5.2, 7.6)
    private val upgradeMult = doubleArrayOf(0.0, 2.2, 4.8, 9.5, 18.0)

    val buildings = listOf(
        BuildingKind("coffee", R.string.building_coffee, 800, 160, art("coffee")),
        BuildingKind("bakery", R.string.building_bakery, 1_500, 280, art("bakery")),
        BuildingKind("books", R.string.building_books, 2_400, 430, art("books")),
        BuildingKind("garage", R.string.building_garage, 3_200, 600, art("garage")),
        BuildingKind("bank", R.string.building_bank, 6_000, 1_150, art("bank")),
        BuildingKind("hotel", R.string.building_hotel, 4_500, 850, art("hotel")),
        BuildingKind("office", R.string.building_office, 8_000, 1_600, art("office")),
        BuildingKind("tech", R.string.building_tech, 11_000, 2_200, art("tech")),
    )

    val decor = listOf(
        DecorKind("tree", R.string.decor_tree, 300, 2, "d_tree"),
        DecorKind("park", R.string.decor_park, 2_000, 5, "d_park"),
        DecorKind("lamps", R.string.decor_lamps, 3_500, 5, "d_lamps"),
        DecorKind("fountain", R.string.decor_fountain, 6_000, 10, "d_fountain"),
        DecorKind("plaza", R.string.decor_plaza, 9_000, 10, "d_plaza"),
    )

    val startUnlocked = setOf("coffee", "bakery", "tree", "park")

    private val unlockOrder = listOf(
        listOf(1 to 1, 2 to 1, 1 to 2, 2 to 2),
        listOf(0 to 1, 1 to 0),
        listOf(2 to 0, 0 to 2),
        listOf(3 to 1, 1 to 3),
        listOf(3 to 2, 2 to 3),
        listOf(0 to 0, 3 to 3),
        listOf(3 to 0, 0 to 3),
    )

    private fun art(id: String) = (1..MAX_LEVEL).map { "b_${id}_$it" }

    fun building(id: String) = buildings.first { it.id == id }
    fun decorKind(id: String) = decor.first { it.id == id }

    fun income(kind: BuildingKind, level: Int): Int = (kind.baseIncome * incomeMult[level - 1]).toInt()
    fun upgradeCost(kind: BuildingKind, toLevel: Int): Int = (kind.buildCost * upgradeMult[toLevel - 1]).toInt()
    fun invested(kind: BuildingKind, level: Int): Int = kind.buildCost + (2..level).sumOf { upgradeCost(kind, it) }

    fun cellIndex(i: Int, j: Int) = j * SIZE + i

    fun unlockedCells(chaptersDone: Int): Set<Int> =
        unlockOrder.take(1 + chaptersDone.coerceAtMost(unlockOrder.size - 1)).flatten().map { (i, j) -> cellIndex(i, j) }.toSet()

    fun chapterUnlockingCell(cell: Int): Int =
        unlockOrder.indexOfFirst { group -> group.any { (i, j) -> cellIndex(i, j) == cell } } - 1

    fun unlockedKinds(chaptersDone: Int): Set<String> =
        startUnlocked + Career.chapters.take(chaptersDone).flatMap { it.unlocks }

    fun chapterUnlocking(id: String): Chapter? = Career.chapters.firstOrNull { id in it.unlocks }

    @StringRes
    fun titleOf(id: String): Int = buildings.firstOrNull { it.id == id }?.title ?: decorKind(id).title
}

data class Rank(val index: Int, @StringRes val title: Int, val from: Int, val art: Int)

object Ranks {
    val all = listOf(
        Rank(0, R.string.rank_1, 0, R.drawable.r_1),
        Rank(1, R.string.rank_2, 5_000, R.drawable.r_2),
        Rank(2, R.string.rank_3, 25_000, R.drawable.r_3),
        Rank(3, R.string.rank_4, 75_000, R.drawable.r_4),
        Rank(4, R.string.rank_5, 200_000, R.drawable.r_5),
    )

    fun of(assets: Long): Rank = all.last { assets >= it.from }

    fun progressToNext(assets: Long): Float {
        val r = of(assets)
        val next = all.getOrNull(r.index + 1) ?: return 1f
        return ((assets - r.from).toFloat() / (next.from - r.from)).coerceIn(0f, 1f)
    }
}

data class Achievement(val id: String, @StringRes val title: Int, @StringRes val description: Int, val art: Int)

object Achievements {
    const val REWARD = 500
    val all = listOf(
        Achievement("first100", R.string.ach_first100, R.string.ach_first100_desc, R.drawable.a_first100),
        Achievement("streak10", R.string.ach_streak10, R.string.ach_streak10_desc, R.drawable.a_streak10),
        Achievement("first_business", R.string.ach_first_business, R.string.ach_first_business_desc, R.drawable.a_first_business),
        Achievement("week", R.string.ach_week, R.string.ach_week_desc, R.drawable.a_week),
        Achievement("antifraud", R.string.ach_antifraud, R.string.ach_antifraud_desc, R.drawable.a_antifraud),
        Achievement("taxes", R.string.ach_taxes, R.string.ach_taxes_desc, R.drawable.a_taxes),
        Achievement("no_hints", R.string.ach_no_hints, R.string.ach_no_hints_desc, R.drawable.a_no_hints),
        Achievement("quarter_max", R.string.ach_quarter_max, R.string.ach_quarter_max_desc, R.drawable.a_quarter_max),
        Achievement("magnate", R.string.ach_magnate, R.string.ach_magnate_desc, R.drawable.a_magnate),
        Achievement("blitz", R.string.ach_blitz, R.string.ach_blitz_desc, R.drawable.a_blitz),
        Achievement("marathon", R.string.ach_marathon, R.string.ach_marathon_desc, R.drawable.a_marathon),
        Achievement("daily30", R.string.ach_daily30, R.string.ach_daily30_desc, R.drawable.a_daily30),
    )

    fun byId(id: String) = all.first { it.id == id }
}

object Avatars {
    val all = listOf(
        R.drawable.av_1, R.drawable.av_2, R.drawable.av_3, R.drawable.av_4,
        R.drawable.av_5, R.drawable.av_6, R.drawable.av_7, R.drawable.av_8,
    )
}
