package com.snehil.cvoptima.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.snehil.cvoptima.mainui.splashscreen.ui.SplashScreen
import com.snehil.cvoptima.mainui.loginscreen.ui.LoginScreen
import com.snehil.cvoptima.mainui.registerscreen.ui.RegisterScreen
import com.snehil.cvoptima.mainui.homescreen.ui.HomeScreen
import com.snehil.cvoptima.mainui.profilescreen.ui.ProfileEditorScreen
import com.snehil.cvoptima.mainui.generator.ui.JobInputScreen
import com.snehil.cvoptima.mainui.generator.ui.StreamingGenerationScreen
import androidx.navigation.navArgument
import androidx.navigation.NavType

@Composable
fun SetupNavGraph(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(navController = navController)
        }
        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }
        composable(Screen.Register.route) {
            RegisterScreen(navController = navController)
        }
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(Screen.ProfileEditor.route) {
            ProfileEditorScreen(navController = navController)
        }
        composable(Screen.JobInput.route) {
            JobInputScreen(navController = navController)
        }
        composable(Screen.Apply.route) {
            JobInputScreen(navController = navController)
        }
        composable(
            route = Screen.Progress.route,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
            StreamingGenerationScreen(navController = navController, taskId = taskId)
        }
    }
}
