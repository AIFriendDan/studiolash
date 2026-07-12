package com.studiolash.duplicatecleaner.worker

import android.content.Context
import androidx.work.*
import com.studiolash.duplicatecleaner.model.ScheduleSlot
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ScheduleManager {

    fun scheduleAll(context: Context, enabledSlots: Set<ScheduleSlot>) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag(DuplicateScanWorker.WORK_TAG)

        enabledSlots.forEach { slot ->
            val delay = calculateDelayMs(slot.hour, slot.minute)
            val request = PeriodicWorkRequestBuilder<DuplicateScanWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag(DuplicateScanWorker.WORK_TAG)
                .addTag("slot_${slot.name}")
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .build()

            workManager.enqueueUniquePeriodicWork(
                "scan_${slot.name}",
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(DuplicateScanWorker.WORK_TAG)
    }

    fun runNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<DuplicateScanWorker>()
            .addTag(DuplicateScanWorker.WORK_TAG)
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }

    private fun calculateDelayMs(targetHour: Int, targetMinute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }
}
