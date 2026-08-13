package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cycle_config")
data class CycleConfig(
    @PrimaryKey val id: Int = 1,
    val anchorDate: String, // Format: "yyyy-MM-dd"
    val anchorStage: Int, // 1 to 9
    val anchorPosition: Int, // 1 to 9
    val isActive: Boolean = true,
    val notificationEnabled: Boolean = false,
    val notificationTime: String = "05:00" // Format: "HH:mm"
)

@Entity(tableName = "completed_days")
data class CompletedDay(
    @PrimaryKey val dateString: String // Format: "yyyy-MM-dd"
)
