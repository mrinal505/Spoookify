package com.spoookify.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.spoookify.ui.theme.GlassSurface
import com.spoookify.ui.theme.SpotifyBlack
import com.spoookify.ui.theme.SpotifyGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    onBackClick: () -> Unit,
    viewModel: EqualizerViewModel = hiltViewModel()
) {
    val currentProfile by viewModel.currentProfile.collectAsState()
    val isNormalizationEnabled by viewModel.isNormalizationEnabled.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val balance by viewModel.balance.collectAsState()
    val isMono by viewModel.isMono.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Equalizer & Audio", fontWeight = FontWeight.Bold) },
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
            // Presets
            item {
                Text(
                    text = "Presets",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(viewModel.presets, key = { it.name }) { preset ->
                        FilterChip(
                            selected = currentProfile?.name == preset.name,
                            onClick = { viewModel.setProfile(preset) },
                            label = { Text(preset.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SpotifyGreen,
                                selectedLabelColor = Color.Black,
                                containerColor = GlassSurface,
                                labelColor = Color.White
                            ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                            border = null
                        )
                    }
                }
            }

            // EQ Bands
            item {
                Text(
                    text = "10-Band Equalizer",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                currentProfile?.let { profile ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        profile.bands.forEachIndexed { index, gain ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = getBandLabel(index),
                                    modifier = Modifier.width(48.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                                Slider(
                                    value = gain,
                                    onValueChange = { viewModel.updateBand(index, it) },
                                    valueRange = -12f..12f,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = SpotifyGreen
                                    )
                                )
                                Text(
                                    text = "${gain.toInt()}dB",
                                    modifier = Modifier.width(40.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            // Other Settings
            item {
                Text(
                    text = "Audio Intelligence",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                currentProfile?.let { profile ->
                    ToggleItem(
                        label = "Professional Loudness Normalization",
                        checked = profile.isLoudnessNormalizationEnabled,
                        onCheckedChange = { viewModel.updateLoudnessNormalization(it) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(text = "Compressor Ratio: ${"%.1f".format(profile.compressorRatio)}:1", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = profile.compressorRatio,
                        onValueChange = { viewModel.updateCompressorRatio(it) },
                        valueRange = 1.0f..20.0f,
                        colors = SliderDefaults.colors(activeTrackColor = SpotifyGreen)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(text = "Limiter Threshold: ${"%.1f".format(profile.limiterThreshold)} dB", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = profile.limiterThreshold,
                        onValueChange = { viewModel.updateLimiterThreshold(it) },
                        valueRange = -20.0f..0.0f,
                        colors = SliderDefaults.colors(activeTrackColor = SpotifyGreen)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(text = "Stereo Widening (Virtualizer)", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = profile.virtualizerStrength.toFloat(),
                        onValueChange = { viewModel.updateVirtualizerStrength(it.toInt()) },
                        valueRange = 0.0f..1000.0f,
                        colors = SliderDefaults.colors(activeTrackColor = SpotifyGreen)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Playback & Engine",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                ToggleItem(
                    label = "Basic Volume Normalization (Legacy)",
                    checked = isNormalizationEnabled,
                    onCheckedChange = { viewModel.setNormalization(it) }
                )
                
                ToggleItem(
                    label = "Mono Audio",
                    checked = isMono,
                    onCheckedChange = { viewModel.setMono(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))
                
                Text(text = "Playback Speed: ${"%.2f".format(playbackSpeed)}x", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = playbackSpeed,
                    onValueChange = { viewModel.setPlaybackSpeed(it) },
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(activeTrackColor = SpotifyGreen)
                )

                Spacer(modifier = Modifier.height(16.dp))
                
                Text(text = "Stereo Balance", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = balance,
                    onValueChange = { viewModel.setBalance(it) },
                    valueRange = -1f..1f,
                    colors = SliderDefaults.colors(activeTrackColor = SpotifyGreen)
                )
            }
        }
    }
}

@Composable
fun ToggleItem(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.White, style = MaterialTheme.typography.bodyMedium)
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = SpotifyGreen)
        )
    }
}

private fun getBandLabel(index: Int): String {
    val frequencies = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")
    return frequencies.getOrElse(index) { "" }
}
