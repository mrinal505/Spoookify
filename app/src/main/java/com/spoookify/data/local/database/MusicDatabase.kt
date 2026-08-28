package com.spoookify.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.spoookify.data.local.dao.FavoriteDao
import androidx.room.TypeConverters
import com.spoookify.data.local.dao.AnalyticsDao
import com.spoookify.data.local.dao.AudioProfileDao
import com.spoookify.data.local.entity.AudioProfile
import com.spoookify.data.local.dao.DownloadDao
import com.spoookify.data.local.dao.DynamicPlaylistDao
import com.spoookify.data.local.entity.DownloadTrack
import com.spoookify.data.local.entity.DynamicPlaylist
import com.spoookify.data.local.entity.FavoriteTrack
import com.spoookify.data.local.entity.ListeningEvent

@Database(
    entities = [FavoriteTrack::class, AudioProfile::class, ListeningEvent::class, DownloadTrack::class, DynamicPlaylist::class], 
    version = 7, 
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun audioProfileDao(): AudioProfileDao
    abstract fun analyticsDao(): AnalyticsDao
    abstract fun downloadDao(): DownloadDao
    abstract fun dynamicPlaylistDao(): DynamicPlaylistDao
}
