package com.spoookify.ui.player

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.Replay30
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.spoookify.ui.theme.*

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Bolt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onBackClick: () -> Unit,
    onEqualizerClick: () -> Unit,
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val currentTrack by playerViewModel.currentTrack.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val isLoading by playerViewModel.isLoading.collectAsState()
    val shuffleEnabled by playerViewModel.shuffleModeEnabled.collectAsState()
    val repeatMode by playerViewModel.repeatMode.collectAsState()
    val progress by playerViewModel.playbackProgress.collectAsState()
    val duration by playerViewModel.duration.collectAsState()
    val dominantColorInt by playerViewModel.dominantColor.collectAsState()
    val trackList by playerViewModel.trackList.collectAsState()
    val isFavorite by playerViewModel.isFavorite.collectAsState()
    val isDownloaded by playerViewModel.isDownloaded.collectAsState()
    val downloadProgress by playerViewModel.downloadProgress.collectAsState()
    val skipIntervalSeconds by playerViewModel.skipIntervalSeconds.collectAsState()
    val trackInsights by playerViewModel.trackInsights.collectAsState()

    val energyLevel by playerViewModel.energyLevel.collectAsState()
    val currentMood by playerViewModel.currentMood.collectAsState()
    val interactionLevel by playerViewModel.currentInteractionLevel.collectAsState()

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    
    var showQueue by remember { mutableStateOf(false) }
    var showIntelligence by remember { mutableStateOf(false) }
    var showAiAssistant by remember { mutableStateOf(false) }
    var showAdvancedFavorites by remember { mutableStateOf(false) }
    var showSongInsights by remember { mutableStateOf(false) }
    var aiCommand by remember { mutableStateOf("") }
    
    val sheetState = rememberModalBottomSheetState()
    val scrollState = rememberScrollState()

    val backgroundColor by animateColorAsState(
        targetValue = dominantColorInt?.let { Color(it) } ?: SpotifyBlack,
        animationSpec = tween(durationMillis = 1000),
        label = "backgroundColor"
    )

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

    if (currentTrack == null) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor.copy(alpha = 0.65f),
                        SpotifyDarkGrey.copy(alpha = 0.85f),
                        SpotifyBlack
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PLAYING FROM SEARCH",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "SPOOOKIFY",
                        style = MaterialTheme.typography.labelMedium,
                        color = LocalAppTheme.current.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }
                Row {
                    IconButton(onClick = { 
                        if (!currentUser.isSignedIn) {
                            promptFeature = "Music Insights" to "View track audio signatures, energy metrics, and listening statistics."
                        } else {
                            showSongInsights = true 
                        }
                    }) {
                        Icon(Icons.Default.Info, contentDescription = "Insights", tint = Color.White)
                    }
                    IconButton(onClick = { showAiAssistant = true }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Copilot", tint = SpotifyGreen)
                    }
                    IconButton(onClick = {
                        if (!currentUser.isSignedIn) {
                            promptFeature = "Visual Equalizer" to "Fine-tune 5-band EQ frequencies, virtualizer, and bass boost."
                        } else {
                            onEqualizerClick()
                        }
                    }) {
                        Icon(Icons.Default.Equalizer, contentDescription = "EQ", tint = Color.White)
                    }
                    IconButton(onClick = { showQueue = true }) {
                        Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Queue", tint = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Album Art Card
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .aspectRatio(1f),
                elevation = CardDefaults.cardElevation(defaultElevation = 20.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(currentTrack?.thumbnailUrl)
                        .allowHardware(false)
                        .crossfade(true)
                        .listener(
                            onSuccess = { _, result ->
                                val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                                bitmap?.let {
                                    Palette.from(it).generate { palette ->
                                        palette?.dominantSwatch?.rgb?.let { color ->
                                            playerViewModel.updateDominantColor(color)
                                        }
                                    }
                                }
                            }
                        )
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }


            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(6.dp).background(SpotifyGreen, CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "OPUS 320 KBPS • 48.0 KHZ • STEREO",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray,
                        fontSize = 10.sp,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Track Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentTrack?.title ?: "",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = currentTrack?.artist ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                        maxLines = 1
                    )
                }
                IconButton(onClick = { 
                    if (!currentUser.isSignedIn) {
                        promptFeature = "Liked Songs" to "Save your favorite tracks and build your personal collection."
                    } else {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showAdvancedFavorites = true 
                    }
                }) {
                    val icon = when (interactionLevel) {
                        3 -> Icons.Default.LocalFireDepartment
                        2 -> Icons.Default.Star
                        1 -> Icons.Default.Favorite
                        -1 -> Icons.Default.Block
                        -2 -> Icons.Default.Cancel
                        else -> Icons.Default.FavoriteBorder
                    }
                    val tint = when {
                        interactionLevel >= 3 -> Color(0xFFFF9800)
                        interactionLevel == 2 -> Color(0xFFFFEB3B)
                        interactionLevel == 1 -> SpotifyGreen
                        interactionLevel < 0 -> Color.Red
                        else -> Color.White
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = "Favorite",
                        tint = tint
                    )
                }
                val currentTrackId = currentTrack?.id ?: ""
                val progress = downloadProgress[currentTrackId]

                IconButton(onClick = { 
                    if (!currentUser.isSignedIn) {
                        promptFeature = "Offline Downloads" to "Download high-quality audio tracks for offline playback anytime."
                    } else {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (!isDownloaded && progress == null) playerViewModel.downloadTrack() 
                    }
                }) {
                    if (progress != null) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(24.dp),
                            color = SpotifyGreen,
                            strokeWidth = 2.dp,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    } else {
                        Icon(
                            imageVector = if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                            contentDescription = "Download",
                            tint = if (isDownloaded) SpotifyGreen else Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Seek Bar
            Slider(
                value = if (duration > 0) (progress.toFloat() / duration).coerceIn(0f, 1f) else 0f,
                onValueChange = { 
                    playerViewModel.seekTo((it * duration).toLong())
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = formatTime(progress), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                Text(text = formatTime(duration), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    playerViewModel.toggleShuffle() 
                }) {
                    Icon(
                        Icons.Default.Shuffle, 
                        contentDescription = "Shuffle", 
                        tint = if (shuffleEnabled) SpotifyGreen else Color.White, 
                        modifier = Modifier.size(28.dp)
                    )
                }
                IconButton(onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    playerViewModel.skipPrevious() 
                }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = Color.White, modifier = Modifier.size(36.dp))
                }

                IconButton(onClick = { playerViewModel.seekBackward() }) {
                    val icon = when (skipIntervalSeconds) {
                        5 -> Icons.Default.Replay5
                        30 -> Icons.Default.Replay30
                        else -> Icons.Default.Replay10
                    }
                    Icon(icon, contentDescription = "-${skipIntervalSeconds}s", tint = Color.White, modifier = Modifier.size(28.dp))
                }
                
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(72.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(16.dp),
                            color = SpotifyBlack,
                            strokeWidth = 3.dp
                        )
                    } else {
                        IconButton(onClick = { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            playerViewModel.togglePlayPause() 
                        }) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.Black,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }

                IconButton(onClick = { playerViewModel.seekForward() }) {
                    val icon = when (skipIntervalSeconds) {
                        5 -> Icons.Default.Forward5
                        30 -> Icons.Default.Forward30
                        else -> Icons.Default.Forward10
                    }
                    Icon(icon, contentDescription = "+${skipIntervalSeconds}s", tint = Color.White, modifier = Modifier.size(28.dp))
                }

                IconButton(onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    playerViewModel.skipNext() 
                }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(36.dp))
                }
                IconButton(onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    playerViewModel.toggleRepeatMode() 
                }) {
                    Icon(
                        imageVector = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "Repeat", 
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) SpotifyGreen else Color.White, 
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Inline Up Next Song Queue Section
            val upcomingIndex = trackList.indexOfFirst { it.id == currentTrack?.id }
            val upcomingTracks = if (upcomingIndex != -1 && upcomingIndex < trackList.size - 1) {
                trackList.subList(upcomingIndex + 1, trackList.size)
            } else if (upcomingIndex == -1) trackList else emptyList()

            Card(
                modifier = Modifier.fillMaxWidth(0.95f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = GlassSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = LocalAppTheme.current.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Up Next Queue (${upcomingTracks.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (upcomingTracks.isNotEmpty()) {
                            TextButton(onClick = { playerViewModel.clearUpcomingQueue() }) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = LocalAppTheme.current.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear", color = LocalAppTheme.current.primary, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (upcomingTracks.isEmpty()) {
                        Text(
                            text = "No upcoming songs. Tapping Next will auto-generate smart recommendations.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            upcomingTracks.take(5).forEachIndexed { subIndex, track ->
                                val realIndex = trackList.indexOfFirst { it.id == track.id }
                                QueueItem(
                                    track = track,
                                    isCurrent = false,
                                    onRemove = { playerViewModel.removeFromQueue(track.id) },
                                    onMoveUp = if (subIndex > 0) {
                                        { playerViewModel.moveQueueItem(realIndex, realIndex - 1) }
                                    } else null,
                                    onMoveDown = if (subIndex < upcomingTracks.size - 1) {
                                        { playerViewModel.moveQueueItem(realIndex, realIndex + 1) }
                                    } else null,
                                    onClick = { playerViewModel.playTrack(track, trackList) }
                                )
                            }
                            if (upcomingTracks.size > 5) {
                                TextButton(
                                    onClick = { showQueue = true },
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Text("View all ${upcomingTracks.size} tracks in queue", color = LocalAppTheme.current.primary, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Premium Intelligence Section
            IntelligenceSection(
                energyLevel = energyLevel,
                onEnergyChange = { playerViewModel.setEnergy(it) },
                currentMood = currentMood,
                onMoodChange = { playerViewModel.setMood(it) },
                whyThisSong = playerViewModel.getWhyThisSong()
            )
        }

        if (showQueue) {
            val currentTheme = LocalAppTheme.current
            ModalBottomSheet(
                onDismissRequest = { showQueue = false },
                sheetState = sheetState,
                containerColor = currentTheme.background,
                dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
            ) {
                Column(modifier = Modifier.fillMaxHeight(0.85f).padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Queue",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${trackList.size} tracks in queue",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        if (trackList.size > 1) {
                            TextButton(
                                onClick = { playerViewModel.clearUpcomingQueue() }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Clear Queue", tint = currentTheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear Queue", color = currentTheme.primary, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    androidx.compose.foundation.lazy.LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        item {
                            Text(
                                text = "Now Playing",
                                style = MaterialTheme.typography.labelLarge,
                                color = currentTheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                            currentTrack?.let { track ->
                                QueueItem(
                                    track = track,
                                    isCurrent = true,
                                    onRemove = null,
                                    onMoveUp = null,
                                    onMoveDown = null,
                                    onClick = { showQueue = false }
                                )
                            }
                        }

                        val upcomingIndex = trackList.indexOfFirst { it.id == currentTrack?.id }
                        val upcomingTracks = if (upcomingIndex != -1 && upcomingIndex < trackList.size - 1) {
                            trackList.subList(upcomingIndex + 1, trackList.size)
                        } else if (upcomingIndex == -1) trackList else emptyList()

                        if (upcomingTracks.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Up Next (${upcomingTracks.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
                                )
                            }

                            items(upcomingTracks.size, key = { upcomingTracks[it].id }) { subIndex ->
                                val track = upcomingTracks[subIndex]
                                val realIndex = trackList.indexOfFirst { it.id == track.id }
                                QueueItem(
                                    track = track,
                                    isCurrent = false,
                                    onRemove = { playerViewModel.removeFromQueue(track.id) },
                                    onMoveUp = if (subIndex > 0) {
                                        { playerViewModel.moveQueueItem(realIndex, realIndex - 1) }
                                    } else null,
                                    onMoveDown = if (subIndex < upcomingTracks.size - 1) {
                                        { playerViewModel.moveQueueItem(realIndex, realIndex + 1) }
                                    } else null,
                                    onClick = {
                                        playerViewModel.playTrack(track, trackList)
                                        showQueue = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showAdvancedFavorites) {
            ModalBottomSheet(
                onDismissRequest = { showAdvancedFavorites = false },
                sheetState = sheetState,
                containerColor = SpotifyBlack,
                dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Interaction Preferences", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    InteractionOption(icon = Icons.Default.Favorite, label = "Like", description = "Standard like", isSelected = interactionLevel == 1) {
                        playerViewModel.setInteractionLevel(if (interactionLevel == 1) 0 else 1)
                        showAdvancedFavorites = false
                    }
                    InteractionOption(icon = Icons.Default.Star, label = "Favorite", description = "High priority in Smart Queue", isSelected = interactionLevel == 2) {
                        playerViewModel.setInteractionLevel(if (interactionLevel == 2) 0 else 2)
                        showAdvancedFavorites = false
                    }
                    InteractionOption(icon = Icons.Default.LocalFireDepartment, label = "Replay", description = "Download and keep in heavy rotation", isSelected = interactionLevel == 3) {
                        playerViewModel.setInteractionLevel(if (interactionLevel == 3) 0 else 3)
                        showAdvancedFavorites = false
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))
                    InteractionOption(icon = Icons.Default.Block, label = "Don't Recommend", description = "Hide from smart discovery", isSelected = interactionLevel == -1, tint = Color.Red.copy(alpha = 0.7f)) {
                        playerViewModel.setInteractionLevel(if (interactionLevel == -1) 0 else -1)
                        showAdvancedFavorites = false
                    }
                    InteractionOption(icon = Icons.Default.Cancel, label = "Never Play", description = "Block from all playback", isSelected = interactionLevel == -2, tint = Color.Red) {
                        playerViewModel.setInteractionLevel(if (interactionLevel == -2) 0 else -2)
                        showAdvancedFavorites = false
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        if (showAiAssistant) {
            ModalBottomSheet(
                onDismissRequest = { showAiAssistant = false },
                sheetState = sheetState,
                containerColor = SpotifyBlack,
                dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Music Copilot",
                        style = MaterialTheme.typography.headlineSmall,
                        color = SpotifyGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "What do you want to hear?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    TextField(
                        value = aiCommand,
                        onValueChange = { aiCommand = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Play something energetic", color = Color.DarkGray) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.1f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = MaterialTheme.shapes.medium
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            playerViewModel.executeAiCommand(aiCommand)
                            aiCommand = ""
                            showAiAssistant = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen)
                    ) {
                        Text("Apply Intelligence", color = Color.Black)
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        if (showSongInsights) {
            ModalBottomSheet(
                onDismissRequest = { showSongInsights = false },
                sheetState = sheetState,
                containerColor = SpotifyBlack,
                dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Song Insights & Intelligence", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(currentTrack?.title ?: "", style = MaterialTheme.typography.titleMedium, color = SpotifyGreen, fontWeight = FontWeight.SemiBold)
                    Text(currentTrack?.artist ?: "", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    
                    val genreText = trackInsights?.genre ?: SongInsightsHelper.detectGenre(currentTrack?.artist ?: "", currentTrack?.title ?: "")
                    val energyText = trackInsights?.energy ?: "78% (Balanced)"
                    val bpmText = trackInsights?.bpm ?: "115 BPM"
                    val keyText = trackInsights?.key ?: "C Major"
                    val playText = trackInsights?.playCountText ?: "First session (1 play)"
                    val skipText = trackInsights?.skipRatioText ?: "Low (0%)"
                    val firstText = trackInsights?.firstListenedText ?: "Today"

                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        InsightMetricCard("Genre", genreText, Icons.Default.MusicNote, Modifier.weight(1f))
                        InsightMetricCard("Energy", energyText, Icons.Default.Bolt, Modifier.weight(1f), tint = SpotifyGreen)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        InsightMetricCard("Tempo / BPM", bpmText, Icons.Default.Timer, Modifier.weight(1f))
                        InsightMetricCard("Harmonic Key", keyText, Icons.Default.AutoAwesome, Modifier.weight(1f))
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Text("Personal Listening History", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    InsightHistoryRow(label = "Replay Frequency", value = playText)
                    InsightHistoryRow(label = "Skip Ratio", value = skipText)
                    InsightHistoryRow(label = "First Listened", value = firstText)
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun InsightMetricCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier, tint: Color = Color.White) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(label, color = Color.Gray, fontSize = 10.sp)
        }
    }
}

@Composable
fun InsightHistoryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.Gray, fontSize = 12.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

@Composable
fun IntelligenceSection(
    energyLevel: Float,
    onEnergyChange: (Float) -> Unit,
    currentMood: String,
    onMoodChange: (String) -> Unit,
    whyThisSong: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SpotifyGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Smart Music Intelligence",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            // Energy Slider
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Energy", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Slider(
                value = energyLevel,
                onValueChange = onEnergyChange,
                colors = SliderDefaults.colors(
                    thumbColor = SpotifyGreen,
                    activeTrackColor = SpotifyGreen,
                    inactiveTrackColor = Color.Gray.copy(alpha = 0.2f)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Mood Selector
            Text(text = "Mood", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
            val moods = listOf("Chill", "Workout", "Focus", "Party", "Road Trip")
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(moods.size) { index ->
                    val mood = moods[index]
                    FilterChip(
                        selected = currentMood == mood,
                        onClick = { onMoodChange(mood) },
                        label = { Text(mood) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SpotifyGreen,
                            selectedLabelColor = Color.Black,
                            labelColor = Color.White,
                            containerColor = Color.White.copy(alpha = 0.1f)
                        ),
                        border = null
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Why this song?
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.05f), MaterialTheme.shapes.medium)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = whyThisSong,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
            }
        }
    }
}

@Composable
fun InteractionOption(
    icon: ImageVector,
    label: String,
    description: String,
    isSelected: Boolean,
    tint: Color = SpotifyGreen,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) tint else Color.Gray,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = label, style = MaterialTheme.typography.bodyLarge, color = if (isSelected) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun QueueItem(
    track: com.spoookify.data.remote.Track, 
    isCurrent: Boolean, 
    onRemove: (() -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val theme = LocalAppTheme.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isCurrent) theme.primary.copy(alpha = 0.15f) else GlassSurface,
        border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.dp, theme.primary.copy(alpha = 0.4f)) else null
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = track.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    text = track.title, 
                    style = MaterialTheme.typography.bodyMedium, 
                    color = if (isCurrent) theme.primary else Color.White,
                    maxLines = 1,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = track.artist, 
                    style = MaterialTheme.typography.bodySmall, 
                    color = Color.Gray,
                    maxLines = 1
                )
            }
            
            if (isCurrent) {
                Icon(Icons.Default.Equalizer, contentDescription = null, tint = theme.primary, modifier = Modifier.padding(horizontal = 8.dp))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onMoveUp != null) {
                        IconButton(onClick = onMoveUp, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", tint = Color.Gray, modifier = Modifier.size(20.dp))
                        }
                    }
                    if (onMoveDown != null) {
                        IconButton(onClick = onMoveDown, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", tint = Color.Gray, modifier = Modifier.size(20.dp))
                        }
                    }
                    if (onRemove != null) {
                        IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val seconds = totalSeconds % 60
    val minutes = totalSeconds / 60
    return "%d:%02d".format(minutes, seconds)
}
