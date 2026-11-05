package ru.kvizcap.quiz.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.kvizcap.quiz.app.core.Achievements
import ru.kvizcap.quiz.app.core.Ranks
import ru.kvizcap.quiz.app.mvi.AppState
import ru.kvizcap.quiz.app.mvi.CollectFx
import ru.kvizcap.quiz.app.mvi.Intent
import ru.kvizcap.quiz.app.mvi.Screen
import ru.kvizcap.quiz.app.mvi.Toast
import ru.kvizcap.quiz.app.ui.components.BigButton
import ru.kvizcap.quiz.app.ui.components.BottomNav
import ru.kvizcap.quiz.app.ui.components.ButtonKind
import ru.kvizcap.quiz.app.ui.components.Coin
import ru.kvizcap.quiz.app.ui.components.fmt
import ru.kvizcap.quiz.app.ui.components.pressable
import ru.kvizcap.quiz.app.ui.quarter.QuarterScreen
import ru.kvizcap.quiz.app.ui.quarter.QuarterSheets
import ru.kvizcap.quiz.app.ui.quarter.UpgradeOverlay
import ru.kvizcap.quiz.app.ui.screens.BlitzResultScreen
import ru.kvizcap.quiz.app.ui.screens.BlitzScreen
import ru.kvizcap.quiz.app.ui.screens.CareerScreen
import ru.kvizcap.quiz.app.ui.screens.HomeScreen
import ru.kvizcap.quiz.app.ui.screens.OnboardingScreen
import ru.kvizcap.quiz.app.ui.screens.ProfileScreen
import ru.kvizcap.quiz.app.ui.screens.ProgressScreen
import ru.kvizcap.quiz.app.ui.screens.ResultScreen
import ru.kvizcap.quiz.app.ui.screens.ReviewScreen
import ru.kvizcap.quiz.app.ui.screens.RoundScreen
import ru.kvizcap.quiz.app.ui.screens.SettingsScreen
import ru.kvizcap.quiz.app.ui.screens.TopicScreen
import ru.kvizcap.quiz.app.ui.screens.TopicsScreen
import ru.kvizcap.quiz.app.ui.theme.App
import ru.kvizcap.quiz.app.ui.theme.Type
import ru.kvizcap.quiz.app.ui.components.topSafePadding
import ru.kvizcap.quiz.app.ui.components.money
import ru.kvizcap.quiz.app.R
import androidx.compose.ui.res.stringResource

private fun depth(s: Screen): Int = when (s) {
    Screen.Onboarding -> 0
    Screen.Home, Screen.Topics, Screen.Quarter, Screen.Progress, Screen.Profile -> 1
    Screen.Career, Screen.Settings, is Screen.Topic -> 2
    Screen.Round, Screen.Blitz -> 3
    Screen.Result, Screen.BlitzResult -> 4
    Screen.Review, Screen.HistoryReview -> 5
    Screen.History, Screen.Backup -> 2
}

