package com.spoookify.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_tracks")
data class FavoriteTrack(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val addedAt: Long = System.currentTimeMillis(),
    val interactionLevel: Int = 1 // 1: Like, 2: Favorite, 3: Replay, -1: Don't recommend, -2: Never play
)
