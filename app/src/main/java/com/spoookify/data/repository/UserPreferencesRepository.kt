package com.spoookify.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("spoookify_prefs", Context.MODE_PRIVATE)

    private val _isGuestMode = MutableStateFlow(prefs.getBoolean("is_guest_mode", false))
    val isGuestMode = _isGuestMode.asStateFlow()

    fun setGuestMode(enabled: Boolean) {
        _isGuestMode.value = enabled
        prefs.edit().putBoolean("is_guest_mode", enabled).apply()
    }

    private val _isAmoledBlack = MutableStateFlow(prefs.getBoolean("amoled_black", false))
    val isAmoledBlack = _isAmoledBlack.asStateFlow()

    private val _appTheme = MutableStateFlow(parseAppTheme(prefs.getString("app_theme_id", com.spoookify.ui.theme.AppTheme.SPOTIFY_GREEN.id)))
    val appTheme = _appTheme.asStateFlow()

    private fun parseAppTheme(id: String?): com.spoookify.ui.theme.AppTheme {
        return com.spoookify.ui.theme.AppTheme.values().firstOrNull { it.id == id } ?: com.spoookify.ui.theme.AppTheme.SPOTIFY_GREEN
    }

    fun setAppTheme(theme: com.spoookify.ui.theme.AppTheme) {
        _appTheme.value = theme
        prefs.edit().putString("app_theme_id", theme.id).apply()
    }

    private val _useDynamicColors = MutableStateFlow(prefs.getBoolean("dynamic_colors", false))
    val useDynamicColors = _useDynamicColors.asStateFlow()

    private val _animationIntensity = MutableStateFlow(prefs.getFloat("animation_intensity", 1.0f))
    val animationIntensity = _animationIntensity.asStateFlow()

    private val _appScaleMode = MutableStateFlow(prefs.getString("app_scale_mode", "auto") ?: "auto")
    val appScaleMode = _appScaleMode.asStateFlow()

    private val _bufferSizeMs = MutableStateFlow(prefs.getInt("buffer_size_ms", 30000))
    val bufferSizeMs = _bufferSizeMs.asStateFlow()

    private val _cacheLimitMb = MutableStateFlow(prefs.getInt("cache_limit_mb", 512))
    val cacheLimitMb = _cacheLimitMb.asStateFlow()

    private val _isSmartOfflineEnabled = MutableStateFlow(prefs.getBoolean("smart_offline", false))
    val isSmartOfflineEnabled = _isSmartOfflineEnabled.asStateFlow()

    private val _homeModules = MutableStateFlow(parseHomeModules(prefs.getString("home_modules", null)))
    val homeModules = _homeModules.asStateFlow()

    private val _audioBitrate = MutableStateFlow(prefs.getString("audio_bitrate", "320kbps") ?: "320kbps")
    val audioBitrate = _audioBitrate.asStateFlow()

    private val _isDataSaverEnabled = MutableStateFlow(prefs.getBoolean("data_saver", false))
    val isDataSaverEnabled = _isDataSaverEnabled.asStateFlow()

    private val _crossfadeSeconds = MutableStateFlow(prefs.getInt("crossfade_sec", 2))
    val crossfadeSeconds = _crossfadeSeconds.asStateFlow()

    private val _skipIntervalSeconds = MutableStateFlow(prefs.getInt("skip_interval_sec", 10))
    val skipIntervalSeconds = _skipIntervalSeconds.asStateFlow()

    private val _autoAudioModeEnabled = MutableStateFlow(prefs.getBoolean("auto_audio_mode", true))
    val autoAudioModeEnabled = _autoAudioModeEnabled.asStateFlow()

    private val _sleepTimerMinutes = MutableStateFlow(prefs.getInt("sleep_timer_min", 0))
    val sleepTimerMinutes = _sleepTimerMinutes.asStateFlow()

    private val _sleepTimerEndTime = MutableStateFlow(prefs.getLong("sleep_timer_end", 0L))
    val sleepTimerEndTime = _sleepTimerEndTime.asStateFlow()

    private fun parseHomeModules(csv: String?): List<com.spoookify.ui.home.HomeModule> {
        val defaultModules = listOf(
            com.spoookify.ui.home.HomeModule.ContinueListening,
            com.spoookify.ui.home.HomeModule.QuickMix,
            com.spoookify.ui.home.HomeModule.Trending,
            com.spoookify.ui.home.HomeModule.Recommended
        )
        if (csv.isNullOrBlank()) return defaultModules
        val parsed = csv.split(",").mapNotNull { 
            try { com.spoookify.ui.home.HomeModule.valueOf(it) } catch(_: Exception) { null }
        }.filter { it != com.spoookify.ui.home.HomeModule.RecentlyPlayed && it != com.spoookify.ui.home.HomeModule.NewReleases }
        return if (parsed.isEmpty()) defaultModules else parsed
    }

    fun setHomeModules(modules: List<com.spoookify.ui.home.HomeModule>) {
        _homeModules.value = modules
        prefs.edit().putString("home_modules", modules.joinToString(",") { it.name }).apply()
    }

    fun setAmoledBlack(enabled: Boolean) {
        _isAmoledBlack.value = enabled
        prefs.edit().putBoolean("amoled_black", enabled).apply()
    }

    fun setDynamicColors(enabled: Boolean) {
        _useDynamicColors.value = enabled
        prefs.edit().putBoolean("dynamic_colors", enabled).apply()
    }

    fun setAnimationIntensity(intensity: Float) {
        _animationIntensity.value = intensity
        prefs.edit().putFloat("animation_intensity", intensity).apply()
    }

    fun setAppScaleMode(mode: String) {
        _appScaleMode.value = mode
        prefs.edit().putString("app_scale_mode", mode).apply()
    }

    fun setBufferSize(ms: Int) {
        _bufferSizeMs.value = ms
        prefs.edit().putInt("buffer_size_ms", ms).apply()
    }

    fun setCacheLimit(mb: Int) {
        _cacheLimitMb.value = mb
        prefs.edit().putInt("cache_limit_mb", mb).apply()
    }

    fun setSmartOfflineEnabled(enabled: Boolean) {
        _isSmartOfflineEnabled.value = enabled
        prefs.edit().putBoolean("smart_offline", enabled).apply()
    }

    fun setAudioBitrate(bitrate: String) {
        _audioBitrate.value = bitrate
        prefs.edit().putString("audio_bitrate", bitrate).apply()
    }

    fun setDataSaverEnabled(enabled: Boolean) {
        _isDataSaverEnabled.value = enabled
        if (enabled) {
            _audioBitrate.value = "96kbps"
            prefs.edit().putString("audio_bitrate", "96kbps").apply()
        } else {
            _audioBitrate.value = "320kbps"
            prefs.edit().putString("audio_bitrate", "320kbps").apply()
        }
        prefs.edit().putBoolean("data_saver", enabled).apply()
    }

    fun setCrossfadeSeconds(sec: Int) {
        _crossfadeSeconds.value = sec
        prefs.edit().putInt("crossfade_sec", sec).apply()
    }

    fun setSkipIntervalSeconds(sec: Int) {
        _skipIntervalSeconds.value = sec
        prefs.edit().putInt("skip_interval_sec", sec).apply()
    }

    fun setAutoAudioModeEnabled(enabled: Boolean) {
        _autoAudioModeEnabled.value = enabled
        prefs.edit().putBoolean("auto_audio_mode", enabled).apply()
    }

    fun setSleepTimerMinutes(minutes: Int) {
        _sleepTimerMinutes.value = minutes
        val endTime = if (minutes > 0) System.currentTimeMillis() + (minutes * 60 * 1000L) else 0L
        _sleepTimerEndTime.value = endTime
        prefs.edit().putInt("sleep_timer_min", minutes).putLong("sleep_timer_end", endTime).apply()
    }
}
