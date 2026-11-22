package com.hybridmusic.player.player

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.hybridmusic.player.HybridMusicApp
import com.hybridmusic.player.MainActivity
import com.hybridmusic.player.offline.OfflineAudioRepository
import java.io.File

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        val app = application as HybridMusicApp
        val controller = app.container.playerController
        val basePlayer = controller.getExoPlayer()

        val forwardingPlayer = object : ForwardingPlayer(basePlayer) {
            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .addAll(
                        Player.COMMAND_SEEK_TO_NEXT,
                        Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                        Player.COMMAND_SEEK_TO_PREVIOUS,
                        Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
                    )
                    .build()
            }

            override fun isCommandAvailable(command: Int): Boolean {
                return when (command) {
                    Player.COMMAND_SEEK_TO_NEXT,
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                    Player.COMMAND_SEEK_TO_PREVIOUS,
                    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> true
                    else -> super.isCommandAvailable(command)
                }
            }

            override fun hasNextMediaItem(): Boolean = true

            override fun hasPreviousMediaItem(): Boolean = true

            override fun seekToNext() {
                controller.playNextInQueue()
            }

            override fun seekToNextMediaItem() {
                controller.playNextInQueue()
            }

            override fun seekToPrevious() {
                if (currentPosition > 3500L) {
                    seekTo(0L)
                } else {
                    controller.playPreviousInQueue()
                }
            }

            override fun seekToPreviousMediaItem() {
                controller.playPreviousInQueue()
            }

            override fun getMediaMetadata(): MediaMetadata {
                val existing = super.getMediaMetadata()
                if (!existing.title.isNullOrBlank()) return existing
                val track = controller.uiState.value.currentTrack ?: return existing
                val localCover = OfflineAudioRepository.getInstance(this@PlaybackService)
                    .getLocalCoverFile(track.videoId)
                val artUri = if (localCover != null && File(localCover.absolutePath).exists()) {
                    Uri.fromFile(localCover)
                } else if (track.thumbnailUrl.isNotBlank()) {
                    Uri.parse(track.thumbnailUrl)
                } else null

                return existing.buildUpon()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .apply { if (artUri != null) setArtworkUri(artUri) }
                    .build()
            }
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, forwardingPlayer)
            .setSessionActivity(sessionActivity)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}
