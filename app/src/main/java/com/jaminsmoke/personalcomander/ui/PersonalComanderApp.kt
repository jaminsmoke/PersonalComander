package com.jaminsmoke.personalcomander.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jaminsmoke.personalcomander.PersonalComanderApp as App
import com.jaminsmoke.personalcomander.ui.components.PcBottomBar
import com.jaminsmoke.personalcomander.ui.components.TopLevelDestination
import com.jaminsmoke.personalcomander.ui.components.isTopLevelRoute
import com.jaminsmoke.personalcomander.ui.gestion.GestionAcceso
import com.jaminsmoke.personalcomander.ui.gestion.GestionScreen
import com.jaminsmoke.personalcomander.ui.sesion.AuthScreen
import com.jaminsmoke.personalcomander.ui.sesion.PerfilScreen

private const val ANIM_DURATION = 350

@Composable
fun PersonalComanderApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = isTopLevelRoute(currentRoute)

    fun navigateMenu(abrir: String = GestionAcceso.NAV_HUB) {
        val route = if (abrir == GestionAcceso.NAV_HUB) {
            TopLevelDestination.MENU.route
        } else {
            "${TopLevelDestination.MENU.route}?abrir=$abrir"
        }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = abrir == GestionAcceso.NAV_HUB
        }
    }

    fun navigateAjustes(abrir: String = AjustesAcceso.NAV_HUB) {
        val route = if (abrir == AjustesAcceso.NAV_HUB) {
            TopLevelDestination.AJUSTES.route
        } else {
            "${TopLevelDestination.AJUSTES.route}?abrir=$abrir"
        }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = abrir == AjustesAcceso.NAV_HUB
        }
    }

    fun navigateTopLevel(dest: TopLevelDestination) {
        if (dest == TopLevelDestination.MENU) {
            navigateMenu(GestionAcceso.NAV_HUB)
            return
        }
        if (dest == TopLevelDestination.AJUSTES) {
            navigateAjustes(AjustesAcceso.NAV_HUB)
            return
        }
        navController.navigate(dest.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val app = LocalContext.current.applicationContext as App
    LaunchedEffect(app) {
        app.recoger.avisos.collect { aviso ->
            snackbarHostState.showSnackbar(aviso.texto)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    onOpenAjustes = { navigateAjustes(AjustesAcceso.TURNO.navKey) },
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
                    onBack = null,
                )
            }
            composable(
                route = "${TopLevelDestination.AJUSTES.route}?abrir={abrir}",
                arguments = listOf(
                    navArgument("abrir") {
                        type = NavType.StringType
                        defaultValue = AjustesAcceso.NAV_HUB
                    },
                ),
                enterTransition = { fadeIn(animationSpec = tween(ANIM_DURATION)) },
                exitTransition = { fadeOut(animationSpec = tween(ANIM_DURATION)) },
            ) { entry ->
                AjustesScreen(
                    abrir = entry.arguments?.getString("abrir"),
                    onOpenAuth = { navController.navigate("auth") },
                    onOpenPerfil = { navController.navigate("perfil") },
                )
            }
            composable(
                route = "${TopLevelDestination.MENU.route}?abrir={abrir}",
                arguments = listOf(
                    navArgument("abrir") {
                        type = NavType.StringType
                        defaultValue = GestionAcceso.NAV_HUB
                    },
                ),
                enterTransition = { fadeIn(animationSpec = tween(ANIM_DURATION)) },
                exitTransition = { fadeOut(animationSpec = tween(ANIM_DURATION)) },
            ) { entry ->
                GestionScreen(
                    abrir = entry.arguments?.getString("abrir"),
                    onOpenAjustes = { navigateAjustes(AjustesAcceso.TURNO.navKey) },
                    onOpenAuth = { navController.navigate("auth") },
                    onOpenPerfil = { navController.navigate("perfil") },
                )
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
