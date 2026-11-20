package com.hybridmusic.player.innertube

object YtMusicLinkParser {
    private val VIDEO_ID_REGEX = Regex("^[a-zA-Z0-9_-]{11}$")
    private val VIDEO_PATTERNS = listOf(
        Regex("[?&]v=([a-zA-Z0-9_-]{11})"),
        Regex("youtu\\.be/([a-zA-Z0-9_-]{11})"),
        Regex("youtube\\.com/shorts/([a-zA-Z0-9_-]{11})"),
        Regex("youtube\\.com/embed/([a-zA-Z0-9_-]{11})"),
        Regex("youtube\\.com/live/([a-zA-Z0-9_-]{11})")
    )
    private val PLAYLIST_PATTERN = Regex("[?&]list=([a-zA-Z0-9_-]{10,})")

    fun extractVideoId(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        if (VIDEO_ID_REGEX.matches(trimmed)) return trimmed
        for (pattern in VIDEO_PATTERNS) {
            val match = pattern.find(trimmed)?.groupValues?.getOrNull(1)
            if (!match.isNullOrBlank()) return match
        }
        return null
    }

    fun extractPlaylistId(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("PL") || trimmed.startsWith("OLAK5uy_") || trimmed.startsWith("VLPL")) {
            if (!trimmed.contains(" ") && !trimmed.contains("/")) return trimmed
        }
        return PLAYLIST_PATTERN.find(trimmed)?.groupValues?.getOrNull(1)
    }

    fun isUrlInput(input: String): Boolean {
        val trimmed = input.trim()
        return trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.contains("youtube.com/", ignoreCase = true) ||
            trimmed.contains("youtu.be/", ignoreCase = true)
    }
}
