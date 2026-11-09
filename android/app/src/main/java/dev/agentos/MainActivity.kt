package dev.agentos

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal val Ink = Color(0xFF101612)
internal val Panel = Color(0xFF1A231D)
internal val PanelRaised = Color(0xFF222D26)
internal val Mint = Color(0xFFB6F29A)
internal val Muted = Color(0xFFA1ADA4)
internal val Line = Color(0xFF344139)

private data class OpenedReport(val title: String, val content: String)
private data class OpenedImage(val title: String, val bytes: ByteArray)

class MainActivity : ComponentActivity() {
    private var isForeground by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sessionStore = SessionStore(this)
        val preferences = getSharedPreferences("agentos_settings", MODE_PRIVATE)
        setContent {
            val scheme = darkColorScheme(
                primary = Mint,
                onPrimary = Ink,
                secondary = Color(0xFFA7C6AA),
                background = Ink,
                onBackground = Color(0xFFF2F4EF),
                surface = Panel,
                onSurface = Color(0xFFF2F4EF),
                surfaceVariant = PanelRaised,
                onSurfaceVariant = Muted,
                outline = Line,
            )
            MaterialTheme(colorScheme = scheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = Ink, contentColor = scheme.onBackground) {
                    AgentOSApp(
                        isForeground = isForeground,
                        initialToken = sessionStore.read(),
                        initialEndpoint = preferences.getString("endpoint", "http://10.0.2.2:8787")!!,
                        saveToken = sessionStore::save,
                        clearToken = sessionStore::clear,
                        saveEndpoint = { preferences.edit().putString("endpoint", it).apply() },
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        isForeground = true
    }

    override fun onStop() {
        isForeground = false
        super.onStop()
    }
}

private enum class Destination(val title: String, val glyph: String) {
    CHAT("Chat", "◌"), TASKS("Tasks", "◷"), COMPUTER("Computer", "▣"), FILES("Files", "▤"), SETTINGS("Settings", "⚙"),
}

@Composable
private fun AgentOSApp(
    isForeground: Boolean,
    initialToken: String?,
    initialEndpoint: String,
    saveToken: (String) -> Unit,
    clearToken: () -> Unit,
    saveEndpoint: (String) -> Unit,
) {
    var token by remember { mutableStateOf(initialToken) }
    var endpoint by remember { mutableStateOf(initialEndpoint) }
    var destination by remember { mutableStateOf(Destination.CHAT) }
    val tasks = remember { mutableStateListOf<AgentTask>() }
    val files = remember { mutableStateListOf<AgentFile>() }
    var openedReport by remember { mutableStateOf<OpenedReport?>(null) }
    var openedImage by remember { mutableStateOf<OpenedImage?>(null) }
    var selectedTaskId by remember { mutableStateOf<String?>(null) }
    var openingReport by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val api = remember(endpoint) { if (AgentApi.isValidEndpoint(endpoint)) AgentApi(endpoint) else null }

    suspend fun refresh() {
        val current = token ?: return
        val client = api ?: return
        runCatching { withContext(Dispatchers.IO) { client.tasks(current) } }
            .onSuccess { updated -> tasks.clear(); tasks.addAll(updated) }
            .onFailure {
                if (it is kotlinx.coroutines.CancellationException) throw it
                error = "Could not load tasks. Check your connection and refresh."
            }
    }

    suspend fun refreshFiles() {
        val current = token ?: return
        val client = api ?: return
        runCatching { withContext(Dispatchers.IO) { client.files(current) } }
            .onSuccess { updated -> files.clear(); files.addAll(updated) }
            .onFailure {
                if (it is kotlinx.coroutines.CancellationException) throw it
                error = "Could not load files. Check your connection and refresh."
            }
    }

    LaunchedEffect(token, endpoint, destination, isForeground, selectedTaskId) {
        if (token == null || !isForeground || selectedTaskId != null || destination == Destination.COMPUTER || destination == Destination.SETTINGS) {
            return@LaunchedEffect
        }
        while (true) {
            refresh()
            if (destination == Destination.FILES) refreshFiles()
            delay(5_000)
        }
    }

    if (token == null) {
        LoginScreen(
            endpoint = endpoint,
            busy = busy,
            error = error,
            onEndpointChange = { value -> endpoint = value; saveEndpoint(value) },
            onLogin = { email, password ->
                busy = true
                error = null
                scope.launch {
                    val client = api
                    if (client == null) error = "Use HTTPS, or the Android emulator address in a debug build."
                    else runCatching { withContext(Dispatchers.IO) { client.login(email, password) } }
                        .onSuccess { saveToken(it); token = it }
                        .onFailure { error = it.message ?: "Sign in failed" }
                    busy = false
                }
            },
        )
        return
    }

    fun openReport(fileId: String, title: String) {
        val client = api ?: return
        val currentToken = token ?: return
        val reportEndpoint = endpoint
        if (openingReport) return
        openingReport = true
        error = null
        scope.launch {
            try {
                val content = withContext(Dispatchers.IO) { client.downloadFile(currentToken, fileId) }
                if (token == currentToken && endpoint == reportEndpoint) openedReport = OpenedReport(title, content)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = "Report could not be opened. Check your connection and try again."
            } finally {
                openingReport = false
            }
        }
    }

    fun openWorkspaceFile(file: AgentFile) {
        if (file.contentType != "image/png") {
            openReport(file.id, file.filename)
            return
        }
        val client = api ?: return
        val currentToken = token ?: return
        val imageEndpoint = endpoint
        if (openingReport) return
        openingReport = true
        error = null
        scope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) { client.downloadImage(currentToken, file.id) }
                if (token == currentToken && endpoint == imageEndpoint) openedImage = OpenedImage(file.filename, bytes)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                error = "Image could not be opened. Check your connection and try again."
            } finally {
                openingReport = false
            }
        }
    }

    val selected = selectedTaskId
    val currentClient = api
    if (selected != null && currentClient != null) {
        TaskDetailScreen(
            taskId = selected,
            api = currentClient,
            token = token!!,
            isForeground = isForeground,
            openingReport = openingReport,
            onBack = { selectedTaskId = null; error = null },
            onTaskChanged = { updated ->
                val index = tasks.indexOfFirst { it.id == updated.id }
                if (index >= 0) tasks[index] = updated
            },
            onOpenReport = ::openReport,
        )
        error?.let { message ->
            AlertDialog(
                onDismissRequest = { error = null },
                title = { Text("Could not open report") },
                text = { Text(message) },
                confirmButton = { TextButton(onClick = { error = null }) { Text("Done") } },
                containerColor = Panel,
            )
        }
        openedReport?.let { ReportDialog(it) { openedReport = null } }
        openedImage?.let { ImagePreviewDialog(it) { openedImage = null } }
        return
    }

    WorkspaceShell(
        destination = destination,
        onDestinationChange = { destination = it; error = null },
        endpoint = endpoint,
        tasks = tasks,
        files = files,
        openedReport = openedReport,
        onCloseReport = { openedReport = null },
        openedImage = openedImage,
        onCloseImage = { openedImage = null },
        busy = busy,
        error = error,
        onRefresh = { scope.launch { refresh() } },
        onOpenTask = { selectedTaskId = it.id; error = null },
        onCreateApprovalDemo = { message, simulate ->
            busy = true
            error = null
            scope.launch {
                val client = api
                val currentToken = token
                if (client == null || currentToken == null) error = "Sign in to your server before preparing a request."
                else runCatching { withContext(Dispatchers.IO) { client.createApprovalDemo(currentToken, message, simulate) } }
                    .onSuccess { tasks.add(0, it); selectedTaskId = it.id }
                    .onFailure { error = it.message ?: "The approval demo could not be prepared." }
                busy = false
            }
        },
        onGenerateImage = { prompt ->
            busy = true
            error = null
            scope.launch {
                val client = api
                val currentToken = token
                if (client == null || currentToken == null) error = "Sign in to your server before generating an image."
                else runCatching { withContext(Dispatchers.IO) { client.generateImage(currentToken, prompt) } }
                    .onSuccess { file ->
                        files.add(0, file)
                        openWorkspaceFile(file)
                    }
                    .onFailure { error = it.message ?: "Gemini could not generate the image." }
                busy = false
            }
        },
        onSend = { instruction ->
            busy = true
            error = null
            scope.launch {
                val client = api
                if (client == null) error = "Set a valid HTTPS server address in Settings."
                else runCatching { withContext(Dispatchers.IO) { client.createTask(token!!, instruction) } }
                    .onSuccess { tasks.add(0, it); selectedTaskId = it.id }
                    .onFailure { error = it.message ?: "Task could not be saved" }
                busy = false
            }
        },
        onTaskAction = { task, action ->
            actOnTask(api, token!!, task, action, tasks, scope) { error = it }
        },
        onRefreshFiles = { scope.launch { refreshFiles() } },
        onOpenFile = ::openWorkspaceFile,
        onSaveEndpoint = { value ->
            if (AgentApi.isValidEndpoint(value)) {
                // OWASP A01 Broken Access Control: discard private UI content when changing server context.
                openedReport = null
                openedImage = null
                selectedTaskId = null
                tasks.clear()
                files.clear()
                val updatedEndpoint = value.trim().trimEnd('/')
                // OWASP A01 Broken Access Control: never forward the previous server's session to a new server.
                if (updatedEndpoint != endpoint) {
                    clearToken()
                    token = null
                    destination = Destination.CHAT
                }
                endpoint = updatedEndpoint
                saveEndpoint(endpoint)
                error = null
            } else {
                error = "Use an HTTPS URL. HTTP is allowed only for the Android emulator in debug builds."
            }
        },
        onLogout = {
            val oldToken = token!!
            busy = true
            scope.launch {
                api?.let { client -> runCatching { withContext(Dispatchers.IO) { client.logout(oldToken) } } }
                clearToken()
                token = null
                openedReport = null
                openedImage = null
                selectedTaskId = null
                tasks.clear()
                files.clear()
                busy = false
            }
        },
    )
}

