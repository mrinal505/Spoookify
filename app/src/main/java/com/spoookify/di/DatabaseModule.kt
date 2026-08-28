package com.spoookify.di

import android.content.Context
import androidx.room.Room
import com.spoookify.data.local.dao.AnalyticsDao
import com.spoookify.data.local.dao.AudioProfileDao
import com.spoookify.data.local.dao.DownloadDao
import com.spoookify.data.local.dao.DynamicPlaylistDao
import com.spoookify.data.local.dao.FavoriteDao
import com.spoookify.data.local.database.MusicDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MusicDatabase {
        return Room.databaseBuilder(
            context,
            MusicDatabase::class.java,
            "spoookify_db"
        )
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    @Provides
    fun provideFavoriteDao(database: MusicDatabase): FavoriteDao {
        return database.favoriteDao()
    }

    @Provides
    fun provideAudioProfileDao(database: MusicDatabase): AudioProfileDao {
        return database.audioProfileDao()
    }

    @Provides
    fun provideAnalyticsDao(database: MusicDatabase): AnalyticsDao {
        return database.analyticsDao()
    }

    @Provides
    fun provideDownloadDao(database: MusicDatabase): DownloadDao {
        return database.downloadDao()
    }

    @Provides
    fun provideDynamicPlaylistDao(database: MusicDatabase): DynamicPlaylistDao {
        return database.dynamicPlaylistDao()
    }
}
