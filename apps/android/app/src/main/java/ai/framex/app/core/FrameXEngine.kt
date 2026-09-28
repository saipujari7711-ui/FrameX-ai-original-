package ai.framex.app.core

import android.content.Context
import android.util.Base64
import ai.framex.app.security.SecureCredentialStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

data class FrameMessage(val id:String=UUID.randomUUID().toString(), val role:String, val content:String, val provider:String="", val createdAt:Long=System.currentTimeMillis())
data class FrameChat(val id:String, val title:String, val category:String, val updatedAt:Long, val messages:List<FrameMessage>)
data class FrameMemory(val id:String, val text:String, val category:String, val createdAt:Long)
data class FrameAttachment(val name:String, val mime:String, val bytes:ByteArray) {
  val isImage get() = mime.startsWith("image/")
  val isPdf get() = mime == "application/pdf"
}
enum class FrameMode { NORMAL, CODE, VIDEO }

class FrameXEngine(private val context:Context) {
  private val secure=SecureCredentialStore(context)
  private val prefs=context.getSharedPreferences("frame_x_app",Context.MODE_PRIVATE)
  private val chatDir=context.filesDir.resolve("FrameX AI/All Chats").apply{mkdirs()}

  fun setKey(provider:String,value:String){if(value.isBlank())secure.remove(provider) else secure.put(provider,value.trim())}
  fun hasKey(provider:String)=!secure.get(provider).isNullOrBlank()
  fun clearKey(provider:String)=secure.remove(provider)

  fun memories():List<FrameMemory>{
    val a=runCatching{JSONArray(prefs.getString("memories","[]"))}.getOrElse{return emptyList()}
    return (0 until a.length()).mapNotNull{runCatching{val o=a.getJSONObject(it);FrameMemory(o.getString("id"),o.getString("text"),o.optString("category","General"),o.optLong("createdAt"))}.getOrNull()}
  }
  fun addMemory(text:String,category:String="General"){
    val all=memories().toMutableList()
    all.add(0,FrameMemory(UUID.randomUUID().toString(),text.trim(),category,System.currentTimeMillis()))
    val a=JSONArray();all.forEach{a.put(JSONObject().put("id",it.id).put("text",it.text).put("category",it.category).put("createdAt",it.createdAt))}
    prefs.edit().putString("memories",a.toString()).apply()
  }
  fun deleteMemory(id:String){
    val a=JSONArray();memories().filterNot{it.id==id}.forEach{a.put(JSONObject().put("id",it.id).put("text",it.text).put("category",it.category).put("createdAt",it.createdAt))}
    prefs.edit().putString("memories",a.toString()).apply()
  }
  fun clearMemories(){prefs.edit().remove("memories").apply()}

  fun saveChat(chat:FrameChat){
    val o=JSONObject().put("schemaVersion",1).put("chatId",chat.id).put("title",chat.title).put("category",chat.category).put("updatedAt",chat.updatedAt)
    val a=JSONArray();chat.messages.forEach{a.put(JSONObject().put("id",it.id).put("role",it.role).put("content",it.content).put("provider",it.provider).put("createdAt",it.createdAt))}
    o.put("messages",a)
    chatDir.resolve(chat.id+".json").writeText(o.toString())
  }
  fun loadChat(id:String):FrameChat?=runCatching{
    val o=JSONObject(chatDir.resolve(id+".json").readText());val a=o.getJSONArray("messages")
    val ms=(0 until a.length()).map{val m=a.getJSONObject(it);FrameMessage(m.getString("id"),m.getString("role"),m.getString("content"),m.optString("provider"),m.optLong("createdAt"))}
    FrameChat(o.getString("chatId"),o.getString("title"),o.optString("category","Other"),o.optLong("updatedAt"),ms)
  }.getOrNull()
  fun chats():List<FrameChat>=chatDir.listFiles()?.filter{it.extension=="json"}?.mapNotNull{loadChat(it.nameWithoutExtension)}?.sortedByDescending{it.updatedAt}?:emptyList()
  fun deleteChat(id:String){chatDir.resolve(id+".json").delete()}

