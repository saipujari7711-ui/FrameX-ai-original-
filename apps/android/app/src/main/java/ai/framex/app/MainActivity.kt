package ai.framex.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ai.framex.app.core.*
import ai.framex.app.ui.FrameXLogo
import ai.framex.app.ui.FrameXTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private enum class Page(val label:String,val glyph:String){CHAT("Chat","✦"),HISTORY("History","◷"),MEMORY("Memory","◇"),SETTINGS("Settings","⚙")}

class MainActivity:ComponentActivity(){
  override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{FrameXTheme{FrameXApp(applicationContext)}}}
}

@Composable
private fun FrameXApp(context:Context){
  val engine=remember{FrameXEngine(context)}
  var page by remember{mutableStateOf(Page.CHAT)}
  var mode by remember{mutableStateOf(FrameMode.NORMAL)}
  var provider by remember{mutableStateOf("auto")}
  var chats by remember{mutableStateOf(engine.chats())}
  var memories by remember{mutableStateOf(engine.memories())}
  var current by remember{mutableStateOf<FrameChat?>(null)}
  var messages by remember{mutableStateOf(listOf<FrameMessage>())}
  var input by remember{mutableStateOf("")}
  var attachments by remember{mutableStateOf(listOf<FrameAttachment>())}
  var busy by remember{mutableStateOf(false)}
  var streaming by remember{mutableStateOf("")}
  var error by remember{mutableStateOf("")}
  var search by remember{mutableStateOf("")}
  val scope=rememberCoroutineScope()
  val main=remember{Handler(Looper.getMainLooper())}

  fun newChat(){current=null;messages=emptyList();input="";attachments=emptyList();mode=FrameMode.NORMAL;error="";streaming=""}
  fun refresh(){chats=engine.chats();memories=engine.memories()}
  fun saveChat(){
    if(messages.isEmpty())return
    val first=messages.firstOrNull{it.role=="user"}?.content?:"New chat"
    val title=first.replace("\n"," ").trim().let{if(it.length>48)it.take(48)+"…" else it}
    val cat=when{
      first.lowercase().contains(Regex("\\b(pdf|study|exam|notes|college|assignment)\\b"))->"Study"
      first.lowercase().contains(Regex("\\b(research|latest|news|compare|analysis)\\b"))->"Research"
      first.lowercase().contains(Regex("\\b(code|coding|python|java|kotlin|html|css|debug)\\b"))->"Coding"
      first.lowercase().contains(Regex("\\b(plan|planning|strategy|goal|roadmap)\\b"))->"Planning"
      first.lowercase().contains(Regex("\\b(automate|automation|workflow|n8n)\\b"))->"Automation"
      else->"Other"
    }
    val id=current?.id?:UUID.randomUUID().toString()
    current=FrameChat(id,title,cat,System.currentTimeMillis(),messages)
    engine.saveChat(current!!);refresh()
  }

  val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()){uris->
    if(uris.isEmpty())return@rememberLauncherForActivityResult
    scope.launch{
      val loaded=withContext(Dispatchers.IO){uris.take(8).mapNotNull{uri->
        runCatching{
          val name=uri.lastPathSegment?.substringAfterLast('/')?:"attachment"
          val mime=context.contentResolver.getType(uri)?:"application/octet-stream"
          val bytes=context.contentResolver.openInputStream(uri)?.use{it.readBytes()}?:ByteArray(0)
          if(bytes.size>25*1024*1024)throw IllegalStateException(name+" is larger than 25 MB")
          FrameAttachment(name,mime,bytes)
        }.getOrElse{error=it.message?:"Attachment failed";null}
      }}
      attachments=attachments+loaded
    }
  }

  val exportLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->
    if(uri!=null)scope.launch{withContext(Dispatchers.IO){context.contentResolver.openOutputStream(uri)?.use{it.write(engine.exportAll().toByteArray())}}}
  }
  val importLauncher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
    if(uri!=null)scope.launch{runCatching{val raw=withContext(Dispatchers.IO){context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()?:throw IllegalStateException("Empty backup")};engine.importAll(raw);refresh()}.onFailure{error=it.message?:"Import failed"}}}

  fun send(){
    val raw=input.trim()
    if(raw.isEmpty()&&attachments.isEmpty())return
    if(busy)return
    if(raw.matches(Regex("(?i)^(remember|save this|don't forget).*"))){
      val mem=raw.replace(Regex("(?i)^(remember|save this|don't forget)\\s*(that|this)?\\s*:?" ),"").trim()
      if(mem.isNotBlank()){engine.addMemory(mem);memories=engine.memories();messages=messages+FrameMessage(role="user",content=raw)+FrameMessage(role="assistant",content="Saved to memory.");saveChat();input="";return}
    }
    val prompt=raw.ifBlank{"Please analyze the attached file(s)."}
    val localAttachments=attachments
    input="";attachments=emptyList();busy=true;error="";streaming=""
    messages=messages+FrameMessage(role="user",content=prompt)
    scope.launch{
      try{
        var streamed=""
        val (answer,used)=engine.send(prompt,messages,mode,localAttachments,if(provider=="auto")null else provider){chunk->main.post{streamed+=chunk;streaming=streamed}}
        streaming=""
        messages=messages+FrameMessage(role="assistant",content=answer,provider=used)
        provider=used
        saveChat()
      }catch(e:Throwable){streaming="";error=e.message?:"Request failed";saveChat()}
      finally{busy=false;refresh()}
    }
  }

  Scaffold(
    topBar={
      Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
        Row(verticalAlignment=Alignment.CenterVertically){FrameXLogo(Modifier.size(36.dp));Text("FRAME X AI",Modifier.padding(start=8.dp),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)}
        TextButton(onClick={newChat()}){Text("＋ New chat")}
      }
    },
    bottomBar={
      NavigationBar(containerColor=Color(0xFF07111B)){
        Page.entries.forEach{p->NavigationBarItem(selected=page==p,onClick={page=p},icon={Text(p.glyph)},label={Text(p.label)})}
      }
    }
  ){pad->
    Surface(Modifier.fillMaxSize().padding(pad),color=MaterialTheme.colorScheme.background){
      when(page){
        Page.CHAT->ChatPage(messages,input,{input=it},mode,{mode=it},provider,{provider=it},busy,streaming,error,attachments,{attachments=it},{picker.launch(arrayOf("*/*"))},::send)
        Page.HISTORY->HistoryPage(chats,search,{search=it},{c->current=c;messages=c.messages;page=Page.CHAT},{id->engine.deleteChat(id);refresh()})
        Page.MEMORY->MemoryPage(memories,{text,cat->engine.addMemory(text,cat);refresh()},{id->engine.deleteMemory(id);refresh()},{engine.clearMemories();refresh()})
        Page.SETTINGS->SettingsPage(engine,exportLauncher,importLauncher,{newChat();page=Page.CHAT})
      }
    }
  }
}

