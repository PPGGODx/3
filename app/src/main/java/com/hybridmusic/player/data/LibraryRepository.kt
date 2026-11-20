package com.hybridmusic.player.data

import android.content.Context
import com.hybridmusic.player.innertube.TrackItem
import com.hybridmusic.player.innertube.UserPlaylist
import com.hybridmusic.player.innertube.YtPlaylistCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class LibraryRepository(context: Context) {
    private val prefs = context.getSharedPreferences("playzongx_library_v3", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _likedTracks = MutableStateFlow<List<TrackItem>>(loadList("liked_tracks"))
    val likedTracks: StateFlow<List<TrackItem>> = _likedTracks.asStateFlow()

    private val _savedYtPlaylists = MutableStateFlow<List<YtPlaylistCard>>(loadList("saved_yt_playlists"))
    val savedYtPlaylists: StateFlow<List<YtPlaylistCard>> = _savedYtPlaylists.asStateFlow()

    private val _userPlaylists = MutableStateFlow<List<UserPlaylist>>(loadList("user_playlists"))
    val userPlaylists: StateFlow<List<UserPlaylist>> = _userPlaylists.asStateFlow()

    private val _recentTracks = MutableStateFlow<List<TrackItem>>(loadList("recent_tracks"))
    val recentTracks: StateFlow<List<TrackItem>> = _recentTracks.asStateFlow()

    private inline fun <reified T> loadList(key: String): List<T> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<T>>(raw) }.getOrDefault(emptyList())
    }

    private inline fun <reified T> saveList(key: String, list: List<T>) {
        prefs.edit().putString(key, json.encodeToString(list)).apply()
    }

    fun isLiked(videoId: String): Boolean = _likedTracks.value.any { it.videoId == videoId }

    fun toggleLike(track: TrackItem): Boolean {
        val current = _likedTracks.value
        val nowLiked = if (current.any { it.videoId == track.videoId }) {
            val updated = current.filter { it.videoId != track.videoId }
            _likedTracks.value = updated
            saveList("liked_tracks", updated)
            false
        } else {
            val updated = listOf(track) + current
            _likedTracks.value = updated
            saveList("liked_tracks", updated)
            true
        }
        return nowLiked
    }

    fun recordRecent(track: TrackItem) {
        val updated = (listOf(track) + _recentTracks.value.filter { it.videoId != track.videoId }).take(40)
        _recentTracks.value = updated
        saveList("recent_tracks", updated)
    }

    fun toggleSaveYtPlaylist(card: YtPlaylistCard): Boolean {
        val current = _savedYtPlaylists.value
        return if (current.any { it.browseId == card.browseId }) {
            val updated = current.filter { it.browseId != card.browseId }
            _savedYtPlaylists.value = updated
            saveList("saved_yt_playlists", updated)
            false
        } else {
            val updated = listOf(card) + current
            _savedYtPlaylists.value = updated
            saveList("saved_yt_playlists", updated)
            true
        }
    }

    fun createUserPlaylist(name: String, initialTrack: TrackItem? = null): UserPlaylist {
        val created = UserPlaylist(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            tracks = listOfNotNull(initialTrack)
        )
        val updated = listOf(created) + _userPlaylists.value
        _userPlaylists.value = updated
        saveList("user_playlists", updated)
        return created
    }

    fun addTrackToUserPlaylist(playlistId: String, track: TrackItem) {
        val updated = _userPlaylists.value.map { pl ->
            if (pl.id == playlistId && pl.tracks.none { it.videoId == track.videoId }) {
                pl.copy(tracks = pl.tracks + track)
            } else {
                pl
            }
        }
        _userPlaylists.value = updated
        saveList("user_playlists", updated)
    }

    fun deleteUserPlaylist(playlistId: String) {
        val updated = _userPlaylists.value.filter { it.id != playlistId }
        _userPlaylists.value = updated
        saveList("user_playlists", updated)
    }
}
