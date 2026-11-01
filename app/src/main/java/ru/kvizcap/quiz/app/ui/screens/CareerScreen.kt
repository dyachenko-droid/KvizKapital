package ru.kvizcap.quiz.app.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.kvizcap.quiz.app.core.ArtMeta
import ru.kvizcap.quiz.app.core.Career
import ru.kvizcap.quiz.app.core.Glyphs
import ru.kvizcap.quiz.app.core.Quarter
import ru.kvizcap.quiz.app.mvi.AppState
import ru.kvizcap.quiz.app.mvi.Intent
import ru.kvizcap.quiz.app.ui.components.Bar
import ru.kvizcap.quiz.app.ui.components.CapitalPill
import ru.kvizcap.quiz.app.ui.components.Glyph
import ru.kvizcap.quiz.app.ui.components.TopBar
import ru.kvizcap.quiz.app.ui.components.fmt
import ru.kvizcap.quiz.app.ui.components.pressable
import ru.kvizcap.quiz.app.ui.theme.App
import ru.kvizcap.quiz.app.ui.theme.Type
import ru.kvizcap.quiz.app.ui.components.unlocksOf
import ru.kvizcap.quiz.app.ui.components.money
import ru.kvizcap.quiz.app.R
import androidx.compose.ui.res.stringResource

@Composable
fun CareerScreen(state: AppState, dispatch: (Intent) -> Unit) {
    val c = App.c
    val p = state.profile
    val (curCh, curLvl) = p.currentLevel()
    var chapter by remember { mutableIntStateOf(curCh) }
    val ch = Career.chapters[chapter]
    val chapterOpen = p.isLevelOpen(chapter, 0)
    Column(Modifier.fillMaxSize().background(c.bg)) {
        TopBar(stringResource(R.string.career), { dispatch(Intent.Back) }) { CapitalPill(state.liveCapital, coin = 24.dp, style = Type.numberSmall) }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Career.chapters.forEach { item ->
                val done = p.career[item.index][Career.LEVELS - 1] >= 1
                val open = p.isLevelOpen(item.index, 0)
                val sel = item.index == chapter
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (sel) c.pri else if (done) c.priSoft else c.surface2)
                        .pressable { chapter = item.index }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (done) Glyph(Glyphs.CHECK, if (sel) c.priInk else c.priSoftInk, 14.dp)
                    if (!open) Glyph(Glyphs.LOCK, c.ink2, 14.dp)
                    Text(stringResource(item.title), style = Type.small, color = if (sel) c.priInk else if (done) c.priSoftInk else c.ink2)
                }
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(c.hero).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.career_header, chapter + 1, chapter * Career.LEVELS + 1, chapter * Career.LEVELS + Career.LEVELS), style = Type.caps, color = c.heroInk2)
                        Text(stringResource(ch.title), style = Type.titleSmall, color = androidx.compose.ui.graphics.Color.White)
                        Text(stringResource(ch.subtitle), style = Type.small, color = c.heroInk2)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${p.starsIn(chapter)}/${Career.LEVELS * 3}", style = Type.number, color = androidx.compose.ui.graphics.Color.White)
                        Text(stringResource(R.string.stars_label), style = Type.tiny, color = c.heroInk2)
                    }
                }
                Bar(p.starsIn(chapter) / (Career.LEVELS * 3f), c.gold, Modifier.fillMaxWidth(), androidx.compose.ui.graphics.Color.White.copy(alpha = 0.14f))
                val unlocks = unlocksOf(ch)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Glyph(Glyphs.TROPHY, c.gold, 18.dp)
                    Text(stringResource(R.string.career_reward, unlocks, money(ch.reward.toLong())), style = Type.small, color = androidx.compose.ui.graphics.Color(0xFFF3D08A))
                }
            }
            if (!chapterOpen) {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.surface2).padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Glyph(Glyphs.LOCK, c.ink2, 20.dp)
                    Text(stringResource(R.string.chapter_locked), style = Type.small, color = c.ink2)
                }
            }
            LevelPath(state, chapter, if (chapter == curCh) curLvl else -1, dispatch)
        }
    }
}

