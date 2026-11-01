package ru.kvizcap.quiz.app.ui.quarter

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.kvizcap.quiz.app.core.ArtMeta
import ru.kvizcap.quiz.app.core.Career
import ru.kvizcap.quiz.app.core.Economy
import ru.kvizcap.quiz.app.core.Glyphs
import ru.kvizcap.quiz.app.core.Quarter
import ru.kvizcap.quiz.app.mvi.AppState
import ru.kvizcap.quiz.app.mvi.Intent
import ru.kvizcap.quiz.app.mvi.QuarterSheet
import ru.kvizcap.quiz.app.mvi.UpgradeFx
import ru.kvizcap.quiz.app.ui.components.BigButton
import ru.kvizcap.quiz.app.ui.components.ButtonKind
import ru.kvizcap.quiz.app.ui.components.CapitalPill
import ru.kvizcap.quiz.app.ui.components.Coin
import ru.kvizcap.quiz.app.ui.components.Glyph
import ru.kvizcap.quiz.app.ui.components.IconBubble
import ru.kvizcap.quiz.app.ui.components.Pill
import ru.kvizcap.quiz.app.ui.components.Sheet
import ru.kvizcap.quiz.app.ui.components.fmt
import ru.kvizcap.quiz.app.ui.components.pressable
import ru.kvizcap.quiz.app.ui.theme.App
import ru.kvizcap.quiz.app.ui.theme.Type
import kotlin.math.ceil
import ru.kvizcap.quiz.app.ui.components.topSafePadding
import ru.kvizcap.quiz.app.ui.components.count
import ru.kvizcap.quiz.app.ui.components.moneyPerDay
import ru.kvizcap.quiz.app.ui.components.money
import ru.kvizcap.quiz.app.R
import androidx.compose.ui.res.stringResource

private data class UpgradeHint(val cell: Int, val title: Int, val toLevel: Int, val gain: Long, val cost: Long, val days: Int)

private fun bestUpgrade(state: AppState): UpgradeHint? {
    val p = state.profile
    val bonus = Economy.bonusPercent(p)
    return p.cells.mapIndexedNotNull { i, cell ->
        val id = cell.building ?: return@mapIndexedNotNull null
        if (cell.level >= Quarter.MAX_LEVEL) return@mapIndexedNotNull null
        val kind = Quarter.building(id)
        val cost = Quarter.upgradeCost(kind, cell.level + 1).toLong()
        val gain = Economy.incomePerDay(cell.copy(level = cell.level + 1), bonus) - Economy.incomePerDay(cell, bonus)
        UpgradeHint(i, kind.title, cell.level + 1, gain, cost, ceil(cost.toDouble() / gain).toInt())
    }.minByOrNull { it.days }
}

