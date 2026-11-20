package com.hybridmusic.player.innertube

data class TrackItem(
    val videoId: String,
    val title: String,
    val artist: String,
    val durationMs: Long = 0L,
    val thumbnailUrl: String = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
)

data class AudioStreamInfo(
    val url: String,
    val mimeType: String,
    val bitrate: Int,
    val userAgent: String,
    val expiresAtMs: Long
)
