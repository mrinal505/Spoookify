package com.spoookify.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio_profiles")
data class AudioProfile(
    @PrimaryKey val id: String, // "default", "track_{id}", or "artist_{name}"
    val name: String,
    val bands: List<Float>, // Gains in dB for each band
    val bassBoost: Int = 0, // 0-1000 range usually
    val virtualizerStrength: Int = 0,
    val compressorRatio: Float = 1.0f, // 1.0 to 20.0
    val limiterThreshold: Float = 0.0f, // -20.0 to 0.0 dB
    val isLoudnessNormalizationEnabled: Boolean = false,
    val isCustom: Boolean = false
)
