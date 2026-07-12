package com.studiolash.duplicatecleaner.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.studiolash.duplicatecleaner.R
import com.studiolash.duplicatecleaner.detector.PhotoDuplicateDetector
import com.studiolash.duplicatecleaner.ui.DuplicatesActivity

class DuplicateScanWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val detector = PhotoDuplicateDetector(context)
            val result = detector.scan()

            if (result.duplicateGroups.isNotEmpty()) {
                showDuplicatesFoundNotification(
                    result.totalDuplicates,
                    result.totalWastedBytes
                )
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun showDuplicatesFoundNotification(count: Int, wastedBytes: Long) {
        val channelId = "duplicate_scan"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            "Duplicate Photo Scanner",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Alerts when duplicate photos are found"
        }
        nm.createNotificationChannel(channel)

        val intent = Intent(context, DuplicatesActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val wastedMb = wastedBytes / (1024 * 1024)
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_photo_duplicate)
            .setContentTitle("Duplicate Photos Found")
            .setContentText("$count duplicate(s) found — ${wastedMb}MB wasted")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("We found $count duplicate photo(s) taking up ${wastedMb}MB. Tap to review and clean up.")
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_delete,
                "Delete All Duplicates",
                buildDeleteAllIntent()
            )
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun buildDeleteAllIntent(): PendingIntent {
        val intent = Intent(context, DuplicatesActivity::class.java).apply {
            putExtra(DuplicatesActivity.EXTRA_AUTO_DELETE, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            context, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val WORK_TAG = "duplicate_scan"
    }
}
