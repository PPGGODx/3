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
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
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
    private var playJob: Job? = null
    private var prefetchJob: Job? = null
    private var useEmbedFallback = false
    private var retriedWithIosVideoId: String? = null

    private val trackLookup = ConcurrentHashMap<String, TrackItem>()

    var onAutoTrackChanged: ((TrackItem) -> Unit)? = null

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

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (useEmbedFallback || mediaItem == null) return
                val newTrack = trackLookup[mediaItem.mediaId] ?: return
                val prevId = _uiState.value.currentTrack?.videoId
                if (newTrack.videoId != prevId) {
                    _uiState.value = _uiState.value.copy(
                        currentTrack = newTrack,
                        positionMs = 0L,
                        durationMs = newTrack.durationMs,
                        errorMessage = null
                    )
                    onAutoTrackChanged?.invoke(newTrack)
                    scheduleNextTracksPrefetch()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val track = _uiState.value.currentTrack ?: return
                innerTubeClient.invalidateCache(track.videoId)
                if (retriedWithIosVideoId != track.videoId) {
                    retriedWithIosVideoId = track.videoId
                    playJob?.cancel()
                    playJob = scope.launch {
                        val iosStream = innerTubeClient.getCleanAudioStream(
                            videoId = track.videoId,
                            forceRefresh = true,
                            preferIos = true
                        )
                        val ctrl = awaitMediaController()
                        if (iosStream != null && ctrl != null) {
                            startExoPlayback(ctrl, track, iosStream.url)
                        } else {
                            switchToAdFreeEmbed(track)
                        }
                    }
                } else {
                    switchToAdFreeEmbed(track)
                }
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
                delay(350L)
            }
        }
    }

    private fun buildMediaItem(track: TrackItem, streamUrl: String): MediaItem {
        trackLookup[track.videoId] = track
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setArtworkUri(Uri.parse(track.thumbnailUrl))
            .build()

        return MediaItem.Builder()
            .setMediaId(track.videoId)
            .setUri(Uri.parse(streamUrl))
            .setMediaMetadata(metadata)
            .build()
    }

    private fun startExoPlayback(controller: MediaController, track: TrackItem, streamUrl: String) {
        useEmbedFallback = false
        embedEngine.stop()
        _uiState.value = _uiState.value.copy(engineBadge = "Direct Audio • ไม่มีโฆษณา 100%")
        controller.setMediaItem(buildMediaItem(track, streamUrl))
        controller.prepare()
        controller.play()
        scheduleNextTracksPrefetch()
    }

    // อัปเดตคิววิทยุเบื้องหลังโดยไม่ขัดจังหวะเพลงที่กำลังเล่นอยู่
    fun updateQueue(newQueue: List<TrackItem>) {
        if (newQueue.isEmpty()) return
        newQueue.forEach { trackLookup[it.videoId] = it }
        _uiState.value = _uiState.value.copy(queue = newQueue)
        scheduleNextTracksPrefetch()
    }

    fun playTrack(track: TrackItem, queue: List<TrackItem> = listOf(track)) {
        val normalizedQueue = if (queue.any { it.videoId == track.videoId }) queue else listOf(track) + queue
        normalizedQueue.forEach { trackLookup[it.videoId] = it }
        retriedWithIosVideoId = null

        _uiState.value = _uiState.value.copy(
            currentTrack = track,
            queue = normalizedQueue,
            isBuffering = true,
            positionMs = 0L,
            durationMs = track.durationMs,
            errorMessage = null
        )

        playJob?.cancel()

        // Fast-Path: หากเพลงนี้ถูก Pre-fetch ไว้แล้ว ให้สั่ง ExoPlayer เล่นทันทีใน 0ms!
        val cachedStream = innerTubeClient.getCachedStream(track.videoId)
        val readyController = mediaController
        if (cachedStream != null && readyController != null) {
            startExoPlayback(readyController, track, cachedStream.url)
            return
        }

        playJob = scope.launch {
            val stream = innerTubeClient.getCleanAudioStream(track.videoId)
            val controller = awaitMediaController()
            if (_uiState.value.currentTrack?.videoId != track.videoId) return@launch

            if (stream != null && controller != null) {
                startExoPlayback(controller, track, stream.url)
            } else {
                switchToAdFreeEmbed(track)
            }
        }
    }

    private fun computeNextTrack(state: PlaybackUiState = _uiState.value): TrackItem? {
        val current = state.currentTrack ?: return null
        val list = state.queue
        if (list.isEmpty()) return null
        val idx = list.indexOfFirst { it.videoId == current.videoId }
        return if (state.shuffleEnabled && list.size > 1) {
            list.filter { it.videoId != current.videoId }.random()
        } else if (idx in 0 until list.lastIndex) {
            list[idx + 1]
        } else if (state.repeatMode == Player.REPEAT_MODE_ALL) {
            list.first()
        } else {
            null
        }
    }

    // ดึงสตรีมเพลงถัดไป 2 เพลงล่วงหน้า + ใส่เพลงถัดไปเข้า ExoPlayer Timeline เพื่อบัฟเฟอร์รอแบบไร้รอยต่อ
    private fun scheduleNextTracksPrefetch() {
        prefetchJob?.cancel()
        val snapshot = _uiState.value
        val next1 = computeNextTrack(snapshot) ?: return
        val next1Idx = snapshot.queue.indexOfFirst { it.videoId == next1.videoId }
        val next2 = if (next1Idx in 0 until snapshot.queue.lastIndex) snapshot.queue[next1Idx + 1] else null

        prefetchJob = scope.launch(Dispatchers.IO) {
            val stream1 = innerTubeClient.getCleanAudioStream(next1.videoId)
            if (stream1 != null) {
                withContext(Dispatchers.Main) {
                    val ctrl = mediaController
                    if (!useEmbedFallback && ctrl != null && computeNextTrack()?.videoId == next1.videoId) {
                        val curIdx = ctrl.currentMediaItemIndex
                        val hasExpectedNext = ctrl.mediaItemCount > curIdx + 1 &&
                            ctrl.getMediaItemAt(curIdx + 1).mediaId == next1.videoId
                        if (!hasExpectedNext) {
                            if (ctrl.mediaItemCount > curIdx + 1) {
                                ctrl.removeMediaItems(curIdx + 1, ctrl.mediaItemCount)
                            }
                            ctrl.addMediaItem(buildMediaItem(next1, stream1.url))
                        }
                    }
                }
            }
            if (next2 != null && next2.videoId != next1.videoId) {
                innerTubeClient.getCleanAudioStream(next2.videoId)
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
        if (state.currentTrack == null) return
        if (state.repeatMode == Player.REPEAT_MODE_ONE) {
            seekTo(0L)
            if (useEmbedFallback) embedEngine.play() else mediaController?.play()
            return
        }

        val nextTrack = computeNextTrack(state) ?: return
        val ctrl = mediaController

        // หาก ExoPlayer บัฟเฟอร์เพลงถัดไปรอไว้แล้ว ให้ข้ามไปเล่นทันทีแบบไร้รอยต่อ!
        if (!useEmbedFallback && ctrl != null && ctrl.hasNextMediaItem()) {
            val nextItem = ctrl.getMediaItemAt(ctrl.nextMediaItemIndex)
            if (nextItem.mediaId == nextTrack.videoId) {
                _uiState.value = state.copy(
                    currentTrack = nextTrack,
                    positionMs = 0L,
                    durationMs = nextTrack.durationMs,
                    errorMessage = null
                )
                ctrl.seekToNextMediaItem()
                ctrl.play()
                scheduleNextTracksPrefetch()
                return
            }
        }

        playTrack(nextTrack, state.queue)
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
        scheduleNextTracksPrefetch()
    }

    fun cycleRepeatMode() {
        val next = when (_uiState.value.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        _uiState.value = _uiState.value.copy(repeatMode = next)
        scheduleNextTracksPrefetch()
    }
}
