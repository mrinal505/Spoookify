package com.spoookify.playback

import com.spoookify.data.local.dao.DynamicPlaylistDao
import com.spoookify.data.local.dao.FavoriteDao
import com.spoookify.data.local.entity.DynamicPlaylist
import com.spoookify.data.remote.Track
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistManager @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val dynamicPlaylistDao: DynamicPlaylistDao,
    private val analyticsDao: com.spoookify.data.local.dao.AnalyticsDao,
    private val musicRepository: com.spoookify.data.repository.MusicRepository
) {
    suspend fun getFavoritesPlaylist(): List<Track> {
        val favorites = favoriteDao.getAllFavorites().first()
        // Definition: songs played 3+ times OR explicitly favorited
        val topArtists = analyticsDao.getTopArtists()
        return favorites.filter { it.interactionLevel >= 2 }.map {
            Track(it.id, it.title, it.artist, it.thumbnailUrl)
        }
    }

    suspend fun getNightDrivePlaylist(): List<Track> {
        // Real logic: Fetch "Chill" music based on top artists
        val topArtists = analyticsDao.getTopArtists()
        val seed = topArtists.firstOrNull()?.artist ?: "Chill Lo-fi"
        return musicRepository.searchSongs("$seed chill mix").take(15)
    }

    suspend fun refreshDynamicPlaylists() {
        createInitialDynamicPlaylists()
        // In a more complex app, this would update the cache/Room for these playlists
    }

    private suspend fun createInitialDynamicPlaylists() {
        val current = dynamicPlaylistDao.getAllDynamicPlaylists().first()
        if (current.isEmpty()) {
            dynamicPlaylistDao.insertPlaylist(
                DynamicPlaylist("favorites_hot", "🔥 My Current Favorites", "Based on your most played tracks", "{}")
            )
            dynamicPlaylistDao.insertPlaylist(
                DynamicPlaylist("night_drive", "🌙 Night Drive", "Automatically tuned for late night", "{}")
            )
        }
    }
}