@Composable
fun QuarterScreen(state: AppState, dispatch: (Intent) -> Unit) {
    val c = App.c
    val p = state.profile
    val stored = Economy.storedTotal(p, state.now)
    BoxWithConstraints(Modifier.fillMaxSize().background(c.sky)) {
        val tile = (maxWidth - 16.dp) / Quarter.SIZE
        SkyDecor()
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.topSafePadding().padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CapitalPill(state.liveCapital)
                    Spacer(Modifier.weight(1f))
                }
                Text(stringResource(R.string.my_quarter), style = Type.title, color = c.ink)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(moneyPerDay(Economy.incomePerDay(p)), c.priSoft, c.priSoftInk)
                    Pill(stringResource(R.string.assets_pill, money(Economy.assets(p))), c.surface, c.ink2)
                    val bonus = Economy.bonusPercent(p)
                    if (bonus > 0) Pill(stringResource(R.string.bonus_pill, bonus), c.goldSoft, c.goldText)
                }
            }
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 2.6f)
                            val limitX = size.width * (scale - 1f) / 2f + 40f
                            val limitY = size.height * (scale - 1f) / 2f + 60f
                            offset = Offset((offset.x + pan.x).coerceIn(-limitX, limitX), (offset.y + pan.y).coerceIn(-limitY, limitY))
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                QuarterMap(
                    profile = p, now = state.now, tile = tile,
                    modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y },
                    highlight = (state.sheet as? QuarterSheet.Building)?.cell,
                    onCell = { dispatch(Intent.OpenCell(it)) },
                )
            }
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 108.dp, top = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (stored > 0) BigButton(stringResource(R.string.collect, money(stored)), Modifier.fillMaxWidth(), ButtonKind.GOLD, height = 58.dp) { dispatch(Intent.CollectAll) }
                val hint = bestUpgrade(state)
                if (hint != null) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.surface).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        IconBubble(Glyphs.ARROW_UP, c.goldSoft, c.goldText)
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.upgrade_hint_title, stringResource(hint.title), hint.toLevel), style = Type.label, color = c.ink)
                            Text(moneyPerDay(hint.gain) + " · " + count(R.plurals.payback, hint.days), style = Type.small, color = c.ink2)
                        }
                        val can = state.liveCapital + Economy.stored(p.cells[hint.cell], Economy.bonusPercent(p), state.now) >= hint.cost
                        BigButton(money(hint.cost), kind = if (can) ButtonKind.PRIMARY else ButtonKind.GHOST, enabled = can, height = 42.dp) { dispatch(Intent.Upgrade(hint.cell)) }
                    }
                } else if (Economy.businessCount(p) == 0) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.surface).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        IconBubble(Glyphs.PLUS, c.priSoft, c.priSoftInk)
                        Text(stringResource(R.string.quarter_empty_hint), style = Type.small, color = c.ink2)
                    }
                }
                val locked = Quarter.SIZE * Quarter.SIZE - Quarter.unlockedCells(p.chaptersDone).size
                if (locked > 0) {
                    val next = Career.chapters.getOrNull(p.chaptersDone)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Glyph(Glyphs.LOCK, c.ink2, 16.dp)
                        Text(
                            count(R.plurals.plots_locked, locked) + (next?.let { " " + stringResource(R.string.plots_next, stringResource(it.title)) } ?: ""),
                            style = Type.small, color = c.ink2,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SkyDecor() {
    val c = App.c
    val t = rememberInfiniteTransition(label = "sky")
    val drift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(46_000, easing = LinearEasing)), label = "drift")
    val tw by t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "tw")
    Canvas(Modifier.fillMaxSize()) {
        if (!c.dark) {
            listOf(0.18f to 0f, 0.62f to 0.45f, 0.82f to 0.75f).forEachIndexed { i, (y, shift) ->
                val x = ((drift + shift) % 1f) * (size.width + 240f) - 120f
                val cy = size.height * y
                val k = if (i == 1) 0.7f else 1f
                drawOval(Color.White.copy(alpha = 0.75f), Offset(x - 60f * k, cy - 18f * k), androidx.compose.ui.geometry.Size(120f * k, 40f * k))
                drawOval(Color.White.copy(alpha = 0.75f), Offset(x - 20f * k, cy - 34f * k), androidx.compose.ui.geometry.Size(90f * k, 52f * k))
            }
        } else {
            val stars = listOf(0.1f to 0.08f, 0.3f to 0.16f, 0.52f to 0.06f, 0.7f to 0.13f, 0.9f to 0.09f, 0.18f to 0.3f, 0.82f to 0.27f, 0.45f to 0.22f, 0.64f to 0.33f, 0.05f to 0.55f, 0.95f to 0.6f)
            stars.forEachIndexed { i, (x, y) ->
                val a = if (i % 2 == 0) tw else 1.25f - tw
                drawCircle(Color(0xFFF6E7B8).copy(alpha = a.coerceIn(0.15f, 1f)), 2.2f, Offset(size.width * x, size.height * y))
            }
        }
    }
}

@Composable
fun QuarterSheets(state: AppState, dispatch: (Intent) -> Unit) {
    val sheet = state.sheet
    var last by remember { mutableStateOf<QuarterSheet?>(null) }
    if (sheet != null) last = sheet
    Sheet(visible = sheet != null, onDismiss = { dispatch(Intent.CloseSheet) }) {
        when (val s = last) {
            is QuarterSheet.Building -> BuildingContent(state, s.cell, dispatch)
            is QuarterSheet.Shop -> ShopContent(state, s.cell, dispatch)
            null -> Unit
        }
    }
}

