package com.brajesh.gaas.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * A small progress ring for one macro: consumed vs goal, with distinct visuals
 * for an over-goal day (error color + "+N" center) and an unset goal ("–").
 * The consumed side labels the ring, the subtitle shows what's left.
 */
@Composable
fun MacroRing(
    label: String,
    consumed: Double,
    goal: Double,
    unit: String,
    modifier: Modifier = Modifier
) {
    val hasGoal = goal > 0
    val over = hasGoal && consumed > goal
    val targetFraction = if (hasGoal) (consumed / goal).coerceIn(0.0, 1.0) else 0.0
    val accent = when {
        over -> MaterialTheme.colorScheme.error
        hasGoal -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    // Fill the ring from empty each time the value (or goal) changes, so logging
    // a meal reads as the ring visibly growing rather than snapping to the new
    // fill. Keyed on the target so a new meal restarts the sweep from the old cap.
    val animatedFraction = remember { Animatable(0f) }
    LaunchedEffect(targetFraction) {
        animatedFraction.animateTo(
            targetValue = targetFraction.toFloat(),
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }
    val fraction = animatedFraction.value

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val strokeWidth = 6.dp.toPx()
                val inset = strokeWidth / 2
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val arcTopLeft = Offset(inset, inset)
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(strokeWidth)
                )
                if (fraction > 0f) {
                    drawArc(
                        color = accent,
                        startAngle = -90f,
                        sweepAngle = 360f * fraction,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        // Butt cap on a full ring so the seam doesn't overlap.
                        style = Stroke(strokeWidth, cap = if (fraction >= 1f) StrokeCap.Butt else StrokeCap.Round)
                    )
                }
            }
            Text(
                text = when {
                    !hasGoal -> "–"
                    over -> "+${(consumed - goal).roundToInt()}"
                    else -> consumed.roundToInt().toString()
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = when {
                !hasGoal -> "no goal"
                over -> "over by ${(consumed - goal).roundToInt()} $unit"
                else -> "${(goal - consumed).roundToInt()} $unit left"
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
    }
}