@Composable
private fun WorkspaceShell(
    destination: Destination,
    onDestinationChange: (Destination) -> Unit,
    endpoint: String,
    tasks: List<AgentTask>,
    files: List<AgentFile>,
    openedReport: OpenedReport?,
    onCloseReport: () -> Unit,
    busy: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onSend: (String) -> Unit,
    onTaskAction: (AgentTask, String) -> Unit,
    onOpenTask: (AgentTask) -> Unit,
    onCreateApprovalDemo: (String, Boolean) -> Unit,
    onRefreshFiles: () -> Unit,
    onOpenFile: (AgentFile) -> Unit,
    onSaveEndpoint: (String) -> Unit,
    onLogout: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        containerColor = Ink,
        bottomBar = {
            NavigationBar(containerColor = Panel, windowInsets = WindowInsets.navigationBars) {
                Destination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = { onDestinationChange(item) },
                        icon = { Text(item.glyph, fontSize = 20.sp) },
                        label = { Text(item.title, fontSize = 10.sp) },
                        colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                            selectedIconColor = Mint,
                            selectedTextColor = Mint,
                            indicatorColor = PanelRaised,
                            unselectedIconColor = Muted,
                            unselectedTextColor = Muted,
                        ),
                    )
                }
            }
        },
    ) { insets ->
        DestinationContent(
            destination, endpoint, tasks, files, busy, error, insets,
            onRefresh, onSend, onTaskAction, onOpenTask, onCreateApprovalDemo, onRefreshFiles, onOpenFile, onSaveEndpoint, onLogout,
        )
    }
        openedReport?.let { ReportDialog(it, onCloseReport) }
    }
}

