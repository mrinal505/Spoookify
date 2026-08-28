package com.spoookify.service

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.spoookify.data.local.entity.AudioProfile
import com.spoookify.playback.AudioProfileManager
import com.spoookify.playback.CarModeManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AudioDeviceReceiver : BroadcastReceiver() {

    @Inject
    lateinit var audioProfileManager: AudioProfileManager

    @Inject
    lateinit var carModeManager: CarModeManager

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }

                if (device != null) {
                    val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                    } else true

                    if (hasPermission) {
                        try {
                            if (carModeManager.isAutomotiveDevice(device.name)) {
                                val profile = audioProfileManager.getProfileForDevice(device.name) 
                                    ?: AudioProfile("car", "Car EQ", listOf(2f, 4f, 2f, 0f, -2f, -2f, 0f, 2f, 4f, 2f))
                                audioProfileManager.setProfile(profile, device.name)
                                carModeManager.triggerCarMode(true)
                            } else {
                                carModeManager.triggerCarMode(false)
                                val profile = audioProfileManager.getProfileForDevice(device.name)
                                    ?: AudioProfile("speaker", "Speaker EQ", listOf(4f, 2f, 0f, -2f, -4f, -4f, -2f, 0f, 2f, 4f))
                                audioProfileManager.setProfile(profile, device.name)
                            }
                        } catch (e: SecurityException) {
                            // Permission might have been revoked
                        }
                    }
                }
            }
            Intent.ACTION_HEADSET_PLUG -> {
                val state = intent.getIntExtra("state", -1)
                if (state == 1) { // Plugged
                    audioProfileManager.setProfile(AudioProfile("headphones", "Headphone EQ", listOf(0f, 2f, 4f, 2f, 0f, 0f, 2f, 4f, 2f, 0f)))
                }
            }
        }
    }
}
