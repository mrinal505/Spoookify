package com.spoookify.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.spoookify.data.remote.Track
import com.spoookify.data.repository.MusicRepository
import com.spoookify.playback.MusicController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.spoookify.data.local.dao.FavoriteDao
import com.spoookify.data.local.entity.FavoriteTrack
import com.spoookify.playback.OfflineManager
import com.spoookify.playback.SmartQueueManager
import kotlinx.coroutines.flow.*

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val musicController: MusicController,
    private val musicRepository: MusicRepository,
    private val favoriteDao: FavoriteDao,
    private val smartQueueManager: SmartQueueManager,
    private val offlineManager: OfflineManager,
    private val userPreferencesRepository: com.spoookify.data.repository.UserPreferencesRepository,
    private val analyticsRepository: com.spoookify.data.repository.UserAnalyticsRepository,
    private val cloudSyncRepository: com.spoookify.data.repository.CloudSyncRepository
) : ViewModel() {

    init {
        cloudSyncRepository.listenToRemotePlaybackCommands { cmd, trackId, title, artist, thumbnailUrl, audioUrl ->
            viewModelScope.launch {
                when (cmd) {
                    "PLAY" -> {
                        val current = musicController.currentTrack.value
                        if (current == null || current.id != trackId) {
                            val trackToPlay = Track(
                                id = trackId,
                                title = title.ifEmpty { "Synced Track" },
                                artist = artist.ifEmpty { "Spoookify Web" },
                                thumbnailUrl = thumbnailUrl,
                                audioUrl = audioUrl.ifEmpty { null }
                            )
                            musicController.playTrack(trackToPlay)
                        } else {
                            if (!musicController.isPlaying.value) {
                                musicController.togglePlayPause()
                            }
                        }
                    }
                    "PAUSE", "PAUSED" -> {
                        if (musicController.isPlaying.value) {
                            musicController.togglePlayPause()
                        }
                    }
                    "NEXT" -> {
                        musicController.skipNext()
                    }
                    "PREV" -> {
                        musicController.skipPrevious()
                    }
                }
            }
        }
    }

    val skipIntervalSeconds = userPreferencesRepository.skipIntervalSeconds

    val currentTrack = musicController.currentTrack

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val trackInsights: StateFlow<TrackInsightsData?> = currentTrack
        .flatMapLatest { track ->
            if (track != null) {
                flow {
                    val plays = analyticsRepository.getPlayCountForTrack(track.id)
                    val skips = analyticsRepository.getSkipCountForTrack(track.id)
                    val firstDateMs = analyticsRepository.getFirstPlayedDateForTrack(track.id)
                    
                    val genre = SongInsightsHelper.detectGenre(track.artist, track.title)
                    val energy = SongInsightsHelper.calculateEnergy(track.id, track.title)
                    val bpm = SongInsightsHelper.calculateBpm(track.id, track.title)
                    val key = SongInsightsHelper.calculateKey(track.id, track.title)
                    
                    val playText = if (plays == 0) "First Session (1 play)" else "$plays plays"
                    val skipPct = if (plays > 0) (skips * 100 / plays).coerceIn(0, 100) else 0
                    val skipText = if (skipPct > 30) "High ($skipPct%)" else "Low ($skipPct%)"
                    val firstText = SongInsightsHelper.formatFirstPlayedDate(firstDateMs)

                    emit(
                        TrackInsightsData(
                            genre = genre,
                            energy = energy,
                            bpm = bpm,
                            key = key,
                            playCountText = playText,
                            skipRatioText = skipText,
                            firstListenedText = firstText
                        )
                    )
                }
            } else flowOf<TrackInsightsData?>(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val isPlaying = musicController.isPlaying
    val shuffleModeEnabled = musicController.shuffleModeEnabled
    val repeatMode = musicController.repeatMode
    val playbackProgress = musicController.playbackProgress
    val duration = musicController.duration
    val trackList = musicController.trackList
    val isLoading = musicController.isLoading

    val energyLevel = smartQueueManager.energyLevel
    val currentMood = smartQueueManager.currentMood

    fun setEnergy(level: Float) {
        smartQueueManager.setEnergy(level)
    }

    fun setMood(mood: String) {
        smartQueueManager.setMood(mood)
    }

    fun getWhyThisSong(): String {
        return currentTrack.value?.let { smartQueueManager.getWhyThisSong(it) } ?: ""
    }

    fun startSleepTimer(minutes: Int) {
        musicController.startSleepTimer(minutes)
    }

    private val _dominantColor = MutableStateFlow<Int?>(null)
    val dominantColor = _dominantColor.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val isFavorite: StateFlow<Boolean> = currentTrack
        .flatMapLatest { track ->
            if (track != null) favoriteDao.isFavorite(track.id)
            else flowOf(false)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val isDownloaded: StateFlow<Boolean> = currentTrack
        .flatMapLatest { track ->
            if (track != null) {
                // Now uses the reactive DAO flow
                musicController.isDownloadedFlow(track.id)
            } else flowOf(false)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val downloadProgress = offlineManager.downloadProgress

    private var nextTrackFetched = false
    private var secondNextTrackFetched = false

    init {
        viewModelScope.launch {
            playbackProgress.collect { progress ->
                val total = duration.value
                if (total > 0 && progress > (total * 0.7) && !nextTrackFetched) {
                    prefetchNextTrack(offset = 1)
                }
                if (total > 0 && progress > (total * 0.9) && !secondNextTrackFetched) {
                    prefetchNextTrack(offset = 2)
                }
            }
        }
        
        // Reset fetch flags when track changes
        viewModelScope.launch {
            currentTrack.collect {
                nextTrackFetched = false
                secondNextTrackFetched = false
                // Proactive warming: start fetching next track URL immediately when a song starts
                prefetchNextTrack(offset = 1)
            }
        }
    }

    fun toggleFavorite() {
        val track = currentTrack.value ?: return
        viewModelScope.launch {
            if (isFavorite.value) {
                favoriteDao.deleteFavorite(FavoriteTrack(track.id, track.title, track.artist, track.thumbnailUrl))
            } else {
                favoriteDao.insertFavorite(FavoriteTrack(track.id, track.title, track.artist, track.thumbnailUrl, interactionLevel = 1))
            }
        }
    }

    fun setInteractionLevel(level: Int) {
        val track = currentTrack.value ?: return
        viewModelScope.launch {
            if (level == 0) {
                favoriteDao.deleteFavorite(FavoriteTrack(track.id, track.title, track.artist, track.thumbnailUrl))
            } else {
                favoriteDao.insertFavorite(
                    FavoriteTrack(
                        id = track.id,
                        title = track.title,
                        artist = track.artist,
                        thumbnailUrl = track.thumbnailUrl,
                        interactionLevel = level
                    )
                )
            }
            
            if (level <= -2) {
                skipNext() // Immediately stop playing "Never Play" songs
            }
        }
    }

    val currentInteractionLevel: StateFlow<Int> = currentTrack
        .flatMapLatest { track ->
            if (track != null) {
                flow {
                    val fav = favoriteDao.getFavoriteById(track.id)
                    emit(fav?.interactionLevel ?: 0)
                }
            } else flowOf(0)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private fun prefetchNextTrack(offset: Int) {
        val current = currentTrack.value
        val list = trackList.value
        if (current != null && list.isNotEmpty()) {
            val index = list.indexOfFirst { it.id == current.id }
            if (index != -1 && index < list.size - offset) {
                val targetTrack = list[index + offset]
                if (offset == 1) nextTrackFetched = true else secondNextTrackFetched = true
                viewModelScope.launch {
                    // Pre-fetch URL and cache it in YoutubeExtractor
                    musicRepository.getStreamUrl(targetTrack.id)
                }
            }
        }
    }

    fun updateDominantColor(color: Int) {
        _dominantColor.value = color
    }

    fun playTrack(track: Track, playlist: List<Track> = emptyList()) {
        musicController.playTrack(track, playlist)
    }

    fun togglePlayPause() {
        musicController.togglePlayPause()
    }

    fun stopPlayback() {
        musicController.stopPlayback()
    }

    fun toggleShuffle() {
        musicController.toggleShuffle()
    }

    fun toggleRepeatMode() {
        musicController.toggleRepeatMode()
    }

    fun seekTo(position: Long) {
        musicController.playerState.value?.seekTo(position)
    }

    fun skipNext() {
        musicController.skipNext()
    }

    fun skipPrevious() {
        musicController.skipPrevious()
    }

    fun seekForward() {
        val ms = userPreferencesRepository.skipIntervalSeconds.value * 1000L
        musicController.seekForward(ms)
    }

    fun seekBackward() {
        val ms = userPreferencesRepository.skipIntervalSeconds.value * 1000L
        musicController.seekBackward(ms)
    }

    fun downloadTrack(track: Track? = currentTrack.value) {
        track?.let { offlineManager.downloadTrack(it) }
    }

    fun removeFromQueue(trackId: String) {
        musicController.removeFromQueue(trackId)
    }

    fun clearUpcomingQueue() {
        musicController.clearUpcomingQueue()
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        musicController.moveQueueItem(fromIndex, toIndex)
    }

    fun addToQueue(track: Track) {
        musicController.addToQueue(track)
    }

    fun executeAiCommand(command: String) {
        if (command.isBlank()) return
        viewModelScope.launch {
            smartQueueManager.parseAiCommand(command)
            smartQueueManager.generateSmartQueue(currentTrack.value)
            val queue = smartQueueManager.smartQueue.value
            if (queue.isNotEmpty()) {
                playTrack(queue.first(), queue)
            }
        }
    }

    fun playSimilar() {
        val current = currentTrack.value ?: return
        viewModelScope.launch {
            smartQueueManager.generateSmartQueue(current)
            val queue = smartQueueManager.smartQueue.value
            if (queue.isNotEmpty()) {
                playTrack(queue.first(), queue)
            }
        }
    }

    fun playFavorites() {
        viewModelScope.launch {
            favoriteDao.getAllFavorites().first().let { favorites ->
                if (favorites.isNotEmpty()) {
                    val tracks = favorites.map { favorite ->
                        Track(
                            id = favorite.id,
                            title = favorite.title,
                            artist = favorite.artist,
                            thumbnailUrl = favorite.thumbnailUrl
                        )
                    }
                    playTrack(tracks.first(), tracks)
                }
            }
        }
    }
}
