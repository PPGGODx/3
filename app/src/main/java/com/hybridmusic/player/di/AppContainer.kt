package com.hybridmusic.player.di

import android.content.Context
import com.hybridmusic.player.data.LibraryRepository
import com.hybridmusic.player.innertube.InnerTubeClient
import com.hybridmusic.player.player.PlayerController
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
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
                .header("Accept-Encoding", "identity")
                .header("Connection", "keep-alive")
                .build()
            chain.proceed(req)
        } else {
            chain.proceed(original)
        }
    }

    private val okDispatcher = Dispatcher().apply {
        maxRequests = 64
        maxRequestsPerHost = 16
    }

    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .dispatcher(okDispatcher)
        .connectionPool(ConnectionPool(16, 5, TimeUnit.MINUTES))
        .addNetworkInterceptor(streamNetworkInterceptor)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    val innerTubeClient: InnerTubeClient = InnerTubeClient(httpClient)
    val libraryRepository: LibraryRepository = LibraryRepository(context.applicationContext)
    val playerController: PlayerController = PlayerController(
        context = context,
        innerTubeClient = innerTubeClient
    )
}
