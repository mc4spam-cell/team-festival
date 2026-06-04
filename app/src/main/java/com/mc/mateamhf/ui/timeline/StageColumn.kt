package com.mc.mateamhf.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mc.mateamhf.domain.Concert
import com.mc.mateamhf.domain.ConcertWithState
import com.mc.mateamhf.domain.Stage
import com.mc.mateamhf.domain.artistKey
import kotlinx.datetime.Instant

@Composable
fun StageColumn(
    stage: Stage,
    concerts: List<ConcertWithState>,
    dayStart: Instant,
    dpPerMinute: Dp,
    width: Dp,
    height: Dp,
    friendsByArtist: Map<String, List<String>>,
    onConcertClick: (Concert) -> Unit,
) {
    Box(
        Modifier
            .width(width)
            .height(height)
            .background(stage.color.copy(alpha = 0.06f))
            .border(0.5.dp, stage.color.copy(alpha = 0.25f))
    ) {
        concerts.forEach { cws ->
            val startMin = (cws.concert.start - dayStart).inWholeMinutes
            val durMin = (cws.concert.end - cws.concert.start).inWholeMinutes
            ConcertCard(
                cws = cws,
                modifier = Modifier
                    .offset(y = (startMin * dpPerMinute.value).dp)
                    .padding(horizontal = 3.dp, vertical = 1.dp)
                    .height((durMin * dpPerMinute.value).dp)
                    .fillMaxWidth(),
                friends = friendsByArtist[artistKey(cws.concert.artist)].orEmpty(),
                onClick = onConcertClick,
            )
        }
    }
}
