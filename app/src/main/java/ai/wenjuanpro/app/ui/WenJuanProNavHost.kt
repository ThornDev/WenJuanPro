package ai.wenjuanpro.app.ui

import ai.wenjuanpro.app.ui.screens.configlist.ConfigListScreen
import ai.wenjuanpro.app.ui.screens.permission.PermissionScreen
import ai.wenjuanpro.app.ui.screens.scan.ScanScreen
import ai.wenjuanpro.app.ui.screens.welcome.WelcomeConfirmScreen
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import timber.log.Timber

object NavRoutes {
    const val PERMISSION = "permission"
    const val CONFIG_LIST = "configlist"
    const val SCAN = "scan?configId={configId}"
    const val WELCOME = "welcome?studentId={studentId}&configId={configId}"
    const val QUESTION = "question?studentId={studentId}&configId={configId}"
    const val RESUME = "resume?studentId={studentId}&configId={configId}"
}

@Composable
fun WenJuanProNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.PERMISSION,
    ) {
        composable("permission") {
            PermissionScreen(
                onNavigateToConfigList = {
                    navController.navigate("configlist") {
                        popUpTo("permission") { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable("configlist") {
            ConfigListScreen(
                onNavigateToScan = { configId ->
                    navController.navigate("scan?configId=$configId") {
                        launchSingleTop = true
                    }
                },
                onNavigateToDiagnostics = {
                    Timber.d("diagnostics route not wired; Epic 5 will implement")
                },
            )
        }
        composable(
            "scan?configId={configId}",
            arguments =
                listOf(
                    navArgument("configId") {
                        type = NavType.StringType
                        nullable = false
                    },
                ),
        ) {
            ScanScreen(
                onNavigateToWelcome = { studentId, configId ->
                    navController.navigate(
                        "welcome?studentId=$studentId&configId=$configId",
                    ) {
                        launchSingleTop = true
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }
        composable(
            "welcome?studentId={studentId}&configId={configId}",
            arguments =
                listOf(
                    navArgument("studentId") {
                        type = NavType.StringType
                        nullable = false
                    },
                    navArgument("configId") {
                        type = NavType.StringType
                        nullable = false
                    },
                ),
        ) {
            WelcomeConfirmScreen(
                onNavigateToFirstQuestion = { studentId, configId ->
                    navController.navigate(
                        "question?studentId=$studentId&configId=$configId",
                    ) {
                        launchSingleTop = true
                    }
                },
                onNavigateToResume = { studentId, configId ->
                    navController.navigate(
                        "resume?studentId=$studentId&configId=$configId",
                    ) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(
            "question?studentId={studentId}&configId={configId}",
            arguments =
                listOf(
                    navArgument("studentId") { type = NavType.StringType },
                    navArgument("configId") { type = NavType.StringType },
                ),
        ) {
            Text("Story 2.3/2.4 will implement this")
        }
        composable(
            "resume?studentId={studentId}&configId={configId}",
            arguments =
                listOf(
                    navArgument("studentId") { type = NavType.StringType },
                    navArgument("configId") { type = NavType.StringType },
                ),
        ) {
            Text("Story 4.1 will implement this")
        }
    }
}
