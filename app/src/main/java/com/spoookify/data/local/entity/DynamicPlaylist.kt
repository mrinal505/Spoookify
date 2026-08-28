package com.spoookify.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dynamic_playlists")
data class DynamicPlaylist(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val rulesJson: String, // Simplified: store rules as JSON
    val lastUpdated: Long = System.currentTimeMillis()
)