@Composable
fun AppRoot(state: AppState, dispatch: (Intent) -> Unit, onExit: () -> Unit) {
    val c = App.c
    if (state.storageError) { ru.kvizcap.quiz.app.ui.screens.NativeStorageError(); return }
    if (!state.loaded) {
        Splash()
        return
    }
    BackHandler { dispatch(Intent.Back) }
    Box(Modifier.fillMaxSize().background(c.bg)) {
        AnimatedContent(
            targetState = state.screen,
            transitionSpec = {
                val from = depth(initialState)
                val to = depth(targetState)
                when {
                    from == to && initialState.isTab && targetState.isTab -> fadeIn(tween(220)) togetherWith fadeOut(tween(160))
                    targetState == Screen.Round || targetState == Screen.Blitz -> (slideInVertically(tween(380, easing = FastOutSlowInEasing)) { it / 5 } + fadeIn(tween(300))) togetherWith fadeOut(tween(200))
                    to > from -> (slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { it / 3 } + fadeIn(tween(300))) togetherWith (slideOutHorizontally(tween(300)) { -it / 6 } + fadeOut(tween(220)))
                    else -> (slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { -it / 3 } + fadeIn(tween(300))) togetherWith (slideOutHorizontally(tween(300)) { it / 6 } + fadeOut(tween(220)))
                }
            },
            label = "screens",
        ) { screen ->
            when (screen) {
                Screen.Onboarding -> OnboardingScreen(dispatch)
                Screen.Home -> HomeScreen(state, dispatch)
                Screen.Topics -> TopicsScreen(state, dispatch)
                Screen.Quarter -> QuarterScreen(state, dispatch)
                Screen.Progress -> ProgressScreen(state, dispatch)
                Screen.Profile -> ProfileScreen(state, dispatch)
                Screen.Career -> CareerScreen(state, dispatch)
                Screen.Settings -> SettingsScreen(state, dispatch)
                Screen.Round -> RoundScreen(state, dispatch)
                Screen.Result -> ResultScreen(state, dispatch)
                Screen.Review, Screen.HistoryReview -> ReviewScreen(state, dispatch)
                Screen.History -> ru.kvizcap.quiz.app.ui.screens.HistoryScreen(state, dispatch)
                Screen.Backup -> ru.kvizcap.quiz.app.ui.screens.ProgressBackupScreen(state)
                Screen.Blitz -> BlitzScreen(state, dispatch)
                Screen.BlitzResult -> BlitzResultScreen(state, dispatch)
                is Screen.Topic -> TopicScreen(state, screen.topic, dispatch)
            }
        }
        AnimatedVisibility(
            state.screen.isTab,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(spring(dampingRatio = 0.8f)) { it } + fadeIn(),
            exit = slideOutVertically(tween(200)) { it } + fadeOut(),
        ) {
            BottomNav(state.screen) { dispatch(Intent.Tab(it)) }
        }
        QuarterSheets(state, dispatch)
        CoinBurst(state.collectFx) { dispatch(Intent.DismissCollect) }
        UpgradeOverlay(state.upgradeFx, dispatch)
        ToastHost(state.toasts, dispatch)
        ru.kvizcap.quiz.app.ui.screens.ResumeSessionDialog(state, dispatch)
        ConfirmDialog(
            visible = state.confirmExit,
            title = stringResource(R.string.exit_title),
            text = stringResource(R.string.exit_text),
            confirm = stringResource(R.string.exit_confirm), cancel = stringResource(R.string.exit_cancel),
            onConfirm = { dispatch(Intent.ConfirmExit(false)); onExit() },
            onCancel = { dispatch(Intent.ConfirmExit(false)) },
        )
        ConfirmDialog(
            visible = state.confirmReset,
            title = stringResource(R.string.reset_title),
            text = stringResource(R.string.reset_text),
            confirm = stringResource(R.string.reset_confirm), cancel = stringResource(R.string.cancel),
            onConfirm = { dispatch(Intent.ResetProgress) },
            onCancel = { dispatch(Intent.ConfirmReset(false)) },
            danger = true,
        )
    }
}

@Composable
private fun Splash() {
    val c = App.c
    val t = rememberInfiniteTransition(label = "splash")
    val s by t.animateFloat(0.92f, 1.06f, infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "s")
    Box(Modifier.fillMaxSize().background(c.bg), contentAlignment = Alignment.Center) {
        Box(Modifier.graphicsLayer { scaleX = s; scaleY = s }) { Coin(84.dp) }
    }
}

@Composable
private fun CoinBurst(fx: CollectFx?, onDone: () -> Unit) {
    if (fx == null) return
    val progress = remember(fx.serial) { List(9) { Animatable(0f) } }
    LaunchedEffect(fx.serial) {
        progress.forEachIndexed { i, a -> launch { delay(i * 55L); a.animateTo(1f, tween(620, easing = FastOutSlowInEasing)) } }
        delay(9 * 55L + 700)
        onDone()
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val startX = maxWidth / 2
        val startY = maxHeight * 0.42f
        val endX = 34.dp
        val endY = 62.dp
        progress.forEachIndexed { i, a ->
            val v = a.value
            if (v in 0.001f..0.999f) {
                val spread = (i - 4) * 14
                val x = startX + spread.dp * (1 - v) + (endX - startX) * v
                val arc = (-120).dp * (4 * v * (1 - v))
                val y = startY + (endY - startY) * v + arc
                Box(Modifier.offset(x - 13.dp, y - 13.dp).graphicsLayer { val k = 1.1f - 0.4f * v; scaleX = k; scaleY = k }) { Coin(26.dp) }
            }
        }
        val label = remember(fx.serial) { Animatable(0f) }
        LaunchedEffect(fx.serial) { label.animateTo(1f, tween(1300)) }
        if (label.value < 1f) Text(
            "+" + money(fx.amount),
            style = Type.number.copy(fontSize = androidx.compose.ui.unit.TextUnit(26f, androidx.compose.ui.unit.TextUnitType.Sp)),
            color = App.c.goldText,
            modifier = Modifier.align(Alignment.Center).graphicsLayer { translationY = -140f * label.value; alpha = if (label.value > 0.7f) (1 - label.value) / 0.3f else 1f },
        )
    }
}

