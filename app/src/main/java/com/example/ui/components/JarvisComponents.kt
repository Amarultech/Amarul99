package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyberIndigo
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.HudSurface
import com.example.ui.theme.HudSurfaceElevated
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun JarvisGlassCard(
    modifier: Modifier = Modifier,
    highlightBorder: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val borderBrush = if (highlightBorder) {
        Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.65f), ElectricBlue.copy(alpha = 0.45f)))
    } else {
        Brush.linearGradient(listOf(GlassBorder, Color(0xFF132038)))
    }

    val surfaceModifier = modifier
        .clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(
                    HudSurfaceElevated.copy(alpha = 0.85f),
                    HudSurface.copy(alpha = 0.78f)
                )
            )
        )
        .border(BorderStroke(1.dp, borderBrush), shape)
        .let { base ->
            if (onClick != null) {
                base.clickable(role = Role.Button, onClick = onClick)
            } else {
                base
            }
        }

    Column(
        modifier = surfaceModifier.padding(16.dp),
        content = content
    )
}

@Composable
fun JarvisStatusBadge(
    label: String,
    dotColor: Color = EmeraldPulse,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "status_pulse")
    val alpha by infinite.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    Surface(
        modifier = modifier,
        color = dotColor.copy(alpha = 0.12f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, dotColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor.copy(alpha = alpha))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = dotColor
            )
        }
    }
}

@Composable
fun JarvisAnimatedOrb(
    modifier: Modifier = Modifier,
    size: Dp = 176.dp,
    isActive: Boolean = false,
    isProcessing: Boolean = false,
    rmsLevel: Float = 0f,
    statusLabel: String = "TAP TO SPEAK",
    onClick: (() -> Unit)? = null
) {
    val infinite = rememberInfiniteTransition(label = "jarvis_orb")
    val outerAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isProcessing || isActive) 4500 else 12000, easing = LinearEasing)
        ),
        label = "outer_angle"
    )
    val innerAngle by infinite.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isProcessing || isActive) 3200 else 9000, easing = LinearEasing)
        ),
        label = "inner_angle"
    )
    val idleBreath by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )
    val animatedRms by animateFloatAsState(
        targetValue = if (isActive) rmsLevel.coerceIn(0.1f, 1f) else 0f,
        animationSpec = spring(stiffness = 400f),
        label = "rms_anim"
    )

    val clickModifier = if (onClick != null) {
        Modifier
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }

    Column(
        modifier = modifier.semantics {
            contentDescription = "JARVIS Neural Core Orb: $statusLabel"
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .then(clickModifier),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(size)) {
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val maxR = this.size.minDimension / 2f
                val pulseScale = if (isActive) 1f + animatedRms * 0.14f else idleBreath

                // Ambient glow halo
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            (if (isActive) NeonCyan else ElectricBlue).copy(alpha = if (isActive) 0.34f else 0.2f),
                            CyberIndigo.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = maxR * pulseScale
                    ),
                    radius = maxR * pulseScale,
                    center = center
                )

                // Outer segmented arc ring
                val outerDiameter = maxR * 1.72f
                val outerTopLeft = Offset(center.x - outerDiameter / 2f, center.y - outerDiameter / 2f)
                for (i in 0 until 3) {
                    drawArc(
                        color = NeonCyan.copy(alpha = if (isActive) 0.95f else 0.55f),
                        startAngle = outerAngle + i * 120f,
                        sweepAngle = 72f,
                        useCenter = false,
                        topLeft = outerTopLeft,
                        size = Size(outerDiameter, outerDiameter),
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Middle counter-rotating electric ring
                val midDiameter = maxR * 1.38f
                val midTopLeft = Offset(center.x - midDiameter / 2f, center.y - midDiameter / 2f)
                for (i in 0 until 4) {
                    drawArc(
                        color = ElectricBlue.copy(alpha = 0.85f),
                        startAngle = innerAngle + i * 90f,
                        sweepAngle = 52f,
                        useCenter = false,
                        topLeft = midTopLeft,
                        size = Size(midDiameter, midDiameter),
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Inner dashed precision ring
                val innerDiameter = maxR * 1.06f
                val innerTopLeft = Offset(center.x - innerDiameter / 2f, center.y - innerDiameter / 2f)
                drawArc(
                    color = Color(0xFF7DD3FC).copy(alpha = 0.5f),
                    startAngle = outerAngle * 1.5f,
                    sweepAngle = 260f,
                    useCenter = false,
                    topLeft = innerTopLeft,
                    size = Size(innerDiameter, innerDiameter),
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Core sphere
                val coreR = maxR * 0.34f * pulseScale
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White,
                            NeonCyan,
                            ElectricBlue.copy(alpha = 0.85f),
                            ObsidianBlack
                        ),
                        center = center,
                        radius = coreR * 1.15f
                    ),
                    radius = coreR,
                    center = center
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = statusLabel,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) NeonCyan else TextSecondary
        )
    }
}

