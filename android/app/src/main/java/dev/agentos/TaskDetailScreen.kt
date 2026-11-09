package dev.agentos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

private val ErrorTone = Color(0xFFFFB1A8)
private val terminalStatuses = setOf("COMPLETED", "FAILED", "CANCELLED")

@Composable
internal fun TaskDetailScreen(
    taskId: String,
    api: AgentApi,
    token: String,
    isForeground: Boolean,
    openingReport: Boolean,
    onBack: () -> Unit,
    onTaskChanged: (AgentTask) -> Unit,
    onOpenReport: (String, String) -> Unit,
) {
    val state = remember(taskId, api, token) { TaskDetailState(taskId, api, token, onTaskChanged) }
    var refresh by remember(taskId) { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    BackHandler(onBack = onBack)

    // OWASP A04 Insecure Design: poll only the foreground task and stop on terminal states to bound requests.
    LaunchedEffect(state, isForeground, refresh) {
        if (!isForeground) return@LaunchedEffect
        do {
            state.reload()
            if (state.error == null && state.detail?.task?.status in terminalStatuses) break
            delay(5_000)
        } while (true)
    }

    TaskDetailContent(
        state = state,
        openingReport = openingReport,
        onBack = onBack,
        onRefresh = { state.beginRefresh(); refresh += 1 },
        onAction = { value -> scope.launch { state.performAction(value); refresh += 1 } },
        onOpenReport = onOpenReport,
        onApprovalAction = { value, message -> scope.launch { state.performApprovalAction(value, message) } },
    )
}

@Composable
private fun TaskDetailContent(
    state: TaskDetailState,
    openingReport: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onAction: (String) -> Unit,
    onOpenReport: (String, String) -> Unit,
    onApprovalAction: (String, String?) -> Unit,
) {
    Scaffold(containerColor = Ink, contentWindowInsets = WindowInsets.safeDrawing) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = onBack) { Text("Back", color = Mint) }
                Text("Task details", fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onRefresh, enabled = !state.loading) {
                    Text(if (state.loading) "Refreshing" else "Refresh", color = Muted)
                }
            }
            val current = state.detail
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (state.error != null) item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(state.error.orEmpty(), color = ErrorTone, fontSize = 14.sp)
                        if (current != null) Text("Showing the last available update.", color = Muted, fontSize = 12.sp)
                        TextButton(onClick = onRefresh, enabled = !state.loading) { Text("Try again", color = Mint) }
                    }
                }
                if (current == null) item {
                    Text(if (state.loading) "Loading task history…" else "Task history is unavailable.", color = Muted)
                }
                if (current != null) {
                    item {
                        Text(current.task.instruction, fontSize = 23.sp, lineHeight = 31.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(16.dp))
                        StatusSummary(current)
                    }
                    item {
                        val approval = current.approval
                        if (approval != null) ApprovalPanel(approval, current.task.status, state.action, onApprovalAction)
                        else TaskOutcome(current, openingReport, onOpenReport)
                    }
                    item {
                        TaskControls(current.task.status, state.action, onAction)
                        HorizontalDivider(color = Line)
                        Spacer(Modifier.height(20.dp))
                        Text("Activity", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text("Updates while this screen is open.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                    items(current.events, key = { it.sequence }) { event -> TimelineEvent(event) }
                }
            }
        }
    }
}

