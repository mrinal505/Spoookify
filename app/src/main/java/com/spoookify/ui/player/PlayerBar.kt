package com.spoookify.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.spoookify.ui.theme.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun PlayerBar(
    onClick: () -> Unit,
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val currentTrack by playerViewModel.currentTrack.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val isLoading by playerViewModel.isLoading.collectAsState()
    val progress by playerViewModel.playbackProgress.collectAsState()
    val duration by playerViewModel.duration.collectAsState()
    val isFavorite by playerViewModel.isFavorite.collectAsState()

    val haptic = LocalHapticFeedback.current

    if (currentTrack == null) return

    val currentTheme = LocalAppTheme.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .pointerInput(Unit) {
                coroutineScope {
                    launch {
                        detectTapGestures(
                            onTap = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onClick()
                            }
                        )
                    }
                    launch {
                        var dragX = 0f
                        var dragY = 0f
                        var handled = false

                        detectDragGestures(
                            onDragStart = {
                                dragX = 0f
                                dragY = 0f
                                handled = false
                            },
                            onDragEnd = {
                                if (!handled) {
                                    if (dragY > 30f) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        playerViewModel.stopPlayback()
                                    } else if (dragY < -30f) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onClick()
                                    } else if (dragX > 50f) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        playerViewModel.skipPrevious()
                                    } else if (dragX < -50f) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        playerViewModel.skipNext()
                                    }
                                }
                                dragX = 0f
                                dragY = 0f
                                handled = false
                            },
                            onDragCancel = {
                                dragX = 0f
                                dragY = 0f
                                handled = false
                            },
                            onDrag = { change: androidx.compose.ui.input.pointer.PointerInputChange, dragAmount: androidx.compose.ui.geometry.Offset ->
                                dragX += dragAmount.x
                                dragY += dragAmount.y

                                if (!handled) {
                                    if (dragY > 35f) {
                                        handled = true
                                        change.consume()
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        playerViewModel.stopPlayback()
                                    } else if (dragY < -35f) {
                                        handled = true
                                        change.consume()
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onClick()
                                    } else if (dragX > 60f) {
                                        handled = true
                                        change.consume()
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        playerViewModel.skipPrevious()
                                    } else if (dragX < -60f) {
                                        handled = true
                                        change.consume()
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        playerViewModel.skipNext()
                                    }
                                }
                            }
                        )
                    }
                }
            },
        shape = RoundedCornerShape(18.dp),
        color = currentTheme.cardSurface.copy(alpha = 0.94f),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
        shadowElevation = 12.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = currentTrack?.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = currentTrack?.title ?: "", 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = Color.White,
                        maxLines = 1,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentTrack?.artist ?: "", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = Color.Gray,
                        maxLines = 1
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        playerViewModel.toggleFavorite() 
                    }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, 
                            contentDescription = "Favorite",
                            tint = if (isFavorite) currentTheme.primary else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp).padding(2.dp),
                            color = currentTheme.primary,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                playerViewModel.togglePlayPause()
                            },
                            shape = CircleShape,
                            color = currentTheme.primary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.Black,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            // Progress Bar Line
            if (duration > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .padding(horizontal = 10.dp)
                        .background(Color.White.copy(alpha = 0.15f), shape = CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((progress.toFloat() / duration).coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(currentTheme.primary, shape = CircleShape)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}