@Composable
fun QuotaProgressCard(
    planTitle: String,
    used: Int,
    limit: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (limit <= 0) 0f else (used.toFloat() / limit.toFloat()).coerceIn(0f, 1f)
    JarvisGlassCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "NEURAL QUOTA • ${planTitle.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$used / ${if (limit >= 10000) "∞" else limit} daily requests",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                color = if (progress > 0.85f) Color(0xFFF59E0B) else NeonCyan
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50)),
            color = NeonCyan,
            trackColor = DeepSpace
        )
    }
}

@Composable
fun ModuleHeaderBar(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onBack: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            Surface(
                onClick = onBack,
                shape = CircleShape,
                color = HudSurfaceElevated,
                border = BorderStroke(1.dp, GlassBorder),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "Back",
                        tint = NeonCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun JarvisMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    textColor: Color = TextPrimary
) {
    val lines = markdown.replace("\r\n", "\n").split("\n")
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        var inCodeBlock = false
        val codeBuffer = StringBuilder()

        lines.forEach { rawLine ->
            val trimmed = rawLine.trim()
            if (trimmed.startsWith("```")) {
                if (!inCodeBlock) {
                    inCodeBlock = true
                    codeBuffer.clear()
                } else {
                    inCodeBlock = false
                    Surface(
                        color = ObsidianBlack.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, GlassBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = codeBuffer.toString().trimEnd(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = NeonCyan,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            } else if (inCodeBlock) {
                codeBuffer.appendLine(rawLine)
            } else if (trimmed.startsWith("### ") || trimmed.startsWith("## ") || trimmed.startsWith("# ")) {
                val cleanHeading = trimmed.removePrefix("### ").removePrefix("## ").removePrefix("# ")
                Text(
                    text = parseInlineMarkdown(cleanHeading),
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonCyan
                )
            } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                val itemText = trimmed.drop(2)
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "• ",
                        style = MaterialTheme.typography.bodyLarge,
                        color = NeonCyan
                    )
                    Text(
                        text = parseInlineMarkdown(itemText),
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor
                    )
                }
            } else if (trimmed.isNotEmpty()) {
                Text(
                    text = parseInlineMarkdown(rawLine),
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor
                )
            } else {
                Spacer(modifier = Modifier.height(2.dp))
            }
        }

        if (inCodeBlock && codeBuffer.isNotEmpty()) {
            Surface(
                color = ObsidianBlack.copy(alpha = 0.9f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, GlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = codeBuffer.toString().trimEnd(),
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = NeonCyan,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}

private fun parseInlineMarkdown(input: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < input.length) {
            if (i + 1 < input.length && input[i] == '*' && input[i + 1] == '*') {
                val end = input.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) {
                        append(input.substring(i + 2, end))
                    }
                    i = end + 2
                    continue
                }
            }
            if (input[i] == '`') {
                val end = input.indexOf('`', i + 1)
                if (end != -1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            color = NeonCyan,
                            background = Color(0xFF0A2036)
                        )
                    ) {
                        append(" ${input.substring(i + 1, end)} ")
                    }
                    i = end + 1
                    continue
                }
            }
            append(input[i])
            i++
        }
    }
}