  fun exportAll():String{
    val root=JSONObject().put("version",1)
    val mem=JSONArray();memories().forEach{mem.put(JSONObject().put("id",it.id).put("text",it.text).put("category",it.category).put("createdAt",it.createdAt))}
    val cs=JSONArray();chats().forEach{c->val m=JSONArray();c.messages.forEach{m.put(JSONObject().put("id",it.id).put("role",it.role).put("content",it.content).put("provider",it.provider).put("createdAt",it.createdAt))};cs.put(JSONObject().put("id",c.id).put("title",c.title).put("category",c.category).put("updatedAt",c.updatedAt).put("messages",m))}
    return root.put("memories",mem).put("chats",cs).toString(2)
  }

  fun importAll(raw:String){
    val root=JSONObject(raw)
    val mem=root.optJSONArray("memories")?:JSONArray()
    prefs.edit().putString("memories",mem.toString()).apply()
    val cs=root.optJSONArray("chats")?:JSONArray()
    for(i in 0 until cs.length()){
      val x=cs.getJSONObject(i)
      val o=JSONObject().put("schemaVersion",1).put("chatId",x.getString("id")).put("title",x.optString("title","Imported chat")).put("category",x.optString("category","Other")).put("updatedAt",x.optLong("updatedAt",System.currentTimeMillis())).put("messages",x.optJSONArray("messages")?:JSONArray())
      chatDir.resolve(x.getString("id")+".json").writeText(o.toString())
    }
  }

  fun route(text:String,mode:FrameMode,attachments:List<FrameAttachment>):String{
    if(mode==FrameMode.VIDEO)return "magichour"
    if(attachments.any{it.isImage||it.isPdf})return if(hasKey("gemini"))"gemini" else "unavailable"
    val t=text.lowercase()
    return when{
      t.matches(Regex(".*\\b(code|program|debug|python|java|kotlin|javascript|html|css|api|regex|algorithm)\\b.*")) -> if(hasKey("groq"))"groq" else if(hasKey("gemini"))"gemini" else "unavailable"
      hasKey("gemini")->"gemini"
      hasKey("groq")->"groq"
      hasKey("nvidia")->"nvidia"
      else->"unavailable"
    }
  }

  suspend fun send(text:String,history:List<FrameMessage>,mode:FrameMode,attachments:List<FrameAttachment>,onChunk:(String)->Unit):Pair<String,String> = withContext(Dispatchers.IO){
    if(mode==FrameMode.VIDEO)return@withContext video(text) to "magichour"
    val primary=route(text,mode,attachments)
    if(primary=="unavailable")throw IllegalStateException("Connect an AI provider in Settings.")
    val order=linkedSetOf(primary,"gemini","groq","nvidia").filter{it!="magichour"&&hasKey(it)}
    var last:Throwable?=null
    for(p in order){try{
      val out=when(p){
        "gemini"->gemini(text,history,attachments,onChunk)
        "groq"->openAiCompatible("https://api.groq.com/openai/v1/chat/completions",secure.get("groq")!!,"llama-3.3-70b-versatile",text,history,onChunk)
        else->openAiCompatible("https://integrate.api.nvidia.com/v1/chat/completions",secure.get("nvidia")!!,"meta/llama-3.1-405b-instruct",text,history,onChunk)
      }
      return@withContext out to p
    }catch(e:Throwable){last=e}}
    throw last?:IllegalStateException("All configured providers failed.")
  }

