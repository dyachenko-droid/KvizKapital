package ru.kvizcap.quiz.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import ru.kvizcap.quiz.app.core.Glyphs
import ru.kvizcap.quiz.app.mvi.Screen
import ru.kvizcap.quiz.app.ui.theme.App
import ru.kvizcap.quiz.app.ui.theme.Type
import ru.kvizcap.quiz.app.ui.theme.Unbounded
import kotlin.math.abs
import ru.kvizcap.quiz.app.R
import androidx.compose.ui.res.stringResource

private val vectorCache = HashMap<String, ImageVector>()

private fun vectorOf(d: String, filled: Boolean): ImageVector = vectorCache.getOrPut(d + filled) {
    ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        val nodes = PathParser().parsePathString(d).toNodes()
        if (filled) addPath(nodes, fill = SolidColor(Color.Black))
        else addPath(nodes, stroke = SolidColor(Color.Black), strokeLineWidth = 1.9f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
    }.build()
}

@Composable
fun Glyph(d: String, color: Color, size: Dp = 22.dp, filled: Boolean = false, modifier: Modifier = Modifier) {
    val painter = rememberVectorPainter(vectorOf(d, filled))
    androidx.compose.foundation.Canvas(modifier.size(size)) {
        with(painter) { draw(this@Canvas.size, colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(color)) }
    }
}

fun fmt(value: Long): String {
    val s = abs(value).toString().reversed().chunked(3).joinToString(" ").reversed()
    return if (value < 0) "−$s" else s
}

fun signed(value: Long): String = if (value >= 0) "+${fmt(value)}" else fmt(value)

@Composable
fun Coin(size: Dp = 28.dp, modifier: Modifier = Modifier) {
    val c = App.c
    Box(
        modifier
            .size(size)
            .background(c.gold, CircleShape)
            .border((size.value / 14).dp, Color(0x2E3D2A05), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(R.string.coin_letter), style = TextStyle(fontFamily = Unbounded, fontSize = (size.value * 0.46f).sp, color = c.goldInk, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
    }
}

@Composable
fun animatedLong(target: Long, durationMs: Int = 700): Long {
    val anim = remember { Animatable(target.toFloat()) }
    LaunchedEffect(target) { anim.animateTo(target.toFloat(), tween(durationMs, easing = FastOutSlowInEasing)) }
    return anim.value.toLong()
}

@Composable
fun CapitalPill(value: Long, modifier: Modifier = Modifier, coin: Dp = 30.dp, style: TextStyle = Type.number) {
    val c = App.c
    val shown = animatedLong(value)
    val bump = remember { Animatable(1f) }
    LaunchedEffect(value) {
        bump.snapTo(1f)
        bump.animateTo(1.08f, tween(120))
        bump.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium))
    }
    Row(
        modifier
            .scale(bump.value)
            .shadow(6.dp, RoundedCornerShape(50), ambientColor = c.shadow, spotColor = c.shadow)
            .background(c.surface, RoundedCornerShape(50))
            .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Coin(coin)
        Text(fmt(shown), style = style, color = c.ink)
    }
}

fun Modifier.topSafePadding(): Modifier = composed { windowInsetsPadding(WindowInsets.displayCutout.union(WindowInsets.statusBars).only(WindowInsetsSides.Top)) }

fun Modifier.pressable(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow), label = "press")
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
}

fun Modifier.shimmer(color: Color = Color.White.copy(alpha = 0.45f), periodMs: Int = 2800): Modifier = composed {
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(-0.5f, 1.8f, infiniteRepeatable(keyframes { durationMillis = periodMs; -0.5f at 0; 1.8f at (periodMs * 0.55f).toInt(); 1.8f at periodMs }), label = "sx")
    drawWithContent {
        drawContent()
        val w = size.width * 0.35f
        val left = size.width * x
        drawRect(
            Brush.linearGradient(listOf(Color.Transparent, color, Color.Transparent), start = Offset(left, 0f), end = Offset(left + w, size.height)),
            topLeft = Offset(left, 0f),
            size = androidx.compose.ui.geometry.Size(w, size.height),
        )
    }
}

