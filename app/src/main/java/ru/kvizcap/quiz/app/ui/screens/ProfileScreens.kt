package ru.kvizcap.quiz.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.kvizcap.quiz.app.core.Achievements
import ru.kvizcap.quiz.app.core.Avatars
import ru.kvizcap.quiz.app.core.Difficulty
import ru.kvizcap.quiz.app.core.Economy
import ru.kvizcap.quiz.app.core.Glyphs
import ru.kvizcap.quiz.app.core.Ranks
import ru.kvizcap.quiz.app.mvi.AppState
import ru.kvizcap.quiz.app.mvi.Intent
import ru.kvizcap.quiz.app.mvi.Screen
import ru.kvizcap.quiz.app.ui.components.Glyph
import ru.kvizcap.quiz.app.ui.components.TopBar
import ru.kvizcap.quiz.app.ui.components.fmt
import ru.kvizcap.quiz.app.ui.components.pressable
import ru.kvizcap.quiz.app.ui.theme.App
import ru.kvizcap.quiz.app.ui.theme.Type
import ru.kvizcap.quiz.app.ui.components.topSafePadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.activity.compose.BackHandler
import ru.kvizcap.quiz.app.ui.components.Sheet
import ru.kvizcap.quiz.app.data.Language
import ru.kvizcap.quiz.app.data.Lang
import ru.kvizcap.quiz.app.R
import androidx.compose.ui.res.stringResource

