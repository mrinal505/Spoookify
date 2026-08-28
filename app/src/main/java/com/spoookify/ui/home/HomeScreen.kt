package com.spoookify.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.spoookify.data.remote.Track
import com.spoookify.ui.components.GlassCard
import com.spoookify.ui.components.PremiumSectionHeader
import com.spoookify.ui.components.PremiumTrackItem
import com.spoookify.ui.player.PlayerViewModel
import com.spoookify.ui.theme.*
import com.valentinilk.shimmer.shimmer
import java.util.Calendar

import androidx.compose.material3.pulltorefresh.PullToRefreshBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSettingsClick: () -> Unit,
    onStatsClick: () -> Unit,
    onCustomizeClick: () -> Unit,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val rediscoverTracks by homeViewModel.rediscoverTracks.collectAsState()
    val trendingTracks by homeViewModel.trendingTracks.collectAsState()
    val recommendedTracks by homeViewModel.recommendedTracks.collectAsState()
    val recentlyPlayed by homeViewModel.recentlyPlayed.collectAsState()
    val featuredHeroTrack by homeViewModel.featuredHeroTrack.collectAsState()
    val isHomeLoading by homeViewModel.isHomeLoading.collectAsState()
    val isRefreshing by homeViewModel.isRefreshing.collectAsState()
    val homeModules by homeViewModel.homeModules.collectAsState()
    val downloadedTrackIds by homeViewModel.downloadedTrackIds.collectAsState()
    val currentTrack by playerViewModel.currentTrack.collectAsState()
    
    val listState = rememberLazyListState()

    val currentTheme = LocalAppTheme.current

    val themeViewModel: com.spoookify.ui.settings.ThemeSettingsViewModel = hiltViewModel()
    val currentUser by themeViewModel.authManager.currentUser.collectAsState()

    val baseGreeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    val userFirstName = remember(currentUser) {
        if (currentUser.isSignedIn && !currentUser.displayName.isNullOrBlank()) {
            val name = currentUser.displayName?.trim()?.split(" ")?.firstOrNull() ?: ""
            if (name.isNotBlank() && name != "Google" && name != "Guest") name else ""
        } else {
            ""
        }
    }

    var promptFeature by remember { mutableStateOf<Pair<String, String>?>(null) }

    val googleAuthLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                if (account != null) {
                    themeViewModel.authManager.handleGoogleAccount(account) { _, _ -> }
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        currentTheme.topGradientStart,
                        currentTheme.topGradientEnd,
                        currentTheme.background
                    ),
                    startY = 0f,
                    endY = 800f
                )
            )
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { homeViewModel.refreshHomeData(isUserPull = true) },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp)
            ) {
            // Header Bar
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = baseGreeting,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        )
                        if (userFirstName.isNotBlank()) {
                            Text(
                                text = userFirstName,
                                style = MaterialTheme.typography.titleMedium,
                                color = LocalAppTheme.current.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Music & Recommendations",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (!currentUser.isSignedIn) {
                                    promptFeature = "Customize Home Screen" to "Reorder sections, pin genres, and arrange your home feed."
                                } else {
                                    onCustomizeClick()
                                }
                            },
                            modifier = Modifier
                                .background(GlassSurface, CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(Icons.Default.DashboardCustomize, contentDescription = "Customize", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        IconButton(
                            onClick = {
                                if (!currentUser.isSignedIn) {
                                    promptFeature = "Music Insights" to "Explore retro music time machines and your personalized listening insights."
                                } else {
                                    onStatsClick()
                                }
                            },
                            modifier = Modifier
                                .background(GlassSurface, CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Stats", tint = SpotifyGreen, modifier = Modifier.size(20.dp))
                        }
                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier
                                .background(GlassSurface, CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            if (isHomeLoading) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                        HomeShimmer()
                    }
                }
            } else {
                // Hero Track Banner
                featuredHeroTrack?.let { hero ->
                    item {
                        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                            HeroBannerCard(
                                track = hero,
                                onPlayClick = { playerViewModel.playTrack(hero, trendingTracks) }
                            )
                        }
                    }
                }

                for (module in homeModules) {
                    when (module) {
                        HomeModule.QuickMix -> {
                            item {
                                val mixTracks = remember(recentlyPlayed, trendingTracks) {
                                    (recentlyPlayed + trendingTracks).distinctBy { it.id }.take(6)
                                }
                                if (mixTracks.isNotEmpty()) {
                                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                                        PremiumSectionHeader("Quick Mix", "Jump back into your favorites")
                                        val rows = mixTracks.chunked(2)
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            for (rowTracks in rows) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    for (track in rowTracks) {
                                                        Box(modifier = Modifier.weight(1f)) {
                                                            QuickPickItem(
                                                                track = track, 
                                                                isPlaying = track.id == currentTrack?.id,
                                                                onClick = { playerViewModel.playTrack(track, mixTracks) }
                                                            )
                                                        }
                                                    }
                                                    if (rowTracks.size == 1) {
                                                        Spacer(modifier = Modifier.weight(1f))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        HomeModule.Recommended -> {
                            if (recommendedTracks.isNotEmpty()) {
                                item {
                                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                                        HomeSection(
                                            title = "Recommended for You",
                                            subtitle = "Handpicked songs you might love",
                                            tracks = recommendedTracks, 
                                            currentTrackId = currentTrack?.id,
                                            downloadedTrackIds = downloadedTrackIds,
                                            onTrackClick = { playerViewModel.playTrack(it, recommendedTracks) }
                                        )
                                    }
                                }
                            }
                        }
                        HomeModule.MoodMixes -> {
                            if (rediscoverTracks.isNotEmpty()) {
                                item {
                                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                                        HomeSection(
                                            title = "Mood Mixes", 
                                            subtitle = "Curated audio vibes",
                                            tracks = rediscoverTracks, 
                                            currentTrackId = currentTrack?.id,
                                            downloadedTrackIds = downloadedTrackIds,
                                            onTrackClick = { playerViewModel.playTrack(it, rediscoverTracks) }
                                        )
                                    }
                                }
                            }
                        }
                        HomeModule.Trending -> {
                            if (trendingTracks.isNotEmpty()) {
                                item {
                                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                                        HomeSection(
                                            title = "Trending Now",
                                            subtitle = "Popular tracks globally",
                                            tracks = trendingTracks, 
                                            currentTrackId = currentTrack?.id,
                                            downloadedTrackIds = downloadedTrackIds,
                                            onTrackClick = { playerViewModel.playTrack(it, trendingTracks) }
                                        )
                                    }
                                }
                            }
                        }
                        HomeModule.ContinueListening -> {
                            if (recentlyPlayed.isNotEmpty()) {
                                item {
                                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                                        HomeSection(
                                            title = "Continue Listening",
                                            subtitle = "Pick up right where you left off",
                                            tracks = recentlyPlayed, 
                                            currentTrackId = currentTrack?.id,
                                            downloadedTrackIds = downloadedTrackIds,
                                            onTrackClick = { playerViewModel.playTrack(it, recentlyPlayed) }
                                        )
                                    }
                                }
                            }
                        }
                        else -> { }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
}

@Composable
fun HeroBannerCard(track: Track, onPlayClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(22.dp),
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = track.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.9f),
                                Color.Black.copy(alpha = 0.6f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = SpotifyGreen.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SpotifyGreen.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "FEATURED TRACK",
                            color = SpotifyGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            letterSpacing = 1.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray,
                        maxLines = 1
                    )
                }

                Surface(
                    onClick = onPlayClick,
                    shape = CircleShape,
                    color = LocalAppTheme.current.primary,
                    modifier = Modifier.size(54.dp),
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickPickItem(track: Track, isPlaying: Boolean, onClick: () -> Unit) {
    val theme = LocalAppTheme.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isPlaying) theme.primary.copy(alpha = 0.15f) else GlassSurface,
        border = if (isPlaying) androidx.compose.foundation.BorderStroke(1.dp, theme.primary.copy(alpha = 0.4f)) else null
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = track.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.size(50.dp),
                contentScale = ContentScale.Crop
            )
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isPlaying) SpotifyGreen else Color.White,
                modifier = Modifier.padding(horizontal = 10.dp),
                maxLines = 2,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun HomeSection(
    title: String, 
    subtitle: String? = null,
    tracks: List<Track>, 
    currentTrackId: String?,
    downloadedTrackIds: Set<String>, 
    onTrackClick: (Track) -> Unit
) {
    Column {
        PremiumSectionHeader(title, subtitle)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(tracks, key = { it.id }) { track ->
                HomeTrackItem(
                    track = track, 
                    isPlaying = track.id == currentTrackId,
                    isDownloaded = downloadedTrackIds.contains(track.id),
                    onClick = { onTrackClick(track) }
                )
            }
        }
    }
}

