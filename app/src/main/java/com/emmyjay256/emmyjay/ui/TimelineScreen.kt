package com.emmyjay256.emmyjay.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
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

    // Fixed height = 20% of screen
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val blockHeight = remember(screenHeightDp) { (screenHeightDp.dp * 0.20f) }

    // First incomplete index (used for auto-scroll only)
    val nextIndex = remember(tasks) {
        tasks.indexOfFirst { !it.isCompleted }.takeIf { it >= 0 } ?: 0
    }

    LaunchedEffect(nextIndex) {
        if (tasks.isNotEmpty()) listState.animateScrollToItem(nextIndex)
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

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 10.dp,
                    bottom = 14.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    count = tasks.size,
                    key = { idx -> tasks[idx].id }
                ) { idx ->
                    val t = tasks[idx]
                    TimelineBlock(
                        task = t,
                        height = blockHeight,
                        onComplete = {
                            vm.complete(t)
                            scope.launch {
                                val target = tasks.indexOfFirst { !it.isCompleted }
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

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun TimelineBlock(
    task: TaskEntity,
    height: Dp,
    onComplete: () -> Unit
) {
    val dismissState = rememberDismissState(
        confirmStateChange = { value ->
            if (value == DismissValue.DismissedToEnd || value == DismissValue.DismissedToStart) {
                if (!task.isCompleted) onComplete()
                false
            } else true
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
            val alpha = if (task.isCompleted) 0.55f else 1f

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height),
                // ✅ Increased border radius
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "${task.startTime} → ${task.endTime}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = task.category.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.weight(1f))

                    // ✅ Only show Completed (no “Active Block” anymore)
                    if (task.isCompleted) {
                        Text(
                            text = "Completed",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    )
}