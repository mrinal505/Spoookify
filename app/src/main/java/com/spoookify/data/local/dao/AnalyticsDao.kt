package com.spoookify.data.local.dao

import androidx.room.*
import com.spoookify.data.local.entity.ListeningEvent

@Dao
interface AnalyticsDao {
    @Insert
    suspend fun insertEvent(event: ListeningEvent)

    @Query("SELECT artist, COUNT(*) as count FROM listening_history GROUP BY artist ORDER BY count DESC LIMIT 10")
    suspend fun getTopArtists(): List<ArtistCount>

    @Query("SELECT trackId, trackTitle, artist, thumbnailUrl, COUNT(*) as count FROM listening_history GROUP BY trackId ORDER BY count DESC LIMIT 10")
    suspend fun getTopTracks(): List<TrackCount>

    @Query("SELECT artist, COUNT(*) as count FROM listening_history WHERE isSkipped = 1 GROUP BY artist ORDER BY count DESC LIMIT 5")
    suspend fun getMostSkippedArtists(): List<ArtistCount>

    @Query("SELECT timestamp FROM listening_history ORDER BY timestamp DESC")
    suspend fun getAllListeningTimestamps(): List<Long>

    @Query("SELECT MIN(timestamp) FROM listening_history")
    suspend fun getFirstPlayedDate(): Long?

    @Query("SELECT MAX(timestamp) FROM listening_history")
    suspend fun getLastPlayedDate(): Long?

    @Query("SELECT AVG(durationPlayed) FROM listening_history WHERE isFinished = 1 AND durationPlayed > 0 AND durationPlayed <= 18000000")
    suspend fun getAverageSongDuration(): Long?

    @Query("SELECT trackId FROM listening_history WHERE isFinished = 1 ORDER BY timestamp DESC LIMIT 50")
    suspend fun getRecentLikes(): List<String>

    @Query("SELECT trackId FROM listening_history ORDER BY timestamp ASC LIMIT 10")
    suspend fun getRediscoverIds(): List<String>

    @Query("SELECT TOTAL(durationPlayed) FROM listening_history WHERE durationPlayed > 0 AND durationPlayed <= 18000000")
    suspend fun getTotalDurationPlayed(): Long?

    @Query("DELETE FROM listening_history WHERE durationPlayed <= 0 OR durationPlayed > 18000000")
    suspend fun cleanCorruptEvents()

    @Query("SELECT artist, COUNT(*) as count FROM listening_history GROUP BY artist ORDER BY count DESC")
    suspend fun getAllArtistCounts(): List<ArtistCount>

    @Query("SELECT COUNT(*) FROM listening_history WHERE trackId = :trackId")
    suspend fun getPlayCountForTrack(trackId: String): Int

    @Query("SELECT COUNT(*) FROM listening_history WHERE trackId = :trackId AND isSkipped = 1")
    suspend fun getSkipCountForTrack(trackId: String): Int

    @Query("SELECT MIN(timestamp) FROM listening_history WHERE trackId = :trackId")
    suspend fun getFirstPlayedDateForTrack(trackId: String): Long?
}

data class ArtistCount(
    val artist: String,
    val count: Int
)

data class TrackCount(
    val trackId: String,
    val trackTitle: String,
    val artist: String,
    val thumbnailUrl: String,
    val count: Int
)
