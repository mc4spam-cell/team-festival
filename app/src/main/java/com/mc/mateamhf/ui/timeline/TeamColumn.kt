package com.mc.mateamhf.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mc.mateamhf.domain.TeamEvent
import kotlinx.datetime.Instant
import kotlinx.datetime.toLocalDateTime

/** The 7th column sitting right of the 6 stage columns — holds personal team events. */
@Composable
fun TeamColumn(
    events: List<TeamEvent>,
    myUid: String?,
    dayStart: Instant,
    dpPerMinute: Dp,
    width: Dp,
    height: Dp,
    onEventClick: (TeamEvent) -> Unit,
) {
    val tint = Color(0xFF00BCD4) // cyan — distinct from any stage
    Box(
        Modifier
            .width(width)
            .height(height)
            .background(tint.copy(alpha = 0.05f))
            .border(0.5.dp, tint.copy(alpha = 0.25f))
    ) {
        events.forEach { ev ->
            val startMin = (ev.start - dayStart).inWholeMinutes
            val durMin = (ev.end - ev.start).inWholeMinutes
            EventCard(
                event = ev,
                isMine = myUid != null && ev.creatorUid == myUid,
                modifier = Modifier
                    .offset(y = (startMin * dpPerMinute.value).dp)
                    .padding(horizontal = 3.dp, vertical = 1.dp)
                    .height((durMin * dpPerMinute.value).dp)
                    .fillMaxWidth(),
                onClick = { onEventClick(ev) },
            )
        }
    }
}

@Composable
private fun EventCard(
    event: TeamEvent,
    isMine: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val gradient = Brush.linearGradient(
        colors = listOf(Color(0xFF00BCD4), Color(0xFF00838F)),
    )
    Box(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(gradient)
            .border(
                width = if (isMine) 2.dp else 1.dp,
                color = Color.White.copy(alpha = if (isMine) 0.85f else 0.4f),
                shape = RoundedCornerShape(6.dp),
            )
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(
                text = event.title,
                color = Color(0xFF001318),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                lineHeight = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!event.location.isNullOrBlank()) {
                Text(
                    text = "📍 ${event.location}",
                    color = Color(0xFF001318),
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "${formatHm(event.start)}–${formatHm(event.end)} · ${event.creatorName}",
                color = Color(0xFF001318).copy(alpha = 0.85f),
                fontSize = 9.sp,
                lineHeight = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
