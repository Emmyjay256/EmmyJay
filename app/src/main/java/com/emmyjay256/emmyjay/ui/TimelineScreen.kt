package com.emmyjay256.emmyjay.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emmyjay256.emmyjay.data.TaskEntity
import com.emmyjay256.emmyjay.viewmodel.TimelineViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun TimelineScreen(
    vm: TimelineViewModel,
    onGoProgrammer: () -> Unit
) {
    val tasks by vm.tasksToday.collectAsState()
    val pct by vm.percentOfGoal.collectAsState()

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Find first incomplete index (the “next block”)
    val nextIndex = remember(tasks) {
        tasks.indexOfFirst { !it.isCompleted }.takeIf { it >= 0 } ?: 0
    }

    LaunchedEffect(nextIndex) {
        // auto-scroll so the next block becomes top/active
        if (tasks.isNotEmpty()) {
            listState.animateScrollToItem(nextIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EmmyJay") },
                actions = {
                    Text(
                        text = "${(pct * 100f).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    TextButton(onClick = onGoProgrammer) { Text("Programmer") }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
        ) {
            LinearProgressIndicator(
                progress = { pct },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )

            // Gapless: no padding, no spacing
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(0.dp)
            ) {
                items(
                    count = tasks.size,
                    key = { idx -> tasks[idx].id }
                ) { idx ->
                    val t = tasks[idx]
                    TimelineBlock(
                        task = t,
                        isActive = (idx == nextIndex && !t.isCompleted),
                        height = heightForTask(t),
                        onComplete = {
                            vm.complete(t)
                            scope.launch {
                                val updated = tasks
                                val target = updated.indexOfFirst { !it.isCompleted }
                                    .let { if (it < 0) 0 else it }
                                listState.animateScrollToItem(target)
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun heightForTask(task: TaskEntity): Dp {
    val dpPerMinute = 2.2f
    val mins = task.durationMinutes().coerceAtLeast(15)
    return (mins * dpPerMinute).dp
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun TimelineBlock(
    task: TaskEntity,
    isActive: Boolean,
    height: Dp,
    onComplete: () -> Unit
) {
    val dismissState = rememberDismissState(
        confirmStateChange = { value ->
            if (value == DismissValue.DismissedToEnd || value == DismissValue.DismissedToStart) {
                if (!task.isCompleted) onComplete()
                false
            } else {
                true
            }
        }
    )

    SwipeToDismiss(
        state = dismissState,
        directions = setOf(DismissDirection.EndToStart, DismissDirection.StartToEnd),
        background = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
            )
        },
        dismissContent = {
            val tonal = if (isActive) 6.dp else 2.dp
            val alpha = if (task.isCompleted) 0.55f else 1f

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height),
                shape = MaterialTheme.shapes.extraSmall,
                elevation = CardDefaults.cardElevation(defaultElevation = tonal),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "${task.startTime} → ${task.endTime}  •  ${task.category.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (task.isCompleted) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Completed",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else if (isActive) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Active Block",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    )
}