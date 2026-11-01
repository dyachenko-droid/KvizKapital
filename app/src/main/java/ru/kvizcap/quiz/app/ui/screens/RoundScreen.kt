package ru.kvizcap.quiz.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.kvizcap.quiz.app.core.Glyphs
import ru.kvizcap.quiz.app.mvi.AppState
import ru.kvizcap.quiz.app.mvi.Intent
import ru.kvizcap.quiz.app.mvi.Phase
import ru.kvizcap.quiz.app.mvi.Reducer
import ru.kvizcap.quiz.app.mvi.RoundMode
import ru.kvizcap.quiz.app.mvi.RoundState
import ru.kvizcap.quiz.app.ui.components.BigButton
import ru.kvizcap.quiz.app.ui.components.ButtonKind
import ru.kvizcap.quiz.app.ui.components.CapitalPill
import ru.kvizcap.quiz.app.ui.components.FloatingLabel
import ru.kvizcap.quiz.app.ui.components.Flame
import ru.kvizcap.quiz.app.ui.components.Glyph
import ru.kvizcap.quiz.app.ui.components.Pill
import ru.kvizcap.quiz.app.ui.components.RoundIconButton
import ru.kvizcap.quiz.app.ui.components.Sheet
import ru.kvizcap.quiz.app.ui.components.fmt
import ru.kvizcap.quiz.app.ui.components.pressable
import ru.kvizcap.quiz.app.ui.theme.App
import ru.kvizcap.quiz.app.ui.theme.Type
import ru.kvizcap.quiz.app.ui.components.topSafePadding
import ru.kvizcap.quiz.app.ui.components.money
import ru.kvizcap.quiz.app.R
import androidx.compose.ui.res.stringResource


fun multLabel(m: Double): String = "×" + String.format("%.1f", m)

@Composable
fun RoundScreen(state: AppState, dispatch: (Intent) -> Unit) {
    val r = state.round ?: return
    val c = App.c
    val rq = r.current
    val timerOn = state.profile.settings.timer && r.mode != RoundMode.Daily || state.profile.settings.timer
    Box(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxSize().topSafePadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RoundIconButton(Glyphs.PAUSE, { dispatch(Intent.Pause(true)) })
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(progressLabel(r), style = Type.tiny.copy(fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp)), color = c.ink2)
                    if (r.mode != RoundMode.Marathon && r.questions.size > 1) Segments(r)
                }
                Box {
                    CapitalPill(state.liveCapital, coin = 26.dp, style = Type.number.copy(fontSize = androidx.compose.ui.unit.TextUnit(16f, androidx.compose.ui.unit.TextUnitType.Sp)))
                    if (r.phase == Phase.REVEALED && r.lastGain > 0) {
                        FloatingLabel("+" + money(r.lastGain), c.mint, Color(0xFF06301C), r.serial, Modifier.align(Alignment.BottomEnd).padding(top = 40.dp))
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StreakBadge(r)
                Pill("${stringResource(rq.question.topic.title)} · ${stringResource(rq.question.difficulty.title).lowercase()}", c.surface2, c.priSoftInk, Modifier.weight(1f, fill = false), Type.tiny.copy(fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp)))
                Spacer(Modifier.weight(1f))
                if (state.profile.settings.timer) {
                    val left = ((r.deadline - state.now + 999) / 1000).coerceIn(0, 20)
                    val shown = if (r.paused) ((r.remainingOnPause + 999) / 1000) else if (r.phase == Phase.ASKING) left else null
                    if (shown != null) Text(stringResource(R.string.seconds_short, shown), style = Type.numberSmall, color = if (shown <= 5) c.coralInk else c.ink2)
                }
            }
            if (state.profile.settings.timer) TimerBar(state, r)
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(rq.question.text, style = Type.question, color = c.ink)
                AnimatedVisibility(r.hintShown, enter = fadeIn() + expandVertically()) {
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.goldSoft).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Glyph(Glyphs.BULB, c.goldText, 20.dp)
                        Text(rq.question.hint, style = Type.small, color = c.goldText)
                    }
                }
                Spacer(Modifier.height(2.dp))
                rq.options.forEachIndexed { i, text -> AnswerCard(r, i, text) { dispatch(Intent.Answer(i)) } }
            }
            Box(Modifier.fillMaxWidth().padding(16.dp)) {
                if (r.phase == Phase.ASKING) HintsRow(state, r, dispatch)
                else Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BigButton(stringResource(R.string.why), kind = ButtonKind.GHOST) { dispatch(Intent.Explain(true)) }
                    BigButton(if (r.isLast || (r.mode == RoundMode.Marathon && !r.wasCorrect)) stringResource(R.string.results) else stringResource(R.string.further), Modifier.weight(1f)) { dispatch(Intent.Next) }
                }
            }
        }
        Sheet(visible = r.explain, onDismiss = { dispatch(Intent.Explain(false)) }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(44.dp).background(c.goldSoft, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Glyph(Glyphs.BULB, c.goldText, 24.dp) }
                Text(stringResource(R.string.why_title), style = Type.titleSmall, color = c.ink)
            }
            val record = r.records.lastOrNull()
            if (record != null) {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.mintSoft).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Glyph(Glyphs.CHECK, c.mintInk, 20.dp)
                    Text(stringResource(R.string.correct_answer, record.correctAnswer), style = Type.label, color = c.mintInk)
                }
            }
            Text(rq.question.explanation, style = Type.body.copy(fontSize = androidx.compose.ui.unit.TextUnit(16f, androidx.compose.ui.unit.TextUnitType.Sp), lineHeight = androidx.compose.ui.unit.TextUnit(24f, androidx.compose.ui.unit.TextUnitType.Sp)), color = c.ink)
            if (r.streak >= 2) Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.flameSoft).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Flame(20.dp)
                Text(stringResource(R.string.streak_info, r.streak, multLabel(r.nextMultiplier)), style = Type.small, color = c.flameInk)
            }
            BigButton(if (r.isLast) stringResource(R.string.results) else stringResource(R.string.further), Modifier.fillMaxWidth()) { dispatch(Intent.Next) }
        }
        PauseDialog(r.paused, dispatch)
    }
}

