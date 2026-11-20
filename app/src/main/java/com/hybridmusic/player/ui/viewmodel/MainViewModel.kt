package com.hybridmusic.player.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hybridmusic.player.data.LibraryRepository
import com.hybridmusic.player.innertube.HomeFeedResult
import com.hybridmusic.player.innertube.InnerTubeClient
import com.hybridmusic.player.innertube.LyricsResult
import com.hybridmusic.player.innertube.MoodChip
import com.hybridmusic.player.innertube.RelatedResult
import com.hybridmusic.player.innertube.TrackItem
import com.hybridmusic.player.innertube.UserPlaylist
import com.hybridmusic.player.innertube.YtMusicLinkParser
import com.hybridmusic.player.innertube.YtPlaylistCard
import com.hybridmusic.player.player.PlaybackUiState
import com.hybridmusic.player.player.PlayerController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BrowseCategory(
    val title: String,
    val query: String,
    val colorHex: Long
)

data class ActiveCollectionView(
    val id: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String,
    val tracks: List<TrackItem>,
    val isLoading: Boolean = false,
    val ytCard: YtPlaylistCard? = null
)

class MainViewModel(
    private val innerTubeClient: InnerTubeClient,
    private val libraryRepository: LibraryRepository,
    val playerController: PlayerController
) : ViewModel() {

    val playbackState: StateFlow<PlaybackUiState> = playerController.uiState
    val likedTracks: StateFlow<List<TrackItem>> = libraryRepository.likedTracks
    val savedYtPlaylists: StateFlow<List<YtPlaylistCard>> = libraryRepository.savedYtPlaylists
    val userPlaylists: StateFlow<List<UserPlaylist>> = libraryRepository.userPlaylists
    val recentTracks: StateFlow<List<TrackItem>> = libraryRepository.recentTracks

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

    private val _searchSuggestions = MutableStateFlow<List<String>>(emptyList())
    val searchSuggestions: StateFlow<List<String>> = _searchSuggestions.asStateFlow()

    private val _searchFilterIndex = MutableStateFlow(0)
    val searchFilterIndex: StateFlow<Int> = _searchFilterIndex.asStateFlow()

    private val _searchSongsResult = MutableStateFlow<List<TrackItem>>(emptyList())
    val searchSongsResult: StateFlow<List<TrackItem>> = _searchSongsResult.asStateFlow()

    private val _searchPlaylistsResult = MutableStateFlow<List<YtPlaylistCard>>(emptyList())
    val searchPlaylistsResult: StateFlow<List<YtPlaylistCard>> = _searchPlaylistsResult.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _autoplayRadioEnabled = MutableStateFlow(true)
    val autoplayRadioEnabled: StateFlow<Boolean> = _autoplayRadioEnabled.asStateFlow()

    private val _lyricsResult = MutableStateFlow<LyricsResult?>(null)
    val lyricsResult: StateFlow<LyricsResult?> = _lyricsResult.asStateFlow()

    private val _isLoadingLyrics = MutableStateFlow(false)
    val isLoadingLyrics: StateFlow<Boolean> = _isLoadingLyrics.asStateFlow()

    private val _relatedResult = MutableStateFlow(RelatedResult(emptyList(), emptyList()))
    val relatedResult: StateFlow<RelatedResult> = _relatedResult.asStateFlow()

    private val _isLoadingRelated = MutableStateFlow(false)
    val isLoadingRelated: StateFlow<Boolean> = _isLoadingRelated.asStateFlow()

    private val _activeCollection = MutableStateFlow<ActiveCollectionView?>(null)
    val activeCollection: StateFlow<ActiveCollectionView?> = _activeCollection.asStateFlow()

    private val _isFullPlayerOpen = MutableStateFlow(false)
    val isFullPlayerOpen: StateFlow<Boolean> = _isFullPlayerOpen.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _sleepTimerRemainingSec = MutableStateFlow<Int?>(null)
    val sleepTimerRemainingSec: StateFlow<Int?> = _sleepTimerRemainingSec.asStateFlow()

    private var suggestionJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var contextFetchJob: Job? = null

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
        viewModelScope.launch { innerTubeClient.prewarmConnections() }
        playerController.onAutoTrackChanged = { newTrack ->
            libraryRepository.recordRecent(newTrack)
            fetchTrackMetadataContext(newTrack, updateRadioQueue = false)
        }
        loadHomeFeed(0)
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
        _activeCollection.value = null
    }

    fun openFullPlayer(open: Boolean) {
        _isFullPlayerOpen.value = open
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun toggleAutoplayRadio() {
        _autoplayRadioEnabled.value = !_autoplayRadioEnabled.value
    }

    fun loadHomeFeed(moodIndex: Int = 0) {
        viewModelScope.launch {
            _selectedMoodIndex.value = moodIndex
            _isLoadingHome.value = true
            val mood = _homeFeed.value.moods.getOrNull(moodIndex)
            val feed = innerTubeClient.fetchHomeFeed(if (moodIndex == 0) null else mood)
            _homeFeed.value = feed
            _isLoadingHome.value = false

            // อุ่นสตรีมเพลง Quick Picks 3 เพลงแรกไว้ล่วงหน้าเพื่อให้กดเล่นได้ทันที
            launch(Dispatchers.IO) {
                feed.quickPicks.take(3).forEach { track ->
                    innerTubeClient.getCleanAudioStream(track.videoId)
                }
            }
        }
    }

    fun playTrackOneClick(track: TrackItem, baseQueue: List<TrackItem> = listOf(track), forceRadio: Boolean = false) {
        libraryRepository.recordRecent(track)
        val initialQueue = if (baseQueue.isEmpty() || forceRadio) listOf(track) else baseQueue
        playerController.playTrack(track, initialQueue)

        val shouldPopulateRadio = forceRadio || (_autoplayRadioEnabled.value && initialQueue.size <= 1)
        fetchTrackMetadataContext(track, updateRadioQueue = shouldPopulateRadio)
    }

    private fun fetchTrackMetadataContext(track: TrackItem, updateRadioQueue: Boolean) {
        contextFetchJob?.cancel()
        contextFetchJob = viewModelScope.launch {
            _isLoadingLyrics.value = true
            _isLoadingRelated.value = true

            val nextCtx = innerTubeClient.fetchNextContext(track.videoId)
            if (updateRadioQueue && nextCtx.radioTracks.isNotEmpty()) {
                val merged = listOf(track) + nextCtx.radioTracks.filter { it.videoId != track.videoId }
                if (playerController.uiState.value.currentTrack?.videoId == track.videoId) {
                    // อัปเดตคิววิทยุและเริ่ม Pre-fetch เพลงถัดไปทันทีโดยไม่สะดุดเพลงปัจจุบัน
                    playerController.updateQueue(merged)
                }
            }

            _lyricsResult.value = innerTubeClient.fetchLyrics(nextCtx.lyricsBrowseId, track.title, track.artist)
            _isLoadingLyrics.value = false

            _relatedResult.value = innerTubeClient.fetchRelatedContent(nextCtx.relatedBrowseId, track.artist)
            _isLoadingRelated.value = false
        }
    }

    fun startRadioForTrack(track: TrackItem) {
        _statusBanner.value = "กำลังเปิดสถานีวิทยุของ \"${track.title}\"..."
        playTrackOneClick(track, listOf(track), forceRadio = true)
    }

    fun openYtPlaylist(card: YtPlaylistCard) {
        viewModelScope.launch {
            _activeCollection.value = ActiveCollectionView(
                id = card.browseId,
                title = card.title,
                subtitle = card.subtitle,
                thumbnailUrl = card.thumbnailUrl,
                tracks = emptyList(),
                isLoading = true,
                ytCard = card
            )
            val tracks = innerTubeClient.fetchPlaylistTracks(card.browseId)
                .ifEmpty { innerTubeClient.searchSongs(card.title) }
            _activeCollection.value = _activeCollection.value?.copy(
                tracks = tracks,
                isLoading = false
            )
        }
    }

    fun openLocalCollection(id: String, title: String, subtitle: String, tracks: List<TrackItem>) {
        val thumb = tracks.firstOrNull()?.thumbnailUrl ?: ""
        _activeCollection.value = ActiveCollectionView(
            id = id,
            title = title,
            subtitle = subtitle,
            thumbnailUrl = thumb,
            tracks = tracks,
            isLoading = false
        )
    }

    fun closeCollection() {
        _activeCollection.value = null
    }

    fun shufflePlayList(tracks: List<TrackItem>) {
        if (tracks.isEmpty()) return
        val shuffled = tracks.shuffled()
        playTrackOneClick(shuffled.first(), shuffled)
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
        suggestionJob?.cancel()
        if (newQuery.isBlank()) {
            _searchSuggestions.value = emptyList()
            _searchSongsResult.value = emptyList()
            _searchPlaylistsResult.value = emptyList()
            return
        }
        suggestionJob = viewModelScope.launch {
            delay(250L)
            _searchSuggestions.value = innerTubeClient.fetchSearchSuggestions(newQuery)
        }
    }

    fun setSearchFilter(index: Int) {
        _searchFilterIndex.value = index
        if (_searchQuery.value.isNotBlank()) {
            submitSearchOrLink()
        }
    }

    fun submitSearchOrLink(overrideInput: String? = null) {
        val raw = (overrideInput ?: _searchQuery.value).trim()
        if (raw.isEmpty()) return
        _searchQuery.value = raw
        _searchSuggestions.value = emptyList()

        viewModelScope.launch {
            _isSearching.value = true
            _statusBanner.value = null

            val playlistId = YtMusicLinkParser.extractPlaylistId(raw)
            val videoId = YtMusicLinkParser.extractVideoId(raw)

            if (videoId != null) {
                val track = innerTubeClient.getTrackByVideoId(videoId)
                _searchSongsResult.value = listOf(track) + _searchSongsResult.value.filter { it.videoId != videoId }
                _isSearching.value = false
                playTrackOneClick(track)
            } else if (playlistId != null) {
                _isSearching.value = false
                openYtPlaylist(
                    YtPlaylistCard(
                        browseId = playlistId,
                        title = "YouTube Music Playlist",
                        subtitle = "นำเข้าจากลิงก์",
                        thumbnailUrl = ""
                    )
                )
            } else if (YtMusicLinkParser.isUrlInput(raw)) {
                _isSearching.value = false
                _statusBanner.value = "ไม่พบรหัสเพลงหรือเพลย์ลิสต์ในลิงก์นี้"
            } else {
                when (_searchFilterIndex.value) {
                    0 -> {
                        val songs = innerTubeClient.searchSongs(raw)
                        _searchSongsResult.value = songs
                        // Pre-fetch เพลงแรกของผลการค้นหาทันที
                        songs.firstOrNull()?.let { first ->
                            launch(Dispatchers.IO) { innerTubeClient.getCleanAudioStream(first.videoId) }
                        }
                    }
                    1 -> _searchPlaylistsResult.value = innerTubeClient.searchPlaylists(raw, albumsOnly = false)
                    2 -> _searchPlaylistsResult.value = innerTubeClient.searchPlaylists(raw, albumsOnly = true)
                }
                _isSearching.value = false
            }
        }
    }

    fun selectCategory(category: BrowseCategory) {
        _searchFilterIndex.value = 0
        _searchQuery.value = category.title
        _searchSuggestions.value = emptyList()
        viewModelScope.launch {
            _isSearching.value = true
            _searchSongsResult.value = innerTubeClient.searchSongs(category.query)
            _isSearching.value = false
        }
    }

    fun toggleLikeTrack(track: TrackItem) {
        val liked = libraryRepository.toggleLike(track)
        _statusBanner.value = if (liked) "เพิ่ม \"${track.title}\" ลงเพลงที่ถูกใจแล้ว" else "นำออกจากเพลงที่ถูกใจแล้ว"
    }

    fun toggleSaveYtPlaylist(card: YtPlaylistCard) {
        val saved = libraryRepository.toggleSaveYtPlaylist(card)
        _statusBanner.value = if (saved) "บันทึกเพลย์ลิสต์ \"${card.title}\" เข้าคลังแล้ว" else "นำเพลย์ลิสต์ออกจากคลังแล้ว"
    }

    fun createPlaylist(name: String, initialTrack: TrackItem? = null) {
        if (name.isBlank()) return
        libraryRepository.createUserPlaylist(name, initialTrack)
        _statusBanner.value = "สร้างเพลย์ลิสต์ \"$name\" แล้ว"
    }

    fun addTrackToPlaylist(playlistId: String, track: TrackItem) {
        libraryRepository.addTrackToUserPlaylist(playlistId, track)
        _statusBanner.value = "เพิ่ม \"${track.title}\" ลงเพลย์ลิสต์แล้ว"
    }

    fun deleteUserPlaylist(playlistId: String) {
        libraryRepository.deleteUserPlaylist(playlistId)
        _activeCollection.value = null
        _statusBanner.value = "ลบเพลย์ลิสต์แล้ว"
    }

    fun setSleepTimerMinutes(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerRemainingSec.value = null
            _statusBanner.value = "ยกเลิกตัวตั้งเวลาปิดเพลงแล้ว"
            return
        }
        val totalSec = minutes * 60
        _sleepTimerRemainingSec.value = totalSec
        _statusBanner.value = "ตั้งเวลาปิดเพลงอัตโนมัติใน $minutes นาที"
        sleepTimerJob = viewModelScope.launch {
            var left = totalSec
            while (left > 0) {
                delay(1000L)
                left--
                _sleepTimerRemainingSec.value = left
            }
            _sleepTimerRemainingSec.value = null
            if (playerController.uiState.value.isPlaying) {
                playerController.togglePlayPause()
            }
            _statusBanner.value = "หยุดเล่นเพลงตามเวลาที่ตั้งไว้แล้ว"
        }
    }

    class Factory(
        private val innerTubeClient: InnerTubeClient,
        private val libraryRepository: LibraryRepository,
        private val playerController: PlayerController
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(innerTubeClient, libraryRepository, playerController) as T
        }
    }
}
