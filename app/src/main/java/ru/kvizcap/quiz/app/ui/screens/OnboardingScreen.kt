package ru.kvizcap.quiz.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import ru.kvizcap.quiz.app.R
import ru.kvizcap.quiz.app.core.Glyphs
import ru.kvizcap.quiz.app.core.TopicId
import ru.kvizcap.quiz.app.mvi.Intent
import ru.kvizcap.quiz.app.ui.components.BigButton
import ru.kvizcap.quiz.app.ui.components.Coin
import ru.kvizcap.quiz.app.ui.components.Glyph
import ru.kvizcap.quiz.app.ui.components.pressable
import ru.kvizcap.quiz.app.ui.theme.App
import ru.kvizcap.quiz.app.ui.theme.Type
import ru.kvizcap.quiz.app.ui.components.topSafePadding
import androidx.compose.ui.res.stringResource

private data class Slide(val art: Int, val title: Int, val text: Int)

private val slides = listOf(
    Slide(R.drawable.onb_1, R.string.onb_1_title, R.string.onb_1_text),
    Slide(R.drawable.onb_2, R.string.onb_2_title, R.string.onb_2_text),
    Slide(R.drawable.onb_3, R.string.onb_3_title, R.string.onb_3_text),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(dispatch: (Intent) -> Unit) {
    val c = App.c
    var page by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var level by remember { mutableIntStateOf(1) }
    var interests by remember { mutableStateOf(setOf(TopicId.BUDGET.key, TopicId.CREDIT.key, TopicId.FRAUD.key)) }
    val t = rememberInfiniteTransition(label = "onb")
    val float by t.animateFloat(0f, -10f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "f")
    Box(Modifier.fillMaxSize().background(c.bg)) {
        AnimatedContent(
            targetState = page,
            transitionSpec = { (slideInHorizontally(tween(380)) { it / 3 } + fadeIn(tween(380))) togetherWith (slideOutHorizontally(tween(300)) { -it / 3 } + fadeOut(tween(250))) },
            label = "onb",
        ) { current ->
            if (current < slides.size) {
                val s = slides[current]
                Column(Modifier.fillMaxSize().topSafePadding().navigationBarsPadding().padding(horizontal = 24.dp)) {
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.End) {
                        Text(stringResource(R.string.skip), style = Type.label, color = c.ink2, modifier = Modifier.pressable { page = slides.size }.padding(10.dp))
                    }
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(1f).padding(vertical = 8.dp).clip(RoundedCornerShape(36.dp)).background(androidx.compose.ui.graphics.Color(0xFFE1F0E5)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(painterResource(s.art), null, Modifier.fillMaxSize().graphicsLayer { translationY = float }, contentScale = ContentScale.Crop)
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(stringResource(s.title), style = Type.title, color = c.ink)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(s.text), style = Type.body.copy(fontSize = androidx.compose.ui.unit.TextUnit(16f, androidx.compose.ui.unit.TextUnitType.Sp), lineHeight = androidx.compose.ui.unit.TextUnit(24f, androidx.compose.ui.unit.TextUnitType.Sp)), color = c.ink2)
                    Spacer(Modifier.weight(1f))
                    Row(Modifier.padding(bottom = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            slides.indices.forEach { i ->
                                val w by animateDpAsState(if (i == current) 28.dp else 8.dp, label = "dot")
                                Box(Modifier.height(8.dp).width(w).clip(CircleShape).background(if (i == current) c.pri else c.line))
                            }
                        }
                        BigButton(stringResource(R.string.next)) { page = current + 1 }
                    }
                }
            } else {
                Column(Modifier.fillMaxSize().topSafePadding().navigationBarsPadding().imePadding().padding(horizontal = 20.dp)) {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Spacer(Modifier.height(24.dp))
                        Text(stringResource(R.string.onb_start_title), style = Type.title, color = c.ink)
                        Text(stringResource(R.string.onb_start_text), style = Type.body, color = c.ink2)
                        Text(stringResource(R.string.onb_name_label), style = Type.heading, color = c.ink)
                        BasicTextField(
                            value = name,
                            onValueChange = { name = it.take(24) },
                            singleLine = true,
                            textStyle = Type.label.copy(color = c.ink, fontSize = androidx.compose.ui.unit.TextUnit(17f, androidx.compose.ui.unit.TextUnitType.Sp)),
                            cursorBrush = SolidColor(c.pri),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            decorationBox = { inner ->
                                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).border(1.5.dp, c.line, RoundedCornerShape(16.dp)).padding(16.dp)) {
                                    if (name.isEmpty()) Text(stringResource(R.string.onb_name_hint), style = Type.label, color = c.ink2)
                                    inner()
                                }
                            },
                        )
                        val options = listOf(
                            Triple(R.string.onb_level_1, R.string.onb_level_1_desc, Glyphs.CLOCK),
                            Triple(R.string.onb_level_2, R.string.onb_level_2_desc, Glyphs.TREND),
                            Triple(R.string.onb_level_3, R.string.onb_level_3_desc, Glyphs.TROPHY),
                        )
                        options.forEachIndexed { i, (title, desc, d) ->
                            val on = i == level
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (on) c.priSoft else c.surface)
                                    .border(2.dp, if (on) c.pri else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(20.dp))
                                    .pressable { level = i }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(Modifier.size(44.dp).background(if (on) c.pri else c.surface2, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Glyph(d, if (on) c.priInk else c.priSoftInk, 22.dp) }
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(title), style = Type.label, color = c.ink)
                                    Text(stringResource(desc), style = Type.small, color = c.ink2)
                                }
                                Box(Modifier.size(22.dp).border(if (on) 7.dp else 2.dp, if (on) c.pri else c.line, CircleShape))
                            }
                        }
                        Text(stringResource(R.string.onb_interests), style = Type.heading, color = c.ink)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TopicId.entries.forEach { topic ->
                                val on = topic.key in interests
                                Text(
                                    stringResource(topic.title),
                                    style = Type.label.copy(fontSize = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp)),
                                    color = if (on) c.priInk else c.ink,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(if (on) c.pri else c.surface)
                                        .border(1.5.dp, if (on) c.pri else c.line, RoundedCornerShape(50))
                                        .pressable { interests = if (on) interests - topic.key else interests + topic.key }
                                        .padding(horizontal = 14.dp, vertical = 9.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.onb_start_capital) + " ", style = Type.small, color = c.ink2)
                        Coin(22.dp)
                        Text(" 1 000", style = Type.numberSmall, color = c.goldText)
                    }
                    BigButton(stringResource(R.string.start), Modifier.fillMaxWidth().padding(bottom = 16.dp), height = 58.dp) {
                        dispatch(Intent.FinishOnboarding(name.trim(), level, interests.toList()))
                    }
                }
            }
        }
    }
}
