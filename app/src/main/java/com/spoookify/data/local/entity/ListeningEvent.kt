package com.spoookify.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "listening_history")
data class ListeningEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: String,
    val trackTitle: String,
    val artist: String,
    val thumbnailUrl: String,
    val durationPlayed: Long,
    val isSkipped: Boolean,
    val isFinished: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
