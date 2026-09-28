package ai.framex.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ai.framex.app.ui.FrameXLogo
import ai.framex.app.ui.FrameXTheme
import kotlinx.coroutines.launch

private enum class Destination(val label: String, val glyph: String) {
  HOME("Home", "⌂"), CHAT("Chat", "✦"), HISTORY("History", "◷"), PROJECTS("Projects", "▱"), SEARCH("Search", "⌕"),
  CATEGORIES("Categories", "◈"), PROVIDERS("Providers", "◎"), MODELS("Models", "◇"), DRIVE("Google Drive", "↕"),
  SETTINGS("Settings", "⚙"), PROFILE("Profile", "○")
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { FrameXTheme { FrameXApp() } }
  }
}

@Composable
private fun FrameXApp() {
  var destination by remember { mutableStateOf(Destination.HOME) }
  val drawerState = rememberDrawerState(DrawerValue.Closed)
  val scope = rememberCoroutineScope()
  val primary = listOf(Destination.HOME, Destination.CHAT, Destination.HISTORY, Destination.PROJECTS, Destination.SEARCH)
  val go: (Destination) -> Unit = {
    destination = it
    scope.launch { drawerState.close() }
  }

  ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = {
      ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(22.dp).fillMaxWidth()) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            FrameXLogo()
            Column(Modifier.padding(start = 12.dp)) {
              Text("FRAME X", style = MaterialTheme.typography.titleLarge)
              Text("AI", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
          }
          Spacer(Modifier.height(22.dp))
          Text("WORKSPACE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Destination.entries.filter { it in primary }.forEach { item ->
            DrawerItem(item, destination == item) { go(item) }
          }
          Spacer(Modifier.height(14.dp))
          Text("SYSTEM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Destination.entries.filter { it !in primary }.forEach { item ->
            DrawerItem(item, destination == item) { go(item) }
          }
          Spacer(Modifier.weight(1f))
          HorizontalDivider(color = MaterialTheme.colorScheme.outline)
          Text("Sync status available when Drive is connected", modifier = Modifier.padding(top = 14.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    }
  ) {
    Scaffold(
      containerColor = MaterialTheme.colorScheme.background,
      topBar = {
        Row(
          Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(horizontal = 16.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          TextButton(onClick = { scope.launch { drawerState.open() } }) { Text("☰", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge) }
          Row(verticalAlignment = Alignment.CenterVertically) {
            FrameXLogo(modifier = Modifier.size(38.dp))
            Text("FRAME X AI", modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleLarge)
          }
          Text("S", modifier = Modifier.background(Color(0xFF102A3A), RoundedCornerShape(50)).padding(horizontal = 11.dp, vertical = 7.dp), color = MaterialTheme.colorScheme.primary)
        }
      },
      bottomBar = {
        NavigationBar(containerColor = Color(0xFF07111B), modifier = Modifier.navigationBarsPadding()) {
          primary.forEach { item ->
            NavigationBarItem(
              selected = destination == item,
              onClick = { go(item) },
              icon = { Text(item.glyph) },
              label = { Text(item.label) }
            )
          }
        }
      }
    ) { padding ->
      Surface(Modifier.fillMaxSize().padding(padding), color = MaterialTheme.colorScheme.background) {
        when (destination) {
          Destination.HOME -> HomeScreen { go(Destination.CHAT) }
          Destination.CHAT -> ChatScreen()
          Destination.HISTORY -> ListScreen("Conversation archive", "Every chat remains independently versioned and searchable.")
          Destination.PROJECTS -> ListScreen("Project library", "Group chats, files and instructions around a meaningful outcome.")
          Destination.SEARCH -> ListScreen("Search workspace", "Find conversations, files and project context.")
          Destination.CATEGORIES -> ListScreen("Your thinking, organized", "Study · Research · Coding · Planning · Automation · Other")
          Destination.PROVIDERS -> ProvidersScreen()
          Destination.MODELS -> ListScreen("Models & capabilities", "Supported · unsupported · unknown — never assume unknown is supported.")
          Destination.DRIVE -> DriveScreen()
          Destination.SETTINGS -> SettingsScreen()
          Destination.PROFILE -> ListScreen("Your FRAME X account", "Identity, devices, sync and account controls.")
        }
      }
    }
  }
}

@Composable
private fun DrawerItem(item: Destination, selected: Boolean, onClick: () -> Unit) {
  TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
    Row(Modifier.fillMaxWidth().background(if (selected) Color(0x1400D4FF) else Color.Transparent, RoundedCornerShape(10.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
      Text(item.glyph, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
      Text(item.label, color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

@Composable
private fun HomeScreen(openChat: () -> Unit) {
  LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    item { Spacer(Modifier.height(12.dp)); Text("FRAME X / WORKSPACE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("Good morning.", style = MaterialTheme.typography.headlineLarge); Text("Your workspace is ready.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    item {
      Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(22.dp)) {
          Text("FRAME X / INTELLIGENCE LAYER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text("One workspace. Every capable model.", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 12.dp))
          Text("Capability-aware routing with your connected providers.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
          Button(onClick = openChat, modifier = Modifier.padding(top = 18.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)) { Text("Open new chat") }
        }
      }
    }
    item { Text("WORKSPACE PULSE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp)) }
    item { StatRow("Provider network", "Not configured", "Connect a provider to begin") }
    item { StatRow("Conversations", "Local", "Your saved chats appear here") }
    item { StatRow("Drive sync", "Not connected", "Connect Google Drive when ready") }
  }
}

@Composable
private fun StatRow(title: String, value: String, detail: String) {
  Surface(color = Color(0xFF07111B), shape = RoundedCornerShape(14.dp)) {
    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.labelMedium); Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall) }
      Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
    }
  }
}

@Composable
private fun ChatScreen() {
  var taskMode by remember { mutableStateOf(false) }
  var input by remember { mutableStateOf("") }
  var notice by remember { mutableStateOf("") }
  LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    item {
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text("CHAT / NEW CONVERSATION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(if (taskMode) "Challenge mode" else "Untitled conversation", style = MaterialTheme.typography.headlineMedium) }
        TextButton(onClick = { taskMode = !taskMode }) { Text(if (taskMode) "Challenge" else "Task mode", color = if (taskMode) Color(0xFFE37B7B) else MaterialTheme.colorScheme.primary) }
      }
    }
    item { Surface(color = Color(0x1200D4FF), shape = RoundedCornerShape(10.dp)) { Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) { Text("✦", color = MaterialTheme.colorScheme.primary); Column(Modifier.padding(start = 10.dp)) { Text("Model not selected", fontWeight = FontWeight.SemiBold); Text("Automatic routing waits for a compatible configured provider", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } } }
    item { Text("YOU", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary); Bubble("Help me structure the next phase of FRAME X AI around the user experience.", true, taskMode) }
    item { Text("FRAME X / AI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Bubble("Expose decisions, hide implementation noise. Keep provider, model and sync state clear without turning the chat into infrastructure.", false, taskMode) }
    if (notice.isNotBlank()) item { Text(notice, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall) }
    item {
      Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.Bottom) {
          Text("＋", modifier = Modifier.padding(8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
          androidx.compose.material3.OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), placeholder = { Text("Ask FRAME X anything...") })
          Button(onClick = { notice = if (input.isBlank()) "Write a message first." else "No provider is connected to this chat surface yet. Your message was not sent." }, modifier = Modifier.padding(start = 6.dp)) { Text("↑") }
        }
      }
    }
  }
}

@Composable
private fun Bubble(text: String, user: Boolean, taskMode: Boolean) {
  Surface(color = if (user && taskMode) Color(0xFF281014) else if (user) Color(0xFF102A2C) else Color(0xFF0D1822), shape = RoundedCornerShape(15.dp)) {
    Text(text, modifier = Modifier.padding(16.dp), color = if (user) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface)
  }
}

@Composable
private fun ListScreen(title: String, subtitle: String) {
  Column(Modifier.fillMaxSize().padding(20.dp)) {
    Text("FRAME X AI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 8.dp))
    Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
    Spacer(Modifier.height(20.dp))
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) { Text("This surface is structured for the existing data layer. Real records are not fabricated.", modifier = Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
  }
}

@Composable
private fun ProvidersScreen() {
  val providers = listOf("OpenAI" to "Not configured", "Groq" to "Not configured", "Google Gemini" to "Not configured", "Anthropic" to "Not configured")
  LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    item { Text("MODEL NETWORK", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("Providers", style = MaterialTheme.typography.headlineLarge) }
    items(providers) { (name, state) -> Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(14.dp)) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Text(name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold); Text(state, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall) } } }
  }
}

@Composable
private fun DriveScreen() {
  Column(Modifier.fillMaxSize().padding(20.dp)) {
    Text("STORAGE / GOOGLE DRIVE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text("Your sync layer", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 8.dp))
    Surface(color = Color(0x0D80C6A0), shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(top = 20.dp)) {
      Column(Modifier.padding(20.dp)) { Text("Not connected", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold); Text("Drive sync is not configured yet.", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 8.dp)); Text("No synchronization has been performed.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)) }
    }
  }
}

@Composable
private fun SettingsScreen() {
  val rows = listOf("Appearance" to "Dark environment · reduced motion", "Routing" to "Automatic · capability-aware", "API keys" to "No provider credentials configured", "Google Drive" to "Not connected", "Privacy & security" to "Local-first · no secret logging", "Accessibility" to "Scalable type · screen reader")
  LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
    item { Text("SYSTEM / SETTINGS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("Settings", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 8.dp, bottom = 18.dp)) }
    items(rows) { (title, detail) -> Surface(color = MaterialTheme.colorScheme.surface) { Row(Modifier.fillMaxWidth().padding(17.dp), horizontalArrangement = Arrangement.SpaceBetween) { Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text("›", color = MaterialTheme.colorScheme.primary) } } }
  }
}
