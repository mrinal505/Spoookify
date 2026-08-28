package com.spoookify.data.local.dao

import androidx.room.*
import com.spoookify.data.local.entity.DownloadTrack
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM download_tracks")
    fun getAllDownloads(): Flow<List<DownloadTrack>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(track: DownloadTrack)

    @Delete
    suspend fun deleteDownload(track: DownloadTrack)

    @Query("SELECT * FROM download_tracks WHERE id = :id")
    suspend fun getDownloadById(id: String): DownloadTrack?

    @Query("SELECT EXISTS(SELECT 1 FROM download_tracks WHERE id = :id)")
    fun isDownloaded(id: String): Flow<Boolean>

    @Query("DELETE FROM download_tracks")
    suspend fun clearAllDownloads()
}
