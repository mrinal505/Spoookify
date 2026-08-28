package com.spoookify.data.local.dao

import androidx.room.*
import com.spoookify.data.local.entity.AudioProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioProfileDao {
    @Query("SELECT * FROM audio_profiles")
    fun getAllProfiles(): Flow<List<AudioProfile>>

    @Query("SELECT * FROM audio_profiles WHERE id = :id")
    suspend fun getProfileById(id: String): AudioProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: AudioProfile)

    @Delete
    suspend fun deleteProfile(profile: AudioProfile)
}
