package com.emmyjay256.emmyjay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emmyjay256.emmyjay.data.TaskCategory
import com.emmyjay256.emmyjay.data.TaskEntity
import com.emmyjay256.emmyjay.viewmodel.ProgrammerViewModel
import kotlinx.coroutines.launch
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgrammerScreen(
    vm: ProgrammerViewModel,
    onBack: () -> Unit
) {
    val selectedDay by vm.selectedDay.collectAsState()
    val tasks by vm.tasksForSelectedDay.collectAsState()

    var title by remember { mutableStateOf("") }

    // Wheel states (default 08:00 -> 09:00)
    var startHour by remember { mutableIntStateOf(8) }
    var startMin by remember { mutableIntStateOf(0) }
    var endHour by remember { mutableIntStateOf(9) }
    var endMin by remember { mutableIntStateOf(0) }

    var category by remember { mutableStateOf(TaskCategory.Project) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Programmer") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
        ) {
            DaySelectorScrollable(
                selected = selectedDay,
                onSelect = vm::selectDay
            )

            Spacer(Modifier.height(10.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "Start / End",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))

                    TimeWheelRow(
                        startHour = startHour,
                        startMin = startMin,
                        endHour = endHour,
                        endMin = endMin,
                        onStartHour = { startHour = it },
                        onStartMin = { startMin = it },
                        onEndHour = { endHour = it },
                        onEndMin = { endMin = it }
                    )

                    Spacer(Modifier.height(12.dp))

                    CategoryDropdown(
                        category = category,
                        onCategoryChange = { category = it }
                    )

                    Spacer(Modifier.height(12.dp))

                    val start = LocalTime.of(startHour, startMin)
                    val end = LocalTime.of(endHour, endMin)
                    val valid = title.trim().isNotEmpty() && end.isAfter(start)

                    Button(
                        onClick = {
                            val t = title.trim()
                            if (t.isNotEmpty() && end.isAfter(start)) {
                                vm.addTask(
                                    title = t,
                                    start = start,
                                    end = end,
                                    category = category
                                )
                                title = ""
                            }
                        },
                        enabled = valid,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Add Block") }

                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (valid) "Locked. No gaps. Execute." else "End time must be after start time.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Blocks",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(0.dp)
            ) {
                items(
                    count = tasks.size,
                    key = { idx -> tasks[idx].id }
                ) { idx ->
                    ProgrammerTaskRow(
                        task = tasks[idx],
                        onDelete = { vm.delete(tasks[idx]) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DaySelectorScrollable(
    selected: Int,
    onSelect: (Int) -> Unit
) {
    val labels = listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun")

    // Scrollable + not squeezed
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 6.dp)
    ) {
        items(7) { idx ->
            val day = idx + 1
            val isSel = (day == selected)
            FilterChip(
                selected = isSel,
                onClick = { onSelect(day) },
                label = { Text(labels[idx]) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    category: TaskCategory,
    onCategoryChange: (TaskCategory) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = category.name,
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            TaskCategory.entries.forEach { c ->
                DropdownMenuItem(
                    text = { Text(c.name) },
                    onClick = {
                        onCategoryChange(c)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ProgrammerTaskRow(task: TaskEntity, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(task.title, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${task.startTime} → ${task.endTime}  •  ${task.category.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onDelete) { Text("Delete") }
        }
    }
}

/**
 * Two time pickers: Start (HH:MM) and End (HH:MM)
 * This is a wheel-scroller feel using LazyColumn lists.
 */
@Composable
private fun TimeWheelRow(
    startHour: Int,
    startMin: Int,
    endHour: Int,
    endMin: Int,
    onStartHour: (Int) -> Unit,
    onStartMin: (Int) -> Unit,
    onEndHour: (Int) -> Unit,
    onEndMin: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            TimeWheelCard(
                label = "Start",
                hour = startHour,
                minute = startMin,
                onHour = onStartHour,
                onMinute = onStartMin
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            TimeWheelCard(
                label = "End",
                hour = endHour,
                minute = endMin,
                onHour = onEndHour,
                onMinute = onEndMin
            )
        }
    }
}

@Composable
private fun TimeWheelCard(
    label: String,
    hour: Int,
    minute: Int,
    onHour: (Int) -> Unit,
    onMinute: (Int) -> Unit
) {
    val hours = remember { (0..23).toList() }
    val minutes = remember { (0..59).toList() } // you can change to step-5 later if you want

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WheelPicker(
                    values = hours,
                    selected = hour,
                    width = 72.dp,
                    onSelected = onHour,
                    format = { it.toString().padStart(2, '0') }
                )
                Text(":", style = MaterialTheme.typography.titleLarge)
                WheelPicker(
                    values = minutes,
                    selected = minute,
                    width = 72.dp,
                    onSelected = onMinute,
                    format = { it.toString().padStart(2, '0') }
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Simple wheel-like picker using LazyColumn.
 * - Shows 5 items vertically
 * - Center item is the selected value
 * - Tap on any visible row to select it
 *
 * No extra dependencies needed.
 */
@Composable
private fun WheelPicker(
    values: List<Int>,
    selected: Int,
    width: Dp,
    onSelected: (Int) -> Unit,
    format: (Int) -> String
) {
    val itemHeight = 38.dp
    val visibleCount = 5
    val sidePaddingCount = visibleCount / 2

    // Add padding items so first/last can center
    val padded: List<Int?> = remember(values) {
        val pad = List(sidePaddingCount) { null }
        pad + values.map { it as Int? } + pad
    }

    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // initial scroll to the selected value (center it)
    LaunchedEffect(values, selected) {
        val idx = values.indexOf(selected).coerceAtLeast(0)
        val target = idx // within "values"
        state.scrollToItem(target) // because we add contentPadding below, this lands nicely
    }

    // whenever user scrolls, pick the centered item
    LaunchedEffect(state.isScrollInProgress) {
        if (!state.isScrollInProgress) {
            val centerIndex = state.firstVisibleItemIndex + sidePaddingCount
            val centerValue = padded.getOrNull(centerIndex)
            if (centerValue != null && centerValue != selected) {
                onSelected(centerValue)
            }
        }
    }

    Box(
        modifier = Modifier
            .width(width)
            .height(itemHeight * visibleCount)
    ) {
        // Selection window (subtle highlight)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(itemHeight)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(10.dp)
                )
        )

        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = itemHeight * sidePaddingCount)
        ) {
            items(padded.size) { idx ->
                val v = padded[idx]
                val isSel = (v != null && v == selected)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .clickable(enabled = v != null) {
                            if (v != null) {
                                onSelected(v)
                                // nudge scroll so the clicked item becomes centered-ish
                                scope.launch {
                                    val target = (idx - sidePaddingCount).coerceAtLeast(0)
                                    state.animateScrollToItem(target)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = v?.let(format) ?: "",
                        style = if (isSel) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                        color = if (isSel) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}