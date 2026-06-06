package com.mc.mateamhf.ui.timeline

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mc.mateamhf.domain.Concert
import com.mc.mateamhf.domain.ConcertWithState
import com.mc.mateamhf.domain.Priority

@Composable
fun ConcertCard(
    cws: ConcertWithState,
    modifier: Modifier = Modifier,
    friends: List<String> = emptyList(),
    onClick: (Concert) -> Unit,
) {
    val stage = cws.concert.stage
    val borderWidth = when (cws.priority) {
        Priority.P1 -> 4.dp
        Priority.P2 -> 3.dp
        Priority.P3 -> 2.dp
        Priority.NONE -> 0.dp
    }
    val borderColor = when (cws.priority) {
        Priority.P1 -> Color(0xFFB2EBF2) // Material Cyan 100 — pale, lightest
        Priority.P2 -> Color(0xFF00BCD4) // Material Cyan 500 — saturated mid
        Priority.P3 -> Color(0xFF006064) // Material Cyan 900 — darkest teal
        Priority.NONE -> Color.Transparent
    }
    Card(
        modifier = modifier.clickable { onClick(cws.concert) },
        colors = CardDefaults.cardColors(containerColor = stage.color),
        border = if (borderWidth > 0.dp) BorderStroke(borderWidth, borderColor) else null,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (cws.rating != null) {
                    Text(
                        text = cws.rating.label,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        lineHeight = 12.sp,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                }
                Text(
                    text = cws.concert.artist,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    lineHeight = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = formatRange(cws.concert.start, cws.concert.end),
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 9.sp,
                lineHeight = 10.sp,
            )
            if (friends.isNotEmpty()) {
                Text(
                    text = "👥 ${friends.joinToString(", ")}",
                    color = Color.White,
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
    }
}
