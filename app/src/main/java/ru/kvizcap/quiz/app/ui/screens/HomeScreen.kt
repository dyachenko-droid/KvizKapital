package ru.kvizcap.quiz.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import ru.kvizcap.quiz.app.core.Career
import ru.kvizcap.quiz.app.core.Economy
import ru.kvizcap.quiz.app.core.Glyphs
import ru.kvizcap.quiz.app.core.Quarter
import ru.kvizcap.quiz.app.core.TopicId
import ru.kvizcap.quiz.app.mvi.AppState
import ru.kvizcap.quiz.app.mvi.Intent
import ru.kvizcap.quiz.app.mvi.Screen
import ru.kvizcap.quiz.app.ui.components.BigButton
import ru.kvizcap.quiz.app.ui.components.ButtonKind
import ru.kvizcap.quiz.app.ui.components.CapitalPill
import ru.kvizcap.quiz.app.ui.components.Card
import ru.kvizcap.quiz.app.ui.components.Coin
import ru.kvizcap.quiz.app.ui.components.Glyph
import ru.kvizcap.quiz.app.ui.components.IconBubble
import ru.kvizcap.quiz.app.ui.components.StreakChip
import ru.kvizcap.quiz.app.ui.components.fmt
import ru.kvizcap.quiz.app.ui.components.pressable
import ru.kvizcap.quiz.app.ui.quarter.QuarterMap
import ru.kvizcap.quiz.app.ui.quarter.mapHeight
import ru.kvizcap.quiz.app.ui.theme.App
import ru.kvizcap.quiz.app.ui.theme.Type
import ru.kvizcap.quiz.app.ui.components.topSafePadding
import androidx.compose.ui.text.AnnotatedString
import ru.kvizcap.quiz.app.ui.components.levelTitle
import ru.kvizcap.quiz.app.ui.components.count
import ru.kvizcap.quiz.app.ui.components.unlocksOf
import ru.kvizcap.quiz.app.ui.components.money
import ru.kvizcap.quiz.app.R
import androidx.compose.ui.res.stringResource

@Composable
fun HomeScreen(state: AppState, dispatch: (Intent) -> Unit) {
    val c = App.c
    val p = state.profile
    val stored = Economy.storedTotal(p, state.now)
    val perDay = Economy.incomePerDay(p)
    val hasBusiness = Economy.businessCount(p) > 0
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val tile = (maxWidth - 28.dp) / Quarter.SIZE
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                        .background(c.sky)
                        .topSafePadding()
                        .padding(top = 12.dp, bottom = 40.dp),
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        CapitalPill(state.liveCapital)
                        Spacer(Modifier.weight(1f))
                        StreakChip(p.dailyStreak)
                    }
                    Text(
                        incomeLine(hasBusiness, perDay, c.pri),
                        style = Type.small, color = c.ink2,
                        modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 10.dp),
                    )
                    QuarterMap(
                        profile = p, now = state.now, tile = tile,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 2.dp).height(mapHeight(tile) - tile * 0.25f),
                        plusMarks = !hasBusiness,
                        onCell = { dispatch(Intent.Tab(Screen.Quarter)) },
                    )
                }
                Box(Modifier.align(Alignment.BottomCenter).offset(y = 26.dp)) {
                    if (stored > 0) BigButton(
                        stringResource(R.string.collect, money(stored)), kind = ButtonKind.GOLD,
                        modifier = Modifier.shadow(12.dp, RoundedCornerShape(18.dp), spotColor = c.gold).border(4.dp, c.bg, RoundedCornerShape(18.dp)),
                    ) { dispatch(Intent.CollectAll) }
                    else BigButton(
                        if (hasBusiness) stringResource(R.string.to_quarter) else stringResource(R.string.build_business), kind = ButtonKind.GOLD,
                        modifier = Modifier.shadow(12.dp, RoundedCornerShape(18.dp), spotColor = c.gold).border(4.dp, c.bg, RoundedCornerShape(18.dp)),
                    ) { dispatch(Intent.Tab(Screen.Quarter)) }
                }
            }
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 44.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CareerCard(state, dispatch)
                DailyCard(state, dispatch)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModeTile(stringResource(R.string.mode_quick), stringResource(R.string.quick_sub), Glyphs.BOLT, c.priSoft, c.priSoftInk, Modifier.weight(1f)) { dispatch(Intent.StartQuick) }
                    ModeTile(stringResource(R.string.mode_blitz), stringResource(R.string.blitz_sub), Glyphs.TIMER, c.coralSoft, c.coralInk, Modifier.weight(1f)) { dispatch(Intent.StartBlitz) }
                    ModeTile(stringResource(R.string.mode_marathon), stringResource(R.string.marathon_sub), Glyphs.INFINITY, c.surface2, c.ink, Modifier.weight(1f)) { dispatch(Intent.StartMarathon) }
                }
                if (p.mistakes.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.surface).pressable { dispatch(Intent.StartErrors) }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        IconBubble(Glyphs.REDO, c.coralSoft, c.coralInk)
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.mode_errors), style = Type.label, color = c.ink)
                            Text(count(R.plurals.errors_waiting, p.mistakes.size), style = Type.small, color = c.ink2)
                        }
                        Glyph(Glyphs.CHEVRON, c.ink2, 20.dp)
                    }
                }
                Insight(state)
            }
        }
    }
}

