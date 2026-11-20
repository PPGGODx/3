package com.hybridmusic.player.di

import android.content.Context
import com.hybridmusic.player.innertube.InnerTubeClient
import com.hybridmusic.player.player.PlayerController
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    private val streamNetworkInterceptor = Interceptor { chain ->
        val original = chain.request()
        val urlStr = original.url.toString()
        if (urlStr.contains("googlevideo.com")) {
            val matchedUa = InnerTubeClient.resolveUserAgentForUrl(urlStr)
            val req = original.newBuilder()
                .header("User-Agent", matchedUa)
                .header("Accept", "*/*")
                .header("Connection", "keep-alive")
                .build()
            chain.proceed(req)
        } else {
            chain.proceed(original)
        }
    }

    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .addNetworkInterceptor(streamNetworkInterceptor)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    val innerTubeClient: InnerTubeClient = InnerTubeClient(httpClient)
    val playerController: PlayerController = PlayerController(
        context = context,
        innerTubeClient = innerTubeClient
    )
}
