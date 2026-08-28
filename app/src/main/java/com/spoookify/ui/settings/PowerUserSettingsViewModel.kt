package com.spoookify.ui.settings

import androidx.lifecycle.ViewModel
import com.spoookify.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PowerUserSettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val bufferSizeMs = userPreferencesRepository.bufferSizeMs
    val cacheLimitMb = userPreferencesRepository.cacheLimitMb

    fun setBufferSize(ms: Int) = userPreferencesRepository.setBufferSize(ms)
    fun setCacheLimit(mb: Int) = userPreferencesRepository.setCacheLimit(mb)
}
