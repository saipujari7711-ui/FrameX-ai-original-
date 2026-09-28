package ai.framex.app.drive

import android.content.Context
import android.net.Uri
import androidx.work.*
import ai.framex.app.auth.GoogleAuthManager
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class DriveSyncItem(
    val id:String,
    val localPath:String,
    val fileName:String,
    val parentKey:String,
    val mimeType:String,
    val revision:Long
)

class DriveSyncQueue(private val context:Context) {
    private val prefs=context.getSharedPreferences("frame_x_drive_sync_queue",Context.MODE_PRIVATE)

    @Synchronized fun enqueue(item:DriveSyncItem) {
        val items=read().filterNot { it.id==item.id } + item
        val a=JSONArray()
        items.forEach { a.put(JSONObject().put("id",it.id).put("localPath",it.localPath).put("fileName",it.fileName).put("parentKey",it.parentKey).put("mimeType",it.mimeType).put("revision",it.revision)) }
        prefs.edit().putString("items",a.toString()).apply()
        schedule()
    }

    @Synchronized fun remove(id:String) {
        val a=JSONArray()
        read().filterNot { it.id==id }.forEach { a.put(JSONObject().put("id",it.id).put("localPath",it.localPath).put("fileName",it.fileName).put("parentKey",it.parentKey).put("mimeType",it.mimeType).put("revision",it.revision)) }
        prefs.edit().putString("items",a.toString()).apply()
    }

    fun read():List<DriveSyncItem> {
        val a=runCatching { JSONArray(prefs.getString("items","[]")) }.getOrElse { JSONArray() }
        return (0 until a.length()).mapNotNull { i -> runCatching {
            val o=a.getJSONObject(i)
            DriveSyncItem(o.getString("id"),o.getString("localPath"),o.getString("fileName"),o.getString("parentKey"),o.getString("mimeType"),o.optLong("revision"))
        }.getOrNull() }
    }

    private fun schedule() {
        val request=OneTimeWorkRequestBuilder<DriveSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("framex-drive-sync",ExistingWorkPolicy.REPLACE,request)
    }
}

class DriveSyncWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params) {
    override suspend fun doWork():ListenableWorker.Result {
        val queue=DriveSyncQueue(applicationContext)
        if(queue.read().isEmpty()) return ListenableWorker.Result.success()
        val auth=GoogleAuthManager(applicationContext)
        if(auth.savedAccount()==null) return ListenableWorker.Result.success()
        val token=auth.cachedAccessToken() ?: return ListenableWorker.Result.retry()
        val service=GoogleDriveService(applicationContext.contentResolver)
        val structure=runCatching { service.ensureStructure(token) }.getOrElse {
            if(it is DriveApiException && it.status==401) auth.clearAccessToken(token)
            return ListenableWorker.Result.retry()
        }
        for(item in queue.read()) {
            val file=File(item.localPath)
            if(!file.exists()){ queue.remove(item.id); continue }
            val parent=structure.folders[item.parentKey] ?: structure.folders["Other"] ?: continue
            val uri=Uri.fromFile(file)
            runCatching {
                service.upload(token,uri,parent,item.fileName,item.mimeType,props=mapOf(
                    "framexType" to "chat","framexId" to item.id,"framexRevision" to item.revision.toString()
                ))
            }.onSuccess { queue.remove(item.id) }.onFailure { return ListenableWorker.Result.retry() }
        }
        return ListenableWorker.Result.success()
    }
}
