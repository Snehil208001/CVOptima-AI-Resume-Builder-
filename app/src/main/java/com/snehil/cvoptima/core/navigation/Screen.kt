package com.snehil.cvoptima.core.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object ProfileEditor : Screen("profile_editor")
    object JobInput : Screen("job_input")
    object Apply : Screen("apply")
    object Progress : Screen("progress/{taskId}") {
        fun createRoute(taskId: String) = "progress/$taskId"
    }
}