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
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.fatum.presentation.theme.FatumColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ─────────────────────────────────────────────────────────────────────────────
// Surface card — the building block of all screens
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun FatumCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    highlight: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val bgColor = if (highlight) FatumColors.GreenSurface else FatumColors.Surface
    val borderColor = if (highlight) FatumColors.GreenBorder else FatumColors.Border
    val m = if (onClick != null) modifier.clickable(onClick = onClick) else modifier
    Surface(
        modifier = m,
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Green primary button — loggd.life style
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun FatumButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = FatumColors.Green,
            contentColor   = Color(0xFF0F1117),
            disabledContainerColor = FatumColors.SurfaceVariant,
            disabledContentColor   = FatumColors.TextMuted
        )
    ) {
        if (icon != null) { icon(); Spacer(Modifier.width(6.dp)) }
        Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section label — uppercase small text like loggd.life section headers
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SectionHeader(
    title: String,
    action: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = FatumColors.TextMuted,
            letterSpacing = 1.sp
        )
        action?.invoke()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Streak badge — fire + count, loggd.life style
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun StreakBadge(streak: Int, modifier: Modifier = Modifier) {
    if (streak <= 0) return
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF2A1F12))
            .border(1.dp, Color(0xFF4A2E12), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text("🔥", style = MaterialTheme.typography.bodySmall)
        Text(
            text = "$streak",
            style = MaterialTheme.typography.titleSmall,
            color = FatumColors.Warning
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Circular habit toggle — green check on completion, loggd.life style
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun HabitToggle(
    done: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animScale by animateFloatAsState(
        targetValue = if (done) 1f else 0.85f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "toggle_scale"
    )
    Box(
        modifier = modifier
            .size(40.dp)
            .graphicsLayer { scaleX = animScale; scaleY = animScale }
            .clip(CircleShape)
            .background(if (done) FatumColors.Green else FatumColors.SurfaceVariant)
            .border(2.dp, if (done) FatumColors.GreenDim else FatumColors.Border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(visible = done, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color(0xFF0F1117),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Mood picker — loggd.life style emoji row
// ─────────────────────────────────────────────────────────────────────────────
@Composable
    current: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val moods = listOf("😞","😟","😕","😐","🙂","😊","😄","😁","🤩","🥳")
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        moods.forEachIndexed { idx, emoji ->
            val n = idx + 1
            val selected = n == current
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) FatumColors.GreenSurface else Color.Transparent)
                    .border(
                        width = if (selected) 1.dp else 0.dp,
                        color = if (selected) FatumColors.GreenBorder else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelect(n) }
                    .padding(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Text(emoji, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "$n",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) FatumColors.Green else FatumColors.TextMuted
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Priority stars
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PriorityStars(
    value: Int,
    onValueChange: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        (1..3).forEach { n ->
            val filled = n <= value
            val color = when { n > value -> FatumColors.Border; value == 1 -> FatumColors.Star1; value == 2 -> FatumColors.Star2; else -> FatumColors.Star3 }
            Icon(
                if (filled) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = null,
                tint = color,
                modifier = Modifier
                    .size(16.dp)
                    .then(if (onValueChange != null) Modifier.clickable { onValueChange(n) } else Modifier)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GitHub/loggd.life-style year heat-map
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun HeatMapGrid(
    data: Map<String, Int>,
    onDayClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val today     = LocalDate.now()
    val monday    = today.minusDays(today.dayOfWeek.value.toLong() - 1)
    val startDay  = monday.minusWeeks(51)
    val allDates  = (0 until 364).map { startDay.plusDays(it.toLong()) }
    val maxCount  = (data.values.maxOrNull() ?: 1).toFloat().coerceAtLeast(1f)
    val fmt       = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    Column(modifier = modifier) {
        // Month labels row
        Row(modifier = Modifier.padding(start = 20.dp)) {
            val monthsSeen = mutableSetOf<Int>()
            allDates.filterIndexed { i, _ -> i % 7 == 0 }.forEach { weekStart ->
                val m = weekStart.monthValue
                if (monthsSeen.add(m)) {
                    Text(
                        text = weekStart.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = FatumColors.TextMuted,
                        modifier = Modifier.width(26.dp)
                    )
                } else {
                    Spacer(Modifier.width(26.dp))
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row {
            // Day labels
            Column(modifier = Modifier.padding(top = 4.dp)) {
                listOf("M","W","F").forEachIndexed { idx, label ->
                    val topPad = if (idx == 0) 0.dp else 10.dp
                    Text(label, style = MaterialTheme.typography.labelSmall, color = FatumColors.TextMuted,
                        modifier = Modifier.padding(top = topPad, end = 4.dp).height(12.dp))
                    if (idx < 2) Spacer(Modifier.height(2.dp))
                }
            }
            // 52-week grid
            Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    (0 until 52).forEach { week ->
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            (0 until 7).forEach { dayOfWeek ->
                                val date = startDay.plusDays((week * 7 + dayOfWeek).toLong())
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
                                Box(
                                    modifier = Modifier
                                        .size(11.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(color)
                                        .clickable { onDayClick(key) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Progress bar with label
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun FatumProgressBar(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(FatumColors.SurfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(3.dp))
                .background(
                    Brush.horizontalGradient(listOf(FatumColors.GreenDim, FatumColors.Green))
                )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Chip — used for tag filters, frequency selectors
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun FatumChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg     = if (selected) FatumColors.GreenSurface else FatumColors.SurfaceVariant
    val border = if (selected) FatumColors.GreenBorder   else FatumColors.Border
    val text   = if (selected) FatumColors.Green          else FatumColors.TextSecondary

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = text)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Stat tile — compact number + label used in summaries
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(FatumColors.SurfaceVariant)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = FatumColors.Green)
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = FatumColors.TextMuted)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty state placeholder
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun EmptyState(emoji: String, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(emoji, style = MaterialTheme.typography.displayMedium)
        Text(title, style = MaterialTheme.typography.titleLarge, color = FatumColors.TextPrimary)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = FatumColors.TextMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared TextField color helpers — import these instead of per-screen duplicates
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun fatumOutlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = FatumColors.Green,
    unfocusedBorderColor    = FatumColors.Border,
    focusedTextColor        = FatumColors.TextPrimary,
    unfocusedTextColor      = FatumColors.TextPrimary,
    cursorColor             = FatumColors.Green,
    focusedLabelColor       = FatumColors.Green,
    unfocusedLabelColor     = FatumColors.TextMuted,
    focusedContainerColor   = androidx.compose.ui.graphics.Color.Transparent,
    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent
)
