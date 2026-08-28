package com.spoookify.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.spoookify.ui.theme.SpotifyBlack
import com.spoookify.ui.theme.SpotifyGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageManagerScreen(
    onBackClick: () -> Unit,
    viewModel: StorageManagerViewModel = hiltViewModel()
) {
    val downloads by viewModel.downloads.collectAsState()
    val storageInfo = viewModel.getStorageInfo()

    var showClearCacheConfirm by remember { mutableStateOf(false) }
    var showClearDownloadsConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage Manager", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
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
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                StorageSummary(storageInfo)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { showClearCacheConfirm = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))
                    ) {
                        Text("Clear Cache", color = Color.White)
                    }
                    Button(
                        onClick = { showClearDownloadsConfirm = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                    ) {
                        Text("Delete All", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SpotifyGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clean Library & Health Scan", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Detects broken downloads, unplayed streams (>30 days old), and duplicate files.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.clearCache() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen.copy(alpha = 0.2f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SpotifyGreen.copy(alpha = 0.5f))
                        ) {
                            Text("Auto-Clean Library Now", color = SpotifyGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Downloaded Tracks (${downloads.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            items(downloads, key = { it.id }) { track ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = track.title, color = Color.White, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(text = "${track.artist} • ${formatFileSize(track.fileSize)}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { viewModel.deleteDownload(track) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                    }
                }
            }
        }

        if (showClearCacheConfirm) {
            AlertDialog(
                onDismissRequest = { showClearCacheConfirm = false },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.clearCache()
                        showClearCacheConfirm = false
                    }) { Text("Clear", color = SpotifyGreen) }
                },
                dismissButton = {
                    TextButton(onClick = { showClearCacheConfirm = false }) { Text("Cancel", color = Color.Gray) }
                },
                title = { Text("Clear Temporary Cache?", color = Color.White) },
                text = { Text("This will free up temporary audio buffers without deleting your saved downloads.", color = Color.Gray) },
                containerColor = com.spoookify.ui.theme.SpotifyDarkGrey
            )
        }

        if (showClearDownloadsConfirm) {
            AlertDialog(
                onDismissRequest = { showClearDownloadsConfirm = false },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.clearAllDownloads()
                        showClearDownloadsConfirm = false
                    }) { Text("Delete All", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDownloadsConfirm = false }) { Text("Cancel", color = Color.Gray) }
                },
                title = { Text("Delete All Offline Downloads?", color = Color.White) },
                text = { Text("This will permanently delete all offline music files from your device.", color = Color.Gray) },
                containerColor = com.spoookify.ui.theme.SpotifyDarkGrey
            )
        }
    }
}

@Composable
fun StorageSummary(info: StorageInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            StorageRow("Music", formatFileSize(info.musicSize))
            StorageRow("Cache", formatFileSize(info.cacheSize))
            if (info.recommendToRemove > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Recommended to remove: ${formatFileSize(info.recommendToRemove)}",
                    color = SpotifyGreen,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun StorageRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

private fun formatFileSize(size: Long): String {
    val kb = size / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1 -> "%.1f GB".format(gb)
        mb >= 1 -> "%.1f MB".format(mb)
        else -> "%.1f KB".format(kb)
    }
}
