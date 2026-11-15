package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SubscriptionTier
import com.example.ui.components.JarvisAnimatedOrb
import com.example.ui.components.JarvisGlassCard
import com.example.ui.components.JarvisStatusBadge
import com.example.ui.components.QuotaProgressCard
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.HudSurfaceElevated
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AsyncUiState
import com.example.ui.viewmodel.JarvisViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class QuickModuleTarget {
    VOICE_HUD,
    AI_CHAT,
    DAILY_PLANNER,
    CONTENT_WRITER,
    TODO_MANAGER,
    WEATHER,
    REMINDERS
}

@Composable
fun DashboardScreen(
    viewModel: JarvisViewModel,
    isVoiceListening: Boolean,
    voiceRms: Float,
    onQuickPromptSubmit: (String) -> Unit,
    onOpenModule: (QuickModuleTarget) -> Unit,
    onOpenConversation: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val plannerEvents by viewModel.plannerEvents.collectAsStateWithLifecycle()
    val weatherState by viewModel.weatherState.collectAsStateWithLifecycle()

    var quickPrompt by remember { mutableStateOf("") }
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(15_000L)
        }
    }

    val greeting = remember(nowMillis) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good morning"
            hour < 18 -> "Good afternoon"
            else -> "Good evening"
        }
    }
    val dateText = remember(nowMillis) {
        SimpleDateFormat("EEE, MMM d • HH:mm", Locale.getDefault()).format(Date(nowMillis))
    }

    val firstName = profile?.displayName?.split(" ")?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "Commander"
    val pendingTasks = tasks.count { !it.completed }
    val activeReminders = reminders.count { !it.completed }
    val todayBlocks = plannerEvents.count { !it.completed }
    val todayKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val usedToday = if (profile?.dailyDateKey == todayKey) (profile?.dailyRequestsUsed?.toInt() ?: 0) else 0
    val tier = profile?.tier ?: SubscriptionTier.FREE

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header: Greeting + Date/Time + AI Status
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dateText.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$greeting, $firstName",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "JARVIS Neural Core is calibrated and ready.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                JarvisStatusBadge(
                    label = if (isVoiceListening) "LISTENING" else "AI ONLINE",
                    dotColor = if (isVoiceListening) NeonCyan else EmeraldPulse
                )
            }
        }

        // Central Interactive AI Orb
        item {
            JarvisGlassCard(
                highlightBorder = true,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    JarvisAnimatedOrb(
                        size = 168.dp,
                        isActive = isVoiceListening,
                        rmsLevel = voiceRms,
                        statusLabel = if (isVoiceListening) "LISTENING TO VOICE..." else "TAP ORB FOR VOICE HUD",
                        modifier = Modifier.testTag("dashboard_ai_orb"),
                        onClick = { onOpenModule(QuickModuleTarget.VOICE_HUD) }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = quickPrompt,
                        onValueChange = { quickPrompt = it },
                        placeholder = {
                            Text(
                                "Ask JARVIS anything...",
                                color = TextMuted
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = GlassBorder,
                            focusedContainerColor = ObsidianBlack.copy(alpha = 0.65f),
                            unfocusedContainerColor = ObsidianBlack.copy(alpha = 0.45f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (quickPrompt.isNotBlank()) {
                                        val prompt = quickPrompt.trim()
                                        quickPrompt = ""
                                        onQuickPromptSubmit(prompt)
                                    }
                                },
                                modifier = Modifier.testTag("dashboard_quick_send_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send prompt to JARVIS",
                                    tint = NeonCyan
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_quick_chat_input")
                    )
                }
            }
        }

        // Quick Action Grid
        item {
            Text(
                text = "QUICK COMMANDS",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionCard(
                    title = "Voice HUD",
                    subtitle = "Hands-free AI",
                    icon = Icons.Filled.Mic,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_voice"),
                    onClick = { onOpenModule(QuickModuleTarget.VOICE_HUD) }
                )
                QuickActionCard(
                    title = "AI Chat",
                    subtitle = "Streaming core",
                    icon = Icons.AutoMirrored.Filled.Chat,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_chat"),
                    onClick = { onOpenModule(QuickModuleTarget.AI_CHAT) }
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionCard(
                    title = "Daily Plan",
                    subtitle = "$todayBlocks blocks left",
                    icon = Icons.Filled.CalendarMonth,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_planner"),
                    onClick = { onOpenModule(QuickModuleTarget.DAILY_PLANNER) }
                )
                QuickActionCard(
                    title = "Content AI",
                    subtitle = "Draft & write",
                    icon = Icons.Filled.EditNote,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_writer"),
                    onClick = { onOpenModule(QuickModuleTarget.CONTENT_WRITER) }
                )
            }
        }

        // Productivity & Weather Telemetry Overview
        item {
            Text(
                text = "SYSTEM TELEMETRY",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                JarvisGlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_tasks_summary"),
                    onClick = { onOpenModule(QuickModuleTarget.TODO_MANAGER) }
                ) {
                    Icon(Icons.Filled.TaskAlt, contentDescription = "Tasks", tint = NeonCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("$pendingTasks", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                    Text("Pending Tasks", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                JarvisGlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_reminders_summary"),
                    onClick = { onOpenModule(QuickModuleTarget.REMINDERS) }
                ) {
                    Icon(Icons.Filled.NotificationsActive, contentDescription = "Reminders", tint = ElectricBlue, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("$activeReminders", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                    Text("Reminders", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                JarvisGlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_weather_summary"),
                    onClick = { onOpenModule(QuickModuleTarget.WEATHER) }
                ) {
                    Icon(Icons.Filled.Cloud, contentDescription = "Weather", tint = EmeraldPulse, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    val weatherLabel = when (val w = weatherState) {
                        is AsyncUiState.Success -> "${w.data.temperatureCelsius.toInt()}°C"
                        is AsyncUiState.Loading -> "..."
                        else -> "N/A"
                    }
                    Text(weatherLabel, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                    Text(
                        text = (weatherState as? AsyncUiState.Success)?.data?.conditionLabel ?: "Live Weather",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Quota Overview
        item {
            QuotaProgressCard(
                planTitle = tier.title,
                used = usedToday,
                limit = tier.dailyLimit
            )
        }

        // Recent Conversations
        item {
            Text(
                text = "RECENT SESSIONS",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (conversations.isEmpty()) {
                JarvisGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onOpenModule(QuickModuleTarget.AI_CHAT) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Start first session",
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("No neural sessions yet", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text("Tap to start your first AI conversation.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    conversations.take(3).forEach { conv ->
                        JarvisGlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recent_conversation_${conv.id}"),
                            onClick = { onOpenConversation(conv.id) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = conv.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${conv.messageCount} msgs",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonCyan
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = conv.lastMessagePreview,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    JarvisGlassCard(modifier = modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = DeepSpace,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, GlassBorder),
                modifier = Modifier.size(40.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
