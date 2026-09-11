package han1997.renovation.ui.nav

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Column
import han1997.renovation.RenovationApp
import han1997.renovation.InitializationState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import han1997.renovation.ui.AppViewModel
import han1997.renovation.ui.components.LoadingState
import han1997.renovation.ui.onboarding.OnboardingScreen
import han1997.renovation.ui.home.HomeScreen
import han1997.renovation.ui.stages.StagesScreen
import han1997.renovation.ui.budget.BudgetScreen
import han1997.renovation.ui.guide.GuideScreen
import han1997.renovation.ui.more.MoreScreen
import han1997.renovation.ui.planner.PlannerScreen
import han1997.renovation.ui.quote.QuoteScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 全屏子路由(非 Tab):逐空间报价计算器与需求规划。 */
const val QuoteRoute = "quote"
const val PlannerRoute = "planner"

/** 顶层 5 个 Tab 路由。 */
sealed class TopLevelRoute(val route: String, val label: String, val icon: ImageVector) {
    object Home : TopLevelRoute("home", "首页", Icons.Filled.Home)
    object Stages : TopLevelRoute("stages", "流程", Icons.Filled.ViewModule)
    object Budget : TopLevelRoute("budget", "预算", Icons.Filled.AccountBalanceWallet)
    object Guide : TopLevelRoute("guide", "指南", Icons.Filled.MenuBook)
    object More : TopLevelRoute("more", "我的", Icons.Filled.Person)

    companion object {
        val entries = listOf(Home, Stages, Budget, Guide, More)
    }
}

class RootViewModel(application: android.app.Application) : AppViewModel(application) {
    val hasProfile: Flow<Boolean> = container.houseProfileRepo.observe().map { it != null }
}

@Composable
fun AppRoot() {
    val rootVm: RootViewModel = viewModel()
    val hasProfile by rootVm.hasProfile.collectAsState(initial = null)
    val app = LocalContext.current.applicationContext as RenovationApp
    val initialization by app.initialization.collectAsState()
    val files: FileActionsViewModel = viewModel()
    val exportActions = rememberExportActions(files)
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(files) {
        files.results.collect { result -> when (result) {
            is FileActionResult.Success -> snackbar.showSnackbar(result.message)
            is FileActionResult.Failure -> snackbar.showSnackbar(result.message)
            FileActionResult.Cancelled -> Unit
        } }
    }
    CompositionLocalProvider(LocalExportActions provides exportActions) {
        Scaffold(snackbarHost = { SnackbarHost(snackbar) }, contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)) { padding ->
            androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().padding(padding)) {
                when (val state = initialization) {
                    InitializationState.Loading -> LoadingState()
                    is InitializationState.Failed -> Column {
                        Text(state.message)
                        Button(onClick = app::retryInitialization) { Text("重试") }
                    }
                    InitializationState.Ready -> when (hasProfile) {
                        null -> LoadingState()
                        false -> OnboardingScreen(onFinished = {})
                        true -> MainTabs()
                    }
                }
            }
        }
    }
}

@Composable
private fun MainTabs() {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = { if (entry?.destination?.route?.substringBefore("?") in TopLevelRoute.entries.map { it.route }) BottomNavBar(navController) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelRoute.Home.route,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            composable(TopLevelRoute.Home.route) { HomeScreen(onOpenStage = { id ->
                navController.navigate("stages?stage=$id") { launchSingleTop = true }
            }, onOpenBudget = { navController.navigate("budget") { launchSingleTop = true } }) }
            composable("stages?stage={stage}", arguments = listOf(androidx.navigation.navArgument("stage") { defaultValue = "" })) { entry ->
                StagesScreen(initialStageId = entry.arguments?.getString("stage")?.takeIf { it.isNotBlank() })
            }
            composable(TopLevelRoute.Budget.route) { BudgetScreen(onOpenQuote = { navController.navigate(QuoteRoute) }) }
            composable(TopLevelRoute.Guide.route) { GuideScreen() }
            composable(TopLevelRoute.More.route) { MoreScreen(onOpenPlanner = { navController.navigate(PlannerRoute) }) }
            composable(QuoteRoute) { QuoteScreen(onBack = { navController.popBackStack() }) }
            composable(PlannerRoute) { PlannerScreen(onBack = { navController.popBackStack() }) }
        }
    }
}

@Composable
private fun BottomNavBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    NavigationBar {
        TopLevelRoute.entries.forEach { route ->
            val selected = currentDestination?.hierarchy?.any { it.route?.substringBefore("?") == route.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(route.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(route.icon, contentDescription = route.label) },
                label = { Text(route.label) },
            )
        }
    }
}
