package com.hairacademy.app.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.hairacademy.app.data.Video
import com.hairacademy.app.data.formatDuration

/** Источник страницы для YouTube-плеера (YouTube требует Referer/origin, иначе ошибка 153). Можно заменить на свой домен. */
private const val EMBED_ORIGIN = "https://com.alex.hairacademy"

fun openExternally(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
    }
}

@Composable
fun PlayerScreen(
    video: Video?,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
    onShowFullscreen: (View, WebChromeClient.CustomViewCallback) -> Unit,
    onHideFullscreen: () -> Unit,
) {
    if (video == null) {
        AppScaffold("Урок", onBack) { pad ->
            EmptyState("🤷", "Урок не найден", "Возможно, он был удалён из списка.", Modifier.padding(pad))
        }
        return
    }
    val context = LocalContext.current
    var playerFailed by remember(video.key) { mutableStateOf(false) }
    var errorCode by remember(video.key) { mutableStateOf(0) }

    AppScaffold("Урок", onBack) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState())) {
            if (video.isYoutube && !playerFailed) {
                YouTubePlayer(
                    videoId = video.id,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                    onPlayerError = { code -> errorCode = code; playerFailed = true },
                    onShowFullscreen = onShowFullscreen,
                    onHideFullscreen = onHideFullscreen,
                )
            } else {
                // Запасной вариант: превью + переход по ссылке
                Box(
                    Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black)
                        .clickable { openExternally(context, video.watchUrl) }
                ) {
                    if (video.thumbnailUrl.isNotEmpty()) {
                        AsyncImage(video.thumbnailUrl, video.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                    Box(
                        Modifier.align(Alignment.Center).size(64.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(40.dp)) }
                }
                if (playerFailed) {
                    Text(
                        "Это видео нельзя воспроизвести внутри приложения (код $errorCode) — откройте его по ссылке.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 0.dp),
                    )
                }
            }

            Column(Modifier.padding(16.dp)) {
                Text(video.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    listOf(video.channel, video.language.uppercase(), formatDuration(video.durationMin))
                        .filter { it.isNotEmpty() }.joinToString("  ·  "),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${video.section.title} › ${video.sub.title}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(20.dp))
                Button(onClick = onToggleFavorite, modifier = Modifier.fillMaxWidth()) {
                    Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isFavorite) "В избранном" else "Добавить в избранное")
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { openExternally(context, video.watchUrl) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Открыть по ссылке")
                }
                if (video.isYoutube && !playerFailed) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "На весь экран — кнопка в правом нижнем углу плеера.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Плеер YouTube во встроенном WebView (IFrame Player API).
 * Полноэкранный режим — через WebChromeClient.onShowCustomView, его показывает MainActivity.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubePlayer(
    videoId: String,
    modifier: Modifier,
    onPlayerError: (Int) -> Unit,
    onShowFullscreen: (View, WebChromeClient.CustomViewCallback) -> Unit,
    onHideFullscreen: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentError by rememberUpdatedState(onPlayerError)
    val currentShow by rememberUpdatedState(onShowFullscreen)
    val currentHide by rememberUpdatedState(onHideFullscreen)

    val webView = remember(videoId) {
        WebView(context).apply {
            val self = this
            setBackgroundColor(android.graphics.Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            // Плеер YouTube живёт во вложенном iframe: без сторонних cookie WebView он остаётся чёрным
            android.webkit.CookieManager.getInstance().apply {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(self, true)
            }
            addJavascriptInterface(object {
                @JavascriptInterface
                fun onPlayerFailed(code: Int) {
                    self.post { currentError(code) }
                }
            }, "Android")
            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                    currentShow(view, callback)
                }

                override fun onHideCustomView() {
                    currentHide()
                }
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    // Любой переход «наружу» (например, по логотипу YouTube) открываем в браузере
                    val host = request.url.host.orEmpty()
                    val own = host.endsWith("youtube.com") || host.endsWith("google.com") || host.endsWith("gstatic.com") || host.endsWith("googlevideo.com") || host.endsWith("youtube-nocookie.com")
                    if (request.isForMainFrame && !own) {
                        openExternally(view.context, request.url.toString())
                        return true
                    }
                    return false
                }
            }
            loadDataWithBaseURL("$EMBED_ORIGIN/", playerHtml(videoId), "text/html", "utf-8", null)
        }
    }

    DisposableEffect(lifecycleOwner, webView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> webView.onPause()
                Lifecycle.Event.ON_RESUME -> webView.onResume()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            currentHide()
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.destroy()
        }
    }

    AndroidView(factory = { webView }, modifier = modifier)
}

private fun playerHtml(rawId: String): String {
    val id = rawId.filter { it.isLetterOrDigit() || it == '_' || it == '-' }
    // Тот же iframe, что в рабочем приложении 0.8
    return """
<!DOCTYPE html><html><head>
<meta name="viewport" content="width=device-width, initial-scale=1">
<style>html,body{margin:0;height:100%;background:#000;overflow:hidden}iframe{position:absolute;top:0;left:0;width:100%;height:100%;border:0}</style>
</head><body>
<iframe src="https://www.youtube.com/embed/$id?playsinline=1&fs=1&enablejsapi=1&origin=https%3A%2F%2Fcom.alex.hairacademy&hl=ru"
 allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; fullscreen"
 referrerpolicy="strict-origin-when-cross-origin" allowfullscreen></iframe>
</body></html>
""".trimIndent()
}
