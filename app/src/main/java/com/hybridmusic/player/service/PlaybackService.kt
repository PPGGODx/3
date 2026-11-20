package com.hybridmusic.player.service

import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.hybridmusic.player.HybridMusicApp
import com.hybridmusic.player.innertube.InnerTubeClient
import com.hybridmusic.player.player.YoutubeChunkedDataSource

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        val app = applicationContext as HybridMusicApp
        val okHttpFactory = OkHttpDataSource.Factory(app.container.httpClient)
            .setUserAgent(InnerTubeClient.VR_USER_AGENT)
        val chunkedYoutubeFactory = YoutubeChunkedDataSource.Factory(okHttpFactory)
        val dataSourceFactory = DefaultDataSource.Factory(this, chunkedYoutubeFactory)

        // ลดบัฟเฟอร์เริ่มต้นเหลือ 250ms และใช้ 256 KB Chunked DataSource เพื่อเริ่มเล่นทันที
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                12_000,
                45_000,
                250,
                600
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
