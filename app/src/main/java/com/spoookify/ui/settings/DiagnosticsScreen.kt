package com.spoookify.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import com.spoookify.ui.theme.SpotifyBlack
import com.spoookify.ui.theme.SpotifyGreen

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    onBackClick: () -> Unit,
    viewModel: DiagnosticsViewModel = hiltViewModel()
) {
    val currentFormat by viewModel.currentFormat.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    
    // Auto-refresh diagnostics every second for real-time monitoring
    var diagnostics by remember { mutableStateOf(viewModel.getDiagnostics()) }
    
    LaunchedEffect(currentFormat, isPlaying) {
        while(true) {
            diagnostics = viewModel.getDiagnostics()
            kotlinx.coroutines.delay(1000)
        }
    }

    val healthTestResult by viewModel.healthTestResult.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Technical Diagnostics", fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InfoSection(title = "Audio Stream") {
                val codec = diagnostics.codec.substringAfterLast("/")
                InfoItem(label = "Codec", value = if (codec.isNotEmpty() && codec != "None (Idle)") codec else "Unknown / Idle")
                InfoItem(label = "Bitrate", value = if (diagnostics.bitrate > 0) "${diagnostics.bitrate / 1000} kbps" else "Variable / Unknown")
                InfoItem(label = "Sample Rate", value = if (diagnostics.sampleRate > 0) "${diagnostics.sampleRate} Hz" else "N/A")
                InfoItem(label = "Channels", value = if (diagnostics.channels > 0) "${diagnostics.channels}" else "N/A")
            }

            InfoSection(title = "Network & Buffer") {
                InfoItem(label = "Connection State", value = if (isPlaying) "Streaming" else "Connected (Idle)")
                InfoItem(label = "Live Buffer", value = "${diagnostics.bufferSizeMs.coerceAtLeast(0)} ms")
                InfoItem(label = "Decoder Info", value = "ExoPlayer (Hardware Accelerated)")
            }

            Button(
                onClick = { viewModel.runHealthCheck() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen)
            ) {
                Text("Run Health Diagnostic", color = Color.Black, fontWeight = FontWeight.Bold)
            }

            healthTestResult?.let { result ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f))
                ) {
                    Text(
                        text = result,
                        modifier = Modifier.padding(16.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = "Real-time stream metadata provided by the playback engine. Bitrate and buffer size fluctuate based on network conditions.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun InfoSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = SpotifyGreen, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun InfoItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}
