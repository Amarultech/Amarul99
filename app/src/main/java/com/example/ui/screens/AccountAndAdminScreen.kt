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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.SubscriptionTier
import com.example.ui.components.JarvisGlassCard
import com.example.ui.components.JarvisStatusBadge
import com.example.ui.components.QuotaProgressCard
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.HudSurface
import com.example.ui.theme.HudSurfaceElevated
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AsyncUiState
import com.example.ui.viewmodel.JarvisViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AccountAndAdminScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val usageHistory by viewModel.usageHistory.collectAsStateWithLifecycle()
    val adminUsersState by viewModel.adminUsersState.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    var voiceEnabled by remember(profile?.voiceEnabled) { mutableStateOf(profile?.voiceEnabled ?: true) }
    var speechRate by remember(profile?.speechRate) { mutableFloatStateOf((profile?.speechRate ?: 1.0).toFloat()) }
    var preferredLanguage by remember(profile?.preferredLanguage) { mutableStateOf(profile?.preferredLanguage ?: "English") }

    val isAdmin = profile?.isAdminUser == true
    val todayKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val usedToday = if (profile?.dailyDateKey == todayKey) (profile?.dailyRequestsUsed?.toInt() ?: 0) else 0
    val currentTier = profile?.tier ?: SubscriptionTier.FREE

    LaunchedEffect(isAdmin) {
        if (isAdmin) {
            viewModel.loadAdminUsers()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("ACCOUNT & SAAS CONSOLE", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Text("Subscription tiers, voice controls & usage telemetry", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }

        if (statusBanner != null) {
            item {
                Surface(
                    color = NeonCyan.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(statusBanner ?: "", color = NeonCyan, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "OK",
                            color = TextPrimary,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }

        // Profile Card
        item {
            JarvisGlassCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!profile?.photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = profile?.photoUrl,
                            contentDescription = "User profile avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Surface(
                            color = HudSurfaceElevated,
                            shape = CircleShape,
                            border = BorderStroke(1.dp, NeonCyan),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(Icons.Filled.Person, contentDescription = "User", tint = NeonCyan, modifier = Modifier.padding(14.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(profile?.displayName ?: "Commander", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        Text(profile?.email ?: "", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            JarvisStatusBadge(label = currentTier.title.uppercase(), dotColor = NeonCyan)
                            if (isAdmin) {
                                JarvisStatusBadge(label = "ADMIN", dotColor = EmeraldPulse)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = { signOutUser(credentialManager, scope) },
                    border = BorderStroke(1.dp, CrimsonError.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sign out", tint = CrimsonError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out", color = CrimsonError)
                }
            }
        }

        // Quota Progress
        item {
            QuotaProgressCard(
                planTitle = currentTier.title,
                used = usedToday,
                limit = currentTier.dailyLimit
            )
        }

        // Subscription Plans
        item {
            Text("SUBSCRIPTION PLANS (SANDBOX SWITCHER)", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Preview tier limits instantly. Connect Google Play Billing or Stripe before commercial launch.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        items(SubscriptionTier.entries, key = { it.id }) { tier ->
            val isCurrent = tier == currentTier
            JarvisGlassCard(highlightBorder = isCurrent, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(tier.title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        Text(tier.tagline, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                    Text(tier.priceLabel, style = MaterialTheme.typography.titleMedium, color = NeonCyan, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                tier.features.forEach { feat ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = EmeraldPulse, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(feat, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { viewModel.updatePlan(tier.id) },
                    enabled = !isCurrent,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCurrent) HudSurfaceElevated else NeonCyan,
                        contentColor = if (isCurrent) TextSecondary else ObsidianBlack
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("select_plan_${tier.id}")
                ) {
                    Text(if (isCurrent) "Active Plan" else "Switch to ${tier.title}")
                }
            }
        }

        // Voice & AI Settings
        item {
            JarvisGlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("VOICE & AI SETTINGS", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Speak AI Replies", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("Read chat responses aloud via Text-to-Speech", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                    Switch(
                        checked = voiceEnabled,
                        onCheckedChange = { voiceEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = ObsidianBlack, checkedTrackColor = NeonCyan)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Speech Rate: ${String.format(Locale.US, "%.1fx", speechRate)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Slider(
                    value = speechRate,
                    onValueChange = { speechRate = it },
                    valueRange = 0.6f..1.8f,
                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text("Default Language", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("English", "Spanish", "French", "Hindi").forEach { lang ->
                        FilterChip(
                            selected = preferredLanguage == lang,
                            onClick = { preferredLanguage = lang },
                            label = { Text(lang) },
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
                Button(
                    onClick = { viewModel.updatePreferences(voiceEnabled, speechRate.toDouble(), preferredLanguage) },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Save Preferences")
                }
            }
        }

        // Usage Telemetry
        item {
            JarvisGlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("RECENT AI TELEMETRY", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Lifetime Requests: ${profile?.totalRequests ?: 0}",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonCyan
                )
                if (usageHistory.isEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("No API calls logged yet.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                } else {
                    Spacer(modifier = Modifier.height(6.dp))
                    usageHistory.take(6).forEach { rec ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(rec.module.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                            Text(
                                "${rec.promptChars} in / ${rec.responseChars} out chars",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Admin Dashboard Section (Protected by Firestore role/verified email rules)
        if (isAdmin) {
            item {
                JarvisGlassCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AdminPanelSettings, contentDescription = "Admin", tint = EmeraldPulse)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ADMIN SAAS ANALYTICS", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        }
                        IconButton(onClick = { viewModel.loadAdminUsers() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh admin metrics", tint = NeonCyan)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    when (val state = adminUsersState) {
                        is AsyncUiState.Loading -> {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = NeonCyan)
                        }
                        is AsyncUiState.Error -> {
                            Text(state.message, color = Color(0xFFFCA5A5))
                        }
                        is AsyncUiState.Success -> {
                            val allUsers = state.data
                            val activeToday = allUsers.count { it.dailyDateKey == todayKey && it.dailyRequestsUsed > 0 }
                            val totalApiCalls = allUsers.sumOf { it.totalRequests }
                            val proCount = allUsers.count { it.plan == "pro" }
                            val premCount = allUsers.count { it.plan == "premium" }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AdminStatPill("Users", "${allUsers.size}", Modifier.weight(1f))
                                AdminStatPill("Active Today", "$activeToday", Modifier.weight(1f))
                                AdminStatPill("API Calls", "$totalApiCalls", Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "System: AI Logic Online • Firestore Healthy • Paid Tiers: ${proCount} Pro, ${premCount} Enterprise",
                                style = MaterialTheme.typography.bodyMedium,
                                color = EmeraldPulse
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            allUsers.take(15).forEach { u ->
                                Surface(
                                    color = HudSurfaceElevated,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, GlassBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(u.displayName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                            Text("${u.email} • ${u.totalRequests} reqs", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                        }
                                        Text(u.plan.uppercase(), style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                                    }
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminStatPill(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = HudSurfaceElevated,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, GlassBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = NeonCyan)
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}
