package com.jaminsmoke.personalcomander.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

private const val ANIM_DURATION = 350

@Composable
fun PersonalComanderApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable(
            route = "home",
            enterTransition = { fadeIn(animationSpec = tween(ANIM_DURATION)) },
            exitTransition = { fadeOut(animationSpec = tween(ANIM_DURATION)) }
        ) {
            HomeScreen(
                onOpenMesas = { navController.navigate("mesas") },
                onOpenMenu = { navController.navigate("menu") },
                onOpenAjustes = { navController.navigate("ajustes") }
            )
        }
        composable(
            route = "mesas",
            enterTransition = { slideInHorizontally(tween(ANIM_DURATION)) { it } },
            exitTransition = { slideOutHorizontally(tween(ANIM_DURATION)) { -it } }
        ) {
            MesasScreen(
                onOpenMesa = { mesaId -> navController.navigate("comanda/$mesaId") },
                onOpenMenu = { navController.navigate("menu") },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "ajustes",
            enterTransition = { slideInHorizontally(tween(ANIM_DURATION)) { it } },
            exitTransition = { slideOutHorizontally(tween(ANIM_DURATION)) { -it } }
        ) {
            AjustesScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = "menu",
            enterTransition = { slideInHorizontally(tween(ANIM_DURATION)) { it } },
            exitTransition = { slideOutHorizontally(tween(ANIM_DURATION)) { -it } }
        ) {
            MenuScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = "comanda/{mesaId}",
            arguments = listOf(navArgument("mesaId") { type = NavType.LongType }),
            enterTransition = { slideInHorizontally(tween(ANIM_DURATION)) { it } },
            exitTransition = { slideOutHorizontally(tween(ANIM_DURATION)) { -it } }
        ) { backStackEntry ->
            val mesaId = backStackEntry.arguments?.getLong("mesaId") ?: 0L
            ComandaScreen(
                mesaId = mesaId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
