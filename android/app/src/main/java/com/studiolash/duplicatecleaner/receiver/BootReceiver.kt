package com.studiolash.duplicatecleaner.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.studiolash.duplicatecleaner.model.ScheduleSlot
import com.studiolash.duplicatecleaner.worker.ScheduleManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("schedule_prefs", Context.MODE_PRIVATE)
            val enabledSlots = ScheduleSlot.entries
                .filter { prefs.getBoolean("slot_${it.name}", it == ScheduleSlot.MORNING) }
                .toSet()
            if (enabledSlots.isNotEmpty()) {
                ScheduleManager.scheduleAll(context, enabledSlots)
            }
        }
    }
}
