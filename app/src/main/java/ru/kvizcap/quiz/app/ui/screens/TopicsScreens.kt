package ru.kvizcap.quiz.app.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import ru.kvizcap.quiz.app.core.Content
import ru.kvizcap.quiz.app.core.Difficulty
import ru.kvizcap.quiz.app.core.Glyphs
import ru.kvizcap.quiz.app.core.TopicId
import ru.kvizcap.quiz.app.mvi.AppState
import ru.kvizcap.quiz.app.mvi.Intent
import ru.kvizcap.quiz.app.mvi.Screen
import ru.kvizcap.quiz.app.ui.components.Bar
import ru.kvizcap.quiz.app.ui.components.BigButton
import ru.kvizcap.quiz.app.ui.components.ButtonKind
import ru.kvizcap.quiz.app.ui.components.Glyph
import ru.kvizcap.quiz.app.ui.components.RoundIconButton
import ru.kvizcap.quiz.app.ui.components.fmt
import ru.kvizcap.quiz.app.ui.components.pressable
import ru.kvizcap.quiz.app.ui.theme.App
import ru.kvizcap.quiz.app.ui.theme.Type
import ru.kvizcap.quiz.app.ui.components.topSafePadding
import ru.kvizcap.quiz.app.ui.components.money
import ru.kvizcap.quiz.app.R
import androidx.compose.ui.res.stringResource

object TopicCounts {
    var totals: Map<TopicId, Int> = emptyMap()
    fun init(content: Content) { totals = TopicId.entries.associateWith { t -> content.questions.count { it.topic == t } } }
}

@Composable
fun topicColors(t: TopicId): Pair<Color, Color> = if (App.c.dark) Color(t.tintDark) to Color(t.inkDark) else Color(t.tint) to Color(t.ink)

@Composable
fun TopicsScreen(state: AppState, dispatch: (Intent) -> Unit) {
    val c = App.c
    val stats = state.profile.stats
    val totalQuestions = TopicCounts.totals.values.sum().coerceAtLeast(1)
    val seenCount = state.profile.seen.size
    Column(Modifier.fillMaxSize().background(c.bg).verticalScroll(rememberScrollState()).topSafePadding().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.nav_topics), style = Type.title, color = c.ink)
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.hero).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            val share = seenCount.toFloat() / totalQuestions
            Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(58.dp)) {
                    val s = 6.dp.toPx()
                    drawArc(Color.White.copy(alpha = 0.16f), 0f, 360f, false, style = Stroke(s), topLeft = androidx.compose.ui.geometry.Offset(s / 2, s / 2), size = androidx.compose.ui.geometry.Size(size.width - s, size.height - s))
                    drawArc(Color(0xFFE9B44C), -90f, 360f * share, false, style = Stroke(s, cap = StrokeCap.Round), topLeft = androidx.compose.ui.geometry.Offset(s / 2, s / 2), size = androidx.compose.ui.geometry.Size(size.width - s, size.height - s))
                }
                Text("${(share * 100).toInt()}%", style = Type.numberSmall, color = Color.White)
            }
            Column {
                Text(stringResource(R.string.topics_studied, seenCount, totalQuestions), style = Type.label, color = Color.White)
                Text(if (stats.answered > 0) stringResource(R.string.topics_accuracy, stats.accuracy) else stringResource(R.string.topics_pick), style = Type.small, color = c.heroInk2)
            }
        }
        TopicId.entries.forEach { topic ->
            val (bg, fg) = topicColors(topic)
            val ts = stats.perTopic[topic.key]
            val total = TopicCounts.totals[topic] ?: 0
            val seen = state.profile.seen.keys.count { it.startsWith(topic.key + "_") }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.surface).pressable { dispatch(Intent.Open(Screen.Topic(topic))) }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(46.dp).background(bg, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Glyph(topic.icon, fg, 24.dp) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row {
                        Text(stringResource(topic.title), style = Type.label, color = c.ink, modifier = Modifier.weight(1f))
                        val acc = ts?.takeIf { it.answered > 0 }?.accuracy
                        Text(acc?.let { "$it%" } ?: stringResource(R.string.topic_new), style = Type.numberSmall, color = when {
                            acc == null -> c.pri
                            acc >= 80 -> c.mintInk
                            acc >= 60 -> c.goldText
                            else -> c.coralInk
                        })
                    }
                    Bar(if (total == 0) 0f else seen.toFloat() / total, c.pri, Modifier.fillMaxWidth(), height = 5.dp)
                    Text(stringResource(R.string.topic_seen, seen, total), style = Type.tiny, color = c.ink2)
                }
            }
        }
    }
}

