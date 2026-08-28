package com.spoookify.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spoookify.data.local.dao.DownloadDao
import com.spoookify.data.local.entity.DownloadTrack
import com.spoookify.playback.OfflineManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StorageManagerViewModel @Inject constructor(
    private val downloadDao: DownloadDao,
    private val offlineManager: OfflineManager
) : ViewModel() {

    val downloads: StateFlow<List<DownloadTrack>> = downloadDao.getAllDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteDownload(track: DownloadTrack) {
        viewModelScope.launch {
            downloadDao.deleteDownload(track)
            // Actual file deletion should also happen here
        }
    }

    fun getStorageInfo(): StorageInfo {
        val totalDownloadSize = offlineManager.getDownloadDirSize()
        val cacheSize = getCacheSize()
        return StorageInfo(
            musicSize = totalDownloadSize,
            cacheSize = cacheSize,
            recommendToRemove = if (totalDownloadSize > 1000 * 1024 * 1024) 500 * 1024 * 1024L else 0L
        )
    }

    private fun getCacheSize(): Long {
        val cacheDir = offlineManager.getCacheDir()
        return getDirSize(cacheDir)
    }

    private fun getDirSize(dir: java.io.File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) getDirSize(file) else file.length()
        }
        return size
    }
    fun clearCache() {
        viewModelScope.launch {
            val cacheDir = offlineManager.getCacheDir()
            cacheDir?.listFiles()?.forEach { it.deleteRecursively() }
        }
    }

    fun clearAllDownloads() {
        viewModelScope.launch {
            downloadDao.clearAllDownloads()
            val downloadDir = offlineManager.getDownloadDir()
            downloadDir?.listFiles()?.forEach { it.deleteRecursively() }
        }
    }
}

data class StorageInfo(
    val musicSize: Long,
    val cacheSize: Long,
    val recommendToRemove: Long
)
