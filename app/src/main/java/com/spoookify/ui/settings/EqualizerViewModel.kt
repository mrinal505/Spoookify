package com.spoookify.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spoookify.data.local.entity.AudioProfile
import com.spoookify.playback.AudioProfileManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EqualizerViewModel @Inject constructor(
    private val audioProfileManager: AudioProfileManager
) : ViewModel() {

    val currentProfile = audioProfileManager.currentProfile
    val isNormalizationEnabled = audioProfileManager.isNormalizationEnabled
    val playbackSpeed = audioProfileManager.playbackSpeed
    val balance = audioProfileManager.balance
    val isMono = audioProfileManager.isMonoEnabled

    val presets = audioProfileManager.getDefaultProfiles()

    init {
        if (currentProfile.value == null) {
            audioProfileManager.setProfile(presets[0])
        }
    }

    fun updateBand(index: Int, gain: Float) {
        val current = currentProfile.value ?: return
        if (index !in current.bands.indices) return
        val newBands = current.bands.toMutableList().apply {
            this[index] = gain
        }
        val updated = current.copy(bands = newBands, isCustom = true)
        audioProfileManager.setProfile(updated)
        viewModelScope.launch { audioProfileManager.saveProfile(updated) }
    }

    fun updateCompressorRatio(ratio: Float) {
        val current = currentProfile.value ?: return
        val updated = current.copy(compressorRatio = ratio, isCustom = true)
        audioProfileManager.setProfile(updated)
        viewModelScope.launch { audioProfileManager.saveProfile(updated) }
    }

    fun updateLimiterThreshold(threshold: Float) {
        val current = currentProfile.value ?: return
        val updated = current.copy(limiterThreshold = threshold, isCustom = true)
        audioProfileManager.setProfile(updated)
        viewModelScope.launch { audioProfileManager.saveProfile(updated) }
    }

    fun updateVirtualizerStrength(strength: Int) {
        val current = currentProfile.value ?: return
        val updated = current.copy(virtualizerStrength = strength, isCustom = true)
        audioProfileManager.setProfile(updated)
        viewModelScope.launch { audioProfileManager.saveProfile(updated) }
    }

    fun updateLoudnessNormalization(enabled: Boolean) {
        val current = currentProfile.value ?: return
        val updated = current.copy(isLoudnessNormalizationEnabled = enabled, isCustom = true)
        audioProfileManager.setProfile(updated)
        viewModelScope.launch { audioProfileManager.saveProfile(updated) }
    }

    fun setProfile(profile: AudioProfile) {
        audioProfileManager.setProfile(profile)
        viewModelScope.launch { audioProfileManager.saveProfile(profile) }
    }

    fun setNormalization(enabled: Boolean) {
        audioProfileManager.setNormalization(enabled)
    }

    fun setPlaybackSpeed(speed: Float) {
        audioProfileManager.setPlaybackSpeed(speed)
    }

    fun setBalance(balance: Float) {
        audioProfileManager.setBalance(balance)
    }

    fun setMono(enabled: Boolean) {
        audioProfileManager.setMono(enabled)
    }
}