@Composable
private fun BuildingContent(state: AppState, cellIndex: Int, dispatch: (Intent) -> Unit) {
    val c = App.c
    val p = state.profile
    val cell = p.cells[cellIndex]
    val bonus = Economy.bonusPercent(p)
    val t = rememberInfiniteTransition(label = "bob")
    val bob by t.animateFloat(0f, -6f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b")
    val ghost by t.animateFloat(0.4f, 0.7f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "g")
    if (cell.decor != null) {
        val d = Quarter.decorKind(cell.decor)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(d.title), style = Type.titleSmall, color = c.ink, modifier = Modifier.weight(1f))
            RoundCloseButton { dispatch(Intent.CloseSheet) }
        }
        Box(Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(24.dp)).background(c.sky), contentAlignment = Alignment.Center) {
            Image(painterResource(ArtMeta.tile(d.art).res), null, Modifier.size(170.dp).graphicsLayer { translationY = bob }, contentScale = ContentScale.Fit)
        }
        Text(stringResource(R.string.decor_bonus, d.bonusPercent), style = Type.label, color = c.pri)
        Text(stringResource(R.string.decor_text), style = Type.body, color = c.ink2)
        return
    }
    val id = cell.building ?: return
    val kind = Quarter.building(id)
    val income = Economy.incomePerDay(cell, bonus)
    val stored = Economy.stored(cell, bonus, state.now)
    val cap = Economy.storageCap(cell, bonus)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(kind.title), style = Type.titleSmall, color = c.ink)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.level_of, cell.level, Quarter.MAX_LEVEL), style = Type.small, color = c.ink2)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(Quarter.MAX_LEVEL) { i -> Box(Modifier.size(9.dp).background(if (i < cell.level) c.gold else c.line, CircleShape)) }
                }
            }
        }
        RoundCloseButton { dispatch(Intent.CloseSheet) }
    }
    Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(24.dp)).background(c.sky)) {
        Image(
            painterResource(ArtMeta.tile(kind.art[cell.level - 1]).res), null,
            Modifier.align(if (cell.level < Quarter.MAX_LEVEL) Alignment.CenterStart else Alignment.Center).padding(start = 12.dp).size(176.dp).graphicsLayer { translationY = bob },
            contentScale = ContentScale.Fit,
        )
        if (cell.level < Quarter.MAX_LEVEL) {
            Glyph(Glyphs.ARROW_RIGHT, c.pri, 26.dp, modifier = Modifier.align(Alignment.Center).padding(start = 40.dp))
            Image(
                painterResource(ArtMeta.tile(kind.art[cell.level]).res), null,
                Modifier.align(Alignment.CenterEnd).padding(end = 12.dp).size(124.dp).graphicsLayer { alpha = ghost },
                contentScale = ContentScale.Fit,
            )
            Text(stringResource(R.string.level_short, cell.level + 1), style = Type.tiny, color = c.ink2, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 52.dp, bottom = 10.dp))
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatBox(stringResource(R.string.income), money(income), stringResource(R.string.per_day), c.pri, Modifier.weight(1f))
        Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(c.surface2).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.stored_cap, fmt(cap)), style = Type.tiny, color = c.ink2, maxLines = 1)
            Text(money(stored), style = Type.number, color = c.ink)
            ru.kvizcap.quiz.app.ui.components.Bar(if (cap > 0) stored.toFloat() / cap else 0f, c.gold, Modifier.fillMaxWidth(), c.line, 5.dp)
        }
    }
    if (cell.level < Quarter.MAX_LEVEL) {
        val next = cell.copy(level = cell.level + 1)
        val nextIncome = Economy.incomePerDay(next, bonus)
        val cost = Quarter.upgradeCost(kind, cell.level + 1).toLong()
        val days = ceil(cost.toDouble() / (nextIncome - income)).toInt()
        Column(Modifier.fillMaxWidth().border(1.5.dp, c.line, RoundedCornerShape(20.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.upgrade_to, cell.level + 1), style = Type.label, color = c.ink)
            KeyValue(stringResource(R.string.income_per_day), stringResource(R.string.money, "${fmt(income)} → ${fmt(nextIncome)}"))
            KeyValue(stringResource(R.string.storage), stringResource(R.string.money, "${fmt(cap)} → ${fmt(nextIncome)}"))
            KeyValue(stringResource(R.string.payback_label), count(R.plurals.days, days))
        }
        val can = state.liveCapital + stored >= cost
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (stored > 0) BigButton(stringResource(R.string.collect, fmt(stored)), kind = ButtonKind.SOFT) { dispatch(Intent.CollectAll) }
            if (can) BigButton(stringResource(R.string.upgrade_for, money(cost)), Modifier.weight(1f)) { dispatch(Intent.Upgrade(cellIndex)) }
            else BigButton(stringResource(R.string.not_enough, money(cost - state.liveCapital - stored)), Modifier.weight(1f), enabled = false) {}
        }
        if (!can) Text(stringResource(R.string.round_avg), style = Type.small, color = c.goldText)
    } else {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.goldSoft).padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Glyph(Glyphs.TROPHY, c.goldText, 22.dp)
            Text(stringResource(R.string.max_level), style = Type.small, color = c.goldText)
        }
        if (stored > 0) BigButton(stringResource(R.string.collect, money(stored)), Modifier.fillMaxWidth(), ButtonKind.GOLD) { dispatch(Intent.CollectAll) }
    }
}