@Composable
private fun DestinationContent(
    destination: Destination,
    endpoint: String,
    tasks: List<AgentTask>,
    files: List<AgentFile>,
    busy: Boolean,
    error: String?,
    padding: PaddingValues,
    onRefresh: () -> Unit,
    onSend: (String) -> Unit,
    onTaskAction: (AgentTask, String) -> Unit,
    onOpenTask: (AgentTask) -> Unit,
    onCreateApprovalDemo: (String, Boolean) -> Unit,
    onRefreshFiles: () -> Unit,
    onOpenFile: (AgentFile) -> Unit,
    onSaveEndpoint: (String) -> Unit,
    onLogout: () -> Unit,
) {
    when (destination) {
        Destination.CHAT -> ChatScreen(tasks, busy, error, padding, onRefresh, onTaskAction, onOpenTask, onSend)
        Destination.TASKS -> TasksScreen(tasks, busy, padding, error, onRefresh, onTaskAction, onOpenTask, onCreateApprovalDemo)
        Destination.COMPUTER -> UnavailableScreen(
            "Your cloud computer",
            "Live viewing and manual control will appear here when computer sessions are connected.",
            "COMPUTER SETUP IN PROGRESS",
            padding,
        )
        Destination.FILES -> FilesScreen(files, error, padding, onRefreshFiles, onOpenFile)
        Destination.SETTINGS -> SettingsScreen(endpoint, error, onSaveEndpoint, onLogout, padding)
    }
}

