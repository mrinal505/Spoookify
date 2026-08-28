package com.spoookify.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spoookify.data.local.dao.DownloadDao
import com.spoookify.data.remote.Track
import com.spoookify.data.repository.MusicRepository
import com.spoookify.playback.OfflineManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val offlineManager: OfflineManager,
    private val downloadDao: DownloadDao,
    private val analyticsDao: com.spoookify.data.local.dao.AnalyticsDao,
    private val favoriteDao: com.spoookify.data.local.dao.FavoriteDao,
    private val authManager: com.spoookify.auth.AuthManager
) : ViewModel() {

    private val _searchResults = MutableStateFlow<List<Track>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    val downloadedTrackIds: StateFlow<Set<String>> = downloadDao.getAllDownloads()
        .map { downloads -> downloads.map { it.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val downloadProgress = offlineManager.downloadProgress

    val recentlyPlayedAndSearchedTracks: StateFlow<List<Track>> = combine(
        favoriteDao.getAllFavorites(),
        authManager.currentUser
    ) { favs, user ->
        if (!user.isSignedIn) {
            _recentSearches.value = emptyList()
            emptyList()
        } else {
            val favTracks = favs.map { Track(it.id, it.title, it.artist, it.thumbnailUrl) }
            val topTracks = try {
                analyticsDao.getTopTracks().map { Track(it.trackId, it.trackTitle, it.artist, it.thumbnailUrl) }
            } catch (e: Throwable) { emptyList() }
            (topTracks + favTracks).distinctBy { it.id }.take(15)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches = _recentSearches.asStateFlow()

    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter = _selectedFilter.asStateFlow()

    private var searchJob: Job? = null
    private var lastQuery: String = ""

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun search(query: String) {
        lastQuery = query
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        // 0ms Instant Local Search Filtering
        val localPool = recentlyPlayedAndSearchedTracks.value
        val localMatches = localPool.filter {
            it.title.contains(trimmed, ignoreCase = true) || it.artist.contains(trimmed, ignoreCase = true)
        }
        if (localMatches.isNotEmpty()) {
            _searchResults.value = localMatches
            _isSearching.value = false
        } else {
            _isSearching.value = true
        }

        if (trimmed.length >= 3 && !_recentSearches.value.contains(trimmed)) {
            val current = _recentSearches.value.toMutableList()
            current.add(0, trimmed)
            _recentSearches.value = current.take(8)
        }

        searchJob = viewModelScope.launch {
            delay(150)
            try {
                val remote = musicRepository.searchSongs(trimmed)
                if (remote.isNotEmpty()) {
                    val combined = (localMatches + remote).distinctBy { it.id }
                    _searchResults.value = combined
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun removeRecentSearch(term: String) {
        _recentSearches.value = _recentSearches.value.filter { it != term }
    }

    fun clearAllRecentSearches() {
        _recentSearches.value = emptyList()
    }

    fun loadMore() {
        if (_isSearching.value || lastQuery.isBlank()) return

        viewModelScope.launch {
            _isSearching.value = true
            try {
                val moreResults = musicRepository.searchSongs(lastQuery, isLoadMore = true)
                if (moreResults.isNotEmpty()) {
                    val currentList = _searchResults.value.toMutableList()
                    val newItems = moreResults.filter { newItem -> currentList.none { it.id == newItem.id } }
                    _searchResults.value = currentList + newItems
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun downloadTrack(track: Track) {
        offlineManager.downloadTrack(track)
    }
}

