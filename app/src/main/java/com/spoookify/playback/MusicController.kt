package com.spoookify.playback

import android.content.ComponentName
import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.spoookify.data.remote.Track
import com.spoookify.data.repository.MusicRepository
import com.spoookify.data.repository.UserAnalyticsRepository
import com.spoookify.service.MusicService
import java.io.File
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(UnstableApi::class)
@Singleton
class MusicController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val player: ExoPlayer,
    private val musicRepository: MusicRepository,
    private val audioProfileManager: AudioProfileManager,
    private val analyticsRepository: UserAnalyticsRepository,
    private val smartQueueManager: SmartQueueManager,
    private val carModeManager: CarModeManager,
    private val downloadDao: com.spoookify.data.local.dao.DownloadDao,
    private val favoriteDao: com.spoookify.data.local.dao.FavoriteDao
) {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var dynamicsProcessing: DynamicsProcessing? = null

    private var sleepTimerJob: Job? = null
    private val _sleepTimerRemaining = MutableStateFlow(0L)
    val sleepTimerRemaining = _sleepTimerRemaining.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val _playerState = MutableStateFlow<Player?>(player)
    val playerState: StateFlow<Player?> = _playerState

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack = _currentTrack.asStateFlow()

    private val _trackList = MutableStateFlow<List<Track>>(emptyList())
    val trackList = _trackList.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0L)
    val playbackProgress = _playbackProgress.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration = _duration.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _shuffleModeEnabled = MutableStateFlow(false)
    val shuffleModeEnabled = _shuffleModeEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode = _repeatMode.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _currentFormat = MutableStateFlow<androidx.media3.common.Format?>(null)
    val currentFormat = _currentFormat.asStateFlow()

    private val _bufferedPosition = MutableStateFlow(0L)
    val bufferedPosition = _bufferedPosition.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onMetadata(metadata: androidx.media3.common.Metadata) {
            super.onMetadata(metadata)
        }

        override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
            // Find the selected audio track and get its format
            tracks.groups.forEach { group ->
                if (group.type == C.TRACK_TYPE_AUDIO && group.isSelected) {
                    _currentFormat.value = group.getTrackFormat(0)
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            mediaItem?.let { item ->
                val track = _trackList.value.find { it.id == item.mediaId }
                if (track != null) {
                    _currentTrack.value = track
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (isPlaying) {
                fadeIn()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                logTrackFinished()
                skipNext()
            }
        }

        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            setupAudioEffects(audioSessionId)
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _shuffleModeEnabled.value = shuffleModeEnabled
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            super.onPlayerError(error)
            val failedTrack = _currentTrack.value
            if (failedTrack != null) {
                scope.launch {
                    val freshUrl = musicRepository.getFreshStreamUrl(failedTrack.id)
                    if (freshUrl != null) {
                        val freshMediaItem = createMediaItem(failedTrack, android.net.Uri.parse(freshUrl))
                        player.setMediaItems(listOf(freshMediaItem))
                        player.prepare()
                        player.play()
                    } else {
                        skipNext()
                    }
                }
            }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeatMode.value = repeatMode
        }
    }

    init {
        player.addListener(playerListener)
        _isPlaying.value = player.isPlaying
        _shuffleModeEnabled.value = player.shuffleModeEnabled
        _repeatMode.value = player.repeatMode
        startProgressUpdate()
        observeAudioProfiles()
        observeCarMode()
        
        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
    }

    private fun observeCarMode() {
        scope.launch {
            carModeManager.carModeEvents.collect { active ->
                if (active && !player.isPlaying) {
                    player.play()
                }
            }
        }
    }

    private fun observeAudioProfiles() {
        scope.launch {
            audioProfileManager.currentProfile.collect { profile ->
                profile?.let { applyAudioProfile(it) }
            }
        }
        scope.launch {
            audioProfileManager.playbackSpeed.collect { speed ->
                try {
                    player.playbackParameters = androidx.media3.common.PlaybackParameters(speed)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
        scope.launch {
            audioProfileManager.balance.collect { bal ->
                try {
                    val left = (1.0f - bal).coerceIn(0f, 1f)
                    val right = (1.0f + bal).coerceIn(0f, 1f)
                    player.volume = (left + right) / 2f
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun setupAudioEffects(sessionId: Int) {
        if (sessionId == C.AUDIO_SESSION_ID_UNSET || sessionId == 0) return
        
        try { equalizer?.release() } catch (_: Exception) {}
        try { bassBoost?.release() } catch (_: Exception) {}
        try { virtualizer?.release() } catch (_: Exception) {}
        try { dynamicsProcessing?.release() } catch (_: Exception) {}

        try {
            equalizer = Equalizer(0, sessionId).apply { enabled = true }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        try {
            bassBoost = BassBoost(0, sessionId).apply { enabled = true }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        try {
            virtualizer = Virtualizer(0, sessionId).apply { enabled = true }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val config = DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    1,
                    true, 0, true, 0, true, 0, true
                ).build()
                dynamicsProcessing = DynamicsProcessing(0, sessionId, config).apply { enabled = true }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        audioProfileManager.currentProfile.value?.let { applyAudioProfile(it) }
    }

    private fun applyAudioProfile(profile: com.spoookify.data.local.entity.AudioProfile) {
        equalizer?.let { eq ->
            try {
                val numBands = eq.numberOfBands.toInt()
                val range = eq.bandLevelRange
                val minGainMb = if (range.size >= 2) range[0] else -1500.toShort()
                val maxGainMb = if (range.size >= 2) range[1] else 1500.toShort()

                for (i in 0 until numBands) {
                    val profileGainIndex = (i * profile.bands.size / numBands).coerceIn(0, profile.bands.size - 1)
                    val rawGain = (profile.bands[profileGainIndex] * 100).toInt()
                    val gain = rawGain.coerceIn(minGainMb.toInt(), maxGainMb.toInt()).toShort()
                    try {
                        eq.setBandLevel(i.toShort(), gain)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        bassBoost?.let { bb ->
            try {
                if (bb.strengthSupported) {
                    val strength = profile.bassBoost.toShort().coerceIn(0, 1000)
                    bb.setStrength(strength)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        virtualizer?.let { virt ->
            try {
                if (virt.strengthSupported) {
                    val strength = profile.virtualizerStrength.toShort().coerceIn(0, 1000)
                    virt.setStrength(strength)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            dynamicsProcessing?.let { dp ->
                try {
                    val limiter = DynamicsProcessing.Limiter(true, true, 0, 1.0f, 2.0f, 10.0f, profile.limiterThreshold, 0.0f)
                    dp.setLimiterByChannelIndex(0, limiter)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun fadeIn() {
        scope.launch {
            var volume = 0f
            while (volume < 1.0f && player.isPlaying) {
                volume += 0.05f
                player.volume = volume
                delay(50)
            }
            player.volume = 1.0f
        }
    }

    private fun fadeOut(onComplete: () -> Unit) {
        scope.launch {
            var volume = player.volume
            while (volume > 0f && player.isPlaying) {
                volume -= 0.05f
                player.volume = volume
                delay(50)
            }
            onComplete()
            player.volume = 1.0f // Reset for next play
        }
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerRemaining.value = 0
            return
        }
        
        _sleepTimerRemaining.value = minutes * 60 * 1000L
        sleepTimerJob = scope.launch {
            while (_sleepTimerRemaining.value > 0) {
                delay(1000)
                _sleepTimerRemaining.value -= 1000
            }
            player.pause()
        }
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                _playbackProgress.value = player.currentPosition
                _duration.value = if (player.duration > 0) player.duration else 0L
                _bufferedPosition.value = player.bufferedPosition
                delay(1000)
            }
        }
    }

    fun playTrack(track: Track, playlist: List<Track> = emptyList()) {
        if (_currentTrack.value?.id == track.id && player.isPlaying) return

        scope.launch(CoroutineExceptionHandler { _, throwable ->
            throwable.printStackTrace()
            _isLoading.value = false
        }) {
            // Block check
            val fav = favoriteDao.getFavoriteById(track.id)
            if (fav != null && fav.interactionLevel <= -2) {
                skipNext()
                return@launch
            }

            _isLoading.value = true
            _currentTrack.value = track
            val currentPlaylist = if (playlist.isNotEmpty()) playlist else _trackList.value
            _trackList.value = currentPlaylist

            try {
                // Resolve requested track stream URI immediately
                val targetUri = getTrackUri(track)
                if (targetUri != null) {
                    val targetMediaItem = createMediaItem(track, targetUri)
                    player.setMediaItems(listOf(targetMediaItem))
                    player.prepare()
                    player.play()
                    _isLoading.value = false

                    // Pre-fetch next queue items asynchronously in background
                    val startIndex = currentPlaylist.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
                    val maxWarm = (startIndex + 5).coerceAtMost(currentPlaylist.size)
                    
                    scope.launch(Dispatchers.IO) {
                        for (i in (startIndex + 1) until maxWarm) {
                            try {
                                val nextTrack = currentPlaylist[i]
                                val nextUri = getTrackUri(nextTrack)
                                if (nextUri != null) {
                                    withContext(Dispatchers.Main) {
                                        player.addMediaItem(createMediaItem(nextTrack, nextUri))
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                } else {
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isLoading.value = false
            }
        }
    }

    private suspend fun getTrackUri(track: Track): android.net.Uri? {
        val downloadedTrack = downloadDao.getDownloadById(track.id)
        if (downloadedTrack != null && File(downloadedTrack.localFilePath).exists()) {
            return android.net.Uri.fromFile(File(downloadedTrack.localFilePath))
        }
        var streamUrl = track.audioUrl ?: musicRepository.getStreamUrl(track.id)
        if (streamUrl.isNullOrBlank()) {
            val searchResults = musicRepository.searchSongs("${track.title} ${track.artist}")
            val fallbackTrack = searchResults.firstOrNull()
            if (fallbackTrack != null) {
                streamUrl = musicRepository.getStreamUrl(fallbackTrack.id)
            }
        }
        return streamUrl?.let { android.net.Uri.parse(it) }
    }

    private fun createMediaItem(track: Track, uri: android.net.Uri): MediaItem {
        return MediaItem.Builder()
            .setUri(uri)
            .setMediaId(track.id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setArtworkUri(android.net.Uri.parse(track.thumbnailUrl))
                    .build()
            )
            .build()
    }

    fun setTrackList(tracks: List<Track>) {
        _trackList.value = tracks
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            fadeOut { player.pause() }
        } else {
            player.play()
            // fadeIn is called by listener
        }
    }

    fun toggleShuffle() {
        val nextState = !_shuffleModeEnabled.value
        player.shuffleModeEnabled = nextState
        _shuffleModeEnabled.value = nextState
    }

    fun toggleRepeatMode() {
        val nextMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        player.repeatMode = nextMode
        _repeatMode.value = nextMode
    }

    fun seekForward(intervalMs: Long = 10000L) {
        player.seekTo(player.currentPosition + intervalMs)
    }

    fun seekBackward(intervalMs: Long = 10000L) {
        player.seekTo((player.currentPosition - intervalMs).coerceAtLeast(0L))
    }

    fun skipNext() {
        logTrackSkipped()
        val current = _currentTrack.value
        val list = _trackList.value
        if (list.isEmpty()) return

        if (_repeatMode.value == Player.REPEAT_MODE_ONE && current != null) {
            playTrack(current)
            return
        }

        if (_shuffleModeEnabled.value) {
            val candidates = if (list.size > 1 && current != null) list.filter { it.id != current.id } else list
            val randomTrack = candidates.randomOrNull() ?: current
            if (randomTrack != null) {
                playTrack(randomTrack)
            }
            return
        }

        val currentIndex = list.indexOfFirst { it.id == current?.id }
        if (currentIndex != -1) {
            if (currentIndex < list.size - 1) {
                playTrack(list[currentIndex + 1])
            } else {
                if (_repeatMode.value == Player.REPEAT_MODE_ALL) {
                    playTrack(list.first())
                } else {
                    scope.launch {
                        if (current != null) {
                            smartQueueManager.generateSmartQueue(current)
                            val nextQueue = smartQueueManager.smartQueue.value
                            if (nextQueue.isNotEmpty()) {
                                _trackList.value = nextQueue
                                playTrack(nextQueue.first())
                            }
                        }
                    }
                }
            }
        } else if (list.isNotEmpty()) {
            playTrack(list.first())
        }
    }

    private fun logTrackFinished() {
        val track = _currentTrack.value ?: return
        val dur = if (player.duration > 0 && player.duration < 18000000L) player.duration else 180000L
        scope.launch {
            analyticsRepository.logEvent(track, dur, false, true)
        }
    }

    private fun logTrackSkipped() {
        val track = _currentTrack.value ?: return
        val pos = if (player.currentPosition > 0 && player.currentPosition < 18000000L) player.currentPosition else 30000L
        scope.launch {
            analyticsRepository.logEvent(track, pos, true, false)
        }
    }

    fun skipPrevious() {
        val current = _currentTrack.value
        val list = _trackList.value
        if (list.isEmpty()) return

        if (player.currentPosition > 5000) {
            player.seekTo(0L)
            return
        }

        if (_shuffleModeEnabled.value) {
            val candidates = if (list.size > 1 && current != null) list.filter { it.id != current.id } else list
            val randomTrack = candidates.randomOrNull() ?: current
            if (randomTrack != null) {
                playTrack(randomTrack)
            }
            return
        }

        val currentIndex = list.indexOfFirst { it.id == current?.id }
        if (currentIndex != -1) {
            if (currentIndex > 0) {
                playTrack(list[currentIndex - 1])
            } else {
                if (_repeatMode.value == Player.REPEAT_MODE_ALL) {
                    playTrack(list.last())
                } else {
                    player.seekTo(0L)
                }
            }
        } else {
            playTrack(list.first())
        }
    }

    fun removeFromQueue(trackId: String) {
        val updated = _trackList.value.filterNot { it.id == trackId }
        _trackList.value = updated
    }

    fun clearUpcomingQueue() {
        val current = _currentTrack.value
        if (current != null) {
            _trackList.value = listOf(current)
        } else {
            _trackList.value = emptyList()
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val list = _trackList.value.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _trackList.value = list
        }
    }

    fun addToQueue(track: Track) {
        val currentList = _trackList.value.toMutableList()
        if (!currentList.any { it.id == track.id }) {
            currentList.add(track)
            _trackList.value = currentList
        }
    }

    fun setCurrentTrack(track: Track?) {
        _currentTrack.value = track
    }

    fun isDownloadedFlow(trackId: String): kotlinx.coroutines.flow.Flow<Boolean> {
        return downloadDao.isDownloaded(trackId)
    }

    fun stopPlayback() {
        scope.launch(Dispatchers.Main) {
            try {
                player.stop()
                player.clearMediaItems()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _currentTrack.value = null
            _isPlaying.value = false
        }
    }

    fun release() {
        _playerState.value?.removeListener(playerListener)
        progressJob?.cancel()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}
