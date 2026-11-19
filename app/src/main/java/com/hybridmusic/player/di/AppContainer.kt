package com.hybridmusic.player.di

import android.content.Context
import com.hybridmusic.player.data.local.AppDatabase
import com.hybridmusic.player.data.local.ZongStorageManager
import com.hybridmusic.player.data.remote.cloud.GoogleDriveSyncManager
import com.hybridmusic.player.data.remote.innertube.InnerTubeClient
import com.hybridmusic.player.data.repository.MusicRepository
import com.hybridmusic.player.player.PlayerController
import com.hybridmusic.player.util.NetworkMonitor
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    private val streamNetworkInterceptor = Interceptor { chain ->
        val original = chain.request()
        val urlStr = original.url.toString()
        if (urlStr.contains("googlevideo.com")) {
            val matchedUa = InnerTubeClient.resolveUserAgentForUrl(urlStr)
            val reqBuilder = original.newBuilder()
                .header("User-Agent", matchedUa)
                .header("Accept", "*/*")
                .header("Connection", "keep-alive")

            if (urlStr.contains("c=WEB", ignoreCase = true) ||
                urlStr.contains("c=TVHTML5", ignoreCase = true)
            ) {
                reqBuilder
                    .header("Origin", "https://www.youtube.com")
                    .header("Referer", "https://www.youtube.com/")
            }

            // แปลง Range แบบปลายเปิด (bytes=start-) เป็นช่วงละ 2MB เพื่อเลี่ยง HTTP 403 บน ExoPlayer
            val rangeHeader = original.header("Range")
            val chunkBytes = 2L * 1024L * 1024L
            if (rangeHeader == null) {
                reqBuilder.header("Range", "bytes=0-${chunkBytes - 1}")
            } else {
                val openRangeMatch = Regex("^bytes=(\\d+)-$").find(rangeHeader.trim())
                if (openRangeMatch != null) {
                    val start = openRangeMatch.groupValues[1].toLongOrNull() ?: 0L
                    reqBuilder.header("Range", "bytes=$start-${start + chunkBytes - 1}")
                }
            }

            chain.proceed(reqBuilder.build())
        } else {
            chain.proceed(original)
        }
    }

    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .addNetworkInterceptor(streamNetworkInterceptor)
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    val database: AppDatabase = AppDatabase.getInstance(context)
    val networkMonitor: NetworkMonitor = NetworkMonitor(context)
    val innerTubeClient: InnerTubeClient = InnerTubeClient(httpClient)
    val driveSyncManager: GoogleDriveSyncManager = GoogleDriveSyncManager(context)
    val zongStorageManager: ZongStorageManager = ZongStorageManager(context).apply {
        ensureZongFolderExists()
    }

    val musicRepository: MusicRepository = MusicRepository(
        context = context,
        musicDao = database.musicDao(),
        innerTubeClient = innerTubeClient,
        driveSyncManager = driveSyncManager,
        zongStorageManager = zongStorageManager,
        networkMonitor = networkMonitor,
        httpClient = httpClient
    )

    val playerController: PlayerController = PlayerController(
        context = context,
        repository = musicRepository
    )
}
