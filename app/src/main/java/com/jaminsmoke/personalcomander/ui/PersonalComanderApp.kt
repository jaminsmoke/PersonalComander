package com.jaminsmoke.personalcomander.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun PersonalComanderApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "mesas") {
        composable("mesas") {
            MesasScreen(
                onOpenMesa = { mesaId ->
                    navController.navigate("comanda/$mesaId")
                },
                onOpenMenu = {
                    navController.navigate("menu")
                }
            )
        }
        composable("menu") {
            MenuScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = "comanda/{mesaId}",
            arguments = listOf(navArgument("mesaId") { type = NavType.LongType })
        ) { backStackEntry ->
            val mesaId = backStackEntry.arguments?.getLong("mesaId") ?: 0L
            ComandaScreen(
                mesaId = mesaId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
