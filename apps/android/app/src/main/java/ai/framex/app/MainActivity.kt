package ai.framex.app

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.framex.app.core.FrameAttachment
import ai.framex.app.core.FrameChat
import ai.framex.app.core.FrameMemory
import ai.framex.app.core.FrameMessage
import ai.framex.app.core.FrameMode
import ai.framex.app.core.FrameXEngine
import ai.framex.app.ui.FrameXLogo
import ai.framex.app.ui.FrameXTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private enum class Destination { CHAT, HISTORY, MEMORY, SETTINGS }

private data class ProviderUi(val id: String, val name: String, val model: String)

private val providers = listOf(
    ProviderUi("auto", "Smart Route", "Chooses a capable configured provider"),
    ProviderUi("gemini", "Gemini", "Gemini 3.6 Flash"),
    ProviderUi("groq", "Groq", "Llama 3.3 70B"),
    ProviderUi("nvidia", "NVIDIA", "Llama 3.1 405B")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { FrameXTheme { FrameXApp(applicationContext) } }
    }
}

@Composable
private fun FrameXApp(context: Context) {
    val engine = remember { FrameXEngine(context) }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    var destination by rememberSaveable { mutableStateOf(Destination.CHAT) }
    var chats by remember { mutableStateOf(engine.chats()) }
    var memories by remember { mutableStateOf(engine.memories()) }
    var currentChatId by rememberSaveable { mutableStateOf<String?>(null) }
    var messages by remember { mutableStateOf(emptyList<FrameMessage>()) }
    var input by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf(FrameMode.NORMAL) }
    var provider by rememberSaveable { mutableStateOf("auto") }
    var attachments by remember { mutableStateOf(emptyList<FrameAttachment>()) }
    var busy by remember { mutableStateOf(false) }
    var streaming by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var search by rememberSaveable { mutableStateOf("") }
    var showModelPicker by remember { mutableStateOf(false) }

    fun refresh() {
        chats = engine.chats()
        memories = engine.memories()
    }

    fun newChat() {
        currentChatId = null
        messages = emptyList()
        input = ""
        mode = FrameMode.NORMAL
        provider = "auto"
        attachments = emptyList()
        streaming = ""
        error = ""
    }

    fun openChat(chat: FrameChat) {
        currentChatId = chat.id
        messages = chat.messages
        destination = Destination.CHAT
        error = ""
    }

    fun saveCurrentChat() {
        if (messages.isEmpty()) return
        val first = messages.firstOrNull { it.role == "user" }?.content ?: "New chat"
        val title = first.replace("\n", " ").trim().let { if (it.length > 56) it.take(56) + "…" else it }
        val category = when {
            first.contains(Regex("(?i)\\b(pdf|study|exam|notes|college|assignment)\\b")) -> "Study"
            first.contains(Regex("(?i)\\b(research|latest|news|compare|analysis)\\b")) -> "Research"
            first.contains(Regex("(?i)\\b(code|coding|python|java|kotlin|html|css|debug)\\b")) -> "Coding"
            first.contains(Regex("(?i)\\b(plan|planning|strategy|goal|roadmap)\\b")) -> "Planning"
            first.contains(Regex("(?i)\\b(automate|automation|workflow|n8n)\\b")) -> "Automation"
            else -> "Other"
        }
        val id = currentChatId ?: UUID.randomUUID().toString()
        currentChatId = id
        engine.saveChat(FrameChat(id, title, category, System.currentTimeMillis(), messages))
        refresh()
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            val loaded = withContext(Dispatchers.IO) {
                uris.take(8).mapNotNull { uri ->
                    runCatching {
                        val name = uri.lastPathSegment?.substringAfterLast('/') ?: "attachment"
                        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
                        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            ?: ByteArray(0)
                        require(bytes.size <= 25 * 1024 * 1024) { "$name is larger than 25 MB" }
                        FrameAttachment(name, mime, bytes)
                    }.getOrElse {
                        error = it.message ?: "Attachment failed"
                        null
                    }
                }
            }
            attachments = attachments + loaded
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(engine.exportAll().toByteArray())
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val raw = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                            ?: error("Empty backup")
                    }
                    engine.importAll(raw)
                    refresh()
                }.onFailure { error = it.message ?: "Import failed" }
            }
        }
    }

    fun sendMessage() {
        val text = input.trim()
        if (busy || (text.isEmpty() && attachments.isEmpty())) return

        if (text.matches(Regex("(?i)^(remember|save this|don't forget).*"))) {
            val memory = text.replace(
                Regex("(?i)^(remember|save this|don't forget)\\s*(that|this)?\\s*:?"),
                ""
            ).trim()
            if (memory.isNotBlank()) {
                engine.addMemory(memory)
                memories = engine.memories()
                messages = messages + FrameMessage(role = "user", content = text) +
                    FrameMessage(role = "assistant", content = "Saved to FrameX memory.")
                input = ""
                saveCurrentChat()
                return
            }
        }

        val prompt = text.ifBlank { "Please analyze the attached file(s)." }
        val selectedAttachments = attachments
        val preferred = if (provider == "auto") null else provider
        input = ""
        attachments = emptyList()
        error = ""
        streaming = ""
        busy = true
        messages = messages + FrameMessage(role = "user", content = prompt)

        scope.launch {
            try {
                val (answer, usedProvider) = engine.send(
                    prompt,
                    messages,
                    mode,
                    selectedAttachments,
                    preferred
                ) { chunk -> Handler(Looper.getMainLooper()).post { streaming += chunk } }

                streaming = ""
                messages = messages + FrameMessage(
                    role = "assistant",
                    content = answer,
                    provider = usedProvider
                )
                provider = usedProvider
                saveCurrentChat()
            } catch (t: Throwable) {
                streaming = ""
                error = t.message ?: "The request failed."
                saveCurrentChat()
            } finally {
                busy = false
                refresh()
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !busy,
        drawerContent = {
            FrameXDrawer(
                destination = destination,
                chats = chats,
                search = search,
                onSearch = { search = it },
                onNewChat = {
                    newChat()
                    destination = Destination.CHAT
                    scope.launch { drawerState.close() }
                },
                onDestination = {
                    destination = it
                    scope.launch { drawerState.close() }
                },
                onChat = {
                    openChat(it)
                    scope.launch { drawerState.close() }
                },
                onDeleteChat = {
                    engine.deleteChat(it)
                    refresh()
                },
                selectedChatId = currentChatId
            )
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                FrameXTopBar(
                    title = when (destination) {
                        Destination.CHAT -> chats.firstOrNull { it.id == currentChatId }?.title ?: "New chat"
                        Destination.HISTORY -> "Chat history"
                        Destination.MEMORY -> "Memory"
                        Destination.SETTINGS -> "Settings"
                    },
                    onMenu = { scope.launch { drawerState.open() } },
                    onNewChat = ::newChat
                )
            }
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (destination) {
                    Destination.CHAT -> ChatWorkspace(
                        messages = messages,
                        input = input,
                        onInput = { input = it },
                        mode = mode,
                        onMode = { mode = it },
                        provider = provider,
                        onProvider = { provider = it },
                        attachments = attachments,
                        onAttachments = { attachments = it },
                        busy = busy,
                        streaming = streaming,
                        error = error,
                        onPickAttachment = { picker.launch(arrayOf("*/*")) },
                        onModelPicker = { showModelPicker = true },
                        onSend = ::sendMessage
                    )

                    Destination.HISTORY -> HistoryWorkspace(
                        chats = chats,
                        query = search,
                        onQuery = { search = it },
                        onOpen = ::openChat,
                        onDelete = {
                            engine.deleteChat(it)
                            refresh()
                        }
                    )

                    Destination.MEMORY -> MemoryWorkspace(
                        memories = memories,
                        onAdd = { text, category ->
                            engine.addMemory(text, category)
                            refresh()
                        },
                        onDelete = {
                            engine.deleteMemory(it)
                            refresh()
                        },
                        onClear = {
                            engine.clearMemories()
                            refresh()
                        }
                    )

                    Destination.SETTINGS -> SettingsWorkspace(
                        engine = engine,
                        onExport = { exportLauncher.launch("framex-backup.json") },
                        onImport = { importLauncher.launch(arrayOf("application/json")) },
                        onNewChat = ::newChat
                    )
                }
            }
        }
    }

    if (showModelPicker) {
        ModelPickerSheet(
            selected = provider,
            engine = engine,
            onSelect = {
                provider = it
                showModelPicker = false
            },
            onDismiss = { showModelPicker = false }
        )
    }
}

