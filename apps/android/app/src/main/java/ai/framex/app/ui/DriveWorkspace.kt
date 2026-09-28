package ai.framex.app.ui

import ai.framex.app.drive.DriveFile
import ai.framex.app.drive.GoogleDriveService
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DriveWorkspace(
    tokenProvider: suspend () -> String?,
    service: GoogleDriveService,
    rootId: String,
    onError: (String) -> Unit
) {
    var folderId by remember(rootId) { mutableStateOf(rootId) }
    var files by remember { mutableStateOf<List<DriveFile>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<DriveFile?>(null) }

    suspend fun reload() {
        loading = true
        runCatching {
            val token = tokenProvider() ?: error("Drive is not connected.")
            files = service.list(token, parentId = folderId, search = query.ifBlank { null }).first
        }.onFailure { onError(it.message ?: "Unable to load Drive.") }
        loading = false
    }

    LaunchedEffect(folderId, query) { reload() }

    Column(Modifier.fillMaxSize().padding(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Google Drive", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            TextButton(onClick = { reload() }) { Text("Refresh") }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            singleLine = true,
            placeholder = { Text("Search files") }
        )
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (files.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No files in this folder.") }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(files, key = { it.id }) { file ->
                    Surface(Modifier.fillMaxWidth(), tonalElevation = 1.dp) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(file.name, fontWeight = FontWeight.Medium)
                                Text(
                                    if (file.isFolder) "Folder" else file.mimeType,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (file.isFolder) {
                                TextButton(onClick = { folderId = file.id; query = "" }) { Text("Open") }
                            } else {
                                TextButton(onClick = { selected = file }) { Text("Details") }
                            }
                        }
                    }
                }
            }
        }
    }

    selected?.let { file ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(file.name) },
            text = {
                Text(
                    buildString {
                        append(file.mimeType)
                        file.size?.let { append("\nSize: ").append(it).append(" bytes") }
                        file.modifiedTime?.let { append("\nModified: ").append(it) }
                    }
                )
            },
            confirmButton = { TextButton(onClick = { selected = null }) { Text("Close") } }
        )
    }
}
