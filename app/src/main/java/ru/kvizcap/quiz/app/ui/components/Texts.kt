package ru.kvizcap.quiz.app.ui.components

import androidx.annotation.PluralsRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import ru.kvizcap.quiz.app.R
import ru.kvizcap.quiz.app.core.Career
import ru.kvizcap.quiz.app.core.Chapter
import ru.kvizcap.quiz.app.core.Quarter
import ru.kvizcap.quiz.app.mvi.RoundMode

@Composable
fun money(value: Long): String = stringResource(R.string.money, fmt(value))

@Composable
fun moneyPerDay(value: Long): String = stringResource(R.string.money_per_day, fmt(value))

@Composable
fun count(@PluralsRes id: Int, n: Int): String = pluralStringResource(id, n, n)

@Composable
fun levelTitle(chapter: Int, level: Int): String =
    if (Career.isExam(level)) stringResource(R.string.level_exam) else stringResource(R.string.level_n, Career.levelNumber(chapter, level))

@Composable
fun chapterLevel(chapter: Int, level: Int): String = stringResource(R.string.chapter_level, chapter + 1, levelTitle(chapter, level))

@Composable
fun unlocksOf(chapter: Chapter): String = chapter.unlocks.map { stringResource(Quarter.titleOf(it)) }.joinToString(", ")

@Composable
fun modeTitle(mode: RoundMode): String = when (mode) {
    is RoundMode.CareerLevel -> chapterLevel(mode.chapter, mode.level)
    is RoundMode.TopicPlay -> stringResource(mode.topic.title)
    RoundMode.Quick -> stringResource(R.string.mode_quick)
    RoundMode.Marathon -> stringResource(R.string.mode_marathon)
    RoundMode.Errors -> stringResource(R.string.mode_errors)
    RoundMode.Daily -> stringResource(R.string.mode_daily)
}