@Composable
fun ProfileScreen(state: AppState, dispatch: (Intent) -> Unit) {
    val c = App.c
    val p = state.profile
    val assets = Economy.assets(p)
    val rank = Ranks.of(assets)
    val t = rememberInfiniteTransition(label = "prof")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(60_000, easing = LinearEasing)), label = "spin")
    val grey = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    Column(Modifier.fillMaxSize().background(c.bg).verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)).background(c.hero)) {
            Canvas(Modifier.size(300.dp).align(Alignment.TopEnd).offset(x = 90.dp, y = (-90).dp).graphicsLayer { rotationZ = spin; alpha = 0.14f }) {
                drawCircle(Color(0xFFF0C062), size.minDimension / 2 - 4f, style = Stroke(3f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 26f))))
                drawCircle(Color(0xFFF0C062), size.minDimension / 3, style = Stroke(3f))
            }
            Column(Modifier.fillMaxWidth().topSafePadding().padding(top = 12.dp, bottom = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.End) {
                    Box(Modifier.size(44.dp).background(Color.White.copy(alpha = 0.14f), CircleShape).pressable { dispatch(Intent.Open(Screen.Settings)) }, contentAlignment = Alignment.Center) {
                        Glyph(Glyphs.GEAR, Color.White, 22.dp)
                    }
                }
                Image(
                    painterResource(Avatars.all[p.avatar.coerceIn(0, Avatars.all.lastIndex)]), null,
                    Modifier.size(108.dp).clip(CircleShape).background(Color(0xFFF7E6C4)).border(4.dp, c.gold, CircleShape),
                    contentScale = ContentScale.Crop,
                )
                Text(p.name.ifBlank { stringResource(R.string.player) }, style = Type.title, color = Color.White)
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.12f)).padding(start = 6.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Image(painterResource(rank.art), null, Modifier.size(26.dp), contentScale = ContentScale.Fit)
                    Text(stringResource(R.string.rank_progress, stringResource(rank.title), (Ranks.progressToNext(assets) * 100).toInt()), style = Type.small, color = Color.White)
                }
            }
        }
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(fmt(assets) to stringResource(R.string.label_assets), "${Economy.businessCount(p)}" to stringResource(R.string.label_businesses), "${p.dailyStreak}" to stringResource(R.string.label_days_row)).forEachIndexed { i, (v, l) ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(c.surface).padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(v, style = Type.number.copy(fontSize = androidx.compose.ui.unit.TextUnit(18f, androidx.compose.ui.unit.TextUnitType.Sp)), color = if (i == 0) c.goldText else c.ink)
                        Text(l, style = Type.tiny, color = c.ink2)
                    }
                }
            }
            Text(stringResource(R.string.avatar), style = Type.heading, color = c.ink)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Avatars.all.forEachIndexed { i, res ->
                    val on = i == p.avatar
                    val scale by animateFloatAsState(if (on) 1.08f else 1f, spring(dampingRatio = 0.5f), label = "av")
                    Image(
                        painterResource(res), null,
                        Modifier.size(58.dp).graphicsLayer { scaleX = scale; scaleY = scale }.clip(CircleShape).background(c.surface2)
                            .border(3.dp, if (on) c.gold else Color.Transparent, CircleShape).pressable { dispatch(Intent.SetAvatar(i)) },
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Text(stringResource(R.string.achievements_count, p.achievements.size, Achievements.all.size), style = Type.heading, color = c.ink)
            Achievements.all.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { a ->
                        val open = a.id in p.achievements
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Image(
                                painterResource(a.art), null,
                                Modifier.size(64.dp).graphicsLayer { alpha = if (open) 1f else 0.35f },
                                contentScale = ContentScale.Fit,
                                colorFilter = if (open) null else grey,
                            )
                            Text(stringResource(a.title), style = Type.tiny, color = if (open) c.ink else c.ink2, textAlign = TextAlign.Center, maxLines = 2)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(state: AppState, dispatch: (Intent) -> Unit) {
    val c = App.c
    val s = state.profile.settings
    var pickLanguage by remember { mutableStateOf(false) }
    BackHandler(pickLanguage) { pickLanguage = false }
    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().background(c.bg)) {
        TopBar(stringResource(R.string.settings), { dispatch(Intent.Back) })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Group(stringResource(R.string.appearance)) {
                Row(Modifier.fillMaxWidth().padding(4.dp).clip(RoundedCornerShape(14.dp)).background(c.surface2).padding(4.dp)) {
                    listOf(stringResource(R.string.theme_system), stringResource(R.string.theme_light), stringResource(R.string.theme_dark)).forEachIndexed { i, label ->
                        val on = s.theme == i
                        Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (on) c.surface else Color.Transparent).pressable { dispatch(Intent.SetTheme(i)) }.padding(vertical = 11.dp), contentAlignment = Alignment.Center) {
                            Text(label, style = Type.small, color = if (on) c.ink else c.ink2)
                        }
                    }
                }
            }
            Group(stringResource(R.string.language)) {
                val current = Lang.all.firstOrNull { it.tag == state.language }?.native ?: stringResource(R.string.language_system)
                Row(Modifier.fillMaxWidth().pressable { pickLanguage = true }.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(36.dp).background(c.surface2, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Glyph(Glyphs.GLOBE, c.priSoftInk, 20.dp) }
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.language_app), style = Type.label, color = c.ink)
                        Text(current, style = Type.small, color = c.ink2)
                    }
                    Text(stringResource(R.string.change), style = Type.label, color = c.pri, modifier = Modifier.padding(6.dp))
                }
            }
            Group(stringResource(R.string.game)) {
                SwitchRow(stringResource(R.string.sound), stringResource(R.string.sound_sub), Glyphs.SOUND, s.sound) { dispatch(Intent.ToggleSound) }
                SwitchRow(stringResource(R.string.haptics), stringResource(R.string.haptics_sub), Glyphs.VIBRATE, s.haptics) { dispatch(Intent.ToggleHaptics) }
                SwitchRow(stringResource(R.string.timer), stringResource(R.string.timer_sub), Glyphs.TIMER, s.timer) { dispatch(Intent.ToggleTimer) }
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(36.dp).background(c.surface2, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Glyph(Glyphs.TREND, c.priSoftInk, 20.dp) }
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.quick_difficulty), style = Type.label, color = c.ink)
                        Text(stringResource(Difficulty.entries[s.defaultDifficulty].title), style = Type.small, color = c.ink2)
                    }
                    Text(stringResource(R.string.change), style = Type.label, color = c.pri, modifier = Modifier.pressable { dispatch(Intent.SetDefaultDifficulty((s.defaultDifficulty + 1) % 3)) }.padding(6.dp))
                }
            }
            Group(stringResource(R.string.data)) {
                Text(stringResource(R.string.native_backup), style = Type.label, color = c.pri,
                    modifier = Modifier.fillMaxWidth().pressable { dispatch(Intent.Open(Screen.Backup)) }.padding(16.dp))
                Row(Modifier.fillMaxWidth().pressable { dispatch(Intent.ConfirmReset(true)) }.padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(36.dp).background(c.coralSoft, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Glyph(Glyphs.REDO, c.coralInk, 20.dp) }
                    Column {
                        Text(stringResource(R.string.reset), style = Type.label, color = c.coralInk)
                        Text(stringResource(R.string.reset_sub), style = Type.small, color = c.ink2)
                    }
                }
            }
            Text(stringResource(R.string.footer, ru.kvizcap.quiz.app.BuildConfig.VERSION_NAME), style = Type.tiny, color = c.ink2, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            Spacer(Modifier.height(20.dp))
        }
    }
    Sheet(pickLanguage, { pickLanguage = false }) {
        Text(stringResource(R.string.language_app), style = Type.titleSmall, color = c.ink)
        Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            (listOf(Language("", stringResource(R.string.language_system))) + Lang.all).forEach { lang ->
                val on = lang.tag == state.language
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (on) c.priSoft else c.bg)
                        .pressable { pickLanguage = false; dispatch(Intent.SetLanguage(lang.tag)) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(lang.native, style = Type.label, color = if (on) c.priSoftInk else c.ink, modifier = Modifier.weight(1f))
                    Box(Modifier.size(22.dp).border(if (on) 7.dp else 2.dp, if (on) c.pri else c.line, CircleShape))
                }
            }
        }
    }
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    val c = App.c
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title.uppercase(), style = Type.caps, color = c.ink2)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.surface)) { content() }
    }
}

@Composable
private fun SwitchRow(title: String, sub: String, d: String, on: Boolean, onToggle: () -> Unit) {
    val c = App.c
    val knob by animateFloatAsState(if (on) 1f else 0f, spring(dampingRatio = 0.6f), label = "knob")
    Row(Modifier.fillMaxWidth().pressable(onClick = onToggle).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(36.dp).background(c.surface2, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Glyph(d, c.priSoftInk, 20.dp) }
        Column(Modifier.weight(1f)) {
            Text(title, style = Type.label, color = c.ink)
            Text(sub, style = Type.small, color = c.ink2)
        }
        Box(Modifier.size(50.dp, 30.dp).clip(RoundedCornerShape(50)).background(if (on) c.pri else c.line)) {
            Box(Modifier.offset(x = (3 + 20 * knob).dp, y = 3.dp).size(24.dp).background(Color.White, CircleShape))
        }
    }
}
