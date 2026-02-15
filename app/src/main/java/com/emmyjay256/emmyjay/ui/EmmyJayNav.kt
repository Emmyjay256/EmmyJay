package com.emmyjay256.emmyjay.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.emmyjay256.emmyjay.repo.TaskRepository
import com.emmyjay256.emmyjay.viewmodel.ProgrammerViewModel
import com.emmyjay256.emmyjay.viewmodel.TimelineViewModel

object Routes {
    const val TIMELINE = "timeline"
    const val PROGRAMMER = "programmer"
}

@Composable
fun EmmyJayNav(repo: TaskRepository) {
    val nav = rememberNavController()

    val timelineVm: TimelineViewModel = viewModel(factory = TimelineViewModel.Factory(repo))
    val programmerVm: ProgrammerViewModel = viewModel(factory = ProgrammerViewModel.Factory(repo))

    NavHost(navController = nav, startDestination = Routes.TIMELINE) {
        composable(Routes.TIMELINE) {
            TimelineScreen(
                vm = timelineVm,
                onGoProgrammer = { nav.navigate(Routes.PROGRAMMER) }
            )
        }
        composable(Routes.PROGRAMMER) {
            ProgrammerScreen(
                vm = programmerVm,
                onBack = { nav.popBackStack() }
            )
        }
    }
}