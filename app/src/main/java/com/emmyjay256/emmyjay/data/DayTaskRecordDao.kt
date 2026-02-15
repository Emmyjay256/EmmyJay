package com.emmyjay256.emmyjay.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DayTaskRecordDao {

    @Query("SELECT COUNT(*) FROM day_task_records WHERE dateIso = :dateIso")
    suspend fun countForDate(dateIso: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(records: List<DayTaskRecordEntity>)
}