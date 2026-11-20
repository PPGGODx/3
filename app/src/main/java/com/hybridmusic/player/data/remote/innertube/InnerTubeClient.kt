package com.hybridmusic.player.data.remote.innertube

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
        private const val YTM_SEARCH_URL =
            "https://music.youtube.com/youtubei/v1/search?prettyPrint=false"
        private const val YT_PLAYER_URL =
            "https://www.youtube.com/youtubei/v1/player?prettyPrint=false"
        private const val SONG_FILTER_PARAMS = "EgWKAQIIAWoKEAMQBBAJEAoQBQ%3D%3D"

        private val urlUserAgentMap = ConcurrentHashMap<String, String>()

        const val DEFAULT_VR_USER_AGENT =
            "com.google.android.apps.youtube.vr.oculus/1.71.26 (Linux; U; Android 14; eureka-user Build/UP1A.231005.007.A1) gzip"

        private const val CREATOR_USER_AGENT =
            "com.google.android.apps.youtube.creator/24.47.100 (Linux; U; Android 14; Pixel 8 Build/UP1A.231005.007) gzip"

        private const val TESTSUITE_USER_AGENT =
            "com.google.android.youtube/1.9 (Linux; U; Android 11; eng.build) gzip"

        private const val TV_USER_AGENT =
            "Mozilla/5.0 (PlayStation; PlayStation 5/9.00) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Safari/605.1.15"

        private const val WEB_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36"

        fun resolveUserAgentForUrl(url: String): String {
            urlUserAgentMap[url]?.let { return it }
            val cParam = Regex("[?&]c=([^&]+)").find(url)?.groupValues?.getOrNull(1) ?: ""
            return when {
                cParam.contains("ANDROID_VR", ignoreCase = true) -> DEFAULT_VR_USER_AGENT
                cParam.contains("ANDROID_CREATOR", ignoreCase = true) -> CREATOR_USER_AGENT
                cParam.contains("ANDROID_TESTSUITE", ignoreCase = true) -> TESTSUITE_USER_AGENT
                cParam.contains("TVHTML5", ignoreCase = true) -> TV_USER_AGENT
                cParam.contains("WEB", ignoreCase = true) -> WEB_USER_AGENT
                else -> DEFAULT_VR_USER_AGENT
            }
        }
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
                val root = json.parseToJsonElement(body)
                extractTracksFromSearch(root)
            }
        }.getOrDefault(emptyList())
    }

    suspend fun getAudioStream(videoId: String, forceRefresh: Boolean = false): AudioStreamInfo? =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            if (!forceRefresh) {
                streamCache[videoId]?.let { cached ->
                    if (cached.expiresAtMs > now + 90_000L) {
                        return@withContext cached
                    }
                }
            } else {
                streamCache.remove(videoId)
            }

            val clients = listOf(
                { fetchAndroidVrStream(videoId) },
                { fetchAndroidCreatorStream(videoId) },
                { fetchAndroidTestsuiteStream(videoId) },
                { fetchTvHtml5Stream(videoId) },
                { fetchWebEmbeddedStream(videoId) }
            )

            for (fetcher in clients) {
                val candidate = fetcher() ?: continue
                if (verifyStreamReachable(candidate)) {
                    cacheStream(videoId, candidate)
                    return@withContext candidate
                }
            }
            null
        }

    fun invalidateStreamCache(videoId: String) {
        streamCache.remove(videoId)
    }

    private fun cacheStream(videoId: String, stream: AudioStreamInfo) {
        streamCache[videoId] = stream
        urlUserAgentMap[stream.url] = stream.userAgent
    }

    private fun verifyStreamReachable(stream: AudioStreamInfo): Boolean {
        return runCatching {
            val reqBuilder = Request.Builder()
                .url(stream.url)
                .header("User-Agent", stream.userAgent)
                .header("Range", "bytes=0-1023")
                .get()

            if (stream.url.contains("c=WEB", ignoreCase = true) ||
                stream.url.contains("c=TVHTML5", ignoreCase = true)
            ) {
                reqBuilder
                    .header("Origin", "https://www.youtube.com")
                    .header("Referer", "https://www.youtube.com/")
            }

            httpClient.newCall(reqBuilder.build()).execute().use { resp ->
                if (resp.code != 200 && resp.code != 206) return@runCatching false
                val firstByte = resp.body?.byteStream()?.read() ?: -1
                firstByte != -1
            }
        }.getOrDefault(false)
    }

    private fun fetchAndroidVrStream(videoId: String): AudioStreamInfo? {
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

        return executePlayerRequest(
            payload = payload,
            userAgent = DEFAULT_VR_USER_AGENT,
            clientIdHeader = "28",
            clientVersion = "1.71.26"
        )
    }

    private fun fetchAndroidCreatorStream(videoId: String): AudioStreamInfo? {
        val payload = buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", "ANDROID_CREATOR")
                    put("clientVersion", "24.47.100")
                    put("deviceMake", "Google")
                    put("deviceModel", "Pixel 8")
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

        return executePlayerRequest(
            payload = payload,
            userAgent = CREATOR_USER_AGENT,
            clientIdHeader = "14",
            clientVersion = "24.47.100"
        )
    }

    private fun fetchAndroidTestsuiteStream(videoId: String): AudioStreamInfo? {
        val payload = buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", "ANDROID_TESTSUITE")
                    put("clientVersion", "1.9")
                    put("osName", "Android")
                    put("osVersion", "11")
                    put("androidSdkVersion", 30)
                    put("hl", "th")
                    put("gl", "TH")
                }
            }
            put("videoId", videoId)
            put("contentCheckOk", true)
            put("racyCheckOk", true)
        }.toString()

        return executePlayerRequest(
            payload = payload,
            userAgent = TESTSUITE_USER_AGENT,
            clientIdHeader = "30",
            clientVersion = "1.9"
        )
    }

    private fun fetchTvHtml5Stream(videoId: String): AudioStreamInfo? {
        val payload = buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", "TVHTML5")
                    put("clientVersion", "7.20250312.16.00")
                    put("hl", "th")
                    put("gl", "TH")
                }
            }
            put("videoId", videoId)
            put("contentCheckOk", true)
            put("racyCheckOk", true)
        }.toString()

        return executePlayerRequest(
            payload = payload,
            userAgent = TV_USER_AGENT,
            clientIdHeader = "7",
            clientVersion = "7.20250312.16.00",
            origin = "https://www.youtube.com"
        )
    }

    private fun fetchWebEmbeddedStream(videoId: String): AudioStreamInfo? {
        val payload = buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", "WEB_EMBEDDED_PLAYER")
                    put("clientVersion", "1.20250310.01.00")
                    put("hl", "th")
                    put("gl", "TH")
                }
                putJsonObject("thirdParty") {
                    put("embedUrl", "https://www.youtube.com/")
                }
            }
            put("videoId", videoId)
            put("contentCheckOk", true)
            put("racyCheckOk", true)
        }.toString()

        return executePlayerRequest(
            payload = payload,
            userAgent = WEB_USER_AGENT,
            clientIdHeader = "56",
            clientVersion = "1.20250310.01.00",
            origin = "https://www.youtube.com"
        )
    }

    private fun appendCpnIfMissing(rawUrl: String): String {
        if (!rawUrl.contains("googlevideo.com") || rawUrl.contains("cpn=")) return rawUrl
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
        val cpn = (1..16).map { chars.random() }.joinToString("")
        val sep = if (rawUrl.contains("?")) "&" else "?"
        return "$rawUrl${sep}cpn=$cpn"
    }

    private fun executePlayerRequest(
        payload: String,
        userAgent: String,
        clientIdHeader: String,
        clientVersion: String,
        origin: String? = null
    ): AudioStreamInfo? {
        val reqBuilder = Request.Builder()
            .url(YT_PLAYER_URL)
            .header("User-Agent", userAgent)
            .header("X-YouTube-Client-Name", clientIdHeader)
            .header("X-YouTube-Client-Version", clientVersion)
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))

        if (!origin.isNullOrBlank()) {
            reqBuilder.header("Origin", origin)
            reqBuilder.header("Referer", "$origin/")
        }

        return runCatching {
            httpClient.newCall(reqBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val root = json.parseToJsonElement(body).jsonObject

                val status = root["playabilityStatus"]?.jsonObject
                    ?.get("status")?.jsonPrimitive?.contentOrNull
                if (status != null && status != "OK") return null

                val streamingData = root["streamingData"]?.jsonObject ?: return null
                val expiresInSec = streamingData["expiresInSeconds"]?.jsonPrimitive?.longOrNull ?: 3600L

                val adaptiveFormats = streamingData["adaptiveFormats"]?.jsonArray
                if (adaptiveFormats != null) {
                    var bestMp4Audio: AudioStreamInfo? = null
                    var bestAnyAudio: AudioStreamInfo? = null

                    for (item in adaptiveFormats) {
                        val obj = item.jsonObject
                        val mimeType = obj["mimeType"]?.jsonPrimitive?.contentOrNull ?: continue
                        if (!mimeType.startsWith("audio/")) continue
                        val rawUrl = obj["url"]?.jsonPrimitive?.contentOrNull ?: continue
                        if (rawUrl.isBlank()) continue
                        val url = appendCpnIfMissing(rawUrl)
                        val bitrate = obj["bitrate"]?.jsonPrimitive?.intOrNull ?: 0
                        val contentLength = obj["contentLength"]?.jsonPrimitive?.longOrNull ?: 0L

                        val candidate = AudioStreamInfo(
                            url = url,
                            mimeType = mimeType,
                            bitrate = bitrate,
                            contentLength = contentLength,
                            userAgent = userAgent,
                            expiresAtMs = System.currentTimeMillis() + (expiresInSec * 1000L)
                        )

                        if (mimeType.startsWith("audio/mp4")) {
                            if (bestMp4Audio == null || bitrate > bestMp4Audio.bitrate) {
                                bestMp4Audio = candidate
                            }
                        }
                        if (bestAnyAudio == null || bitrate > bestAnyAudio.bitrate) {
                            bestAnyAudio = candidate
                        }
                    }
                    if (bestMp4Audio != null || bestAnyAudio != null) {
                        return bestMp4Audio ?: bestAnyAudio
                    }
                }

                // Fallback to muxed formats if adaptiveFormats had no direct url
                val formats = streamingData["formats"]?.jsonArray ?: return null
                var bestMuxed: AudioStreamInfo? = null
                for (item in formats) {
                    val obj = item.jsonObject
                    val mimeType = obj["mimeType"]?.jsonPrimitive?.contentOrNull ?: continue
                    val rawUrl = obj["url"]?.jsonPrimitive?.contentOrNull ?: continue
                    if (rawUrl.isBlank()) continue
                    val url = appendCpnIfMissing(rawUrl)
                    val bitrate = obj["bitrate"]?.jsonPrimitive?.intOrNull ?: 0
                    val contentLength = obj["contentLength"]?.jsonPrimitive?.longOrNull ?: 0L
                    if (bestMuxed == null || bitrate > bestMuxed.bitrate) {
                        bestMuxed = AudioStreamInfo(
                            url = url,
                            mimeType = mimeType,
                            bitrate = bitrate,
                            contentLength = contentLength,
                            userAgent = userAgent,
                            expiresAtMs = System.currentTimeMillis() + (expiresInSec * 1000L)
                        )
                    }
                }
                bestMuxed
            }
        }.getOrNull()
    }

    private fun extractTracksFromSearch(root: JsonElement): List<TrackItem> {
        val results = mutableListOf<TrackItem>()
        val seenIds = mutableSetOf<String>()
        collectMusicItems(root, results, seenIds)
        return results
    }

    private fun collectMusicItems(
        element: JsonElement,
        out: MutableList<TrackItem>,
        seenIds: MutableSet<String>
    ) {
        when (element) {
            is JsonObject -> {
                val renderer = element["musicResponsiveListItemRenderer"]?.jsonObject
                if (renderer != null) {
                    parseMusicListItem(renderer)?.let { track ->
                        if (seenIds.add(track.videoId)) {
                            out.add(track)
                        }
                    }
                }
                for ((_, value) in element) {
                    collectMusicItems(value, out, seenIds)
                }
            }
            is JsonArray -> {
                for (item in element) {
                    collectMusicItems(item, out, seenIds)
                }
            }
            else -> Unit
        }
    }

    private fun parseMusicListItem(renderer: JsonObject): TrackItem? {
        val videoId = renderer["playlistItemData"]?.jsonObject
            ?.get("videoId")?.jsonPrimitive?.contentOrNull
            ?: findNestedVideoId(renderer)
            ?: return null

        val flexColumns = renderer["flexColumns"]?.jsonArray ?: return null
        val title = flexColumns.getOrNull(0)?.jsonObject
            ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
            ?.get("text")?.jsonObject
            ?.get("runs")?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("text")?.jsonPrimitive?.contentOrNull
            ?: return null

        val secondaryRuns = flexColumns.getOrNull(1)?.jsonObject
            ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
            ?.get("text")?.jsonObject
            ?.get("runs")?.jsonArray

        val subtitleParts = secondaryRuns?.mapNotNull {
            it.jsonObject["text"]?.jsonPrimitive?.contentOrNull?.trim()
        }?.filter { it.isNotEmpty() && it != "•" && it != "เพลง" && it != "Song" }.orEmpty()

        val artist = subtitleParts.firstOrNull() ?: "Unknown Artist"
        val durationText = subtitleParts.lastOrNull { it.contains(":") }
        val durationMs = parseDurationToMs(durationText)

        val thumbnails = renderer["thumbnail"]?.jsonObject
            ?.get("musicThumbnailRenderer")?.jsonObject
            ?.get("thumbnail")?.jsonObject
            ?.get("thumbnails")?.jsonArray

        val rawThumb = thumbnails?.lastOrNull()?.jsonObject
            ?.get("url")?.jsonPrimitive?.contentOrNull
            ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

        val highResThumb = rawThumb.replace(Regex("=w\\d+-h\\d+"), "=w544-h544")

        return TrackItem(
            videoId = videoId,
            title = title,
            artist = artist,
            durationMs = durationMs,
            thumbnailUrl = highResThumb
        )
    }

    private fun findNestedVideoId(element: JsonElement): String? {
        when (element) {
            is JsonObject -> {
                val vid = (element["videoId"] as? JsonPrimitive)?.contentOrNull
                if (!vid.isNullOrBlank() && vid.length == 11) return vid
                for ((_, v) in element) {
                    val found = findNestedVideoId(v)
                    if (found != null) return found
                }
            }
            is JsonArray -> {
                for (item in element) {
                    val found = findNestedVideoId(item)
                    if (found != null) return found
                }
            }
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