@Composable
private fun CareerCard(state: AppState, dispatch: (Intent) -> Unit) {
    val c = App.c
    val (ch, lvl) = state.profile.currentLevel()
    val chapter = Career.chapters[ch]
    val unlock = unlocksOf(chapter)
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(24.dp), spotColor = c.pri, ambientColor = c.pri)
            .clip(RoundedCornerShape(24.dp))
            .background(c.pri)
            .pressable { dispatch(Intent.ContinueCareer) }
            .padding(start = 18.dp, end = 14.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(stringResource(R.string.continue_career), style = Type.heading.copy(fontSize = androidx.compose.ui.unit.TextUnit(18f, androidx.compose.ui.unit.TextUnitType.Sp)), color = c.priInk)
            Text(stringResource(R.string.career_line, ch + 1, stringResource(chapter.title), levelTitle(ch, lvl)), style = Type.small, color = c.priInk.copy(alpha = 0.85f))
            Text(stringResource(R.string.chapter_reward, unlock, money(chapter.reward.toLong())), style = Type.small, color = c.priInk.copy(alpha = 0.7f))
        }
        Box(Modifier.size(48.dp).background(c.priInk.copy(alpha = 0.18f), RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
            Glyph(Glyphs.PLAY, c.priInk, 22.dp, filled = true)
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.End) {
        Text(stringResource(R.string.career_map), style = Type.small, color = c.pri, modifier = Modifier.pressable { dispatch(Intent.Open(Screen.Career)) }.padding(6.dp))
    }
}

@Composable
private fun DailyCard(state: AppState, dispatch: (Intent) -> Unit) {
    val c = App.c
    val p = state.profile
    val done = p.dailyLastDay == Economy.today(state.now)
    val reward = 500 + 50 * (if (p.dailyLastDay == Economy.today(state.now) - 1) p.dailyStreak + 1 else 1).coerceAtMost(10)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(if (done) c.surface else c.goldSoft)
            .pressable(!done) { dispatch(Intent.StartDaily) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(44.dp).background(if (done) c.mintSoft else c.gold, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
            Glyph(if (done) Glyphs.CHECK else Glyphs.STAR, if (done) c.mintInk else c.goldInk, 22.dp)
        }
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.mode_daily), style = Type.label, color = c.ink)
            Text(
                if (done) count(R.plurals.daily_done, p.dailyStreak) else stringResource(R.string.daily_reward, money(reward.toLong())),
                style = Type.small, color = if (done) c.ink2 else c.goldText,
            )
        }
        if (!done) Coin(26.dp)
    }
}

@Composable
private fun incomeLine(hasBusiness: Boolean, perDay: Long, accent: androidx.compose.ui.graphics.Color): AnnotatedString {
    val template = stringResource(R.string.home_income)
    val amount = money(perDay)
    val empty = stringResource(R.string.home_build_first)
    return buildAnnotatedString {
        if (hasBusiness) {
            val parts = template.split("%1\$s")
            append(parts[0])
            withStyle(SpanStyle(color = accent)) { append(amount) }
            append(parts.getOrElse(1) { "" })
        } else append(empty)
    }
}

@Composable
private fun ModeTile(title: String, sub: String, d: String, bg: androidx.compose.ui.graphics.Color, fg: androidx.compose.ui.graphics.Color, modifier: Modifier, onClick: () -> Unit) {
    val c = App.c
    Card(modifier.pressable(onClick = onClick), padding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
        IconBubble(d, bg, fg, 38.dp, 12.dp, 20.dp)
        Spacer(Modifier.height(8.dp))
        Text(title, style = Type.label.copy(fontSize = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp)), color = c.ink, maxLines = 1)
        Text(sub, style = Type.tiny.copy(fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp)), color = c.ink2, maxLines = 1)
    }
}

@Composable
private fun Insight(state: AppState) {
    val c = App.c
    val stats = state.profile.stats.perTopic.filterValues { it.answered >= 5 }
    val text = if (stats.size >= 2) {
        val best = stats.maxBy { it.value.accuracy }
        val worst = stats.minBy { it.value.accuracy }
        stringResource(R.string.insight_compare, stringResource(TopicId.of(best.key).title), best.value.accuracy, stringResource(TopicId.of(worst.key).title))
    } else stringResource(R.string.insight_tip)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.surface2).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Glyph(Glyphs.TREND, c.pri, 20.dp)
        Text(text, style = Type.small, color = c.ink2)
    }
}
