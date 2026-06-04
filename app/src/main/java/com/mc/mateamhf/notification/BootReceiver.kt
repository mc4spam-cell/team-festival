package com.mc.mateamhf.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mc.mateamhf.MaTeamHFApp
import com.mc.mateamhf.domain.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return

        val app = context.applicationContext as? MaTeamHFApp ?: return
        val pending = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val runningOrder = app.runningOrderRepository.load()
                val concertsById = runningOrder.days
                    .flatMap { it.concerts }
                    .associateBy { it.id }
                val states = app.concertStateRepository.observeStates().first()

                states.values
                    .filter { it.priority == Priority.P1.value }
                    .mapNotNull { concertsById[it.concertId] }
                    .forEach { app.reminderScheduler.schedule(it) }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}
