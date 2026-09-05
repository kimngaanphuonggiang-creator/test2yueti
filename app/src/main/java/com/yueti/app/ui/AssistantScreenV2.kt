package com.yueti.app.ui

import androidx.activity.compose.BackHandler

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yueti.app.data.ChatMessageEntity
import com.yueti.app.data.ChatThreadEntity
import com.yueti.app.data.YuetiDatabase
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val EmotionMarker = Regex(
    pattern = """^\s*(?:\[\[emotion\s*[:：]\s*(\d{2})]]|\[emotion\s*[:：]\s*(\d{2})]|\[emotion]\s*[:：]\s*(\d{2}))\s*(?:\r?\n)?""",
    option = RegexOption.IGNORE_CASE,
)

internal data class EmotionEnvelope(val emotionId: String?, val visibleText: String)

/** DeepSeek occasionally varies the marker brackets.  Accept every observed form and never
 * expose this implementation metadata in the chat bubble. */
internal fun parseEmotionEnvelope(raw: String): EmotionEnvelope {
    val match = EmotionMarker.find(raw)
    if (match != null) {
        val id = match.groupValues.drop(1).firstOrNull { it.isNotBlank() }
            ?.takeIf(EmotionBallIds.all::contains)
        return EmotionEnvelope(id, raw.removeRange(match.range).trimStart())
    }
    // During SSE delivery the first chunks can be just "[" or "[emotion".  Hold that short
    // prefix back until it is complete so it cannot flash in the UI.
    val trimmed = raw.trimStart()
    val possibleMarkerPrefix = trimmed.startsWith("[") && trimmed.length <= 48 &&
        (trimmed.length <= 2 || "emotion".startsWith(trimmed.trimStart('[').takeWhile { it.isLetter() }, ignoreCase = true))
    return EmotionEnvelope(null, if (possibleMarkerPrefix) "" else raw)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AssistantScreenV2(onBack: () -> Unit) {
    val context = LocalContext.current
    val dao = remember { YuetiDatabase.get(context).dao() }
    val gateway = DeepSeekAiGateway
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val threads by dao.observeChatThreads().collectAsStateWithLifecycle(emptyList())
    var currentThreadId by remember { mutableStateOf<String?>(null) }
    val messagesFlow = remember(currentThreadId) { currentThreadId?.let(dao::observeChatMessages) ?: flowOf(emptyList()) }
    val messages by messagesFlow.collectAsStateWithLifecycle(emptyList())
    var mode by remember { mutableStateOf(AssistantMode.Chat) }
    var draft by remember { mutableStateOf("") }
    var streaming by remember { mutableStateOf(false) }
    var streamingContent by remember { mutableStateOf("") }
    var streamingReasoning by remember { mutableStateOf("") }
    var streamingEmotion by remember { mutableStateOf("30") }
    var requestJob by remember { mutableStateOf<Job?>(null) }
    var historyVisible by remember { mutableStateOf(false) }
    var keyDialogVisible by remember { mutableStateOf(false) }
    var apiKeyDraft by remember { mutableStateOf("") }
    var aiConfigured by remember { mutableStateOf(gateway.configured) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var exiting by remember { mutableStateOf(false) }

    fun exitAssistant() {
        if (exiting) return
        exiting = true
        requestJob?.cancel()
        scope.launch {
            // WebView owns a platform rendering layer. Remove it before changing the app
            // destination so no detached Emotion Ball frame can sit above the home fade.
            delay(96)
            onBack()
        }
    }

    BackHandler(onBack = ::exitAssistant)

    suspend fun createThread(): String {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        dao.saveChatThread(ChatThreadEntity(id, "新对话", mode.name, createdAt = now, updatedAt = now))
        currentThreadId = id
        return id
    }

    LaunchedEffect(Unit) {
        val saved = withContext(Dispatchers.IO) { AiCredentialStore.load(context) }
        if (saved.isNotBlank()) gateway.configure(saved)
        aiConfigured = gateway.configured
    }
    LaunchedEffect(threads) {
        if (currentThreadId == null || threads.none { it.id == currentThreadId }) {
            currentThreadId = threads.firstOrNull()?.id ?: createThread()
        }
    }
    LaunchedEffect(messages.size, streamingContent) {
        val count = messages.size + if (streamingContent.isNotEmpty()) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    fun sendPrompt(promptOverride: String? = null) {
        val prompt = (promptOverride ?: draft).trim()
        if (prompt.isBlank() || streaming) return
        if (!aiConfigured) { keyDialogVisible = true; return }
        draft = ""
        requestJob = scope.launch {
            val threadId = currentThreadId ?: createThread()
            val now = System.currentTimeMillis()
            val thread = threads.firstOrNull { it.id == threadId } ?: ChatThreadEntity(threadId, "新对话", mode.name, createdAt = now, updatedAt = now)
            val userMessage = ChatMessageEntity(UUID.randomUUID().toString(), threadId, "user", prompt, createdAt = now)
            dao.saveChatMessage(userMessage)
            dao.saveChatThread(thread.copy(title = if (thread.title == "新对话") prompt.take(22) else thread.title, mode = mode.name, updatedAt = now))
            streaming = true
            streamingContent = ""
            streamingReasoning = ""
            streamingEmotion = "30"
            var rawContent = ""
            var completed = false
            try {
                val existing = dao.getChatMessages(threadId)
                val currentThread = threads.firstOrNull { it.id == threadId }
                val payload = buildList {
                    currentThread?.summary?.takeIf { it.isNotBlank() }?.let { add(AiPromptMessage("system", "较早对话的长期记忆摘要：$it")) }
                    existing.takeLast(48).forEach { add(AiPromptMessage(it.role, it.content)) }
                }
                gateway.streamChat(payload, mode).collect { chunk ->
                    when (chunk) {
                        is ChatChunk.Reasoning -> streamingReasoning += chunk.text
                        is ChatChunk.Content -> {
                            rawContent += chunk.text
                            val envelope = parseEmotionEnvelope(rawContent)
                            envelope.emotionId?.let { streamingEmotion = it }
                            val visible = envelope.visibleText
                            if (visible.length > streamingContent.length) {
                                visible.substring(streamingContent.length).chunked(4).forEach { part -> streamingContent += part; delay(8) }
                            }
                        }
                        ChatChunk.Done -> completed = true
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                errorMessage = error.message ?: "AI 请求失败"
                streamingEmotion = "34"
            } finally {
                val finalContent = streamingContent.trim()
                if (finalContent.isNotEmpty()) {
                    dao.saveChatMessage(
                        ChatMessageEntity(
                            id = UUID.randomUUID().toString(), threadId = threadId, role = "assistant",
                            content = finalContent, reasoningContent = streamingReasoning.trim(), emotionId = streamingEmotion,
                            status = if (completed) "complete" else "partial", createdAt = System.currentTimeMillis(),
                        ),
                    )
                }
                streaming = false
                streamingContent = ""
                streamingReasoning = ""
                val all = dao.getChatMessages(threadId)
                if (completed && all.size > 48) {
                    val older = all.dropLast(24)
                    val summaryPrompt = "把下面较早的学习对话压缩成可供后续继续聊天的事实摘要，保留用户偏好、未完成问题、关键数学结论，不超过1200字：\n" + older.joinToString("\n") { "${it.role}: ${it.content}" }.take(80_000)
                    gateway.chat(summaryPrompt, AssistantMode.Chat).getOrNull()?.let { summary ->
                        dao.updateChatSummary(threadId, summary, older.lastOrNull()?.createdAt ?: 0L, System.currentTimeMillis())
                    }
                }
            }
        }
    }

    val lastAssistantMessage = messages.lastOrNull { it.role == "assistant" }
    val lastEmotion = if (streaming) {
        BotEmotion.fromId(streamingEmotion)
    } else {
        BotEmotion.fromId(
            lastAssistantMessage?.content?.let(::parseEmotionEnvelope)?.emotionId
                ?: lastAssistantMessage?.emotionId?.takeIf(EmotionBallIds.all::contains),
        )
    }

    Scaffold(
        containerColor = ToolScreenBackground,
        contentColor = Color.White,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).windowInsetsPadding(WindowInsets.navigationBars).imePadding()) {
            ToolScreenHeader(
                title = threads.firstOrNull { it.id == currentThreadId }?.title ?: "跃题助手",
                subtitle = if (mode == AssistantMode.DeepThinking) "深度思考" else "普通对话",
                onBack = ::exitAssistant,
            ) {
                IconButton(onClick = { historyVisible = true }) { Icon(Icons.Rounded.History, "会话历史", tint = Color.White) }
                IconButton(onClick = { scope.launch { createThread() } }) { Icon(Icons.Rounded.AddComment, "新建对话", tint = Color.White) }
                IconButton(onClick = { keyDialogVisible = true }) { Icon(Icons.Rounded.Key, "配置 API 密钥", tint = Color.White) }
            }
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(Modifier.fillMaxWidth().height(190.dp), contentAlignment = Alignment.Center) {
                    if (!exiting) {
                        EmotionBallView(lastEmotion, active = true, lite = false, onTap = {}, modifier = Modifier.size(188.dp))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = mode == AssistantMode.Chat,
                            onClick = { mode = AssistantMode.Chat },
                            label = { Text("对话") },
                            leadingIcon = { Icon(Icons.Rounded.Chat, null) },
                            colors = assistantChipColors(),
                        )
                        FilterChip(
                            selected = mode == AssistantMode.DeepThinking,
                            onClick = { mode = AssistantMode.DeepThinking },
                            label = { Text("深度思考") },
                            leadingIcon = { Icon(Icons.Rounded.Psychology, null) },
                            colors = assistantChipColors(),
                        )
                }
                Text(if (streaming) "跃跃正在组织回答…" else "消息会保存在这台设备上", color = Color.White.copy(alpha = .72f), style = MaterialTheme.typography.bodySmall)
            }
            LazyColumn(
                state = listState, modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (messages.isEmpty() && !streaming) item { Text("问我一道题，或者继续之前的学习话题。", color = Color.White.copy(alpha = .72f), modifier = Modifier.padding(vertical = 26.dp)) }
                items(messages, key = { it.id }) { message ->
                    ChatBubble(message.role == "user", parseEmotionEnvelope(message.content).visibleText, message.status == "partial")
                }
                if (streaming) item("streaming") {
                    AnimatedVisibility(true, enter = fadeIn() + slideInVertically { it / 4 }) {
                        if (streamingContent.isBlank()) CircularProgressIndicator(Modifier.size(42.dp)) else ChatBubble(false, streamingContent, false)
                    }
                }
            }
            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
            OutlinedTextField(
                value = draft, onValueChange = { draft = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                placeholder = { Text("输入问题…") }, maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFFE6D6FF), unfocusedContainerColor = Color(0xFFE6D6FF), focusedTextColor = Color(0xFF171318), unfocusedTextColor = Color(0xFF171318)),
                trailingIcon = {
                    if (streaming) IconButton(onClick = { requestJob?.cancel() }) { Icon(Icons.Rounded.StopCircle, "停止生成", tint = Color(0xFF4B176F)) }
                    else IconButton(onClick = { sendPrompt() }, enabled = draft.isNotBlank()) { Icon(Icons.AutoMirrored.Rounded.Send, "发送", tint = Color(0xFF4B176F)) }
                },
            )
        }
    }

    if (historyVisible) ModalBottomSheet(onDismissRequest = { historyVisible = false }) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("会话历史", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            threads.forEach { thread ->
                Surface(onClick = { currentThreadId = thread.id; mode = runCatching { AssistantMode.valueOf(thread.mode) }.getOrDefault(AssistantMode.Chat); historyVisible = false }, shape = RoundedCornerShape(18.dp), color = if (thread.id == currentThreadId) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(thread.title, fontWeight = FontWeight.Bold, maxLines = 1); Text(if (thread.summary.isBlank()) "本地完整记录" else "已建立长期记忆", style = MaterialTheme.typography.bodySmall) }; IconButton(onClick = { scope.launch { dao.deleteChatThread(thread.id) } }) { Icon(Icons.Rounded.Delete, "删除会话") } }
                }
            }
        }
    }
    if (keyDialogVisible) AlertDialog(
        onDismissRequest = { keyDialogVisible = false }, icon = { Icon(Icons.Rounded.Key, null) }, title = { Text("配置 DeepSeek API") },
        text = { OutlinedTextField(value = apiKeyDraft, onValueChange = { apiKeyDraft = it }, label = { Text("API 密钥") }, singleLine = true) },
        confirmButton = { Button(onClick = { val key = apiKeyDraft.trim(); if (key.isNotBlank()) { AiCredentialStore.save(context, key); gateway.configure(key); aiConfigured = true; keyDialogVisible = false } }) { Text("保存并启用") } },
        dismissButton = { TextButton(onClick = { keyDialogVisible = false }) { Text("取消") } },
    )
}

@Composable
private fun ChatBubble(isUser: Boolean, content: String, partial: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        Surface(
            shape = RoundedCornerShape(if (isUser) 22.dp else 18.dp),
            color = if (isUser) Color(0xFFE0C7FF) else Color(0xFF2B272D),
            contentColor = if (isUser) Color(0xFF25112F) else Color.White,
            modifier = Modifier.fillMaxWidth(.88f),
        ) { Column(Modifier.padding(horizontal = 15.dp, vertical = 12.dp)) { Text(content); if (partial) Text("已停止", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
    }
}

@Composable
private fun assistantChipColors() = androidx.compose.material3.FilterChipDefaults.filterChipColors(
    containerColor = Color.Transparent,
    labelColor = Color.White,
    iconColor = Color(0xFFD6B6FF),
    selectedContainerColor = Color(0xFF53137A),
    selectedLabelColor = Color.White,
    selectedLeadingIconColor = Color(0xFFEBD8FF),
)
