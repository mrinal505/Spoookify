package com.spoookify.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spoookify.data.local.dao.DownloadDao
import com.spoookify.data.local.dao.DynamicPlaylistDao
import com.spoookify.data.local.dao.FavoriteDao
import com.spoookify.data.local.entity.DynamicPlaylist
import com.spoookify.data.local.entity.FavoriteTrack
import com.spoookify.data.remote.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val downloadDao: DownloadDao,
    private val dynamicPlaylistDao: DynamicPlaylistDao,
    private val musicRepository: com.spoookify.data.repository.MusicRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow("All")
    val selectedTab = _selectedTab.asStateFlow()

    val favoriteTracks: StateFlow<List<Track>> = favoriteDao.getAllFavorites()
        .map { favorites ->
            favorites.map { favorite ->
                Track(
                    id = favorite.id,
                    title = favorite.title,
                    artist = favorite.artist,
                    thumbnailUrl = favorite.thumbnailUrl
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedTracks: StateFlow<List<Track>> = downloadDao.getAllDownloads()
        .map { downloads ->
            downloads.map { download ->
                Track(
                    id = download.id,
                    title = download.title,
                    artist = download.artist,
                    thumbnailUrl = download.thumbnailUrl
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedTrackIds: StateFlow<Set<String>> = downloadedTracks
        .map { tracks -> tracks.map { it.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val dynamicPlaylists: StateFlow<List<DynamicPlaylist>> = dynamicPlaylistDao.getAllDynamicPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedTab(tab: String) {
        _selectedTab.value = tab
    }

    fun createPlaylist(name: String, description: String = "") {
        if (name.isBlank()) return
        viewModelScope.launch {
            val playlist = DynamicPlaylist(
                id = java.util.UUID.randomUUID().toString(),
                name = name.trim(),
                description = if (description.isBlank()) "Custom playlist" else description,
                rulesJson = "{}"
            )
            dynamicPlaylistDao.insertPlaylist(playlist)
        }
    }

    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            val favorite = FavoriteTrack(
                id = track.id,
                title = track.title,
                artist = track.artist,
                thumbnailUrl = track.thumbnailUrl
            )
            val existing = favoriteDao.getFavoriteById(track.id)
            if (existing != null) {
                favoriteDao.deleteFavorite(favorite)
            } else {
                favoriteDao.insertFavorite(favorite)
            }
        }
    }

    fun playTimeMachineYear(year: String, onLoaded: (Track, List<Track>) -> Unit) {
        viewModelScope.launch {
            try {
                val query = "Top Hit Songs of $year"
                val songs = musicRepository.searchSongs(query).take(15)
                if (songs.isNotEmpty()) {
                    onLoaded(songs.first(), songs)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