@Composable
private fun FrameXTopBar(
    title: String,
    onMenu: () -> Unit,
    onNewChat: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .heightIn(min = 64.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onMenu,
                modifier = Modifier.semantics { contentDescription = "Open navigation drawer" }
            ) {
                Text("☰", fontSize = 25.sp)
            }

            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FrameXLogo(Modifier.size(32.dp))
                Column(Modifier.padding(start = 9.dp)) {
                    Text(
                        "FRAME X AI",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        title,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }

            IconButton(
                onClick = onNewChat,
                modifier = Modifier.semantics { contentDescription = "Start new chat" }
            ) {
                Text("＋", fontSize = 25.sp)
            }
        }
    }
}

@Composable
private fun FrameXDrawer(
    destination: Destination,
    chats: List<FrameChat>,
    search: String,
    onSearch: (String) -> Unit,
    onNewChat: () -> Unit,
    onDestination: (Destination) -> Unit,
    onChat: (FrameChat) -> Unit,
    onDeleteChat: (String) -> Unit,
    selectedChatId: String?
) {
    val filtered = remember(chats, search) {
        chats.filter {
            search.isBlank() ||
                it.title.contains(search, true) ||
                it.category.contains(search, true) ||
                it.messages.any { message -> message.content.contains(search, true) }
        }
    }

    ModalDrawerSheet(
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(max = 380.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FrameXLogo(Modifier.size(42.dp))
                Column(Modifier.padding(start = 10.dp)) {
                    Text("FRAME X AI", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Personal AI workspace", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Button(
                onClick = onNewChat,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(15.dp)
            ) {
                Text("＋  New chat", fontWeight = FontWeight.SemiBold)
            }

            OutlinedTextField(
                value = search,
                onValueChange = onSearch,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                singleLine = true,
                placeholder = { Text("Search chats") },
                leadingIcon = { Text("⌕") }
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DrawerDestination("Chat", "✦", destination == Destination.CHAT) {
                    onDestination(Destination.CHAT)
                }
                DrawerDestination("History", "◷", destination == Destination.HISTORY) {
                    onDestination(Destination.HISTORY)
                }
                DrawerDestination("Memory", "◇", destination == Destination.MEMORY) {
                    onDestination(Destination.MEMORY)
                }
                DrawerDestination("Settings", "⚙", destination == Destination.SETTINGS) {
                    onDestination(Destination.SETTINGS)
                }
            }

            Text(
                "CONVERSATIONS",
                Modifier.padding(start = 18.dp, top = 16.dp, bottom = 8.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyColumn(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (filtered.isEmpty()) {
                    item {
                        Text(
                            if (search.isBlank()) "No conversations yet." else "No matching conversations.",
                            Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(filtered, key = { it.id }) { chat ->
                    DrawerChatRow(
                        chat = chat,
                        selected = chat.id == selectedChatId,
                        onClick = { onChat(chat) },
                        onDelete = { onDeleteChat(chat.id) }
                    )
                }
            }

            HorizontalDivider()
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    Modifier.size(40.dp),
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) { Text("S", fontWeight = FontWeight.Bold) }
                }
                Column(Modifier.padding(start = 10.dp)) {
                    Text("FRAME X user", fontWeight = FontWeight.SemiBold)
                    Text("Local workspace", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun DrawerDestination(
    label: String,
    icon: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text("$icon  $label") },
        modifier = Modifier.padding(start = 6.dp)
    )
}

@Composable
private fun DrawerChatRow(
    chat: FrameChat,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .45f)
        else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(chat.title, maxLines = 1, fontWeight = FontWeight.Medium)
                Text(
                    chat.category,
                    maxLines = 1,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onDelete) { Text("×") }
        }
    }
}

@Composable
private fun ChatWorkspace(
    messages: List<FrameMessage>,
    input: String,
    onInput: (String) -> Unit,
    mode: FrameMode,
    onMode: (FrameMode) -> Unit,
    provider: String,
    onProvider: (String) -> Unit,
    attachments: List<FrameAttachment>,
    onAttachments: (List<FrameAttachment>) -> Unit,
    busy: Boolean,
    streaming: String,
    error: String,
    onPickAttachment: () -> Unit,
    onModelPicker: () -> Unit,
    onSend: () -> Unit
) {
    val listState = rememberLazyListState()
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(messages.size, streaming) {
        if (messages.isNotEmpty() || streaming.isNotEmpty()) {
            listState.animateScrollToItem(
                (messages.size + if (streaming.isNotEmpty()) 1 else 0).coerceAtLeast(0)
            )
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModeChip("Normal", mode == FrameMode.NORMAL) { onMode(FrameMode.NORMAL) }
            Spacer(Modifier.width(6.dp))
            ModeChip("Code", mode == FrameMode.CODE) { onMode(FrameMode.CODE) }
            Spacer(Modifier.width(6.dp))
            ModeChip("Video", mode == FrameMode.VIDEO) { onMode(FrameMode.VIDEO) }
            Spacer(Modifier.weight(1f))
            AssistChip(
                onClick = onModelPicker,
                label = { Text(providers.firstOrNull { it.id == provider }?.name ?: provider.uppercase()) },
                leadingIcon = { Text("◈") }
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (messages.isEmpty() && streaming.isEmpty()) {
                item { WelcomeState(onSuggestion = onInput) }
            }

            items(messages, key = { it.id }) { message ->
                MessageBubble(message)
            }

            if (streaming.isNotEmpty()) {
                item {
                    MessageBubble(
                        FrameMessage(
                            role = "assistant",
                            content = streaming,
                            provider = provider
                        )
                    )
                }
            }

            if (busy && streaming.isEmpty()) {
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                "Thinking…",
                                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            if (error.isNotBlank()) {
                item {
                    Surface(
                        Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            "⚠  $error",
                            Modifier.padding(14.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        Composer(
            input = input,
            onInput = onInput,
            mode = mode,
            provider = provider,
            attachments = attachments,
            onAttachments = onAttachments,
            busy = busy,
            onPickAttachment = onPickAttachment,
            onModelPicker = onModelPicker,
            onSend = {
                keyboard?.hide()
                onSend()
            }
        )
    }
}

@Composable
private fun WelcomeState(onSuggestion: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 54.dp, bottom = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            Modifier.size(72.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                FrameXLogo(Modifier.size(52.dp))
            }
        }
        Text(
            "How can FrameX help?",
            Modifier.padding(top = 20.dp),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "Ask, create, code, research, or generate.",
            Modifier.padding(top = 7.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val suggestions = listOf(
            "Explain this concept simply",
            "Help me plan my next project",
            "Write and debug C code",
            "Turn an idea into a video prompt"
        )

        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEach { suggestion ->
                OutlinedButton(
                    onClick = { onSuggestion(suggestion) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(suggestion, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}

@Composable
private fun Composer(
    input: String,
    onInput: (String) -> Unit,
    mode: FrameMode,
    provider: String,
    attachments: List<FrameAttachment>,
    onAttachments: (List<FrameAttachment>) -> Unit,
    busy: Boolean,
    onPickAttachment: () -> Unit,
    onModelPicker: () -> Unit,
    onSend: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            if (attachments.isNotEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    attachments.forEachIndexed { index, attachment ->
                        AssistChip(
                            onClick = {},
                            label = { Text(attachment.name.take(20), maxLines = 1) },
                            trailingIcon = {
                                Text(
                                    "×",
                                    Modifier.clickable {
                                        onAttachments(
                                            attachments.filterIndexed { i, _ -> i != index }
                                        )
                                    }
                                )
                            }
                        )
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                IconButton(
                    onClick = onPickAttachment,
                    enabled = !busy,
                    modifier = Modifier.semantics { contentDescription = "Add attachment" }
                ) {
                    Text("＋", fontSize = 24.sp)
                }

                OutlinedTextField(
                    value = input,
                    onValueChange = onInput,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 54.dp, max = 150.dp),
                    placeholder = {
                        Text(
                            when (mode) {
                                FrameMode.VIDEO -> "Describe the video you want…"
                                FrameMode.CODE -> "Ask for code or debugging help…"
                                else -> "Message FrameX…"
                            }
                        )
                    },
                    maxLines = 6,
                    minLines = 1,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Default
                    ),
                    keyboardActions = KeyboardActions.Default,
                    shape = RoundedCornerShape(18.dp)
                )

                Spacer(Modifier.width(6.dp))

                FilledIconButton(
                    onClick = onSend,
                    enabled = !busy && (input.isNotBlank() || attachments.isNotEmpty()),
                    modifier = Modifier.semantics { contentDescription = "Send message" }
                ) {
                    Text(if (busy) "…" else "↑", fontSize = 22.sp)
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, top = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onModelPicker, enabled = !busy) {
                    Text(
                        providers.firstOrNull { it.id == provider }?.name ?: provider.uppercase(),
                        fontSize = 12.sp
                    )
                }
                Text(
                    when {
                        busy -> "Generating…"
                        mode == FrameMode.VIDEO -> "Video generation mode"
                        else -> "FrameX AI"
                    },
                    Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(message: FrameMessage) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.role == "user") Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 620.dp),
            color = if (message.role == "user") {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.padding(horizontal = 15.dp, vertical = 12.dp)) {
                if (message.provider.isNotBlank()) {
                    Text(
                        message.provider.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    message.content,
                    Modifier.padding(top = if (message.provider.isNotBlank()) 4.dp else 0.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelPickerSheet(
    selected: String,
    engine: FrameXEngine,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text("Choose model", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "Select a configured provider or let FrameX route automatically.",
                Modifier.padding(top = 5.dp, bottom = 16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            providers.forEach { provider ->
                val configured = provider.id == "auto" || engine.hasKey(provider.id)
                Surface(
                    color = if (selected == provider.id) MaterialTheme.colorScheme.primaryContainer
                    else Color.Transparent,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = configured) { onSelect(provider.id) }
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(provider.name, fontWeight = FontWeight.SemiBold)
                            Text(provider.model, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            when {
                                selected == provider.id -> "✓"
                                provider.id == "auto" -> "AUTO"
                                configured -> "READY"
                                else -> "SETUP"
                            },
                            color = if (configured) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(Modifier.height(5.dp))
            }
        }
    }
}

@Composable
private fun HistoryWorkspace(
    chats: List<FrameChat>,
    query: String,
    onQuery: (String) -> Unit,
    onOpen: (FrameChat) -> Unit,
    onDelete: (String) -> Unit
) {
    val filtered = chats.filter {
        query.isBlank() ||
            it.title.contains(query, true) ||
            it.category.contains(query, true) ||
            it.messages.any { message -> message.content.contains(query, true) }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            singleLine = true,
            placeholder = { Text("Search conversations") },
            leadingIcon = { Text("⌕") }
        )
        LazyColumn(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (filtered.isEmpty()) {
                item {
                    EmptyPanel(
                        title = if (query.isBlank()) "No chats yet" else "No matches",
                        message = "Your saved conversations will appear here."
                    )
                }
            }
            items(filtered, key = { it.id }) { chat ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onOpen(chat) }
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(chat.title, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(chat.category, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            Text(
                                chat.messages.lastOrNull()?.content ?: "",
                                maxLines = 2,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { onDelete(chat.id) }) { Text("Delete") }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryWorkspace(
    memories: List<FrameMemory>,
    onAdd: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onClear: () -> Unit
) {
    var text by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("General") }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(
            "Memory",
            Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "Only saved memory is included when FrameX prepares a provider request.",
            Modifier.padding(top = 5.dp, bottom = 12.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Save something for future chats") },
            maxLines = 4
        )
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("General", "Study", "Coding", "Planning").forEach {
                FilterChip(
                    selected = category == it,
                    onClick = { category = it },
                    label = { Text(it) },
                    modifier = Modifier.padding(end = 5.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onAdd(text.trim(), category)
                        text = ""
                    }
                }
            ) { Text("Save") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onClear, enabled = memories.isNotEmpty()) { Text("Clear all") }
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (memories.isEmpty()) {
                item { EmptyPanel("No saved memories", "You can say “remember this…” from a chat.") }
            }
            items(memories, key = { it.id }) { memory ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(memory.category, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            Text(memory.text)
                        }
                        TextButton(onClick = { onDelete(memory.id) }) { Text("Delete") }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsWorkspace(
    engine: FrameXEngine,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onNewChat: () -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp, bottom = 28.dp)
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Credentials stay in Android Keystore-backed encrypted storage.",
                Modifier.padding(top = 5.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item { ProviderKeyCard("Gemini", engine, "gemini") }
        item { ProviderKeyCard("Groq", engine, "groq") }
        item { ProviderKeyCard("NVIDIA", engine, "nvidia") }
        item { ProviderKeyCard("Magic Hour", engine, "magichour") }

        item {
            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Local data", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Chats and memory are stored locally. Export a JSON backup when you need a portable copy.",
                        Modifier.padding(top = 5.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onExport) { Text("Export") }
                        OutlinedButton(onClick = onImport) { Text("Import") }
                    }
                }
            }
        }

        item {
            OutlinedButton(onClick = onNewChat, modifier = Modifier.fillMaxWidth()) {
                Text("Start a new chat")
            }
        }
    }
}

@Composable
private fun ProviderKeyCard(
    label: String,
    engine: FrameXEngine,
    id: String
) {
    var value by rememberSaveable(id) { mutableStateOf("") }
    var saved by remember { mutableStateOf(engine.hasKey(id)) }

    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(label, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (saved) "Credential stored securely" else "Not configured",
                        fontSize = 12.sp,
                        color = if (saved) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (saved) TextButton(onClick = {
                    engine.clearKey(id)
                    saved = false
                    value = ""
                }) { Text("Remove") }
            }
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                placeholder = { Text("Paste API key") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            Button(
                onClick = {
                    if (value.isNotBlank()) {
                        engine.setKey(id, value)
                        value = ""
                        saved = true
                    }
                },
                modifier = Modifier.padding(top = 8.dp)
            ) { Text(if (saved) "Replace key" else "Save key") }
        }
    }
}

@Composable
private fun EmptyPanel(title: String, message: String) {
    Surface(
        Modifier.fillMaxWidth().padding(top = 30.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                message,
                Modifier.padding(top = 5.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/*
 * Keeps the drawer destination row horizontally scrollable without introducing
 * fixed-width layout assumptions on narrow phones.
 */
private fun Modifier.horizontalScrollIfNeeded(): Modifier =
    this.horizontalScroll(rememberScrollState())
