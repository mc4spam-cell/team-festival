package com.mc.mateamhf.ui.timeline

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mc.mateamhf.R
import com.mc.mateamhf.domain.Concert
import com.mc.mateamhf.domain.Stage

@Composable
fun DayTimeline(
    day: DayUi,
    friendsByArtist: Map<String, List<String>>,
    onConcertClick: (Concert) -> Unit,
) {
    val dpPerMinute = 1.6.dp
    val rulerWidth = 52.dp
    val stageWidth = 150.dp
    val headerHeight = 36.dp

    val (dayStart, dayEnd) = day.bounds
    val totalMin = (dayEnd - dayStart).inWholeMinutes.toInt()
    val totalHeight = (totalMin * dpPerMinute.value).dp

    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()

    val chromeBg = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)

    Box(Modifier.fillMaxSize().background(Color.Black)) {

        // Fixed background image + dark veil for card readability
        Image(
            painter = painterResource(R.drawable.timeline_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillHeight,
            alignment = Alignment.Center,
            alpha = 0.55f,
        )
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))

        // Stage area (scrolls both axes)
        Row(
            modifier = Modifier
                .padding(start = rulerWidth, top = headerHeight)
                .verticalScroll(vScroll)
                .horizontalScroll(hScroll)
        ) {
            Stage.entries.forEach { stage ->
                StageColumn(
                    stage = stage,
                    concerts = day.concerts.filter { it.concert.stage == stage },
                    dayStart = dayStart,
                    dpPerMinute = dpPerMinute,
                    width = stageWidth,
                    height = totalHeight,
                    friendsByArtist = friendsByArtist,
                    onConcertClick = onConcertClick,
                )
            }
            Spacer(Modifier.width(8.dp))
        }

        // Time ruler — fixed left, scrolls with vertical
        Box(
            modifier = Modifier
                .padding(top = headerHeight)
                .width(rulerWidth)
                .background(chromeBg)
                .verticalScroll(vScroll)
        ) {
            TimeRuler(
                dayStart = dayStart,
                dayEnd = dayEnd,
                dpPerMinute = dpPerMinute,
                width = rulerWidth,
            )
        }

        // Stage headers — fixed top, scrolls with horizontal
        Row(
            modifier = Modifier
                .padding(start = rulerWidth)
                .height(headerHeight)
                .horizontalScroll(hScroll)
                .background(chromeBg)
        ) {
            Stage.entries.forEach { stage ->
                Box(
                    modifier = Modifier
                        .width(stageWidth)
                        .height(headerHeight)
                        .background(stage.color),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stage.displayName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                }
            }
        }

        // Top-left corner cover
        Box(
            modifier = Modifier
                .size(rulerWidth, headerHeight)
                .background(chromeBg)
        )
    }
}
