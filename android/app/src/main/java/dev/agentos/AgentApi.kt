package dev.agentos

import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URI
import java.net.URLEncoder
import java.util.UUID

data class AgentTask(
    val id: String,
    val instruction: String,
    val status: String,
    val createdAt: Long,
    val eventSequence: Long,
)

data class AgentFile(
    val id: String,
    val taskId: String?,
    val filename: String,
    val contentType: String,
    val sizeBytes: Long,
    val createdAt: Long,
    val kind: String,
)

data class AgentTaskDetail(
    val task: AgentTask,
    val events: List<AgentTaskEvent>,
    val approval: AgentApproval? = null,
)

data class AgentApproval(
    val id: String,
    val status: String,
    val payloadHash: String,
    val version: Int,
    val expiresAt: Long,
    val destination: String,
    val message: String,
    val simulateLostResponse: Boolean,
    val executionState: String,
)

data class AgentTaskEvent(
    val sequence: Long,
    val type: String,
    val timestamp: Long,
    val stage: String,
    val decision: String,
    val issues: List<String>,
    val message: String,
    val errorCode: String,
    val artifactId: String,
    val filename: String,
    val elapsedMs: Long?,
)

class AgentApiException(val code: String, message: String) : Exception(message)

class AgentApi(baseUrl: String) {
    private val root = baseUrl.trim().trimEnd('/')

    init {
        require(isValidEndpoint(root)) { "Use HTTPS for the server URL" }
    }

    companion object {
        fun isValidEndpoint(value: String): Boolean = runCatching {
            val uri = URI(value.trim().trimEnd('/'))
            val secure = uri.scheme.equals("https", ignoreCase = true)
            val localDebug = BuildConfig.DEBUG && uri.scheme.equals("http", ignoreCase = true) &&
                uri.host in setOf("localhost", "127.0.0.1", "10.0.2.2")
            // OWASP A04: require TLS in release to protect bearer tokens in transit.
            uri.host != null && uri.userInfo == null && uri.query == null && uri.fragment == null &&
                (secure || localDebug)
        }.getOrDefault(false)
    }

    fun login(email: String, password: String): String {
        val result = request("POST", "/auth/login", null, JSONObject()
            .put("email", email)
            .put("password", password))
        return result.getString("access_token")
    }

    fun logout(token: String) {
        request("POST", "/auth/logout", token, JSONObject())
    }

    fun tasks(token: String): List<AgentTask> {
        val items = request("GET", "/tasks?limit=50", token).getJSONArray("items")
        return (0 until items.length()).map { index ->
            val item = items.getJSONObject(index)
            readTask(item)
        }
    }

    fun task(token: String, taskId: String): AgentTaskDetail {
        // OWASP A07 Injection: encode the identifier so it cannot change the requested API path.
        val result = request("GET", "/tasks/${pathSegment(taskId)}", token)
        val events = result.getJSONArray("events")
        return AgentTaskDetail(
            task = readTask(result),
            events = (0 until events.length()).map { readEvent(events.getJSONObject(it)) },
            approval = readApproval(result.optJSONObject("approval")),
        )
    }

    fun createApprovalDemo(token: String, message: String, simulateLostResponse: Boolean): AgentTask = readTask(
        request("POST", "/approval-demos", token,
            JSONObject().put("message", message).put("simulate_lost_response", simulateLostResponse),
            mapOf("Idempotency-Key" to UUID.randomUUID().toString())),
    )

    fun approvalAction(token: String, approval: AgentApproval, action: String, message: String? = null) {
        require(action in setOf("approve", "reject", "revise", "reconcile"))
        val body = JSONObject().put("payload_hash", approval.payloadHash)
        if (message != null) body.put("payload", JSONObject()
            .put("message", message).put("simulate_lost_response", approval.simulateLostResponse))
        request("POST", "/approvals/${pathSegment(approval.id)}/$action", token, body)
    }

    private fun readApproval(item: JSONObject?): AgentApproval? {
        if (item == null) return null
        val intent = item.getJSONObject("intent")
        val payload = intent.getJSONObject("payload")
        return AgentApproval(
            id = item.getString("id"), status = item.getString("status"),
            payloadHash = item.getString("payload_hash"), version = item.getInt("version"),
            expiresAt = item.getLong("expires_at"), destination = intent.getString("destination"),
            message = payload.getString("message"), simulateLostResponse = payload.optBoolean("simulate_lost_response"),
            executionState = item.optJSONObject("execution")?.optString("state").orEmpty(),
        )
    }

    private fun readTask(item: JSONObject) = AgentTask(
        id = item.getString("id"),
        instruction = item.getString("instruction"),
        status = item.getString("status"),
        createdAt = item.getLong("created_at"),
        eventSequence = item.getLong("event_sequence"),
    )

    private fun readEvent(item: JSONObject): AgentTaskEvent {
        val payload = item.optJSONObject("payload") ?: JSONObject()
        val issues = payload.optJSONArray("issues")
        return AgentTaskEvent(
            sequence = item.getLong("sequence"),
            type = item.getString("event_type"),
            timestamp = item.getLong("timestamp"),
            stage = payload.optString("stage"),
            decision = payload.optString("decision"),
            issues = if (issues == null) emptyList() else (0 until issues.length()).map { issues.optString(it) },
            message = payload.optString("message"),
            errorCode = payload.optString("error_code"),
            artifactId = payload.optString("artifact_id"),
            filename = payload.optString("filename", "research-report.md"),
            elapsedMs = if (payload.has("elapsed_ms")) payload.optLong("elapsed_ms") else null,
        )
    }

