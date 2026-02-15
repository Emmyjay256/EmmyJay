package com.emmyjay256.emmyjay.ui

import android.widget.NumberPicker
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.emmyjay256.emmyjay.data.TaskCategory
import com.emmyjay256.emmyjay.data.TaskEntity
import com.emmyjay256.emmyjay.viewmodel.ProgrammerViewModel
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
    var category by remember { mutableStateOf(TaskCategory.Project) }

    var startTime by remember { mutableStateOf(LocalTime.of(8, 0)) }
    var endTime by remember { mutableStateOf(LocalTime.of(9, 0)) }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val canAdd = title.trim().isNotEmpty() && endTime.isAfter(startTime)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Programmer") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { pad ->
        // ✅ One scrollable surface. No “page stuck” issue.
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad),
            contentPadding = PaddingValues(bottom = 18.dp)
        ) {
            item {
                DaySelectorScrollable(
                    selected = selectedDay,
                    onSelect = vm::selectDay
                )
            }

            item { Spacer(Modifier.height(10.dp)) }

            item {
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

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "Time",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TimeButton(
                                label = "Start",
                                time = startTime,
                                modifier = Modifier.weight(1f),
                                onClick = { showStartPicker = true }
                            )
                            TimeButton(
                                label = "End",
                                time = endTime,
                                modifier = Modifier.weight(1f),
                                onClick = { showEndPicker = true }
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        CategoryDropdown(
                            category = category,
                            onCategoryChange = { category = it }
                        )

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val t = title.trim()
                                if (t.isNotEmpty() && endTime.isAfter(startTime)) {
                                    vm.addTask(
                                        title = t,
                                        start = startTime,
                                        end = endTime,
                                        category = category
                                    )
                                    title = ""
                                }
                            },
                            enabled = canAdd,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Add Block")
                        }

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = if (canAdd) "Ready." else "End must be after Start.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(12.dp)) }

            item {
                Text(
                    text = "Blocks",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Spacer(Modifier.height(6.dp))
            }

            // ✅ Blocks are part of the same LazyColumn → page scrolls naturally
            items(items = tasks, key = { it.id }) { task ->
                ProgrammerTaskRow(
                    task = task,
                    onDelete = { vm.delete(task) }
                )
            }
        }

        // ✅ Real spinner wheels (NumberPicker), 3 visible numbers, always snapped.
        if (showStartPicker) {
            SpinnerTimePickerDialog(
                title = "Select Start Time",
                initial = startTime,
                onDismiss = { showStartPicker = false },
                onConfirm = { picked ->
                    startTime = picked
                    // Optional: keep end >= start+1min automatically if user goes past it
                    if (!endTime.isAfter(startTime)) {
                        endTime = startTime.plusMinutes(30)
                    }
                    showStartPicker = false
                }
            )
        }

        if (showEndPicker) {
            SpinnerTimePickerDialog(
                title = "Select End Time",
                initial = endTime,
                onDismiss = { showEndPicker = false },
                onConfirm = { picked ->
                    endTime = picked
                    showEndPicker = false
                }
            )
        }
    }
}

@Composable
private fun DaySelectorScrollable(
    selected: Int,
    onSelect: (Int) -> Unit
) {
    val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

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
private fun TimeButton(
    label: String,
    time: LocalTime,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(time.toString(), style = MaterialTheme.typography.titleMedium)
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
 * REAL spinner wheels:
 * - Hour wheel (0..23)
 * - Minute wheel (0..59)
 * - 3 visible rows
 * - Always snapped (NumberPicker does that natively)
 */
@Composable
private fun SpinnerTimePickerDialog(
    title: String,
    initial: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit
) {
    var hour by remember { mutableIntStateOf(initial.hour) }
    var minute by remember { mutableIntStateOf(initial.minute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NumberPickerWheel(
                    range = 0..23,
                    value = hour,
                    format = { it.toString().padStart(2, '0') },
                    onValueChange = { hour = it }
                )
                Text(":", style = MaterialTheme.typography.titleLarge)
                NumberPickerWheel(
                    range = 0..59,
                    value = minute,
                    format = { it.toString().padStart(2, '0') },
                    onValueChange = { minute = it }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(LocalTime.of(hour, minute)) }) { Text("Set") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun NumberPickerWheel(
    range: IntRange,
    value: Int,
    format: (Int) -> String,
    onValueChange: (Int) -> Unit
) {
    AndroidView(
        factory = { context ->
            NumberPicker(context).apply {
                minValue = range.first
                maxValue = range.last
                wrapSelectorWheel = true

                // ✅ Exactly 3 visible numbers
                displayedValues = (range.first..range.last).map(format).toTypedArray()
                setFormatter { i -> format(i) }

                this.value = value

                setOnValueChangedListener { _, _, newVal ->
                    onValueChange(newVal)
                }
            }
        },
        update = { picker ->
            if (picker.value != value) picker.value = value
        }
    )
}