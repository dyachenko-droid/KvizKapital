package ru.kvizcap.quiz.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.kvizcap.quiz.app.BuildConfig
import ru.kvizcap.quiz.app.R
import ru.kvizcap.quiz.app.core.weakestTopic
import ru.kvizcap.quiz.app.data.*
import ru.kvizcap.quiz.app.mvi.*
import ru.kvizcap.quiz.app.ui.components.*
import ru.kvizcap.quiz.app.ui.components.Card as AppCard
import ru.kvizcap.quiz.app.ui.theme.App
import ru.kvizcap.quiz.app.ui.theme.Type
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun NativeStorageError(store: Store = viewModel()) {
    val c = App.c
    var restoring by remember { mutableStateOf(false) }
    val state by store.state.collectAsState()
    if (restoring) { ProgressBackupScreen(state, store) { restoring = false }; return }
    Column(Modifier.fillMaxSize().background(c.bg).verticalScroll(rememberScrollState()).topSafePadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)) {
        Text(stringResource(R.string.native_storage_error), style = Type.titleSmall, color = c.ink)
        Text(stringResource(R.string.native_storage_hint), style = Type.body, color = c.ink2)
        BigButton(stringResource(R.string.native_retry), onClick = { store.retryStorage() })
        BigButton(stringResource(R.string.native_import), kind = ButtonKind.SOFT, onClick = { restoring = true })
    }
}

@Composable
fun ResumeSessionDialog(state: AppState, dispatch: (Intent) -> Unit) {
    if (state.pendingSession == null) return
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.native_resume_title)) },
        text = { Text(stringResource(R.string.native_resume_text)) },
        confirmButton = { TextButton({ dispatch(Intent.ResumePending) }) { Text(stringResource(R.string.native_resume)) } },
        dismissButton = { TextButton({ dispatch(Intent.AbandonSaved) }) { Text(stringResource(R.string.native_abandon)) } },
        containerColor = App.c.surface,
    )
}

@Composable
fun TrainingSuggestion(state: AppState, dispatch: (Intent) -> Unit) {
    val topic = weakestTopic(state.profile) ?: return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.native_train_hint), style = Type.small, color = App.c.ink2)
        BigButton(stringResource(R.string.native_train_topic, stringResource(topic.title)), kind = ButtonKind.SOFT,
            modifier = Modifier.fillMaxWidth()) { dispatch(Intent.StartTraining(topic)) }
    }
}

@Composable
fun HistoryScreen(state: AppState, dispatch: (Intent) -> Unit) {
    val c = App.c
    val format = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(LocalLocale.current.platformLocale)
    Column(Modifier.fillMaxSize().background(c.bg)) {
        TopBar(stringResource(R.string.native_history), { dispatch(Intent.Back) })
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.profile.history.isEmpty()) item { Text(stringResource(R.string.native_history_empty), style = Type.body, color = c.ink2) }
            items(state.profile.history, key = { it.id }) { entry ->
                AppCard(Modifier.fillMaxWidth().pressable { dispatch(Intent.OpenHistory(entry.id)) }) {
                    Text(modeTitle(entry.result.mode), style = Type.label, color = c.ink)
                    Text(format.format(Instant.ofEpochMilli(entry.finishedAt).atZone(ZoneId.systemDefault())), style = Type.tiny, color = c.ink2)
                    Text(stringResource(R.string.native_history_result, entry.result.correct, entry.result.total, signed(entry.result.delta)), style = Type.body, color = c.ink)
                }
            }
        }
    }
}

@Composable
fun ProgressBackupScreen(state: AppState, store: Store = viewModel(), onBack: () -> Unit = { store.dispatch(Intent.Back) }) {
    val c = App.c
    val resolver = LocalContext.current.contentResolver
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Int?>(null) }
    var preview by remember { mutableStateOf<BackupDocument?>(null) }
    BackHandler(busy) { }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val save = store.exportSnapshot()
            busy = true
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val text = ProgressCodec.encodeBackup(save, System.currentTimeMillis(), BuildConfig.VERSION_NAME)
                        require(text.toByteArray().size <= ProgressCodec.MAX_BYTES)
                        requireNotNull(resolver.openOutputStream(uri, "wt")).use { it.write(text.toByteArray(Charsets.UTF_8)) }
                    }
                    message = R.string.native_export_ok
                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (_: Exception) { message = R.string.native_transfer_error }
                finally { busy = false }
            }
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            busy = true
            scope.launch {
                try {
                    val text = withContext(Dispatchers.IO) {
                        requireNotNull(resolver.openInputStream(uri)).use { input ->
                            val bytes = ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            while (true) {
                                val n = input.read(buffer)
                                if (n < 0) break
                                require(bytes.size() + n <= ProgressCodec.MAX_BYTES)
                                bytes.write(buffer, 0, n)
                            }
                            Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                                .decode(ByteBuffer.wrap(bytes.toByteArray())).toString()
                        }
                    }
                    preview = store.previewBackup(text)
                    message = null
                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (_: IncompatibleBackup) { message = R.string.native_incompatible_backup }
                catch (_: Exception) { message = R.string.native_invalid_backup }
                finally { busy = false }
            }
        }
    }
    Column(Modifier.fillMaxSize().background(c.bg)) {
        TopBar(stringResource(R.string.native_backup), { if (!busy) onBack() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.native_backup_hint), style = Type.body, color = c.ink2)
            BigButton(stringResource(R.string.native_export), enabled = !busy && state.loaded && !state.storageError, modifier = Modifier.fillMaxWidth()) { export.launch("kvizkapital-${LocalDate.now()}.json") }
            BigButton(stringResource(R.string.native_import), enabled = !busy, kind = ButtonKind.SOFT, modifier = Modifier.fillMaxWidth()) {
                import.launch(arrayOf("application/json", "text/*", "application/octet-stream"))
            }
            if (busy) CircularProgressIndicator(color = c.pri)
            message?.let { Text(stringResource(it), style = Type.body, color = c.ink) }
            Text("v${BuildConfig.VERSION_NAME}", style = Type.tiny, color = c.ink2)
        }
    }
    preview?.let { doc ->
        val p = doc.save.profile
        val date = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(LocalLocale.current.platformLocale)
            .format(Instant.ofEpochMilli(doc.createdAt).atZone(ZoneId.systemDefault()))
        AlertDialog(
            onDismissRequest = { preview = null }, containerColor = c.surface,
            title = { Text(stringResource(R.string.native_preview_title)) },
            text = {
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.native_preview, p.name.ifBlank { stringResource(R.string.player) }, money(p.capital), p.totalStars, p.history.size, date, doc.appVersion))
                    if (doc.save.session != null) Text(stringResource(R.string.native_resume_title))
                    Text(stringResource(R.string.native_replace_text))
                }
            },
            confirmButton = { TextButton({
                busy = true
                preview = null
                store.dispatch(Intent.ImportProgress(doc.save.profile, doc.save.session, doc.save.language))
            }) { Text(stringResource(R.string.native_replace)) } },
            dismissButton = { TextButton({ preview = null }) { Text(stringResource(R.string.native_cancel)) } },
        )
    }
}
