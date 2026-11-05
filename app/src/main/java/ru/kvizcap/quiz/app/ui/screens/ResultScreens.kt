package ru.kvizcap.quiz.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ru.kvizcap.quiz.app.core.ArtMeta
import ru.kvizcap.quiz.app.core.Economy
import ru.kvizcap.quiz.app.core.Glyphs
import ru.kvizcap.quiz.app.core.Quarter
import ru.kvizcap.quiz.app.mvi.AppState
import ru.kvizcap.quiz.app.mvi.Intent
import ru.kvizcap.quiz.app.mvi.Reducer
import ru.kvizcap.quiz.app.mvi.RoundMode
import ru.kvizcap.quiz.app.mvi.Screen
import ru.kvizcap.quiz.app.ui.components.BigButton
import ru.kvizcap.quiz.app.ui.components.ButtonKind
import ru.kvizcap.quiz.app.ui.components.Coin
import ru.kvizcap.quiz.app.ui.components.Flame
import ru.kvizcap.quiz.app.ui.components.Glyph
import ru.kvizcap.quiz.app.ui.components.Pill
import ru.kvizcap.quiz.app.ui.components.RoundIconButton
import ru.kvizcap.quiz.app.ui.components.TopBar
import ru.kvizcap.quiz.app.ui.components.animatedLong
import ru.kvizcap.quiz.app.ui.components.fmt
import ru.kvizcap.quiz.app.ui.components.pressable
import ru.kvizcap.quiz.app.ui.components.signed
import ru.kvizcap.quiz.app.ui.theme.App
import ru.kvizcap.quiz.app.ui.theme.Type
import ru.kvizcap.quiz.app.ui.components.topSafePadding
import ru.kvizcap.quiz.app.ui.components.modeTitle
import ru.kvizcap.quiz.app.ui.components.unlocksOf
import ru.kvizcap.quiz.app.ui.components.count
import ru.kvizcap.quiz.app.ui.components.moneyPerDay
import ru.kvizcap.quiz.app.ui.components.money
import ru.kvizcap.quiz.app.R
import androidx.compose.ui.res.stringResource

private data class Suggestion(val title: Int, val level: Int?, val cost: Long, val gain: Long, val art: String)

private fun suggestion(state: AppState): Suggestion? {
    val p = state.profile
    val capital = p.capital
    val bonus = Economy.bonusPercent(p)
    val upgrades = p.cells.mapNotNull { cell ->
        val id = cell.building ?: return@mapNotNull null
        if (cell.level >= Quarter.MAX_LEVEL) return@mapNotNull null
        val kind = Quarter.building(id)
        val cost = Quarter.upgradeCost(kind, cell.level + 1)
        if (cost > capital) return@mapNotNull null
        val gain = Economy.incomePerDay(cell.copy(level = cell.level + 1), bonus) - Economy.incomePerDay(cell, bonus)
        Suggestion(kind.title, cell.level + 1, cost.toLong(), gain, kind.art[cell.level])
    }
    upgrades.firstOrNull()?.let { return it }
    val free = Quarter.unlockedCells(p.chaptersDone).any { p.cells[it].isEmpty }
    if (!free) return null
    val kinds = Quarter.unlockedKinds(p.chaptersDone)
    val build = Quarter.buildings.filter { it.id in kinds && it.buildCost <= capital }.maxByOrNull { it.buildCost } ?: return null
    return Suggestion(build.title, null, build.buildCost.toLong(), Quarter.income(build, 1).toLong(), build.art[0])
}

