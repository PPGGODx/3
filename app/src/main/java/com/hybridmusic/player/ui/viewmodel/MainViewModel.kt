package com.hybridmusic.player.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hybridmusic.player.innertube.InnerTubeClient
import com.hybridmusic.player.innertube.TrackItem
import com.hybridmusic.player.innertube.YtMusicLinkParser
import com.hybridmusic.player.player.PlaybackUiState
import com.hybridmusic.player.player.PlayerController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val innerTubeClient: InnerTubeClient,
    val playerController: PlayerController
) : ViewModel() {

    val playbackState: StateFlow<PlaybackUiState> = playerController.uiState

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _trackList = MutableStateFlow<List<TrackItem>>(emptyList())
    val trackList: StateFlow<List<TrackItem>> = _trackList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun onInputChange(newText: String) {
        _inputText.value = newText
    }

    fun submitLinkOrSearch(overrideInput: String? = null) {
        val raw = (overrideInput ?: _inputText.value).trim()
        if (raw.isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = null

            val videoId = YtMusicLinkParser.extractVideoId(raw)
            if (videoId != null) {
                _statusMessage.value = "กำลังดึงข้อมูลเพลงจากลิงก์ YT Music..."
                val track = innerTubeClient.getTrackByVideoId(videoId)
                val updated = listOf(track) + _trackList.value.filter { it.videoId != videoId }
                _trackList.value = updated
                _isLoading.value = false
                _statusMessage.value = "กำลังเล่น: ${track.title} (ไม่มีโฆษณา)"
                playerController.playTrack(track, updated)
            } else if (YtMusicLinkParser.isUrlInput(raw)) {
                _isLoading.value = false
                _statusMessage.value = "ไม่พบรหัสเพลงในลิงก์นี้ กรุณาวางลิงก์เพลงจาก YT Music (watch?v=...)"
            } else {
                val results = innerTubeClient.searchSongs(raw)
                _trackList.value = results
                _isLoading.value = false
                if (results.isEmpty()) {
                    _statusMessage.value = "ไม่พบเพลงที่ค้นหา ลองวางลิงก์เพลง YT Music โดยตรง"
                }
            }
        }
    }

    fun playTrack(track: TrackItem) {
        val queue = _trackList.value.ifEmpty { listOf(track) }
        playerController.playTrack(track, queue)
    }

    class Factory(
        private val innerTubeClient: InnerTubeClient,
        private val playerController: PlayerController
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(innerTubeClient, playerController) as T
        }
    }
}