@Composable
fun BigButton(
    text: String,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.PRIMARY,
    enabled: Boolean = true,
    height: Dp = 56.dp,
    leading: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val c = App.c
    val (bg, fg) = when {
        !enabled -> c.surface2 to c.ink2
        kind == ButtonKind.PRIMARY -> c.pri to c.priInk
        kind == ButtonKind.GOLD -> c.gold to c.goldInk
        kind == ButtonKind.SOFT -> c.priSoft to c.priSoftInk
        kind == ButtonKind.CORAL -> c.coralSoft to c.coralInk
        else -> c.surface to c.ink
    }
    Row(
        modifier
            .height(height)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .then(if (enabled && kind == ButtonKind.GOLD) Modifier.shimmer() else Modifier)
            .then(if (kind == ButtonKind.GHOST) Modifier.border(1.5.dp, c.line, RoundedCornerShape(18.dp)) else Modifier)
            .pressable(enabled, onClick)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.let { it(); Spacer(Modifier.width(8.dp)) }
        Text(text, style = Type.button, color = fg, textAlign = TextAlign.Center, maxLines = 2)
    }
}

enum class ButtonKind { PRIMARY, GOLD, SOFT, CORAL, GHOST }

@Composable
fun Pill(text: String, bg: Color, fg: Color, modifier: Modifier = Modifier, style: TextStyle = Type.small, leading: (@Composable () -> Unit)? = null) {
    Row(
        modifier.background(bg, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        leading?.invoke()
        Text(text, style = style, color = fg, maxLines = 1)
    }
}

@Composable
fun Flame(size: Dp = 20.dp, alive: Boolean = true) {
    val t = rememberInfiniteTransition(label = "flame")
    val s by t.animateFloat(1f, 1.15f, infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "fs")
    Box(Modifier.graphicsLayer { if (alive) { scaleX = s; scaleY = s } }) {
        Glyph(Glyphs.FLAME, if (alive) Color(0xFFF08A2C) else App.c.ink2, size, filled = true)
    }
}

@Composable
fun StreakChip(days: Int, modifier: Modifier = Modifier) {
    val c = App.c
    Pill("$days", c.flameSoft, c.flameInk, modifier, Type.label) { Flame(20.dp, days > 0) }
}

@Composable
fun Bar(progress: Float, color: Color, modifier: Modifier = Modifier, track: Color = App.c.surface2, height: Dp = 8.dp) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing), label = "bar")
    Box(modifier.height(height).clip(RoundedCornerShape(50)).background(track)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(p).clip(RoundedCornerShape(50)).background(color))
    }
}

@Composable
fun Card(modifier: Modifier = Modifier, padding: PaddingValues = PaddingValues(16.dp), color: Color = App.c.surface, content: @Composable ColumnScope.() -> Unit) {
    val c = App.c
    Column(
        modifier
            .shadow(if (c.dark) 0.dp else 3.dp, RoundedCornerShape(24.dp), ambientColor = c.shadow, spotColor = c.shadow)
            .background(color, RoundedCornerShape(24.dp))
            .padding(padding),
        content = content,
    )
}

@Composable
fun IconBubble(d: String, bg: Color, fg: Color, size: Dp = 44.dp, radius: Dp = 14.dp, icon: Dp = 22.dp) {
    Box(Modifier.size(size).background(bg, RoundedCornerShape(radius)), contentAlignment = Alignment.Center) { Glyph(d, fg, icon) }
}

@Composable
fun RoundIconButton(d: String, onClick: () -> Unit, modifier: Modifier = Modifier, bg: Color = App.c.surface, fg: Color = App.c.ink, size: Dp = 42.dp) {
    val c = App.c
    Box(
        modifier
            .size(size)
            .shadow(if (c.dark || bg == Color.Transparent) 0.dp else 4.dp, CircleShape, ambientColor = c.shadow, spotColor = c.shadow)
            .background(bg, CircleShape)
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Glyph(d, fg, size * 0.48f) }
}