@Composable
fun ResultScreen(state: AppState, dispatch: (Intent) -> Unit) {
    val res = state.result ?: return
    val c = App.c
    var stage by remember(res) { mutableStateOf(0) }
    LaunchedEffect(res) { repeat(6) { delay(220); stage++ } }
    val counted = animatedLong(if (stage >= 1) res.delta else 0, 1200)
    Column(Modifier.fillMaxSize().background(c.bg).topSafePadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(Glyphs.CROSS, { dispatch(Intent.Back) })
            Text(modeTitle(res.mode).uppercase(), style = Type.caps, color = c.ink2, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            Spacer(Modifier.size(42.dp))
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (res.mode !is RoundMode.Marathon && res.mode != RoundMode.Daily) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                    repeat(3) { i -> ResultStar(i < res.stars, if (i == 1) 66 else 50, stage >= i + 1) }
                }
            }
            Text(
                when {
                    res.chapterCompleted != null -> stringResource(R.string.res_chapter_done)
                    res.mode is RoundMode.CareerLevel && !res.passed -> stringResource(R.string.res_almost)
                    res.mode == RoundMode.Daily -> if (res.passed) stringResource(R.string.res_daily_ok) else stringResource(R.string.res_daily_fail)
                    res.mode == RoundMode.Marathon -> stringResource(R.string.res_marathon)
                    res.mode is RoundMode.CareerLevel -> stringResource(R.string.res_level_done)
                    else -> stringResource(R.string.res_round_done)
                },
                style = Type.title, color = c.ink, textAlign = TextAlign.Center,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Coin(44.dp)
                Text(signed(counted), style = Type.display, color = if (res.delta >= 0) c.pri else c.coralInk)
            }
            Text(stringResource(R.string.earned_round), style = Type.small, color = c.ink2)
            if (res.mode is RoundMode.CareerLevel && !res.passed) {
                Text(stringResource(R.string.need_5), style = Type.small, color = c.coralInk, textAlign = TextAlign.Center)
            }
            res.chapterCompleted?.let { ch ->
                AnimatedVisibility(stage >= 2, enter = fadeIn() + expandVertically()) {
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.hero).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Glyph(Glyphs.TROPHY, c.gold, 30.dp)
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.chapter_passed, stringResource(ch.title)), style = Type.label, color = Color.White)
                            val unlocks = unlocksOf(ch)
                            Text(stringResource(R.string.chapter_unlocked, money(ch.reward.toLong()), unlocks), style = Type.small, color = c.heroInk2)
                        }
                    }
                }
            }
            AnimatedVisibility(stage >= 2, enter = fadeIn() + expandVertically()) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BreakRow(stringResource(R.string.br_answers, res.correct, res.total), "+${fmt(res.base)}", c.mintInk)
                    if (res.streakBonus > 0) BreakRow(stringResource(R.string.br_streak), "+${fmt(res.streakBonus)}", c.mintInk)
                    if (res.hintsSpent > 0) BreakRow(stringResource(R.string.br_hints), "−${fmt(res.hintsSpent)}", c.coralInk)
                    Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
                    BreakRow(stringResource(R.string.br_total), stringResource(R.string.money, signed(res.delta)), c.pri, bold = true)
                }
            }
            AnimatedVisibility(stage >= 3, enter = fadeIn() + expandVertically()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniStat("${if (res.total == 0) 0 else res.correct * 100 / res.total}%", stringResource(R.string.label_accuracy), Modifier.weight(1f))
                    MiniStat("${res.bestStreak}", stringResource(R.string.label_best_streak), Modifier.weight(1f))
                    MiniStat("${state.profile.stats.bestStreak}", stringResource(R.string.label_streak_record), Modifier.weight(1f))
                }
            }
            val sug = suggestion(state)
            if (sug != null) AnimatedVisibility(stage >= 4, enter = fadeIn() + expandVertically()) {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.goldSoft).padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Image(painterResource(ArtMeta.tile(sug.art).res), null, Modifier.size(58.dp), contentScale = ContentScale.Fit)
                    Column(Modifier.weight(1f)) {
                        Text(sug.level?.let { stringResource(R.string.suggest_upgrade, stringResource(sug.title), it) } ?: stringResource(R.string.suggest_build, stringResource(sug.title)), style = Type.label, color = c.ink)
                        Text(money(sug.cost) + " · " + moneyPerDay(sug.gain), style = Type.small, color = c.goldText)
                    }
                    BigButton(stringResource(R.string.to_quarter), kind = ButtonKind.GOLD, height = 42.dp) { dispatch(Intent.Tab(Screen.Quarter)) }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigButton(stringResource(R.string.review), kind = ButtonKind.SOFT) { dispatch(Intent.Open(Screen.Review)) }
            val m = res.mode
            when {
                m is RoundMode.CareerLevel && res.passed -> BigButton(stringResource(R.string.further), Modifier.weight(1f)) { dispatch(Intent.ContinueCareer) }
                m == RoundMode.Daily -> BigButton(stringResource(R.string.home), Modifier.weight(1f)) { dispatch(Intent.Tab(Screen.Home)) }
                else -> BigButton(stringResource(R.string.again), Modifier.weight(1f)) { dispatch(Intent.Replay) }
            }
        }
    }
}

@Composable
private fun ResultStar(filled: Boolean, size: Int, show: Boolean) {
    val c = App.c
    val anim = remember { Animatable(0f) }
    LaunchedEffect(show) {
        if (show) anim.animateTo(1f, keyframes { durationMillis = 520; 1.3f at 300; 0.95f at 420 })
    }
    Box(Modifier.size(size.dp).graphicsLayer { scaleX = anim.value; scaleY = anim.value; rotationZ = (1f - anim.value.coerceAtMost(1f)) * -30f }) {
        Glyph(Glyphs.STAR, if (filled) c.gold else c.line, size.dp, filled = true)
    }
}

