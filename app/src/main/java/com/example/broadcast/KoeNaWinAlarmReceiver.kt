package com.example.broadcast

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.AppDatabase
import com.example.data.ChantingEngine
import com.example.data.KoeNaWinRepository
import com.example.widget.KoeNaWinWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class KoeNaWinAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        
        if (action == ACTION_BOOT_COMPLETED || action == ACTION_LOCKED_BOOT_COMPLETED || 
            action == Intent.ACTION_TIME_CHANGED || action == "android.intent.action.TIME_SET" ||
            action == Intent.ACTION_TIMEZONE_CHANGED) {
            
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Update widgets dynamically if the date changed (with SharedPreferences check to save battery)
                    val sharedPrefs = context.getSharedPreferences("koenawin_prefs", Context.MODE_PRIVATE)
                    val todayStr = ChantingEngine.getTodayDateString()
                    val lastUpdateDay = sharedPrefs.getString("last_widget_update_date", "")
                    
                    if (lastUpdateDay != todayStr) {
                        updateAllWidgets(context)
                    }
                    
                    // Always reschedule alarms across system boots or system clock adjustments
                    scheduleAlarms(context)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        } else if (action == ACTION_TRIGGER_DAILY_REMINDER) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    NotificationHelper.showDailyReminderNotification(context)
                    // Reschedule daily reminder for tomorrow
                    scheduleAlarms(context)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        } else if (action == ACTION_TRIGGER_MIDNIGHT_UPDATE) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // It is exactly midnight, refresh the widgets unconditionally
                    updateAllWidgets(context)
                    // Reschedule midnight alarm for tomorrow
                    scheduleAlarms(context)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED"
        const val ACTION_LOCKED_BOOT_COMPLETED = "android.intent.action.LOCKED_BOOT_COMPLETED"
        const val ACTION_TRIGGER_DAILY_REMINDER = "com.example.broadcast.ACTION_TRIGGER_DAILY_REMINDER"
        const val ACTION_TRIGGER_MIDNIGHT_UPDATE = "com.example.broadcast.ACTION_TRIGGER_MIDNIGHT_UPDATE"

        const val REQUEST_CODE_DAILY = 4001
        const val REQUEST_CODE_MIDNIGHT = 4002

        suspend fun updateAllWidgets(context: Context) {
            try {
                val db = AppDatabase.getDatabase(context)
                val repository = KoeNaWinRepository(db.koeNaWinDao())
                val config = repository.getCycleConfig()
                val completed = repository.getCompletedDays().map { it.dateString }

                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisWidget = ComponentName(context, KoeNaWinWidgetProvider::class.java)
                val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

                for (widgetId in allWidgetIds) {
                    KoeNaWinWidgetProvider.updateWidget(context, appWidgetManager, widgetId, config, completed)
                }

                // Track last update date to avoid redundant widget updates and minimize battery usage
                val sharedPrefs = context.getSharedPreferences("koenawin_prefs", Context.MODE_PRIVATE)
                val todayStr = ChantingEngine.getTodayDateString()
                sharedPrefs.edit().putString("last_widget_update_date", todayStr).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        suspend fun scheduleAlarms(context: Context) {
            val db = AppDatabase.getDatabase(context)
            val repository = KoeNaWinRepository(db.koeNaWinDao())
            val config = repository.getCycleConfig() ?: return

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // 1. Cancel previous alarms
            val dailyIntent = Intent(context, KoeNaWinAlarmReceiver::class.java).apply {
                action = ACTION_TRIGGER_DAILY_REMINDER
            }
            val pendingDaily = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_DAILY,
                dailyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingDaily)

            val midnightIntent = Intent(context, KoeNaWinAlarmReceiver::class.java).apply {
                action = ACTION_TRIGGER_MIDNIGHT_UPDATE
            }
            val pendingMidnight = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_MIDNIGHT,
                midnightIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingMidnight)

            if (!config.isActive) return

            // 2. Schedule Midnight Widget Update Alarm (fired daily at 12:00:00 AM)
            val midnightCal = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                
                // If this is in the past (i.e. today's midnight already passed), schedule for tomorrow's midnight
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        midnightCal.timeInMillis,
                        pendingMidnight
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        midnightCal.timeInMillis,
                        pendingMidnight
                    )
                }
            } catch (e: SecurityException) {
                // Ignore fallback or try standard set
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    midnightCal.timeInMillis,
                    pendingMidnight
                )
            }

            // 3. Schedule Daily Chanting Reminder Notification
            if (config.notificationEnabled) {
                val timeParts = config.notificationTime.split(":")
                val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 5
                val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

                val reminderCal = Calendar.getInstance().apply {
                    timeInMillis = System.currentTimeMillis()
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                // If reminder time has already passed today, schedule it for tomorrow
                if (reminderCal.timeInMillis <= System.currentTimeMillis()) {
                    reminderCal.add(Calendar.DAY_OF_YEAR, 1)
                }

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            reminderCal.timeInMillis,
                            pendingDaily
                        )
                    } else {
                        alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            reminderCal.timeInMillis,
                            pendingDaily
                        )
                    }
                } catch (e: SecurityException) {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        reminderCal.timeInMillis,
                        pendingDaily
                    )
                }
            }
        }
    }
}
