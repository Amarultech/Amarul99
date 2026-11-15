package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ChatMessage
import com.example.ui.components.JarvisGlassCard
import com.example.ui.components.JarvisMarkdownText
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.HudSurface
import com.example.ui.theme.HudSurfaceElevated
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.JarvisViewModel

@Composable
fun ChatScreen(
    viewModel: JarvisViewModel,
    onSpeakText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val activeConvId by viewModel.activeConversationId.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val streamingText by viewModel.chatStreamingText.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isChatGenerating.collectAsStateWithLifecycle()
    val chatError by viewModel.chatError.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    var copiedBanner by remember { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboardManager.current
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, streamingText) {
        val target = (messages.size + if (streamingText != null) 1 else 0) - 1
        if (target >= 0) {
            listState.animateScrollToItem(target)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("JARVIS NEURAL CHAT", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Text(
                    text = "Gemini 3.8 Flash • Streaming Cloud Sync",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan
                )
            }
            Row {
                IconButton(
                    onClick = { viewModel.createNewConversation() },
                    modifier = Modifier.testTag("chat_new_session_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "New Conversation", tint = NeonCyan)
                }
                IconButton(
                    onClick = { viewModel.clearActiveConversation() },
                    enabled = activeConvId != null && messages.isNotEmpty(),
                    modifier = Modifier.testTag("chat_clear_button")
                ) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear Conversation", tint = TextSecondary)
                }
            }
        }

        // Conversation Thread Chips
        if (conversations.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                items(conversations, key = { it.id }) { conv ->
                    val selected = conv.id == activeConvId
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.selectConversation(conv.id) },
                        label = {
                            Text(
                                text = conv.title.take(20),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                            selectedLabelColor = NeonCyan,
                            containerColor = HudSurface,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = GlassBorder,
                            selectedBorderColor = NeonCyan
                        )
                    )
                }
            }
        }

        if (copiedBanner != null) {
            Surface(
                color = NeonCyan.copy(alpha = 0.14f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Text(
                    text = copiedBanner ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        if (chatError != null) {
            Surface(
                color = CrimsonError.copy(alpha = 0.16f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CrimsonError.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = chatError ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFCA5A5),
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { viewModel.clearChatError() }) {
                        Text("Dismiss", color = NeonCyan)
                    }
                }
            }
        }

        // Messages Area
        Box(modifier = Modifier.weight(1f)) {
            if (messages.isEmpty() && streamingText == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = "Ready",
                        tint = NeonCyan,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Neural Channel Ready", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Ask JARVIS to brainstorm ideas, write code, summarize concepts, or plan your day.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    val starters = listOf(
                        "Summarize key AI productivity habits",
                        "Draft a clean Kotlin coroutine pattern",
                        "Give me a 3-point morning briefing"
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        starters.forEach { starter ->
                            Surface(
                                color = HudSurfaceElevated,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, GlassBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.sendChatMessage(starter) { reply ->
                                            if (profile?.voiceEnabled == true) onSpeakText(reply)
                                        }
                                    }
                            ) {
                                Text(
                                    text = starter,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NeonCyan,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(messages, key = { it.id }) { msg ->
                        val isLastModel = msg.role == "model" && msg.id == messages.lastOrNull()?.id
                        ChatBubble(
                            message = msg,
                            showRegenerate = isLastModel && !isGenerating,
                            onCopy = {
                                clipboard.setText(AnnotatedString(msg.text))
                                copiedBanner = "Copied response to clipboard"
                            },
                            onSpeak = { onSpeakText(msg.text) },
                            onRegenerate = {
                                viewModel.regenerateLastResponse { reply ->
                                    if (profile?.voiceEnabled == true) onSpeakText(reply)
                                }
                            }
                        )
                    }

                    if (streamingText != null) {
                        item(key = "streaming_bubble") {
                            StreamingAssistantBubble(text = streamingText ?: "")
                        }
                    }
                }
            }
        }

        // Input Bar
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Message JARVIS...", color = TextMuted) },
                maxLines = 4,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = GlassBorder,
                    focusedContainerColor = DeepSpace,
                    unfocusedContainerColor = DeepSpace,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_message_input")
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                onClick = {
                    val prompt = inputText.trim()
                    if (prompt.isNotEmpty() && !isGenerating) {
                        inputText = ""
                        copiedBanner = null
                        viewModel.sendChatMessage(prompt) { reply ->
                            if (profile?.voiceEnabled == true) onSpeakText(reply)
                        }
                    }
                },
                enabled = !isGenerating && inputText.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                color = if (inputText.isNotBlank() && !isGenerating) NeonCyan else HudSurfaceElevated,
                modifier = Modifier
                    .size(54.dp)
                    .testTag("chat_send_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = NeonCyan,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send message",
                            tint = if (inputText.isNotBlank()) ObsidianBlack else TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    showRegenerate: Boolean,
    onCopy: () -> Unit,
    onSpeak: () -> Unit,
    onRegenerate: () -> Unit
) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (isUser) {
            Surface(
                color = ElectricBlue.copy(alpha = 0.25f),
                shape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp),
                border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.55f)),
                modifier = Modifier.widthIn(max = 320.dp)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    modifier = Modifier.padding(14.dp)
                )
            }
        } else {
            JarvisGlassCard(
                highlightBorder = true,
                modifier = Modifier.fillMaxWidth(0.95f)
            ) {
                Text(
                    text = "JARVIS",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan
                )
                Spacer(modifier = Modifier.height(6.dp))
                JarvisMarkdownText(markdown = message.text)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onCopy, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy response", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onSpeak, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Speak response", tint = NeonCyan, modifier = Modifier.size(18.dp))
                    }
                    if (showRegenerate) {
                        IconButton(onClick = onRegenerate, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Regenerate response", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamingAssistantBubble(text: String) {
    JarvisGlassCard(
        highlightBorder = true,
        modifier = Modifier.fillMaxWidth(0.95f)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                color = NeonCyan,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "JARVIS • TRANSMITTING",
                style = MaterialTheme.typography.labelSmall,
                color = NeonCyan
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        if (text.isBlank()) {
            Text("Synthesizing neural response...", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        } else {
            JarvisMarkdownText(markdown = text)
        }
    }
}