@Composable
private fun BreakRow(label: String, value: String, color: Color, bold: Boolean = false) {
    val c = App.c
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = if (bold) Type.label else Type.small, color = if (bold) c.ink else c.ink2, modifier = Modifier.weight(1f))
        Text(value, style = Type.numberSmall.copy(fontSize = TextUnit(if (bold) 17f else 15f, TextUnitType.Sp)), color = color)
    }
}

@Composable
private fun MiniStat(value: String, label: String, modifier: Modifier) {
    val c = App.c
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(c.surface2).padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, style = Type.number, color = c.ink)
        Text(label, style = Type.tiny, color = c.ink2, textAlign = TextAlign.Center)
    }
}

@Composable
fun ReviewScreen(state: AppState, dispatch: (Intent) -> Unit) {
    val res = (if (state.screen == Screen.HistoryReview) state.historyReview?.result else state.result) ?: return
    val c = App.c
    var open by remember(res) { mutableStateOf(res.records.indexOfFirst { !it.correct }) }
    Column(Modifier.fillMaxSize().background(c.bg)) {
        TopBar(stringResource(R.string.review_title), { dispatch(Intent.Back) })
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(stringResource(R.string.correct_n, res.correct), c.mintSoft, c.mintInk)
            val wrong = res.records.count { !it.correct && !it.skipped }
            if (wrong > 0) Pill(count(R.plurals.mistakes, wrong), c.coralSoft, c.coralInk)
            Pill(stringResource(R.string.money, signed(res.delta)), c.goldSoft, c.goldText)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            res.records.forEachIndexed { i, rec ->
                val expanded = open == i
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(c.surface)
                        .border(2.dp, if (!rec.correct && !rec.skipped) c.coral else Color.Transparent, RoundedCornerShape(18.dp))
                        .pressable { open = if (expanded) -1 else i }
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            Modifier.size(30.dp).background(if (rec.correct) c.mintSoft else if (rec.skipped) c.goldSoft else c.coral, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) { Text("${i + 1}", style = Type.numberSmall, color = if (rec.correct) c.mintInk else if (rec.skipped) c.goldText else Color.White) }
                        Text(rec.question, style = Type.small, color = c.ink, modifier = Modifier.weight(1f), maxLines = if (expanded) 6 else 2)
                        Text(if (rec.gained > 0) "+${fmt(rec.gained)}" else if (rec.skipped) stringResource(R.string.skipped) else "—", style = Type.numberSmall, color = if (rec.correct) c.mintInk else c.ink2)
                    }
                    AnimatedVisibility(expanded, enter = fadeIn() + expandVertically()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val chosenText = rec.chosenTruth?.let { stringResource(if (it) R.string.answer_true else R.string.answer_false) } ?: rec.chosen
                            val correctText = rec.correctTruth?.let { stringResource(if (it) R.string.answer_true else R.string.answer_false) } ?: rec.correctAnswer
                            if (!rec.correct && chosenText != null) Text(stringResource(R.string.your_answer, chosenText), style = Type.small, color = c.coralInk)
                            Text(stringResource(R.string.right_answer, correctText), style = Type.small, color = c.mintInk)
                            Text(rec.explanation, style = Type.body, color = c.ink2)
                            if (!rec.correct && rec.chosenTruth == null) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Glyph(Glyphs.REDO, c.priSoftInk, 16.dp)
                                Text(stringResource(R.string.added_to_errors), style = Type.tiny, color = c.priSoftInk)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BlitzScreen(state: AppState, dispatch: (Intent) -> Unit) {
    val b = state.blitz ?: return
    val bg = Color(0xFF0B130F)
    val left = ((b.endsAt - state.now).coerceAtLeast(0) + 999) / 1000
    val fraction = (b.endsAt - state.now).coerceAtLeast(0).toFloat() / Reducer.BLITZ_MS
    val card = remember(b.index) { Animatable(0f) }
    LaunchedEffect(b.index) { card.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)) }
    val t = rememberInfiniteTransition(label = "blitz")
    val pulse by t.animateFloat(1f, 1.1f, infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p")
    Column(Modifier.fillMaxSize().background(bg).topSafePadding().navigationBarsPadding().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RoundIconButton(Glyphs.CROSS, { dispatch(Intent.QuitBlitz) }, bg = Color(0xFF1A2921), fg = Color(0xFFE6F1EA))
            Text(stringResource(R.string.mode_blitz), style = Type.titleSmall, color = Color(0xFFE6F1EA), modifier = Modifier.weight(1f))
            if (b.streak >= 2) Box(Modifier.graphicsLayer { scaleX = pulse; scaleY = pulse }) {
                Pill(stringResource(R.string.combo, multLabel(b.combo)), Color(0xFF3D1D18), Color(0xFFFFC4B8), style = Type.label)
            }
        }
        Box(Modifier.fillMaxWidth().padding(top = 12.dp).height(200.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(190.dp)) {
                val stroke = 14.dp.toPx()
                drawCircle(Color(0xFF1A2921), radius = size.minDimension / 2 - stroke / 2, style = Stroke(stroke))
                drawArc(if (left <= 10) Color(0xFFFF8A75) else Color(0xFF57E0A0), -90f, 360f * fraction, false, topLeft = Offset(stroke / 2, stroke / 2), size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke), style = Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$left", style = Type.display.copy(fontSize = TextUnit(60f, TextUnitType.Sp), lineHeight = TextUnit(62f, TextUnitType.Sp)), color = Color(0xFFE6F1EA))
                Text(stringResource(R.string.seconds_word), style = Type.small, color = Color(0xFF9EB4A8))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text(stringResource(R.string.blitz_line, b.answered, b.correct) + " ", style = Type.small, color = Color(0xFF9EB4A8))
            Text("+" + money(b.delta), style = Type.small, color = Color(0xFFF3D08A))
        }
        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth().weight(1f)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationX = (1f - card.value) * 220f
                        rotationZ = (1f - card.value) * 6f
                        alpha = card.value.coerceIn(0f, 1f)
                    }
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF131E18))
                    .border(1.dp, Color(0xFF26382E), RoundedCornerShape(28.dp))
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(stringResource(R.string.statement_n, b.index + 1), style = Type.caps, color = Color(0xFF9EB4A8))
                Text(b.current.text, style = Type.question.copy(fontSize = TextUnit(22f, TextUnitType.Sp), lineHeight = TextUnit(30f, TextUnitType.Sp)), color = Color(0xFFE6F1EA))
            }
            Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                b.history.forEachIndexed { i, ok ->
                    val last = i == b.history.lastIndex
                    Box(Modifier.size(if (last) 16.dp else 12.dp).background(if (ok) Color(0xFF57E0A0) else Color(0xFFFF8A75), CircleShape))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BlitzButton(false, Modifier.weight(1f)) { dispatch(Intent.BlitzAnswer(false)) }
            BlitzButton(true, Modifier.weight(1f)) { dispatch(Intent.BlitzAnswer(true)) }
        }
    }
}

