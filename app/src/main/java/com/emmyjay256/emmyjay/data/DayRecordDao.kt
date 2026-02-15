package com.emmyjay256.emmyjay.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DayRecordDao {

    @Query("SELECT * FROM day_records WHERE dateIso = :dateIso LIMIT 1")
    suspend fun getByDate(dateIso: String): DayRecordEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: DayRecordEntity)
}