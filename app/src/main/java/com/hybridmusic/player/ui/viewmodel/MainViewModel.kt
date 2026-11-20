package com.hybridmusic.player.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hybridmusic.player.innertube.HomeFeedResult
import com.hybridmusic.player.innertube.InnerTubeClient
import com.hybridmusic.player.innertube.MoodChip
import com.hybridmusic.player.innertube.TrackItem
import com.hybridmusic.player.innertube.YtMusicLinkParser
import com.hybridmusic.player.innertube.YtPlaylistCard
import com.hybridmusic.player.player.PlaybackUiState
import com.hybridmusic.player.player.PlayerController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BrowseCategory(
    val title: String,
    val query: String,
    val colorHex: Long
)

class MainViewModel(
    private val innerTubeClient: InnerTubeClient,
    val playerController: PlayerController
) : ViewModel() {

    val playbackState: StateFlow<PlaybackUiState> = playerController.uiState

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _homeFeed = MutableStateFlow(
        HomeFeedResult(
            moods = listOf(MoodChip("ทั้งหมด", "")),
            quickPicks = emptyList(),
            shelves = emptyList()
        )
    )
    val homeFeed: StateFlow<HomeFeedResult> = _homeFeed.asStateFlow()

    private val _selectedMoodIndex = MutableStateFlow(0)
    val selectedMoodIndex: StateFlow<Int> = _selectedMoodIndex.asStateFlow()

    private val _isLoadingHome = MutableStateFlow(true)
    val isLoadingHome: StateFlow<Boolean> = _isLoadingHome.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<TrackItem>>(emptyList())
    val searchResults: StateFlow<List<TrackItem>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _recentTracks = MutableStateFlow<List<TrackItem>>(emptyList())
    val recentTracks: StateFlow<List<TrackItem>> = _recentTracks.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _isFullPlayerOpen = MutableStateFlow(false)
    val isFullPlayerOpen: StateFlow<Boolean> = _isFullPlayerOpen.asStateFlow()

    val browseCategories = listOf(
        BrowseCategory("เพลงไทยยอดนิยม", "เพลงไทยยอดนิยม 2025 2026", 0xFFE13300),
        BrowseCategory("Pop ฮิตติดชาร์ต", "Top Pop Hits Official", 0xFF1E3264),
        BrowseCategory("Hip-Hop / R&B", "Thai Hip Hop R&B Hits", 0xFFBA5D07),
        BrowseCategory("Rock / Alternative", "Thai Rock Band Hits", 0xFF8D67AB),
        BrowseCategory("K-Pop มาแรง", "K-Pop Top Hits", 0xFFE8115B),
        BrowseCategory("Lo-Fi & ชิลล์", "Chill Lo-Fi Beats", 0xFF148A08),
        BrowseCategory("เพลงรักซึ้งๆ", "เพลงรักซึ้งๆ ยอดนิยม", 0xFF503750),
        BrowseCategory("ลูกทุ่งอินดี้", "ลูกทุ่งอินดี้มาแรง", 0xFF0D73EC)
    )

    init {
        loadHomeFeed(0)
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun openFullPlayer(open: Boolean) {
        _isFullPlayerOpen.value = open
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun loadHomeFeed(moodIndex: Int = 0) {
        viewModelScope.launch {
            _selectedMoodIndex.value = moodIndex
            _isLoadingHome.value = true
            val mood = _homeFeed.value.moods.getOrNull(moodIndex)
            val feed = innerTubeClient.fetchHomeFeed(if (moodIndex == 0) null else mood)
            _homeFeed.value = feed
            _isLoadingHome.value = false
        }
    }

    fun playTrackOneClick(track: TrackItem, queue: List<TrackItem> = listOf(track)) {
        val updatedRecent = listOf(track) + _recentTracks.value.filter { it.videoId != track.videoId }
        _recentTracks.value = updatedRecent.take(30)
        val normalizedQueue = if (queue.isEmpty()) listOf(track) else queue
        playerController.playTrack(track, normalizedQueue)
    }

    fun playPlaylistOneClick(playlist: YtPlaylistCard) {
        viewModelScope.launch {
            _statusBanner.value = "กำลังเปิดเพลย์ลิสต์ \"${playlist.title}\"..."
            val tracks = innerTubeClient.fetchPlaylistTracks(playlist.browseId)
                .ifEmpty { innerTubeClient.searchSongs(playlist.title) }
            if (tracks.isNotEmpty()) {
                _statusBanner.value = null
                playTrackOneClick(tracks.first(), tracks)
            } else {
                _statusBanner.value = "ไม่พบเพลงในเพลย์ลิสต์นี้"
            }
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
        if (newQuery.isBlank()) {
            _searchResults.value = emptyList()
        }
    }

    fun submitSearchOrLink(overrideInput: String? = null) {
        val raw = (overrideInput ?: _searchQuery.value).trim()
        if (raw.isEmpty()) return

        viewModelScope.launch {
            _isSearching.value = true
            _statusBanner.value = null

            val videoId = YtMusicLinkParser.extractVideoId(raw)
            if (videoId != null) {
                val track = innerTubeClient.getTrackByVideoId(videoId)
                val updated = listOf(track) + _searchResults.value.filter { it.videoId != videoId }
                _searchResults.value = updated
                _isSearching.value = false
                playTrackOneClick(track, updated)
            } else if (YtMusicLinkParser.isUrlInput(raw)) {
                _isSearching.value = false
                _statusBanner.value = "ไม่พบรหัสเพลงในลิงก์นี้ กรุณาวางลิงก์เพลงจาก YT Music"
            } else {
                val results = innerTubeClient.searchSongs(raw)
                _searchResults.value = results
                _isSearching.value = false
                if (results.isEmpty()) {
                    _statusBanner.value = "ไม่พบเพลงที่ค้นหา ลองคำค้นหาอื่น"
                }
            }
        }
    }

    fun selectCategory(category: BrowseCategory) {
        _searchQuery.value = category.title
        viewModelScope.launch {
            _isSearching.value = true
            _searchResults.value = innerTubeClient.searchSongs(category.query)
            _isSearching.value = false
        }
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
