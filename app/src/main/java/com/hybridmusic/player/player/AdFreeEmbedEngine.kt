package com.hybridmusic.player.player

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream

class AdFreeEmbedEngine(
    context: Context,
    private val onProgressUpdate: (isPlaying: Boolean, positionMs: Long, durationMs: Long) -> Unit,
    private val onTrackEnded: () -> Unit,
    private val onEngineError: (String) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null
    private var currentVideoId: String? = null
    private var endedNotified = false

    private val blockedAdKeywords = listOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "adservice.google", "imasdk.googleapis.com", "/pagead/",
        "/get_midroll_info", "/api/stats/ads", "/ptracking",
        "youtube.com/api/stats/qoe?ad"
    )

    init {
        mainHandler.post { initWebView(context.applicationContext) }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView(appContext: Context) {
        val wv = WebView(appContext).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.userAgentString =
                "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Mobile Safari/537.36"

            addJavascriptInterface(BridgeInterface(), "ZongBridge")
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                    val url = request?.url?.toString()?.lowercase() ?: return null
                    if (blockedAdKeywords.any { url.contains(it) }) {
                        return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
                    }
                    return super.shouldInterceptRequest(view, request)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    injectZeroAdMonitor()
                }
            }
        }
        webView = wv
    }

    fun getAttachableView(): WebView? = webView

    fun loadAndPlay(videoId: String) {
        mainHandler.post {
            currentVideoId = videoId
            endedNotified = false
            val embedUrl =
                "https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&controls=0&disablekb=1&fs=0&modestbranding=1&playsinline=1&rel=0"
            webView?.loadUrl(embedUrl, mapOf("Referer" to "https://www.youtube-nocookie.com/"))
        }
    }

    fun play() {
        mainHandler.post {
            webView?.evaluateJavascript(
                "window.__zongUserPaused=false; var v=document.querySelector('video'); if(v){v.muted=false; v.play();}",
                null
            )
        }
    }

    fun pause() {
        mainHandler.post {
            webView?.evaluateJavascript(
                "window.__zongUserPaused=true; var v=document.querySelector('video'); if(v){v.pause();}",
                null
            )
        }
    }

    fun seekTo(positionMs: Long) {
        val sec = positionMs / 1000.0
        mainHandler.post {
            webView?.evaluateJavascript(
                "var v=document.querySelector('video'); if(v && isFinite(v.duration)){v.currentTime=$sec;}",
                null
            )
        }
    }

    fun stop() {
        mainHandler.post {
            currentVideoId = null
            webView?.loadUrl("about:blank")
        }
    }

    private fun injectZeroAdMonitor() {
        val js = """
            (function() {
                if (window.__zongAdShieldInstalled) return;
                window.__zongAdShieldInstalled = true;
                setInterval(function() {
                    try {
                        var player = document.querySelector('.html5-video-player');
                        var video = document.querySelector('video');
                        if (!video) return;

                        document.querySelectorAll('.ytp-ad-overlay-container, .ytp-ad-module, .ytp-ad-player-overlay')
                            .forEach(function(el) { el.remove(); });

                        var isAd = player && (player.classList.contains('ad-showing') || player.classList.contains('ad-interrupting'));
                        if (isAd) {
                            video.muted = true;
                            video.playbackRate = 16.0;
                            if (isFinite(video.duration) && video.duration > 0) video.currentTime = video.duration;
                            var skipBtn = document.querySelector('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button');
                            if (skipBtn) skipBtn.click();
                            return;
                        }

                        if (video.muted) video.muted = false;
                        if (video.playbackRate !== 1.0) video.playbackRate = 1.0;
                        if (video.paused && !video.ended && video.readyState >= 2 && !window.__zongUserPaused) {
                            video.play().catch(function(){});
                        }

                        var posMs = Math.floor((video.currentTime || 0) * 1000);
                        var durMs = isFinite(video.duration) ? Math.floor(video.duration * 1000) : 0;
                        var playing = !video.paused && !video.ended && video.readyState > 2;
                        if (window.ZongBridge) window.ZongBridge.onTick(playing, posMs, durMs, video.ended === true);
                    } catch (e) {}
                }, 250);
            })();
        """.trimIndent()
        webView?.evaluateJavascript(js, null)
    }

    private inner class BridgeInterface {
        @JavascriptInterface
        fun onTick(isPlaying: Boolean, positionMs: Long, durationMs: Long, ended: Boolean) {
            mainHandler.post {
                if (currentVideoId == null) return@post
                onProgressUpdate(isPlaying, positionMs, durationMs)
                if (ended && !endedNotified) {
                    endedNotified = true
                    onTrackEnded()
                }
            }
        }
    }
}
