package com.pulse.data.drive

import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.pulse.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.google.android.gms.auth.UserRecoverableAuthException
import com.pulse.domain.service.IDriveAuthManager

sealed class PulseAuthException(message: String) : Exception(message) {
    object UserNotSignedIn : PulseAuthException("User not signed in")
    class PermissionRequired(val intent: android.content.Intent) : PulseAuthException("Permission required for Google Drive")
    class Fatal(message: String) : PulseAuthException(message)
}

class DriveAuthManager(private val context: Context) : IDriveAuthManager {
    
    // Use the required scope for read-only access to files
    private val driveScope = Scope("https://www.googleapis.com/auth/drive.readonly")

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
