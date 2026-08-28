package com.spoookify.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spoookify.data.local.dao.AnalyticsDao
import com.spoookify.data.local.dao.ArtistCount
import com.spoookify.data.local.dao.TrackCount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val analyticsDao: AnalyticsDao
) : ViewModel() {

    private val _topArtists = MutableStateFlow<List<ArtistCount>>(emptyList())
    val topArtists = _topArtists.asStateFlow()

    private val _topTracks = MutableStateFlow<List<TrackCount>>(emptyList())
    val topTracks = _topTracks.asStateFlow()

    private val _stats = MutableStateFlow(MusicStats())
    val stats = _stats.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            try {
                analyticsDao.cleanCorruptEvents()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val topArt = analyticsDao.getTopArtists()
            val topTra = analyticsDao.getTopTracks()
            val skipped = analyticsDao.getMostSkippedArtists()
            val timestamps = analyticsDao.getAllListeningTimestamps()
            
            _topArtists.value = topArt
            _topTracks.value = topTra
            
            val totalMillis = (analyticsDao.getTotalDurationPlayed() ?: 0L).coerceAtLeast(0L)
            val allArtists = analyticsDao.getAllArtistCounts()
            
            val dna = allArtists.take(5).associate { 
                it.artist to ((it.count.toFloat() / allArtists.sumOf { a -> a.count }.coerceAtLeast(1)) * 100).toInt()
            }

            val rawAvgDuration = (analyticsDao.getAverageSongDuration() ?: 210000L).coerceAtLeast(0L)
            val avgDurationSecs = (rawAvgDuration / 1000).toInt().coerceIn(0, 1800)
            val streak = calculateStreak(timestamps)

            val hours = (totalMillis / (1000f * 60 * 60)).coerceIn(0f, 9999f)

            _stats.value = MusicStats(
                totalHours = hours,
                discoveryRate = if (allArtists.isNotEmpty()) (allArtists.size * 100 / (allArtists.sumOf { it.count }.coerceAtLeast(1))).coerceIn(0, 100) else 0,
                musicDNA = dna,
                mostSkippedArtist = skipped.firstOrNull()?.artist ?: "None",
                averageSongDurationSeconds = if (avgDurationSecs > 0) avgDurationSecs else 210,
                listeningStreakDays = streak,
                firstPlayedDate = analyticsDao.getFirstPlayedDate(),
                lastPlayedDate = analyticsDao.getLastPlayedDate()
            )
        }
    }

    private fun calculateStreak(timestamps: List<Long>): Int {
        if (timestamps.isEmpty()) return 0
        
        val uniqueDays = timestamps.map { 
            val cal = Calendar.getInstance()
            cal.timeInMillis = it
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.distinct().sortedDescending()

        var streak = 0
        var currentDay = Calendar.getInstance()
        currentDay.set(Calendar.HOUR_OF_DAY, 0)
        currentDay.set(Calendar.MINUTE, 0)
        currentDay.set(Calendar.SECOND, 0)
        currentDay.set(Calendar.MILLISECOND, 0)
        
        var checkTime = currentDay.timeInMillis

        for (day in uniqueDays) {
            if (day == checkTime) {
                streak++
                checkTime -= 24 * 60 * 60 * 1000L
            } else if (day > checkTime) {
                continue
            } else {
                break
            }
        }
        return streak
    }
}

data class MusicStats(
    val totalHours: Float = 0f,
    val discoveryRate: Int = 0,
    val musicDNA: Map<String, Int> = emptyMap(),
    val mostSkippedArtist: String = "None",
    val averageSongDurationSeconds: Int = 0,
    val listeningStreakDays: Int = 0,
    val firstPlayedDate: Long? = null,
    val lastPlayedDate: Long? = null
)
