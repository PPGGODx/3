package com.hybridmusic.player.offline

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import com.hybridmusic.player.innertube.TrackItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

typealias DownloadProgressMap = Map<String, DownloadTaskProgress>

class OfflineAudioRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val rootDir = File(appContext.filesDir, "offline_music").apply { mkdirs() }
    val audioDir = File(rootDir, "audio").apply { mkdirs() }
    val coversDir = File(rootDir, "covers").apply { mkdirs() }
    private val indexFile = File(rootDir, "offline_index.json")

    private val _offlineTracks = MutableStateFlow<List<OfflineTrackEntity>>(emptyList())
    val offlineTracks: StateFlow<List<OfflineTrackEntity>> = _offlineTracks.asStateFlow()

    private val _activeDownloads = MutableStateFlow<DownloadProgressMap>(emptyMap())
    val activeDownloads: StateFlow<DownloadProgressMap> = _activeDownloads.asStateFlow()

    init {
        loadIndexFromDisk()
    }

    @Synchronized
    private fun loadIndexFromDisk() {
        if (!indexFile.exists()) {
            _offlineTracks.value = emptyList()
            return
        }
        runCatching {
            val raw = indexFile.readText(Charsets.UTF_8)
            val arr = JSONArray(raw)
            val validList = mutableListOf<OfflineTrackEntity>()
            for (i in 0 until arr.length()) {
                val item = OfflineTrackEntity.fromJson(arr.getJSONObject(i)) ?: continue
                val f = File(item.audioPath)
                if (f.exists() && f.length() > 1024L) {
                    validList.add(item)
                }
            }
            _offlineTracks.value = validList.sortedByDescending { it.downloadedAt }
        }
    }

    @Synchronized
    private fun saveIndexToDisk(list: List<OfflineTrackEntity>) {
        runCatching {
            val arr = JSONArray()
            list.forEach { arr.put(it.toJson()) }
            val tmpFile = File(rootDir, "offline_index.json.tmp")
            tmpFile.writeText(arr.toString(2), Charsets.UTF_8)
            if (indexFile.exists()) indexFile.delete()
            tmpFile.renameTo(indexFile)
        }
    }

    fun getOfflineTrack(videoId: String): OfflineTrackEntity? {
        val entity = _offlineTracks.value.firstOrNull { it.videoId == videoId } ?: return null
        val file = File(entity.audioPath)
        return if (file.exists() && file.length() > 1024L) entity else null
    }

    fun isDownloaded(videoId: String): Boolean = getOfflineTrack(videoId) != null

    fun getLocalCoverFile(videoId: String): File? {
        val f = File(coversDir, "$videoId.webp")
        return if (f.exists() && f.length() > 100L) f else null
    }

    fun totalStorageBytes(): Long {
        return _offlineTracks.value.sumOf { entity ->
            val audioLen = File(entity.audioPath).takeIf { it.exists() }?.length() ?: entity.fileSizeBytes
            val coverLen = File(entity.coverPath).takeIf { it.exists() }?.length() ?: 0L
            audioLen + coverLen
        }
    }

    fun formatStorageSize(bytes: Long): String {
        if (bytes <= 0L) return "0 MB"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format("%.2f GB", gb)
            mb >= 1.0 -> String.format("%.1f MB", mb)
            else -> String.format("%.0f KB", kb)
        }
    }

    fun updateDownloadProgress(progress: DownloadTaskProgress) {
        _activeDownloads.update { current ->
            current + (progress.videoId to progress)
        }
    }

    fun clearDownloadState(videoId: String) {
        _activeDownloads.update { current ->
            current - videoId
        }
    }

    suspend fun saveCompressedCover(videoId: String, rawBytes: ByteArray): String = withContext(Dispatchers.IO) {
        val outFile = File(coversDir, "$videoId.webp")
        runCatching {
            val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
                ?: return@runCatching ""
            val maxDim = 512
            val scaled = if (decoded.width > maxDim || decoded.height > maxDim) {
                val ratio = maxDim.toFloat() / max(decoded.width, decoded.height).toFloat()
                val targetW = (decoded.width * ratio).roundToInt().coerceAtLeast(1)
                val targetH = (decoded.height * ratio).roundToInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(decoded, targetW, targetH, true).also {
                    if (it != decoded) decoded.recycle()
                }
            } else {
                decoded
            }

            FileOutputStream(outFile).use { fos ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    scaled.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, fos)
                } else {
                    @Suppress("DEPRECATION")
                    scaled.compress(Bitmap.CompressFormat.WEBP, 80, fos)
                }
            }
            scaled.recycle()
            outFile.absolutePath
        }.getOrDefault("")
    }

    @Synchronized
    fun registerCompletedTrack(entity: OfflineTrackEntity) {
        val updated = (_offlineTracks.value.filterNot { it.videoId == entity.videoId } + entity)
            .sortedByDescending { it.downloadedAt }
        _offlineTracks.value = updated
        saveIndexToDisk(updated)
        clearDownloadState(entity.videoId)
    }

    @Synchronized
    fun deleteOfflineTrack(videoId: String) {
        val target = _offlineTracks.value.firstOrNull { it.videoId == videoId }
        if (target != null) {
            runCatching { File(target.audioPath).delete() }
            runCatching { File(target.coverPath).delete() }
        }
        runCatching { File(audioDir, "$videoId.opus").delete() }
        runCatching { File(audioDir, "$videoId.m4a").delete() }
        runCatching { File(coversDir, "$videoId.webp").delete() }

        val updated = _offlineTracks.value.filterNot { it.videoId == videoId }
        _offlineTracks.value = updated
        saveIndexToDisk(updated)
        clearDownloadState(videoId)
    }

    @Synchronized
    fun clearAllOffline() {
        _offlineTracks.value.forEach { entity ->
            runCatching { File(entity.audioPath).delete() }
            runCatching { File(entity.coverPath).delete() }
        }
        audioDir.listFiles()?.forEach { runCatching { it.delete() } }
        coversDir.listFiles()?.forEach { runCatching { it.delete() } }
        _offlineTracks.value = emptyList()
        saveIndexToDisk(emptyList())
        _activeDownloads.value = emptyMap()
    }

    companion object {
        @Volatile
        private var INSTANCE: OfflineAudioRepository? = null

        fun getInstance(context: Context): OfflineAudioRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OfflineAudioRepository(context).also { INSTANCE = it }
            }
        }
    }
}

fun TrackItem.effectiveArtworkModel(context: Context): Any {
    val localCover = OfflineAudioRepository.getInstance(context).getLocalCoverFile(videoId)
    return localCover ?: thumbnailUrl
}
