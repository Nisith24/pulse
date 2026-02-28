package com.pulse.data.services.btr

import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.android.gms.auth.UserRecoverableAuthException
import com.pulse.domain.services.btr.IBtrAuthManager

sealed class PulseAuthException(message: String) : Exception(message) {
    object UserNotSignedIn : PulseAuthException("User not signed in")
    class PermissionRequired(val intent: android.content.Intent) : PulseAuthException("Permission required for Google Drive")
    class Fatal(message: String) : PulseAuthException(message)
}

class GoogleAuthManager(private val context: Context) : IBtrAuthManager {

    private val driveScope = Scope("https://www.googleapis.com/auth/drive.readonly")

    // Google Sign-In account = our auth state
    override val isSignedIn: Boolean get() = GoogleSignIn.getLastSignedInAccount(context) != null

    override val displayName: String? get() = GoogleSignIn.getLastSignedInAccount(context)?.displayName
    override val email: String? get() = GoogleSignIn.getLastSignedInAccount(context)?.email
    override val photoUrl: String? get() = GoogleSignIn.getLastSignedInAccount(context)?.photoUrl?.toString()

    fun getSignInOptions(): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(driveScope)
            .build()
    }

    suspend fun getSignedInAccount(): GoogleSignInAccount? {
        return try {
            GoogleSignIn.getLastSignedInAccount(context)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun signOut() {
        GoogleSignIn.getClient(context, getSignInOptions()).signOut()
    }

    override suspend fun getToken(): String = withContext(Dispatchers.IO) {
        val account = getSignedInAccount() ?: throw PulseAuthException.UserNotSignedIn
        val scope = "oauth2:https://www.googleapis.com/auth/drive.readonly"
        
        try {
            GoogleAuthUtil.getToken(context, account.account!!, scope)
        } catch (e: UserRecoverableAuthException) {
            val intent = e.intent ?: throw PulseAuthException.Fatal("Auth required but no intent provided")
            throw PulseAuthException.PermissionRequired(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            throw PulseAuthException.Fatal(e.message ?: "Unknown auth error")
        }
    }

    override suspend fun clearToken(token: String) = withContext(Dispatchers.IO) {
        try {
            GoogleAuthUtil.clearToken(context, token)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
