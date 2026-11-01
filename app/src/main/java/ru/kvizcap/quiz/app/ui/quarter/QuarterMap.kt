package ru.kvizcap.quiz.app.ui.quarter

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.kvizcap.quiz.app.core.ArtMeta
import ru.kvizcap.quiz.app.core.CellState
import ru.kvizcap.quiz.app.core.Economy
import ru.kvizcap.quiz.app.core.Glyphs
import ru.kvizcap.quiz.app.core.Profile
import ru.kvizcap.quiz.app.core.Quarter
import ru.kvizcap.quiz.app.ui.components.Coin
import ru.kvizcap.quiz.app.ui.components.Glyph
import ru.kvizcap.quiz.app.ui.components.fmt
import ru.kvizcap.quiz.app.ui.theme.App
import ru.kvizcap.quiz.app.ui.theme.Type
import kotlin.math.abs

private const val N = Quarter.SIZE
private const val B_RATIO = 0.29f
private const val TOP_RATIO = 1.05f
private const val BOTTOM_RATIO = 0.5f

fun mapWidth(tile: Dp): Dp = tile * N
fun mapHeight(tile: Dp): Dp = tile * (TOP_RATIO + B_RATIO * (2 * N) + BOTTOM_RATIO)

fun tileArt(cell: CellState, unlocked: Boolean): String = when {
    !unlocked -> "d_plot_locked"
    cell.building != null -> "b_${cell.building}_${cell.level}"
    cell.decor != null -> "d_${cell.decor}"
    else -> "d_plot_empty"
}

@Composable
fun QuarterMap(
    profile: Profile,
    now: Long,
    tile: Dp,
    modifier: Modifier = Modifier,
    bubbles: Boolean = true,
    plusMarks: Boolean = true,
    highlight: Int? = null,
    onCell: ((Int) -> Unit)? = null,
) {
    val c = App.c
    val density = LocalDensity.current
    val unlocked = remember(profile.chaptersDone) { Quarter.unlockedCells(profile.chaptersDone) }
    val bonus = Economy.bonusPercent(profile)
    val a = tile / 2
    val b = tile * B_RATIO
    val top = tile * TOP_RATIO
    val order = remember { (0 until N * N).sortedWith(compareBy({ (it % N) + (it / N) }, { it % N })) }
    val night = c.dark
    val nightFilter = remember(night) {
        if (!night) null else ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
            0.62f, 0f, 0f, 0f, 4f,
            0f, 0.7f, 0f, 0f, 8f,
            0f, 0f, 0.82f, 0f, 16f,
            0f, 0f, 0f, 1f, 0f,
        )))
    }
    val t = rememberInfiniteTransition(label = "map")
    val phase by t.animateFloat(0f, 6.2832f, infiniteRepeatable(tween(2400, easing = androidx.compose.animation.core.LinearEasing)), label = "phase")
    val pulse by t.animateFloat(0.9f, 1.12f, infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")

    fun center(cell: Int): Pair<Dp, Dp> {
        val i = cell % N
        val j = cell / N
        return (a * (i - j) + a * N) to (top + b * (i + j) + b)
    }

    Box(
        modifier
            .size(mapWidth(tile), mapHeight(tile))
            .then(if (onCell != null) Modifier.pointerInput(profile.chaptersDone, profile.cells) {
                detectTapGestures { pos ->
                    val ax = with(density) { a.toPx() }
                    val bx = with(density) { b.toPx() }
                    val hit = order.reversed().firstOrNull { cell ->
                        val (cxd, cyd) = center(cell)
                        val cx = with(density) { cxd.toPx() }
                        val cy = with(density) { cyd.toPx() }
                        val inDiamond = abs(pos.x - cx) / ax + abs(pos.y - cy) / bx <= 1f
                        val tall = profile.cells[cell].building != null && cell in unlocked
                        inDiamond || (tall && pos.y < cy && pos.y > cy - ax * 2.2f && abs(pos.x - cx) < ax * 0.7f)
                    }
                    if (hit != null) onCell(hit)
                }
            } else Modifier),
    ) {
        Canvas(Modifier.size(mapWidth(tile), mapHeight(tile))) {
            val ax = a.toPx()
            val bx = b.toPx()
            val tp = top.toPx()
            val slab = tile.toPx() * 0.09f
            val nTop = Offset(ax * N, tp)
            val e = Offset(ax * 2 * N, tp + bx * N)
            val s = Offset(ax * N, tp + bx * 2 * N)
            val w = Offset(0f, tp + bx * N)
            drawPath(Path().apply { moveTo(w.x, w.y); lineTo(s.x, s.y); lineTo(s.x, s.y + slab); lineTo(w.x, w.y + slab); close() }, c.groundL)
            drawPath(Path().apply { moveTo(s.x, s.y); lineTo(e.x, e.y); lineTo(e.x, e.y + slab); lineTo(s.x, s.y + slab); close() }, c.groundR)
            drawPath(Path().apply { moveTo(nTop.x, nTop.y); lineTo(e.x, e.y); lineTo(s.x, s.y); lineTo(w.x, w.y); close() }, c.ground)
        }
        order.forEach { cell ->
            val state = profile.cells[cell]
            val open = cell in unlocked
            val art = ArtMeta.tile(tileArt(state, open))
            val (cx, cy) = center(cell)
            val h = tile * art.aspect
            val left = cx - a
            val topY = cy - h * art.anchor
            val lifted = highlight == cell
            Image(
                painterResource(art.res),
                contentDescription = null,
                modifier = Modifier
                    .offset(left, topY)
                    .size(tile, h)
                    .graphicsLayer {
                        if (lifted) { translationY = -6.dp.toPx() * pulse; }
                    },
                contentScale = ContentScale.FillBounds,
                colorFilter = nightFilter,
            )
        }
        if (plusMarks) order.forEach { cell ->
            if (cell in unlocked && profile.cells[cell].isEmpty) {
                val (cx, cy) = center(cell)
                Box(
                    Modifier
                        .offset(cx - 15.dp, cy - 15.dp)
                        .size(30.dp)
                        .graphicsLayer { scaleX = pulse; scaleY = pulse }
                        .shadow(6.dp, CircleShape)
                        .background(c.pri, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Glyph(Glyphs.PLUS, c.priInk, 16.dp) }
            }
        }
        if (bubbles) order.forEach { cell ->
            val state = profile.cells[cell]
            if (state.building != null && cell in unlocked) {
                val stored = Economy.stored(state, bonus, now)
                if (stored >= 1) {
                    val art = ArtMeta.tile(tileArt(state, true))
                    val (cx, cy) = center(cell)
                    val h = tile * art.aspect
                    val bubbleTop = cy - h * art.anchor - 14.dp
                    val full = stored >= Economy.storageCap(state, bonus)
                    Row(
                        Modifier
                            .offset(cx - 34.dp, bubbleTop)
                            .graphicsLayer { translationY = -4.dp.toPx() * kotlin.math.sin(phase + cell * 1.3f) }
                            .shadow(6.dp, RoundedCornerShape(50))
                            .background(if (full) c.gold else c.surface, RoundedCornerShape(50))
                            .padding(start = 3.dp, end = 8.dp, top = 3.dp, bottom = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Coin(18.dp)
                        Text("+" + fmt(stored), style = Type.numberSmall.copy(fontSize = androidx.compose.ui.unit.TextUnit(11f, androidx.compose.ui.unit.TextUnitType.Sp)), color = if (full) c.goldInk else c.goldText, modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
        }
    }
}