@Composable
private fun RoundCloseButton(onClick: () -> Unit) {
    val c = App.c
    Box(Modifier.size(40.dp).background(c.surface2, CircleShape).pressable(onClick = onClick), contentAlignment = Alignment.Center) { Glyph(Glyphs.CROSS, c.ink, 18.dp) }
}

@Composable
private fun StatBox(label: String, value: String, sub: String, color: Color, modifier: Modifier) {
    val c = App.c
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(c.surface2).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = Type.tiny, color = c.ink2)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = Type.number, color = color)
            Text(" $sub", style = Type.tiny, color = c.ink2, modifier = Modifier.padding(bottom = 2.dp))
        }
    }
}

@Composable
private fun KeyValue(k: String, v: String) {
    val c = App.c
    Row(Modifier.fillMaxWidth()) {
        Text(k, style = Type.small, color = c.ink2, modifier = Modifier.weight(1f))
        Text(v, style = Type.small, color = c.ink)
    }
}

@Composable
private fun ShopContent(state: AppState, cellIndex: Int, dispatch: (Intent) -> Unit) {
    val c = App.c
    val p = state.profile
    var tab by remember { mutableIntStateOf(0) }
    val unlocked = Quarter.unlockedKinds(p.chaptersDone)
    val bonus = Economy.bonusPercent(p)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.build_on_plot), style = Type.titleSmall, color = c.ink, modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Coin(22.dp)
            Text(fmt(state.liveCapital), style = Type.numberSmall, color = c.goldText)
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(stringResource(R.string.tab_business), stringResource(R.string.tab_decor)).forEachIndexed { i, label ->
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(if (tab == i) c.pri else c.surface2).pressable { tab = i }.padding(horizontal = 16.dp, vertical = 9.dp),
            ) { Text(label, style = Type.label.copy(fontSize = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp)), color = if (tab == i) c.priInk else c.ink) }
        }
    }
    Column(Modifier.height(420.dp).verticalScrollCompat(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (tab == 0) {
            Quarter.buildings.sortedBy { it.buildCost }.forEach { kind ->
                val open = kind.id in unlocked
                val income = Quarter.income(kind, 1).toLong() * (100 + bonus) / 100
                val days = ceil(kind.buildCost.toDouble() / income).toInt()
                ShopRow(
                    art = kind.art[0], title = stringResource(kind.title),
                    meta = if (open) moneyPerDay(income) else stringResource(R.string.opens_in_chapter, Quarter.chapterUnlocking(kind.id)?.let { stringResource(it.title) }.orEmpty()),
                    sub = if (open) count(R.plurals.payback_cap, days) else "",
                    price = kind.buildCost.toLong(), open = open, can = state.liveCapital >= kind.buildCost,
                ) { dispatch(Intent.Build(cellIndex, kind.id)) }
            }
        } else {
            Quarter.decor.forEach { d ->
                val open = d.id in unlocked
                ShopRow(
                    art = d.art, title = stringResource(d.title),
                    meta = if (open) stringResource(R.string.decor_meta, d.bonusPercent) else stringResource(R.string.opens_in_chapter, Quarter.chapterUnlocking(d.id)?.let { stringResource(it.title) }.orEmpty()),
                    sub = if (open) stringResource(R.string.decor_sub) else "",
                    price = d.cost.toLong(), open = open, can = state.liveCapital >= d.cost,
                ) { dispatch(Intent.Build(cellIndex, d.id)) }
            }
        }
    }
}

@Composable
private fun Modifier.verticalScrollCompat(): Modifier = this.then(Modifier.verticalScroll(rememberScrollState()))

