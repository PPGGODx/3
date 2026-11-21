package com.hybridmusic.player.offline

import android.content.Context
import com.hybridmusic.player.innertube.InnerTubeClient
import com.hybridmusic.player.innertube.TrackItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

class OfflineDownloadManager private constructor(
    context: Context,
    private val innerTubeClient: InnerTubeClient
) {
    private val appContext = context.applicationContext
    private val repository = OfflineAudioRepository.getInstance(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val semaphore = Semaphore(permits = 2)
    private val runningJobs = ConcurrentHashMap<String, Job>()

    private data class ResolvedAudioStream(
        val url: String,
        val extension: String,
        val codecLabel: String,
        val bitrateKbps: Int,
        val contentLength: Long,
        val userAgent: String
    )

    fun enqueueDownload(track: TrackItem, onCompleteMessage: ((String) -> Unit)? = null) {
        if (repository.isDownloaded(track.videoId)) {
            onCompleteMessage?.invoke("ดาวน์โหลด \"${track.title}\" ไว้แล้ว")
            return
        }
        if (runningJobs[track.videoId]?.isActive == true) {
            return
        }

        repository.updateDownloadProgress(
            DownloadTaskProgress(
                videoId = track.videoId,
                title = track.title,
                status = OfflineDownloadStatus.QUEUED,
                progress = 0f
            )
        )

        val job = scope.launch {
            semaphore.withPermit {
                try {
                    repository.updateDownloadProgress(
                        DownloadTaskProgress(
                            videoId = track.videoId,
                            title = track.title,
                            status = OfflineDownloadStatus.DOWNLOADING,
                            progress = 0.03f
                        )
                    )

                    val stream = resolveBestEfficiencyStream(track.videoId)
                        ?: throw IOException("ไม่พบสตรีมเสียงสำหรับดาวน์โหลด")

                    val tempAudioFile = File(repository.audioDir, "${track.videoId}.${stream.extension}.part")
                    val finalAudioFile = File(repository.audioDir, "${track.videoId}.${stream.extension}")
                    if (tempAudioFile.exists()) tempAudioFile.delete()

                    downloadChunkedAudio(
                        stream = stream,
                        outFile = tempAudioFile,
                        onProgress = { fraction ->
                            repository.updateDownloadProgress(
                                DownloadTaskProgress(
                                    videoId = track.videoId,
                                    title = track.title,
                                    status = OfflineDownloadStatus.DOWNLOADING,
                                    progress = (0.05f + fraction * 0.88f).coerceIn(0.05f, 0.94f)
                                )
                            )
                        }
                    )

                    if (!tempAudioFile.exists() || tempAudioFile.length() < 4096L) {
                        throw IOException("ไฟล์เสียงไม่สมบูรณ์")
                    }
                    if (finalAudioFile.exists()) finalAudioFile.delete()
                    tempAudioFile.renameTo(finalAudioFile)

                    // ดาวน์โหลดและบีบอัดปกเพลงเป็น WebP 512x512 (~30KB)
                    val coverPath = downloadAndCompressCover(track)

                    val entity = OfflineTrackEntity(
                        videoId = track.videoId,
                        title = track.title,
                        artist = track.artist,
                        audioPath = finalAudioFile.absolutePath,
                        coverPath = coverPath,
                        fileSizeBytes = finalAudioFile.length(),
                        codecLabel = stream.codecLabel,
                        bitrateKbps = stream.bitrateKbps,
                        downloadedAt = System.currentTimeMillis()
                    )
                    repository.registerCompletedTrack(entity)

                    val sizeLabel = repository.formatStorageSize(entity.fileSizeBytes)
                    withContext(Dispatchers.Main) {
                        onCompleteMessage?.invoke("บันทึกออฟไลน์แล้ว: ${track.title} ($sizeLabel • ${stream.codecLabel})")
                    }
                } catch (e: Exception) {
                    repository.updateDownloadProgress(
                        DownloadTaskProgress(
                            videoId = track.videoId,
                            title = track.title,
                            status = OfflineDownloadStatus.FAILED,
                            progress = 0f,
                            errorMessage = e.message ?: "ดาวน์โหลดล้มเหลว"
                        )
                    )
                    withContext(Dispatchers.Main) {
                        onCompleteMessage?.invoke("ดาวน์โหลดไม่สำเร็จ: ${track.title}")
                    }
                } finally {
                    runningJobs.remove(track.videoId)
                }
            }
        }
        runningJobs[track.videoId] = job
    }

    fun enqueueBatch(tracks: List<TrackItem>, onStatus: ((String) -> Unit)? = null) {
        val pending = tracks.filterNot { repository.isDownloaded(it.videoId) }
        if (pending.isEmpty()) {
            onStatus?.invoke("เพลงทั้งหมดถูกดาวน์โหลดไว้แล้ว")
            return
        }
        onStatus?.invoke("กำลังดาวน์โหลด ${pending.size} เพลงสำหรับฟังออฟไลน์...")
        pending.forEach { enqueueDownload(it) }
    }

    fun cancelDownload(videoId: String) {
        runningJobs.remove(videoId)?.cancel()
        repository.clearDownloadState(videoId)
        runCatching { File(repository.audioDir, "$videoId.opus.part").delete() }
        runCatching { File(repository.audioDir, "$videoId.m4a.part").delete() }
    }

    /**
     * คัดเลือกสตรีมเสียงที่กินพื้นที่น้อยที่สุดแต่คุณภาพสูงสุด:
     * 1. Opus 128-160 kbps (itag 251) -> คุณภาพเทียบเท่า MP3 320k แต่ไฟล์เล็กกว่า ~60%
     * 2. Opus 70 kbps (itag 250) -> เสียงใสมาก กินพื้นที่ ~0.5 MB/นาที
     * 3. AAC-LC 128 kbps M4A (itag 140) -> รองรับมาตรฐานสูงสุด
     */
    private suspend fun resolveBestEfficiencyStream(videoId: String): ResolvedAudioStream? {
        val clients = listOf(
            Triple("ANDROID_VR", "1.56.21", "com.google.android.apps.youtube.vr.oculus/1.56.21 (Linux; U; Android 13; Quest 3 Build/TQ3A.230805.001) gzip"),
            Triple("IOS", "19.29.1", "com.google.ios.youtube/19.29.1 (iPhone16,2; U; CPU iOS 17_5_1 like Mac OS X)"),
            Triple("ANDROID_MUSIC", "7.16.53", "com.google.android.apps.youtube.music/7.16.53 (Linux; U; Android 14) gzip")
        )

        for ((clientName, clientVersion, ua) in clients) {
            val candidate = fetchFromInnerTubePlayer(videoId, clientName, clientVersion, ua)
            if (candidate != null) return candidate
        }

        // Fallback ผ่าน InnerTubeClient เดิมของโปรเจกต์
        val fallbackUrl = runCatching {
            val method = innerTubeClient.javaClass.methods.firstOrNull { m ->
                m.parameterTypes.size == 1 &&
                    m.parameterTypes[0] == String::class.java &&
                    (m.name.contains("Stream", ignoreCase = true) || m.name.contains("Audio", ignoreCase = true))
            }
            method?.invoke(innerTubeClient, videoId)?.toString()
        }.getOrNull()

        if (!fallbackUrl.isNullOrBlank() && fallbackUrl.startsWith("http")) {
            val isOpus = fallbackUrl.contains("mime=audio%2Fwebm") || fallbackUrl.contains("itag=251")
            return ResolvedAudioStream(
                url = fallbackUrl,
                extension = if (isOpus) "opus" else "m4a",
                codecLabel = if (isOpus) "OPUS 128k" else "AAC 128k",
                bitrateKbps = 128,
                contentLength = -1L,
                userAgent = "com.google.android.apps.youtube.music/7.16.53 (Linux; U; Android 14) gzip"
            )
        }
        return null
    }

    private fun fetchFromInnerTubePlayer(
        videoId: String,
        clientName: String,
        clientVersion: String,
        userAgent: String
    ): ResolvedAudioStream? {
        return runCatching {
            val endpoint = URL("https://www.youtube.com/youtubei/v1/player?prettyPrint=false")
            val conn = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 12_000
                readTimeout = 15_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("User-Agent", userAgent)
                setRequestProperty("X-YouTube-Client-Name", if (clientName == "IOS") "5" else "28")
                setRequestProperty("X-YouTube-Client-Version", clientVersion)
            }

            val clientObj = JSONObject().apply {
                put("clientName", clientName)
                put("clientVersion", clientVersion)
                put("hl", "th")
                put("gl", "TH")
                if (clientName == "IOS") {
                    put("deviceMake", "Apple")
                    put("deviceModel", "iPhone16,2")
                    put("osName", "iPhone")
                    put("osVersion", "17.5.1")
                } else {
                    put("androidSdkVersion", 33)
                }
            }
            val payload = JSONObject().apply {
                put("videoId", videoId)
                put("context", JSONObject().put("client", clientObj))
                put("contentCheckOk", true)
                put("racyCheckOk", true)
            }

            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray(Charsets.UTF_8))
            }

            if (conn.responseCode !in 200..299) return@runCatching null
            val responseText = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val json = JSONObject(responseText)
            val streamingData = json.optJSONObject("streamingData") ?: return@runCatching null
            val adaptiveFormats = streamingData.optJSONArray("adaptiveFormats") ?: JSONArray()

            // เรียงลำดับความคุ้มค่า: itag 251 (Opus ~128k) > itag 250 (Opus ~70k) > itag 140 (AAC 128k) > audio อื่นๆ
            val priorityItags = listOf(251, 250, 140, 249, 139)
            var bestObj: JSONObject? = null
            var bestScore = Int.MAX_VALUE

            for (i in 0 until adaptiveFormats.length()) {
                val fmt = adaptiveFormats.getJSONObject(i)
                val mime = fmt.optString("mimeType")
                if (!mime.startsWith("audio/")) continue
                val url = fmt.optString("url")
                if (url.isBlank()) continue

                val itag = fmt.optInt("itag", -1)
                val idx = priorityItags.indexOf(itag)
                val score = if (idx >= 0) idx else (100 + i)
                if (score < bestScore) {
                    bestScore = score
                    bestObj = fmt
                }
            }

            val chosen = bestObj ?: return@runCatching null
            val streamUrl = chosen.optString("url")
            val mimeType = chosen.optString("mimeType")
            val isOpus = mimeType.contains("opus", ignoreCase = true) || mimeType.contains("webm", ignoreCase = true)
            val bitrateBps = chosen.optInt("averageBitrate", chosen.optInt("bitrate", 128_000))
            val kbps = (bitrateBps / 1000).coerceIn(48, 256)
            val contentLength = chosen.optString("contentLength").toLongOrNull() ?: -1L

            ResolvedAudioStream(
                url = streamUrl,
                extension = if (isOpus) "opus" else "m4a",
                codecLabel = if (isOpus) "OPUS ${kbps}k" else "AAC ${kbps}k",
                bitrateKbps = kbps,
                contentLength = contentLength,
                userAgent = userAgent
            )
        }.getOrNull()
    }

    private fun downloadChunkedAudio(
        stream: ResolvedAudioStream,
        outFile: File,
        onProgress: (Float) -> Unit
    ) {
        val chunkSize = 524_288L // 512 KB ต่อก้อน ป้องกันการลดสปีดจาก YouTube
        var downloadedBytes = 0L
        var totalBytes = stream.contentLength

        FileOutputStream(outFile).use { fos ->
            val buffer = ByteArray(16 * 1024)
            while (true) {
                val rangeStart = downloadedBytes
                val rangeEnd = if (totalBytes > 0L) {
                    min(rangeStart + chunkSize - 1L, totalBytes - 1L)
                } else {
                    rangeStart + chunkSize - 1L
                }
                if (totalBytes > 0L && rangeStart >= totalBytes) break

                var chunkSuccess = false
                var lastErr: Exception? = null

                for (attempt in 1..3) {
                    try {
                        val conn = (URL(stream.url).openConnection() as HttpURLConnection).apply {
                            requestMethod = "GET"
                            connectTimeout = 12_000
                            readTimeout = 20_000
                            setRequestProperty("User-Agent", stream.userAgent)
                            setRequestProperty("Range", "bytes=$rangeStart-$rangeEnd")
                        }
                        val code = conn.responseCode
                        if (code !in 200..299 && code != 206) {
                            throw IOException("HTTP $code")
                        }

                        if (totalBytes <= 0L) {
                            val contentRange = conn.getHeaderField("Content-Range")
                            val parsedTotal = contentRange?.substringAfterLast('/')?.trim()?.toLongOrNull()
                            if (parsedTotal != null && parsedTotal > 0L) {
                                totalBytes = parsedTotal
                            }
                        }

                        var bytesInThisChunk = 0L
                        conn.inputStream.use { input ->
                            while (true) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                fos.write(buffer, 0, read)
                                downloadedBytes += read
                                bytesInThisChunk += read
                                if (totalBytes > 0L) {
                                    onProgress((downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f))
                                }
                            }
                        }
                        chunkSuccess = true
                        if (bytesInThisChunk == 0L || code == 200) {
                            totalBytes = downloadedBytes
                        }
                        break
                    } catch (e: Exception) {
                        lastErr = e
                        Thread.sleep(350L * attempt)
                    }
                }

                if (!chunkSuccess) {
                    throw lastErr ?: IOException("Chunk download failed at byte $rangeStart")
                }
                if (totalBytes > 0L && downloadedBytes >= totalBytes) break
            }
            fos.flush()
        }
    }

    private suspend fun downloadAndCompressCover(track: TrackItem): String {
        val candidateUrls = listOf(
            "https://i.ytimg.com/vi/${track.videoId}/hqdefault.jpg",
            track.thumbnailUrl
        ).distinct().filter { it.startsWith("http") }

        for (coverUrl in candidateUrls) {
            val rawBytes = runCatching {
                val conn = (URL(coverUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8_000
                    readTimeout = 10_000
                }
                if (conn.responseCode in 200..299) {
                    conn.inputStream.use { input ->
                        val baos = ByteArrayOutputStream()
                        input.copyTo(baos)
                        baos.toByteArray()
                    }
                } else null
            }.getOrNull()

            if (rawBytes != null && rawBytes.isNotEmpty()) {
                val saved = repository.saveCompressedCover(track.videoId, rawBytes)
                if (saved.isNotBlank()) return saved
            }
        }
        return ""
    }

    companion object {
        @Volatile
        private var INSTANCE: OfflineDownloadManager? = null

        fun getInstance(context: Context, innerTubeClient: InnerTubeClient): OfflineDownloadManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OfflineDownloadManager(context, innerTubeClient).also { INSTANCE = it }
            }
        }
    }
}
