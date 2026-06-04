package com.mc.mateamhf.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mc.mateamhf.MainActivity
import com.mc.mateamhf.R

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderScheduler.ACTION_FIRE) return
        val concertId = intent.getStringExtra(ReminderScheduler.EXTRA_CONCERT_ID) ?: return
        val artist = intent.getStringExtra(ReminderScheduler.EXTRA_ARTIST) ?: return
        val stage = intent.getStringExtra(ReminderScheduler.EXTRA_STAGE).orEmpty()
        val startHm = intent.getStringExtra(ReminderScheduler.EXTRA_START_HM).orEmpty()

        NotificationChannels.ensure(context)

        val tap = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val leadMin = ReminderScheduler.LEAD_TIME.inWholeMinutes
        val notification = NotificationCompat.Builder(context, NotificationChannels.REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$artist dans $leadMin min")
            .setContentText("$stage · $startHm")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(tap)
            .build()

        val nm = NotificationManagerCompat.from(context)
        if (nm.areNotificationsEnabled()) {
            nm.notify(concertId.hashCode(), notification)
        }
    }
}
