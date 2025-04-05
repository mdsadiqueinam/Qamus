package io.github.mdsadiqueinam.qamus.data.repository.auth

import android.content.Context
import android.util.Log
import androidx.credentials.*
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.mdsadiqueinam.qamus.R
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages authentication operations.
 * Follows Single Responsibility Principle by focusing only on authentication.
 */
@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth
) {
    companion object {
        private const val TAG = "AuthManager"
    }

    private val credentialManager = CredentialManager.create(context)

    // Instantiate a Google sign-in request
    private val googleIdOption = GetGoogleIdOption.Builder()
        .setServerClientId(context.getString(R.string.default_web_client_id))
        .setFilterByAuthorizedAccounts(false)
        .setAutoSelectEnabled(true)
        .setNonce("nonce")
        .build()

    // Create the Credential Manager request
    private val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    /**
     * Get the current user
     */
    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    /**
     * Observe the current user state
     */
    fun observeUserState(): Flow<FirebaseUser?> = callbackFlow {
        val authListener = FirebaseAuth.AuthStateListener {
            val currentUser = it.currentUser
            trySend(currentUser)
        }
        firebaseAuth.addAuthStateListener(authListener)
        awaitClose {
            firebaseAuth.removeAuthStateListener(authListener)
        }
    }

    /**
     * Sign in the user
     */
    suspend fun signIn(activity: Context) {
        try {
            // Launch Credential Manager UI
            val result = credentialManager.getCredential(
                context = activity, request = request
            )

            // Extract credential from the result returned by Credential Manager
            handleSignIn(result.credential)
        } catch (e: Exception) {
            // Log the exception with detailed information
            Log.w(TAG, "Couldn't retrieve user's credentials: ${e.localizedMessage}")
        }
    }

    /**
     * Handle the sign in process
     */
    private fun handleSignIn(credential: Credential) {
        // Check if credential is of type Google ID
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            // Create Google ID Token
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

            // Sign in to Firebase with using the token
            firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
        } else {
            Log.w(TAG, "Credential is not of type Google ID!")
        }
    }

    /**
     * Authenticate with Firebase using Google credentials
     */
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Sign in success, update UI with the signed-in user's information
                Log.d(TAG, "signInWithCredential:success")
            } else {
                // If sign in fails, display a message to the user
                Log.w(TAG, "signInWithCredential:failure", task.exception)
            }
        }
    }

    /**
     * Sign out the user
     */
    suspend fun signOut() {
        try {
            firebaseAuth.signOut()
            val clearRequest = ClearCredentialStateRequest()
            credentialManager.clearCredentialState(clearRequest)
        } catch (e: Exception) {
            Log.w(TAG, "Error signing out", e)
        }
    }
} 