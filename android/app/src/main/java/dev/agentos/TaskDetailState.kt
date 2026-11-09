package dev.agentos

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Owns one screen's requests; the composable lifecycle cancels work when it closes. */
internal class TaskDetailState(
    private val taskId: String,
    private val api: AgentApi,
    private val token: String,
    private val onTaskChanged: (AgentTask) -> Unit,
) {
    var detail by mutableStateOf<AgentTaskDetail?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var loading by mutableStateOf(true)
        private set
    var action by mutableStateOf<String?>(null)
        private set

    fun beginRefresh() { loading = true }

    suspend fun reload() {
        try {
            val updated = withContext(Dispatchers.IO) { api.task(token, taskId) }
            if (updated.task.eventSequence >= (detail?.task?.eventSequence ?: 0)) {
                detail = updated
                onTaskChanged(updated.task)
            }
            error = null
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            error = connectionMessage(failure)
        } finally {
            loading = false
        }
    }

    suspend fun performAction(value: String) {
        if (action != null) return
        action = value
        error = null
        try {
            val (status, sequence) = withContext(Dispatchers.IO) { api.taskAction(token, taskId, value) }
            val current = detail
            if (current != null && sequence >= current.task.eventSequence) {
                val updated = current.task.copy(status = status, eventSequence = sequence)
                detail = current.copy(task = updated)
                onTaskChanged(updated)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            error = connectionMessage(failure)
        } finally {
            action = null
        }
    }
    suspend fun performApprovalAction(value: String, message: String?) {
        val approval = detail?.approval ?: return
        if (action != null) return
        action = value
        error = null
        try {
            withContext(Dispatchers.IO) { api.approvalAction(token, approval, value, message) }
            reload()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            error = connectionMessage(failure)
        } finally {
            action = null
        }
    }

}

private fun connectionMessage(failure: Exception): String = when (failure) {
    is AgentApiException -> when (failure.code) {
        "UNAUTHENTICATED" -> "Your session expired. Go back and sign out in Settings, then sign in again."
        "NOT_FOUND" -> "This task is no longer available. Go back and refresh your tasks."
        "APPROVAL_STALE", "APPROVAL_BLOCKED" -> "This request changed or is no longer pending. Refresh and review its current state."
        "RECOVERY_REQUIRED", "ACTION_IN_PROGRESS" -> "This request was already dispatched. Check its result before taking another action."
        "RECEIPT_UNAVAILABLE" -> "No receipt could be confirmed yet. The action remains blocked. Try checking again later."
        "INVALID_TRANSITION" -> "The task changed before this action completed. Refresh to see its current state."
        else -> "The server could not complete this request. Refresh and try again."
    }
    else -> "Could not reach your server. Check your connection and try again."
}
