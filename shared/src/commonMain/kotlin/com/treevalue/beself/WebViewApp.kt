package com.treevalue.beself

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.treevalue.beself.ui.FunctionPage

@Composable
fun webViewApp() {
    val controller = rememberNavController()
    NavHost(
        navController = controller,
        startDestination = "intercept",
        enterTransition = {
            EnterTransition.None
        },
        exitTransition = {
            ExitTransition.None
        },
    ) {
        composable("main") {
            MainScreen(controller)
        }
        composable("intercept") {
            interceptRequestEnter()
        }
    }
}

@Composable
fun MainScreen(controller: NavController) {
    FunctionPage()
}
