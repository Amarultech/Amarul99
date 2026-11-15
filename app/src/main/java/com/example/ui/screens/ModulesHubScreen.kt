package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.JarvisGlassCard
import com.example.ui.theme.CyberIndigo
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class JarvisModuleId(
    val title: String,
    val subtitle: String,
    val category: String,
    val icon: ImageVector,
    val accent: Color
) {
    AI_CHAT("AI Chat", "Multi-thread Gemini chat", "AI Core", Icons.AutoMirrored.Filled.Chat, NeonCyan),
    VOICE_ASSISTANT("Voice Assistant", "Hands-free Speech & TTS", "AI Core", Icons.Filled.Mic, ElectricBlue),
    DAILY_PLANNER("Daily Planner", "AI & manual time blocks", "Productivity", Icons.Filled.CalendarMonth, EmeraldPulse),
    TODO_MANAGER("To-Do Manager", "Priority task tracker", "Productivity", Icons.Filled.TaskAlt, NeonCyan),
    NOTES_MANAGER("Notes Manager", "Cloud notes & pinning", "Productivity", Icons.Filled.Description, ElectricBlue),
    AI_CONTENT_WRITER("Content Writer", "Blogs, emails & social", "Creator AI", Icons.Filled.EditNote, CyberIndigo),
    YOUTUBE_CREATOR("YouTube Studio", "Titles, hooks & SEO", "Creator AI", Icons.Filled.PlayCircle, Color(0xFFF43F5E)),
    IMAGE_PROMPT_GEN("Image Prompts", "Pro prompt engineer", "Creator AI", Icons.Filled.Image, NeonCyan),
    VIDEO_SCRIPT_GEN("Video Scripts", "Scene-by-scene scripts", "Creator AI", Icons.Filled.MovieCreation, ElectricBlue),
    TRANSLATOR("Translator", "Multi-language AI + TTS", "AI Core", Icons.Filled.Translate, EmeraldPulse),
    CALCULATOR("Calculator", "Scientific + AI solver", "Utilities", Icons.Filled.Calculate, Color(0xFFF59E0B)),
    WEATHER("Weather Live", "Real-time global forecast", "Utilities", Icons.Filled.Cloud, NeonCyan),
    REMINDERS("Reminders", "Smart alerts & schedules", "Productivity", Icons.Filled.NotificationsActive, ElectricBlue),
    KNOWLEDGE_CENTER("Knowledge Hub", "Personal wiki & AI Q&A", "AI Core", Icons.Filled.MenuBook, CyberIndigo),
    SETTINGS("Settings & SaaS", "Plans, voice & admin", "System", Icons.Filled.Settings, TextSecondary)
}

@Composable
fun ModulesHubScreen(
    onModuleSelected: (JarvisModuleId) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            JarvisModuleId.entries
        } else {
            val q = searchQuery.trim().lowercase()
            JarvisModuleId.entries.filter {
                it.title.lowercase().contains(q) ||
                    it.subtitle.lowercase().contains(q) ||
                    it.category.lowercase().contains(q)
            }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 156.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("COMMAND MODULES", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "15 integrated AI & productivity systems",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search modules...", color = TextMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = DeepSpace,
                        unfocusedContainerColor = DeepSpace,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("modules_search_input")
                )
            }
        }

        items(filtered, key = { it.name }) { module ->
            JarvisGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("module_card_${module.name.lowercase()}"),
                onClick = { onModuleSelected(module) }
            ) {
                Surface(
                    color = module.accent.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, module.accent.copy(alpha = 0.35f)),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = module.icon,
                            contentDescription = module.title,
                            tint = module.accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = module.category.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = module.accent
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = module.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = module.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