@Composable
private fun ChatPage(messages:List<FrameMessage>,input:String,onInput:(String)->Unit,mode:FrameMode,onMode:(FrameMode)->Unit,provider:String,onProvider:(String)->Unit,busy:Boolean,streaming:String,error:String,attachments:List<FrameAttachment>,onAttachments:(List<FrameAttachment>)->Unit,onPick:()->Unit,onSend:()->Unit){
  Column(Modifier.fillMaxSize()){
    Row(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=4.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
      Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){FilterChip(mode==FrameMode.NORMAL,{onMode(FrameMode.NORMAL)},label={Text("Normal")});FilterChip(mode==FrameMode.CODE,{onMode(FrameMode.CODE)},label={Text("Code")});FilterChip(mode==FrameMode.VIDEO,{onMode(FrameMode.VIDEO)},label={Text("Video")})}
      Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){listOf("auto","gemini","groq","nvidia").forEach{p->FilterChip(provider==p,{onProvider(p)},label={Text(if(p=="auto")"Auto" else p.uppercase(),style=MaterialTheme.typography.labelSmall)})}}
    }
    LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal=12.dp),verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(vertical=12.dp)){
      if(messages.isEmpty()&&streaming.isEmpty())item{
        Column(Modifier.fillMaxWidth().padding(top=60.dp),horizontalAlignment=Alignment.CenterHorizontally){
          Text("FrameX",style=MaterialTheme.typography.headlineLarge)
          Text("Your personal AI workspace",color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=6.dp))
          Text("Smart routing · persistent memory · chat history · attachments · video mode",color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=12.dp))
        }
      }
      items(messages){m->MessageBubble(m)}
      if(streaming.isNotEmpty())item{MessageBubble(FrameMessage(role="assistant",content=streaming,provider=provider))}
      if(busy&&streaming.isEmpty())item{Text("Thinking…",color=MaterialTheme.colorScheme.primary)}
      if(error.isNotBlank())item{Surface(color=Color(0x331F0A0A),shape=RoundedCornerShape(12.dp)){Text("⚠ "+error,Modifier.padding(12.dp),color=MaterialTheme.colorScheme.error)}}
    }
    if(attachments.isNotEmpty())Row(Modifier.fillMaxWidth().padding(horizontal=10.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)){
      attachments.forEachIndexed{index,a->AssistChip(onClick={},label={Text(a.name.take(18))},trailingIcon={Text("×",Modifier.clickable{onAttachments(attachments.filterIndexed{i,_->i!=index})})})}
    }
    Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.Bottom){
      IconButton(onClick=onPick){Text("＋")}
      OutlinedTextField(value=input,onValueChange=onInput,modifier=Modifier.weight(1f),placeholder={Text(if(mode==FrameMode.VIDEO)"Describe the video…" else "Ask FrameX…")},maxLines=5)
      Button(onClick=onSend,enabled=!busy){Text("↑")}
    }
  }
}

@Composable private fun MessageBubble(m:FrameMessage){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=if(m.role=="user")Arrangement.End else Arrangement.Start){
    Surface(color=if(m.role=="user")Color(0xFF163B43) else Color(0xFF0D1822),shape=RoundedCornerShape(16.dp),modifier=Modifier.widthIn(max=360.dp)){
      Column(Modifier.padding(14.dp)){if(m.provider.isNotBlank())Text(m.provider.uppercase(),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary);Text(m.content,Modifier.padding(top=4.dp))}
    }
  }
}

