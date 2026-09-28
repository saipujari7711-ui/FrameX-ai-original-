package ai.framex.app.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.tasks.await

data class GoogleAccount(val email: String, val displayName: String?, val photoUrl: String?)

class GoogleAuthManager(private val context: Context) {
    companion object {
        const val DRIVE_SCOPE = "https://www.googleapis.com/auth/drive"
        private const val PREFS = "frame_x_google_session"
    }

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val credentialManager = CredentialManager.create(context)

    fun savedAccount(): GoogleAccount? {
        val email = prefs.getString("email", null)?.takeIf { it.isNotBlank() } ?: return null
        return GoogleAccount(email, prefs.getString("displayName", null), prefs.getString("photoUrl", null))
    }

    suspend fun signIn(activity: Activity): GoogleAccount {
        val serverClientId = activity.getString(ai.framex.app.R.string.google_web_client_id)
        require(serverClientId.isNotBlank()) {
            "Google Sign-In is not configured. Add GOOGLE_WEB_CLIENT_ID before using Google authentication."
        }

        val option = GetSignInWithGoogleOption.Builder(serverClientId).build()
        val result = credentialManager.getCredential(activity, GetCredentialRequest(listOf(option)))
        val credential = result.credential
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            throw IllegalStateException("Google did not return a valid account credential.")
        }

        val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val account = GoogleAccount(
            email = googleCredential.id,
            displayName = googleCredential.displayName,
            photoUrl = googleCredential.profilePictureUri?.toString()
        )
        prefs.edit()
            .putString("email", account.email)
            .putString("displayName", account.displayName)
            .putString("photoUrl", account.photoUrl)
            .apply()
        return account
    }

    fun authorizationRequest(): AuthorizationRequest =
        AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DRIVE_SCOPE)))
            .build()

    fun authorization(activity: Activity) =
        Identity.getAuthorizationClient(activity).authorize(authorizationRequest())

    fun parseAuthorizationResult(data: Intent?): AuthorizationResult =
        Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data)

    suspend fun clearCredentialState() {
        runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
        prefs.edit().clear().apply()
    }

    fun clearAccessToken(token: String) {
        Identity.getAuthorizationClient(context)
            .clearToken(ClearTokenRequest.builder().setToken(token).build())
    }

    suspend fun cachedAccessToken(): String? =
        runCatching {
            val result = Identity.getAuthorizationClient(context).authorize(authorizationRequest()).await()
            if (result.hasResolution()) null else result.accessToken
        }.getOrNull()
}
