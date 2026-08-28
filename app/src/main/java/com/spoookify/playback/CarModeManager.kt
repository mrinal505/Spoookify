package com.spoookify.playback

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CarModeManager @Inject constructor() {
    private val _carModeEvents = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val carModeEvents = _carModeEvents.asSharedFlow()

    private val _isCarConnected = MutableStateFlow(false)
    val isCarConnected = _isCarConnected.asStateFlow()

    fun triggerCarMode(active: Boolean) {
        _isCarConnected.value = active
        _carModeEvents.tryEmit(active)
    }

    fun isAutomotiveDevice(name: String?): Boolean {
        if (name == null) return false
        val keywords = listOf("car", "toyota", "honda", "ford", "chevrolet", "nissan", "hyundai", "bmw", "audi", "mercedes", "tesla", "volkswagen", "mazda", "subaru", "kia")
        return keywords.any { name.contains(it, ignoreCase = true) }
    }
}