@Composable
private fun StatusSummary(detail: AgentTaskDetail) {
    val status = detail.task.status
    val label = when (status) {
        "RUNNING" -> stageLabel(detail.events.lastOrNull { it.type == "TASK_STAGE_STARTED" }?.stage.orEmpty())
        else -> statusLabel(status)
    }
    Row(
        Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (status == "FAILED") ErrorTone else Mint))
        Text(label, color = if (status == "FAILED") ErrorTone else Mint, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
private fun TaskOutcome(detail: AgentTaskDetail, openingReport: Boolean, onOpenReport: (String, String) -> Unit) {
    val latestReview = detail.events.lastOrNull { it.type == "TASK_REVIEWED" }
    val completed = detail.events.lastOrNull { it.type == "TASK_COMPLETED" }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (detail.task.status) {
            "FAILED" -> {
                Text("This task could not finish", color = ErrorTone, fontWeight = FontWeight.SemiBold)
                val code = detail.events.lastOrNull { it.type == "TASK_FAILED" }?.errorCode.orEmpty()
                Text(failureGuidance(code), color = Muted, fontSize = 14.sp, lineHeight = 21.sp)
            }
            "PAUSED" -> Text("Work is paused. Resume starts fresh research; an in-flight request may still finish and incur charges.", color = Muted, fontSize = 14.sp, lineHeight = 21.sp)
            "CANCELLED" -> Text("This task was cancelled. No further stages will run. You can create a new request from Chat.", color = Muted, fontSize = 14.sp, lineHeight = 21.sp)
            "QUEUED" -> Text("Waiting for the research worker. If it stays queued, check that your server has a provider key configured.", color = Muted, fontSize = 14.sp, lineHeight = 21.sp)
        }
        latestReview?.let { review ->
            Text(if (review.decision == "pass") "Review passed" else "Review found issues", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            if (review.issues.isNotEmpty()) Text(review.issues.joinToString(" · ") { issueLabel(it) }, color = Muted, fontSize = 13.sp)
        }
        if (detail.task.status == "COMPLETED") ReportAction(completed, openingReport, onOpenReport)
    }
}

@Composable
private fun ReportAction(completed: AgentTaskEvent?, openingReport: Boolean, onOpenReport: (String, String) -> Unit) {
    if (completed == null || completed.artifactId.isBlank()) {
        Text("Your report is available from Files. Refresh if it has not appeared yet.", color = Muted, fontSize = 14.sp)
        return
    }
    Button(
        onClick = { onOpenReport(completed.artifactId, completed.filename) },
        enabled = !openingReport,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Ink),
    ) { Text(if (openingReport) "Opening report…" else "Open report", fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun TaskControls(status: String, action: String?, onAction: (String) -> Unit) {
    val canPause = status in setOf("QUEUED", "RUNNING")
    val canResume = status == "PAUSED"
    val canCancel = status in setOf("QUEUED", "PLANNING", "RUNNING", "PAUSED", "WAITING_FOR_USER", "WAITING_FOR_APPROVAL", "RECOVERY_REQUIRED")
    if (!canCancel) return
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (canPause) TextButton(onClick = { onAction("pause") }, enabled = action == null) { Text("Pause", color = Mint) }
            if (canResume) TextButton(onClick = { onAction("resume") }, enabled = action == null) { Text("Resume", color = Mint) }
            TextButton(onClick = { onAction("cancel") }, enabled = action == null) { Text("Cancel task", color = ErrorTone) }
        }
        if (action != null) Text("Updating task…", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun TimelineEvent(event: AgentTaskEvent) {
    val time = remember(event.timestamp) { DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(event.timestamp * 1000)) }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 6.dp).size(6.dp).clip(CircleShape).background(Muted))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(eventTitle(event), fontSize = 14.sp, lineHeight = 20.sp)
            if (event.type == "TASK_REVIEWED" && event.issues.isNotEmpty()) {
                Text(event.issues.joinToString(" · ") { issueLabel(it) }, color = Muted, fontSize = 12.sp)
            }
            val duration = event.elapsedMs?.let { " · ${it / 1000}s" }.orEmpty()
            Text(time + duration, color = Muted, fontSize = 12.sp)
        }
    }
}

private fun eventTitle(event: AgentTaskEvent): String = when (event.type) {
    "TASK_STAGE_STARTED" -> stageLabel(event.stage)
    "TASK_STAGE_COMPLETED" -> stageCompletionLabel(event.stage)
    "TASK_REVIEWED" -> if (event.decision == "pass") "Review passed" else "Changes requested"
    else -> eventLabels[event.type] ?: "Task updated"
}