@Composable private fun HistoryPage(chats:List<FrameChat>,query:String,onQuery:(String)->Unit,onOpen:(FrameChat)->Unit,onDelete:(String)->Unit){
  val filtered=chats.filter{query.isBlank()||it.title.contains(query,true)||it.category.contains(query,true)||it.messages.any{m->m.content.contains(query,true)}}
  Column(Modifier.fillMaxSize().padding(14.dp)){Text("Chat history",style=MaterialTheme.typography.headlineLarge);OutlinedTextField(query,onQuery,Modifier.fillMaxWidth().padding(vertical=10.dp),placeholder={Text("Search conversations…")})
    LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(filtered){c->Surface(color=MaterialTheme.colorScheme.surface,shape=RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth().clickable{onOpen(c)}){Row(Modifier.padding(14.dp),horizontalArrangement=Arrangement.SpaceBetween){Column(Modifier.weight(1f)){Text(c.title,fontWeight=FontWeight.SemiBold);Text(c.category,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary);Text(c.messages.lastOrNull()?.content?.take(100)?:"",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}};TextButton(onClick={onDelete(c.id)}){Text("Delete")}}}}
  }
}

@Composable private fun MemoryPage(memories:List<FrameMemory>,onAdd:(String,String)->Unit,onDelete:(String)->Unit,onClear:()->Unit){
  var text by remember{mutableStateOf("")};var cat by remember{mutableStateOf("General")}
  Column(Modifier.fillMaxSize().padding(14.dp)){Text("Memory",style=MaterialTheme.typography.headlineLarge);Text("Only relevant memory is sent with AI requests.",color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(vertical=6.dp))
    OutlinedTextField(text,{text=it},Modifier.fillMaxWidth(),placeholder={Text("Save a memory…")})
    Row(horizontalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.padding(vertical=8.dp)){listOf("General","Study","Coding","Planning").forEach{FilterChip(cat==it,{cat=it},label={Text(it)})};Button(onClick={if(text.isNotBlank()){onAdd(text,cat);text=""}}){Text("Save")}}
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){TextButton(onClick=onClear){Text("Clear all")}}
    LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(memories){m->Surface(color=MaterialTheme.colorScheme.surface,shape=RoundedCornerShape(12.dp),modifier=Modifier.fillMaxWidth()){Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(m.category,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary);Text(m.text)};TextButton(onClick={onDelete(m.id)}){Text("Delete")}}}}}
  }
}

@Composable private fun SettingsPage(engine:FrameXEngine,exportLauncher:androidx.activity.result.ActivityResultLauncher<String>,importLauncher:androidx.activity.result.ActivityResultLauncher<Array<String>>,onNewChat:()->Unit){
  var gemini by remember{mutableStateOf("")};var groq by remember{mutableStateOf("")};var nvidia by remember{mutableStateOf("")};var mh by remember{mutableStateOf("")}
  LazyColumn(Modifier.fillMaxSize().padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Text("Settings",style=MaterialTheme.typography.headlineLarge);Text("Provider credentials are encrypted with Android Keystore.",color=MaterialTheme.colorScheme.onSurfaceVariant)}
    item{KeyRow("Gemini",gemini,{gemini=it},{engine.setKey("gemini",gemini);gemini=""},engine.hasKey("gemini"))}
    item{KeyRow("Groq",groq,{groq=it},{engine.setKey("groq",groq);groq=""},engine.hasKey("groq"))}
    item{KeyRow("NVIDIA",nvidia,{nvidia=it},{engine.setKey("nvidia",nvidia);nvidia=""},engine.hasKey("nvidia"))}
    item{KeyRow("Magic Hour",mh,{mh=it},{engine.setKey("magichour",mh);mh=""},engine.hasKey("magichour"))}
    item{Surface(color=MaterialTheme.colorScheme.surface,shape=RoundedCornerShape(14.dp)){Column(Modifier.padding(14.dp)){Text("Google Drive");Text("OAuth/Drive sync requires a Google Cloud Android OAuth client and consent configuration; the app does not fake a connected state.",color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)} }}
    item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={exportLauncher.launch("framex-backup.json")}){Text("Export data")};Button(onClick={importLauncher.launch(arrayOf("application/json"))}){Text("Import data")};TextButton(onClick=onNewChat){Text("New chat")}}}
  }
}

@Composable private fun KeyRow(label:String,value:String,onValue:(String)->Unit,onSave:()->Unit,set:Boolean){
  Column{Text(label,fontWeight=FontWeight.SemiBold);Row(verticalAlignment=Alignment.CenterVertically){OutlinedTextField(value,onValue,Modifier.weight(1f),singleLine=true,placeholder={Text(if(set)"Key saved securely" else "Paste API key")});Button(onClick=onSave,Modifier.padding(start=6.dp)){Text(if(set)"Replace" else "Save")}}}
}
