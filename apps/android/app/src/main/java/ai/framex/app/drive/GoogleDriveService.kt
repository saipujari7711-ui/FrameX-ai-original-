package ai.framex.app.drive

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

class DriveApiException(val status: Int, message: String) : Exception(message)

class GoogleDriveService(private val resolver: ContentResolver) {
    companion object {
        const val ROOT_NAME = "FrameX AI"
        const val FOLDER_MIME = "application/vnd.google-apps.folder"
        const val JSON_MIME = "application/json"
        private const val API = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3/files"
        private const val CHUNK = 8L * 1024L * 1024L
        val FOLDER_NAMES = listOf("All Chats","projects","Attachments","Study","Research","Coding","Planning","Automation","Other")
    }

    private fun conn(url: String, method: String, token: String, type: String? = null): HttpURLConnection {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = method
        c.connectTimeout = 30_000
        c.readTimeout = 120_000
        c.setRequestProperty("Authorization", "Bearer " + token)
        c.setRequestProperty("Accept", "application/json")
        if (type != null) c.setRequestProperty("Content-Type", type)
        return c
    }

    private fun check(c: HttpURLConnection) {
        if (c.responseCode in 200..299) return
        val msg = when (c.responseCode) {
            401 -> "Google Drive authorization expired or was revoked. Reconnect Drive."
            403 -> "Google Drive denied this operation. Check account permissions."
            404 -> "The requested Drive item was not found."
            408,429 -> "Google Drive is temporarily busy. Please retry."
            in 500..599 -> "Google Drive is temporarily unavailable."
            else -> "Google Drive request failed (HTTP " + c.responseCode + ")."
        }
        throw DriveApiException(c.responseCode, msg)
    }

    private fun parse(o: JSONObject) = DriveFile(
        o.getString("id"), o.optString("name"), o.optString("mimeType","application/octet-stream"),
        o.optJSONArray("parents").asStrings(), o.optString("modifiedTime").takeIf { it.isNotBlank() },
        o.optString("size").toLongOrNull(), o.optBoolean("trashed"),
        o.optJSONObject("appProperties").asMap(), o.optJSONObject("capabilities").asBoolMap()
    )

