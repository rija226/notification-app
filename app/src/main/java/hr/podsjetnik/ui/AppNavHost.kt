package hr.podsjetnik.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import hr.podsjetnik.R
import hr.podsjetnik.ui.danas.DanasScreen
import hr.podsjetnik.ui.detalji.DetaljiScreen
import hr.podsjetnik.ui.dijagnostika.DijagnostikaScreen
import hr.podsjetnik.ui.onboarding.OnboardingScreen
import hr.podsjetnik.ui.onboarding.OnboardingViewModel
import hr.podsjetnik.ui.postavke.PostavkeScreen
import hr.podsjetnik.ui.svizadaci.SviZadaciScreen
import hr.podsjetnik.ui.theme.LocalPodsjetnikColors
import hr.podsjetnik.ui.uredi.UrediScreen
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

// Routes (HANDOFF §6).
@Serializable
object DanasRoute

@Serializable
object SviZadaciRoute

@Serializable
object PostavkeRoute

@Serializable
object DijagnostikaRoute

@Serializable
data class UrediRoute(val taskId: Long? = null)

@Serializable
data class DetaljiRoute(val taskId: Long)

private data class TopLevel(val route: Any, val type: KClass<*>, @StringRes val label: Int, @DrawableRes val icon: Int)

private val topLevels = listOf(
    TopLevel(DanasRoute, DanasRoute::class, R.string.nav_danas, R.drawable.ic_event_available),
    TopLevel(SviZadaciRoute, SviZadaciRoute::class, R.string.nav_svi_zadaci, R.drawable.ic_list),
    TopLevel(PostavkeRoute, PostavkeRoute::class, R.string.nav_postavke, R.drawable.ic_tune),
)

/** First launch shows onboarding until it's completed (HANDOFF §6.8), then the app. */
@Composable
fun AppNavHost(openTaskId: Long?, onOpenTaskHandled: () -> Unit) {
    val onboarding: OnboardingViewModel = viewModel(factory = OnboardingViewModel.Factory)
    val completed by onboarding.completed.collectAsStateWithLifecycle()
    val colors = LocalPodsjetnikColors.current
    when (completed) {
        null -> Box(Modifier.fillMaxSize().background(colors.bg))
        false -> Box(Modifier.fillMaxSize().background(colors.bg).safeDrawingPadding()) { OnboardingScreen(onboarding) }
        true -> MainNavHost(openTaskId, onOpenTaskHandled)
    }
}

@Composable
private fun MainNavHost(openTaskId: Long?, onOpenTaskHandled: () -> Unit) {
    val colors = LocalPodsjetnikColors.current
    val navController = rememberNavController()
    LaunchedEffect(openTaskId) {
        if (openTaskId != null) {
            navController.navigate(DetaljiRoute(openTaskId))
            onOpenTaskHandled()
        }
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val showBottomBar = topLevels.any { top -> destination?.hierarchy?.any { it.hasRoute(top.type) } == true }
    val openNewTask = { navController.navigate(UrediRoute()) }
    val openTask = { id: Long -> navController.navigate(DetaljiRoute(id)) }

    Scaffold(
        containerColor = colors.bg,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = colors.nav) {
                    topLevels.forEach { top ->
                        NavigationBarItem(
                            selected = destination?.hierarchy?.any { it.hasRoute(top.type) } == true,
                            onClick = { navController.navigateTopLevel(top.route) },
                            icon = { Icon(painterResource(top.icon), contentDescription = null) },
                            label = { Text(stringResource(top.label)) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = colors.navInd),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = DanasRoute,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            composable<DanasRoute> {
                DanasScreen(
                    onNewTask = openNewTask,
                    onOpenTask = openTask,
                    onOpenSettings = { navController.navigateTopLevel(PostavkeRoute) },
                )
            }
            composable<SviZadaciRoute> { SviZadaciScreen(onNewTask = openNewTask, onOpenTask = openTask) }
            composable<DetaljiRoute> {
                DetaljiScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(UrediRoute(it)) },
                )
            }
            composable<PostavkeRoute> { PostavkeScreen(onOpenDiagnostics = { navController.navigate(DijagnostikaRoute) }) }
            composable<DijagnostikaRoute> { DijagnostikaScreen(onBack = { navController.popBackStack() }) }
            composable<UrediRoute> { UrediScreen(onClose = { navController.popBackStack() }) }
        }
    }
}

/** Bottom-bar navigation: one copy of each tab, state kept per tab. */
private fun NavHostController.navigateTopLevel(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
