package com.hybridmusic.player.innertube

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.ConcurrentHashMap

class InnerTubeClient(
    private val httpClient: OkHttpClient
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val streamCache = ConcurrentHashMap<String, AudioStreamInfo>()

    companion object {
        private const val YTM_SEARCH_URL = "https://music.youtube.com/youtubei/v1/search?prettyPrint=false"
        private const val YT_PLAYER_URL = "https://www.youtube.com/youtubei/v1/player?prettyPrint=false"
        private const val SONG_FILTER_PARAMS = "EgWKAQIIAWoKEAMQBBAJEAoQBQ%3D%3D"

        private val urlUserAgentMap = ConcurrentHashMap<String, String>()

        const val VR_USER_AGENT =
            "com.google.android.apps.youtube.vr.oculus/1.71.26 (Linux; U; Android 14; eureka-user Build/UP1A.231005.007.A1) gzip"
        private const val IOS_USER_AGENT =
            "com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)"
        private const val WEB_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36"

        fun resolveUserAgentForUrl(url: String): String {
            urlUserAgentMap[url]?.let { return it }
            val cParam = Regex("[?&]c=([^&]+)").find(url)?.groupValues?.getOrNull(1) ?: ""
            return when {
                cParam.contains("ANDROID_VR", ignoreCase = true) -> VR_USER_AGENT
                cParam.contains("IOS", ignoreCase = true) -> IOS_USER_AGENT
                cParam.contains("WEB", ignoreCase = true) -> WEB_USER_AGENT
                else -> VR_USER_AGENT
            }
        }
    }

    suspend fun getTrackByVideoId(videoId: String): TrackItem = withContext(Dispatchers.IO) {
        fetchMetadataFromPlayer(videoId)
            ?: fetchMetadataFromOembed(videoId)
            ?: TrackItem(
                videoId = videoId,
                title = "YouTube Music ($videoId)",
                artist = "YT Music Stream",
                thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
            )
    }

    private fun fetchMetadataFromPlayer(videoId: String): TrackItem? {
        val payload = buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", "ANDROID_VR")
                    put("clientVersion", "1.71.26")
                    put("deviceMake", "Oculus")
                    put("deviceModel", "Quest 3S")
                    put("osName", "Android")
                    put("osVersion", "14")
                    put("androidSdkVersion", 34)
                    put("hl", "th")
                    put("gl", "TH")
                }
            }
            put("videoId", videoId)
        }.toString()

        val req = Request.Builder()
            .url(YT_PLAYER_URL)
            .header("User-Agent", VR_USER_AGENT)
            .header("X-YouTube-Client-Name", "28")
            .header("X-YouTube-Client-Version", "1.71.26")
            .header("X-Goog-Api-Format-Version", "2")
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        return runCatching {
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val root = json.parseToJsonElement(body).jsonObject
                val details = root["videoDetails"]?.jsonObject ?: return null
                val title = details["title"]?.jsonPrimitive?.contentOrNull ?: return null
                val author = details["author"]?.jsonPrimitive?.contentOrNull ?: "Unknown Artist"
                val lengthSec = details["lengthSeconds"]?.jsonPrimitive?.longOrNull ?: 0L
                val thumbs = details["thumbnail"]?.jsonObject?.get("thumbnails")?.jsonArray
                val thumbUrl = thumbs?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull
                    ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

                TrackItem(
                    videoId = videoId,
                    title = title,
                    artist = author.removeSuffix(" - Topic").trim(),
                    durationMs = lengthSec * 1000L,
                    thumbnailUrl = thumbUrl
                )
            }
        }.getOrNull()
    }

    private fun fetchMetadataFromOembed(videoId: String): TrackItem? {
        val req = Request.Builder()
            .url("https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json")
            .header("User-Agent", WEB_USER_AGENT)
            .get()
            .build()

        return runCatching {
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val root = json.parseToJsonElement(body).jsonObject
                val title = root["title"]?.jsonPrimitive?.contentOrNull ?: return null
                val author = root["author_name"]?.jsonPrimitive?.contentOrNull ?: "YT Music"
                TrackItem(
                    videoId = videoId,
                    title = title,
                    artist = author.removeSuffix(" - Topic").trim(),
                    thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                )
            }
        }.getOrNull()
    }

    suspend fun searchSongs(query: String): List<TrackItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val payload = buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", "WEB_REMIX")
                    put("clientVersion", "1.20250310.01.00")
                    put("hl", "th")
                    put("gl", "TH")
                }
            }
            put("query", query)
            put("params", SONG_FILTER_PARAMS)
        }.toString()

        val request = Request.Builder()
            .url(YTM_SEARCH_URL)
            .header("User-Agent", WEB_USER_AGENT)
            .header("Origin", "https://music.youtube.com")
            .header("Referer", "https://music.youtube.com/")
            .header("X-YouTube-Client-Name", "67")
            .header("X-YouTube-Client-Version", "1.20250310.01.00")
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        runCatching {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                extractTracksFromSearch(json.parseToJsonElement(body))
            }
        }.getOrDefault(emptyList())
    }

    suspend fun getCleanAudioStream(videoId: String, forceRefresh: Boolean = false): AudioStreamInfo? =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            if (!forceRefresh) {
                streamCache[videoId]?.let { cached ->
                    if (cached.expiresAtMs > now + 90_000L) return@withContext cached
                }
            } else {
                streamCache.remove(videoId)
            }

            val candidates = listOfNotNull(fetchAndroidVrAudio(videoId), fetchIosAudio(videoId))
            for (stream in candidates) {
                if (verifyStreamReachable(stream)) {
                    streamCache[videoId] = stream
                    urlUserAgentMap[stream.url] = stream.userAgent
                    return@withContext stream
                }
            }
            null
        }

    fun invalidateCache(videoId: String) {
        streamCache.remove(videoId)
    }

    private fun verifyStreamReachable(stream: AudioStreamInfo): Boolean {
        return runCatching {
            val req = Request.Builder()
                .url(stream.url)
                .header("User-Agent", stream.userAgent)
                .header("Range", "bytes=0-1023")
                .get()
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.code != 200 && resp.code != 206) return@runCatching false
                (resp.body?.byteStream()?.read() ?: -1) != -1
            }
        }.getOrDefault(false)
    }

    private fun fetchAndroidVrAudio(videoId: String): AudioStreamInfo? {
        val payload = buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", "ANDROID_VR")
                    put("clientVersion", "1.71.26")
                    put("deviceMake", "Oculus")
                    put("deviceModel", "Quest 3S")
                    put("osName", "Android")
                    put("osVersion", "14")
                    put("androidSdkVersion", 34)
                    put("hl", "th")
                    put("gl", "TH")
                }
            }
            put("videoId", videoId)
            put("contentCheckOk", true)
            put("racyCheckOk", true)
        }.toString()
        return parseAudioOnlyStream(payload, VR_USER_AGENT, "28", "1.71.26")
    }

    private fun fetchIosAudio(videoId: String): AudioStreamInfo? {
        val payload = buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", "IOS")
                    put("clientVersion", "20.10.4")
                    put("deviceMake", "Apple")
                    put("deviceModel", "iPhone16,2")
                    put("osName", "iPhone")
                    put("osVersion", "18.3.2.22D82")
                    put("hl", "th")
                    put("gl", "TH")
                }
            }
            put("videoId", videoId)
            put("contentCheckOk", true)
            put("racyCheckOk", true)
        }.toString()
        return parseAudioOnlyStream(payload, IOS_USER_AGENT, "5", "20.10.4")
    }

    private fun parseAudioOnlyStream(
        payload: String,
        userAgent: String,
        clientNameId: String,
        clientVersion: String
    ): AudioStreamInfo? {
        val req = Request.Builder()
            .url(YT_PLAYER_URL)
            .header("User-Agent", userAgent)
            .header("X-YouTube-Client-Name", clientNameId)
            .header("X-YouTube-Client-Version", clientVersion)
            .header("X-Goog-Api-Format-Version", "2")
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        return runCatching {
            httpClient.newCall(req).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val root = json.parseToJsonElement(body).jsonObject
                val status = root["playabilityStatus"]?.jsonObject?.get("status")?.jsonPrimitive?.contentOrNull
                if (status != null && status != "OK") return null

                val streamingData = root["streamingData"]?.jsonObject ?: return null
                val expiresIn = streamingData["expiresInSeconds"]?.jsonPrimitive?.longOrNull ?: 3600L
                val formats = streamingData["adaptiveFormats"]?.jsonArray ?: return null

                var bestMp4: AudioStreamInfo? = null
                var bestAny: AudioStreamInfo? = null

                for (el in formats) {
                    val obj = el.jsonObject
                    val mime = obj["mimeType"]?.jsonPrimitive?.contentOrNull ?: continue
                    if (!mime.startsWith("audio/")) continue
                    val rawUrl = obj["url"]?.jsonPrimitive?.contentOrNull ?: continue
                    if (rawUrl.isBlank()) continue
                    val url = appendCpnIfMissing(rawUrl)
                    val bitrate = obj["bitrate"]?.jsonPrimitive?.intOrNull ?: 0

                    val info = AudioStreamInfo(
                        url = url,
                        mimeType = mime,
                        bitrate = bitrate,
                        userAgent = userAgent,
                        expiresAtMs = System.currentTimeMillis() + expiresIn * 1000L
                    )
                    if (mime.startsWith("audio/mp4") && (bestMp4 == null || bitrate > bestMp4.bitrate)) bestMp4 = info
                    if (bestAny == null || bitrate > bestAny.bitrate) bestAny = info
                }
                bestMp4 ?: bestAny
            }
        }.getOrNull()
    }

    private fun appendCpnIfMissing(rawUrl: String): String {
        if (!rawUrl.contains("googlevideo.com") || rawUrl.contains("cpn=")) return rawUrl
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
        val cpn = (1..16).map { chars.random() }.joinToString("")
        val sep = if (rawUrl.contains("?")) "&" else "?"
        return "$rawUrl${sep}cpn=$cpn"
    }

    private fun extractTracksFromSearch(root: JsonElement): List<TrackItem> {
        val results = mutableListOf<TrackItem>()
        collectMusicItems(root, results, mutableSetOf())
        return results
    }

    private fun collectMusicItems(element: JsonElement, out: MutableList<TrackItem>, seenIds: MutableSet<String>) {
        when (element) {
            is JsonObject -> {
                element["musicResponsiveListItemRenderer"]?.jsonObject?.let { renderer ->
                    parseMusicListItem(renderer)?.let { track ->
                        if (seenIds.add(track.videoId)) out.add(track)
                    }
                }
                for ((_, value) in element) collectMusicItems(value, out, seenIds)
            }
            is JsonArray -> for (item in element) collectMusicItems(item, out, seenIds)
            else -> Unit
        }
    }

    private fun parseMusicListItem(renderer: JsonObject): TrackItem? {
        val videoId = renderer["playlistItemData"]?.jsonObject?.get("videoId")?.jsonPrimitive?.contentOrNull
            ?: findNestedVideoId(renderer) ?: return null
        val flexColumns = renderer["flexColumns"]?.jsonArray ?: return null
        val title = flexColumns.getOrNull(0)?.jsonObject
            ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
            ?.get("text")?.jsonObject?.get("runs")?.jsonArray
            ?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull ?: return null

        val secondaryRuns = flexColumns.getOrNull(1)?.jsonObject
            ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
            ?.get("text")?.jsonObject?.get("runs")?.jsonArray

        val subtitleParts = secondaryRuns?.mapNotNull {
            it.jsonObject["text"]?.jsonPrimitive?.contentOrNull?.trim()
        }?.filter { it.isNotEmpty() && it != "•" && it != "เพลง" && it != "Song" }.orEmpty()

        val artist = subtitleParts.firstOrNull() ?: "Unknown Artist"
        val durationMs = parseDurationToMs(subtitleParts.lastOrNull { it.contains(":") })
        val thumbnails = renderer["thumbnail"]?.jsonObject
            ?.get("musicThumbnailRenderer")?.jsonObject
            ?.get("thumbnail")?.jsonObject?.get("thumbnails")?.jsonArray
        val rawThumb = thumbnails?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull
            ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

        return TrackItem(
            videoId = videoId,
            title = title,
            artist = artist,
            durationMs = durationMs,
            thumbnailUrl = rawThumb.replace(Regex("=w\\d+-h\\d+"), "=w544-h544")
        )
    }

    private fun findNestedVideoId(element: JsonElement): String? {
        when (element) {
            is JsonObject -> {
                val vid = (element["videoId"] as? JsonPrimitive)?.contentOrNull
                if (!vid.isNullOrBlank() && vid.length == 11) return vid
                for ((_, v) in element) findNestedVideoId(v)?.let { return it }
            }
            is JsonArray -> for (item in element) findNestedVideoId(item)?.let { return it }
            else -> Unit
        }
        return null
    }

    private fun parseDurationToMs(text: String?): Long {
        if (text.isNullOrBlank()) return 0L
        val parts = text.split(":").mapNotNull { it.trim().toLongOrNull() }
        return when (parts.size) {
            2 -> (parts[0] * 60 + parts[1]) * 1000L
            3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000L
            else -> 0L
        }
    }
}
