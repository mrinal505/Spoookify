package com.spoookify.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import coil.compose.AsyncImage
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.spoookify.data.remote.Track
import com.spoookify.ui.components.PremiumSectionHeader
import com.spoookify.ui.components.PremiumTrackItem
import com.spoookify.ui.player.PlayerViewModel
import com.spoookify.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onSearchClick: () -> Unit,
    onAddClick: () -> Unit = {},
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val favoriteTracks by libraryViewModel.favoriteTracks.collectAsState()
    val downloadedTracks by libraryViewModel.downloadedTracks.collectAsState()
    val downloadedTrackIds by libraryViewModel.downloadedTrackIds.collectAsState()
    val dynamicPlaylists by libraryViewModel.dynamicPlaylists.collectAsState()
    val selectedTab by libraryViewModel.selectedTab.collectAsState()
    val currentTrack by playerViewModel.currentTrack.collectAsState()

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    var showCustomYearDialog by remember { mutableStateOf(false) }
    var customYearText by remember { mutableStateOf("") }
    var isYearLoading by remember { mutableStateOf(false) }

    val currentTheme = LocalAppTheme.current

    val themeViewModel: com.spoookify.ui.settings.ThemeSettingsViewModel = hiltViewModel()
    val currentUser by themeViewModel.authManager.currentUser.collectAsState()

    var promptFeature by remember { mutableStateOf<Pair<String, String>?>(null) }

    val googleAuthLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                if (account != null) {
                    themeViewModel.authManager.handleGoogleAccount(account) { _, name ->
                        // Signed in
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    promptFeature?.let { (featureName, featureDesc) ->
        com.spoookify.ui.components.GoogleSignInPromptDialog(
            featureName = featureName,
            featureDescription = featureDesc,
            onDismiss = { promptFeature = null },
            onSignInClick = {
                googleAuthLauncher.launch(themeViewModel.authManager.getSignInIntent())
            }
        )
    }

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
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = com.spoookify.R.mipmap.ic_launcher_round,
                    contentDescription = "App Icon",
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Your Library",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )
            }
            Row {
                IconButton(onClick = onSearchClick) { 
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White) 
                }
                IconButton(onClick = { 
                    if (!currentUser.isSignedIn) {
                        promptFeature = "Playlists" to "Create & manage custom playlists synced across all your devices."
                    } else {
                        showCreatePlaylistDialog = true 
                    }
                }) { 
                    Icon(Icons.Default.Add, contentDescription = "Add Playlist", tint = Color.White) 
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tabs Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf("All", "Liked Songs", "Downloaded", "Playlists", "Time Machine")
            items(tabs) { tab ->
                FilterChip(
                    selected = selectedTab == tab,
                    onClick = { 
                        when (tab) {
                            "Liked Songs" -> {
                                if (!currentUser.isSignedIn) {
                                    promptFeature = "Liked Songs" to "Save your favorite tracks and build your personal collection."
                                } else {
                                    libraryViewModel.setSelectedTab(tab)
                                }
                            }
                            "Downloaded" -> {
                                if (!currentUser.isSignedIn) {
                                    promptFeature = "Offline Downloads" to "Download high-quality audio tracks for offline playback anytime."
                                } else {
                                    libraryViewModel.setSelectedTab(tab)
                                }
                            }
                            "Playlists" -> {
                                if (!currentUser.isSignedIn) {
                                    promptFeature = "Playlists" to "Create & manage custom playlists synced across all your devices."
                                } else {
                                    libraryViewModel.setSelectedTab(tab)
                                }
                            }
                            "Time Machine" -> {
                                if (!currentUser.isSignedIn) {
                                    promptFeature = "Music Insights" to "Explore retro music time machines and your personalized listening insights."
                                } else {
                                    libraryViewModel.setSelectedTab(tab)
                                }
                            }
                            else -> libraryViewModel.setSelectedTab(tab)
                        }
                    },
                    label = { Text(tab, fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = currentTheme.primary,
                        selectedLabelColor = Color.Black,
                        containerColor = GlassSurface,
                        labelColor = Color.White
                    ),
                    border = null,
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (selectedTab == "All" || selectedTab == "Time Machine") {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GlassSurface, RoundedCornerShape(18.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = currentTheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Music Time Machine", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            if (isYearLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = currentTheme.primary, strokeWidth = 2.dp)
                            }
                        }
                        Text("Reconstruct top hit songs from any year", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Spacer(modifier = Modifier.height(12.dp))
                        val years = listOf("2024", "2020", "2015", "2010", "2000", "1990", "1980")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(years) { year ->
                                Button(
                                    onClick = {
                                        isYearLoading = true
                                        libraryViewModel.playTimeMachineYear(year) { first, list ->
                                            playerViewModel.playTrack(first, list)
                                            isYearLoading = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(year, color = currentTheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                            item {
                                Button(
                                    onClick = { showCustomYearDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = currentTheme.primary),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Browse Year", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
            if (selectedTab == "All" || selectedTab == "Liked Songs") {
                item {
                    LikedSongsCard(
                        count = favoriteTracks.size,
                        onPlayAll = {
                            if (favoriteTracks.isNotEmpty()) {
                                playerViewModel.playTrack(favoriteTracks.first(), favoriteTracks)
                            }
                        },
                        onShuffle = {
                            if (favoriteTracks.isNotEmpty()) {
                                playerViewModel.playTrack(favoriteTracks.shuffled().first(), favoriteTracks)
                            }
                        }
                    )
                }
            }

            if (selectedTab == "All" || selectedTab == "Liked Songs") {
                if (favoriteTracks.isNotEmpty()) {
                    item {
                        PremiumSectionHeader("Liked Tracks (${favoriteTracks.size})")
                    }
                    items(favoriteTracks, key = { "fav_${it.id}" }) { track ->
                        PremiumTrackItem(
                            track = track, 
                            isDownloaded = downloadedTrackIds.contains(track.id),
                            isPlaying = track.id == currentTrack?.id,
                            onClick = { playerViewModel.playTrack(track, favoriteTracks) }
                        )
                    }
                }
            }

            if (selectedTab == "All" || selectedTab == "Downloaded") {
                if (downloadedTracks.isNotEmpty()) {
                    item {
                        PremiumSectionHeader("Offline Downloads (${downloadedTracks.size})")
                    }
                    items(downloadedTracks, key = { "dl_${it.id}" }) { track ->
                        PremiumTrackItem(
                            track = track,
                            isDownloaded = true,
                            isPlaying = track.id == currentTrack?.id,
                            onClick = { playerViewModel.playTrack(track, downloadedTracks) }
                        )
                    }
                }
            }

            if (selectedTab == "All" || selectedTab == "Playlists") {
                if (dynamicPlaylists.isNotEmpty()) {
                    item {
                        PremiumSectionHeader("Custom Playlists")
                    }
                    items(dynamicPlaylists, key = { "pl_${it.id}" }) { playlist ->
                        PlaylistRowItem(title = playlist.name, subtitle = playlist.description)
                    }
                }
            }

            if (favoriteTracks.isEmpty() && downloadedTracks.isEmpty() && dynamicPlaylists.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "Your library is empty", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Search for tracks and add them to favorites", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onSearchClick,
                                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen)
                            ) {
                                Text("Explore Songs", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("Create Playlist", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SpotifyGreen,
                        focusedLabelColor = SpotifyGreen,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        libraryViewModel.createPlaylist(newPlaylistName)
                        newPlaylistName = ""
                        showCreatePlaylistDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen)
                ) {
                    Text("Create", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = CardSurface
        )
    }

    if (showCustomYearDialog) {
        AlertDialog(
            onDismissRequest = { showCustomYearDialog = false },
            title = { Text("Browse Music Year", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter any year to play top hit songs from that time:", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customYearText,
                        onValueChange = { customYearText = it.take(4) },
                        label = { Text("Year (e.g. 2000)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = currentTheme.primary,
                            focusedLabelColor = currentTheme.primary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val year = customYearText.trim()
                        if (year.isNotEmpty()) {
                            isYearLoading = true
                            libraryViewModel.playTimeMachineYear(year) { first, list ->
                                playerViewModel.playTrack(first, list)
                                isYearLoading = false
                            }
                            showCustomYearDialog = false
                            customYearText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = currentTheme.primary)
                ) {
                    Text("Play Hits", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomYearDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = CardSurface
        )
    }
}

@Composable
fun LikedSongsCard(count: Int, onPlayAll: () -> Unit, onShuffle: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF450AF5), Color(0xFF8E24AA), SpotifyGreen)
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Liked Songs", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                        Text("$count tracks saved", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onShuffle,
                        modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape).size(40.dp)
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = onPlayAll,
                        modifier = Modifier.background(SpotifyGreen, CircleShape).size(40.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play All", tint = Color.Black, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistRowItem(title: String, subtitle: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = GlassSurface
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(10.dp),
                color = ElectricPurple.copy(alpha = 0.3f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = SpotifyGreen)
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