    private fun JSONArray?.asStrings(): List<String> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optString(it).takeIf(String::isNotBlank) }

    private fun JSONObject?.asMap(): Map<String,String> {
        if (this == null) return emptyMap()
        val m=mutableMapOf<String,String>(); keys().forEach { m[it]=optString(it) }; return m
    }

    private fun JSONObject?.asBoolMap(): Map<String,Boolean> {
        if (this == null) return emptyMap()
        val m=mutableMapOf<String,Boolean>(); keys().forEach { m[it]=optBoolean(it) }; return m
    }

    suspend fun list(token:String,parentId:String?=null,search:String?=null,pageToken:String?=null):Pair<List<DriveFile>,String?> =
        withContext(Dispatchers.IO) {
            val q=mutableListOf("trashed = false")
            if(parentId!=null) q += "'" + parentId.replace("'","\\'") + "' in parents"
            if(!search.isNullOrBlank()) {
                val safe=search.replace("'","\\\\'")
                q += "(name contains '" + safe + "' or fullText contains '" + safe + "')"
            }
            val fields=URLEncoder.encode("files(id,name,mimeType,parents,modifiedTime,size,trashed,appProperties,capabilities),nextPageToken","UTF-8")
            var u=API+"/files?q="+URLEncoder.encode(q.joinToString(" and "),"UTF-8")+"&spaces=drive&orderBy=folder,name&pageSize=100&fields="+fields+"&supportsAllDrives=true&includeItemsFromAllDrives=true"
            if(pageToken!=null) u += "&pageToken="+URLEncoder.encode(pageToken,"UTF-8")
            val c=conn(u,"GET",token); check(c)
            val root=JSONObject(c.inputStream.bufferedReader().readText())
            val a=root.optJSONArray("files")
            val items=if(a==null) emptyList() else (0 until a.length()).map { parse(a.getJSONObject(it)) }
            items to root.optString("nextPageToken").takeIf { it.isNotBlank() }
        }

    suspend fun search(token:String,text:String):List<DriveFile>{
        val out=mutableListOf<DriveFile>(); var page:String?=null
        do { val r=list(token,search=text,pageToken=page); out+=r.first; page=r.second } while(page!=null && out.size<500)
        return out
    }

    suspend fun metadata(token:String,id:String):DriveFile=withContext(Dispatchers.IO){
        val f=URLEncoder.encode("id,name,mimeType,parents,modifiedTime,size,trashed,appProperties,capabilities","UTF-8")
        val c=conn(API+"/files/"+URLEncoder.encode(id,"UTF-8")+"?fields="+f+"&supportsAllDrives=true","GET",token)
        check(c); parse(JSONObject(c.inputStream.bufferedReader().readText()))
    }

    private suspend fun findProperty(token:String,key:String,value:String):DriveFile?=withContext(Dispatchers.IO){
        val q=URLEncoder.encode("trashed = false and appProperties has { key='"+key+"' and value='"+value+"' }","UTF-8")
        val f=URLEncoder.encode("files(id,name,mimeType,parents,modifiedTime,size,trashed,appProperties,capabilities)","UTF-8")
        val c=conn(API+"/files?q="+q+"&spaces=drive&pageSize=20&fields="+f,"GET",token); check(c)
        val a=JSONObject(c.inputStream.bufferedReader().readText()).optJSONArray("files")
        if(a==null||a.length()==0)null else parse(a.getJSONObject(0))
    }

    private suspend fun findChildProperty(token:String,parent:String,value:String):DriveFile?=withContext(Dispatchers.IO){
        val q=URLEncoder.encode("'"+parent+"' in parents and trashed = false and appProperties has { key='framexFolder' and value='"+value+"' }","UTF-8")
        val f=URLEncoder.encode("files(id,name,mimeType,parents,modifiedTime,size,trashed,appProperties,capabilities)","UTF-8")
        val c=conn(API+"/files?q="+q+"&spaces=drive&pageSize=10&fields="+f,"GET",token); check(c)
        val a=JSONObject(c.inputStream.bufferedReader().readText()).optJSONArray("files")
        if(a==null||a.length()==0)null else parse(a.getJSONObject(0))
    }

    suspend fun ensureStructure(token:String):DriveStructure{
        var root=findProperty(token,"framexType","root")
        if(root==null) root=list(token,parentId="root",search=ROOT_NAME).first.firstOrNull { it.name==ROOT_NAME && it.isFolder }
        val rootFile=root ?: createFolder(token,ROOT_NAME,"root",mapOf("framexType" to "root"))
        val ids=mutableMapOf<String,String>()
        for(name in FOLDER_NAMES){
            val key=name.lowercase().replace("[^a-z0-9]+".toRegex(),"-")
            val existing=findChildProperty(token,rootFile.id,key)
                ?: list(token,parentId=rootFile.id,search=name).first.firstOrNull { it.name==name && it.isFolder }
            ids[name]=(existing ?: createFolder(token,name,rootFile.id,mapOf("framexFolder" to key))).id
        }
        return DriveStructure(rootFile.id,ids)
    }

    private suspend fun createFolder(token:String,name:String,parent:String,props:Map<String,String>):DriveFile =
        createMetadata(token,JSONObject().put("name",name).put("mimeType",FOLDER_MIME).put("parents",JSONArray().put(parent)).put("appProperties",JSONObject(props)))

    private suspend fun createMetadata(token:String,body:JSONObject):DriveFile=withContext(Dispatchers.IO){
        val f=URLEncoder.encode("id,name,mimeType,parents,modifiedTime,size,trashed,appProperties,capabilities","UTF-8")
        val c=conn(API+"/files?fields="+f+"&supportsAllDrives=true","POST",token,"application/json"); c.doOutput=true
        c.outputStream.use { it.write(body.toString().toByteArray()) }; check(c)
        parse(JSONObject(c.inputStream.bufferedReader().readText()))
    }

    suspend fun rename(token:String,id:String,name:String)=updateMetadata(token,id,JSONObject().put("name",name))

    suspend fun updateMetadata(token:String,id:String,body:JSONObject):DriveFile=withContext(Dispatchers.IO){
        val f=URLEncoder.encode("id,name,mimeType,parents,modifiedTime,size,trashed,appProperties,capabilities","UTF-8")
        val c=conn(API+"/files/"+URLEncoder.encode(id,"UTF-8")+"?fields="+f+"&supportsAllDrives=true","PATCH",token,"application/json")
        c.doOutput=true; c.outputStream.use { it.write(body.toString().toByteArray()) }; check(c)
        parse(JSONObject(c.inputStream.bufferedReader().readText()))
    }

    suspend fun move(token:String,id:String,destination:String):DriveFile{
        val old=metadata(token,id)
        val url=API+"/files/"+URLEncoder.encode(id,"UTF-8")+"?addParents="+URLEncoder.encode(destination,"UTF-8")+"&removeParents="+URLEncoder.encode(old.parents.joinToString(","),"UTF-8")+"&supportsAllDrives=true"
        return withContext(Dispatchers.IO){
            val c=conn(url,"PATCH",token,"application/json"); c.doOutput=true; c.outputStream.use { it.write("{}".toByteArray()) }; check(c)
            parse(JSONObject(c.inputStream.bufferedReader().readText()))
        }
    }

    suspend fun delete(token:String,id:String)=withContext(Dispatchers.IO){
        val c=conn(API+"/files/"+URLEncoder.encode(id,"UTF-8")+"?supportsAllDrives=true","DELETE",token); check(c); c.inputStream.close()
    }

    suspend fun download(token:String,id:String,out:OutputStream,onProgress:(Long,Long?)->Unit={_,_->})=withContext(Dispatchers.IO){
        val meta=metadata(token,id)
        val c=conn(API+"/files/"+URLEncoder.encode(id,"UTF-8")+"?alt=media&supportsAllDrives=true","GET",token); check(c)
        var done=0L
        BufferedInputStream(c.inputStream,64*1024).use { input ->
            BufferedOutputStream(out,64*1024).use { output ->
                val b=ByteArray(64*1024)
                while(true){ val n=input.read(b); if(n<0)break; output.write(b,0,n); done+=n; onProgress(done,meta.size) }
                output.flush()
            }
        }
    }

    suspend fun upload(token:String,uri:Uri,parent:String,fileName:String,mime:String,existingId:String?=null,props:Map<String,String> = emptyMap(),onProgress:(Long,Long?)->Unit={_,_->}):DriveFile=withContext(Dispatchers.IO){
        val length=resolver.openAssetFileDescriptor(uri,"r")?.use { it.length }?.takeIf { it>=0 } ?: throw IllegalArgumentException("Unable to determine file size.")
        val meta=JSONObject().put("name",fileName).put("mimeType",mime).put("appProperties",JSONObject(props))
        if(existingId==null) meta.put("parents",JSONArray().put(parent))
        val idPart=if(existingId==null) "" else "/"+URLEncoder.encode(existingId,"UTF-8")
        val method=if(existingId==null) "POST" else "PATCH"
        val init=conn(UPLOAD+idPart+"?uploadType=resumable",method,token,"application/json; charset=UTF-8")
        init.doOutput=true; init.setRequestProperty("X-Upload-Content-Type",mime); init.setRequestProperty("X-Upload-Content-Length",length.toString())
        init.outputStream.use { it.write(meta.toString().toByteArray()) }; check(init)
        val session=init.getHeaderField("Location") ?: throw IllegalStateException("Drive did not return an upload session.")
        var offset=0L
        resolver.openInputStream(uri)?.use { raw ->
            BufferedInputStream(raw,64*1024).use { input ->
                while(offset<length){
                    val size=minOf(CHUNK,length-offset).toInt(); val chunk=ByteArray(size); var filled=0
                    while(filled<size){ val n=input.read(chunk,filled,size-filled); if(n<0)break; filled+=n }
                    if(filled==0)break
                    val end=offset+filled-1
                    val c=conn(session,"PUT",token); c.doOutput=true; c.setFixedLengthStreamingMode(filled)
                    c.setRequestProperty("Content-Length",filled.toString()); c.setRequestProperty("Content-Range","bytes "+offset+"-"+end+"/"+length)
                    c.outputStream.use { it.write(chunk,0,filled) }
                    if(c.responseCode in 200..299) return@withContext parse(JSONObject(c.inputStream.bufferedReader().readText()))
                    if(c.responseCode==308){ val range=c.getHeaderField("Range"); offset=if(range!=null&&range.contains("-")) range.substringAfterLast("-").toLong()+1 else end+1; onProgress(offset,length); continue }
                    check(c)
                }
            }
        } ?: throw IllegalArgumentException("Unable to read selected file.")
        throw IllegalStateException("Upload ended before all file data was sent.")
    }

    suspend fun uploadText(token:String,parent:String,fileName:String,text:String,props:Map<String,String> = emptyMap()):DriveFile=withContext(Dispatchers.IO){
        val boundary="framex-"+UUID.randomUUID()
        val meta=JSONObject().put("name",fileName).put("mimeType",JSON_MIME).put("parents",JSONArray().put(parent)).put("appProperties",JSONObject(props))
        val body=("--"+boundary+"\\r\\nContent-Type: application/json; charset=UTF-8\\r\\n\\r\\n"+meta+"\\r\\n--"+boundary+"\\r\\nContent-Type: application/json\\r\\n\\r\\n"+text+"\\r\\n--"+boundary+"--\\r\\n").toByteArray()
        val f=URLEncoder.encode("id,name,mimeType,parents,modifiedTime,size,trashed,appProperties,capabilities","UTF-8")
        val c=conn(UPLOAD+"?uploadType=multipart&fields="+f,"POST",token,"multipart/related; boundary="+boundary); c.doOutput=true
        c.setFixedLengthStreamingMode(body.size); c.outputStream.use { it.write(body) }; check(c)
        parse(JSONObject(c.inputStream.bufferedReader().readText()))
    }
}