private fun actOnTask(
    api: AgentApi?,
    token: String,
    task: AgentTask,
    action: String,
    tasks: MutableList<AgentTask>,
    scope: kotlinx.coroutines.CoroutineScope,
    onError: (String?) -> Unit,
) {
    val client = api ?: run { onError("Set a valid server URL before changing tasks."); return }
    scope.launch {
        runCatching { withContext(Dispatchers.IO) { client.taskAction(token, task.id, action) } }
            .onSuccess { (status, sequence) ->
                val index = tasks.indexOfFirst { it.id == task.id }
                if (index >= 0) tasks[index] = task.copy(status = status, eventSequence = sequence)
                onError(null)
            }
            .onFailure { onError(it.message ?: "Task could not be updated") }
    }
}

@Composable
private fun LoginScreen(
    endpoint: String,
    busy: Boolean,
    error: String?,
    onEndpointChange: (String) -> Unit,
    onLogin: (String, String) -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().background(Ink).imePadding().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        BrandMark()
        Spacer(Modifier.height(26.dp))
        Text("Your own agent.\nIts own computer.", fontSize = 34.sp, lineHeight = 39.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Text("Sign in to connect to your private workspace.", color = Muted, fontSize = 15.sp)
        Spacer(Modifier.height(30.dp))
        OutlinedTextField(endpoint, onEndpointChange, Modifier.fillMaxWidth(), label = { Text("API server URL") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
        error?.let { Spacer(Modifier.height(12.dp)); Text(it, color = Color(0xFFFF9B91)) }
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            "Sign in",
            busy,
            Modifier.fillMaxWidth(),
            isEnabled = AgentApi.isValidEndpoint(endpoint),
        ) { onLogin(email.trim(), password) }
        Spacer(Modifier.height(16.dp))
        if (!AgentApi.isValidEndpoint(endpoint)) Text("Use HTTPS, or the Android emulator address in a debug build.", color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun ChatScreen(
    tasks: List<AgentTask>,
    busy: Boolean,
    error: String?,
    padding: PaddingValues,
    onRefresh: () -> Unit,
    onAction: (AgentTask, String) -> Unit,
    onOpenTask: (AgentTask) -> Unit,
    onSend: (String) -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(padding)) {
        Header(onRefresh)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text("YOUR PRIVATE WORKSPACE", fontSize = 10.sp, color = Mint, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(10.dp))
                Text("What can I\ntake off your plate?", fontSize = 33.sp, lineHeight = 39.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(10.dp))
                Text("Describe the outcome. Your request will be saved as a task.", fontSize = 14.sp, color = Muted, lineHeight = 20.sp)
                Spacer(Modifier.height(17.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(36.dp).clip(CircleShape).background(Mint), Alignment.Center) {
                            Text("A", color = Ink, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("AgentOS", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Spacer(Modifier.height(5.dp))
                            Text(
                                "When configured, your server sends task text to OpenAI for read-only web research. Charges may apply. Do not include passwords or private data.",
                                color = Muted,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                            )
                        }
                    }
                }
            }
            if (tasks.isNotEmpty()) {
                item { SectionHeading("RECENT TASKS", "${tasks.size}") }
                items(tasks.take(10), key = { it.id }) { TaskRow(it, onAction, onOpenTask) }
            }
            if (error != null) item { Text(error, color = Color(0xFFFF9B91), fontSize = 13.sp) }
        }
        Row(
            Modifier.fillMaxWidth().background(Panel).padding(horizontal = 16.dp, vertical = 12.dp).imePadding(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it.take(8000) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Describe a task…", color = Muted) },
                maxLines = 4,
                shape = RoundedCornerShape(20.dp),
            )
            PrimaryButton("↑", busy, Modifier.size(54.dp)) {
                val instruction = draft.trim()
                if (instruction.isNotEmpty()) { onSend(instruction); draft = "" }
            }
        }
    }
}

@Composable
private fun TasksScreen(
    tasks: List<AgentTask>,
    busy: Boolean,
    padding: PaddingValues,
    error: String?,
    onRefresh: () -> Unit,
    onAction: (AgentTask, String) -> Unit,
    onOpenTask: (AgentTask) -> Unit,
    onCreateApprovalDemo: (String, Boolean) -> Unit,
) {
    var showApprovalDemo by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(padding)) {
        Header(onRefresh)
        Column(Modifier.padding(horizontal = 20.dp, vertical = 22.dp)) {
            Text("Your tasks", fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
            Text("Requests saved to your cloud workspace.", color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp, bottom = 18.dp))
            TextButton(onClick = { showApprovalDemo = true }, enabled = !busy) {
                Text(if (busy) "Preparing request…" else "Try approval demo", color = Mint)
            }
            error?.let { Text(it, color = Color(0xFFFF9B91), modifier = Modifier.padding(bottom = 12.dp)) }
            if (tasks.isEmpty()) EmptyState("No tasks yet", "Start a request from Chat and it will appear here.")
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(tasks, key = { it.id }) { TaskRow(it, onAction, onOpenTask) } }
        }
    }
    if (showApprovalDemo) ApprovalMessageDialog(
        title = "Local approval demo", allowSimulation = true,
        onDismiss = { showApprovalDemo = false },
        onConfirm = { message, simulate -> showApprovalDemo = false; onCreateApprovalDemo(message, simulate) },
    )

}

