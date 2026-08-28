package com.spoookify.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.spoookify.ui.theme.SpotifyBlack
import com.spoookify.ui.theme.SpotifyGreen
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.material.icons.automirrored.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onBackClick: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val stats by viewModel.stats.collectAsState()
    val topArtists by viewModel.topArtists.collectAsState()
    val topTracks by viewModel.topTracks.collectAsState()

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Music Insights", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SpotifyBlack,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = SpotifyBlack
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // 1. Hero Stats (Grid)
            item {
                Column {
                    Text(
                        text = "The Big Picture",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatCard(
                            title = "Total Time",
                            value = "${stats.totalHours.toInt()}h",
                            icon = Icons.Default.Timer,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Streak",
                            value = "${stats.listeningStreakDays}d",
                            icon = Icons.Default.LocalFireDepartment,
                            modifier = Modifier.weight(1f),
                            tint = Color(0xFFFF5722)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatCard(
                            title = "Discovery",
                            value = "${stats.discoveryRate}%",
                            icon = Icons.Default.AutoAwesome,
                            modifier = Modifier.weight(1f),
                            tint = Color(0xFFE91E63)
                        )
                        StatCard(
                            title = "Avg. Song",
                            value = "${stats.averageSongDurationSeconds / 60}m",
                            icon = Icons.Default.MusicNote,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 2. Music DNA Breakdown
            item {
                Column {
                    Text(
                        text = "Your Music DNA",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (stats.musicDNA.isEmpty()) {
                            Text("Keep listening to reveal your DNA...", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                        }
                        stats.musicDNA.forEach { (genre, percentage) ->
                            DnaBar(genre = genre, percentage = percentage)
                        }
                    }
                }
            }

            // 3. Top Tracks
            item {
                Column {
                    Text(
                        text = "Most Replayed Songs",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    topTracks.take(5).forEachIndexed { index, track ->
                        TopTrackItem(index = index + 1, track = track)
                    }
                }
            }

            // 4. Habits & History
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Habits & History",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    HabitRow(
                        icon = Icons.Default.SkipNext,
                        label = "Most Skipped Artist",
                        value = stats.mostSkippedArtist,
                        tint = Color.Red.copy(alpha = 0.7f)
                    )
                    
                    HabitRow(
                        icon = Icons.Default.CalendarToday,
                        label = "First Played",
                        value = stats.firstPlayedDate?.let { dateFormat.format(Date(it)) } ?: "N/A"
                    )
                    
                    HabitRow(
                        icon = Icons.Default.History,
                        label = "Last Active",
                        value = stats.lastPlayedDate?.let { dateFormat.format(Date(it)) } ?: "N/A"
                    )
                }
            }
            
            // 5. Top Artists
            item {
                Column {
                    Text(
                        text = "Heavy Rotations (Artists)",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    topArtists.take(5).forEachIndexed { index, artist ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape,
                                color = SpotifyGreen.copy(alpha = 0.1f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("${index + 1}", color = SpotifyGreen, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(artist.artist, color = Color.White, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text("${artist.count} plays", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
fun TopTrackItem(index: Int, track: com.spoookify.data.local.dao.TrackCount) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$index",
            modifier = Modifier.width(24.dp),
            color = Color.Gray,
            fontWeight = FontWeight.Bold
        )
        AsyncImage(
            model = track.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(track.trackTitle, color = Color.White, maxLines = 1, fontWeight = FontWeight.SemiBold)
            Text(track.artist, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
        Text("${track.count} plays", color = SpotifyGreen, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun StatCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier, tint: Color = SpotifyGreen) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

@Composable
fun HabitRow(icon: ImageVector, label: String, value: String, tint: Color = Color.Gray) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(label, color = Color.Gray, fontSize = 10.sp)
            Text(value, color = Color.White, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun DnaBar(genre: String, percentage: Int) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = genre, style = MaterialTheme.typography.bodyMedium, color = Color.White)
            Text(text = "$percentage%", style = MaterialTheme.typography.bodySmall, color = SpotifyGreen)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { percentage / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = SpotifyGreen,
            trackColor = Color.White.copy(alpha = 0.1f)
        )
    }
}