@Composable
private fun ToastHost(toasts: List<Toast>, dispatch: (Intent) -> Unit) {
    val c = App.c
    val toast = toasts.firstOrNull()
    LaunchedEffect(toast?.serial) {
        if (toast != null) {
            delay(if (toast is Toast.Message) 2600 else 3400)
            dispatch(Intent.DismissToast(toast.serial))
        }
    }
    Box(Modifier.fillMaxSize().topSafePadding().padding(top = 8.dp, start = 16.dp, end = 16.dp), contentAlignment = Alignment.TopCenter) {
        AnimatedContent(
            targetState = toast,
            transitionSpec = { (slideInVertically(spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)) { -it } + fadeIn()) togetherWith (slideOutVertically(tween(200)) { -it } + fadeOut()) },
            contentKey = { it?.serial },
            label = "toast",
        ) { t ->
            if (t != null) Row(
                Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (t is Toast.Message) c.surface else c.hero)
                    .pressable { dispatch(Intent.DismissToast(t.serial)) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (t) {
                    is Toast.AchievementUnlocked -> {
                        val a = Achievements.byId(t.id)
                        Image(painterResource(a.art), null, Modifier.size(52.dp), contentScale = ContentScale.Fit)
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.toast_achievement, stringResource(a.title)), style = Type.label, color = Color.White)
                            Text(stringResource(R.string.toast_achievement_sub, stringResource(a.description), money(Achievements.REWARD.toLong())), style = Type.small, color = c.heroInk2)
                        }
                    }
                    is Toast.RankUp -> {
                        val r = Ranks.all[t.rank]
                        Image(painterResource(r.art), null, Modifier.size(52.dp), contentScale = ContentScale.Fit)
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.toast_rank, stringResource(r.title)), style = Type.label, color = Color.White)
                            Text(stringResource(R.string.toast_rank_sub, money(r.from.toLong())), style = Type.small, color = c.heroInk2)
                        }
                    }
                    is Toast.Message -> Text(t.arg?.let { stringResource(t.text, stringResource(it)) } ?: stringResource(t.text), style = Type.label, color = c.ink, modifier = Modifier.padding(4.dp))
                }
            } else Box(Modifier.size(1.dp))
        }
    }
}

@Composable
private fun ConfirmDialog(visible: Boolean, title: String, text: String, confirm: String, cancel: String, onConfirm: () -> Unit, onCancel: () -> Unit, danger: Boolean = false) {
    val c = App.c
    AnimatedVisibility(visible, enter = fadeIn(tween(160)), exit = fadeOut(tween(160))) {
        Box(Modifier.fillMaxSize().background(c.scrim).pressable(onClick = onCancel), contentAlignment = Alignment.Center) {
            AnimatedVisibility(visible, enter = scaleIn(spring(dampingRatio = 0.65f), initialScale = 0.88f) + fadeIn()) {
                Column(
                    Modifier.padding(24.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(c.surface).pressable { }.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(title, style = Type.titleSmall, color = c.ink)
                    Text(text, style = Type.body, color = c.ink2)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BigButton(cancel, Modifier.weight(1f), ButtonKind.SOFT, height = 50.dp, onClick = onCancel)
                        BigButton(confirm, Modifier.weight(1f), if (danger) ButtonKind.CORAL else ButtonKind.PRIMARY, height = 50.dp, onClick = onConfirm)
                    }
                }
            }
        }
    }
}