@Composable
private fun BlitzButton(value: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg = if (value) Color(0xFF57E0A0) else Color(0xFF3D1D18)
    val fg = if (value) Color(0xFF06301C) else Color(0xFFFFC4B8)
    Column(
        modifier.height(140.dp).clip(RoundedCornerShape(28.dp)).background(bg).pressable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Glyph(if (value) Glyphs.CHECK else Glyphs.CROSS, fg, 40.dp)
        Spacer(Modifier.height(8.dp))
        Text(if (value) stringResource(R.string.answer_true) else stringResource(R.string.answer_false), style = Type.button.copy(fontSize = TextUnit(20f, TextUnitType.Sp)), color = fg)
    }
}

@Composable
fun BlitzResultScreen(state: AppState, dispatch: (Intent) -> Unit) {
    val res = state.blitzResult ?: return
    val c = App.c
    val counted = animatedLong(res.delta, 1200)
    Column(Modifier.fillMaxSize().background(c.bg).topSafePadding().navigationBarsPadding().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth()) { RoundIconButton(Glyphs.CROSS, { dispatch(Intent.Back) }) }
        Spacer(Modifier.height(20.dp))
        Box(Modifier.size(96.dp).background(c.coralSoft, RoundedCornerShape(30.dp)), contentAlignment = Alignment.Center) { Glyph(Glyphs.BOLT, c.coralInk, 48.dp) }
        Text(if (res.record) stringResource(R.string.new_record) else stringResource(R.string.blitz_over), style = Type.title, color = c.ink)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Coin(40.dp)
            Text("+${fmt(counted)}", style = Type.display, color = c.pri)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniStat("${res.correct}", stringResource(R.string.label_correct_word), Modifier.weight(1f))
            MiniStat("${res.answered}", stringResource(R.string.label_answers), Modifier.weight(1f))
            MiniStat("${state.profile.stats.blitzBest}", stringResource(R.string.label_record), Modifier.weight(1f))
        }
        Text(stringResource(R.string.blitz_rule), style = Type.small, color = c.ink2, textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigButton(stringResource(R.string.home), kind = ButtonKind.SOFT) { dispatch(Intent.Tab(Screen.Home)) }
            BigButton(stringResource(R.string.more_blitz), Modifier.weight(1f)) { dispatch(Intent.StartBlitz) }
        }
    }
}
