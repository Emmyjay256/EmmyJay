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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun TimelineScreen(
    vm: TimelineViewModel,
    onGoProgrammer: () -> Unit
) {
    val active by vm.activeTasksToday.collectAsState()
    val completed by vm.completedTasksToday.collectAsState()
    val pct by vm.percentOfGoal.collectAsState()

    val listState = rememberLazyListState()

    // Fixed height = 20% of screen
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val blockHeight = remember(screenHeightDp) { (screenHeightDp.dp * 0.20f) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // When active changes (after swipe), scroll to top so next one is immediately visible
    LaunchedEffect(active.size) {
        if (active.isNotEmpty()) listState.animateScrollToItem(0)
    }

    fun showUndoSnackbar(
        message: String,
        onUndo: () -> Unit
    ) {
        // kill any previous snackbar (only one undo window at a time)
        snackbarHostState.currentSnackbarData?.dismiss()

        scope.launch {
            val mySnackbarJob = launch {
                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = "UNDO",
                    withDismissAction = false,
                    duration = SnackbarDuration.Indefinite
                )
                if (result == SnackbarResult.ActionPerformed) {
                    onUndo()
                }
            }

            // auto-dismiss after 3 seconds
            launch {
                delay(3000)
                snackbarHostState.currentSnackbarData?.dismiss()
            }

            mySnackbarJob.join()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
                // ACTIVE (top)
                items(
                    count = active.size,
                    key = { idx -> active[idx].id }
                ) { idx ->
                    val t = active[idx]
                    TimelineBlock(
                        task = t,
                        height = blockHeight,
                        onSwipeComplete = {
                            // Only completes if midpoint reached (inside TimelineBlock)
                            vm.complete(t)

                            showUndoSnackbar(
                                message = "Completed: ${t.title}",
                                onUndo = { vm.undoComplete(t) }
                            )
                        }
                    )
                }

                // COMPLETED HEADER + COMPLETED LIST (swipe to revert)
                if (completed.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Completed",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    items(
                        count = completed.size,
                        key = { idx -> completed[idx].id }
                    ) { idx ->
                        val t = completed[idx]
                        RevertibleCompletedBlock(
                            task = t,
                            height = blockHeight,
                            onSwipeRevert = {
                                vm.undoComplete(t)

                                // allow undo of the revert (i.e., re-complete it)
                                showUndoSnackbar(
                                    message = "Reverted: ${t.title}",
                                    onUndo = { vm.complete(t) }
                                )
                            }
                        )
                    }
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
    onSwipeComplete: () -> Unit
) {
    val dismissState = rememberDismissState(
        confirmStateChange = { value ->
            val swiped =
                value == DismissValue.DismissedToEnd || value == DismissValue.DismissedToStart

            if (swiped) {
                if (canCompleteNow(task)) {
                    onSwipeComplete()
                }
                false
            } else true
        }
    )

    SwipeToDismiss(
        state = dismissState,
        directions = setOf(DismissDirection.EndToStart, DismissDirection.StartToEnd),
        background = { Box(modifier = Modifier.fillMaxWidth().height(height)) },
        dismissContent = {
            TaskCard(
                task = task,
                height = height,
                alpha = 1f,
                elevation = 6.dp
            )
        }
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun RevertibleCompletedBlock(
    task: TaskEntity,
    height: Dp,
    onSwipeRevert: () -> Unit
) {
    val dismissState = rememberDismissState(
        confirmStateChange = { value ->
            val swiped =
                value == DismissValue.DismissedToEnd || value == DismissValue.DismissedToStart

            if (swiped) {
                onSwipeRevert()
                false // keep item; list will recompose because state changes
            } else true
        }
    )

    SwipeToDismiss(
        state = dismissState,
        directions = setOf(DismissDirection.EndToStart, DismissDirection.StartToEnd),
        background = { Box(modifier = Modifier.fillMaxWidth().height(height)) },
        dismissContent = {
            TaskCard(
                task = task,
                height = height,
                alpha = 0.55f,
                elevation = 2.dp
            )
        }
    )
}

@Composable
private fun TaskCard(
    task: TaskEntity,
    height: Dp,
    alpha: Float,
    elevation: Dp
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
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
        }
    }
}

private fun canCompleteNow(task: TaskEntity, now: LocalTime = LocalTime.now()): Boolean {
    val start = task.startTime
    val end = task.endTime
    if (!end.isAfter(start)) return true

    val durationMinutes = task.durationMinutes().toInt()
    val halfMinutes = durationMinutes / 2
    val midpoint = start.plusMinutes(halfMinutes.toLong())
    return !now.isBefore(midpoint)
}