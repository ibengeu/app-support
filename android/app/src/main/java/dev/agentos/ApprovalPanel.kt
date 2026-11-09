package dev.agentos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DateFormat
import java.util.Date

@Composable
internal fun ApprovalPanel(
    approval: AgentApproval,
    taskStatus: String,
    action: String?,
    onAction: (String, String?) -> Unit,
) {
    var editing by remember(approval.id) { mutableStateOf(false) }
    val destination = if (approval.destination == "local-demo-inbox") "Local demo inbox" else approval.destination
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Review before sending", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text("Destination: $destination", color = Mint, fontSize = 14.sp)
        // OWASP A07 Injection: approval text is displayed as plain text, with no HTML or executable content.
        SelectionContainer { Text(approval.message, fontSize = 16.sp, lineHeight = 23.sp) }
        if (approval.simulateLostResponse) {
            Text("Demo mode: delivery deliberately loses its response, then requires a receipt check.", color = Muted, fontSize = 13.sp)
        }
        Text(approvalOutcome(approval), color = Muted, fontSize = 14.sp, lineHeight = 21.sp)
        if (approval.status == "PENDING" && taskStatus == "WAITING_FOR_APPROVAL") {
            val expiry = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(approval.expiresAt * 1000))
            Text("Approval expires $expiry", color = Muted, fontSize = 12.sp)
            Button(
                onClick = { onAction("approve", null) }, enabled = action == null,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Ink),
            ) { Text(if (action == "approve") "Submitting…" else "Approve & submit") }
            TextButton(onClick = { onAction("reject", null) }, enabled = action == null) { Text("Reject request", color = Mint) }
        }
        if (approval.executionState == "UNKNOWN") {
            Button(onClick = { onAction("reconcile", null) }, enabled = action == null, modifier = Modifier.fillMaxWidth()) {
                Text(if (action == "reconcile") "Checking receipt…" else "Check receipt")
            }
        }
        if (approval.status in setOf("PENDING", "EXPIRED", "REJECTED") && taskStatus != "CANCELLED") {
            TextButton(onClick = { editing = true }, enabled = action == null) { Text("Edit message", color = Mint) }
        }
    }
    if (editing) ApprovalMessageDialog(
        title = "Edit request", initialMessage = approval.message,
        onDismiss = { editing = false },
        onConfirm = { message, _ -> editing = false; onAction("revise", message) },
    )
}

private fun approvalOutcome(approval: AgentApproval): String {
    val execution = mapOf(
        "SUCCEEDED" to "Delivery confirmed in the local demo inbox.",
        "UNKNOWN" to "Delivery is unconfirmed. Check the receipt; this action will not be submitted again.",
        "DISPATCHING" to "Submitting the exact message you approved.",
        "FAILED_SAFE" to "Stopped before sending. Cancel this task, then prepare a new request.",
    )
    val status = mapOf(
        "PENDING" to "This message is waiting for your approval. Editing it creates a new request to review.",
        "REJECTED" to "You rejected this request. Edit the message to prepare a new version, or cancel the task.",
        "EXPIRED" to "Approval expired. Edit the message to prepare a fresh request.",
        "REVOKED" to "This request was cancelled before submission.",
        "SUPERSEDED" to "This request was replaced by a newer version.",
    )
    return execution[approval.executionState] ?: status[approval.status] ?: "Refresh to see the current request state."
}

@Composable
internal fun ApprovalMessageDialog(
    title: String,
    initialMessage: String = "Hello from AgentOS",
    allowSimulation: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean) -> Unit,
) {
    var message by remember { mutableStateOf(initialMessage) }
    var simulate by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Prepare a message for the local demo inbox. You will review and approve it before it is sent.", color = Muted, fontSize = 14.sp)
                OutlinedTextField(message, { message = it.take(2000) }, Modifier.fillMaxWidth(), label = { Text("Message") }, minLines = 2, maxLines = 5)
                if (allowSimulation) Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = simulate, onCheckedChange = { simulate = it })
                    Text("Simulate an interrupted response", fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(message.trim(), simulate) }, enabled = message.isNotBlank()) {
                Text("Prepare request", color = Mint)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Mint) } },
        containerColor = Panel,
    )
}