@Composable
fun HomeTrackItem(track: Track, isPlaying: Boolean, isDownloaded: Boolean, onClick: () -> Unit) {
    val theme = LocalAppTheme.current
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.size(140.dp)) {
            val fallbackModel = remember(track.id) { "https://i.ytimg.com/vi/${track.id}/hqdefault.jpg" }
            AsyncImage(
                model = if (track.thumbnailUrl.isNotBlank()) track.thumbnailUrl else fallbackModel,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .then(
                        if (isPlaying) Modifier.border(2.dp, theme.primary, RoundedCornerShape(16.dp)) else Modifier
                    ),
                contentScale = ContentScale.Crop
            )
            if (isDownloaded) {
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = CircleShape,
                    modifier = Modifier.padding(8.dp).align(Alignment.TopEnd).size(22.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.DownloadDone,
                            contentDescription = null,
                            tint = theme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = track.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isPlaying) SpotifyGreen else Color.White,
            maxLines = 1,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = track.artist,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            maxLines = 1
        )
    }
}

@Composable
fun HomeShimmer() {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(160.dp).shimmer().background(GlassSurface, RoundedCornerShape(20.dp)))
        repeat(2) {
            Column {
                Box(modifier = Modifier.fillMaxWidth(0.4f).height(20.dp).shimmer().background(GlassSurface, RoundedCornerShape(4.dp)))
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    repeat(3) {
                        Box(modifier = Modifier.size(130.dp).shimmer().background(GlassSurface, RoundedCornerShape(16.dp)))
                    }
                }
            }
        }
    }
}

