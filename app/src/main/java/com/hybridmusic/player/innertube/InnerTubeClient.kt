package com.hybridmusic.player.innertube

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class InnerTubeClient(
    private val httpClient: OkHttpClient
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val streamCache = ConcurrentHashMap<String, AudioStreamInfo>()

    @Volatile
    private var cachedVisitorData: String? = null

    companion object {
        private const val YTM_SEARCH_URL = "https://music.youtube.com/youtubei/v1/search?prettyPrint=false"
        private const val YTM_BROWSE_URL = "https://music.youtube.com/youtubei/v1/browse?prettyPrint=false"
        private const val YTM_NEXT_URL = "https://music.youtube.com/youtubei/v1/next?prettyPrint=false"
        private const val YTM_SUGGEST_URL = "https://music.youtube.com/youtubei/v1/music/get_search_suggestions?prettyPrint=false"
        private const val YT_PLAYER_URL = "https://www.youtube.com/youtubei/v1/player?prettyPrint=false"
        private const val YT_VISITOR_URL = "https://www.youtube.com/youtubei/v1/visitor_id?prettyPrint=false"

        private const val SONG_FILTER_PARAMS = "EgWKAQIIAWoKEAMQBBAJEAoQBQ%3D%3D"
        private const val PLAYLIST_FILTER_PARAMS = "EgeKAQQoAEABagoQAxAEEAkQChAF"
        private const val ALBUM_FILTER_PARAMS = "EgWKAQIYAWoKEAMQBBAJEAoQBQ%3D%3D"

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

    private fun JsonElement?.asObj(): JsonObject? = this as? JsonObject
    private fun JsonElement?.asArr(): JsonArray? = this as? JsonArray
    private fun JsonElement?.asStr(): String? = (this as? JsonPrimitive)?.contentOrNull
    private fun JsonElement?.asLong(): Long? = (this as? JsonPrimitive)?.longOrNull
    private fun JsonElement?.asInt(): Int? = (this as? JsonPrimitive)?.intOrNull

    suspend fun prewarmConnections() = withContext(Dispatchers.IO) {
        ensureVisitorData()
    }

    private fun ensureVisitorData(): String? {
        cachedVisitorData?.let { return it }
        val payload = buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", "ANDROID_VR")
                    put("clientVersion", "1.71.26")
                    put("osName", "Android")
                    put("osVersion", "14")
                    put("hl", "th")
                    put("gl", "TH")
                }
            }
        }.toString()

        val req = Request.Builder()
            .url(YT_VISITOR_URL)
            .header("User-Agent", VR_USER_AGENT)
            .header("X-YouTube-Client-Name", "28")
            .header("X-YouTube-Client-Version", "1.71.26")
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        return runCatching {
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val root = json.parseToJsonElement(body).asObj() ?: return null
                val vd = root["responseContext"].asObj()?.get("visitorData").asStr()
                if (!vd.isNullOrBlank()) cachedVisitorData = vd
                vd
            }
        }.getOrNull()
    }

    fun getCachedStream(videoId: String): AudioStreamInfo? {
        val cached = streamCache[videoId] ?: return null
        return if (cached.expiresAtMs > System.currentTimeMillis() + 90_000L) cached else null
    }

    private fun postYtmWebRemix(url: String, payload: String): JsonElement? {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", WEB_USER_AGENT)
            .header("Origin", "https://music.youtube.com")
            .header("Referer", "https://music.youtube.com/")
            .header("X-YouTube-Client-Name", "67")
            .header("X-YouTube-Client-Version", "1.20250310.01.00")
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        return runCatching {
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val parsed = json.parseToJsonElement(body)
                if (cachedVisitorData == null) {
                    parsed.asObj()?.get("responseContext").asObj()?.get("visitorData").asStr()?.let {
                        if (it.isNotBlank()) cachedVisitorData = it
                    }
                }
                parsed
            }
        }.getOrNull()
    }

    suspend fun fetchHomeFeed(mood: MoodChip? = null): HomeFeedResult = withContext(Dispatchers.IO) {
        val defaultMoods = listOf(
            MoodChip("ทั้งหมด", ""),
            MoodChip("เพลงไทยฮิต", "เพลงไทยยอดนิยม 2025 2026"),
            MoodChip("สากลฮิต", "Top Global Pop Hits"),
            MoodChip("ฟังชิลล์ๆ", "Thai Chill Acoustic Lo-Fi"),
            MoodChip("ลูกทุ่ง / อินดี้", "ลูกทุ่งอินดี้ฮิต"),
            MoodChip("ออกกำลังกาย", "Workout EDM Phonk Hits")
        )

        if (mood != null && mood.queryOrParams.isNotEmpty() && !mood.isBrowseParams) {
            val songs = searchSongs(mood.queryOrParams)
            val playlists = searchPlaylists(mood.title)
            val shelves = buildList {
                if (songs.isNotEmpty()) add(HomeShelf("แนะนำสำหรับคุณ • ${mood.title}", tracks = songs))
                if (playlists.isNotEmpty()) add(HomeShelf("เพลย์ลิสต์ยอดนิยมในหมวดนี้", playlists = playlists))
            }
            return@withContext HomeFeedResult(
                moods = defaultMoods,
                quickPicks = songs.take(6),
                shelves = shelves
            )
        }

        val liveResult = fetchYtMusicBrowse("FEmusic_home", mood?.queryOrParams)
        if (liveResult != null && liveResult.shelves.size >= 2) {
            val combinedMoods = if (liveResult.moods.isNotEmpty()) {
                listOf(MoodChip("ทั้งหมด", "")) + liveResult.moods.take(6)
            } else {
                defaultMoods
            }
            val qp = if (liveResult.quickPicks.size >= 6) {
                liveResult.quickPicks.take(6)
            } else {
                liveResult.shelves.flatMap { it.tracks }.distinctBy { it.videoId }.take(6)
            }
            return@withContext HomeFeedResult(
                moods = combinedMoods,
                quickPicks = qp,
                shelves = liveResult.shelves
            )
        }

        coroutineScope {
            val topThaiDeferred = async { searchSongs("เพลงไทยยอดนิยม ฮิตติดชาร์ต") }
            val quickPicksDeferred = async { searchSongs("Thai Pop Hits Official Audio") }
            val playlistsDeferred = async { searchPlaylists("Top Hits Thailand Playlist") }
            val globalHitsDeferred = async { searchSongs("Global Top 50 Hits") }

            val topThai = topThaiDeferred.await()
            val quick = quickPicksDeferred.await()
            val topPlaylists = playlistsDeferred.await()
            val globalHits = globalHitsDeferred.await()

            val fallbackShelves = buildList {
                if (quick.isNotEmpty()) add(HomeShelf("เลือกด่วนสำหรับคุณ (Quick Picks)", tracks = quick))
                if (topPlaylists.isNotEmpty()) add(HomeShelf("เพลย์ลิสต์ชาร์ตยอดนิยม", playlists = topPlaylists))
                if (topThai.isNotEmpty()) add(HomeShelf("มาแรงในไทย (Thailand Top Charts)", tracks = topThai))
                if (globalHits.isNotEmpty()) add(HomeShelf("ฮิตระดับโลก (Global Hits)", tracks = globalHits))
            }
            HomeFeedResult(
                moods = defaultMoods,
                quickPicks = (quick + topThai).distinctBy { it.videoId }.take(6),
                shelves = fallbackShelves
            )
        }
    }

    private fun fetchYtMusicBrowse(browseId: String, params: String? = null): HomeFeedResult? {
        val payload = buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", "WEB_REMIX")
                    put("clientVersion", "1.20250310.01.00")
                    put("hl", "th")
                    put("gl", "TH")
                }
            }
            put("browseId", browseId)
            if (!params.isNullOrBlank()) put("params", params)
        }.toString()

        val root = postYtmWebRemix(YTM_BROWSE_URL, payload) ?: return null
        val moods = mutableListOf<MoodChip>()
        val shelves = mutableListOf<HomeShelf>()
        collectMoodChips(root, moods)
        collectHomeShelves(root, shelves)
        val allTracks = shelves.flatMap { it.tracks }.distinctBy { it.videoId }
        return HomeFeedResult(
            moods = moods.distinctBy { it.title },
            quickPicks = allTracks.take(6),
            shelves = shelves
        )
    }

    suspend fun fetchNextContext(videoId: String): NextContextResult = withContext(Dispatchers.IO) {
        val payload = buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", "WEB_REMIX")
                    put("clientVersion", "1.20250310.01.00")
                    put("hl", "th")
                    put("gl", "TH")
                }
            }
            put("videoId", videoId)
            put("playlistId", "RDAMVM$videoId")
            put("isAudioOnly", true)
        }.toString()

        val root = postYtmWebRemix(YTM_NEXT_URL, payload)
            ?: return@withContext NextContextResult(emptyList(), null, null)

        val radioTracks = mutableListOf<TrackItem>()
        val seenIds = mutableSetOf<String>()
        collectPlaylistPanelTracks(root, radioTracks, seenIds)
        if (radioTracks.isEmpty()) {
            collectMusicItems(root, radioTracks, seenIds)
        }

        val browseIds = mutableListOf<String>()
        collectTabBrowseIds(root, browseIds)
        val lyricsId = browseIds.firstOrNull { it.startsWith("MPLYt") || it.contains("lyrics", ignoreCase = true) }
        val relatedId = browseIds.firstOrNull { it.startsWith("MPTRt") || it.contains("related", ignoreCase = true) }

        NextContextResult(
            radioTracks = radioTracks.filter { it.videoId != videoId }.distinctBy { it.videoId },
            lyricsBrowseId = lyricsId,
            relatedBrowseId = relatedId
        )
    }

    suspend fun fetchLyrics(lyricsBrowseId: String?, trackTitle: String, artist: String): LyricsResult =
        withContext(Dispatchers.IO) {
            if (!lyricsBrowseId.isNullOrBlank()) {
                val payload = buildJsonObject {
                    putJsonObject("context") {
                        putJsonObject("client") {
                            put("clientName", "WEB_REMIX")
                            put("clientVersion", "1.20250310.01.00")
                            put("hl", "th")
                            put("gl", "TH")
                        }
                    }
                    put("browseId", lyricsBrowseId)
                }.toString()

                val root = postYtmWebRemix(YTM_BROWSE_URL, payload)
                if (root != null) {
                    val extracted = extractLyricsText(root)
                    if (extracted != null && extracted.lines.isNotEmpty()) {
                        return@withContext extracted
                    }
                }
            }
            LyricsResult(
                lines = listOf(
                    "ไม่พบเนื้อเพลงอย่างเป็นทางการสำหรับเพลงนี้",
                    "",
                    "$trackTitle — $artist"
                ),
                source = "YouTube Music"
            )
        }

    private fun extractLyricsText(root: JsonElement): LyricsResult? {
        var foundLines: List<String>? = null
        var sourceLabel = "เนื้อเพลงจาก YouTube Music"

        fun walk(el: JsonElement) {
            when (el) {
                is JsonObject -> {
                    el["musicDescriptionShelfRenderer"].asObj()?.let { desc ->
                        val runs = desc["description"].asObj()?.get("runs").asArr()
                        val fullText = runs?.mapNotNull { it.asObj()?.get("text").asStr() }?.joinToString("")
                        if (!fullText.isNullOrBlank()) {
                            foundLines = fullText.lines()
                        }
                        val footer = desc["footer"].asObj()?.get("runs").asArr()
                            ?.mapNotNull { it.asObj()?.get("text").asStr() }?.joinToString("")
                        if (!footer.isNullOrBlank()) sourceLabel = footer
                    }
                    for ((_, v) in el) walk(v)
                }
                is JsonArray -> for (item in el) walk(item)
                else -> Unit
            }
        }
        walk(root)
        return foundLines?.let { LyricsResult(lines = it, source = sourceLabel) }
    }

    suspend fun fetchRelatedContent(relatedBrowseId: String?, fallbackArtist: String): RelatedResult =
        withContext(Dispatchers.IO) {
            if (!relatedBrowseId.isNullOrBlank()) {
                val browse = fetchYtMusicBrowse(relatedBrowseId)
                if (browse != null) {
                    val tracks = browse.shelves.flatMap { it.tracks }.distinctBy { it.videoId }
                    val playlists = browse.shelves.flatMap { it.playlists }.distinctBy { it.browseId }
                    if (tracks.isNotEmpty() || playlists.isNotEmpty()) {
                        return@withContext RelatedResult(tracks = tracks, playlists = playlists)
                    }
                }
            }
            val similar = searchSongs(fallbackArtist)
            RelatedResult(tracks = similar, playlists = emptyList())
        }

    suspend fun fetchSearchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        if (query.isBlank() || YtMusicLinkParser.isUrlInput(query)) return@withContext emptyList()
        val payload = buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", "WEB_REMIX")
                    put("clientVersion", "1.20250310.01.00")
                    put("hl", "th")
                    put("gl", "TH")
                }
            }
            put("input", query)
        }.toString()

        val root = postYtmWebRemix(YTM_SUGGEST_URL, payload) ?: return@withContext emptyList()
        val suggestions = mutableListOf<String>()
        fun walk(el: JsonElement) {
            when (el) {
                is JsonObject -> {
                    el["searchSuggestionRenderer"].asObj()?.let { renderer ->
                        val text = renderer["suggestion"].asObj()?.get("runs").asArr()
                            ?.mapNotNull { it.asObj()?.get("text").asStr() }?.joinToString("")
                        if (!text.isNullOrBlank()) suggestions.add(text)
                    }
                    for ((_, v) in el) walk(v)
                }
                is JsonArray -> for (item in el) walk(item)
                else -> Unit
            }
        }
        walk(root)
        suggestions.distinct().take(6)
    }

    suspend fun fetchPlaylistTracks(browseId: String): List<TrackItem> = withContext(Dispatchers.IO) {
        val normalizedId = if (browseId.startsWith("PL") || browseId.startsWith("OLAK5uy_")) {
            "VL$browseId"
        } else {
            browseId
        }
        val payload = buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", "WEB_REMIX")
                    put("clientVersion", "1.20250310.01.00")
                    put("hl", "th")
                    put("gl", "TH")
                }
            }
            put("browseId", normalizedId)
        }.toString()

        val root = postYtmWebRemix(YTM_BROWSE_URL, payload) ?: return@withContext emptyList()
        val results = mutableListOf<TrackItem>()
        collectMusicItems(root, results, mutableSetOf())
        results
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

        val root = postYtmWebRemix(YTM_SEARCH_URL, payload) ?: return@withContext emptyList()
        val results = mutableListOf<TrackItem>()
        collectMusicItems(root, results, mutableSetOf())
        results
    }

    suspend fun searchPlaylists(query: String, albumsOnly: Boolean = false): List<YtPlaylistCard> =
        withContext(Dispatchers.IO) {
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
                put("params", if (albumsOnly) ALBUM_FILTER_PARAMS else PLAYLIST_FILTER_PARAMS)
            }.toString()

            val root = postYtmWebRemix(YTM_SEARCH_URL, payload) ?: return@withContext emptyList()
            val out = mutableListOf<YtPlaylistCard>()
            val seen = mutableSetOf<String>()

            fun walk(el: JsonElement) {
                when (el) {
                    is JsonObject -> {
                        el["musicResponsiveListItemRenderer"].asObj()?.let { item ->
                            val browseId = item["navigationEndpoint"].asObj()
                                ?.get("browseEndpoint").asObj()?.get("browseId").asStr()
                            val flex = item["flexColumns"].asArr()
                            val title = flex?.getOrNull(0).asObj()
                                ?.get("musicResponsiveListItemFlexColumnRenderer").asObj()
                                ?.get("text").asObj()?.get("runs").asArr()
                                ?.firstOrNull().asObj()?.get("text").asStr()
                            val sub = flex?.getOrNull(1).asObj()
                                ?.get("musicResponsiveListItemFlexColumnRenderer").asObj()
                                ?.get("text").asObj()?.get("runs").asArr()
                                ?.mapNotNull { it.asObj()?.get("text").asStr() }?.joinToString("") ?: "YouTube Music"
                            val thumb = item["thumbnail"].asObj()
                                ?.get("musicThumbnailRenderer").asObj()
                                ?.get("thumbnail").asObj()?.get("thumbnails").asArr()
                                ?.lastOrNull().asObj()?.get("url").asStr()
                                ?.replace(Regex("=w\\d+-h\\d+"), "=w544-h544") ?: ""

                            if (!browseId.isNullOrBlank() && !title.isNullOrBlank() && seen.add(browseId)) {
                                out.add(YtPlaylistCard(browseId, title, sub, thumb))
                            }
                        }
                        for ((_, v) in el) walk(v)
                    }
                    is JsonArray -> for (item in el) walk(item)
                    else -> Unit
                }
            }
            walk(root)
            out
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
                val root = json.parseToJsonElement(body).asObj() ?: return null
                val details = root["videoDetails"].asObj() ?: return null
                val title = details["title"].asStr() ?: return null
                val author = details["author"].asStr() ?: "Unknown Artist"
                val lengthSec = details["lengthSeconds"].asLong() ?: 0L
                val thumbs = details["thumbnail"].asObj()?.get("thumbnails").asArr()
                val thumbUrl = thumbs?.lastOrNull().asObj()?.get("url").asStr()
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
                val root = json.parseToJsonElement(body).asObj() ?: return null
                val title = root["title"].asStr() ?: return null
                val author = root["author_name"].asStr() ?: "YT Music"
                TrackItem(
                    videoId = videoId,
                    title = title,
                    artist = author.removeSuffix(" - Topic").trim(),
                    thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                )
            }
        }.getOrNull()
    }

    // ยิงขอสตรีมจาก ANDROID_VR และ IOS แข่งกันพร้อมกัน (Parallel Race) เอาตัวที่ตอบกลับไวที่สุดทันที
    suspend fun getCleanAudioStream(
        videoId: String,
        forceRefresh: Boolean = false,
        preferIos: Boolean = false
    ): AudioStreamInfo? = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            getCachedStream(videoId)?.let { return@withContext it }
        } else {
            streamCache.remove(videoId)
        }

        val startMs = System.currentTimeMillis()
        val stream = if (preferIos) {
            fetchIosAudio(videoId)
        } else {
            raceAudioClients(videoId)
        }

        if (stream != null) {
            val latency = (System.currentTimeMillis() - startMs).coerceAtLeast(1L)
            val measured = stream.copy(fetchLatencyMs = latency)
            streamCache[videoId] = measured
            urlUserAgentMap[measured.url] = measured.userAgent
            measured
        } else {
            null
        }
    }

    private suspend fun raceAudioClients(videoId: String): AudioStreamInfo? = coroutineScope {
        val winner = CompletableDeferred<AudioStreamInfo?>()
        val finished = AtomicInteger(0)

        val jobs = listOf(
            async(Dispatchers.IO) {
                val res = fetchAndroidVrAudio(videoId)
                if (res != null) winner.complete(res)
                if (finished.incrementAndGet() == 2 && !winner.isCompleted) winner.complete(null)
            },
            async(Dispatchers.IO) {
                val res = fetchIosAudio(videoId)
                if (res != null) winner.complete(res)
                if (finished.incrementAndGet() == 2 && !winner.isCompleted) winner.complete(null)
            }
        )

        val chosen = winner.await()
        jobs.forEach { if (it.isActive) it.cancel() }
        chosen
    }

    fun invalidateCache(videoId: String) {
        streamCache.remove(videoId)
    }

    private fun fetchAndroidVrAudio(videoId: String): AudioStreamInfo? {
        val vd = cachedVisitorData
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
                    if (!vd.isNullOrBlank()) put("visitorData", vd)
                }
            }
            put("videoId", videoId)
            put("contentCheckOk", true)
            put("racyCheckOk", true)
        }.toString()
        return parseAudioOnlyStream(payload, VR_USER_AGENT, "28", "1.71.26", vd)
    }

    private fun fetchIosAudio(videoId: String): AudioStreamInfo? {
        val vd = cachedVisitorData
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
                    if (!vd.isNullOrBlank()) put("visitorData", vd)
                }
            }
            put("videoId", videoId)
            put("contentCheckOk", true)
            put("racyCheckOk", true)
        }.toString()
        return parseAudioOnlyStream(payload, IOS_USER_AGENT, "5", "20.10.4", vd)
    }

    private fun parseAudioOnlyStream(
        payload: String,
        userAgent: String,
        clientNameId: String,
        clientVersion: String,
        visitorData: String?
    ): AudioStreamInfo? {
        val builder = Request.Builder()
            .url(YT_PLAYER_URL)
            .header("User-Agent", userAgent)
            .header("X-YouTube-Client-Name", clientNameId)
            .header("X-YouTube-Client-Version", clientVersion)
            .header("X-Goog-Api-Format-Version", "2")
        if (!visitorData.isNullOrBlank()) {
            builder.header("X-Goog-Visitor-Id", visitorData)
        }
        val req = builder
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        return runCatching {
            httpClient.newCall(req).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val root = json.parseToJsonElement(body).asObj() ?: return null
                val status = root["playabilityStatus"].asObj()?.get("status").asStr()
                if (status != null && status != "OK") return null

                val streamingData = root["streamingData"].asObj() ?: return null
                val expiresIn = streamingData["expiresInSeconds"].asLong() ?: 3600L
                val formats = streamingData["adaptiveFormats"].asArr() ?: return null

                // เลือกสตรีมเสียง AAC/M4A ระดับ ~128-160kbps เป็นหลักเพื่อเริ่มเล่นไวสุดและเสียงใส
                var standardMp4: AudioStreamInfo? = null
                var fallbackMp4: AudioStreamInfo? = null
                var bestAny: AudioStreamInfo? = null

                for (el in formats) {
                    val obj = el.asObj() ?: continue
                    val mime = obj["mimeType"].asStr() ?: continue
                    if (!mime.startsWith("audio/")) continue
                    val rawUrl = obj["url"].asStr() ?: continue
                    if (rawUrl.isBlank()) continue
                    val url = appendCpnIfMissing(rawUrl)
                    val bitrate = obj["bitrate"].asInt() ?: 0

                    val info = AudioStreamInfo(
                        url = url,
                        mimeType = mime,
                        bitrate = bitrate,
                        userAgent = userAgent,
                        expiresAtMs = System.currentTimeMillis() + expiresIn * 1000L
                    )
                    if (mime.startsWith("audio/mp4")) {
                        if (bitrate in 96_000..170_000 && (standardMp4 == null || bitrate > standardMp4.bitrate)) {
                            standardMp4 = info
                        }
                        if (fallbackMp4 == null || bitrate > fallbackMp4.bitrate) {
                            fallbackMp4 = info
                        }
                    }
                    if (bestAny == null || bitrate > bestAny.bitrate) {
                        bestAny = info
                    }
                }
                standardMp4 ?: fallbackMp4 ?: bestAny
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

    private fun collectTabBrowseIds(element: JsonElement, out: MutableList<String>) {
        when (element) {
            is JsonObject -> {
                element["tabRenderer"].asObj()?.let { tab ->
                    val bId = tab["endpoint"].asObj()?.get("browseEndpoint").asObj()?.get("browseId").asStr()
                    if (!bId.isNullOrBlank()) out.add(bId)
                }
                for ((_, v) in element) collectTabBrowseIds(v, out)
            }
            is JsonArray -> for (item in element) collectTabBrowseIds(item, out)
            else -> Unit
        }
    }

    private fun collectPlaylistPanelTracks(element: JsonElement, out: MutableList<TrackItem>, seenIds: MutableSet<String>) {
        when (element) {
            is JsonObject -> {
                element["playlistPanelVideoRenderer"].asObj()?.let { item ->
                    val vid = item["videoId"].asStr()
                    val title = item["title"].asObj()?.get("runs").asArr()?.firstOrNull().asObj()?.get("text").asStr()
                    val artist = item["longBylineText"].asObj()?.get("runs").asArr()?.firstOrNull().asObj()?.get("text").asStr()
                        ?: item["shortBylineText"].asObj()?.get("runs").asArr()?.firstOrNull().asObj()?.get("text").asStr()
                        ?: "YouTube Music"
                    val durText = item["lengthText"].asObj()?.get("runs").asArr()?.firstOrNull().asObj()?.get("text").asStr()
                    val thumb = item["thumbnail"].asObj()?.get("thumbnails").asArr()?.lastOrNull().asObj()?.get("url").asStr()
                        ?.replace(Regex("=w\\d+-h\\d+"), "=w544-h544")
                        ?: "https://i.ytimg.com/vi/$vid/hqdefault.jpg"
                    if (!vid.isNullOrBlank() && !title.isNullOrBlank() && seenIds.add(vid)) {
                        out.add(TrackItem(vid, title, artist, parseDurationToMs(durText), thumb))
                    }
                }
                for ((_, v) in element) collectPlaylistPanelTracks(v, out, seenIds)
            }
            is JsonArray -> for (item in element) collectPlaylistPanelTracks(item, out, seenIds)
            else -> Unit
        }
    }

    private fun collectMoodChips(element: JsonElement, out: MutableList<MoodChip>) {
        when (element) {
            is JsonObject -> {
                element["chipCloudChipRenderer"].asObj()?.let { chip ->
                    val title = chip["text"].asObj()?.get("runs").asArr()
                        ?.firstOrNull().asObj()?.get("text").asStr()
                    val params = chip["navigationEndpoint"].asObj()
                        ?.get("browseEndpoint").asObj()?.get("params").asStr()
                    if (!title.isNullOrBlank() && !params.isNullOrBlank()) {
                        out.add(MoodChip(title = title, queryOrParams = params, isBrowseParams = true))
                    }
                }
                for ((_, v) in element) collectMoodChips(v, out)
            }
            is JsonArray -> for (item in element) collectMoodChips(item, out)
            else -> Unit
        }
    }

    private fun collectHomeShelves(element: JsonElement, out: MutableList<HomeShelf>) {
        when (element) {
            is JsonObject -> {
                element["musicCarouselShelfRenderer"].asObj()?.let { carousel ->
                    val headerTitle = carousel["header"].asObj()
                        ?.get("musicCarouselShelfBasicHeaderRenderer").asObj()
                        ?.get("title").asObj()?.get("runs").asArr()
                        ?.firstOrNull().asObj()?.get("text").asStr()
                        ?: "แนะนำสำหรับคุณ"

                    val tracks = mutableListOf<TrackItem>()
                    val playlists = mutableListOf<YtPlaylistCard>()
                    val contents = carousel["contents"].asArr() ?: JsonArray(emptyList())
                    for (c in contents) {
                        val cObj = c.asObj() ?: continue
                        cObj["musicResponsiveListItemRenderer"].asObj()?.let { item ->
                            parseMusicListItem(item)?.let { tracks.add(it) }
                        }
                        cObj["musicTwoRowItemRenderer"].asObj()?.let { twoRow ->
                            parseTwoRowItem(twoRow, tracks, playlists)
                        }
                    }
                    if (tracks.isNotEmpty() || playlists.isNotEmpty()) {
                        out.add(
                            HomeShelf(
                                title = headerTitle,
                                tracks = tracks.distinctBy { it.videoId },
                                playlists = playlists.distinctBy { it.browseId }
                            )
                        )
                    }
                }
                for ((_, v) in element) collectHomeShelves(v, out)
            }
            is JsonArray -> for (item in element) collectHomeShelves(item, out)
            else -> Unit
        }
    }

    private fun parseTwoRowItem(
        obj: JsonObject,
        tracksOut: MutableList<TrackItem>,
        playlistsOut: MutableList<YtPlaylistCard>
    ) {
        val title = obj["title"].asObj()?.get("runs").asArr()
            ?.firstOrNull().asObj()?.get("text").asStr() ?: return

        val subtitle = obj["subtitle"].asObj()?.get("runs").asArr()
            ?.mapNotNull { it.asObj()?.get("text").asStr() }
            ?.joinToString("")?.trim().orEmpty().ifEmpty { "YouTube Music" }

        val thumbs = obj["thumbnailRenderer"].asObj()
            ?.get("musicThumbnailRenderer").asObj()
            ?.get("thumbnail").asObj()?.get("thumbnails").asArr()
        val thumbUrl = thumbs?.lastOrNull().asObj()?.get("url").asStr()
            ?.replace(Regex("=w\\d+-h\\d+"), "=w544-h544") ?: ""

        val nav = obj["navigationEndpoint"].asObj()
        val watchVideoId = nav?.get("watchEndpoint").asObj()?.get("videoId").asStr()
        if (!watchVideoId.isNullOrBlank()) {
            tracksOut.add(
                TrackItem(
                    videoId = watchVideoId,
                    title = title,
                    artist = subtitle,
                    thumbnailUrl = thumbUrl.ifEmpty { "https://i.ytimg.com/vi/$watchVideoId/hqdefault.jpg" }
                )
            )
            return
        }

        val browseId = nav?.get("browseEndpoint").asObj()?.get("browseId").asStr()
        if (!browseId.isNullOrBlank() && thumbUrl.isNotEmpty()) {
            playlistsOut.add(YtPlaylistCard(browseId, title, subtitle, thumbUrl))
        }
    }

    private fun collectMusicItems(element: JsonElement, out: MutableList<TrackItem>, seenIds: MutableSet<String>) {
        when (element) {
            is JsonObject -> {
                element["musicResponsiveListItemRenderer"].asObj()?.let { renderer ->
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
        val videoId = renderer["playlistItemData"].asObj()?.get("videoId").asStr()
            ?: findNestedVideoId(renderer) ?: return null
        val flexColumns = renderer["flexColumns"].asArr() ?: return null
        val title = flexColumns.getOrNull(0).asObj()
            ?.get("musicResponsiveListItemFlexColumnRenderer").asObj()
            ?.get("text").asObj()?.get("runs").asArr()
            ?.firstOrNull().asObj()?.get("text").asStr() ?: return null

        val secondaryRuns = flexColumns.getOrNull(1).asObj()
            ?.get("musicResponsiveListItemFlexColumnRenderer").asObj()
            ?.get("text").asObj()?.get("runs").asArr()

        val subtitleParts = secondaryRuns?.mapNotNull {
            it.asObj()?.get("text").asStr()?.trim()
        }?.filter { it.isNotEmpty() && it != "•" && it != "เพลง" && it != "Song" }.orEmpty()

        val artist = subtitleParts.firstOrNull() ?: "Unknown Artist"
        val durationMs = parseDurationToMs(subtitleParts.lastOrNull { it.contains(":") })
        val thumbnails = renderer["thumbnail"].asObj()
            ?.get("musicThumbnailRenderer").asObj()
            ?.get("thumbnail").asObj()?.get("thumbnails").asArr()
        val rawThumb = thumbnails?.lastOrNull().asObj()?.get("url").asStr()
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
                val vid = element["videoId"].asStr()
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
