package ai.framex.app.drive

data class DriveFile(
    val id: String,
    val name: String,
    val mimeType: String,
    val parents: List<String> = emptyList(),
    val modifiedTime: String? = null,
    val size: Long? = null,
    val trashed: Boolean = false,
    val appProperties: Map<String, String> = emptyMap(),
    val capabilities: Map<String, Boolean> = emptyMap()
) {
    val isFolder: Boolean get() = mimeType == "application/vnd.google-apps.folder"
}

data class DriveStructure(val rootId: String, val folders: Map<String, String>)
