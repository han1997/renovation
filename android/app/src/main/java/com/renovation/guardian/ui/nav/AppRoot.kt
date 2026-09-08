package com.renovation.guardian.ui.nav

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
import com.renovation.guardian.ui.AppViewModel
import com.renovation.guardian.ui.components.LoadingState
import com.renovation.guardian.ui.onboarding.OnboardingScreen
import com.renovation.guardian.ui.home.HomeScreen
import com.renovation.guardian.ui.stages.StagesScreen
import com.renovation.guardian.ui.budget.BudgetScreen
import com.renovation.guardian.ui.guide.GuideScreen
import com.renovation.guardian.ui.more.MoreScreen
import com.renovation.guardian.ui.planner.PlannerScreen
import com.renovation.guardian.ui.quote.QuoteScreen
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

/** SAF 导出 / 导入动作，由 [AppRoot] 用 ActivityResultLauncher 实现并向下提供。 */
interface ExportActions {
    fun exportCsv(fileName: String, content: String)
    fun exportJson(fileName: String, content: String)
    fun importJson(onImported: (String) -> Unit)
}

val LocalExportActions = staticCompositionLocalOf<ExportActions> {
    error("ExportActions not provided")
}

@Composable
fun rememberExportActions(): ExportActions {
    val ctx = LocalContext.current
    var pendingCsv by remember { mutableStateOf<String?>(null) }
    var pendingJson by remember { mutableStateOf<String?>(null) }
    var pendingImport by remember { mutableStateOf<((String) -> Unit)?>(null) }

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri: Uri? ->
        uri?.let { pendingCsv?.let { content -> writeText(ctx, it, content) } }
        pendingCsv = null
    }
    val jsonExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        uri?.let { pendingJson?.let { content -> writeText(ctx, it, content) } }
        pendingJson = null
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        uri?.let {
            val text = readText(ctx, it)
            if (text != null) pendingImport?.invoke(text)
        }
        pendingImport = null
    }

    return remember(csvLauncher, jsonExportLauncher, importLauncher) {
        object : ExportActions {
            override fun exportCsv(fileName: String, content: String) {
                pendingCsv = content
                csvLauncher.launch(fileName)
            }

            override fun exportJson(fileName: String, content: String) {
                pendingJson = content
                jsonExportLauncher.launch(fileName)
            }

            override fun importJson(onImported: (String) -> Unit) {
                pendingImport = onImported
                importLauncher.launch(arrayOf("application/json"))
            }
        }
    }
}

private fun writeText(ctx: Context, uri: Uri, text: String) {
    try {
        ctx.contentResolver.openOutputStream(uri)?.use { os ->
            os.write(text.toByteArray(Charsets.UTF_8))
        }
    } catch (_: Throwable) {
        // 用户取消或写入失败：静默忽略（UI 已有 toast/状态反馈）
    }
}

private fun readText(ctx: Context, uri: Uri): String? {
    return try {
        ctx.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
    } catch (_: Throwable) {
        null
    }
}

class RootViewModel(application: android.app.Application) : AppViewModel(application) {
    val hasProfile: Flow<Boolean> = container.houseProfileRepo.observe().map { it != null }
}

@Composable
fun AppRoot() {
    val rootVm: RootViewModel = viewModel()
    val hasProfile by rootVm.hasProfile.collectAsState(initial = null)
    val exportActions = rememberExportActions()

    CompositionLocalProvider(LocalExportActions provides exportActions) {
        when (hasProfile) {
            null -> LoadingState()
            false -> OnboardingScreen(onFinished = {})
            true -> MainTabs()
        }
    }
}

@Composable
private fun MainTabs() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = { BottomNavBar(navController) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelRoute.Home.route,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            composable(TopLevelRoute.Home.route) { HomeScreen() }
            composable(TopLevelRoute.Stages.route) { StagesScreen() }
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
            val selected = currentDestination?.hierarchy?.any { it.route == route.route } == true
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
