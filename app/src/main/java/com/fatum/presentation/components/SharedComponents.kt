package com.fatum.presentation.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fatum.presentation.theme.FatumColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ─────────────────────────────────────────────────────────────────────────────
// PriorityStars  –  RF-3.5, RF-4.4
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PriorityStars(
    value: Int,
    onValueChange: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        (1..3).forEach { n ->
            val color = when {
                n > value -> FatumColors.Divider
                value == 1 -> FatumColors.Star1
                value == 2 -> FatumColors.Star2
                else       -> FatumColors.Star3
            }
            Icon(
                imageVector = if (n <= value) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = "Prioridad $n",
                tint = color,
                modifier = Modifier
                    .size(16.dp)
                    .then(if (onValueChange != null) Modifier.clickable { onValueChange(n) } else Modifier)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MoodSelector  –  RF-1.3
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun MoodSelector(
    current: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        (1..10).forEach { n ->
            val isSelected = n == current
            val bg = if (isSelected) FatumColors.Accent else FatumColors.SurfaceVariant
            val textColor = if (isSelected) Color.Black else FatumColors.PrimaryVariant
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(bg)
                    .clickable { onSelect(n) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = n.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HeatMapGrid  –  RF-6.1
// 52 columns × 7 rows; intensity driven by count map.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun HeatMapGrid(
    data: Map<String, Int>,
    onDayClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val startDay = today.minusDays(363) // ~52 weeks back, start on the same weekday

    // Build list of 364 dates starting from startDay's week Monday
    val firstMonday = startDay.minusDays(startDay.dayOfWeek.value.toLong() - 1)
    val dates = (0 until 364).map { firstMonday.plusDays(it.toLong()) }

    val maxCount = data.values.maxOrNull()?.toFloat() ?: 1f
    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    Box(modifier = modifier.horizontalScroll(rememberScrollState())) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            // Day labels
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Spacer(Modifier.width(12.dp))
                repeat(52) {
                    Spacer(Modifier.width(12.dp))
                }
            }
            // 7 rows (Mon–Sun)
            (0..6).forEach { dayOfWeek ->
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    dates.filterIndexed { idx, _ -> idx % 7 == dayOfWeek }.forEach { date ->
                        val key = date.format(fmt)
                        val count = data[key] ?: 0
                        val intensity = if (maxCount > 0) count / maxCount else 0f
                        val cellColor = when {
                            count == 0      -> FatumColors.HeatLevel0
                            intensity < 0.25 -> FatumColors.HeatLevel1
                            intensity < 0.50 -> FatumColors.HeatLevel2
                            intensity < 0.75 -> FatumColors.HeatLevel3
                            else             -> FatumColors.HeatLevel4
                        }
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(cellColor)
                                .clickable { onDayClick(key) }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SectionHeader
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = FatumColors.PrimaryVariant,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// FatumCard – consistent surface card
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FatumCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = FatumColors.Surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    } else {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = FatumColors.Surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// StreakBadge  –  RF-2.3
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun StreakBadge(streak: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(FatumColors.SurfaceVariant)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = "🔥", style = MaterialTheme.typography.bodyMedium)
        Text(
            text = "$streak",
            style = MaterialTheme.typography.titleMedium,
            color = FatumColors.AccentSecondary
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LogItem  –  RF-1.4, RF-1.6
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun LogItem(
    content: String,
    timeLabel: String,
    tags: List<String>,
    onTagClick: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    FatumCard(
        modifier = modifier.fillMaxWidth(),
        onClick = { showMenu = true }
    ) {
        Text(
            text = content,
            style = MaterialTheme.typography.bodyLarge,
            color = FatumColors.Primary,
            maxLines = 6,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(timeLabel, style = MaterialTheme.typography.labelSmall, color = FatumColors.PrimaryVariant)
            tags.forEach { tag ->
                Text(
                    text = "#$tag",
                    style = MaterialTheme.typography.labelSmall,
                    color = FatumColors.Accent,
                    modifier = Modifier.clickable { onTagClick(tag) }
                )
            }
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(text = { Text("Editar") }, onClick = { showMenu = false; onEdit() })
            DropdownMenuItem(text = { Text("Eliminar") }, onClick = { showMenu = false; onDelete() })
        }
    }
}
