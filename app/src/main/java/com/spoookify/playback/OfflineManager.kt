package com.spoookify.playback

import android.content.Context
import com.spoookify.data.local.dao.DownloadDao
import com.spoookify.data.local.dao.FavoriteDao
import com.spoookify.data.local.entity.DownloadTrack
import com.spoookify.data.remote.Track
import com.spoookify.data.repository.MusicRepository
import com.spoookify.data.repository.UserAnalyticsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import okhttp3.OkHttpClient
import okhttp3.Request as OkRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao,
    private val favoriteDao: FavoriteDao,
    private val musicRepository: MusicRepository,
    private val analyticsRepository: UserAnalyticsRepository,
    private val userPreferencesRepository: com.spoookify.data.repository.UserPreferencesRepository
) {
    private val client = OkHttpClient()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress = _downloadProgress.asStateFlow()

    fun downloadTrack(track: Track) {
        if (_downloadProgress.value.containsKey(track.id)) return // Already in progress

        scope.launch {
            _downloadProgress.value += (track.id to 0f)
            try {
                val url = musicRepository.getStreamUrl(track.id) ?: throw Exception("Failed to get stream URL")
                val file = File(context.getExternalFilesDir(null), "${track.id}.m4a")
                
                val request = OkRequest.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw Exception("Network failure")
                    val body = response.body ?: throw Exception("Empty body")
                    val totalBytes = body.contentLength()
                    
                    body.source().use { source ->
                        file.outputStream().use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var totalRead = 0L
                            
                            while (source.inputStream().read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                totalRead += bytesRead
                                if (totalBytes > 0) {
                                    val progress = totalRead.toFloat() / totalBytes
                                    _downloadProgress.value += (track.id to progress)
                                }
                            }
                        }
                    }
                }

                val download = DownloadTrack(
                    id = track.id,
                    title = track.title,
                    artist = track.artist,
                    thumbnailUrl = track.thumbnailUrl,
                    localFilePath = file.absolutePath,
                    fileSize = file.length()
                )
                downloadDao.insertDownload(download)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _downloadProgress.value -= track.id
            }
        }
    }

    fun runSmartOffline() {
        if (!userPreferencesRepository.isSmartOfflineEnabled.value) return
        scope.launch {
            // 1. Download based on interaction levels
            favoriteDao.getAllFavorites().first().forEach { favorite ->
                if (favorite.interactionLevel >= 2) { // Favorite or Replay
                    if (downloadDao.getDownloadById(favorite.id) == null) {
                        downloadTrack(Track(favorite.id, favorite.title, favorite.artist, favorite.thumbnailUrl))
                    }
                }
            }

            // 2. Download top artists as before
            val topArtists = analyticsRepository.getTopArtists()
            topArtists.take(2).forEach { artist ->
                val tracks = musicRepository.searchSongs(artist.artist).take(5)
                tracks.forEach { track ->
                    if (downloadDao.getDownloadById(track.id) == null) {
                        downloadTrack(track)
                    }
                }
            }

            // 3. Automatic Cleanup
            cleanupOldDownloads()
        }
    }

    private suspend fun cleanupOldDownloads() {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val allDownloads = downloadDao.getAllDownloads().first()
        allDownloads.forEach { download ->
            if (download.lastPlayedAt < thirtyDaysAgo) {
                val file = File(download.localFilePath)
                if (file.exists()) file.delete()
                downloadDao.deleteDownload(download)
            }
        }
    }

    fun getDownloadDirSize(): Long {
        return context.getExternalFilesDir(null)?.listFiles()?.sumOf { it.length() } ?: 0L
    }

    fun getCacheDir(): File {
        return context.cacheDir
    }

    fun getDownloadDir(): File? {
        return context.getExternalFilesDir(null)
    }

    suspend fun isTrackDownloaded(trackId: String): Boolean {
        return downloadDao.getDownloadById(trackId) != null
    }
}