@Composable
private fun FilesScreen(
    files: List<AgentFile>,
    error: String?,
    padding: PaddingValues,
    onRefresh: () -> Unit,
    onOpenFile: (AgentFile) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(padding)) {
        Header(onRefresh)
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 22.dp)) {
            Text("Workspace files", fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
            Text("Open a report, follow its sources, or save a copy to your device.", color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp, bottom = 18.dp))
            error?.let { Text(it, color = Color(0xFFFF9B91), modifier = Modifier.padding(bottom = 12.dp)) }
            if (files.isEmpty()) {
                EmptyState("No reports yet", "Completed research reports will appear here.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(files, key = { it.id }) { file ->
                        Card(
                            onClick = { onOpenFile(file) },
                            colors = CardDefaults.cardColors(containerColor = Panel),
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                Text(file.filename, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(5.dp))
                                Text("${file.sizeBytes / 1024} KB  ·  Task ${file.taskId.take(8)}", color = Muted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportDialog(report: OpenedReport, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val linkedContent = remember(report.content) { linkifyReport(report.content) }
    // OWASP A01: write only to the document URI selected by the user instead of requesting broad storage access.
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
        if (uri != null) scope.launch {
            val saved = runCatching {
                withContext(Dispatchers.IO) {
                    val output = context.contentResolver.openOutputStream(uri, "wt")
                        ?: error("Selected document could not be opened")
                    output.use { it.write(report.content.toByteArray(Charsets.UTF_8)) }
                }
            }.isSuccess
            Toast.makeText(
                context,
                if (saved) "Report saved" else "Report could not be saved",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(report.title) },
        text = {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
                item {
                    ClickableText(
                        text = linkedContent,
                        onClick = { offset ->
                            linkedContent.getStringAnnotations("url", offset, offset).firstOrNull()?.item?.let { url ->
                                if (url.startsWith("https://") || url.startsWith("http://")) uriHandler.openUri(url)
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done", color = Mint) } },
        dismissButton = {
            TextButton(onClick = { saveLauncher.launch(report.title.ifBlank { "research-report.md" }) }) {
                Text("Save copy", color = Mint)
            }
        },
        containerColor = Panel,
    )
}

private fun linkifyReport(content: String): AnnotatedString {
    val pattern = Regex("""https?://[^\s<>"']+""")
    return buildAnnotatedString {
        var cursor = 0
        for (match in pattern.findAll(content)) {
            append(content.substring(cursor, match.range.first))
            val rawUrl = match.value
            val url = rawUrl.trimEnd('.', ',', ';')
            val trailing = rawUrl.substring(url.length)
            pushStringAnnotation("url", url)
            withStyle(SpanStyle(color = Mint, textDecoration = TextDecoration.Underline)) { append(url) }
            pop()
            append(trailing)
            cursor = match.range.last + 1
        }
        append(content.substring(cursor))
    }
}

@Composable
private fun SettingsScreen(endpoint: String, error: String?, onSave: (String) -> Unit, onLogout: () -> Unit, padding: PaddingValues) {
    var value by remember(endpoint) { mutableStateOf(endpoint) }
    Column(Modifier.fillMaxSize().padding(padding)) {
        Header {}
        Column(Modifier.padding(20.dp)) {
            Text("Settings", fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(22.dp))
            SectionHeading("CONNECTION", null)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value, { value = it }, Modifier.fillMaxWidth(), label = { Text("API server URL") }, singleLine = true)
            error?.let { Spacer(Modifier.height(8.dp)); Text(it, color = Color(0xFFFF9B91), fontSize = 12.sp) }
            Spacer(Modifier.height(10.dp))
            PrimaryButton("Save server", false, Modifier.fillMaxWidth()) { onSave(value) }
            Spacer(Modifier.height(18.dp))
            Text("Changing servers requires signing in again. Sign out revokes this session on the server.", color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onLogout) { Text("Sign out", color = Color(0xFFFFB1A8)) }
        }
    }
}

@Composable
private fun UnavailableScreen(title: String, detail: String, badge: String, padding: PaddingValues) {
    Column(Modifier.fillMaxSize().padding(padding)) {
        Header {}
        Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.Center) {
            Text(badge, color = Mint, letterSpacing = 1.3.sp, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(title, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            Text(detail, color = Muted, lineHeight = 22.sp, fontSize = 15.sp)
        }
    }
}

@Composable
private fun Header(onRefresh: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        BrandMark()
        if (onRefresh != null) TextButton(onClick = onRefresh) { Text("↻", fontSize = 22.sp, color = Muted) }
    }
}

@Composable
private fun BrandMark() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(Mint), Alignment.Center) {
            Text("A", color = Ink, fontWeight = FontWeight.Black, fontSize = 15.sp)
        }
        Text("AGENTOS", fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
    }
}

@Composable
private fun SectionHeading(title: String, trailing: String?) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        trailing?.let { Text(it, color = Muted, fontSize = 12.sp) }
    }
}

@Composable
private fun TaskRow(task: AgentTask, onAction: (AgentTask, String) -> Unit, onOpenTask: (AgentTask) -> Unit) {
    Card(onClick = { onOpenTask(task) }, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.padding(top = 4.dp).size(8.dp).clip(CircleShape).background(Mint))
            Column(Modifier.weight(1f)) {
                Text(task.instruction, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Text(statusLabel(task.status), color = Muted, fontSize = 11.sp)
                TextButton(onClick = { onOpenTask(task) }) { Text("View activity", color = Mint) }
                if (task.status == "QUEUED" || task.status == "PAUSED") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (task.status == "QUEUED") TextButton(onClick = { onAction(task, "pause") }) { Text("Pause", color = Mint) }
                        if (task.status == "PAUSED") TextButton(onClick = { onAction(task, "resume") }) { Text("Resume", color = Mint) }
                        TextButton(onClick = { onAction(task, "cancel") }) { Text("Cancel", color = Color(0xFFFFB1A8)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, detail: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(detail, color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
        }
    }
}

@Composable
private fun PrimaryButton(
    text: String,
    busy: Boolean,
    modifier: Modifier,
    isEnabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = !busy && isEnabled,
        modifier = modifier.height(52.dp),
        contentPadding = PaddingValues(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Ink),
        shape = RoundedCornerShape(16.dp),
    ) {
        if (busy) CircularProgressIndicator(Modifier.size(19.dp), color = Ink, strokeWidth = 2.dp)
        else Text(text, fontWeight = FontWeight.Bold, fontSize = if (text == "↑") 23.sp else 14.sp)
    }
}