private val eventLabels = mapOf(
    "TASK_CREATED" to "Request saved", "TASK_PLANNING" to "Preparing the task",
    "TASK_RUNNING" to "Research started", "TASK_COMPLETED" to "Report ready",
    "TASK_FAILED" to "Task stopped with an error", "TASK_PAUSED" to "Paused by you",
    "TASK_QUEUED" to "Resumed · fresh research queued", "TASK_CANCELLED" to "Cancelled by you",
    "APPROVAL_REQUESTED" to "Approval requested", "APPROVAL_GRANTED" to "You approved this request",
    "APPROVAL_REJECTED" to "You rejected this request", "APPROVAL_EXPIRED" to "Approval expired",
    "ACTION_DISPATCHED" to "Submitting the approved message", "ACTION_SUCCEEDED" to "Message delivered to the local inbox",
    "ACTION_UNCONFIRMED" to "Delivery needs a receipt check", "ACTION_RECONCILED" to "Delivery confirmed from its receipt",
    "ACTION_ABORTED" to "Stopped before sending",
)
private val statusLabels = mapOf(
    "CREATED" to "Queued", "QUEUED" to "Queued", "PLANNING" to "Preparing", "RUNNING" to "In progress",
    "COMPLETED" to "Completed", "FAILED" to "Could not finish", "PAUSED" to "Paused", "CANCELLED" to "Cancelled",
    "WAITING_FOR_USER" to "Needs your input", "WAITING_FOR_APPROVAL" to "Needs your approval",
    "RECOVERY_REQUIRED" to "Delivery unconfirmed",
)
internal fun statusLabel(status: String) = statusLabels[status] ?: "Updating"

private val stageLabels = mapOf("research" to "Researching", "review" to "Reviewing sources", "revision" to "Revising the report", "final_review" to "Final review")
private val completedStageLabels = mapOf("research" to "Research draft ready", "review" to "Source review finished", "revision" to "Report revised", "final_review" to "Final review finished")
private val issueLabels = mapOf("incomplete" to "Missing information", "unsupported_claim" to "Claims need evidence", "outdated_source" to "Sources need updating", "contradiction" to "Conflicting information", "missing_uncertainty" to "Uncertainty needs explaining", "insufficient_evidence" to "Not enough evidence")
private fun stageLabel(stage: String) = stageLabels[stage] ?: "Working on your task"
private fun stageCompletionLabel(stage: String) = completedStageLabels[stage] ?: "Task step finished"
private fun issueLabel(issue: String) = issueLabels[issue] ?: "An issue needs attention"

private val failureMessages = mapOf(
    "QUALITY_GATE_FAILED" to "The report still had unresolved issues after one revision. No report was published. Try a narrower question or specify the sources you need.",
    "RESEARCH_EVIDENCE_MISSING" to "The research did not include usable source citations. Try a more specific question or name the official sources to use.",
    "REVIEW_INVALID" to "The provider could not produce a usable review. No report was published. Create a new task to try again.",
    "PROVIDER_REFUSED" to "The provider could not fulfill this request. Rephrase the research question and create a new task.",
    "PROVIDER_AUTH_FAILED" to "Your server's provider key was rejected. Update the key on the server, then create a new task.",
    "PROVIDER_RATE_LIMITED" to "The provider is limiting requests. Wait a little, then create a new task.",
    "PROVIDER_REQUEST_FAILED" to "The provider rejected the request. Check your server's model configuration, then create a new task.",
    "PROVIDER_UNAVAILABLE" to "The research provider could not be reached. Check your server's connection and try a new task later.",
    "RESEARCH_DEADLINE_EXCEEDED" to "The task reached its time limit. Try a smaller research question.",
    "RESEARCH_BUDGET_EXCEEDED" to "The task reached its usage limit. Try a smaller research question.",
    "RESEARCH_INPUT_TOO_LARGE" to "The research became too large to review. Narrow the question and create a new task.",
    "WORKER_INTERRUPTED" to "The server stopped before the report was saved. Create a new task to restart the research.",
)
private fun failureGuidance(code: String) = failureMessages[code] ?: "The research could not finish safely. No report was published. Check your server and create a new task to try again."
