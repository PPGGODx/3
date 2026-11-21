package com.hybridmusic.player.offline

import com.hybridmusic.player.innertube.TrackItem
import org.json.JSONObject

data class OfflineTrackEntity(
    val videoId: String,
    val title: String,
    val artist: String,
    val audioPath: String,
    val coverPath: String,
    val fileSizeBytes: Long,
    val codecLabel: String,
    val bitrateKbps: Int,
    val downloadedAt: Long
) {
    fun toTrackItem(): TrackItem = TrackItem(
        videoId = videoId,
        title = title,
        artist = artist,
        thumbnailUrl = coverPath
    )

    fun toJson(): JSONObject = JSONObject().apply {
        put("videoId", videoId)
        put("title", title)
        put("artist", artist)
        put("audioPath", audioPath)
        put("coverPath", coverPath)
        put("fileSizeBytes", fileSizeBytes)
        put("codecLabel", codecLabel)
        put("bitrateKbps", bitrateKbps)
        put("downloadedAt", downloadedAt)
    }

    companion object {
        fun fromJson(obj: JSONObject): OfflineTrackEntity? {
            val videoId = obj.optString("videoId")
            val audioPath = obj.optString("audioPath")
            if (videoId.isBlank() || audioPath.isBlank()) return null
            return OfflineTrackEntity(
                videoId = videoId,
                title = obj.optString("title", "Unknown Title"),
                artist = obj.optString("artist", "Unknown Artist"),
                audioPath = audioPath,
                coverPath = obj.optString("coverPath", ""),
                fileSizeBytes = obj.optLong("fileSizeBytes", 0L),
                codecLabel = obj.optString("codecLabel", "OPUS"),
                bitrateKbps = obj.optInt("bitrateKbps", 128),
                downloadedAt = obj.optLong("downloadedAt", System.currentTimeMillis())
            )
        }
    }
}

enum class OfflineDownloadStatus {
    QUEUED,
    DOWNLOADING,
    FAILED
}

data class DownloadTaskProgress(
    val videoId: String,
    val title: String,
    val status: OfflineDownloadStatus,
    val progress: Float = 0f,
    val errorMessage: String? = null
)
