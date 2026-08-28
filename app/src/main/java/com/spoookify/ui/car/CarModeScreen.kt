package com.spoookify.ui.car

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.spoookify.ui.player.PlayerViewModel
import com.spoookify.ui.theme.SpotifyBlack

@Composable
fun CarModeScreen(
    onBackClick: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    var totalOffsetX by remember { mutableFloatStateOf(0f) }
    var swipeTriggered by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpotifyBlack)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        totalOffsetX = 0f
                        swipeTriggered = false
                    },
                    onDragEnd = {
                        totalOffsetX = 0f
                        swipeTriggered = false
                    },
                    onDragCancel = {
                        totalOffsetX = 0f
                        swipeTriggered = false
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        totalOffsetX += dragAmount
                        if (!swipeTriggered) {
                            if (totalOffsetX > 80f) {
                                swipeTriggered = true
                                viewModel.skipPrevious()
                                change.consume()
                            } else if (totalOffsetX < -80f) {
                                swipeTriggered = true
                                viewModel.skipNext()
                                change.consume()
                            }
                        }
                    }
                )
            }
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Exit Car Mode",
                    modifier = Modifier.size(40.dp),
                    tint = Color.White
                )
            }
            
            Text(
                text = "CAR MODE",
                color = Color.Gray,
                style = MaterialTheme.typography.labelLarge,
                letterSpacing = 2.sp
            )

            IconButton(
                onClick = { /* Voice Search */ },
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Search",
                    modifier = Modifier.size(40.dp),
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Album Art
        currentTrack?.let { track ->
            AsyncImage(
                model = track.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(320.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = track.title,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = track.artist,
                color = Color.Gray,
                fontSize = 24.sp,
                textAlign = TextAlign.Center
            )
        } ?: Box(modifier = Modifier.size(320.dp).background(Color.DarkGray, RoundedCornerShape(16.dp)))

        Spacer(modifier = Modifier.weight(1f))

        // Intelligence Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IntelligenceButton(icon = Icons.Default.AutoAwesome, label = "Similar") { viewModel.playSimilar() }
            IntelligenceButton(icon = Icons.Default.Favorite, label = "Favorites") { viewModel.playFavorites() }
            IntelligenceButton(icon = Icons.Default.PlayArrow, label = "Continue") { viewModel.togglePlayPause() }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Playback Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.skipPrevious() },
                modifier = Modifier.size(96.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    modifier = Modifier.size(64.dp),
                    tint = Color.White
                )
            }

            Surface(
                onClick = { viewModel.togglePlayPause() },
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = Color.White
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        modifier = Modifier.size(80.dp),
                        tint = SpotifyBlack
                    )
                }
            }

            IconButton(
                onClick = { viewModel.skipNext() },
                modifier = Modifier.size(96.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    modifier = Modifier.size(64.dp),
                    tint = Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun IntelligenceButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(64.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}
