package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface KoeNaWinDao {
    @Query("SELECT * FROM cycle_config WHERE id = 1")
    fun getCycleConfigFlow(): Flow<CycleConfig?>

    @Query("SELECT * FROM cycle_config WHERE id = 1")
    suspend fun getCycleConfig(): CycleConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCycleConfig(config: CycleConfig)

    @Update
    suspend fun updateCycleConfig(config: CycleConfig)

    @Query("DELETE FROM cycle_config WHERE id = 1")
    suspend fun deleteCycleConfig()

    @Query("SELECT * FROM completed_days")
    fun getCompletedDaysFlow(): Flow<List<CompletedDay>>

    @Query("SELECT * FROM completed_days")
    suspend fun getCompletedDays(): List<CompletedDay>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletedDay(completedDay: CompletedDay)

    @Delete
    suspend fun deleteCompletedDay(completedDay: CompletedDay)

    @Query("DELETE FROM completed_days")
    suspend fun clearCompletedDays()
}