  private fun post(url:String,key:String,body:String):HttpURLConnection{
    val c=URL(url).openConnection() as HttpURLConnection
    c.requestMethod="POST";c.connectTimeout=30000;c.readTimeout=120000;c.doOutput=true
    c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("Authorization","Bearer "+key)
    c.outputStream.use{it.write(body.toByteArray())};return c
  }
  private fun parseSse(c:HttpURLConnection,onChunk:(String)->Unit):String{
    if(c.responseCode !in 200..299)throw IllegalStateException("Provider HTTP "+c.responseCode)
    val full=StringBuilder();BufferedReader(InputStreamReader(c.inputStream)).useLines{lines->lines.forEach{line->
      val raw=line.removePrefix("data:").trim()
      if(raw.isBlank()||raw=="[DONE]")return@forEach
      runCatching{
        val o=JSONObject(raw)
        val text=o.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text","")
          ?:o.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("delta")?.optString("content","")
          ?:o.optJSONArray("choices")?.optJSONObject(0)?.optString("text","")
        if(!text.isNullOrEmpty()){full.append(text);onChunk(text)}
      }
    }}
    if(full.isEmpty())throw IllegalStateException("Provider returned an empty response")
    return full.toString()
  }
  private fun messagesJson(history:List<FrameMessage>,system:String):JSONArray{
    val a=JSONArray().put(JSONObject().put("role","system").put("content",system))
    history.takeLast(30).forEach{a.put(JSONObject().put("role",if(it.role=="assistant")"assistant" else "user").put("content",it.content))}
    return a
  }
  private fun openAiCompatible(url:String,key:String,model:String,text:String,history:List<FrameMessage>,onChunk:(String)->Unit):String{
    val msgs=messagesJson(history,"You are FRAME X AI. Be concise, accurate and professional.")
    msgs.put(JSONObject().put("role","user").put("content",text))
    return parseSse(post(url,key,JSONObject().put("model",model).put("messages",msgs).put("stream",true).put("max_tokens",2048).toString()),onChunk)
  }
  private fun gemini(text:String,history:List<FrameMessage>,attachments:List<FrameAttachment>,onChunk:(String)->Unit):String{
    val contents=JSONArray()
    contents.put(JSONObject().put("role","user").put("parts",JSONArray().put(JSONObject().put("text","System: You are FRAME X AI. Be concise, accurate and professional. Memory:\n"+memories().take(20).joinToString("\n"){it.text}))))
    history.takeLast(30).forEach{contents.put(JSONObject().put("role",if(it.role=="assistant")"model" else "user").put("parts",JSONArray().put(JSONObject().put("text",it.content))))}
    val parts=JSONArray().put(JSONObject().put("text",text))
    attachments.filter{it.isImage||it.isPdf}.forEach{parts.put(JSONObject().put("inlineData",JSONObject().put("mimeType",it.mime).put("data",Base64.encodeToString(it.bytes,Base64.NO_WRAP))))}
    contents.put(JSONObject().put("role","user").put("parts",parts))
    val body=JSONObject().put("contents",contents).put("generationConfig",JSONObject().put("maxOutputTokens",2048))
    val url="https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:streamGenerateContent?alt=sse&key="+URLEncoder.encode(secure.get("gemini")!!,"UTF-8")
    val c=URL(url).openConnection() as HttpURLConnection
    c.requestMethod="POST";c.connectTimeout=30000;c.readTimeout=120000;c.doOutput=true;c.setRequestProperty("Content-Type","application/json");c.outputStream.use{it.write(body.toString().toByteArray())}
    return parseSse(c,onChunk)
  }
  private fun video(prompt:String):String{
    val key=secure.get("magichour")?:throw IllegalStateException("Add a Magic Hour key in Settings.")
    val c=post("https://api.magichour.ai/v1/text-to-video",key,JSONObject().put("name","FrameX Video").put("end_seconds",5).put("model","kling-3.0").put("resolution","720p").put("style",JSONObject().put("prompt",prompt)).toString())
    if(c.responseCode !in 200..299)throw IllegalStateException("Magic Hour request failed: HTTP "+c.responseCode)
    val job=JSONObject(c.inputStream.bufferedReader().readText()).optString("id")
    if(job.isBlank())throw IllegalStateException("Magic Hour did not return a job ID")
    repeat(20){
      Thread.sleep(4000)
      val p=URL("https://api.magichour.ai/v1/video-projects/"+URLEncoder.encode(job,"UTF-8")).openConnection() as HttpURLConnection
      p.setRequestProperty("Authorization","Bearer "+key)
      if(p.responseCode in 200..299){
        val o=JSONObject(p.inputStream.bufferedReader().readText())
        when(o.optString("status").lowercase()){
          "complete"->{val u=o.optJSONArray("downloads")?.optJSONObject(0)?.optString("url");if(!u.isNullOrBlank())return u}
          "error"->throw IllegalStateException(o.optString("message","Video generation failed"))
        }
      }
    }
    throw IllegalStateException("Video generation timed out after 80 seconds.")
  }
}
