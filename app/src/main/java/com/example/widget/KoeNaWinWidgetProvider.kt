package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.R
import com.example.data.AppDatabase
import com.example.data.ChantingEngine
import com.example.data.CycleConfig
import com.example.data.KoeNaWinRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class KoeNaWinWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val repository = KoeNaWinRepository(db.koeNaWinDao())
                val config = repository.getCycleConfig()
                val completed = repository.getCompletedDays().map { it.dateString }

                for (appWidgetId in appWidgetIds) {
                    updateWidget(context, appWidgetManager, appWidgetId, config, completed)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_DONE_TODAY) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val repository = KoeNaWinRepository(db.koeNaWinDao())
                    val config = repository.getCycleConfig()

                    if (config != null && config.isActive) {
                        val todayStr = ChantingEngine.getTodayDateString()
                        val completedDays = repository.getCompletedDays().map { it.dateString }

                        // Toggle completed status for today
                        if (todayStr in completedDays) {
                            repository.unmarkDayCompleted(todayStr)
                        } else {
                            repository.markDayCompleted(todayStr)
                        }

                        // Trigger widget update
                        val appWidgetManager = AppWidgetManager.getInstance(context)
                        val thisWidget = ComponentName(context, KoeNaWinWidgetProvider::class.java)
                        val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
                        
                        // Force update
                        val updatedConfig = repository.getCycleConfig()
                        val updatedCompleted = repository.getCompletedDays().map { it.dateString }
                        for (widgetId in allWidgetIds) {
                            updateWidget(context, appWidgetManager, widgetId, updatedConfig, updatedCompleted)
                        }

                        // Also notify main app if it is active (indirectly via flows, but we can also broadcast)
                        val updateIntent = Intent(ACTION_DATA_CHANGED)
                        context.sendBroadcast(updateIntent)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_DONE_TODAY = "com.example.widget.ACTION_DONE_TODAY"
        const val ACTION_DATA_CHANGED = "com.example.widget.ACTION_DATA_CHANGED"

        suspend fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            config: CycleConfig?,
            completedDays: List<String>
        ) {
            val views = RemoteViews(context.packageName, R.layout.koenawin_widget)

            if (config == null || !config.isActive) {
                // If not active, show setup instructions
                views.setTextViewText(R.id.widget_title, "📿 ကိုးနဝင်းပုတီးစိပ်နည်း")
                views.setTextViewText(R.id.widget_day_progress, "စတင်ရန်")
                views.setTextViewText(R.id.widget_virtue_name, "ပုတီးစိပ်ဝက်ဂျက်")
                views.setTextViewText(R.id.widget_chant_info, "စတင်ရန် အက်ပ်ထဲဝင်ပါ")
                views.setViewVisibility(R.id.widget_vegetarian_notice, View.GONE)
                views.setTextViewText(R.id.widget_weekday, "")
                views.setTextViewText(R.id.widget_tomorrow_preview, "စက်တင်မှုလုပ်ရန် အက်ပ်ထဲသို့ ဝင်ရောက်ပါ")
                views.setTextViewText(R.id.widget_action_button, "ဖွင့်ပါ")

                // Open App intent
                val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_action_button, pendingIntent)
                appWidgetManager.updateAppWidget(appWidgetId, views)
                return
            }

            // Active Cycle (Real calculations)
            val todayStr = ChantingEngine.getTodayDateString()
            val stateToday = ChantingEngine.calculateDayState(
                config.anchorDate,
                config.anchorStage,
                config.anchorPosition,
                todayStr
            )

            if (stateToday == null) {
                appWidgetManager.updateAppWidget(appWidgetId, views)
                return
            }

            val brmWeekday = ChantingEngine.getBurmeseWeekday(todayStr)
            val engWeekday = ChantingEngine.getEnglishWeekday(todayStr)

            // Calculate Tomorrow
            val tomorrowStr = ChantingEngine.addDaysToDate(todayStr, 1)
            val stateTomorrow = ChantingEngine.calculateDayState(
                config.anchorDate,
                config.anchorStage,
                config.anchorPosition,
                tomorrowStr
            )

            // Setup Details
            val isTodayDone = todayStr in completedDays
            
            views.setTextViewText(R.id.widget_title, "📿 ကိုးနဝင်းပုတီးစိပ်ရက်")

            if (stateToday.isCycleFinished) {
                views.setTextViewText(R.id.widget_day_progress, "81/81 ပြီးဆုံးပါပြီ")
                views.setTextViewText(R.id.widget_virtue_name, "ကိုးနဝင်း အောင်မြင်ပါပြီ")
                views.setTextViewText(R.id.widget_chant_info, "စိပ်စရာမရှိတော့ပါ")
                views.setViewVisibility(R.id.widget_chant_info, View.GONE)
                views.setViewVisibility(R.id.widget_vegetarian_notice, View.GONE)
                views.setTextViewText(R.id.widget_weekday, brmWeekday)
                views.setTextViewText(R.id.widget_tomorrow_preview, "ကိုးနဝင်း ၈၁ ရက် အောင်မြင်စွာ ပြီးဆုံးပါပြီ။ သာဓု သာဓု သာဓု။")
                views.setViewVisibility(R.id.widget_action_button, View.GONE)
            } else if (stateToday.isNotStarted) {
                views.setTextViewText(R.id.widget_day_progress, "စတင်ရန်ဖြစ်သည်")
                views.setTextViewText(R.id.widget_virtue_name, "စတင်မည့်ရက် စောင့်နေပါသည်")
                views.setViewVisibility(R.id.widget_chant_info, View.GONE)
                views.setViewVisibility(R.id.widget_vegetarian_notice, View.GONE)
                views.setTextViewText(R.id.widget_tomorrow_preview, "")
                views.setViewVisibility(R.id.widget_action_button, View.GONE)
            } else {
                // Happy path: active chanting cycle day
                views.setViewVisibility(R.id.widget_action_button, View.VISIBLE)
                views.setTextViewText(R.id.widget_day_progress, "Day ${stateToday.globalDay} / 81")
                views.setTextViewText(R.id.widget_virtue_name, stateToday.virtueName)
                views.setViewVisibility(R.id.widget_chant_info, View.VISIBLE)
                views.setTextViewText(R.id.widget_chant_info, "${stateToday.chantCount} ပတ်စိပ်ရန်  |  $brmWeekday ($engWeekday)")
                views.setTextViewText(R.id.widget_weekday, "")

                // Vegetarian Day Rule (Position = 5)
                if (stateToday.position == 5) {
                    views.setViewVisibility(R.id.widget_vegetarian_notice, View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.widget_vegetarian_notice, View.GONE)
                }

                // Tomorrow Preview description
                if (stateTomorrow != null && !stateTomorrow.isCycleFinished) {
                    views.setTextViewText(
                        R.id.widget_tomorrow_preview,
                        "မနက်ဖြန်: ${stateTomorrow.virtueName} — ${stateTomorrow.chantCount} ပတ်"
                    )
                } else if (stateTomorrow != null && stateTomorrow.isCycleFinished) {
                    views.setTextViewText(R.id.widget_tomorrow_preview, "မနက်ဖြန်: ၈၁ ရက်မြောက် ကိုးနဝင်း အောင်မြင်ပါပြီ!")
                } else {
                    views.setTextViewText(R.id.widget_tomorrow_preview, "")
                }

                // Done state styling
                if (isTodayDone) {
                    views.setTextViewText(R.id.widget_action_button, "✓ ပြီးပါပြီ")
                    views.setInt(R.id.widget_action_button, "setBackgroundResource", R.drawable.widget_button_completed_bg)
                } else {
                    views.setTextViewText(R.id.widget_action_button, "✓ ပြီးပြီ")
                    views.setInt(R.id.widget_action_button, "setBackgroundResource", R.drawable.widget_button_bg)
                }

                // Wire up Done button to custom receiver broadcast intent
                val doneIntent = Intent(context, KoeNaWinWidgetProvider::class.java).apply {
                    action = ACTION_DONE_TODAY
                }
                val pendingDoneIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId,
                    doneIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_action_button, pendingDoneIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
