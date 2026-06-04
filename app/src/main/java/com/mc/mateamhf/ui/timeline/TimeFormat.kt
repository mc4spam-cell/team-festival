package com.mc.mateamhf.ui.timeline

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

internal val ParisTz: TimeZone = TimeZone.of("Europe/Paris")

internal fun formatHm(instant: Instant): String {
    val ldt = instant.toLocalDateTime(ParisTz)
    return "%02d:%02d".format(ldt.hour, ldt.minute)
}

internal fun formatRange(start: Instant, end: Instant): String =
    "${formatHm(start)} → ${formatHm(end)}"