@Composable
fun TopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier.fillMaxWidth().topSafePadding().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RoundIconButton(Glyphs.BACK, onBack)
        Text(title, style = Type.titleSmall, color = App.c.ink, modifier = Modifier.weight(1f), maxLines = 1)
        trailing()
    }
}

@Composable
fun BottomNav(current: Screen, onTab: (Screen) -> Unit) {
    val c = App.c
    val items = listOf(Triple(Screen.Home, stringResource(R.string.nav_home), Glyphs.HOME), Triple(Screen.Topics, stringResource(R.string.nav_topics), Glyphs.BOOK), Triple(Screen.Quarter, stringResource(R.string.nav_quarter), Glyphs.CITY), Triple(Screen.Progress, stringResource(R.string.nav_progress), Glyphs.CHART), Triple(Screen.Profile, stringResource(R.string.nav_profile), Glyphs.USER))
    Box(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp), ambientColor = c.shadow, spotColor = c.shadow)
                .background(c.surface, RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(70.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { (screen, label, d) ->
                val active = screen == current
                Column(
                    Modifier.weight(1f).fillMaxHeight().pressable { onTab(screen) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (screen == Screen.Quarter) {
                        Spacer(Modifier.height(30.dp))
                        Text(label, style = Type.tiny, color = if (active) c.pri else c.ink2)
                    } else {
                        val lift by animateFloatAsState(if (active) -3f else 0f, spring(dampingRatio = 0.5f), label = "navlift")
                        Box(Modifier.offset(y = lift.dp)) { Glyph(d, if (active) c.pri else c.ink2, 24.dp) }
                        Spacer(Modifier.height(4.dp))
                        Text(label, style = Type.tiny, color = if (active) c.pri else c.ink2)
                    }
                }
            }
        }
        val active = current == Screen.Quarter
        val glow = rememberInfiniteTransition(label = "qglow")
        val g by glow.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "qg")
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-22).dp)
                .size(62.dp)
                .graphicsLayer { val k = 1f + 0.03f * kotlin.math.sin(g * 6.283f); scaleX = k; scaleY = k }
                .shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = c.pri, spotColor = c.pri)
                .background(if (active) c.gold else c.pri, RoundedCornerShape(20.dp))
                .border(4.dp, c.surface, RoundedCornerShape(20.dp))
                .pressable { onTab(Screen.Quarter) },
            contentAlignment = Alignment.Center,
        ) { Glyph(Glyphs.CITY, if (active) c.goldInk else c.priInk, 28.dp) }
    }
}

@Composable
fun Sheet(visible: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val c = App.c
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
            Box(Modifier.fillMaxSize().background(c.scrim).clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss))
        }
        AnimatedVisibility(
            visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn(),
            exit = slideOutVertically(tween(220)) { it } + fadeOut(),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(c.surface, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .clickable(remember { MutableInteractionSource() }, null) {}
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.align(Alignment.CenterHorizontally).size(40.dp, 4.dp).background(c.line, RoundedCornerShape(50)))
                content()
            }
        }
    }
}

@Composable
fun Art(res: Int, modifier: Modifier = Modifier, scale: ContentScale = ContentScale.Fit) {
    Image(painterResource(res), null, modifier, contentScale = scale)
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = Type.heading, color = App.c.ink, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun BoxScope.FloatingLabel(text: String, bg: Color, fg: Color, key: Any, modifier: Modifier = Modifier) {
    val anim = remember(key) { Animatable(0f) }
    LaunchedEffect(key) { anim.animateTo(1f, tween(1400, easing = FastOutSlowInEasing)) }
    val v = anim.value
    if (v < 1f) {
        Box(
            modifier.graphicsLayer {
                translationY = -60f * v
                alpha = if (v < 0.15f) v / 0.15f else if (v > 0.7f) (1f - v) / 0.3f else 1f
                val s = if (v < 0.15f) 0.8f + v * 2f else 1.1f - 0.1f * v
                scaleX = s; scaleY = s
            }.background(bg, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 6.dp),
        ) { Text(text, style = Type.numberSmall.copy(fontSize = 15.sp), color = fg) }
    }
}
