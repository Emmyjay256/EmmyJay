package com.emmyjay256.emmyjay.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
    var startStr by remember { mutableStateOf("08:00") }
    var endStr by remember { mutableStateOf("09:00") }
    var category by remember { mutableStateOf(TaskCategory.Project) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Programmer") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
        ) {
            DaySelector(
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

                    Spacer(Modifier.height(10.dp))

                    Row(Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = startStr,
                            onValueChange = { startStr = it },
                            label = { Text("Start (HH:MM)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(Modifier.width(10.dp))
                        OutlinedTextField(
                            value = endStr,
                            onValueChange = { endStr = it },
                            label = { Text("End (HH:MM)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    CategoryDropdown(
                        category = category,
                        onCategoryChange = { category = it }
                    )

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val t = title.trim()
                            val start = parseTimeOrNull(startStr)
                            val end = parseTimeOrNull(endStr)
                            if (t.isNotEmpty() && start != null && end != null && end.isAfter(start)) {
                                vm.addTask(t, start, end, category)
                                title = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Add Block") }

                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Tip: Use 24h time. Example: 13:30",
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
private fun DaySelector(selected: Int, onSelect: (Int) -> Unit) {
    val labels = listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        for (i in 1..7) {
            val isSel = (i == selected)
            FilterChip(
                selected = isSel,
                onClick = { onSelect(i) },
                label = { Text(labels[i - 1]) }
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
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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

private fun parseTimeOrNull(raw: String): LocalTime? {
    return try {
        val cleaned = raw.trim()
        LocalTime.parse(if (cleaned.length == 4 && cleaned[1] == ':') "0$cleaned" else cleaned)
    } catch (_: Exception) {
        null
    }
}