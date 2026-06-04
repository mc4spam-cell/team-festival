package com.mc.mateamhf.ui.timeline

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Instant
import kotlinx.datetime.toLocalDateTime

@Composable
fun TimeRuler(
    dayStart: Instant,
    dayEnd: Instant,
    dpPerMinute: Dp,
    width: Dp,
) {
    val totalMin = (dayEnd - dayStart).inWholeMinutes.toInt()
    val totalHeight = (totalMin * dpPerMinute.value).dp

    val startLdt = dayStart.toLocalDateTime(ParisTz)
    val firstMarkOffset = when {
        startLdt.minute == 0 || startLdt.minute == 30 -> 0
        startLdt.minute < 30 -> 30 - startLdt.minute
        else -> 60 - startLdt.minute
    }

    Box(
        Modifier
            .width(width)
            .height(totalHeight)
    ) {
        var m = firstMarkOffset
        while (m <= totalMin) {
            val instant = dayStart + m.minutes
            val ldt = instant.toLocalDateTime(ParisTz)
            val isHour = ldt.minute == 0
            Text(
                text = "%02d:%02d".format(ldt.hour, ldt.minute),
                modifier = Modifier
                    .offset(y = (m * dpPerMinute.value).dp - 8.dp)
                    .fillMaxWidth()
                    .padding(end = 6.dp),
                fontSize = if (isHour) 12.sp else 10.sp,
                fontWeight = if (isHour) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isHour) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
            )
            m += 30
        }
    }
}