@Composable
private fun ShopRow(art: String, title: String, meta: String, sub: String, price: Long, open: Boolean, can: Boolean, onBuy: () -> Unit) {
    val c = App.c
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.bg).padding(10.dp).graphicsLayer { alpha = if (open) 1f else 0.7f },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(c.sky), contentAlignment = Alignment.Center) {
            Image(painterResource(ArtMeta.tile(art).res), null, Modifier.size(58.dp), contentScale = ContentScale.Fit)
            if (!open) Box(Modifier.fillMaxSize().background(c.bg.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) { Glyph(Glyphs.LOCK, c.ink2, 20.dp) }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = Type.label, color = c.ink)
            Text(meta, style = Type.small, color = if (open) c.pri else c.ink2)
            if (sub.isNotEmpty()) Text(sub, style = Type.tiny, color = c.ink2)
        }
        BigButton(money(price), kind = if (open && can) ButtonKind.GOLD else ButtonKind.GHOST, enabled = open && can, height = 42.dp, onClick = onBuy)
    }
}

@Composable
fun UpgradeOverlay(fx: UpgradeFx?, dispatch: (Intent) -> Unit) {
    if (fx == null) return
    val isBuilding = Quarter.buildings.any { it.id == fx.kind }
    val title = stringResource(Quarter.titleOf(fx.kind))
    val art = if (isBuilding) "b_${fx.kind}_${fx.to}" else Quarter.decorKind(fx.kind).art
    val rise = remember(fx) { Animatable(0.6f) }
    val appear = remember(fx) { Animatable(0f) }
    LaunchedEffect(fx) {
        appear.animateTo(1f, tween(260))
        rise.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessLow))
    }
    val t = rememberInfiniteTransition(label = "up")
    val ring by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "ring")
    val tw by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "tw")
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = appear.value }
            .background(Brush.radialGradient(listOf(Color(0xFF14402E), Color(0xFF0B130F)), radius = 1400f))
            .pressable { },
        contentAlignment = Alignment.TopCenter,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height * 0.4f)
            listOf(0f, 0.33f, 0.66f).forEach { shift ->
                val k = (ring + shift) % 1f
                drawCircle(Color(0xFFF0C062).copy(alpha = (1f - k) * 0.6f), radius = size.width * (0.2f + 0.5f * k), center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))
            }
            listOf(0.15f to 0.25f, 0.85f to 0.22f, 0.22f to 0.5f, 0.8f to 0.47f, 0.5f to 0.18f, 0.1f to 0.38f, 0.9f to 0.36f).forEachIndexed { i, (x, y) ->
                val a = if (i % 2 == 0) tw else 1f - tw
                drawCircle(Color(0xFFF0C062).copy(alpha = a), 5f + 3f * a, Offset(size.width * x, size.height * y))
            }
        }
        Column(Modifier.topSafePadding().padding(top = 36.dp, start = 24.dp, end = 24.dp).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Pill(if (fx.built) stringResource(R.string.new_building) else stringResource(R.string.level_from_to, fx.from, fx.to), Color(0x29F0C062), Color(0xFFF3D08A), style = Type.label)
            Spacer(Modifier.height(12.dp))
            Text(if (fx.built) stringResource(R.string.built, title) else stringResource(R.string.upgraded, title), style = Type.title, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            Image(
                painterResource(ArtMeta.tile(art).res), null,
                Modifier.size(260.dp).graphicsLayer { scaleY = rise.value; scaleX = 0.85f + 0.15f * rise.value; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.85f) },
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.height(12.dp))
            if (isBuilding) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(Quarter.MAX_LEVEL) { i -> Box(Modifier.size(12.dp).background(if (i < fx.to) Color(0xFFF0C062) else Color(0xFF26382E), CircleShape)) }
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DarkStat(stringResource(R.string.income_per_day), if (fx.built) money(fx.incomeAfter) else "${fmt(fx.incomeBefore)} → ${fmt(fx.incomeAfter)}", Modifier.weight(1f))
                    DarkStat(stringResource(R.string.increase), "+" + money(fx.incomeAfter - fx.incomeBefore), Modifier.weight(1f))
                }
            } else {
                DarkStat(stringResource(R.string.quarter_bonus), "+${fx.incomeBefore}% → +${fx.incomeAfter}%", Modifier.fillMaxWidth())
            }
            Spacer(Modifier.weight(1f))
            BigButton(stringResource(R.string.great), Modifier.fillMaxWidth().padding(bottom = 36.dp), ButtonKind.GOLD, height = 58.dp) { dispatch(Intent.DismissUpgrade) }
        }
    }
}

@Composable
private fun DarkStat(label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFF13261D)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = Type.tiny, color = Color(0xFF9EB4A8))
        Text(value, style = Type.number.copy(fontSize = androidx.compose.ui.unit.TextUnit(17f, androidx.compose.ui.unit.TextUnitType.Sp)), color = Color.White)
    }
}
