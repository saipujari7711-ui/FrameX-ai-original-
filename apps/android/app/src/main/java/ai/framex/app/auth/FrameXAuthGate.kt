package ai.framex.app.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ai.framex.app.drive.DriveStructure
import ai.framex.app.drive.GoogleDriveService
import ai.framex.app.drive.DriveSyncQueue
import ai.framex.app.ui.GoogleSignInScreen
import ai.framex.app.FrameXAuthenticatedContent
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun FrameXAuthGate(context: android.content.Context) {
    val activity = LocalContext.current as Activity
    val auth = remember { GoogleAuthManager(context) }
    val drive = remember { GoogleDriveService(context.contentResolver) }
    val scope = rememberCoroutineScope()
    var account by remember { mutableStateOf(auth.savedAccount()) }
    var token by remember { mutableStateOf<String?>(null) }
    var structure by remember { mutableStateOf<DriveStructure?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDrive by remember { mutableStateOf(false) }
    var initialized by remember { mutableStateOf(false) }

    fun finishAuthorization(accessToken: String) {
        token = accessToken
        scope.launch {
            busy = true
            error = null
            runCatching { drive.ensureStructure(accessToken) }
                .onSuccess { structure = it; DriveSyncQueue(context).kick(); initialized = true }
                .onFailure {
                    if (it is ai.framex.app.drive.DriveApiException && it.status == 401) auth.clearAccessToken(accessToken)
                    error = it.message ?: "Drive connection failed."
                    initialized = true
                }
            busy = false
        }
    }

    val authResultLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        runCatching {
            val authorizationResult = auth.parseAuthorizationResult(result.data)
            authorizationResult.accessToken?.takeIf { it.isNotBlank() }?.let(::finishAuthorization)
                ?: run { error = "Drive permission was not granted."; initialized = true }
        }.onFailure { error = "Drive authorization could not be completed. Please retry." }
    }

    fun authorizeDrive() {
        busy = true
        error = null
        auth.authorization(activity).addOnSuccessListener { result ->
            if (result.hasResolution()) {
                busy = false
                authResultLauncher.launch(
                    IntentSenderRequest.Builder(result.pendingIntent!!.intentSender).build()
                )
            } else {
                result.accessToken?.let(::finishAuthorization)
            }
        }.addOnFailureListener {
            busy = false
            error = it.message ?: "Drive authorization failed."
        }
    }

    LaunchedEffect(account) {
        if (account != null && !initialized) authorizeDrive()
    }

    if (account == null) {
        GoogleSignInScreen(
            busy = busy,
            error = error,
            onContinue = {
                if (!busy) scope.launch {
                    busy = true
                    error = null
                    runCatching { auth.signIn(activity) }
                        .onSuccess { account = it; initialized = false }
                        .onFailure { error = it.message ?: "Google sign-in was cancelled or failed." }
                    busy = false
                }
            }
        )
        return
    }

    if (!initialized) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.fillMaxSize().padding(28.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("Connecting Google Drive…", style = MaterialTheme.typography.headlineSmall)
                Text(
                    account!!.email,
                    Modifier.padding(top = 6.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (busy) CircularProgressIndicator(Modifier.padding(top = 18.dp))
                if (!error.isNullOrBlank()) {
                    Text(error!!, Modifier.padding(top = 18.dp), color = MaterialTheme.colorScheme.error)
                    Button(onClick = { authorizeDrive() }, Modifier.padding(top = 10.dp)) { Text("Retry Drive") }
                    TextButton(onClick = { initialized = true }) { Text("Continue offline") }
                }
            }
        }
        return
    }

    Box(Modifier.fillMaxSize()) {
        FrameXAuthenticatedContent(context)
        Surface(
            Modifier.fillMaxWidth().align(Alignment.BottomCenter),
            tonalElevation = 4.dp
        ) {
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(account!!.email, style = MaterialTheme.typography.labelMedium)
                    Text(
                        if (token != null && structure != null) "Drive connected • FrameX AI"
                        else "Drive offline • local data available",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { if (token != null && structure != null) showDrive = true else authorizeDrive() }) {
                    Text(if (token != null && structure != null) "Drive" else "Reconnect")
                }
                TextButton(onClick = {
                    scope.launch {
                        auth.clearCredentialState()
                        account = null
                        token = null
                        structure = null
                        initialized = false
                        error = null
                    }
                }) { Text("Sign out") }
            }
        }
    }

    if (showDrive && token != null && structure != null) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showDrive = false }) {
            Surface(
                Modifier.fillMaxWidth().fillMaxHeight(0.9f),
                shape = MaterialTheme.shapes.large
            ) {
                ai.framex.app.ui.DriveWorkspace(
                    tokenProvider = { token },
                    service = drive,
                    rootId = structure!!.rootId,
                    onError = { error = it }
                )
            }
        }
    }
}
