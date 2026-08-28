package com.spoookify.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_tracks")
data class DownloadTrack(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val localFilePath: String,
    val fileSize: Long,
    val isAutoDownloaded: Boolean = false,
    val lastPlayedAt: Long = System.currentTimeMillis()
)
