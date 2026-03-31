package com.fatum.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.fatum.presentation.theme.FatumColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ── FatumCard ─────────────────────────────────────────────────────────────────
@Composable
fun FatumCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null,
              highlight: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val bg  = if (highlight) FatumColors.GreenSurface else FatumColors.Surface
    val bdr = if (highlight) FatumColors.GreenBorder  else FatumColors.Border
    Surface(
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
        shape  = RoundedCornerShape(16.dp), color = bg,
        border = BorderStroke(1.dp, bdr)
    ) { Column(Modifier.padding(16.dp), content = content) }
}

// ── FatumButton ───────────────────────────────────────────────────────────────
@Composable
fun FatumButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
                enabled: Boolean = true, icon: (@Composable () -> Unit)? = null) {
    Button(
        onClick = onClick, enabled = enabled,
        modifier = modifier.height(48.dp), shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = FatumColors.Green, contentColor = Color(0xFF0F1117),
            disabledContainerColor = FatumColors.SurfaceVariant, disabledContentColor = FatumColors.TextMuted
        )
    ) {
        if (icon != null) { icon(); Spacer(Modifier.width(6.dp)) }
        Text(text, style = MaterialTheme.typography.titleSmall,
             fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
    }
}

// ── SectionHeader ─────────────────────────────────────────────────────────────
@Composable
fun SectionHeader(title: String, action: (@Composable () -> Unit)? = null, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelSmall,
             color = FatumColors.TextMuted, letterSpacing = 1.sp)
        action?.invoke()
    }
}

// ── StreakBadge ───────────────────────────────────────────────────────────────
@Composable
fun StreakBadge(streak: Int, modifier: Modifier = Modifier) {
    if (streak <= 0) return
    Row(modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFF2A1F12))
        .border(1.dp, Color(0xFF4A2E12), RoundedCornerShape(20.dp))
        .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Text("🔥", style = MaterialTheme.typography.labelSmall)
        Text("$streak", style = MaterialTheme.typography.titleSmall, color = FatumColors.Warning)
    }
}

// ── HabitToggle ───────────────────────────────────────────────────────────────
@Composable
fun HabitToggle(done: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scale by animateFloatAsState(if (done) 1f else 0.88f,
        spring(Spring.DampingRatioMediumBouncy), label = "scale")
    Box(modifier.size(40.dp).graphicsLayer { scaleX = scale; scaleY = scale }
        .clip(CircleShape)
        .background(if (done) FatumColors.Green else FatumColors.SurfaceVariant)
        .border(2.dp, if (done) FatumColors.GreenDim else FatumColors.Border, CircleShape)
        .clickable(onClick = onClick),
        contentAlignment = Alignment.Center) {
        AnimatedVisibility(done, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
            Icon(Icons.Default.Check, null, tint = Color(0xFF0F1117), modifier = Modifier.size(20.dp))
        }
    }
}

// ── Priority pill button: HIGH / MEDIUM / LOW ─────────────────────────────────
@Composable
fun PriorityButton(priority: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("HIGH","MEDIUM","LOW").forEach { p ->
            val sel = p == priority
            val (bg, txt) = when (p) {
                "HIGH"   -> FatumColors.Error.copy(.15f) to FatumColors.Error
                "MEDIUM" -> FatumColors.Warning.copy(.15f) to FatumColors.Warning
                else     -> FatumColors.SurfaceVariant to FatumColors.TextMuted
            }
            val label = when(p) { "HIGH" -> "Alta"; "MEDIUM" -> "Media"; else -> "Baja" }
            Box(Modifier.clip(RoundedCornerShape(20.dp))
                .background(if (sel) bg else FatumColors.SurfaceVariant)
                .border(1.dp, if (sel) txt else FatumColors.Border, RoundedCornerShape(20.dp))
                .clickable { onSelect(p) }
                .padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text(label, style = MaterialTheme.typography.labelLarge,
                     color = if (sel) txt else FatumColors.TextMuted)
            }
        }
    }
}

// ── Priority colored dot ──────────────────────────────────────────────────────
@Composable
fun PriorityDot(priority: String, modifier: Modifier = Modifier) {
    val color = when (priority) { "HIGH" -> FatumColors.Error; "MEDIUM" -> FatumColors.Warning; else -> FatumColors.TextMuted }
    Box(modifier.size(8.dp).clip(CircleShape).background(color))
}

// ── FatumChip ─────────────────────────────────────────────────────────────────
@Composable
fun FatumChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(20.dp))
        .background(if (selected) FatumColors.GreenSurface else FatumColors.SurfaceVariant)
        .border(1.dp, if (selected) FatumColors.GreenBorder else FatumColors.Border, RoundedCornerShape(20.dp))
        .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge,
             color = if (selected) FatumColors.Green else FatumColors.TextSecondary)
    }
}