@Composable
fun TopicScreen(state: AppState, topic: TopicId, dispatch: (Intent) -> Unit) {
    val c = App.c
    val (bg, fg) = topicColors(topic)
    val ts = state.profile.stats.perTopic[topic.key]
    val total = TopicCounts.totals[topic] ?: 0
    val seen = state.profile.seen.keys.count { it.startsWith(topic.key + "_") }
    val mistakes = state.profile.mistakes.count { it.startsWith(topic.key + "_") }
    val t = rememberInfiniteTransition(label = "topic")
    val bob by t.animateFloat(-4f, 4f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    Column(Modifier.fillMaxSize().background(c.bg)) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)).background(bg).topSafePadding().padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RoundIconButton(Glyphs.BACK, { dispatch(Intent.Back) })
                Spacer(Modifier.height(8.dp))
                Text(stringResource(topic.title), style = Type.title, color = c.ink, modifier = Modifier.padding(end = 110.dp))
                Text(stringResource(topic.description), style = Type.small, color = fg, modifier = Modifier.padding(end = 110.dp))
                Spacer(Modifier.height(4.dp))
            }
            Box(
                Modifier.align(Alignment.CenterEnd).size(92.dp).graphicsLayer { rotationZ = bob; translationY = bob * 1.5f }.background(fg, RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center,
            ) { Glyph(topic.icon, bg, 46.dp) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf((ts?.takeIf { it.answered > 0 }?.accuracy?.let { "$it%" } ?: "—") to stringResource(R.string.label_accuracy), "$seen/$total" to stringResource(R.string.label_studied), "${ts?.correct ?: 0}" to stringResource(R.string.label_correct)).forEach { (v, l) ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(c.surface).padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(v, style = Type.number, color = c.ink)
                        Text(l, style = Type.tiny, color = c.ink2)
                    }
                }
            }
            Text(stringResource(R.string.difficulty), style = Type.heading, color = c.ink)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Difficulty.entries.forEach { d ->
                    val on = state.topicDifficulty == d
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (on) c.priSoft else c.surface)
                            .border(2.dp, if (on) c.pri else Color.Transparent, RoundedCornerShape(18.dp))
                            .pressable { dispatch(Intent.SetTopicDifficulty(d)) }
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(stringResource(d.title), style = Type.label, color = c.ink)
                        Text(d.multLabel, style = Type.number, color = if (on) c.pri else c.ink2)
                        Text(stringResource(R.string.per_answer, money((100 * d.mult).toLong())), style = Type.tiny, color = c.ink2)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.surface2).padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Glyph(Glyphs.FLAME, Color(0xFFF08A2C), 20.dp, filled = true)
                Text(stringResource(R.string.topic_rule), style = Type.small, color = c.ink2)
            }
        }
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (mistakes > 0) BigButton(stringResource(R.string.errors_button, mistakes), kind = ButtonKind.CORAL) { dispatch(Intent.StartErrors(topic)) }
            BigButton(stringResource(R.string.start_10), Modifier.weight(1f)) { dispatch(Intent.StartTopic(topic, state.topicDifficulty)) }
        }
    }
}
