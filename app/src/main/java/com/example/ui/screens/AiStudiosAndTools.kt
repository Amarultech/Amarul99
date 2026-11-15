package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.MathEvaluator
import com.example.ui.components.JarvisGlassCard
import com.example.ui.components.JarvisMarkdownText
import com.example.ui.components.ModuleHeaderBar
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.HudSurface
import com.example.ui.theme.HudSurfaceElevated
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AsyncUiState
import com.example.ui.viewmodel.JarvisViewModel

enum class AiStudioType(
    val title: String,
    val subtitle: String,
    val promptHint: String,
    val optionsLabel: String,
    val options: List<String>,
    val moduleKey: String
) {
    CONTENT_WRITER(
        title = "AI Content Writer",
        subtitle = "Craft blogs, emails, social posts & proposals",
        promptHint = "Topic, audience, and key points...",
        optionsLabel = "Format",
        options = listOf("Blog Post", "Executive Email", "LinkedIn Thread", "Product Launch"),
        moduleKey = "content_writer"
    ),
    YOUTUBE_CREATOR(
        title = "YouTube Creator Studio",
        subtitle = "Viral titles, retention hooks, SEO tags & descriptions",
        promptHint = "Describe your video topic or niche...",
        optionsLabel = "Goal",
        options = listOf("Full Package", "10 Viral Titles", "Hook & Intro", "SEO Description"),
        moduleKey = "youtube_creator"
    ),
    IMAGE_PROMPT_GEN(
        title = "AI Image Prompt Lab",
        subtitle = "Engineer photorealistic & artistic prompts",
        promptHint = "Subject, mood, or scene idea...",
        optionsLabel = "Aesthetic",
        options = listOf("Cyberpunk Neon", "Photorealistic 85mm", "Minimalist 3D", "Editorial Studio"),
        moduleKey = "image_prompt"
    ),
    VIDEO_SCRIPT_GEN(
        title = "AI Video Script Studio",
        subtitle = "Structured scene-by-scene scripts with visual cues",
        promptHint = "Video concept and target duration...",
        optionsLabel = "Format",
        options = listOf("60s Short/Reel", "5-Min Explainer", "Product Demo", "Documentary"),
        moduleKey = "video_script"
    ),
    TRANSLATOR(
        title = "Translation Assistant",
        subtitle = "Context-aware neural translation + pronunciation",
        promptHint = "Enter text to translate...",
        optionsLabel = "Target Language",
        options = listOf("Spanish", "French", "German", "Japanese", "Arabic", "Hindi", "Bengali", "Mandarin"),
        moduleKey = "translator"
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiStudioModuleScreen(
    studioType: AiStudioType,
    viewModel: JarvisViewModel,
    onSpeakText: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val studioState by viewModel.studioState.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

    var inputPrompt by remember(studioType) { mutableStateOf("") }
    var selectedOption by remember(studioType) { mutableStateOf(studioType.options.first()) }
    var savedNotice by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ModuleHeaderBar(
                title = studioType.title,
                subtitle = studioType.subtitle,
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                onBack = onBack
            )
        }

        item {
            JarvisGlassCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                Text(studioType.optionsLabel.uppercase(), style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    studioType.options.forEach { option ->
                        FilterChip(
                            selected = selectedOption == option,
                            onClick = { selectedOption = option },
                            label = { Text(option) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                                selectedLabelColor = NeonCyan,
                                containerColor = HudSurface,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = inputPrompt,
                    onValueChange = { inputPrompt = it },
                    placeholder = { Text(studioType.promptHint, color = TextMuted) },
                    minLines = 3,
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
                        .testTag("studio_prompt_input")
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        savedNotice = null
                        val compiled = when (studioType) {
                            AiStudioType.CONTENT_WRITER ->
                                "Write a polished $selectedOption about: $inputPrompt. Include compelling structure and clear takeaways."
                            AiStudioType.YOUTUBE_CREATOR ->
                                "Act as a YouTube growth strategist. Generate [$selectedOption] for a video about: $inputPrompt. Include clickable hooks, keywords, and thumbnail ideas."
                            AiStudioType.IMAGE_PROMPT_GEN ->
                                "Generate 3 detailed AI image generation prompts in [$selectedOption] style for: $inputPrompt. Include lighting, camera lens, composition, and negative prompt tips."
                            AiStudioType.VIDEO_SCRIPT_GEN ->
                                "Write a complete [$selectedOption] video script about: $inputPrompt. Format with [VISUAL CUE] and [VOICEOVER] sections."
                            AiStudioType.TRANSLATOR ->
                                "Translate the following text naturally into $selectedOption. Provide the direct translation first, followed by a brief tone/cultural note:\n\n$inputPrompt"
                        }
                        viewModel.runStudioPrompt(studioType.moduleKey, compiled)
                    },
                    enabled = inputPrompt.isNotBlank() && studioState !is AsyncUiState.Loading,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = ObsidianBlack),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("studio_generate_button")
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate with JARVIS", fontWeight = FontWeight.Bold)
                }
            }
        }

        val outputText = when (val s = studioState) {
            is AsyncUiState.Streaming -> s.text
            is AsyncUiState.Success -> s.data
            else -> null
        }

        if (studioState is AsyncUiState.Loading) {
            item {
                JarvisGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = NeonCyan, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Generating studio output...", style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                    }
                }
            }
        } else if (studioState is AsyncUiState.Error) {
            item {
                JarvisGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = (studioState as AsyncUiState.Error).message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFCA5A5)
                    )
                }
            }
        } else if (outputText != null) {
            item {
                JarvisGlassCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("GENERATED OUTPUT", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                        Row {
                            IconButton(onClick = {
                                clipboard.setText(AnnotatedString(outputText))
                                savedNotice = "Copied to clipboard"
                            }) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy output", tint = TextSecondary)
                            }
                            IconButton(onClick = { onSpeakText(outputText) }) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Read aloud", tint = NeonCyan)
                            }
                            IconButton(onClick = {
                                viewModel.addNote(
                                    title = "${studioType.title}: ${inputPrompt.take(36)}",
                                    content = outputText,
                                    tag = studioType.moduleKey
                                )
                                savedNotice = "Saved to Notes Manager"
                            }) {
                                Icon(Icons.Filled.BookmarkAdd, contentDescription = "Save to Notes", tint = EmeraldPulse)
                            }
                        }
                    }
                    if (savedNotice != null) {
                        Text(savedNotice ?: "", style = MaterialTheme.typography.labelSmall, color = EmeraldPulse)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    JarvisMarkdownText(markdown = outputText)
                }
            }
        }
    }
}

