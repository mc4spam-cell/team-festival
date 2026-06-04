package com.mc.mateamhf.data.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Wraps Firebase Auth + the CredentialManager-based Google Sign-In flow.
 *
 * The webClientId is the type-3 OAuth client id from google-services.json — it is exposed
 * automatically by the google-services Gradle plugin as `R.string.default_web_client_id`.
 */
class AuthRepository(
    private val appContext: Context,
    private val webClientId: String,
) {
    private val auth: FirebaseAuth = Firebase.auth
    private val credentialManager: CredentialManager = CredentialManager.create(appContext)

    /** Emits the current Firebase user every time auth state changes (login, logout, token refresh). */
    val currentUser: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    val currentUserOrNull: FirebaseUser?
        get() = auth.currentUser

    /**
     * Triggers Google Sign-In sheet, then exchanges the Google ID token for a Firebase session.
     * Must be called with an Activity context — CredentialManager needs UI.
     */
    suspend fun signIn(activityContext: Context): Result<FirebaseUser> = runCatching {
        val googleOption = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleOption)
            .build()

        val response = credentialManager.getCredential(activityContext, request)
        val idTokenCredential = GoogleIdTokenCredential.createFrom(response.credential.data)
        val firebaseCredential = GoogleAuthProvider.getCredential(idTokenCredential.idToken, null)
        val result = auth.signInWithCredential(firebaseCredential).await()
        result.user ?: error("Firebase sign-in returned no user")
    }

    suspend fun signOut() {
        auth.signOut()
        runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
    }
}
