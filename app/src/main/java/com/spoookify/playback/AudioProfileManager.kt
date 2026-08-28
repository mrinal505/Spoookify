package com.spoookify.playback

import com.spoookify.data.local.dao.AudioProfileDao
import com.spoookify.data.local.entity.AudioProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioProfileManager @Inject constructor(
    private val audioProfileDao: AudioProfileDao
) {
    private val _currentProfile = MutableStateFlow<AudioProfile?>(getDefaultProfiles()[0])
    val currentProfile = _currentProfile.asStateFlow()

    private val _isNormalizationEnabled = MutableStateFlow(false)
    val isNormalizationEnabled = _isNormalizationEnabled.asStateFlow()

    private val _isMonoEnabled = MutableStateFlow(false)
    val isMonoEnabled = _isMonoEnabled.asStateFlow()

    private val _balance = MutableStateFlow(0f) // -1.0 to 1.0
    val balance = _balance.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed = _playbackSpeed.asStateFlow()

    private val deviceProfiles = mutableMapOf<String, AudioProfile>()

    fun setProfile(profile: AudioProfile, deviceName: String? = null) {
        _currentProfile.value = profile
        deviceName?.let { deviceProfiles[it] = profile }
    }

    fun getProfileForDevice(deviceName: String?): AudioProfile? {
        return deviceProfiles[deviceName]
    }

    fun setNormalization(enabled: Boolean) {
        _isNormalizationEnabled.value = enabled
    }

    fun setMono(enabled: Boolean) {
        _isMonoEnabled.value = enabled
    }

    fun setBalance(balance: Float) {
        _balance.value = balance.coerceIn(-1f, 1f)
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed.coerceIn(0.25f, 4.0f)
    }

    suspend fun saveProfile(profile: AudioProfile) {
        audioProfileDao.insertProfile(profile)
    }

    fun getDefaultProfiles(): List<AudioProfile> {
        return listOf(
            AudioProfile("flat", "Flat", List(10) { 0f }, 0, 0, 1.0f, 0.0f, false, false),
            AudioProfile("bass_boost", "Bass Boost", listOf(6f, 5f, 4f, 0f, 0f, 0f, 0f, 0f, 0f, 0f), 500, 0, 1.5f, -1.0f, false, false),
            AudioProfile("rock", "Rock", listOf(4f, 3f, 2f, 0f, -1f, -1f, 0f, 1f, 2f, 3f), 200, 0, 1.2f, -0.5f, false, false),
            AudioProfile("pop", "Pop", listOf(-1f, 0f, 2f, 4f, 5f, 5f, 2f, 1f, 0f, -1f), 100, 0, 1.3f, -0.8f, true, false)
        )
    }
}
