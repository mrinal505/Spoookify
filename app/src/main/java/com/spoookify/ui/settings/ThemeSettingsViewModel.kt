package com.spoookify.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import com.spoookify.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class ThemeSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    val authManager: com.spoookify.auth.AuthManager,
    private val cloudSyncRepository: com.spoookify.data.repository.CloudSyncRepository,
    private val favoriteDao: com.spoookify.data.local.dao.FavoriteDao,
    private val dynamicPlaylistDao: com.spoookify.data.local.dao.DynamicPlaylistDao
) : ViewModel() {

    val isAmoledBlack = userPreferencesRepository.isAmoledBlack
    val appTheme = userPreferencesRepository.appTheme
    val useDynamicColors = userPreferencesRepository.useDynamicColors
    val animationIntensity = userPreferencesRepository.animationIntensity
    val appScaleMode = userPreferencesRepository.appScaleMode
    val isSmartOfflineEnabled = userPreferencesRepository.isSmartOfflineEnabled

    val audioBitrate = userPreferencesRepository.audioBitrate
    val isDataSaverEnabled = userPreferencesRepository.isDataSaverEnabled
    val crossfadeSeconds = userPreferencesRepository.crossfadeSeconds
    val skipIntervalSeconds = userPreferencesRepository.skipIntervalSeconds
    val autoAudioModeEnabled = userPreferencesRepository.autoAudioModeEnabled
    val sleepTimerMinutes = userPreferencesRepository.sleepTimerMinutes
    val bufferSizeMs = userPreferencesRepository.bufferSizeMs
    val cacheLimitMb = userPreferencesRepository.cacheLimitMb

    private val _currentScaleFactor = MutableStateFlow(1.0f)
    val currentScaleFactor = _currentScaleFactor.asStateFlow()

    init {
        calculateScaleFactor()
    }

    private fun calculateScaleFactor() {
        val displayMetrics = context.resources.displayMetrics
        val widthPixels = displayMetrics.widthPixels
        
        val baseWidth = 1080f
        val calculated = widthPixels / baseWidth
        
        _currentScaleFactor.value = when (appScaleMode.value) {
            "compact" -> calculated * 0.85f
            "comfortable" -> calculated * 1.15f
            else -> calculated
        }.coerceIn(0.7f, 1.5f)
    }

    fun triggerCloudSync(onResult: (Int, Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val favs = favoriteDao.getFavoritesList()
            val playlists = dynamicPlaylistDao.getPlaylistsList()
            cloudSyncRepository.syncLocalToCloud()
            cloudSyncRepository.syncCloudToLocal()
            withContext(Dispatchers.Main) {
                onResult(favs.size, playlists.size)
            }
        }
    }

    fun setAmoledBlack(enabled: Boolean) = userPreferencesRepository.setAmoledBlack(enabled)
    fun setAppTheme(theme: com.spoookify.ui.theme.AppTheme) = userPreferencesRepository.setAppTheme(theme)
    fun setDynamicColors(enabled: Boolean) = userPreferencesRepository.setDynamicColors(enabled)
    fun setAnimationIntensity(intensity: Float) = userPreferencesRepository.setAnimationIntensity(intensity)
    fun setSmartOfflineEnabled(enabled: Boolean) = userPreferencesRepository.setSmartOfflineEnabled(enabled)
    fun setAudioBitrate(bitrate: String) = userPreferencesRepository.setAudioBitrate(bitrate)
    fun setDataSaverEnabled(enabled: Boolean) = userPreferencesRepository.setDataSaverEnabled(enabled)
    fun setCrossfadeSeconds(sec: Int) = userPreferencesRepository.setCrossfadeSeconds(sec)
    fun setSkipIntervalSeconds(sec: Int) = userPreferencesRepository.setSkipIntervalSeconds(sec)
    fun setAutoAudioModeEnabled(enabled: Boolean) = userPreferencesRepository.setAutoAudioModeEnabled(enabled)
    fun setSleepTimerMinutes(minutes: Int) = userPreferencesRepository.setSleepTimerMinutes(minutes)
    fun setBufferSize(ms: Int) = userPreferencesRepository.setBufferSize(ms)
    fun setCacheLimit(mb: Int) = userPreferencesRepository.setCacheLimit(mb)
    
    fun setAppScaleMode(mode: String) {
        userPreferencesRepository.setAppScaleMode(mode)
        calculateScaleFactor()
    }
}
