package com.brajesh.gaas.auth

import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.brajesh.gaas.BuildConfig
import com.brajesh.gaas.data.GoogleAccount
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import org.json.JSONObject

/** Outcome of a Google sign-in attempt, including the two expected non-errors. */
sealed interface GoogleSignInResult {
    data class Success(val account: GoogleAccount) : GoogleSignInResult
    data object Cancelled : GoogleSignInResult
    data object NotConfigured : GoogleSignInResult
    data class Failed(val message: String) : GoogleSignInResult
}

/**
 * Google Sign-In through the platform Credential Manager. There is no Firebase
 * project and no backend here: the signed-in profile is only used to greet the
 * user on their own device. The Web OAuth client id comes from BuildConfig, so
 * a checkout without one simply reports [GoogleSignInResult.NotConfigured]
 * instead of crashing on first launch.
 */
class GoogleSignInManager(activity: ComponentActivity) {
    private val credentialManager = CredentialManager.create(activity)
    private val activity = activity

    suspend fun signIn(): GoogleSignInResult {
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) return GoogleSignInResult.NotConfigured

        val option = GetGoogleIdOption.Builder()
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(true)
            .build()

        return try {
            val response = credentialManager.getCredential(
                activity,
                GetCredentialRequest.Builder().addCredentialOption(option).build()
            )
            val credential = GoogleIdTokenCredential.createFrom(response.credential.data)
            val email = idTokenClaims(credential.idToken)?.optString("email").orEmpty()
            if (email.isBlank()) {
                GoogleSignInResult.Failed("Google didn't share an email address with GaaS.")
            } else {
                GoogleSignInResult.Success(
                    GoogleAccount(name = credential.displayName?.takeIf { it.isNotBlank() }, email = email)
                )
            }
        } catch (_: GetCredentialCancellationException) {
            GoogleSignInResult.Cancelled
        } catch (e: GetCredentialException) {
            GoogleSignInResult.Failed(e.message ?: "Google sign-in failed. Please try again.")
        } catch (e: Exception) {
            GoogleSignInResult.Failed(e.message ?: "Google sign-in failed. Please try again.")
        }
    }

    /** Reads the unsigned JWT payload; full token verification belongs on a server. */
    private fun idTokenClaims(token: String): JSONObject? {
        val payload = token.split(".").getOrNull(1) ?: return null
        return try {
            val bytes = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING)
            JSONObject(String(bytes, Charsets.UTF_8))
        } catch (_: Exception) {
            null
        }
    }
}
