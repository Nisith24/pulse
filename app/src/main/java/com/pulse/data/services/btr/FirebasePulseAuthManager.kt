package com.pulse.data.services.btr

import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.pulse.core.domain.util.Constants
import com.pulse.domain.services.btr.IBtrAuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.google.android.gms.auth.UserRecoverableAuthException

class FirebasePulseAuthManager(private val context: Context) : IBtrAuthManager {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val driveScope = "https://www.googleapis.com/auth/drive.readonly"

    override val isSignedIn: Boolean get() = auth.currentUser != null

    override val displayName: String? get() = auth.currentUser?.displayName
    override val email: String? get() = auth.currentUser?.email
    override val photoUrl: String? get() = auth.currentUser?.photoUrl?.toString()

    fun getSignInOptions(): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(Constants.GOOGLE_SIGN_IN_WEB_CLIENT_ID)
            .requestEmail()
            .requestScopes(Scope(driveScope))
            .build()
    }

    suspend fun signInWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential).await()
    }

    override suspend fun signOut() {
        auth.signOut()
        GoogleSignIn.getClient(context, getSignInOptions()).signOut().await()
    }

    override suspend fun getToken(): String = withContext(Dispatchers.IO) {
        val userEmail = auth.currentUser?.email ?: throw PulseAuthException.UserNotSignedIn
        val scope = "oauth2:$driveScope"
        
        try {
            // Get token for the Firebase user's account
            GoogleAuthUtil.getToken(context, userEmail, scope)
        } catch (e: UserRecoverableAuthException) {
            val intent = e.intent ?: throw PulseAuthException.Fatal("Auth required but no intent provided")
            throw PulseAuthException.PermissionRequired(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            throw PulseAuthException.Fatal(e.message ?: "Firebase Token error")
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
