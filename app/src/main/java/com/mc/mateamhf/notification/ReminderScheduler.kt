package com.mc.mateamhf.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import com.mc.mateamhf.domain.Concert
import com.mc.mateamhf.ui.timeline.formatHm
import kotlin.time.Duration.Companion.minutes

class ReminderScheduler(private val context: Context) {

    private val alarmManager: AlarmManager? = context.getSystemService()

    fun schedule(concert: Concert) {
        val am = alarmManager ?: return
        val triggerAtMillis = (concert.start - LEAD_TIME).toEpochMilliseconds()
        if (triggerAtMillis <= System.currentTimeMillis()) return

        val pi = pendingIntentFor(concert)
        runCatching {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }.onFailure {
            am.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    fun cancel(concertId: String) {
        val am = alarmManager ?: return
        val intent = baseIntent()
        val existing = PendingIntent.getBroadcast(
            context,
            concertId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (existing != null) {
            am.cancel(existing)
            existing.cancel()
        }
    }

    private fun pendingIntentFor(concert: Concert): PendingIntent {
        val intent = baseIntent().apply {
            putExtra(EXTRA_CONCERT_ID, concert.id)
            putExtra(EXTRA_ARTIST, concert.artist)
            putExtra(EXTRA_STAGE, concert.stage.displayName)
            putExtra(EXTRA_START_HM, formatHm(concert.start))
        }
        return PendingIntent.getBroadcast(
            context,
            concert.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun baseIntent() = Intent(context, ReminderReceiver::class.java).apply {
        action = ACTION_FIRE
    }

    companion object {
        val LEAD_TIME = 15.minutes
        const val ACTION_FIRE = "com.mc.mateamhf.REMINDER_FIRE"
        const val EXTRA_CONCERT_ID = "concert_id"
        const val EXTRA_ARTIST = "artist"
        const val EXTRA_STAGE = "stage"
        const val EXTRA_START_HM = "start_hm"
    }
}
