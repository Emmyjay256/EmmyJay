package com.emmyjay256.emmyjay.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.emmyjay256.emmyjay.data.TaskCategory
import com.emmyjay256.emmyjay.data.TaskEntity
import com.emmyjay256.emmyjay.repo.TaskRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalTime

class ProgrammerViewModel(
    private val repo: TaskRepository
) : ViewModel() {

    private val _selectedDay = MutableStateFlow(1) // Mon default
    val selectedDay: StateFlow<Int> = _selectedDay.asStateFlow()

    val tasksForSelectedDay: StateFlow<List<TaskEntity>> =
        selectedDay.flatMapLatest { repo.observeTasksForDay(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectDay(day: Int) {
        _selectedDay.value = day
    }

    fun addTask(
        title: String,
        start: LocalTime,
        end: LocalTime,
        category: TaskCategory
    ) {
        viewModelScope.launch {
            repo.upsert(
                TaskEntity(
                    title = title.trim(),
                    dayOfWeek = selectedDay.value,
                    startTime = start,
                    endTime = end,
                    category = category
                )
            )
        }
    }

    fun delete(task: TaskEntity) {
        viewModelScope.launch { repo.delete(task) }
    }

    class Factory(private val repo: TaskRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProgrammerViewModel(repo) as T
        }
    }
}