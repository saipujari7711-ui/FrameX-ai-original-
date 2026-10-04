package ai.framex.app.ui

import ai.framex.app.drive.DriveFile
import ai.framex.app.drive.GoogleDriveService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun DriveWorkspace(
    tokenProvider: suspend () -> String?,
    service: GoogleDriveService,
    rootId: String,
    onError: (String) -> Unit
) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var folderId by remember(rootId) { mutableStateOf(rootId) }
    var files by remember { mutableStateOf<List<DriveFile>>(emptyList()) }
    var folders by remember { mutableStateOf<List<DriveFile>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<DriveFile?>(null) }
    var renameTarget by remember { mutableStateOf<DriveFile?>(null) }
    var renameValue by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<DriveFile?>(null) }
    var moveTarget by remember { mutableStateOf<DriveFile?>(null) }
    var downloading by remember { mutableStateOf(false) }

    val uploadLauncher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri!=null) scope.launch {
            runCatching {
                val token=tokenProvider() ?: error("Drive is not connected.")
                val name=uri.lastPathSegment?.substringAfterLast('/') ?: "upload"
                val mime=context.contentResolver.getType(uri) ?: "application/octet-stream"
                service.upload(token,uri,folderId,name,mime)
                val r=service.list(token,parentId=folderId,search=query.ifBlank { null }); files=r.first
            }.onFailure { onError(it.message ?: "Upload failed.") }
        }
    }

    val downloadLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val file=selected
        if(uri!=null && file!=null) scope.launch {
            downloading=true
            runCatching {
                val token=tokenProvider() ?: error("Drive is not connected.")
                context.contentResolver.openOutputStream(uri)?.use { service.download(token,file.id,it) }
                    ?: error("Unable to create the local file.")
            }.onFailure { onError(it.message ?: "Download failed.") }
            downloading=false
            selected=null
        }
    }

    suspend fun reload() {
        loading=true
        runCatching {
            val token=tokenProvider() ?: error("Drive is not connected.")
            val result=service.list(token,parentId=folderId,search=query.ifBlank { null })
            files=result.first
            folders=service.list(token,parentId=rootId).first.filter { it.isFolder }
        }.onFailure { onError(it.message ?: "Unable to load Drive.") }
        loading=false
    }

    LaunchedEffect(folderId,query) { reload() }

    Column(Modifier.fillMaxSize().padding(14.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text("Google Drive",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f))
            TextButton(onClick={ uploadLauncher.launch(arrayOf("*/*")) }) { Text("Upload") }
            TextButton(onClick={ scope.launch { reload() } }) { Text("Refresh") }
        }
        OutlinedTextField(
            value=query,onValueChange={query=it},modifier=Modifier.fillMaxWidth().padding(vertical=8.dp),
            singleLine=true,placeholder={Text("Search files")}
        )
        if(downloading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if(loading) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator()}
        else if(files.isEmpty()) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("No files in this folder.")}
        else LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            items(files,key={it.id}) { file ->
                Surface(Modifier.fillMaxWidth(),tonalElevation=1.dp) {
                    Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(file.name,fontWeight=FontWeight.Medium)
                            Text(if(file.isFolder) "Folder" else file.mimeType,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if(file.isFolder) TextButton(onClick={folderId=file.id;query=""}){Text("Open")}
                        else TextButton(onClick={selected=file}){Text("Actions")}
                    }
                }
            }
        }
    }

    selected?.let { file ->
        AlertDialog(
            onDismissRequest={selected=null},
            title={Text(file.name)},
            text={Text(file.mimeType + (file.size?.let { "\n" + it + " bytes" } ?: ""))},
            dismissButton={TextButton(onClick={selected=null}){Text("Close")}},
            confirmButton={
                Row {
                    TextButton(onClick={downloadLauncher.launch(file.mimeType);selected=file}){Text("Download")}
                    TextButton(onClick={renameTarget=file;renameValue=file.name;selected=null}){Text("Rename")}
                    TextButton(onClick={moveTarget=file;selected=null}){Text("Move")}
                    TextButton(onClick={deleteTarget=file;selected=null}){Text("Delete")}
                }
            }
        )
    }

    renameTarget?.let { file ->
        AlertDialog(
            onDismissRequest={renameTarget=null},
            title={Text("Rename file")},
            text={OutlinedTextField(renameValue,{renameValue=it},singleLine=true)},
            dismissButton={TextButton(onClick={renameTarget=null}){Text("Cancel")}},
            confirmButton={TextButton(onClick={
                scope.launch {
                    runCatching {
                        val token=tokenProvider() ?: error("Drive is not connected.")
                        service.rename(token,file.id,renameValue.trim().ifBlank { file.name })
                        renameTarget=null; reload()
                    }.onFailure { onError(it.message ?: "Rename failed.") }
                }
            }){Text("Save")}}
        )
    }

    deleteTarget?.let { file ->
        AlertDialog(
            onDismissRequest={deleteTarget=null},
            title={Text("Delete from Drive?")},
            text={Text("This permanently deletes " + file.name + " from Drive when your account has permission.")},
            dismissButton={TextButton(onClick={deleteTarget=null}){Text("Cancel")}},
            confirmButton={TextButton(onClick={
                scope.launch {
                    runCatching {
                        val token=tokenProvider() ?: error("Drive is not connected.")
                        service.delete(token,file.id)
                        deleteTarget=null;reload()
                    }.onFailure { onError(it.message ?: "Delete failed.") }
                }
            }){Text("Delete")}}
        )
    }

    moveTarget?.let { file ->
        AlertDialog(
            onDismissRequest={moveTarget=null},
            title={Text("Move " + file.name)},
            text={
                Column {
                    folders.filter { it.id != folderId }.forEach { destination ->
                        TextButton(onClick={
                            scope.launch {
                                runCatching {
                                    val token=tokenProvider() ?: error("Drive is not connected.")
                                    service.move(token,file.id,destination.id)
                                    moveTarget=null;reload()
                                }.onFailure { onError(it.message ?: "Move failed.") }
                            }
                        },modifier=Modifier.fillMaxWidth()) { Text(destination.name) }
                    }
                }
            },
            confirmButton={TextButton(onClick={moveTarget=null}){Text("Cancel")}}
        )
    }
}
