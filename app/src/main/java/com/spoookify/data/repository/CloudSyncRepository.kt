package com.spoookify.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.spoookify.auth.AuthManager
import com.spoookify.data.local.dao.FavoriteDao
import com.spoookify.data.local.dao.DynamicPlaylistDao
import com.spoookify.data.local.entity.FavoriteTrack
import com.spoookify.data.local.entity.DynamicPlaylist
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudSyncRepository @Inject constructor(
    private val authManager: AuthManager,
    private val favoriteDao: FavoriteDao,
    private val dynamicPlaylistDao: DynamicPlaylistDao
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val firestore: FirebaseFirestore? = try {
        FirebaseFirestore.getInstance()
    } catch (e: Throwable) {
        null
    }

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(0L)
    val lastSyncTime = _lastSyncTime.asStateFlow()

    init {
        scope.launch {
            authManager.currentUser.collect { user ->
                if (user.isSignedIn) {
                    syncCloudToLocal()
                    syncLocalToCloud()
                }
            }
        }
        scope.launch {
            kotlinx.coroutines.flow.combine(
                favoriteDao.getAllFavorites(),
                dynamicPlaylistDao.getAllDynamicPlaylists()
            ) { favs, playlists ->
                favs.size to playlists.size
            }.collect {
                if (authManager.currentUser.value.isSignedIn) {
                    syncLocalToCloud()
                }
            }
        }
    }

    suspend fun syncLocalToCloud() = withContext(Dispatchers.IO) {
        val user = authManager.currentUser.value
        if (!user.isSignedIn || firestore == null) return@withContext

        _isSyncing.value = true
        try {
            val favorites = favoriteDao.getFavoritesList()
            val playlists = dynamicPlaylistDao.getPlaylistsList()

            val userDoc = firestore.collection("users").document(user.uid)

            val favList = favorites.map {
                mapOf(
                    "id" to it.id,
                    "title" to it.title,
                    "artist" to it.artist,
                    "thumbnailUrl" to it.thumbnailUrl,
                    "interactionLevel" to it.interactionLevel
                )
            }
            userDoc.collection("data").document("favorites").set(mapOf("tracks" to favList))
                .addOnFailureListener { e -> e.printStackTrace() }

            val playlistList = playlists.map {
                mapOf(
                    "id" to it.id,
                    "name" to it.name,
                    "description" to it.description,
                    "rulesJson" to it.rulesJson
                )
            }
            userDoc.collection("data").document("playlists").set(mapOf("playlists" to playlistList))
                .addOnFailureListener { e -> e.printStackTrace() }

            _lastSyncTime.value = System.currentTimeMillis()
        } catch (e: Throwable) {
            e.printStackTrace()
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun syncCloudToLocal() = withContext(Dispatchers.IO) {
        val user = authManager.currentUser.value
        if (!user.isSignedIn || firestore == null) return@withContext

        _isSyncing.value = true
        try {
            val userDoc = firestore.collection("users").document(user.uid)

            userDoc.collection("data").document("favorites").get().addOnSuccessListener { snapshot ->
                @Suppress("UNCHECKED_CAST")
                val tracks = snapshot.get("tracks") as? List<Map<String, Any>> ?: return@addOnSuccessListener
                scope.launch {
                    tracks.forEach { item ->
                        val id = item["id"] as? String ?: return@forEach
                        val fav = FavoriteTrack(
                            id = id,
                            title = item["title"] as? String ?: "",
                            artist = item["artist"] as? String ?: "",
                            thumbnailUrl = item["thumbnailUrl"] as? String ?: "",
                            interactionLevel = (item["interactionLevel"] as? Long)?.toInt() ?: 1
                        )
                        favoriteDao.insertFavorite(fav)
                    }
                }
            }.addOnFailureListener { e -> e.printStackTrace() }

            userDoc.collection("data").document("playlists").get().addOnSuccessListener { snapshot ->
                @Suppress("UNCHECKED_CAST")
                val playlists = snapshot.get("playlists") as? List<Map<String, Any>> ?: return@addOnSuccessListener
                scope.launch {
                    playlists.forEach { item ->
                        val id = item["id"] as? String ?: return@forEach
                        val playlist = DynamicPlaylist(
                            id = id,
                            name = item["name"] as? String ?: "Playlist",
                            description = item["description"] as? String ?: "",
                            rulesJson = item["rulesJson"] as? String ?: "[]"
                        )
                        dynamicPlaylistDao.insertPlaylist(playlist)
                    }
                }
            }.addOnFailureListener { e -> e.printStackTrace() }

            _lastSyncTime.value = System.currentTimeMillis()
        } catch (e: Throwable) {
            e.printStackTrace()
        } finally {
            _isSyncing.value = false
        }
    }
}
