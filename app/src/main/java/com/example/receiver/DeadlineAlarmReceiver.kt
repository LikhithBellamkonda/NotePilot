package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

class DeadlineAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val itemId = intent.getIntExtra("itemId", 0)
        val itemTitle = intent.getStringExtra("itemTitle") ?: "Deadline Alert"
        val deadlineText = intent.getStringExtra("deadlineText") ?: "An important item is due!"
        val isWarning = intent.getBooleanExtra("isWarning", false)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "deadline_alerts_channel"

        // Handle Quick Action: Mark Completed from notification drawer
        if (intent.action == "com.example.ACTION_COMPLETE_TASK") {
            if (itemId != 0) {
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        val db = AppDatabase.getDatabase(context.applicationContext)
                        val dao = db.vaultDao()
                        val item = dao.getItemById(itemId)
                        if (item != null) {
                            val updated = item.copy(
                                isCompleted = true,
                                completedTimestamp = System.currentTimeMillis()
                            )
                            dao.updateItem(updated)
                            
                            // Cancel system alarm
                            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                            val cancelIntent = Intent(context, DeadlineAlarmReceiver::class.java)
                            
                            val piMain = PendingIntent.getBroadcast(
                                context, itemId, cancelIntent,
                                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                            )
                            piMain?.cancel()
                            
                            val piWarn = PendingIntent.getBroadcast(
                                context, itemId + 100000, cancelIntent,
                                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                            )
                            piWarn?.cancel()
                            
                            // Dismiss notifications
                            notificationManager.cancel(itemId)          // Main notification
                            notificationManager.cancel(itemId + 100000) // Warning notification
                            Log.d("DeadlineReceiver", "Task completed and notifications/alarms dismissed from action button.")
                        }
                    } catch (e: Exception) {
                        Log.e("DeadlineReceiver", "Error completing task from broadcast action", e)
                    }
                }
            }
            return
        }

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "NotePilot Deadline Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Fired when dynamic task or shared message deadlines are reached"
                enableVibration(true)
                enableLights(true)
                val audioAttributes = android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .build()
                setSound(soundUri, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            if (isWarning) itemId + 100000 else itemId,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Mark complete action button
        val completeActionIntent = Intent(context, DeadlineAlarmReceiver::class.java).apply {
            action = "com.example.ACTION_COMPLETE_TASK"
            putExtra("itemId", itemId)
        }
        val completeActionPendingIntent = PendingIntent.getBroadcast(
            context,
            itemId + 200000,
            completeActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(soundUri)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .clearActions()
            .addAction(
                android.R.drawable.checkbox_on_background,
                "Task Completed",
                completeActionPendingIntent
            )

        if (isWarning) {
            notificationBuilder
                .setContentTitle("⚠️ 2-HOUR WARNING: $itemTitle")
                .setContentText("This critical deadline is due in 2 hours!")
                .setOngoing(true) // Non-removable/ongoing notification
                .setAutoCancel(false)
            
            notificationManager.notify(itemId + 100000, notificationBuilder.build())
        } else {
            notificationBuilder
                .setContentTitle("⏰ DEADLINE DUE: $itemTitle")
                .setContentText(deadlineText)
                .setAutoCancel(true)
            
            notificationManager.notify(itemId, notificationBuilder.build())
            
            // Play continuous Alarm ringtone directly (sound for 8 seconds) to guarantee audio ringing
            try {
                val ringtone = RingtoneManager.getRingtone(context, soundUri)
                ringtone?.play()
                GlobalScope.launch(Dispatchers.Main) {
                    kotlinx.coroutines.delay(8000)
                    if (ringtone != null && ringtone.isPlaying) {
                        try {
                            ringtone.stop()
                        } catch (ignored: Exception) {}
                    }
                }
            } catch (e: Exception) {
                Log.e("DeadlineReceiver", "Direct ringtone play failed", e)
            }
        }
    }
}
