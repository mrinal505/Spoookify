package com.spoookify.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.spoookify.data.remote.Track
import com.spoookify.ui.components.PremiumTrackItem
import com.spoookify.ui.player.PlayerViewModel
import com.spoookify.ui.theme.*
import com.valentinilk.shimmer.shimmer


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    var query by remember { mutableStateOf("") }
    val searchResults by searchViewModel.searchResults.collectAsState()
    val isSearching by searchViewModel.isSearching.collectAsState()
    val recentSearches by searchViewModel.recentSearches.collectAsState()
    val recentlyPlayedAndSearched by searchViewModel.recentlyPlayedAndSearchedTracks.collectAsState()
    val downloadedTrackIds by searchViewModel.downloadedTrackIds.collectAsState()
    val downloadProgress by searchViewModel.downloadProgress.collectAsState()
    val selectedFilter by searchViewModel.selectedFilter.collectAsState()
    val currentTrack by playerViewModel.currentTrack.collectAsState()

    val currentTheme = LocalAppTheme.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(currentTheme.surface, currentTheme.background)
                )
            )
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Search",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(bottom = 16.dp),
            letterSpacing = (-0.5).sp
        )

        // Glassmorphic Search Bar
        TextField(
            value = query,
            onValueChange = { 
                query = it
                searchViewModel.search(it)
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("What do you want to listen to?", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = currentTheme.primary) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        query = ""
                        searchViewModel.search("")
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = GlassSurface,
                unfocusedContainerColor = GlassSurface,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isSearching) {
            SearchShimmer()
        } else if (query.isEmpty()) {
            if (recentSearches.isEmpty() && recentlyPlayedAndSearched.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Play what you love",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Search for artists, songs, or albums.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                if (recentSearches.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Searches",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { searchViewModel.clearAllRecentSearches() }) {
                            Text("Clear all", color = currentTheme.primary, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 140.dp).padding(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(recentSearches, key = { it }) { term ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = GlassSurface
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable { 
                                            query = term
                                            searchViewModel.search(term)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Default.History, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(text = term, color = Color.White, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                    }
                                    IconButton(
                                        onClick = { searchViewModel.removeRecentSearch(term) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                if (recentlyPlayedAndSearched.isNotEmpty()) {
                    Text(
                        text = "Recently Played Songs",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        items(recentlyPlayedAndSearched, key = { it.id }) { track ->
                            val isDownloaded = downloadedTrackIds.contains(track.id)
                            PremiumTrackItem(
                                track = track,
                                isDownloaded = isDownloaded,
                                isPlaying = track.id == currentTrack?.id,
                                onClick = { playerViewModel.playTrack(track, recentlyPlayedAndSearched) }
                            )
                        }
                    }
                }
            }
        } else if (searchResults.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No tracks found for '$query'", color = Color.Gray, style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            // Filter Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf("All", "Downloaded")
                filters.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { searchViewModel.setFilter(filter) },
                        label = { Text(filter) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SpotifyGreen,
                            selectedLabelColor = Color.Black,
                            containerColor = GlassSurface,
                            labelColor = Color.White
                        ),
                        border = null,
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            val filteredList = remember(searchResults, selectedFilter, downloadedTrackIds) {
                if (selectedFilter == "Downloaded") {
                    searchResults.filter { downloadedTrackIds.contains(it.id) }
                } else searchResults
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(filteredList, key = { it.id }) { track ->
                    val isDownloaded = downloadedTrackIds.contains(track.id)
                    val progress = downloadProgress[track.id]

                    PremiumTrackItem(
                        track = track,
                        isDownloaded = isDownloaded,
                        isPlaying = track.id == currentTrack?.id,
                        onClick = { playerViewModel.playTrack(track, filteredList) }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (progress != null) {
                                CircularProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.size(20.dp),
                                    color = SpotifyGreen,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                IconButton(
                                    onClick = { searchViewModel.downloadTrack(track) },
                                    enabled = !isDownloaded
                                ) {
                                    Icon(
                                        imageVector = if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                                        contentDescription = null,
                                        tint = if (isDownloaded) SpotifyGreen else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}



@Composable
fun SearchShimmer() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(7) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .shimmer()
                    .background(GlassSurface, RoundedCornerShape(14.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {}
        }
    }
}

