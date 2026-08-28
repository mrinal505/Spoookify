package com.spoookify.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class UserProfile(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?,
    val isSignedIn: Boolean
)

@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: com.spoookify.data.repository.UserPreferencesRepository
) {
    private val auth: FirebaseAuth? = try {
        FirebaseAuth.getInstance()
    } catch (e: Throwable) {
        null
    }

    private val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("252519054082-fc6d2p8pimqckrdfu6hm78hm9kksl2p3.apps.googleusercontent.com")
        .requestEmail()
        .requestProfile()
        .build()

    private val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(context, gso)

    private val firebaseAnalytics: FirebaseAnalytics? = try {
        FirebaseAnalytics.getInstance(context)
    } catch (e: Throwable) {
        null
    }

    private val _currentUser = MutableStateFlow(getCurrentProfile())
    val currentUser = _currentUser.asStateFlow()

    init {
        auth?.addAuthStateListener {
            val profile = getCurrentProfile()
            _currentUser.value = profile
            logAnalyticsEvent(profile)
        }
        val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
        if (lastAccount != null && auth?.currentUser == null) {
            val profile = UserProfile(
                uid = lastAccount.id ?: "google_user",
                displayName = lastAccount.displayName ?: "Google User",
                email = lastAccount.email,
                photoUrl = lastAccount.photoUrl?.toString(),
                isSignedIn = true
            )
            _currentUser.value = profile
            logAnalyticsEvent(profile)
        } else {
            logAnalyticsEvent(_currentUser.value)
        }
    }

    private fun logAnalyticsEvent(user: UserProfile) {
        try {
            if (user.isSignedIn) {
                firebaseAnalytics?.setUserId(user.uid)
                firebaseAnalytics?.setUserProperty("account_type", "google")
            } else {
                firebaseAnalytics?.setUserId(null)
                firebaseAnalytics?.setUserProperty("account_type", "guest")
            }
            firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.APP_OPEN, null)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    fun handleGoogleAccount(account: GoogleSignInAccount, onComplete: (Boolean, String) -> Unit) {
        userPreferencesRepository.setGuestMode(false)
        val idToken = account.idToken
        if (idToken != null && auth != null) {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        _currentUser.value = getCurrentProfile()
                        onComplete(true, account.displayName ?: "User")
                    } else {
                        setFallbackGoogleAccount(account)
                        onComplete(true, account.displayName ?: "User")
                    }
                }
        } else {
            setFallbackGoogleAccount(account)
            onComplete(true, account.displayName ?: "User")
        }
    }

    private fun setFallbackGoogleAccount(account: GoogleSignInAccount) {
        userPreferencesRepository.setGuestMode(false)
        _currentUser.value = UserProfile(
            uid = account.id ?: "google_user",
            displayName = account.displayName ?: "Google User",
            email = account.email,
            photoUrl = account.photoUrl?.toString(),
            isSignedIn = true
        )
    }

    fun getCurrentProfile(): UserProfile {
        val user: FirebaseUser? = auth?.currentUser
        if (user != null) {
            return UserProfile(
                uid = user.uid,
                displayName = user.displayName ?: "Spoookify Listener",
                email = user.email,
                photoUrl = user.photoUrl?.toString(),
                isSignedIn = true
            )
        }
        val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
        if (lastAccount != null) {
            return UserProfile(
                uid = lastAccount.id ?: "google_user",
                displayName = lastAccount.displayName ?: "Google User",
                email = lastAccount.email,
                photoUrl = lastAccount.photoUrl?.toString(),
                isSignedIn = true
            )
        }
        return UserProfile(
            uid = "guest_user",
            displayName = "Guest User",
            email = "Sign in with Google to sync",
            photoUrl = null,
            isSignedIn = false
        )
    }

    fun signOut() {
        try {
            userPreferencesRepository.setGuestMode(false)
            googleSignInClient.signOut()
            auth?.signOut()
            _currentUser.value = getCurrentProfile()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}
