package com.emmyjay256.emmyjay.repo

import com.emmyjay256.emmyjay.data.TaskDao
import com.emmyjay256.emmyjay.data.TaskEntity
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val dao: TaskDao
) {
    fun observeTasksForDay(day: Int): Flow<List<TaskEntity>> = dao.observeTasksForDay(day)
    fun observeCompletedForDay(day: Int): Flow<List<TaskEntity>> = dao.observeCompletedForDay(day)

    suspend fun upsert(task: TaskEntity): Long = dao.upsert(task)
    suspend fun delete(task: TaskEntity) = dao.delete(task)

    suspend fun markCompleted(task: TaskEntity) {
        dao.update(task.copy(isCompleted = true, completedAtEpochMs = System.currentTimeMillis()))
    }

    suspend fun markIncomplete(task: TaskEntity) {
        dao.update(task.copy(isCompleted = false, completedAtEpochMs = null))
    }
}