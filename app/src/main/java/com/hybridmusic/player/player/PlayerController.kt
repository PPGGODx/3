package com.hybridmusic.player.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.webkit.WebView
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.hybridmusic.player.innertube.InnerTubeClient
import com.hybridmusic.player.innertube.TrackItem
import com.hybridmusic.player.service.PlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class PlaybackUiState(
    val currentTrack: TrackItem? = null,
    val queue: List<TrackItem> = emptyList(),
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val engineBadge: String = "Ad-Free YT Music Engine",
    val errorMessage: String? = null
)

class PlayerController(
    private val context: Context,
    private val innerTubeClient: InnerTubeClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var progressJob: Job? = null
    private var useEmbedFallback = false

    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

    private val embedEngine = AdFreeEmbedEngine(
        context = context,
        onProgressUpdate = { playing, pos, dur ->
            if (useEmbedFallback) {
                _uiState.value = _uiState.value.copy(
                    isPlaying = playing,
                    isBuffering = !playing && pos == 0L,
                    positionMs = pos.coerceAtLeast(0L),
                    durationMs = if (dur > 0L) dur else (_uiState.value.currentTrack?.durationMs ?: 0L),
                    errorMessage = null
                )
            }
        },
        onTrackEnded = { if (useEmbedFallback) playNextInQueue() },
        onEngineError = { msg ->
            if (useEmbedFallback) {
                _uiState.value = _uiState.value.copy(isBuffering = false, isPlaying = false, errorMessage = msg)
            }
        }
    )

    init {
        connectToService()
    }

    fun getEmbedWebView(): WebView? = embedEngine.getAttachableView()

    private fun connectToService() {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener({
            runCatching {
                val controller = future.get()
                mediaController = controller
                attachExoListener(controller)
                startExoProgressUpdater()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private suspend fun awaitMediaController(): MediaController? {
        mediaController?.let { return it }
        val future = controllerFuture ?: return null
        return suspendCancellableCoroutine { cont ->
            future.addListener({
                val ctrl = runCatching { future.get() }.getOrNull()
                if (ctrl != null && mediaController == null) {
                    mediaController = ctrl
                    attachExoListener(ctrl)
                    startExoProgressUpdater()
                }
                if (cont.isActive) cont.resume(ctrl)
            }, ContextCompat.getMainExecutor(context))
        }
    }

    private fun attachExoListener(player: Player) {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!useEmbedFallback) {
                    _uiState.value = _uiState.value.copy(
                        isPlaying = isPlaying,
                        errorMessage = if (isPlaying) null else _uiState.value.errorMessage
                    )
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (!useEmbedFallback) {
                    _uiState.value = _uiState.value.copy(
                        isBuffering = playbackState == Player.STATE_BUFFERING,
                        durationMs = player.duration.coerceAtLeast(_uiState.value.currentTrack?.durationMs ?: 0L)
                    )
                    if (playbackState == Player.STATE_ENDED) playNextInQueue()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val track = _uiState.value.currentTrack ?: return
                innerTubeClient.invalidateCache(track.videoId)
                switchToAdFreeEmbed(track)
            }
        })
    }

    private fun startExoProgressUpdater() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                if (!useEmbedFallback) {
                    mediaController?.let { player ->
                        if (player.isPlaying) {
                            _uiState.value = _uiState.value.copy(
                                positionMs = player.currentPosition.coerceAtLeast(0L),
                                durationMs = player.duration.coerceAtLeast(0L)
                            )
                        }
                    }
                }
                delay(400L)
            }
        }
    }

    fun playTrack(track: TrackItem, queue: List<TrackItem> = listOf(track)) {
        scope.launch {
            val normalizedQueue = if (queue.any { it.videoId == track.videoId }) queue else listOf(track) + queue
            _uiState.value = _uiState.value.copy(
                currentTrack = track,
                queue = normalizedQueue,
                isBuffering = true,
                positionMs = 0L,
                durationMs = track.durationMs,
                errorMessage = null
            )

            val stream = innerTubeClient.getCleanAudioStream(track.videoId)
            val controller = awaitMediaController()

            if (stream != null && controller != null) {
                useEmbedFallback = false
                embedEngine.stop()
                _uiState.value = _uiState.value.copy(engineBadge = "Direct Audio • ไม่มีโฆษณา 100%")

                val metadata = MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setArtworkUri(Uri.parse(track.thumbnailUrl))
                    .build()

                val mediaItem = MediaItem.Builder()
                    .setMediaId(track.videoId)
                    .setUri(Uri.parse(stream.url))
                    .setMediaMetadata(metadata)
                    .build()

                controller.setMediaItem(mediaItem)
                controller.prepare()
                controller.play()
            } else {
                switchToAdFreeEmbed(track)
            }
        }
    }

    private fun switchToAdFreeEmbed(track: TrackItem) {
        useEmbedFallback = true
        mediaController?.stop()
        _uiState.value = _uiState.value.copy(
            isBuffering = true,
            engineBadge = "Ad-Free Stream • บล็อกโฆษณาอัตโนมัติ",
            errorMessage = null
        )
        embedEngine.loadAndPlay(track.videoId)
    }

    fun togglePlayPause() {
        if (useEmbedFallback) {
            if (_uiState.value.isPlaying) {
                embedEngine.pause()
                _uiState.value = _uiState.value.copy(isPlaying = false)
            } else {
                embedEngine.play()
                _uiState.value = _uiState.value.copy(isPlaying = true)
            }
            return
        }
        mediaController?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
                _uiState.value.currentTrack?.let { playTrack(it, _uiState.value.queue) }
            } else {
                player.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        _uiState.value = _uiState.value.copy(positionMs = positionMs)
        if (useEmbedFallback) embedEngine.seekTo(positionMs) else mediaController?.seekTo(positionMs)
    }

    fun playNextInQueue() {
        val state = _uiState.value
        val current = state.currentTrack ?: return
        if (state.repeatMode == Player.REPEAT_MODE_ONE) {
            seekTo(0L)
            if (useEmbedFallback) embedEngine.play() else mediaController?.play()
            return
        }
        val list = state.queue
        if (list.isEmpty()) return
        val idx = list.indexOfFirst { it.videoId == current.videoId }
        val nextTrack = if (state.shuffleEnabled && list.size > 1) {
            list.filter { it.videoId != current.videoId }.random()
        } else if (idx in 0 until list.lastIndex) {
            list[idx + 1]
        } else if (state.repeatMode == Player.REPEAT_MODE_ALL) {
            list.first()
        } else return
        playTrack(nextTrack, list)
    }

    fun playPreviousInQueue() {
        val state = _uiState.value
        val current = state.currentTrack ?: return
        if (state.positionMs > 4000L) {
            seekTo(0L)
            return
        }
        val list = state.queue
        val idx = list.indexOfFirst { it.videoId == current.videoId }
        if (idx > 0) playTrack(list[idx - 1], list) else seekTo(0L)
    }

    fun toggleShuffle() {
        _uiState.value = _uiState.value.copy(shuffleEnabled = !_uiState.value.shuffleEnabled)
    }

    fun cycleRepeatMode() {
        val next = when (_uiState.value.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        _uiState.value = _uiState.value.copy(repeatMode = next)
    }
}
