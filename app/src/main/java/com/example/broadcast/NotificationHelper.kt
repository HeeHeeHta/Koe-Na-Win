package com.example.broadcast

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.ChantingEngine
import com.example.data.KoeNaWinRepository

object NotificationHelper {

    private const val CHANNEL_ID = "koenawin_notifications_channel"
    private const val CHANNEL_NAME = "နေ့စဉ်ပုတီးစိပ်ရန်သတိပေးချက်"
    private const val CHANNEL_DESC = "ကိုးနဝင်းပုတီးစိပ်နည်း နေ့စဉ်သတိပေးချက်များ"
    const val NOTIFICATION_ID = 8181

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    suspend fun showDailyReminderNotification(context: Context) {
        val db = AppDatabase.getDatabase(context)
        val repository = KoeNaWinRepository(db.koeNaWinDao())
        val config = repository.getCycleConfig() ?: return

        if (!config.isActive || !config.notificationEnabled) return

        val todayStr = ChantingEngine.getTodayDateString()
        val stateToday = ChantingEngine.calculateDayState(
            config.anchorDate,
            config.anchorStage,
            config.anchorPosition,
            todayStr
        )

        if (stateToday == null || stateToday.isNotStarted || stateToday.isCycleFinished) return

        // Create intent to open main activity
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        createNotificationChannel(context)

        // Title and content in elegant Burmese
        val title = "📿 ယနေ့ စိပ်ရန် ကိုးနဝင်းပုတီးစိပ်ရက် (${stateToday.globalDay}/81)"
        var content = "${stateToday.virtueName} — ${stateToday.chantCount} ပတ် စိပ်ရန်ကျန်ပါသေးသည်"
        if (stateToday.position == 5) {
            content += " | 🥗 သတ်သတ်လွတ်စားရမည့်ရက်"
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Use custom drawable if available, otherwise fallback to standard system drawable or standard push launcher icons
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // Safe fallback icon
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}
