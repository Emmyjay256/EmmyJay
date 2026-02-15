package com.emmyjay256.emmyjay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emmyjay256.emmyjay.data.TaskCategory
import com.emmyjay256.emmyjay.data.TaskEntity
import com.emmyjay256.emmyjay.viewmodel.ProgrammerViewModel
import kotlinx.coroutines.launch
import java.time.LocalTime
import kotlin.math.abs

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

            items(items = tasks, key = { it.id }) { task ->
                ProgrammerTaskRow(
                    task = task,
                    onDelete = { vm.delete(task) }
                )
            }
        }

        // ✅ Compose wheel picker (3 visible, snapped, readable colors)
        if (showStartPicker) {
            ComposeTimePickerDialog(
                title = "Select Start Time",
                initial = startTime,
                onDismiss = { showStartPicker = false },
                onConfirm = { picked ->
                    startTime = picked
                    if (!endTime.isAfter(startTime)) endTime = startTime.plusMinutes(30)
                    showStartPicker = false
                }
            )
        }

        if (showEndPicker) {
            ComposeTimePickerDialog(
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
            FilterChip(
                selected = (day == selected),
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
    OutlinedButton(onClick = onClick, modifier = modifier) {
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

/* -------------------- Compose Time Picker Dialog -------------------- */

@Composable
private fun ComposeTimePickerDialog(
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
                SnapWheelPicker(
                    range = (0..23).toList(),
                    initialValue = hour,
                    width = 84.dp,
                    onValueChange = { hour = it },
                    label = { it.toString().padStart(2, '0') }
                )
                Text(":", style = MaterialTheme.typography.titleLarge)
                SnapWheelPicker(
                    range = (0..59).toList(),
                    initialValue = minute,
                    width = 84.dp,
                    onValueChange = { minute = it },
                    label = { it.toString().padStart(2, '0') }
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

/**
 * 3-visible-item snapping wheel picker.
 * - Always centered
 * - Always readable colors (no OEM NumberPicker weirdness, no reflection)
 */
@Composable
private fun SnapWheelPicker(
    range: List<Int>,
    initialValue: Int,
    width: Dp,
    onValueChange: (Int) -> Unit,
    label: (Int) -> String
) {
    val itemHeight = 44.dp
    val visibleCount = 3 // <- what you asked for
    val paddingCount = visibleCount / 2

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val fling = rememberSnapFlingBehavior(lazyListState = listState)

    // Map value -> index
    val initialIndex = remember(range, initialValue) {
        range.indexOf(initialValue).coerceAtLeast(0)
    }

    // Start centered (no “empty middle” issue)
    LaunchedEffect(initialIndex) {
        listState.scrollToItem(initialIndex)
    }

    // Determine centered item based on scroll position
    val centeredIndex by remember {
        derivedStateOf {
            val first = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            // if scrolled beyond half an item, center shifts down
            val shift = if (offset > 22) 1 else 0
            (first + shift).coerceIn(0, range.lastIndex)
        }
    }

    // Push selected value outward whenever center changes (and snap on stop)
    LaunchedEffect(centeredIndex) {
        onValueChange(range[centeredIndex])
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            // After fling ends, hard-snap to the centeredIndex
            scope.launch {
                listState.animateScrollToItem(centeredIndex)
            }
        }
    }

    Box(
        modifier = Modifier
            .width(width)
            .height(itemHeight * visibleCount)
    ) {
        // Center highlight bar
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(itemHeight)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                )
        )

        LazyColumn(
            state = listState,
            flingBehavior = fling,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = itemHeight * paddingCount)
        ) {
            items(range.size) { idx ->
                val v = range[idx]
                val dist = abs(idx - centeredIndex)

                val isCenter = (dist == 0)

                val textColor = if (isCenter) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }

                val textStyle = if (isCenter) {
                    MaterialTheme.typography.titleLarge
                } else {
                    MaterialTheme.typography.titleMedium
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label(v),
                        style = textStyle,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}