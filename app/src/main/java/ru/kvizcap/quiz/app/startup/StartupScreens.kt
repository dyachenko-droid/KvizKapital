package ru.kvizcap.quiz.app.startup

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import ru.kvizcap.quiz.app.R
import ru.kvizcap.quiz.app.ui.components.BigButton
import ru.kvizcap.quiz.app.ui.components.Coin
import ru.kvizcap.quiz.app.ui.theme.App
import ru.kvizcap.quiz.app.ui.theme.Type
import ru.kvizcap.quiz.app.util.RotatingScreen

@Composable
fun StartupGate(route: LaunchRoute, onAccept: () -> Unit, nativeContent: @Composable () -> Unit) {
    if (route == LaunchRoute.Native) {
        nativeContent()
        return
    }
    RotatingScreen { isLandscape ->
        when (route) {
            LaunchRoute.Loading -> LoadingScreen(isLandscape)
            is LaunchRoute.Web -> PrivacyWebScreen(route, isLandscape, onAccept)
            LaunchRoute.Native -> Unit
        }
    }
}

@Composable
private fun LoadingScreen(isLandscape: Boolean) {
    val colors = App.c
    val transition = rememberInfiniteTransition(label = "startup")
    val scale by transition.animateFloat(
        0.94f, 1.05f,
        infiniteRepeatable(tween(850, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "coin",
    )
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(colors.bg, colors.priSoft)))
            .windowInsetsPadding(WindowInsets.displayCutout),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = if (isLandscape) 16.dp else 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Coin(if (isLandscape) 64.dp else 100.dp, Modifier.graphicsLayer { scaleX = scale; scaleY = scale })
            Spacer(Modifier.height(if (isLandscape) 14.dp else 28.dp))
            Text(stringResource(R.string.app_name), style = if (isLandscape) Type.titleSmall else Type.title, color = colors.ink, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.startup_loading), style = Type.body, color = colors.ink2, textAlign = TextAlign.Center)
            Spacer(Modifier.height(if (isLandscape) 16.dp else 32.dp))
            CircularProgressIndicator(Modifier.size(28.dp), color = colors.pri, strokeWidth = 3.dp)
        }
    }
}

private class FileChooserState {
    var callback: ValueCallback<Array<Uri>>? = null
    fun finish(value: Array<Uri>?) {
        val pending = callback
        callback = null
        pending?.onReceiveValue(value)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun PrivacyWebScreen(route: LaunchRoute.Web, isLandscape: Boolean, onAccept: () -> Unit) {
    val context = LocalContext.current
    val colors = App.c
    val urlLabel = stringResource(R.string.startup_webview_url)
    val chooser = remember { FileChooserState() }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var loading by remember(route.url) { mutableStateOf(true) }
    var failed by remember(route.url) { mutableStateOf(false) }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val data = it.data
        val selected = if (it.resultCode != Activity.RESULT_OK) null else {
            data?.clipData?.let { clip -> Array(clip.itemCount) { index -> clip.getItemAt(index).uri } }
                ?: data?.data?.let { uri -> arrayOf(uri) }
        }
        chooser.finish(selected)
    }
    DisposableEffect(Unit) {
        onDispose { chooser.finish(null) }
    }
    BackHandler {
        val view = webView
        if (view?.canGoBack() == true) view.goBack()
    }
    Box(Modifier.fillMaxSize().background(colors.bg).windowInsetsPadding(WindowInsets.displayCutout)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                WebView(viewContext).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                            loading = true
                            failed = false
                        }
                        override fun onPageFinished(view: WebView, url: String?) {
                            loading = false
                            CookieManager.getInstance().flush()
                        }
                        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                            if (request.isForMainFrame) {
                                failed = true; loading = false
                            }
                        }
                        override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                            if (request.isForMainFrame && response.statusCode >= 400) {
                                failed = true; loading = false
                            }
                        }
                        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                            super.onReceivedSslError(view, handler, error)
                        }
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val uri = request.url
                            if (uri.scheme in listOf("http", "https")) return false
                            if (uri.scheme in listOf("mailto", "tel", "sms", "geo", "market")) {
                                try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                                catch (_: ActivityNotFoundException) { /* No compatible app installed. */ }
                            }
                            return true
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onShowFileChooser(view: WebView, callback: ValueCallback<Array<Uri>>, params: FileChooserParams): Boolean {
                            chooser.finish(null)
                            chooser.callback = callback
                            val multiple = params.mode == FileChooserParams.MODE_OPEN_MULTIPLE
                            try {
                                filePicker.launch(params.createIntent().apply {
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multiple)
                                })
                            } catch (_: ActivityNotFoundException) {
                                try {
                                    filePicker.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        type = "*/*"
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multiple)
                                    })
                                } catch (_: ActivityNotFoundException) { chooser.finish(null) }
                            }
                            return true
                        }
                    }
                    webView = this
                    loadUrl(route.url)
                }
            },
            onRelease = { view ->
                chooser.finish(null)
                view.stopLoading()
                view.webChromeClient = null
                view.webViewClient = WebViewClient()
                view.destroy()
                webView = null
            },
        )
        if (loading && !failed) {
            CircularProgressIndicator(
                Modifier.align(Alignment.TopCenter).padding(top = if (isLandscape) 10.dp else 24.dp).size(28.dp),
                color = colors.pri, strokeWidth = 3.dp,
            )
        }
        if (failed) {
            Column(
                Modifier.fillMaxSize().background(colors.bg).verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = if (isLandscape) 24.dp else 48.dp)
                    .padding(bottom = if (route.requiresAcceptance) 88.dp else 0.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(stringResource(R.string.startup_page_error), style = Type.titleSmall, color = colors.ink, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.startup_page_retry_hint), style = Type.body, color = colors.ink2, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                BigButton(stringResource(R.string.startup_retry)) {
                    failed = false
                    loading = true
                    webView?.reload()
                }
            }
        }
        if (route.requiresAcceptance) {
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(colors.bg.copy(alpha = 0f), colors.bg)))
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = if (isLandscape) 12.dp else 24.dp),
            ) {
                BigButton(stringResource(R.string.startup_accept), Modifier.fillMaxWidth(), onClick = onAccept)
            }
        }
        Column(
            Modifier.align(Alignment.TopCenter).fillMaxWidth().background(colors.bg)
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            OutlinedTextField(
                value = route.url,
                onValueChange = {},
                readOnly = true,
                label = { Text(urlLabel) },
                maxLines = if (isLandscape) 2 else 3,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(
                modifier = Modifier.align(Alignment.End),
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText(urlLabel, route.url))
                    Toast.makeText(context, R.string.startup_url_copied, Toast.LENGTH_SHORT).show()
                },
            ) { Text(stringResource(R.string.startup_copy_url)) }
        }
    }
}
