package com.emmyjay256.emmyjay.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.emmyjay256.emmyjay.data.TaskEntity
import com.emmyjay256.emmyjay.repo.TaskRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

class TimelineViewModel(
    private val repo: TaskRepository
) : ViewModel() {

    private val _todayDay = MutableStateFlow(todayAsInt())
    val todayDay: StateFlow<Int> = _todayDay.asStateFlow()

    val tasksToday: StateFlow<List<TaskEntity>> =
        todayDay.flatMapLatest { repo.observeTasksForDay(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // 91 hours = 5460 minutes
    private val goalMinutes = 91L * 60L

    val percentOfGoal: StateFlow<Float> =
        todayDay.flatMapLatest { day ->
            repo.observeCompletedForDay(day).map { completed ->
                val doneMinutes = completed.sumOf { it.durationMinutes() }
                (doneMinutes.toFloat() / goalMinutes.toFloat()).coerceIn(0f, 1f)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0f)

    fun refreshToday() {
        _todayDay.value = todayAsInt()
    }

    fun complete(task: TaskEntity) {
        viewModelScope.launch {
            repo.markCompleted(task)
        }
    }

    private fun todayAsInt(): Int {
        val dow: DayOfWeek = LocalDate.now().dayOfWeek
        return dow.value // 1..7
    }

    class Factory(private val repo: TaskRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TimelineViewModel(repo) as T
        }
    }
}