@Composable
private fun LevelPath(state: AppState, chapter: Int, current: Int, dispatch: (Intent) -> Unit) {
    val c = App.c
    val p = state.profile
    val t = rememberInfiniteTransition(label = "path")
    val ring by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "ring")
    val dash by t.animateFloat(0f, 36f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "dash")
    val bob by t.animateFloat(0f, -5f, infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    val ch = Career.chapters[chapter]
    val rewardArt = ch.unlocks.first().let { id -> Quarter.buildings.firstOrNull { it.id == id }?.art?.get(0) ?: Quarter.decorKind(id).art }
    BoxWithConstraints(Modifier.fillMaxWidth().height(110.dp * Career.LEVELS + 40.dp)) {
        val w = maxWidth
        val step = 110.dp
        fun pos(level: Int): Pair<Dp, Dp> {
            val row = Career.LEVELS - 1 - level
            val x = if (level % 2 == 0) w * 0.3f else w * 0.7f
            return x to (step * row + 60.dp)
        }
        Canvas(Modifier.fillMaxSize()) {
            val pts = (0 until Career.LEVELS).map { val (x, y) = pos(it); Offset(x.toPx(), y.toPx()) }
            fun segment(from: Int, to: Int): Path = Path().apply {
                moveTo(pts[from].x, pts[from].y)
                for (i in from + 1..to) {
                    val a = pts[i - 1]; val b = pts[i]
                    val my = (a.y + b.y) / 2
                    cubicTo(a.x, my, b.x, my, b.x, b.y)
                }
            }
            val doneUntil = (0 until Career.LEVELS).lastOrNull { p.career[chapter][it] >= 1 } ?: -1
            if (doneUntil < Career.LEVELS - 1) drawPath(segment((doneUntil).coerceAtLeast(0), Career.LEVELS - 1), c.line, style = Stroke(6.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 28f), -dash)))
            if (doneUntil >= 1) drawPath(segment(0, doneUntil), c.pri, style = Stroke(6.dp.toPx(), cap = StrokeCap.Round))
        }
        (0 until Career.LEVELS).forEach { level ->
            val (x, y) = pos(level)
            val stars = p.career[chapter][level]
            val open = p.isLevelOpen(chapter, level)
            val exam = Career.isExam(level)
            val isCur = level == current
            val size = when { isCur -> 84.dp; exam -> 68.dp; else -> 58.dp }
            Box(Modifier.offset(x - size / 2, y - size / 2).size(size)) {
                if (isCur) Box(Modifier.fillMaxSize().graphicsLayer { val k = 1f + 0.55f * ring; scaleX = k; scaleY = k; alpha = 1f - ring }.border(3.dp, c.gold, CircleShape))
                val shape = if (exam) RoundedCornerShape(22.dp) else CircleShape
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { if (exam) rotationZ = 45f }
                        .shadow(if (open) 6.dp else 0.dp, shape)
                        .background(
                            when {
                                stars >= 1 -> c.pri
                                isCur -> c.pri
                                exam -> c.goldSoft
                                open -> c.priSoft
                                else -> c.surface2
                            }, shape,
                        )
                        .border(if (isCur) 5.dp else if (exam) 3.dp else 0.dp, if (isCur || exam) c.gold else androidx.compose.ui.graphics.Color.Transparent, shape)
                        .pressable(open) { dispatch(Intent.StartCareer(chapter, level)) },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.graphicsLayer { if (exam) rotationZ = -45f }) {
                        when {
                            exam && stars < 1 -> Glyph(Glyphs.TROPHY, c.goldText, 28.dp)
                            !open -> Glyph(Glyphs.LOCK, c.ink2, 20.dp)
                            else -> Text("${chapter * Career.LEVELS + level + 1}", style = Type.number.copy(fontSize = androidx.compose.ui.unit.TextUnit(if (isCur) 24f else 18f, androidx.compose.ui.unit.TextUnitType.Sp)), color = if (stars >= 1 || isCur) c.priInk else c.priSoftInk)
                        }
                    }
                }
            }
            if (stars >= 1) Row(Modifier.offset(x - 27.dp, y + size / 2 + 4.dp).width(54.dp), horizontalArrangement = Arrangement.Center) {
                repeat(3) { i -> Glyph(Glyphs.STAR, if (i < stars) c.gold else c.line, 15.dp, filled = true) }
            }
            if (isCur) Text(
                stringResource(R.string.play_mult, Career.difficultyFor(level).multLabel),
                style = Type.label.copy(fontSize = androidx.compose.ui.unit.TextUnit(13f, androidx.compose.ui.unit.TextUnitType.Sp)),
                color = c.goldInk,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .offset(x - 60.dp, y + size / 2 + 8.dp + bob.dp)
                    .width(120.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.gold)
                    .pressable { dispatch(Intent.StartCareer(chapter, level)) }
                    .padding(vertical = 8.dp),
            )
            if (exam) {
                Text(stringResource(R.string.level_exam), style = Type.tiny, color = c.goldText, textAlign = TextAlign.Center, modifier = Modifier.offset(x - 60.dp, y + 44.dp).width(120.dp))
                Image(
                    painterResource(ArtMeta.tile(rewardArt).res), null,
                    Modifier.offset(x - 150.dp, y - 56.dp).size(86.dp).graphicsLayer { alpha = if (stars >= 1) 1f else 0.5f; translationY = bob },
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
}
