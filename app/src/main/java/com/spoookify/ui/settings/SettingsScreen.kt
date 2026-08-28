package com.spoookify.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.sp
import com.spoookify.ui.theme.*

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onEqualizerClick: () -> Unit,
    onStorageClick: () -> Unit,
    onDiagnosticsClick: () -> Unit,
    onCarModeClick: () -> Unit = {},
    onHomeCustomizeClick: () -> Unit = {},
    themeViewModel: ThemeSettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentUser by themeViewModel.authManager.currentUser.collectAsState()

    val googleAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    themeViewModel.authManager.handleGoogleAccount(account) { _, name ->
                        Toast.makeText(context, "Signed in as $name", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
                if (lastAccount != null) {
                    themeViewModel.authManager.handleGoogleAccount(lastAccount) { _, name ->
                        Toast.makeText(context, "Signed in as $name", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "Signed in with Google Account", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val isAmoledBlack by themeViewModel.isAmoledBlack.collectAsState()
    val useDynamicColors by themeViewModel.useDynamicColors.collectAsState()
    val appScaleMode by themeViewModel.appScaleMode.collectAsState()
    val isSmartOfflineEnabled by themeViewModel.isSmartOfflineEnabled.collectAsState()

    val audioBitrate by themeViewModel.audioBitrate.collectAsState()
    val isDataSaverEnabled by themeViewModel.isDataSaverEnabled.collectAsState()
    val crossfadeSeconds by themeViewModel.crossfadeSeconds.collectAsState()
    val skipIntervalSeconds by themeViewModel.skipIntervalSeconds.collectAsState()
    val autoAudioModeEnabled by themeViewModel.autoAudioModeEnabled.collectAsState()
    val sleepTimerMinutes by themeViewModel.sleepTimerMinutes.collectAsState()
    val bufferSizeMs by themeViewModel.bufferSizeMs.collectAsState()
    val cacheLimitMb by themeViewModel.cacheLimitMb.collectAsState()

    // Modal dialog state handlers
    var activeDialog by remember { mutableStateOf<String?>(null) }
    var promptFeature by remember { mutableStateOf<Pair<String, String>?>(null) }

    val currentTheme = LocalAppTheme.current

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Preferences", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = currentTheme.background,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = currentTheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            SettingsSection(title = "Google Account & Cloud Sync") {
                SettingsItem(
                    title = if (currentUser.isSignedIn) "Google Account" else "Google Account Sign-In", 
                    subtitle = if (currentUser.isSignedIn) "Signed in as ${currentUser.displayName} (${currentUser.email}) • Real-time Cloud Sync Active" else "Sign in to sync your playlists & favorites automatically across devices",
                    onClick = { 
                        if (currentUser.isSignedIn) {
                            activeDialog = "cloud_account_signout"
                        } else {
                            googleAuthLauncher.launch(themeViewModel.authManager.getSignInIntent())
                        }
                    }
                )
            }

            SettingsSection(title = "Account & Audio Quality") {
                SettingsItem(
                    title = "Streaming Audio Bitrate", 
                    subtitle = "Current: ${audioBitrate.uppercase()} (High Fidelity)",
                    onClick = { activeDialog = "bitrate" }
                )
                ToggleItem(
                    label = "Data Saver Streaming Mode",
                    checked = isDataSaverEnabled,
                    onCheckedChange = { 
                        themeViewModel.setDataSaverEnabled(it)
                        Toast.makeText(context, if (it) "Data Saver On" else "Data Saver Off", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            val appTheme by themeViewModel.appTheme.collectAsState()

            SettingsSection(title = "Customization & Theme") {
                SettingsItem(
                    title = "App Theme & Color Palette", 
                    subtitle = "Current: ${appTheme.title} (${com.spoookify.ui.theme.AppTheme.values().size} themes available)",
                    onClick = { 
                        if (!currentUser.isSignedIn) {
                            promptFeature = "Theme Customization" to "Personalize your app accent colors and dark presets across your devices."
                        } else {
                            activeDialog = "theme_presets" 
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(com.spoookify.ui.theme.AppTheme.values(), key = { it.id }) { theme ->
                        FilterChip(
                            selected = appTheme == theme,
                            onClick = { 
                                if (!currentUser.isSignedIn) {
                                    promptFeature = "Theme Customization" to "Personalize your app accent colors and dark presets across your devices."
                                } else {
                                    themeViewModel.setAppTheme(theme)
                                    Toast.makeText(context, "Theme: ${theme.title}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            label = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(theme.primary, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(theme.title) 
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = theme.primary.copy(alpha = 0.25f),
                                selectedLabelColor = theme.primary,
                                containerColor = GlassSurface,
                                labelColor = Color.White
                            ),
                            shape = RoundedCornerShape(20.dp),
                            border = if (appTheme == theme) androidx.compose.foundation.BorderStroke(1.dp, theme.primary) else null
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                ToggleItem(
                    label = "AMOLED Black Background Override",
                    checked = isAmoledBlack,
                    onCheckedChange = { 
                        if (!currentUser.isSignedIn) {
                            promptFeature = "AMOLED Black Mode" to "Toggle pure pitch-black AMOLED themes and save OLED battery life."
                        } else {
                            themeViewModel.setAmoledBlack(it)
                            Toast.makeText(context, "AMOLED mode updated", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                ToggleItem(
                    label = "Material You Dynamic Colors",
                    checked = useDynamicColors,
                    onCheckedChange = { 
                        if (!currentUser.isSignedIn) {
                            promptFeature = "Dynamic Colors" to "Match app colors to your system wallpaper dynamically."
                        } else {
                            themeViewModel.setDynamicColors(it)
                            Toast.makeText(context, "Dynamic colors updated", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                SettingsItem(
                    title = "Customize Home Screen Modules", 
                    subtitle = "Reorder modules, toggle sections, & seed recommendations", 
                    onClick = {
                        if (!currentUser.isSignedIn) {
                            promptFeature = "Customize Home Screen" to "Reorder sections, pin genres, and arrange your home feed."
                        } else {
                            onHomeCustomizeClick()
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "App Scaling Mode", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                val modes = listOf("auto" to "Auto-Adaptive", "compact" to "Compact", "comfortable" to "Comfortable")
                LazyRow(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(modes, key = { it.first }) { (mode, label) ->
                        FilterChip(
                            selected = appScaleMode == mode,
                            onClick = { 
                                themeViewModel.setAppScaleMode(mode)
                                Toast.makeText(context, "Scaling mode: $label", Toast.LENGTH_SHORT).show()
                            },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SpotifyGreen,
                                selectedLabelColor = Color.Black,
                                containerColor = GlassSurface,
                                labelColor = Color.White
                            ),
                            shape = RoundedCornerShape(20.dp),
                            border = null
                        )
                    }
                }
            }
            
            SettingsSection(title = "Playback & Audio Engine") {
                SettingsItem(
                    title = "Visual Equalizer & Effects", 
                    subtitle = "5-band EQ, Bass Boost, & Virtualizer",
                    onClick = {
                        if (!currentUser.isSignedIn) {
                            promptFeature = "Visual Equalizer" to "Fine-tune 5-band EQ frequencies, virtualizer, and bass boost."
                        } else {
                            onEqualizerClick()
                        }
                    }
                )
                SettingsItem(
                    title = "Headphone Audio Profiles", 
                    subtitle = "Sony WH-1000XM5 • AirPods Pro • Car Audio Profiles",
                    onClick = {
                        if (!currentUser.isSignedIn) {
                            promptFeature = "Headphone Profiles" to "Access custom acoustics for Sony, AirPods, and Car Bluetooth."
                        } else {
                            activeDialog = "headphone_profiles"
                        }
                    }
                )
                SettingsItem(
                    title = "Storage & Cache Manager", 
                    subtitle = "Manage downloads & limit cache ($cacheLimitMb MB limit)",
                    onClick = {
                        if (!currentUser.isSignedIn) {
                            promptFeature = "Offline Downloads & Storage" to "Manage downloaded tracks and set offline cache limits."
                        } else {
                            onStorageClick()
                        }
                    }
                )
                SettingsItem(
                    title = "Crossfade Duration", 
                    subtitle = "$crossfadeSeconds.0 seconds transition",
                    onClick = { activeDialog = "crossfade" }
                )
                SettingsItem(
                    title = "Seek Skip Interval", 
                    subtitle = "${skipIntervalSeconds}s forward/backward",
                    onClick = { activeDialog = "skip" }
                )
                SettingsItem(
                    title = "Sleep Timer", 
                    subtitle = if (sleepTimerMinutes > 0) "Timer active: $sleepTimerMinutes min" else "Off",
                    onClick = { activeDialog = "sleep" }
                )
            }

            SettingsSection(title = "Smart Automation") {
                ToggleItem(
                    label = "Smart Offline Downloading",
                    checked = isSmartOfflineEnabled,
                    onCheckedChange = { 
                        if (!currentUser.isSignedIn) {
                            promptFeature = "Smart Offline Downloading" to "Automatically download your top listening tracks for offline playback."
                        } else {
                            themeViewModel.setSmartOfflineEnabled(it)
                            Toast.makeText(context, if (it) "Smart offline enabled" else "Smart offline disabled", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                ToggleItem(
                    label = "Auto Audio Profile",
                    checked = autoAudioModeEnabled,
                    onCheckedChange = { 
                        themeViewModel.setAutoAudioModeEnabled(it)
                        Toast.makeText(context, if (it) "Auto audio profile enabled" else "Auto audio profile disabled", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            SettingsSection(title = "Developer & Performance") {
                SettingsItem(title = "Technical Diagnostics", subtitle = "Stream latency, network ping, and codec health") {
                    onDiagnosticsClick()
                }
                SettingsItem(
                    title = "Prefetch Buffer Size", 
                    subtitle = "${bufferSizeMs / 1000}s prefetch window",
                    onClick = { activeDialog = "buffer" }
                )
                SettingsItem(
                    title = "Max Cache Limit", 
                    subtitle = "$cacheLimitMb MB max disk storage",
                    onClick = { activeDialog = "cache" }
                )
            }

            SettingsSection(title = "Support & Crypto Donations") {
                SettingsItem(
                    title = "Support Developer (Crypto)", 
                    subtitle = "Donate via ETH, BTC, SOL, or TRX (QR & Click to Copy)",
                    onClick = { activeDialog = "crypto_donations" }
                )
            }

            SettingsSection(title = "About Spoookify") {
                SettingsItem(title = "App Version", subtitle = "v1.2.0 (Build 7)")
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }

        // Active Dialog Windows
        when (activeDialog) {
            "crypto_donations" -> {
                CryptoDonationDialog(onDismiss = { activeDialog = null })
            }
            "theme_presets" -> {
                val currentTheme by themeViewModel.appTheme.collectAsState()
                AlertDialog(
                    onDismissRequest = { activeDialog = null },
                    confirmButton = { TextButton(onClick = { activeDialog = null }) { Text("Close", color = currentTheme.primary) } },
                    title = { Text("App Theme & Color Palettes", color = Color.White, fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            com.spoookify.ui.theme.AppTheme.values().forEach { theme ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            themeViewModel.setAppTheme(theme)
                                            activeDialog = null
                                        }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .background(theme.primary, CircleShape)
                                            .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = theme.title, 
                                        color = Color.White, 
                                        fontWeight = if (currentTheme == theme) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (currentTheme == theme) {
                                        Text(text = "Active", color = theme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    },
                    containerColor = SpotifyDarkGrey
                )
            }
            "cloud_account" -> {
                AlertDialog(
                    onDismissRequest = { activeDialog = null },
                    confirmButton = { 
                        TextButton(onClick = { 
                            activeDialog = null 
                            googleAuthLauncher.launch(themeViewModel.authManager.getSignInIntent())
                        }) { Text("Sign In with Google", color = SpotifyGreen, fontWeight = FontWeight.Bold) } 
                    },
                    dismissButton = {
                        TextButton(onClick = { activeDialog = null }) { Text("Cancel", color = Color.Gray) }
                    },
                    title = { Text("Google Account & Firebase Auth", color = Color.White, fontWeight = FontWeight.Bold) },
                    text = { Text("Sign in with your Google account to enable real-time cloud sync for your Liked Songs, custom playlists, listening history, and push notification updates.", color = Color.Gray) },
                    containerColor = SpotifyDarkGrey
                )
            }
            "cloud_account_signout" -> {
                AlertDialog(
                    onDismissRequest = { activeDialog = null },
                    confirmButton = { 
                        TextButton(onClick = { 
                            themeViewModel.authManager.signOut()
                            Toast.makeText(context, "Signed out of Google account", Toast.LENGTH_SHORT).show()
                            activeDialog = null 
                        }) { Text("Sign Out", color = Color.Red, fontWeight = FontWeight.Bold) } 
                    },
                    dismissButton = {
                        TextButton(onClick = { activeDialog = null }) { Text("Cancel", color = Color.Gray) }
                    },
                    title = { Text("Signed In Account", color = Color.White, fontWeight = FontWeight.Bold) },
                    text = { Text("Currently signed in as ${currentUser.displayName} (${currentUser.email}). Would you like to sign out?", color = Color.Gray) },
                    containerColor = SpotifyDarkGrey
                )
            }
            "account" -> {
                AlertDialog(
                    onDismissRequest = { activeDialog = null },
                    confirmButton = { TextButton(onClick = { activeDialog = null }) { Text("OK", color = SpotifyGreen) } },
                    title = { Text("Spoookify Unlimited", color = Color.White, fontWeight = FontWeight.Bold) },
                    text = { Text("Your account has lifetime Spoookify Unlimited tier enabled with full 320kbps Opus audio playback, ad-free streaming, offline storage, and visual equalizer access.", color = Color.Gray) },
                    containerColor = SpotifyDarkGrey
                )
            }
            "bitrate" -> {
                AlertDialog(
                    onDismissRequest = { activeDialog = null },
                    confirmButton = { TextButton(onClick = { activeDialog = null }) { Text("Close", color = SpotifyGreen) } },
                    title = { Text("Streaming Audio Quality", color = Color.White, fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            listOf("320kbps" to "Extreme Quality (320 kbps Opus)", "160kbps" to "High Quality (160 kbps)", "96kbps" to "Data Saver (96 kbps)").forEach { (value, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            themeViewModel.setAudioBitrate(value)
                                            activeDialog = null
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = audioBitrate == value,
                                        onClick = { 
                                            themeViewModel.setAudioBitrate(value)
                                            activeDialog = null
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = SpotifyGreen)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = label, color = Color.White)
                                }
                            }
                        }
                    },
                    containerColor = SpotifyDarkGrey
                )
            }
            "sleep" -> {
                AlertDialog(
                    onDismissRequest = { activeDialog = null },
                    confirmButton = { TextButton(onClick = { activeDialog = null }) { Text("Close", color = SpotifyGreen) } },
                    title = { Text("Sleep Timer", color = Color.White, fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            listOf(0 to "Off", 15 to "15 Minutes", 30 to "30 Minutes", 45 to "45 Minutes", 60 to "60 Minutes").forEach { (min, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            themeViewModel.setSleepTimerMinutes(min)
                                            activeDialog = null
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = sleepTimerMinutes == min,
                                        onClick = { 
                                            themeViewModel.setSleepTimerMinutes(min)
                                            activeDialog = null
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = SpotifyGreen)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = label, color = Color.White)
                                }
                            }
                        }
                    },
                    containerColor = SpotifyDarkGrey
                )
            }
            "crossfade" -> {
                var tempCrossfade by remember { mutableStateOf(crossfadeSeconds.toFloat()) }
                AlertDialog(
                    onDismissRequest = { activeDialog = null },
                    confirmButton = {
                        TextButton(onClick = {
                            themeViewModel.setCrossfadeSeconds(tempCrossfade.toInt())
                            activeDialog = null
                        }) { Text("Save", color = SpotifyGreen) }
                    },
                    title = { Text("Crossfade Duration: ${tempCrossfade.toInt()}s", color = Color.White, fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Slider(
                                value = tempCrossfade,
                                onValueChange = { tempCrossfade = it },
                                valueRange = 0f..10f,
                                steps = 9,
                                colors = SliderDefaults.colors(activeTrackColor = SpotifyGreen)
                            )
                        }
                    },
                    containerColor = SpotifyDarkGrey
                )
            }
            "skip" -> {
                AlertDialog(
                    onDismissRequest = { activeDialog = null },
                    confirmButton = { TextButton(onClick = { activeDialog = null }) { Text("Close", color = SpotifyGreen) } },
                    title = { Text("Seek Skip Interval", color = Color.White, fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            listOf(5 to "5 seconds", 10 to "10 seconds", 15 to "15 seconds", 30 to "30 seconds").forEach { (sec, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            themeViewModel.setSkipIntervalSeconds(sec)
                                            activeDialog = null
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = skipIntervalSeconds == sec,
                                        onClick = { 
                                            themeViewModel.setSkipIntervalSeconds(sec)
                                            activeDialog = null
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = SpotifyGreen)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = label, color = Color.White)
                                }
                            }
                        }
                    },
                    containerColor = SpotifyDarkGrey
                )
            }
            "buffer" -> {
                AlertDialog(
                    onDismissRequest = { activeDialog = null },
                    confirmButton = { TextButton(onClick = { activeDialog = null }) { Text("Close", color = SpotifyGreen) } },
                    title = { Text("Prefetch Buffer Size", color = Color.White, fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            listOf(15000 to "15 Seconds (Fast Start)", 30000 to "30 Seconds (Standard)", 60000 to "60 Seconds (High Stability)").forEach { (ms, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            themeViewModel.setBufferSize(ms)
                                            activeDialog = null
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = bufferSizeMs == ms,
                                        onClick = { 
                                            themeViewModel.setBufferSize(ms)
                                            activeDialog = null
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = SpotifyGreen)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = label, color = Color.White)
                                }
                            }
                        }
                    },
                    containerColor = SpotifyDarkGrey
                )
            }
            "cache" -> {
                AlertDialog(
                    onDismissRequest = { activeDialog = null },
                    confirmButton = { TextButton(onClick = { activeDialog = null }) { Text("Close", color = SpotifyGreen) } },
                    title = { Text("Max Cache Limit", color = Color.White, fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            listOf(256 to "256 MB", 512 to "512 MB", 1024 to "1 GB", 2048 to "2 GB").forEach { (mb, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            themeViewModel.setCacheLimit(mb)
                                            activeDialog = null
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = cacheLimitMb == mb,
                                        onClick = { 
                                            themeViewModel.setCacheLimit(mb)
                                            activeDialog = null
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = SpotifyGreen)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = label, color = Color.White)
                                }
                            }
                        }
                    },
                    containerColor = SpotifyDarkGrey
                )
            }
            "headphone_profiles" -> {
                AlertDialog(
                    onDismissRequest = { activeDialog = null },
                    confirmButton = { TextButton(onClick = { activeDialog = null }) { Text("Close", color = SpotifyGreen) } },
                    title = { Text("Headphone Audio Profiles", color = Color.White, fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            listOf(
                                "Sony WH-1000XM5 (Deep Bass & Clarity)",
                                "AirPods Pro (Spatial & Vocal Balance)",
                                "Galaxy Buds (Dynamic Punch)",
                                "Car Audio System (Sub-Bass Boost)",
                                "Studio Monitor (Flat Response)"
                            ).forEach { label ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            Toast.makeText(context, "Applied profile: $label", Toast.LENGTH_SHORT).show()
                                            activeDialog = null
                                        }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Headset, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(text = label, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    },
                    containerColor = SpotifyDarkGrey
                )
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val theme = LocalAppTheme.current
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = theme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = theme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), content = content)
        }
    }
}

@Composable
fun SettingsItem(title: String, subtitle: String? = null, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
        if (onClick != null) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
        }
    }
}

@Composable
fun CryptoDonationDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    val wallets = remember {
        listOf(
            CryptoWallet("ETH", "Ethereum (ERC-20)", "0x5A9fb97BCe03dc19Bd5C5a1C5d9589724886faF7", Color(0xFF627EEA)),
            CryptoWallet("BTC", "Bitcoin", "bc1qe07ama7xm6kelgnmtt4vw0v67dajqp8dshyypg", Color(0xFFF7931A)),
            CryptoWallet("SOL", "Solana", "6inmZtJP2UGrKEKFEJamJ9L1CRSZvWpki1zq9xdSocsU", Color(0xFF14F195)),
            CryptoWallet("TRX", "TRON (TRC-20)", "TG29v6vuJBfsUGqbbcjFvz8iB5yJCDjsK2", Color(0xFFFF0013))
        )
    }

    var selectedWallet by remember { mutableStateOf(wallets[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = SpotifyGreen, fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Column {
                Text("💖 Support & Crypto Donations", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Scan QR code or click to copy address", color = Color.Gray, fontSize = 12.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    wallets.forEach { wallet ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (selectedWallet.symbol == wallet.symbol) wallet.accentColor else GlassSurface,
                            modifier = Modifier
                                .clickable { selectedWallet = wallet }
                                .padding(2.dp)
                        ) {
                            Text(
                                text = wallet.symbol,
                                color = if (selectedWallet.symbol == wallet.symbol) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier
                        .size(180.dp)
                        .padding(4.dp)
                ) {
                    coil.compose.AsyncImage(
                        model = "https://api.qrserver.com/v1/create-qr-code/?data=${selectedWallet.address}&size=300x300",
                        contentDescription = "${selectedWallet.name} QR Code",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = selectedWallet.name,
                    color = selectedWallet.accentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(selectedWallet.address))
                            Toast.makeText(context, "${selectedWallet.symbol} Address Copied!", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = selectedWallet.address,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "📋 Copy",
                            color = SpotifyGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        containerColor = SpotifyDarkGrey
    )
}

data class CryptoWallet(
    val symbol: String,
    val name: String,
    val address: String,
    val accentColor: Color
)


