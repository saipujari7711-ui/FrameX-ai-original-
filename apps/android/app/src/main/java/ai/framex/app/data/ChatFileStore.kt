package ai.framex.app.data

import android.content.Context
import org.json.JSONObject
import java.io.File

data class ChatRecord(
  val chatId: String,
  val title: String,
  val category: String = "Other",
  val projectId: String? = null,
  val createdAt: String,
  val updatedAt: String,
  val revision: Long = 0,
  val messagesJson: String = "[]",
  val attachmentsJson: String = "[]",
  val modelMetadataJson: String = "{}"
)

class ChatFileStore(context: Context) {
  private val directory = File(context.filesDir, "FrameX AI/All Chats").apply { mkdirs() }

  fun save(chat: ChatRecord) {
    val json = JSONObject()
      .put("schemaVersion", 1)
      .put("chatId", chat.chatId)
      .put("title", chat.title)
      .put("createdAt", chat.createdAt)
      .put("updatedAt", chat.updatedAt)
      .put("category", chat.category)
      .put("projectId", chat.projectId)
      .put("messages", JSONObject().let { org.json.JSONArray(chat.messagesJson) })
      .put("attachments", org.json.JSONArray(chat.attachmentsJson))
      .put("modelMetadata", JSONObject(chat.modelMetadataJson))
      .put("revision", chat.revision)

    File(directory, "${chat.chatId}.json").writeText(json.toString())
  }

  fun read(chatId: String): String? {
    val file = File(directory, "${chatId}.json")
    return file.takeIf(File::exists)?.readText()
  }

  fun delete(chatId: String) {
    File(directory, "${chatId}.json").delete()
  }

  fun listChatFiles(): List<File> =
    directory.listFiles { file -> file.extension == "json" }
      ?.sortedByDescending { it.lastModified() }
      ?: emptyList()
}