@Composable
private fun progressLabel(r: RoundState): String = when (r.mode) {
    RoundMode.Marathon -> stringResource(R.string.marathon_progress, r.index + 1, r.correctCount)
    RoundMode.Daily -> stringResource(R.string.mode_daily)
    else -> stringResource(R.string.question_n, r.index + 1, r.questions.size)
}

@Composable
private fun Segments(r: RoundState) {
    val c = App.c
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        r.questions.indices.forEach { i ->
            val rec = r.records.getOrNull(i)
            val color = when {
                rec == null && i == r.index -> c.pri
                rec == null -> c.surface2
                rec.correct -> c.mint
                rec.skipped -> c.gold
                else -> c.coral
            }
            Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(50)).background(color))
        }
    }
}

@Composable
private fun StreakBadge(r: RoundState) {
    val c = App.c
    val bump = remember { Animatable(1f) }
    LaunchedEffect(r.streak) {
        if (r.streak > 0) {
            bump.snapTo(1f)
            bump.animateTo(1f, keyframes { durationMillis = 420; 1.22f at 120; 0.94f at 240; 1.04f at 330 })
        }
    }
    val dead = r.streak == 0 && r.records.isNotEmpty() && r.records.last().let { !it.correct && !it.skipped }
    Row(
        Modifier.graphicsLayer { scaleX = bump.value; scaleY = bump.value }
            .background(if (dead) c.surface2 else c.flameSoft, RoundedCornerShape(50))
            .padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Flame(18.dp, !dead)
        Text(if (dead) stringResource(R.string.streak_reset) else multLabel(r.nextMultiplier), style = Type.label.copy(fontSize = androidx.compose.ui.unit.TextUnit(13f, androidx.compose.ui.unit.TextUnitType.Sp)), color = if (dead) c.ink2 else c.flameInk)
    }
}

@Composable
private fun TimerBar(state: AppState, r: RoundState) {
    val c = App.c
    val left = when {
        r.phase != Phase.ASKING -> 0L
        r.paused -> r.remainingOnPause
        else -> (r.deadline - state.now).coerceAtLeast(0)
    }
    val fraction = left.toFloat() / Reducer.QUESTION_MS
    val animated by animateFloatAsState(fraction, tween(if (r.phase == Phase.ASKING) 220 else 300, easing = LinearEasing), label = "timer")
    val warn = left in 1..5_000
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).height(6.dp).clip(RoundedCornerShape(50)).background(c.surface2)) {
        Box(Modifier.fillMaxWidth(animated.coerceIn(0f, 1f)).height(6.dp).clip(RoundedCornerShape(50)).background(if (warn) c.coral else c.pri))
    }
}

