package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.JarvisGlassCard
import com.example.ui.components.JarvisMarkdownText
import com.example.ui.components.ModuleHeaderBar
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.HudSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AsyncUiState
import com.example.ui.viewmodel.JarvisViewModel

@Composable
private fun jarvisFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NeonCyan,
    unfocusedBorderColor = GlassBorder,
    focusedContainerColor = DeepSpace,
    unfocusedContainerColor = DeepSpace,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary
)

@Composable
fun DailyPlannerModuleScreen(
    viewModel: JarvisViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val events by viewModel.plannerEvents.collectAsStateWithLifecycle()
    val aiLoading by viewModel.plannerAiLoading.collectAsStateWithLifecycle()
    val plannerError by viewModel.plannerError.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf("") }
    var timeSlot by remember { mutableStateOf("09:00 AM - 10:00 AM") }
    var category by remember { mutableStateOf("Deep Work") }
    var aiGoals by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ModuleHeaderBar(
                title = "Daily Planner",
                subtitle = "Time-blocked schedule & Gemini AI auto-planner",
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                onBack = onBack
            )
        }

        // AI Auto-Planner Card
        item {
            JarvisGlassCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                Text("AI AUTO-SCHEDULE MY DAY", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = aiGoals,
                    onValueChange = { aiGoals = it },
                    placeholder = { Text("e.g. Finish Android release, gym at 5pm, team sync", color = TextMuted) },
                    colors = jarvisFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (plannerError != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(plannerError ?: "", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFFCA5A5))
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (aiGoals.isNotBlank()) {
                            viewModel.generateAiDailyPlan(aiGoals)
                            aiGoals = ""
                        }
                    },
                    enabled = !aiLoading && aiGoals.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = ObsidianBlack),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("planner_ai_generate_button")
                ) {
                    if (aiLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ObsidianBlack, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Building Schedule...")
                    } else {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate AI Time Blocks")
                    }
                }
            }
        }

        // Manual Add Block
        item {
            JarvisGlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("ADD TIME BLOCK", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Block title", color = TextMuted) },
                    singleLine = true,
                    colors = jarvisFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = timeSlot,
                        onValueChange = { timeSlot = it },
                        label = { Text("Time Slot") },
                        singleLine = true,
                        colors = jarvisFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        singleLine = true,
                        colors = jarvisFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.addPlannerEvent(title, timeSlot, 60L, category)
                            title = ""
                        }
                    },
                    border = BorderStroke(1.dp, NeonCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Block", color = NeonCyan)
                }
            }
        }

        if (events.isEmpty()) {
            item {
                JarvisGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("No schedule blocks yet.", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text("Use AI Auto-Schedule above or add your own time blocks.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
        } else {
            items(events, key = { it.id }) { event ->
                JarvisGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = event.completed,
                            onCheckedChange = { viewModel.togglePlannerEvent(event) },
                            colors = CheckboxDefaults.colors(checkedColor = NeonCyan, checkmarkColor = ObsidianBlack)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${event.timeSlot} • ${event.category.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan
                            )
                            Text(
                                text = event.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    textDecoration = if (event.completed) TextDecoration.LineThrough else TextDecoration.None
                                ),
                                color = if (event.completed) TextMuted else TextPrimary
                            )
                        }
                        IconButton(onClick = { viewModel.deletePlannerEvent(event.id) }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete block", tint = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TodoManagerModuleScreen(
    viewModel: JarvisViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("medium") }
    var filter by remember { mutableStateOf("all") }

    val filteredTasks = remember(tasks, filter) {
        when (filter) {
            "active" -> tasks.filter { !it.completed }
            "done" -> tasks.filter { it.completed }
            else -> tasks
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ModuleHeaderBar(
                title = "To-Do Manager",
                subtitle = "Cloud-synced priority task control",
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                onBack = onBack
            )
        }

        item {
            JarvisGlassCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("New task title...", color = TextMuted) },
                    singleLine = true,
                    colors = jarvisFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("todo_title_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Optional details...", color = TextMuted) },
                    colors = jarvisFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("low", "medium", "high").forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p.replaceFirstChar { it.uppercase() }) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                                selectedLabelColor = NeonCyan,
                                containerColor = HudSurface,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.addTask(title, description, priority, "Today")
                            title = ""
                            description = ""
                        }
                    },
                    enabled = title.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = ObsidianBlack),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("todo_add_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Task")
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("all" to "All (${tasks.size})", "active" to "Active", "done" to "Done").forEach { (key, label) ->
                    FilterChip(
                        selected = filter == key,
                        onClick = { filter = key },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricBlue.copy(alpha = 0.25f),
                            selectedLabelColor = Color.White,
                            containerColor = HudSurface,
                            labelColor = TextSecondary
                        )
                    )
                }
            }
        }

        if (filteredTasks.isEmpty()) {
            item {
                JarvisGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("No tasks in this view.", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text("Add a new task above to start tracking.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
        } else {
            items(filteredTasks, key = { it.id }) { task ->
                val priorityColor = when (task.priority) {
                    "high" -> CrimsonError
                    "medium" -> AmberAlert
                    else -> EmeraldPulse
                }
                JarvisGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = task.completed,
                            onCheckedChange = { viewModel.toggleTask(task) },
                            colors = CheckboxDefaults.colors(checkedColor = NeonCyan, checkmarkColor = ObsidianBlack)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.priority.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = priorityColor
                            )
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                                ),
                                color = if (task.completed) TextMuted else TextPrimary
                            )
                            if (task.description.isNotBlank()) {
                                Text(task.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                        }
                        IconButton(onClick = { viewModel.deleteTask(task.id) }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete task", tint = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotesManagerModuleScreen(
    viewModel: JarvisViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val studioState by viewModel.studioState.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("Ideas") }

    val sortedNotes = remember(notes) {
        notes.sortedByDescending { it.pinned }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ModuleHeaderBar(
                title = "Notes Manager",
                subtitle = "Pinned cloud notes & AI summarization",
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                onBack = onBack
            )
        }

        item {
            JarvisGlassCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Note title", color = TextMuted) },
                    singleLine = true,
                    colors = jarvisFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text("Write note content...", color = TextMuted) },
                    minLines = 3,
                    colors = jarvisFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text("Tag") },
                    singleLine = true,
                    colors = jarvisFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (title.isNotBlank() || content.isNotBlank()) {
                            viewModel.addNote(title, content, tag)
                            title = ""
                            content = ""
                        }
                    },
                    enabled = title.isNotBlank() || content.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = ObsidianBlack),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Save Note")
                }
            }
        }

        if (studioState is AsyncUiState.Streaming || studioState is AsyncUiState.Success) {
            item {
                val aiText = when (val s = studioState) {
                    is AsyncUiState.Streaming -> s.text
                    is AsyncUiState.Success -> s.data
                    else -> ""
                }
                JarvisGlassCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                    Text("AI SUMMARY", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                    Spacer(modifier = Modifier.height(6.dp))
                    JarvisMarkdownText(markdown = aiText)
                }
            }
        }

        items(sortedNotes, key = { it.id }) { note ->
            JarvisGlassCard(highlightBorder = note.pinned, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(note.tag.uppercase(), style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                        Text(note.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                    Row {
                        IconButton(onClick = {
                            viewModel.runStudioPrompt(
                                "notes_ai",
                                "Summarize and extract key action items from this note:\nTitle: ${note.title}\n${note.content}"
                            )
                        }) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = "Summarize with AI", tint = NeonCyan)
                        }
                        IconButton(onClick = { viewModel.togglePinNote(note) }) {
                            Icon(
                                Icons.Filled.PushPin,
                                contentDescription = "Pin note",
                                tint = if (note.pinned) NeonCyan else TextSecondary
                            )
                        }
                        IconButton(onClick = { viewModel.deleteNote(note.id) }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete note", tint = TextSecondary)
                        }
                    }
                }
                if (note.content.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(note.content, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
fun ReminderManagerModuleScreen(
    viewModel: JarvisViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    var title by remember { mutableStateOf("") }
    var timeLabel by remember { mutableStateOf("Today • 6:00 PM") }
    var category by remember { mutableStateOf("Personal") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ModuleHeaderBar(
                title = "Reminder Manager",
                subtitle = "Persistent time alerts & quick checklists",
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                onBack = onBack
            )
        }

        item {
            JarvisGlassCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Remind me to...", color = TextMuted) },
                    singleLine = true,
                    colors = jarvisFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = timeLabel,
                        onValueChange = { timeLabel = it },
                        label = { Text("When") },
                        singleLine = true,
                        colors = jarvisFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        singleLine = true,
                        colors = jarvisFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.addReminder(title, timeLabel, category)
                            title = ""
                        }
                    },
                    enabled = title.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = ObsidianBlack),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Set Reminder")
                }
            }
        }

        items(reminders, key = { it.id }) { item ->
            JarvisGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = item.completed,
                        onCheckedChange = { viewModel.toggleReminder(item) },
                        colors = CheckboxDefaults.colors(checkedColor = NeonCyan, checkmarkColor = ObsidianBlack)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${item.timeLabel} • ${item.category}", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                textDecoration = if (item.completed) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            color = if (item.completed) TextMuted else TextPrimary
                        )
                    }
                    IconButton(onClick = { viewModel.deleteReminder(item.id) }) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete reminder", tint = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun KnowledgeCenterModuleScreen(
    viewModel: JarvisViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val entries by viewModel.knowledgeEntries.collectAsStateWithLifecycle()
    val studioState by viewModel.studioState.collectAsStateWithLifecycle()

    var question by remember { mutableStateOf("") }
    var docTitle by remember { mutableStateOf("") }
    var docCategory by remember { mutableStateOf("SOP") }
    var docContent by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ModuleHeaderBar(
                title = "Personal Knowledge Hub",
                subtitle = "Store personal docs & ask JARVIS grounded Q&A",
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                onBack = onBack
            )
        }

        item {
            JarvisGlassCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                Text("ASK YOUR KNOWLEDGE BASE", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    placeholder = { Text("Ask JARVIS about your saved knowledge...", color = TextMuted) },
                    colors = jarvisFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (question.isNotBlank()) {
                            viewModel.askKnowledgeCenter(question.trim())
                        }
                    },
                    enabled = question.isNotBlank() && studioState !is AsyncUiState.Loading,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = ObsidianBlack),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Query Knowledge Core")
                }

                when (val s = studioState) {
                    is AsyncUiState.Loading -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = NeonCyan)
                    }
                    is AsyncUiState.Streaming -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        JarvisMarkdownText(markdown = s.text)
                    }
                    is AsyncUiState.Success -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        JarvisMarkdownText(markdown = s.data)
                    }
                    is AsyncUiState.Error -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(s.message, color = Color(0xFFFCA5A5))
                    }
                    else -> {}
                }
            }
        }

        item {
            JarvisGlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("ADD KNOWLEDGE ENTRY", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = docTitle,
                    onValueChange = { docTitle = it },
                    placeholder = { Text("Entry title", color = TextMuted) },
                    singleLine = true,
                    colors = jarvisFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = docCategory,
                    onValueChange = { docCategory = it },
                    label = { Text("Category") },
                    singleLine = true,
                    colors = jarvisFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = docContent,
                    onValueChange = { docContent = it },
                    placeholder = { Text("Paste reference notes, SOPs, or personal facts...", color = TextMuted) },
                    minLines = 3,
                    colors = jarvisFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        if (docTitle.isNotBlank() && docContent.isNotBlank()) {
                            viewModel.addKnowledgeEntry(docTitle, docContent.take(120), docContent, docCategory)
                            docTitle = ""
                            docContent = ""
                        }
                    },
                    border = BorderStroke(1.dp, NeonCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Store in Knowledge Vault", color = NeonCyan)
                }
            }
        }

        items(entries, key = { it.id }) { entry ->
            JarvisGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.category.uppercase(), style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                        Text(entry.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                    IconButton(onClick = { viewModel.deleteKnowledgeEntry(entry.id) }) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete entry", tint = TextSecondary)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(entry.content, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        }
    }
}
