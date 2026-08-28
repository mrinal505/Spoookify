package com.spoookify.data.repository

import com.spoookify.data.local.dao.AnalyticsDao
import com.spoookify.data.local.entity.ListeningEvent
import com.spoookify.data.remote.Track
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserAnalyticsRepository @Inject constructor(
    private val analyticsDao: AnalyticsDao
) {
    suspend fun logEvent(track: Track, duration: Long, isSkipped: Boolean, isFinished: Boolean) {
        try {
            val event = ListeningEvent(
                trackId = track.id,
                trackTitle = track.title,
                artist = track.artist,
                thumbnailUrl = track.thumbnailUrl,
                durationPlayed = duration,
                isSkipped = isSkipped,
                isFinished = isFinished
            )
            analyticsDao.insertEvent(event)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    suspend fun getTopArtists() = try { analyticsDao.getTopArtists() } catch (e: Throwable) { emptyList() }
    suspend fun getTopTracks() = try { analyticsDao.getTopTracks() } catch (e: Throwable) { emptyList() }
    suspend fun getMostSkippedArtists() = try { analyticsDao.getMostSkippedArtists() } catch (e: Throwable) { emptyList() }
    suspend fun getFirstPlayedDate() = try { analyticsDao.getFirstPlayedDate() } catch (e: Throwable) { null }
    suspend fun getLastPlayedDate() = try { analyticsDao.getLastPlayedDate() } catch (e: Throwable) { null }
    suspend fun getAverageSongDuration() = try { analyticsDao.getAverageSongDuration() } catch (e: Throwable) { null }
    suspend fun getTotalDurationPlayed() = try { analyticsDao.getTotalDurationPlayed() } catch (e: Throwable) { 0L }
    suspend fun getAllArtistCounts() = try { analyticsDao.getAllArtistCounts() } catch (e: Throwable) { emptyList() }
    suspend fun getAllListeningTimestamps() = try { analyticsDao.getAllListeningTimestamps() } catch (e: Throwable) { emptyList() }
    
    suspend fun getRecentLikes() = try { analyticsDao.getRecentLikes() } catch (e: Throwable) { emptyList() }
    suspend fun getRediscoverIds() = try { analyticsDao.getRediscoverIds() } catch (e: Throwable) { emptyList() }

    suspend fun getPlayCountForTrack(trackId: String) = try { analyticsDao.getPlayCountForTrack(trackId) } catch (e: Throwable) { 0 }
    suspend fun getSkipCountForTrack(trackId: String) = try { analyticsDao.getSkipCountForTrack(trackId) } catch (e: Throwable) { 0 }
    suspend fun getFirstPlayedDateForTrack(trackId: String) = try { analyticsDao.getFirstPlayedDateForTrack(trackId) } catch (e: Throwable) { null }
}
