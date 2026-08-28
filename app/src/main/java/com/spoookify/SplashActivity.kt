package com.spoookify

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.spoookify.auth.AuthManager
import com.spoookify.ui.theme.GlassSurface
import com.spoookify.ui.theme.SpoookifyTheme
import com.spoookify.ui.theme.SpotifyBlack
import com.spoookify.ui.theme.SpotifyDarkGrey
import com.spoookify.ui.theme.SpotifyGreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject

@AndroidEntryPoint
@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    @Inject
    lateinit var authManager: AuthManager

    @Inject
    lateinit var userPreferencesRepository: com.spoookify.data.repository.UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpoookifyTheme {
                AnimatedSplashLoginScreen(
                    authManager = authManager,
                    userPreferencesRepository = userPreferencesRepository,
                    onNavigateToMain = {
                        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun AnimatedSplashLoginScreen(
    authManager: AuthManager,
    userPreferencesRepository: com.spoookify.data.repository.UserPreferencesRepository,
    onNavigateToMain: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by authManager.currentUser.collectAsState()
    val isGuestMode by userPreferencesRepository.isGuestMode.collectAsState()

    var isAnimatingUp by remember { mutableStateOf(false) }
    var showLoginPanel by remember { mutableStateOf(false) }

    val googleAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    userPreferencesRepository.setGuestMode(false)
                    authManager.handleGoogleAccount(account) { _, name ->
                        Toast.makeText(context, "Welcome to Spoookify, $name!", Toast.LENGTH_LONG).show()
                        onNavigateToMain()
                    }
                }
            } catch (e: Exception) {
                val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
                if (lastAccount != null) {
                    userPreferencesRepository.setGuestMode(false)
                    authManager.handleGoogleAccount(lastAccount) { _, name ->
                        Toast.makeText(context, "Welcome back, $name!", Toast.LENGTH_LONG).show()
                        onNavigateToMain()
                    }
                } else {
                    onNavigateToMain()
                }
            }
        }
    }

    LaunchedEffect(currentUser.isSignedIn, isGuestMode) {
        delay(800)
        if (currentUser.isSignedIn || isGuestMode) {
            onNavigateToMain()
        } else {
            isAnimatingUp = true
            delay(400)
            showLoginPanel = true
        }
    }

    val logoOffsetY by animateDpAsState(
        targetValue = if (isAnimatingUp) (-120).dp else 0.dp,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "logoOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F1B12),
                        SpotifyBlack,
                        SpotifyBlack
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = logoOffsetY),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.ic_spoookify),
                    contentDescription = null,
                    modifier = Modifier.size(110.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Spoookify",
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Unlimited Lossless Streaming",
                    color = SpotifyGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "from MRD technologies",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        AnimatedVisibility(
            visible = showLoginPanel,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(600, easing = FastOutSlowInEasing)) + fadeIn(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(GlassSurface)
                    .border(1.dp, SpotifyGreen.copy(alpha = 0.25f), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Welcome to Spoookify",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Sign in to sync your playlists & favorites across devices",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clickable {
                            googleAuthLauncher.launch(authManager.getSignInIntent())
                        },
                    shape = RoundedCornerShape(26.dp),
                    color = SpotifyGreen
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Sign In with Google",
                            color = Color.Black,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clickable {
                            userPreferencesRepository.setGuestMode(true)
                            onNavigateToMain()
                        },
                    shape = RoundedCornerShape(26.dp),
                    color = SpotifyDarkGrey,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Continue as Guest (Anonymous)",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black.copy(alpha = 0.4f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = SpotifyGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Privacy First: Guest mode gives full ad-free music access! Sign in anytime in Settings to enable Cloud Playlist & Favorites Backup.",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Developed by MRD technologies",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
