package com.spoookify.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val audioUrl: String? = null
)

@Singleton
class YoutubeExtractor @Inject constructor() {

    private val searchCache = ConcurrentHashMap<String, List<Track>>()
    private val urlCache = ConcurrentHashMap<String, UrlCacheEntry>()
    private val extractors = ConcurrentHashMap<String, org.schabi.newpipe.extractor.search.SearchExtractor>()

    private data class UrlCacheEntry(
        val url: String,
        val timestamp: Long
    )

    private val CACHE_TIMEOUT = 5 * 60 * 60 * 1000 // 5 hours

    suspend fun search(query: String, isLoadMore: Boolean = false): List<Track> = withContext(Dispatchers.IO) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) return@withContext emptyList()

        if (!isLoadMore) {
            searchCache[trimmedQuery]?.let { return@withContext it }
        }
        
        val searchQuery = if (!trimmedQuery.lowercase().contains("song") && 
                              !trimmedQuery.lowercase().contains("music") && 
                              !trimmedQuery.lowercase().contains("audio") &&
                              !trimmedQuery.lowercase().contains("track")) {
            "$trimmedQuery song"
        } else {
            trimmedQuery
        }

        try {
            val service = ServiceList.YouTube
            val extractor = if (isLoadMore) {
                extractors[trimmedQuery] ?: service.getSearchExtractor(searchQuery).also { extractors[trimmedQuery] = it }
            } else {
                service.getSearchExtractor(searchQuery).also { extractors[trimmedQuery] = it }
            }

            if (!isLoadMore) {
                extractor.fetchPage()
            } else {
                val nextPage = extractor.initialPage.nextPage
                if (nextPage != null) {
                    extractor.fetchPage()
                } else {
                    return@withContext emptyList()
                }
            }
            
            val tracks = extractor.initialPage.items
                .filterIsInstance<StreamInfoItem>()
                .filter { item ->
                    // High-strictness filtering for "Pure Songs"
                    val title = item.name.lowercase()
                    val uploader = (item.uploaderName ?: "").lowercase()
                    val nonMusicTerms = listOf(
                        "trading", "zerodha", "nifty", "stock", "share market", "chart reading", "breakout", 
                        "grocery", "shopping", "target fresh", "dhamaka", "recipe", "cooking", "masala",
                        "tutorial", "how to", "explained", "course", "lesson", "guide", "lecture",
                        "gaming", "gameplay", "walkthrough", "review", "unboxing", "vlog", "podcast",
                        "news", "headline", "reaction", "interview", "trailer", "teaser", "promo",
                        "analysis", "strategy", "crypto", "bitcoin", "forex", "investing", "finance",
                        "short film", "movie clip", "preview", "making of", "behind the scenes", "episode",
                        "ad", "commercial", "buy now", "discount", "offer", "tech", "mobile app"
                    )
                    val isLikelyNotSong = nonMusicTerms.any { term -> title.contains(term) || uploader.contains(term) }
                    
                    // Allow songs of any duration (short tracks, long DJ mixes, full albums, live sets)
                    !isLikelyNotSong
                }
                .map { item ->
                    Track(
                        id = item.url.split("v=").last(),
                        title = item.name
                            .replace("(Official Audio)", "", ignoreCase = true)
                            .replace("(Official Video)", "", ignoreCase = true)
                            .replace("(Lyrics)", "", ignoreCase = true)
                            .replace("[Lyrics]", "", ignoreCase = true)
                            .replace("(Audio)", "", ignoreCase = true)
                            .trim(),
                        artist = item.uploaderName ?: "Unknown Artist",
                        thumbnailUrl = item.thumbnails.firstOrNull()?.url ?: ""
                    )
                }
            
            if (!isLoadMore && tracks.isNotEmpty()) {
                searchCache[trimmedQuery] = tracks
                // Pre-fetch the first result for instant play
                tracks.firstOrNull()?.let { prefetchUrl(it.id) }
            }
            tracks
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private suspend fun prefetchUrl(videoId: String) {
        withContext(Dispatchers.IO) {
            getAudioUrl(videoId)
        }
    }

    fun evictUrl(videoId: String) {
        urlCache.remove(videoId)
    }

    suspend fun getAudioUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        val cached = urlCache[videoId]
        if (cached != null && (System.currentTimeMillis() - cached.timestamp) < CACHE_TIMEOUT) {
            return@withContext cached.url
        }
        
        try {
            val service = ServiceList.YouTube
            val url = "https://www.youtube.com/watch?v=$videoId"
            val extractor = service.getStreamExtractor(url) as StreamExtractor
            extractor.fetchPage()
            
            val audioStreams = extractor.audioStreams
            // Sort by average bitrate descending to get highest quality
            val streamUrl = audioStreams
                .sortedByDescending { it.averageBitrate }
                .firstOrNull { it.format?.name == "opus" || it.format?.name == "m4a" }?.url
                ?: audioStreams.sortedByDescending { it.averageBitrate }.firstOrNull()?.url
                
            if (streamUrl != null) {
                urlCache[videoId] = UrlCacheEntry(streamUrl, System.currentTimeMillis())
            }
            streamUrl
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
