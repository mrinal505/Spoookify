package com.spoookify.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spoookify.data.local.dao.FavoriteDao
import com.spoookify.data.local.dao.DownloadDao
import com.spoookify.data.remote.Track
import com.spoookify.data.repository.MusicRepository
import com.spoookify.data.repository.UserAnalyticsRepository
import com.spoookify.playback.OfflineManager
import com.spoookify.playback.PlaylistManager
import com.spoookify.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val analyticsRepository: UserAnalyticsRepository,
    private val offlineManager: OfflineManager,
    private val favoriteDao: FavoriteDao,
    private val playlistManager: PlaylistManager,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val downloadDao: DownloadDao
) : ViewModel() {

    private val _rediscoverTracks = MutableStateFlow<List<Track>>(emptyList())
    val rediscoverTracks = _rediscoverTracks.asStateFlow()

    private val _recentlyPlayed = MutableStateFlow<List<Track>>(emptyList())
    val recentlyPlayed = _recentlyPlayed.asStateFlow()

    private val _trendingTracks = MutableStateFlow<List<Track>>(emptyList())
    val trendingTracks = _trendingTracks.asStateFlow()

    private val _recommendedTracks = MutableStateFlow<List<Track>>(emptyList())
    val recommendedTracks = _recommendedTracks.asStateFlow()

    private val _featuredHeroTrack = MutableStateFlow<Track?>(null)
    val featuredHeroTrack = _featuredHeroTrack.asStateFlow()

    private val _isHomeLoading = MutableStateFlow(true)
    val isHomeLoading = _isHomeLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _homeModules = MutableStateFlow<List<HomeModule>>(emptyList())
    val homeModules = _homeModules.asStateFlow()

    val downloadedTrackIds: StateFlow<Set<String>> = downloadDao.getAllDownloads()
        .map { downloads -> downloads.map { it.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    init {
        val starters = getStarterTracks()
        _trendingTracks.value = starters
        _recommendedTracks.value = starters.shuffled()
        _recentlyPlayed.value = starters.take(6).shuffled()
        _rediscoverTracks.value = starters.takeLast(6).shuffled()
        _featuredHeroTrack.value = starters.random()
        _isHomeLoading.value = false

        loadHomeConfig()
        refreshHomeData()
    }

    private fun getStarterTracks(): List<Track> {
        val pool = listOf(
            Track("fHI8X4OXluQ", "Blinding Lights", "The Weeknd", "https://i.ytimg.com/vi/fHI8X4OXluQ/hqdefault.jpg"),
            Track("JGwWNGJdvx8", "Shape of You", "Ed Sheeran", "https://i.ytimg.com/vi/JGwWNGJdvx8/hqdefault.jpg"),
            Track("34Na4j8AVgA", "Starboy", "The Weeknd ft. Daft Punk", "https://i.ytimg.com/vi/34Na4j8AVgA/hqdefault.jpg"),
            Track("TUVcZfQe-Kw", "Levitating", "Dua Lipa", "https://i.ytimg.com/vi/TUVcZfQe-Kw/hqdefault.jpg"),
            Track("L0MK7qz13bU", "As It Was", "Harry Styles", "https://i.ytimg.com/vi/L0MK7qz13bU/hqdefault.jpg"),
            Track("kTJczUociss", "Stay", "The Kid LAROI & Justin Bieber", "https://i.ytimg.com/vi/kTJczUociss/hqdefault.jpg"),
            Track("gNi_6U5Pm_o", "Cruel Summer", "Taylor Swift", "https://i.ytimg.com/vi/gNi_6U5Pm_o/hqdefault.jpg"),
            Track("ApXoWvfEYVU", "Sunflower", "Post Malone & Swae Lee", "https://i.ytimg.com/vi/ApXoWvfEYVU/hqdefault.jpg"),
            Track("V1Pl8CzNzCw", "bad guy", "Billie Eilish", "https://i.ytimg.com/vi/V1Pl8CzNzCw/hqdefault.jpg"),
            Track("4Vq3Wd6Zq1g", "Flowers", "Miley Cyrus", "https://i.ytimg.com/vi/4Vq3Wd6Zq1g/hqdefault.jpg"),
            Track("0E1bU9856rc", "Kesariya", "Arijit Singh", "https://i.ytimg.com/vi/0E1bU9856rc/hqdefault.jpg"),
            Track("V7LwfY5U_B8", "Tum Hi Ho", "Arijit Singh", "https://i.ytimg.com/vi/V7LwfY5U_B8/hqdefault.jpg"),
            Track("y42j-55353s", "Apna Bana Le", "Arijit Singh & Sachin-Jigar", "https://i.ytimg.com/vi/y42j-55353s/hqdefault.jpg"),
            Track("b7k0u59203g", "Pasoori", "Ali Sethi & Shae Gill", "https://i.ytimg.com/vi/b7k0u59203g/hqdefault.jpg"),
            Track("BddP6PYo2gs", "Raataan Lambiyan", "Jubin Nautiyal & Asees Kaur", "https://i.ytimg.com/vi/BddP6PYo2gs/hqdefault.jpg"),
            Track("RLzC55ai0eo", "Heeriye", "Jasleen Royal ft. Arijit Singh", "https://i.ytimg.com/vi/RLzC55ai0eo/hqdefault.jpg"),
            Track("yIIGQB6EMAM", "Yellow", "Coldplay", "https://i.ytimg.com/vi/yIIGQB6EMAM/hqdefault.jpg"),
            Track("09R8_2nJtjg", "Sugar", "Maroon 5", "https://i.ytimg.com/vi/09R8_2nJtjg/hqdefault.jpg")
        )
        return pool.shuffled()
    }

    fun refreshHomeData(isUserPull: Boolean = false) {
        viewModelScope.launch {
            if (isUserPull) {
                _isRefreshing.value = true
            } else if (_trendingTracks.value.isEmpty()) {
                _isHomeLoading.value = true
            }
            try {
                if (_trendingTracks.value.isEmpty()) {
                    val starters = getStarterTracks()
                    _trendingTracks.value = starters
                    _recommendedTracks.value = starters.shuffled()
                    _recentlyPlayed.value = starters.take(6).shuffled()
                    _rediscoverTracks.value = starters.takeLast(6).shuffled()
                    _featuredHeroTrack.value = starters.random()
                }

                kotlinx.coroutines.coroutineScope {
                    launch { playlistManager.refreshDynamicPlaylists() }
                    launch { loadRecentlyPlayed() }
                    launch { loadRediscover() }
                    launch { loadTrending() }
                    launch { loadRecommended() }
                    launch { offlineManager.runSmartOffline() }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isRefreshing.value = false
                _isHomeLoading.value = false
            }
        }
    }

    private fun loadHomeConfig() {
        viewModelScope.launch {
            userPreferencesRepository.homeModules.collect { modules ->
                if (modules.isEmpty()) {
                    val default = listOf(
                        HomeModule.ContinueListening,
                        HomeModule.QuickMix,
                        HomeModule.Recommended,
                        HomeModule.Trending,
                        HomeModule.Downloads
                    )
                    _homeModules.value = default
                } else {
                    _homeModules.value = modules.filter { it != HomeModule.RecentlyPlayed && it != HomeModule.NewReleases }
                }
            }
        }
    }

    private suspend fun loadRecentlyPlayed() {
        val topArtists = analyticsRepository.getTopArtists()
        val results = if (topArtists.isNotEmpty()) {
            // Song DNA optimization: user has listening history!
            musicRepository.searchSongs("${topArtists.first().artist} hit songs").take(10)
        } else {
            // Cold start: random varied query
            val randomQueries = listOf("Global Hits 2026 songs", "Pop Essentials songs", "Acoustic Vibe music", "Bollywood Top 10 songs")
            musicRepository.searchSongs(randomQueries.random()).take(10)
        }
        if (results.isNotEmpty()) {
            _recentlyPlayed.value = results.shuffled()
        }
    }

    private suspend fun loadRediscover() {
        val topArtists = analyticsRepository.getTopArtists()
        val results = if (topArtists.size > 1) {
            // Song DNA optimization: query secondary top artist
            musicRepository.searchSongs("${topArtists[1].artist} popular songs").take(10)
        } else if (topArtists.isNotEmpty()) {
            musicRepository.searchSongs("${topArtists.first().artist} music recommendations").take(10)
        } else {
            // Cold start: random varied query
            val randomQueries = listOf("Viral Music 2026 songs", "EDM Party music", "Chillout Beats songs", "LoFi Study music")
            musicRepository.searchSongs(randomQueries.random()).take(10)
        }
        if (results.isNotEmpty()) {
            _rediscoverTracks.value = results.shuffled()
        }
    }

    private suspend fun loadTrending() {
        val topArtists = analyticsRepository.getTopArtists()
        val query = if (topArtists.isNotEmpty()) {
            "${topArtists.first().artist} trending songs"
        } else {
            listOf("Top 50 Songs Worldwide", "Global Hit Songs 2026", "Hot 100 Music Songs").random()
        }
        val trending = musicRepository.searchSongs(query).take(10)
        if (trending.isNotEmpty()) {
            _trendingTracks.value = trending
            _featuredHeroTrack.value = trending.first()
        }
    }

    private suspend fun loadRecommended() {
        val topArtists = analyticsRepository.getTopArtists()
        val query = if (topArtists.isNotEmpty()) {
            "${topArtists.first().artist} song playlist"
        } else {
            listOf("Top Recommended Songs", "Popular Music Hits", "Best Hit Songs 2026").random()
        }
        val recs = musicRepository.searchSongs(query).take(15)
        if (recs.isNotEmpty()) {
            _recommendedTracks.value = recs.shuffled()
        }
    }

    fun updateModuleOrder(newOrder: List<HomeModule>) {
        userPreferencesRepository.setHomeModules(newOrder)
    }
}


enum class HomeModule(val label: String) {
    RecentlyPlayed("Recently Played"),
    QuickMix("Quick Mix"),
    Recommended("Recommended for You"),
    NewReleases("New Releases"),
    ContinueListening("Continue Listening"),
    FavoriteArtists("Favorite Artists"),
    Downloads("Downloads"),
    MoodMixes("Mood Mixes"),
    YourPlaylists("Your Playlists"),
    Trending("Trending Now")
}
