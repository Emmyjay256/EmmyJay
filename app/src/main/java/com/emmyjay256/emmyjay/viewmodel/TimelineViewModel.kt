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

    private val todayIso: String
        get() = LocalDate.now().toString()

    private val tasksRaw: StateFlow<List<TaskEntity>> =
        todayDay.flatMapLatest { repo.observeTasksForDay(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeTasksToday: StateFlow<List<TaskEntity>> =
        tasksRaw.map { list -> list.filter { it.lastCompletedDate != todayIso } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val completedTasksToday: StateFlow<List<TaskEntity>> =
        tasksRaw.map { list -> list.filter { it.lastCompletedDate == todayIso } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Percent based ONLY on blocks visible today:
     * doneMinutes / totalMinutes (for today's schedule)
     */
    val percentOfGoal: StateFlow<Float> =
        tasksRaw.map { allToday ->
            val totalMinutes = allToday.sumOf { it.durationMinutes() }.coerceAtLeast(1L)
            val doneMinutes = allToday
                .filter { it.lastCompletedDate == todayIso }
                .sumOf { it.durationMinutes() }

            (doneMinutes.toFloat() / totalMinutes.toFloat()).coerceIn(0f, 1f)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0f)

    fun refreshToday() {
        _todayDay.value = todayAsInt()
    }

    fun complete(task: TaskEntity) {
        viewModelScope.launch {
            repo.markCompletedToday(task, todayIso)
        }
    }

    private fun todayAsInt(): Int {
        val dow: DayOfWeek = LocalDate.now().dayOfWeek
        return dow.value
    }

    class Factory(private val repo: TaskRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TimelineViewModel(repo) as T
        }
    }
}