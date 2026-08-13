package com.example.viewmodel

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.broadcast.KoeNaWinAlarmReceiver
import com.example.data.*
import com.example.widget.KoeNaWinWidgetProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class KoeNaWinViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = KoeNaWinRepository(db.koeNaWinDao())

    val cycleConfig: StateFlow<CycleConfig?> = repository.cycleConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val completedDays: StateFlow<List<CompletedDay>> = repository.completedDaysFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI state reactive flow
    val uiState: StateFlow<KoeNaWinUiState> = combine(cycleConfig, completedDays) { config, completed ->
        if (config == null || !config.isActive) {
            KoeNaWinUiState(isActive = false)
        } else {
            val todayStr = ChantingEngine.getTodayDateString()
            val todayState = ChantingEngine.calculateDayState(
                config.anchorDate,
                config.anchorStage,
                config.anchorPosition,
                todayStr
            )

            // Start date of the 81-day cycle
            val anchorGlobalDayIndex = (config.anchorStage - 1) * 9 + (config.anchorPosition - 1)
            val startDateStr = ChantingEngine.addDaysToDate(config.anchorDate, -anchorGlobalDayIndex.toLong())

            val completedDateStrings = completed.map { it.dateString }.toSet()

            val calendarDays = (0..80).map { i ->
                val dateStr = ChantingEngine.addDaysToDate(startDateStr, i.toLong())
                val dayState = ChantingEngine.calculateDayState(
                    config.anchorDate,
                    config.anchorStage,
                    config.anchorPosition,
                    dateStr
                ) ?: ChantingDayState(
                    dateStr = dateStr,
                    globalDayIndex = i,
                    globalDay = i + 1,
                    stage = (i / 9) + 1,
                    position = (i % 9) + 1,
                    virtueIndex = 1,
                    virtueName = "",
                    chantCount = 0
                )

                dayState.copy(
                    isCompleted = dateStr in completedDateStrings
                )
            }

            val completedCount = calendarDays.count { it.isCompleted && it.globalDay <= 81 }
            
            // Missed days count encompasses past days that are not completed
            val missedCount = calendarDays.count {
                !it.isCompleted && 
                ChantingEngine.getDaysBetween(it.dateStr, todayStr) > 0 && 
                it.globalDay <= 81
            }

            KoeNaWinUiState(
                isActive = true,
                config = config,
                todayState = todayState,
                calendarDays = calendarDays,
                completedCount = completedCount,
                missedCount = missedCount,
                progressPercent = (completedCount.toFloat() / 81f)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), KoeNaWinUiState())

    fun setupNewCycle(dateStr: String, stage: Int, virtueIndex: Int, markPriorDaysCompleted: Boolean = false) {
        viewModelScope.launch {
            val position = ChantingEngine.findPositionForVirtueInStage(stage, virtueIndex)
            repository.startNewCycle(dateStr, stage, position, markPriorDaysCompleted)
            triggerWidgetAndAlarmUpdate()
        }
    }

    fun toggleDayCompleted(dateStr: String) {
        viewModelScope.launch {
            val completedList = completedDays.value.map { it.dateString }
            if (dateStr in completedList) {
                repository.unmarkDayCompleted(dateStr)
            } else {
                repository.markDayCompleted(dateStr)
            }
            triggerWidgetAndAlarmUpdate()
        }
    }

    fun updateNotificationSettings(enabled: Boolean, time: String) {
        viewModelScope.launch {
            repository.updateNotificationSettings(enabled, time)
            triggerWidgetAndAlarmUpdate()
        }
    }

    fun resetCycle() {
        viewModelScope.launch {
            repository.resetAll()
            triggerWidgetAndAlarmUpdate()
        }
    }

    fun triggerWidgetAndAlarmUpdate() {
        viewModelScope.launch {
            KoeNaWinAlarmReceiver.scheduleAlarms(getApplication())
            
            // Command home screen widgets to refresh
            val context = getApplication<Application>()
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, KoeNaWinWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            
            val updateIntent = Intent(context, KoeNaWinWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds)
            }
            context.sendBroadcast(updateIntent)
        }
    }
}

data class KoeNaWinUiState(
    val isActive: Boolean = false,
    val config: CycleConfig? = null,
    val todayState: ChantingDayState? = null,
    val calendarDays: List<ChantingDayState> = emptyList(),
    val completedCount: Int = 0,
    val missedCount: Int = 0,
    val progressPercent: Float = 0f
)
