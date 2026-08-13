package com.example.data

import kotlinx.coroutines.flow.Flow

class KoeNaWinRepository(private val dao: KoeNaWinDao) {

    val cycleConfigFlow: Flow<CycleConfig?> = dao.getCycleConfigFlow()
    val completedDaysFlow: Flow<List<CompletedDay>> = dao.getCompletedDaysFlow()

    suspend fun getCycleConfig(): CycleConfig? = dao.getCycleConfig()

    suspend fun getCompletedDays(): List<CompletedDay> = dao.getCompletedDays()

    suspend fun startNewCycle(anchorDate: String, stage: Int, position: Int, markPriorDaysCompleted: Boolean = false) {
        val config = CycleConfig(
            id = 1,
            anchorDate = anchorDate,
            anchorStage = stage,
            anchorPosition = position,
            isActive = true
        )
        dao.insertCycleConfig(config)
        dao.clearCompletedDays()

        if (markPriorDaysCompleted) {
            val anchorGlobalDayIndex = (stage - 1) * 9 + (position - 1)
            if (anchorGlobalDayIndex > 0) {
                val startDateStr = ChantingEngine.addDaysToDate(anchorDate, -anchorGlobalDayIndex.toLong())
                for (i in 0 until anchorGlobalDayIndex) {
                    val dateStr = ChantingEngine.addDaysToDate(startDateStr, i.toLong())
                    dao.insertCompletedDay(CompletedDay(dateStr))
                }
            }
        }
    }

    suspend fun updateNotificationSettings(enabled: Boolean, time: String) {
        val current = dao.getCycleConfig()
        if (current != null) {
            dao.updateCycleConfig(current.copy(notificationEnabled = enabled, notificationTime = time))
        }
    }

    suspend fun markDayCompleted(dateString: String) {
        dao.insertCompletedDay(CompletedDay(dateString))
    }

    suspend fun unmarkDayCompleted(dateString: String) {
        dao.deleteCompletedDay(CompletedDay(dateString))
    }

    suspend fun resetAll() {
        dao.deleteCycleConfig()
        dao.clearCompletedDays()
    }
}
