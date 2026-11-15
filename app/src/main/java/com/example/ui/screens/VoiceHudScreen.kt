package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.VoiceAssistantEngine
import com.example.ui.components.JarvisAnimatedOrb
import com.example.ui.components.JarvisGlassCard
import com.example.ui.components.ModuleHeaderBar
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.HudSurfaceElevated
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AsyncUiState
import com.example.ui.viewmodel.JarvisViewModel

@Composable
fun VoiceHudScreen(
    viewModel: JarvisViewModel,
    voiceEngine: VoiceAssistantEngine,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val isListening by voiceEngine.isListening.collectAsStateWithLifecycle()
    val isSpeaking by voiceEngine.isSpeaking.collectAsStateWithLifecycle()
    val rmsLevel by voiceEngine.rmsLevel.collectAsStateWithLifecycle()
    val transcript by voiceEngine.partialTranscript.collectAsStateWithLifecycle()
    val voiceError by voiceEngine.voiceError.collectAsStateWithLifecycle()
    val replyState by viewModel.voiceReplyState.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()

    var fallbackPrompt by remember { mutableStateOf("") }
    var permissionDeniedMsg by remember { mutableStateOf<String?>(null) }

    val speechRate = (profile?.speechRate ?: 1.0).toFloat()

    val startVoiceCapture = {
        permissionDeniedMsg = null
        voiceEngine.clearError()
        voiceEngine.startListening { finalTranscript ->
            viewModel.processVoiceCommand(finalTranscript) { reply ->
                voiceEngine.speak(reply, speechRate)
            }
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startVoiceCapture()
        } else {
            permissionDeniedMsg = "Microphone permission was denied. You can enable it in system settings or type voice commands below."
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ModuleHeaderBar(
                title = "Voice HUD",
                subtitle = "Real-time Speech-to-Text & Neural Voice Synthesis",
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                onBack = onBack
            )
        }

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
                        size = 192.dp,
                        isActive = isListening || isSpeaking,
                        isProcessing = replyState is AsyncUiState.Loading,
                        rmsLevel = if (isSpeaking) 0.55f else rmsLevel,
                        statusLabel = when {
                            isListening -> "LISTENING..."
                            replyState is AsyncUiState.Loading -> "THINKING..."
                            isSpeaking -> "SPEAKING RESPONSE..."
                            else -> "TAP MICROPHONE TO SPEAK"
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (isListening) {
                                    voiceEngine.stopListening()
                                } else {
                                    val hasPerm = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasPerm) {
                                        startVoiceCapture()
                                    } else {
                                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isListening) CrimsonError else NeonCyan,
                                contentColor = if (isListening) Color.White else ObsidianBlack
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("voice_listen_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Filled.Stop else Icons.Filled.Mic,
                                contentDescription = if (isListening) "Stop listening" else "Start listening"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isListening) "Stop Listening" else "Start Listening")
                        }

                        if (isSpeaking) {
                            OutlinedButton(
                                onClick = { voiceEngine.stopSpeaking() },
                                border = BorderStroke(1.dp, NeonCyan),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.VolumeOff, contentDescription = "Stop speaking", tint = NeonCyan)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Mute", color = NeonCyan)
                            }
                        }
                    }

                    if (!voiceEngine.isSpeechRecognitionAvailable) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Note: On-device Google Speech Recognition service is not installed on this device/emulator. Use the text voice simulator below with full Text-to-Speech output.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFFBBF24)
                        )
                    }
                }
            }
        }

        if (permissionDeniedMsg != null || voiceError != null) {
            item {
                Surface(
                    color = CrimsonError.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, CrimsonError.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = permissionDeniedMsg ?: voiceError ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFCA5A5),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        if (transcript.isNotBlank()) {
            item {
                JarvisGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("TRANSCRIPT", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(transcript, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                }
            }
        }

        item {
            when (val state = replyState) {
                is AsyncUiState.Loading -> {
                    JarvisGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = NeonCyan, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Processing voice command...", style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                        }
                    }
                }
                is AsyncUiState.Success -> {
                    JarvisGlassCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("JARVIS VOICE RESPONSE", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                            IconButton(onClick = { voiceEngine.speak(state.data, speechRate) }) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Replay voice response", tint = NeonCyan)
                            }
                        }
                        Text(state.data, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                    }
                }
                is AsyncUiState.Error -> {
                    JarvisGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(state.message, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFFCA5A5))
                    }
                }
                else -> {}
            }
        }

        // Direct command input + presets
        item {
            JarvisGlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("VOICE COMMAND CONSOLE", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = fallbackPrompt,
                    onValueChange = { fallbackPrompt = it },
                    placeholder = { Text("Type or test a voice command...", color = TextMuted) },
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
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val cmd = fallbackPrompt.trim()
                                if (cmd.isNotEmpty()) {
                                    fallbackPrompt = ""
                                    viewModel.processVoiceCommand(cmd) { reply ->
                                        voiceEngine.speak(reply, speechRate)
                                    }
                                }
                            },
                            modifier = Modifier.testTag("voice_command_send_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Execute voice command", tint = NeonCyan)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_command_input")
                )

                Spacer(modifier = Modifier.height(10.dp))
                val quickVoicePrompts = listOf(
                    "Give me a 30-second executive motivation boost",
                    "How should I prioritize my pending tasks today?",
                    "Explain quantum computing in two sentences"
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    quickVoicePrompts.forEach { preset ->
                        Surface(
                            color = HudSurfaceElevated,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.processVoiceCommand(preset) { reply ->
                                        voiceEngine.speak(reply, speechRate)
                                    }
                                }
                        ) {
                            Text(
                                text = preset,
                                style = MaterialTheme.typography.bodyMedium,
                                color = NeonCyan,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
