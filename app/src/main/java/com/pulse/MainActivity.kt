package com.pulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.pulse.data.drive.DriveAuthManager
import com.pulse.presentation.lecture.LectureScreen
import com.pulse.presentation.library.LibraryScreen
import com.pulse.presentation.theme.PulseTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val authManager: DriveAuthManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PulseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var userAccount by remember { 
                        mutableStateOf(GoogleSignIn.getLastSignedInAccount(this)) 
                    }

                    val signInLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                        try {
                            userAccount = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                        } catch (e: com.google.android.gms.common.api.ApiException) {
                            e.printStackTrace()
                            android.widget.Toast.makeText(this@MainActivity, "Sign-in failed: ${e.statusCode}", android.widget.Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            android.widget.Toast.makeText(this@MainActivity, "Sign-in error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }

                    if (userAccount == null) {
                        LoginScreen(onSignInClick = {
                            val intent = GoogleSignIn.getClient(this, authManager.getSignInOptions()).signInIntent
                            signInLauncher.launch(intent)
                        })
                    } else {
                        PulseAppContent()
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(onSignInClick: () -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.material3.Button(onClick = onSignInClick) {
            androidx.compose.material3.Text("Sign in with Google")
        }
    }
}

@Composable
fun PulseAppContent() {
    // Simple custom routing
    var selectedLectureId by remember { mutableStateOf<String?>(null) }
    
    if (selectedLectureId == null) {
        LibraryScreen(
            onLectureSelected = { id -> selectedLectureId = id }
        )
    } else {
        androidx.activity.compose.BackHandler {
            selectedLectureId = null
        }
        LectureScreen(lectureId = selectedLectureId!!)
    }
}