@Composable
fun CalculatorModuleScreen(
    viewModel: JarvisViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val studioState by viewModel.studioState.collectAsStateWithLifecycle()
    var expression by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("0") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val history = remember { mutableStateListOf<String>() }
    var aiMathQuestion by remember { mutableStateOf("") }

    fun evaluateNow() {
        if (expression.isBlank()) return
        try {
            val value = MathEvaluator.evaluate(expression)
            val formatted = MathEvaluator.formatResult(value)
            resultText = formatted
            errorMsg = null
            history.add(0, "$expression = $formatted")
            if (history.size > 10) history.removeAt(history.lastIndex)
        } catch (e: Exception) {
            errorMsg = e.localizedMessage ?: "Invalid expression"
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ModuleHeaderBar(
                title = "Scientific Calculator",
                subtitle = "Precision parser + AI step-by-step math solver",
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                onBack = onBack
            )
        }

        item {
            JarvisGlassCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = expression.ifBlank { "0" },
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = TextSecondary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = errorMsg ?: resultText,
                    style = MaterialTheme.typography.displayLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = if (errorMsg != null) CrimsonError else NeonCyan,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calculator_result_text")
                )
                Spacer(modifier = Modifier.height(14.dp))

                val rows = listOf(
                    listOf("sin(", "cos(", "tan(", "sqrt("),
                    listOf("(", ")", "^", "÷"),
                    listOf("7", "8", "9", "×"),
                    listOf("4", "5", "6", "-"),
                    listOf("1", "2", "3", "+"),
                    listOf("C", "0", ".", "=")
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { key ->
                                val isEquals = key == "="
                                val isClear = key == "C"
                                Surface(
                                    onClick = {
                                        when (key) {
                                            "C" -> {
                                                expression = ""
                                                resultText = "0"
                                                errorMsg = null
                                            }
                                            "=" -> evaluateNow()
                                            else -> {
                                                expression += key
                                                errorMsg = null
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = when {
                                        isEquals -> NeonCyan
                                        isClear -> CrimsonError.copy(alpha = 0.25f)
                                        key in listOf("÷", "×", "-", "+", "^") -> ElectricBlue.copy(alpha = 0.22f)
                                        else -> HudSurfaceElevated
                                    },
                                    border = BorderStroke(1.dp, GlassBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("calc_key_$key")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = key,
                                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                            color = if (isEquals) ObsidianBlack else TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (history.isNotEmpty()) {
            item {
                JarvisGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("CALCULATION HISTORY", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    history.forEach { entry ->
                        Text(
                            text = entry,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = NeonCyan
                        )
                    }
                }
            }
        }

        // AI Math Solver
        item {
            JarvisGlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("AI STEP-BY-STEP MATH SOLVER", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = aiMathQuestion,
                    onValueChange = { aiMathQuestion = it },
                    placeholder = { Text("e.g. Solve compound interest on $5000 at 6% for 10 years", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = DeepSpace,
                        unfocusedContainerColor = DeepSpace,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.runStudioPrompt(
                            "calculator_ai",
                            "Solve this math/finance problem clearly with step-by-step calculations and the final answer:\n$aiMathQuestion"
                        )
                    },
                    enabled = aiMathQuestion.isNotBlank() && studioState !is AsyncUiState.Loading,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = ObsidianBlack),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Solve with JARVIS")
                }
                val aiReply = when (val s = studioState) {
                    is AsyncUiState.Streaming -> s.text
                    is AsyncUiState.Success -> s.data
                    else -> null
                }
                if (aiReply != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    JarvisMarkdownText(markdown = aiReply)
                }
            }
        }
    }
}

@Composable
fun WeatherModuleScreen(
    viewModel: JarvisViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val weatherState by viewModel.weatherState.collectAsStateWithLifecycle()
    val studioState by viewModel.studioState.collectAsStateWithLifecycle()
    var cityInput by remember { mutableStateOf("San Francisco") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ModuleHeaderBar(
                title = "Atmospheric Telemetry",
                subtitle = "Live Open-Meteo weather & AI briefing",
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                onBack = onBack
            )
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = cityInput,
                    onValueChange = { cityInput = it },
                    placeholder = { Text("Enter city name...", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = DeepSpace,
                        unfocusedContainerColor = DeepSpace,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("weather_city_input")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    onClick = { viewModel.loadWeather(cityInput) },
                    color = NeonCyan,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .size(54.dp)
                        .testTag("weather_search_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Search, contentDescription = "Search city weather", tint = ObsidianBlack)
                    }
                }
            }
        }

        when (val state = weatherState) {
            is AsyncUiState.Loading -> {
                item {
                    JarvisGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = NeonCyan)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Syncing live atmospheric data...", color = TextSecondary)
                        }
                    }
                }
            }
            is AsyncUiState.Error -> {
                item {
                    JarvisGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(state.message, color = Color(0xFFFCA5A5))
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.loadWeather(cityInput) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = ObsidianBlack)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry")
                        }
                    }
                }
            }
            is AsyncUiState.Success -> {
                val w = state.data
                item {
                    JarvisGlassCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("LIVE TELEMETRY", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(w.locationLabel, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                                Text(w.conditionLabel, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Icon(Icons.Filled.WbSunny, contentDescription = w.conditionLabel, tint = NeonCyan, modifier = Modifier.size(28.dp))
                                Text("${w.temperatureCelsius}°C", style = MaterialTheme.typography.displayLarge, color = TextPrimary)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Feels like: ${w.feelsLikeCelsius}°C", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            Text("Humidity: ${w.humidityPercent}%", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            Text("Wind: ${w.windSpeedKmh} km/h", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                viewModel.runStudioPrompt(
                                    "weather_brief",
                                    "Give a crisp 3-sentence JARVIS atmospheric & outfit/commute briefing for ${w.locationLabel}: ${w.temperatureCelsius}°C (feels like ${w.feelsLikeCelsius}°C), ${w.conditionLabel}, humidity ${w.humidityPercent}%, wind ${w.windSpeedKmh} km/h."
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AI Weather Briefing")
                        }
                        val brief = when (val s = studioState) {
                            is AsyncUiState.Streaming -> s.text
                            is AsyncUiState.Success -> s.data
                            else -> null
                        }
                        if (brief != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            JarvisMarkdownText(markdown = brief)
                        }
                    }
                }

                items(w.forecast) { day ->
                    JarvisGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(day.dateLabel, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text(day.condition, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                            Text(
                                text = "${day.lowCelsius.toInt()}° / ${day.highCelsius.toInt()}°C",
                                style = MaterialTheme.typography.titleMedium,
                                color = NeonCyan
                            )
                        }
                    }
                }
            }
            else -> {}
        }
    }
}
