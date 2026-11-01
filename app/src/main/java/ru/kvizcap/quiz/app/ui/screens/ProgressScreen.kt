package ru.kvizcap.quiz.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.kvizcap.quiz.app.core.Economy
import ru.kvizcap.quiz.app.core.Glyphs
import ru.kvizcap.quiz.app.core.Ranks
import ru.kvizcap.quiz.app.core.TopicId
import ru.kvizcap.quiz.app.mvi.AppState
import ru.kvizcap.quiz.app.mvi.Intent
import ru.kvizcap.quiz.app.ui.components.Bar
import ru.kvizcap.quiz.app.ui.components.Glyph
import ru.kvizcap.quiz.app.ui.components.fmt
import ru.kvizcap.quiz.app.ui.theme.App
import ru.kvizcap.quiz.app.ui.theme.Type
import java.time.format.DateTimeFormatter
import java.util.Locale
import ru.kvizcap.quiz.app.ui.components.topSafePadding
import ru.kvizcap.quiz.app.ui.components.money
import ru.kvizcap.quiz.app.R
import androidx.compose.ui.res.stringResource

@Composable
fun ProgressScreen(state: AppState, dispatch: (Intent) -> Unit) {
    val c = App.c
    val p = state.profile
    val assets = Economy.assets(p)
    val history = p.assetsHistory.toSortedMap().entries.toList().takeLast(30)
    val first = history.firstOrNull()?.value ?: assets
    val growth = if (first > 0) ((assets - first) * 100 / first) else 0
    val draw = remember { Animatable(0f) }
    LaunchedEffect(Unit) { draw.animateTo(1f, tween(1600, easing = FastOutSlowInEasing)) }
    val t = rememberInfiniteTransition(label = "dot")
    val dot by t.animateFloat(5f, 9f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "d")
    Column(Modifier.fillMaxSize().background(c.bg).verticalScroll(rememberScrollState()).topSafePadding().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.nav_progress), style = Type.title, color = c.ink)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(c.surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.assets_value), style = Type.small, color = c.ink2)
                    Text(money(assets), style = Type.title, color = c.ink)
                }
                if (history.size > 1) Row(
                    Modifier.clip(RoundedCornerShape(50)).background(if (growth >= 0) c.mintSoft else c.coralSoft).padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Glyph(Glyphs.ARROW_UP, if (growth >= 0) c.mintInk else c.coralInk, 14.dp)
                    Text("$growth%", style = Type.label.copy(fontSize = androidx.compose.ui.unit.TextUnit(13f, androidx.compose.ui.unit.TextUnitType.Sp)), color = if (growth >= 0) c.mintInk else c.coralInk)
                }
            }
            if (history.size >= 2) {
                Canvas(Modifier.fillMaxWidth().height(130.dp)) {
                    val vals = history.map { it.value.toFloat() }
                    val min = vals.min() * 0.97f
                    val max = vals.max() * 1.01f + 1f
                    val pts = vals.mapIndexed { i, v -> Offset(i / (vals.size - 1f) * (size.width - 12f) + 6f, size.height - 8f - (v - min) / (max - min) * (size.height - 20f)) }
                    listOf(0.25f, 0.55f, 0.85f).forEach { y -> drawLine(c.line, Offset(0f, size.height * y), Offset(size.width, size.height * y), 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 10f))) }
                    val visible = (pts.size * draw.value).toInt().coerceIn(1, pts.size)
                    val line = Path().apply { moveTo(pts[0].x, pts[0].y); pts.take(visible).drop(1).forEach { lineTo(it.x, it.y) } }
                    val area = Path().apply { addPath(line); lineTo(pts[visible - 1].x, size.height); lineTo(pts[0].x, size.height); close() }
                    drawPath(area, c.pri.copy(alpha = 0.12f))
                    drawPath(line, c.pri, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                    if (visible == pts.size) drawCircle(c.pri, dot, pts.last())
                }
                val fmtDate = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
                Row {
                    Text(Economy.dateOf(history.first().key).format(fmtDate), style = Type.tiny, color = c.ink2, modifier = Modifier.weight(1f))
                    Text(Economy.dateOf(history.last().key).format(fmtDate), style = Type.tiny, color = c.ink2)
                }
            } else Text(stringResource(R.string.chart_empty), style = Type.small, color = c.ink2)
        }
        val rank = Ranks.of(assets)
        val next = Ranks.all.getOrNull(rank.index + 1)
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.hero).padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ru.kvizcap.quiz.app.ui.components.Art(rank.art, Modifier.size(54.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(rank.title), style = Type.label, color = androidx.compose.ui.graphics.Color.White)
                Bar(Ranks.progressToNext(assets), c.gold, Modifier.fillMaxWidth(), androidx.compose.ui.graphics.Color.White.copy(alpha = 0.14f))
                Text(next?.let { stringResource(R.string.rank_next, stringResource(it.title), money(it.from - assets)) } ?: stringResource(R.string.rank_top), style = Type.tiny, color = c.heroInk2)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stat("${p.stats.answered}", stringResource(R.string.label_answers), Modifier.weight(1f))
            Stat("${p.stats.accuracy}%", stringResource(R.string.label_accuracy), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stat("${p.stats.bestStreak}", stringResource(R.string.label_best_streak), Modifier.weight(1f))
            Stat(money(p.stats.quarterIncome), stringResource(R.string.label_quarter_income), Modifier.weight(1f), gold = true)
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.topic_accuracy), style = Type.label, color = c.ink)
            val rows = TopicId.entries.mapNotNull { tp -> p.stats.perTopic[tp.key]?.takeIf { it.answered > 0 }?.let { tp to it } }.sortedByDescending { it.second.accuracy }
            if (rows.isEmpty()) Text(stringResource(R.string.stats_empty), style = Type.small, color = c.ink2)
            rows.forEach { (tp, st) ->
                val color = when { st.accuracy >= 80 -> c.mint; st.accuracy >= 60 -> c.gold; else -> c.coral }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(tp.title), style = Type.small, color = c.ink, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${st.accuracy}%", style = Type.numberSmall, color = c.ink)
                    }
                    Bar(st.accuracy / 100f, color, Modifier.fillMaxWidth(), height = 8.dp)
                }
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier, gold: Boolean = false) {
    val c = App.c
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(c.surface).padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, style = Type.number, color = if (gold) c.goldText else c.ink)
        Text(label, style = Type.tiny, color = c.ink2)
    }
}
