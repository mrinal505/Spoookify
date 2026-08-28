package com.spoookify.playback

import com.spoookify.data.local.dao.ArtistCount
import com.spoookify.data.local.dao.FavoriteDao
import com.spoookify.data.remote.Track
import com.spoookify.data.repository.MusicRepository
import com.spoookify.data.repository.UserAnalyticsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartQueueManager @Inject constructor(
    private val musicRepository: MusicRepository,
    private val analyticsRepository: UserAnalyticsRepository,
    private val favoriteDao: FavoriteDao
) {
    private val _smartQueue = MutableStateFlow<List<Track>>(emptyList())
    val smartQueue = _smartQueue.asStateFlow()

    private val _energyLevel = MutableStateFlow(0.5f)
    val energyLevel = _energyLevel.asStateFlow()

    private val _currentMood = MutableStateFlow("Chill")
    val currentMood = _currentMood.asStateFlow()

    private val _aiContext = MutableStateFlow<String?>(null)
    val aiContext = _aiContext.asStateFlow()

    fun setEnergy(level: Float) {
        _energyLevel.value = level
    }

    fun setMood(mood: String) {
        _currentMood.value = mood
    }

    fun setAiContext(context: String?) {
        _aiContext.value = context
    }

    suspend fun generateSmartQueue(seedTrack: Track? = null) {
        val topArtists = analyticsRepository.getTopArtists()
        
        val contextQuery = _aiContext.value ?: ""
        val seedQuery = seedTrack?.let { "similar to ${it.title} ${it.artist}" } ?: ""
        val moodQuery = "${_currentMood.value} ${if (_energyLevel.value > 0.7f) "energetic" else if (_energyLevel.value < 0.3f) "calm" else ""} music"
        
        var searchResults = musicRepository.searchSongs("$contextQuery $seedQuery $moodQuery".trim())
        
        // Filter out "Never Play" and "Don't Recommend"
        val negativeTracks = favoriteDao.getNegativeTracks().first().map { it.id }.toSet()
        searchResults = searchResults.filter { !negativeTracks.contains(it.id) }
        
        val discovery = fetchDiscoveryTracks(topArtists).filter { !negativeTracks.contains(it.id) }
        
        val combined = (searchResults + discovery).distinctBy { it.id }.shuffled()
        _smartQueue.value = combined.take(50)
    }

    private suspend fun fetchDiscoveryTracks(topArtists: List<ArtistCount>): List<Track> {
        val tracks = mutableListOf<Track>()
        topArtists.take(3).forEach { artist ->
            tracks.addAll(musicRepository.searchSongs("${artist.artist} mix"))
        }
        return tracks
    }

    fun getWhyThisSong(track: Track): String {
        val context = _aiContext.value
        return if (context != null) {
            "Selected based on your request for '$context' and your interest in ${track.artist}."
        } else {
            "Selected based on your interest in ${track.artist} and your current ${_currentMood.value} mood."
        }
    }

    fun parseAiCommand(command: String) {
        val lower = command.lowercase()
        when {
            lower.contains("energetic") || lower.contains("fast") || lower.contains("workout") -> {
                setEnergy(0.9f)
                setMood("Workout")
            }
            lower.contains("chill") || lower.contains("relax") || lower.contains("calm") -> {
                setEnergy(0.2f)
                setMood("Chill")
            }
            lower.contains("sad") || lower.contains("emotional") -> {
                setEnergy(0.3f)
                setMood("Emotional")
            }
            lower.contains("party") || lower.contains("dance") -> {
                setEnergy(0.8f)
                setMood("Party")
            }
        }
        setAiContext(command)
    }
}
