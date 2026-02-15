package com.emmyjay256.emmyjay.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("""
        SELECT * FROM tasks
        WHERE dayOfWeek = :day
        ORDER BY startTime ASC
    """)
    fun observeTasksForDay(day: Int): Flow<List<TaskEntity>>

    // ✅ used for day-finalization logging
    @Query("""
        SELECT * FROM tasks
        WHERE dayOfWeek = :day
        ORDER BY startTime ASC
    """)
    suspend fun getTasksForDay(day: Int): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TaskEntity?
}