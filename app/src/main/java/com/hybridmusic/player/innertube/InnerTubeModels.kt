package com.hybridmusic.player.innertube

import kotlinx.serialization.Serializable

@Serializable
data class TrackItem(
    val videoId: String,
    val title: String,
    val artist: String,
    val durationMs: Long = 0L,
    val thumbnailUrl: String = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
)

@Serializable
data class YtPlaylistCard(
    val browseId: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String
)

@Serializable
data class UserPlaylist(
    val id: String,
    val name: String,
    val tracks: List<TrackItem> = emptyList()
)

data class HomeShelf(
    val title: String,
    val tracks: List<TrackItem> = emptyList(),
    val playlists: List<YtPlaylistCard> = emptyList()
)

data class MoodChip(
    val title: String,
    val queryOrParams: String,
    val isBrowseParams: Boolean = false
)

data class HomeFeedResult(
    val moods: List<MoodChip>,
    val quickPicks: List<TrackItem>,
    val shelves: List<HomeShelf>
)

data class NextContextResult(
    val radioTracks: List<TrackItem>,
    val lyricsBrowseId: String?,
    val relatedBrowseId: String?
)

data class LyricsResult(
    val lines: List<String>,
    val source: String
)

data class RelatedResult(
    val tracks: List<TrackItem>,
    val playlists: List<YtPlaylistCard>
)

data class AudioStreamInfo(
    val url: String,
    val mimeType: String,
    val bitrate: Int,
    val userAgent: String,
    val expiresAtMs: Long,
    val fetchLatencyMs: Long = 0L
)
