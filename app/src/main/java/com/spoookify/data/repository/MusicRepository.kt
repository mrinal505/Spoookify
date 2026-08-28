package com.spoookify.data.repository

import com.spoookify.data.remote.Track
import com.spoookify.data.remote.YoutubeExtractor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val youtubeExtractor: YoutubeExtractor
) {
    suspend fun searchSongs(query: String, isLoadMore: Boolean = false): List<Track> {
        return youtubeExtractor.search(query, isLoadMore)
    }

    suspend fun getStreamUrl(videoId: String): String? {
        return youtubeExtractor.getAudioUrl(videoId)
    }

    suspend fun getFreshStreamUrl(videoId: String): String? {
        youtubeExtractor.evictUrl(videoId)
        return youtubeExtractor.getAudioUrl(videoId)
    }

    suspend fun getRelatedTracks(query: String): List<Track> {
        return youtubeExtractor.search("similar to $query", false)
    }

    suspend fun searchByMood(mood: String, energy: Float): List<Track> {
        val energyTag = if (energy > 0.7f) "energetic" else if (energy < 0.3f) "calm" else ""
        return youtubeExtractor.search("$mood $energyTag music", false)
    }
}
