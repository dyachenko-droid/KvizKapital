package ru.kvizcap.quiz.app.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.max
import kotlin.math.min

object Economy {
    fun bonusPercent(p: Profile): Int = p.cells.mapNotNull { it.decor }.sumOf { Quarter.decorKind(it).bonusPercent }

    fun incomePerDay(cell: CellState, bonus: Int): Long {
        val id = cell.building ?: return 0
        return Quarter.income(Quarter.building(id), cell.level).toLong() * (100 + bonus) / 100
    }

    fun incomePerDay(p: Profile): Long {
        val bonus = bonusPercent(p)
        return p.cells.sumOf { incomePerDay(it, bonus) }
    }

    fun storageCap(cell: CellState, bonus: Int): Long = incomePerDay(cell, bonus)

    fun stored(cell: CellState, bonus: Int, now: Long): Long {
        if (cell.building == null) return 0
        val perDay = incomePerDay(cell, bonus)
        val elapsed = max(0L, now - cell.accruedAt)
        return min(perDay, perDay * elapsed / Quarter.DAY_MS)
    }

    fun storedTotal(p: Profile, now: Long): Long {
        val bonus = bonusPercent(p)
        return p.cells.sumOf { stored(it, bonus, now) }
    }

    fun fillRatio(p: Profile, now: Long): Float {
        val cap = incomePerDay(p)
        return if (cap == 0L) 0f else storedTotal(p, now).toFloat() / cap
    }

    fun cellValue(cell: CellState): Long {
        val b = cell.building?.let { Quarter.invested(Quarter.building(it), cell.level).toLong() } ?: 0
        val d = cell.decor?.let { Quarter.decorKind(it).cost.toLong() } ?: 0
        return b + d
    }

    fun assets(p: Profile): Long = p.capital + p.cells.sumOf { cellValue(it) }

    fun businessCount(p: Profile): Int = p.cells.count { it.building != null }

    fun today(now: Long): Long = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()

    fun dateOf(day: Long): LocalDate = LocalDate.ofEpochDay(day)
}
