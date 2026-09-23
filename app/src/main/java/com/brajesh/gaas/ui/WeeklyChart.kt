package com.brajesh.gaas.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.brajesh.gaas.data.MacroGoal
import com.brajesh.gaas.repository.DaySlice
import kotlin.math.roundToInt

enum class MacroFilter(val label: String) {
    CALORIES("Calories"),
    PROTEIN("Protein"),
    CARBS("Carbs"),
    FAT("Fat")
}

/** Distinct color per macro so the selector chips read off the bars. */
private val FatOrange = Color(0xFFF57C00)

private fun DaySlice.valueFor(filter: MacroFilter): Double = when (filter) {
    MacroFilter.CALORIES -> calories
    MacroFilter.PROTEIN -> proteinG
    MacroFilter.CARBS -> carbsG
    MacroFilter.FAT -> fatG
}

private fun MacroGoal?.goalValue(filter: MacroFilter): Double? = when (filter) {
    MacroFilter.CALORIES -> this?.calories
    MacroFilter.PROTEIN -> this?.proteinG
    MacroFilter.CARBS -> this?.carbsG
    MacroFilter.FAT -> this?.fatG
}

/**
 * Last-7-days chart. Pure Canvas against a dashed goal line — no charting
 * dependency for what is bar heights, a goal line, and a few labels.
 * Sparse history is handled explicitly: days with no logged meal draw as a
 * dashed outline slot ("—") rather than a scary zero-height bar, while a
 * genuinely logged 0 kcal day gets a normal (near-flat) bar labeled "0".
 */
@Composable
fun WeeklyChart(
    slices: List<DaySlice>,
    goal: MacroGoal?,
    modifier: Modifier = Modifier
) {
    var selected by remember { mutableStateOf(MacroFilter.CALORIES) }
    val hasAnyData = slices.any { it.hasData }
    val hasGaps = slices.any { !it.hasData }
    val barColor = when (selected) {
        MacroFilter.CALORIES -> MaterialTheme.colorScheme.primary
        MacroFilter.PROTEIN -> MaterialTheme.colorScheme.secondary
        MacroFilter.CARBS -> MaterialTheme.colorScheme.tertiary
        MacroFilter.FAT -> FatOrange
    }

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Last 7 days", style = MaterialTheme.typography.titleMedium)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MacroFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = selected == filter,
                        onClick = { selected = filter },
                        label = { Text(filter.label) }
                    )
                }
            }
            WeeklyBarChart(
                slices = slices,
                filter = selected,
                goalValue = goal.goalValue(selected),
                barColor = barColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
            when {
                !hasAnyData -> Text(
                    "No meals logged in the last 7 days — bars fill in as you log.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                hasGaps -> Text(
                    "Dashed slots are days with no meal logged (not zero — just nothing on record).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun WeeklyBarChart(
    slices: List<DaySlice>,
    filter: MacroFilter,
    goalValue: Double?,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val values = slices.map { it.valueFor(filter) }
    val rawMax = values.maxOrNull() ?: 0.0
    // Scale to the larger of data or goal so the goal line always has a defined position.
    val scaleTop = listOf(rawMax, goalValue ?: 0.0).max().coerceAtLeast(1.0)
    val unit = if (filter == MacroFilter.CALORIES) "kcal" else "g"
    val labelFontSize = MaterialTheme.typography.labelSmall.fontSize
    val dataLabelStyle = TextStyle(fontSize = labelFontSize, color = barColor)
    val mutedLabelStyle = TextStyle(fontSize = labelFontSize, color = MaterialTheme.colorScheme.outline)
    val outlineColor = MaterialTheme.colorScheme.outline
    val outlineVariantColor = MaterialTheme.colorScheme.outlineVariant
    val errorColor = MaterialTheme.colorScheme.error

    Column(modifier = modifier) {
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            val plotBottom = size.height
            val slotWidth = size.width / slices.size.coerceAtLeast(1)
            val barWidth = (slotWidth * 0.5f).coerceAtMost(36.dp.toPx())

            slices.forEachIndexed { i, slice ->
                val cx = slotWidth * i + slotWidth / 2

                if (slice.hasData) {
                    val v = slice.valueFor(filter)
                    val barHeight = ((v / scaleTop) * plotBottom).toFloat()
                    val top = plotBottom - barHeight
                    val over = goalValue != null && goalValue > 0 && v > goalValue
                    drawRect(
                        color = if (over) errorColor else barColor,
                        topLeft = Offset(cx - barWidth / 2, top),
                        size = Size(barWidth, barHeight)
                    )
                    val label = v.roundToInt().toString()
                    val layout = textMeasurer.measure(label, dataLabelStyle)
                    val lx = (cx - layout.size.width / 2f)
                        .coerceAtLeast(0f)
                        .coerceAtMost((size.width - layout.size.width).coerceAtLeast(0f))
                    val ly = (top - layout.size.height - 3.dp.toPx()).coerceAtLeast(2f)
                    drawText(layout, topLeft = Offset(lx, ly))
                } else {
                    // No meals logged that day: hollow dashed slot, not a zero bar.
                    val barHeight = plotBottom * 0.35f
                    val top = plotBottom - barHeight
                    drawRect(
                        color = outlineVariantColor,
                        topLeft = Offset(cx - barWidth / 2, top),
                        size = Size(barWidth, barHeight),
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                        ),
                        alpha = 0.9f
                    )
                    val layout = textMeasurer.measure("—", mutedLabelStyle)
                    val lx = (cx - layout.size.width / 2f)
                        .coerceAtLeast(0f)
                        .coerceAtMost((size.width - layout.size.width).coerceAtLeast(0f))
                    drawText(layout, topLeft = Offset(lx, (top - layout.size.height - 3.dp.toPx()).coerceAtLeast(2f)))
                }
            }

            // Baseline for the bars.
            drawLine(
                color = outlineVariantColor,
                start = Offset(0f, plotBottom),
                end = Offset(size.width, plotBottom),
                strokeWidth = 1.dp.toPx()
            )

            // Dashed goal line + its label.
            if (goalValue != null && goalValue > 0) {
                val gy = (plotBottom - (goalValue / scaleTop) * plotBottom)
                    .toFloat()
                    .coerceIn(0f, plotBottom)
                drawLine(
                    color = outlineColor,
                    start = Offset(0f, gy),
                    end = Offset(size.width, gy),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                )
                val gLabel = "goal ${goalValue.roundToInt()} $unit"
                val layout = textMeasurer.measure(gLabel, mutedLabelStyle)
                drawText(
                    layout,
                    topLeft = Offset(
                        (size.width - layout.size.width).coerceAtLeast(0f),
                        (gy - layout.size.height - 2.dp.toPx()).coerceAtLeast(2f)
                    )
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            slices.forEach { slice ->
                Text(
                    formatWeekday(slice.dayKey),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}