@Composable
private fun AnswerCard(r: RoundState, index: Int, text: String, onClick: () -> Unit) {
    val c = App.c
    val revealed = r.phase == Phase.REVEALED
    val isCorrect = index == r.current.correctIndex
    val chosen = index == r.chosen
    val removed = index in r.removed
    val kind = when {
        !revealed && removed -> "removed"
        !revealed -> "plain"
        isCorrect && chosen -> "ok"
        isCorrect -> "okOutline"
        chosen -> "bad"
        else -> "dim"
    }
    val anim = remember(r.serial, r.index) { Animatable(0f) }
    LaunchedEffect(r.serial, r.index, kind) {
        if (kind == "ok") anim.animateTo(1f, keyframes { durationMillis = 420; 1f at 140; 0f at 420 })
        if (kind == "bad") anim.animateTo(1f, tween(360, easing = LinearEasing))
    }
    val shake = if (kind == "bad") kotlin.math.sin(anim.value * 6 * Math.PI).toFloat() * 8f * (1f - anim.value) else 0f
    val pop = if (kind == "ok") 1f + 0.045f * anim.value else 1f
    val (bg, border, badgeBg, badgeFg, textColor) = when (kind) {
        "ok" -> Quint(c.mintSoft, c.mint, c.mint, Color.White, c.mintInk)
        "okOutline" -> Quint(c.surface, c.mint, c.mintSoft, c.mintInk, c.mintInk)
        "bad" -> Quint(c.coralSoft, c.coral, c.coral, Color.White, c.coralInk)
        else -> Quint(c.surface, c.line, c.surface2, c.priSoftInk, c.ink)
    }
    val alpha by animateFloatAsState(if (kind == "dim" || kind == "removed") 0.4f else 1f, tween(250), label = "a")
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .graphicsLayer { translationX = shake; scaleX = pop; scaleY = pop; this.alpha = alpha }
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .border(if (kind == "plain" || kind == "dim" || kind == "removed") 1.5.dp else 2.dp, border, RoundedCornerShape(18.dp))
            .pressable(!revealed && !removed && !r.paused, onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(34.dp).background(badgeBg, CircleShape), contentAlignment = Alignment.Center) {
            when (kind) {
                "ok", "okOutline" -> Glyph(Glyphs.CHECK, badgeFg, 18.dp)
                "bad" -> Glyph(Glyphs.CROSS, badgeFg, 16.dp)
                else -> Text(stringResource(R.string.option_letters).getOrElse(index) { '?' }.toString(), style = Type.label, color = badgeFg)
            }
        }
        Text(
            text,
            style = Type.label.copy(fontSize = androidx.compose.ui.unit.TextUnit(16f, androidx.compose.ui.unit.TextUnitType.Sp), lineHeight = androidx.compose.ui.unit.TextUnit(21f, androidx.compose.ui.unit.TextUnitType.Sp)),
            color = textColor,
            textDecoration = if (kind == "removed") androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
        )
    }
}

private data class Quint(val a: Color, val b: Color, val c: Color, val d: Color, val e: Color)

@Composable
private fun HintsRow(state: AppState, r: RoundState, dispatch: (Intent) -> Unit) {
    val c = App.c
    val capital = state.liveCapital
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        HintButton(stringResource(R.string.hint_fifty), Reducer.COST_FIFTY, Glyphs.FIFTY, r.removed.isEmpty() && capital >= Reducer.COST_FIFTY, Modifier.weight(1f)) { dispatch(Intent.UseFifty) }
        HintButton(stringResource(R.string.hint_hint), Reducer.COST_HINT, Glyphs.BULB, !r.hintShown && capital >= Reducer.COST_HINT, Modifier.weight(1f)) { dispatch(Intent.UseHint) }
        if (r.mode != RoundMode.Daily) HintButton(stringResource(R.string.hint_skip), Reducer.COST_SKIP, Glyphs.SKIP, capital >= Reducer.COST_SKIP, Modifier.weight(1f)) { dispatch(Intent.Skip) }
    }
}

@Composable
private fun HintButton(title: String, cost: Long, d: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = App.c
    Column(
        modifier
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
            .clip(RoundedCornerShape(18.dp))
            .background(c.surface)
            .pressable(enabled, onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Glyph(d, c.ink, 22.dp)
        Text(title, style = Type.label.copy(fontSize = androidx.compose.ui.unit.TextUnit(13f, androidx.compose.ui.unit.TextUnitType.Sp)), color = c.ink)
        Text("−" + money(cost), style = Type.numberSmall.copy(fontSize = androidx.compose.ui.unit.TextUnit(11f, androidx.compose.ui.unit.TextUnitType.Sp)), color = c.coralInk)
    }
}

@Composable
private fun PauseDialog(visible: Boolean, dispatch: (Intent) -> Unit) {
    val c = App.c
    AnimatedVisibility(visible, enter = fadeIn(tween(180)), exit = fadeOut(tween(180))) {
        Box(Modifier.fillMaxSize().background(c.scrim).pressable { }, contentAlignment = Alignment.Center) {
            AnimatedVisibility(visible, enter = scaleIn(spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow), initialScale = 0.85f) + fadeIn()) {
                Column(
                    Modifier.padding(24.dp).fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(c.surface).padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.size(64.dp).background(c.priSoft, RoundedCornerShape(22.dp)), contentAlignment = Alignment.Center) { Glyph(Glyphs.PAUSE, c.pri, 30.dp) }
                    Text(stringResource(R.string.pause), style = Type.title, color = c.ink)
                    Text(stringResource(R.string.pause_text), style = Type.body, color = c.ink2, textAlign = TextAlign.Center)
                    BigButton(stringResource(R.string.resume), Modifier.fillMaxWidth()) { dispatch(Intent.Pause(false)) }
                    BigButton(stringResource(R.string.restart), Modifier.fillMaxWidth(), ButtonKind.SOFT, height = 50.dp) { dispatch(Intent.Restart) }
                    Text(stringResource(R.string.quit_nosave), style = Type.label, color = c.coralInk, modifier = Modifier.pressable { dispatch(Intent.QuitRound) }.padding(10.dp))
                }
            }
        }
    }
}
