package com.studiolash.duplicatecleaner.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.studiolash.duplicatecleaner.R
import com.studiolash.duplicatecleaner.detector.PhotoDuplicateDetector
import com.studiolash.duplicatecleaner.ui.DuplicatesActivity
import kotlinx.coroutines.*

class ScanForegroundService : Service() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(FOREGROUND_ID, buildProgressNotification(0, 0))
        scope.launch {
            val detector = PhotoDuplicateDetector(this@ScanForegroundService)
            val result = detector.scan { done, total ->
                updateNotification(done, total)
            }
            // Store result for activity pickup
            DuplicatesActivity.pendingResult = result
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun buildProgressNotification(done: Int, total: Int): android.app.Notification {
        val channelId = "scan_progress"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            channelId, "Scan Progress", NotificationManager.IMPORTANCE_LOW
        )
        nm.createNotificationChannel(channel)

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_photo_duplicate)
            .setContentTitle("Scanning for duplicates…")
            .setOngoing(true)

        if (total > 0) {
            builder.setProgress(total, done, false)
                .setContentText("$done / $total photos scanned")
        } else {
            builder.setProgress(0, 0, true)
                .setContentText("Preparing scan…")
        }

        return builder.build()
    }

    private fun updateNotification(done: Int, total: Int) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(FOREGROUND_ID, buildProgressNotification(done, total))
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val FOREGROUND_ID = 1002
    }
}
