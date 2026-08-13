package com.jaminsmoke.personalcomander.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jaminsmoke.personalcomander.ui.components.PcBottomBar
import com.jaminsmoke.personalcomander.ui.components.TopLevelDestination
import com.jaminsmoke.personalcomander.ui.components.isTopLevelRoute
import com.jaminsmoke.personalcomander.ui.sesion.AuthScreen
import com.jaminsmoke.personalcomander.ui.sesion.PerfilScreen

private const val ANIM_DURATION = 350

@Composable
fun PersonalComanderApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = isTopLevelRoute(currentRoute)

    fun navigateTopLevel(dest: TopLevelDestination) {
        navController.navigate(dest.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                PcBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = ::navigateTopLevel,
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.HOME.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(
                route = TopLevelDestination.HOME.route,
                enterTransition = { fadeIn(animationSpec = tween(ANIM_DURATION)) },
                exitTransition = { fadeOut(animationSpec = tween(ANIM_DURATION)) },
            ) {
                HomeScreen(
                    onOpenMesas = { navigateTopLevel(TopLevelDestination.MESAS) },
                    onOpenMenu = { navigateTopLevel(TopLevelDestination.MENU) },
                    onOpenAjustes = { navigateTopLevel(TopLevelDestination.AJUSTES) },
                    onOpenAuth = { navController.navigate("auth") },
                    onOpenPerfil = { navController.navigate("perfil") },
                )
            }
            composable(
                route = TopLevelDestination.MESAS.route,
                enterTransition = { fadeIn(animationSpec = tween(ANIM_DURATION)) },
                exitTransition = { fadeOut(animationSpec = tween(ANIM_DURATION)) },
            ) {
                MesasScreen(
                    onOpenMesa = { mesaId -> navController.navigate("comanda/$mesaId") },
                    onOpenMenu = { navigateTopLevel(TopLevelDestination.MENU) },
                    onBack = null,
                )
            }
            composable(
                route = TopLevelDestination.AJUSTES.route,
                enterTransition = { fadeIn(animationSpec = tween(ANIM_DURATION)) },
                exitTransition = { fadeOut(animationSpec = tween(ANIM_DURATION)) },
            ) {
                AjustesScreen(
                    onBack = null,
                    onOpenAuth = { navController.navigate("auth") },
                    onOpenPerfil = { navController.navigate("perfil") },
                )
            }
            composable(
                route = TopLevelDestination.MENU.route,
                enterTransition = { fadeIn(animationSpec = tween(ANIM_DURATION)) },
                exitTransition = { fadeOut(animationSpec = tween(ANIM_DURATION)) },
            ) {
                MenuScreen(onBack = null)
            }
            composable(
                route = "comanda/{mesaId}",
                arguments = listOf(navArgument("mesaId") { type = NavType.LongType }),
                enterTransition = { slideInHorizontally(tween(ANIM_DURATION)) { it } },
                exitTransition = { slideOutHorizontally(tween(ANIM_DURATION)) { -it } },
            ) { backStackEntry ->
                val mesaId = backStackEntry.arguments?.getLong("mesaId") ?: 0L
                ComandaScreen(
                    mesaId = mesaId,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = "auth",
                enterTransition = { slideInHorizontally(tween(ANIM_DURATION)) { it } },
                exitTransition = { slideOutHorizontally(tween(ANIM_DURATION)) { -it } },
            ) {
                AuthScreen(
                    onBack = { navController.popBackStack() },
                    onAutenticado = { navController.popBackStack() },
                )
            }
            composable(
                route = "perfil",
                enterTransition = { slideInHorizontally(tween(ANIM_DURATION)) { it } },
                exitTransition = { slideOutHorizontally(tween(ANIM_DURATION)) { -it } },
            ) {
                PerfilScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
