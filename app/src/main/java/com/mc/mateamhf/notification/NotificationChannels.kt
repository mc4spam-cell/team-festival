package com.mc.mateamhf.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object NotificationChannels {
    const val REMINDERS = "reminders"

    fun ensure(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(REMINDERS) == null) {
            val channel = NotificationChannel(
                REMINDERS,
                "Rappels concerts",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Rappel quelques minutes avant chaque concert P1"
                enableVibration(true)
            }
            nm.createNotificationChannel(channel)
        }
    }
}