    fun createTask(token: String, instruction: String): AgentTask {
        val result = request(
            "POST",
            "/tasks",
            token,
            JSONObject().put("instruction", instruction),
            mapOf("Idempotency-Key" to UUID.randomUUID().toString()),
        )
        return readTask(result)
    }

    fun taskAction(token: String, taskId: String, action: String): Pair<String, Long> {
        val result = request("POST", "/tasks/$taskId/$action", token, JSONObject())
        return result.getString("status") to result.getLong("event_sequence")
    }

    fun files(token: String): List<AgentFile> {
        val items = request("GET", "/files", token).getJSONArray("items")
        return (0 until items.length()).map { index ->
            readFile(items.getJSONObject(index))
        }
    }

    fun generateImage(token: String, prompt: String): AgentFile = readFile(
        request("POST", "/images", token, JSONObject().put("prompt", prompt)).getJSONObject("image"),
    )

    fun downloadImage(token: String, fileId: String): ByteArray {
        val connection = (URL("$root/files/${pathSegment(fileId)}").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            // OWASP A02: prevent redirects from forwarding the bearer token to another server.
            instanceFollowRedirects = false
            connectTimeout = 5_000
            readTimeout = 20_000
            setRequestProperty("Accept", "image/png")
            setRequestProperty("Authorization", "Bearer $token")
        }
        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            if (status !in 200..299) {
                val text = readBoundedText(stream, 1_000_000)
                val failure = runCatching { JSONObject(text).optJSONObject("error") }.getOrNull()
                throw AgentApiException(
                    failure?.optString("code") ?: "REQUEST_FAILED",
                    failure?.optString("message") ?: "Image could not be downloaded ($status)",
                )
            }
            if (connection.contentType?.substringBefore(';') != "image/png") {
                throw AgentApiException("UNSUPPORTED_IMAGE", "The server returned an unsupported image format.")
            }
            return readBoundedBytes(stream, 8_000_000)
        } finally {
            connection.disconnect()
        }
    }

    private fun readFile(item: JSONObject) = AgentFile(
        id = item.getString("id"),
        taskId = if (item.isNull("task_id")) null else item.optString("task_id").takeIf(String::isNotBlank),
        filename = item.getString("filename"),
        contentType = item.optString("content_type"),
        sizeBytes = item.getLong("size_bytes"),
        createdAt = item.getLong("created_at"),
        kind = item.optString("kind", "REPORT"),
    )

    fun downloadFile(token: String, fileId: String): String {
        val connection = (URL("$root/files/${pathSegment(fileId)}").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            // OWASP A02: prevent an artifact endpoint redirect from forwarding the bearer token elsewhere.
            instanceFollowRedirects = false
            connectTimeout = 5_000
            readTimeout = 8_000
            setRequestProperty("Accept", "text/markdown")
            setRequestProperty("Authorization", "Bearer $token")
        }
        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = readBoundedText(stream, 300_000)
            if (status !in 200..299) {
                val message = runCatching { JSONObject(text).optJSONObject("error")?.optString("message") }.getOrNull()
                error(message ?: "File could not be downloaded ($status)")
            }
            return text
        } finally {
            connection.disconnect()
        }
    }

    private fun readBoundedText(stream: InputStream?, limit: Int): String {
        if (stream == null) return ""
        val buffer = ByteArray(limit + 1)
        var size = 0
        stream.use { input ->
            while (size < buffer.size) {
                val count = input.read(buffer, size, buffer.size - size)
                if (count < 0) break
                size += count
            }
        }
        // OWASP A04 Insecure Design: cap response bytes so a large history cannot exhaust device memory.
        if (size > limit) throw AgentApiException("RESPONSE_TOO_LARGE", "The server response exceeded its size limit.")
        return String(buffer, 0, size, Charsets.UTF_8)
    }

    private fun readBoundedBytes(stream: InputStream?, limit: Int): ByteArray {
        if (stream == null) return byteArrayOf()
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8_192)
        stream.use { input ->
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (output.size() + count > limit) {
                    throw AgentApiException("RESPONSE_TOO_LARGE", "The image exceeded its size limit.")
                }
                output.write(buffer, 0, count)
            }
        }
        return output.toByteArray()
    }

    private fun pathSegment(value: String) = URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")

    private fun request(
        method: String,
        path: String,
        token: String?,
        body: JSONObject? = null,
        extraHeaders: Map<String, String> = emptyMap(),
    ): JSONObject {
        val connection = (URL(root + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            // OWASP A02: reject redirects so bearer credentials stay on the configured API origin.
            instanceFollowRedirects = false
            connectTimeout = 5_000
            readTimeout = 8_000
            setRequestProperty("Accept", "application/json")
            if (token != null) setRequestProperty("Authorization", "Bearer $token")
            extraHeaders.forEach { (name, value) -> setRequestProperty(name, value) }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        try {
            if (body != null) connection.outputStream.use { it.write(body.toString().toByteArray()) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = readBoundedText(stream, 1_000_000)
            val payload = if (text.isBlank()) JSONObject() else JSONObject(text)
            if (status !in 200..299) {
                val failure = payload.optJSONObject("error")
                throw AgentApiException(
                    failure?.optString("code") ?: "REQUEST_FAILED",
                    failure?.optString("message") ?: "Request failed ($status)",
                )
            }
            return payload
        } finally {
            connection.disconnect()
        }
    }
}