// ── FatumProgressBar ──────────────────────────────────────────────────────────
@Composable
fun FatumProgressBar(progress: Float, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(FatumColors.SurfaceVariant)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(progress.coerceIn(0f,1f))
            .clip(RoundedCornerShape(3.dp))
            .background(Brush.horizontalGradient(listOf(FatumColors.GreenDim, FatumColors.Green))))
    }
}

// ── StatTile ──────────────────────────────────────────────────────────────────
@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(FatumColors.SurfaceVariant)
        .padding(horizontal = 16.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = FatumColors.Green)
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = FatumColors.TextMuted)
    }
}

// ── EmptyState ────────────────────────────────────────────────────────────────
@Composable
fun EmptyState(emoji: String, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally,
           verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(emoji, style = MaterialTheme.typography.displayMedium)
        Text(title, style = MaterialTheme.typography.titleLarge, color = FatumColors.TextPrimary)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = FatumColors.TextMuted, textAlign = TextAlign.Center)
    }
}

// ── GitHub-style heatmap for a single habit ───────────────────────────────────
@Composable
fun HabitHeatmap(data: Map<String, Int>, maxVal: Int, modifier: Modifier = Modifier) {
    val today    = LocalDate.now()
    val monday   = today.minusDays(today.dayOfWeek.value.toLong() - 1)
    val startDay = monday.minusWeeks(16)  // 17 weeks ≈ 4 months for per-habit view
    val fmt      = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val maxF     = maxOf(maxVal, 1).toFloat()

    Box(modifier.horizontalScroll(rememberScrollState())) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            (0 until 17).forEach { week ->
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    (0 until 7).forEach { dow ->
                        val date  = startDay.plusDays((week * 7 + dow).toLong())
                        val key   = date.format(fmt)
                        val count = data[key] ?: 0
                        val inten = count / maxF
                        val color = when {
                            count == 0   -> FatumColors.Heat0
                            inten < 0.25 -> FatumColors.Heat1
                            inten < 0.50 -> FatumColors.Heat2
                            inten < 0.75 -> FatumColors.Heat3
                            else         -> FatumColors.Heat4
                        }
                        Box(Modifier.size(11.dp).clip(RoundedCornerShape(2.dp)).background(color))
                    }
                }
            }
        }
    }
}

// ── Full-year heatmap ─────────────────────────────────────────────────────────
@Composable
fun HeatMapGrid(data: Map<String, Int>, onDayClick: (String) -> Unit, modifier: Modifier = Modifier) {
    val today    = LocalDate.now()
    val monday   = today.minusDays(today.dayOfWeek.value.toLong() - 1)
    val startDay = monday.minusWeeks(51)
    val fmt      = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val maxCount = (data.values.maxOrNull() ?: 1).toFloat().coerceAtLeast(1f)

    Box(modifier.horizontalScroll(rememberScrollState())) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            (0 until 52).forEach { week ->
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    (0 until 7).forEach { dow ->
                        val date  = startDay.plusDays((week * 7 + dow).toLong())
                        val key   = date.format(fmt)
                        val count = data[key] ?: 0
                        val inten = count / maxCount
                        val color = when {
                            count == 0   -> FatumColors.Heat0
                            inten < 0.25 -> FatumColors.Heat1
                            inten < 0.50 -> FatumColors.Heat2
                            inten < 0.75 -> FatumColors.Heat3
                            else         -> FatumColors.Heat4
                        }
                        Box(Modifier.size(11.dp).clip(RoundedCornerShape(2.dp)).background(color).clickable { onDayClick(key) })
                    }
                }
            }
        }
    }
}

// ── OutlinedTextField colors ───────────────────────────────────────────────────
@Composable
fun fatumOutlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = FatumColors.Green,
    unfocusedBorderColor    = FatumColors.Border,
    focusedTextColor        = FatumColors.TextPrimary,
    unfocusedTextColor      = FatumColors.TextPrimary,
    cursorColor             = FatumColors.Green,
    focusedLabelColor       = FatumColors.Green,
    unfocusedLabelColor     = FatumColors.TextMuted,
    focusedContainerColor   = Color.Transparent,
    unfocusedContainerColor = Color.Transparent
)

// ── Importance color helper ────────────────────────────────────────────────────
fun importanceColor(importance: String) = when (importance) {
    "HIGH"   -> FatumColors.Error
    "MEDIUM" -> FatumColors.Warning
    else     -> FatumColors.TextMuted
}

// ── Priority label helper ──────────────────────────────────────────────────────
fun priorityLabel(p: String) = when(p) { "HIGH" -> "Alta"; "MEDIUM" -> "Media"; else -> "Baja" }
