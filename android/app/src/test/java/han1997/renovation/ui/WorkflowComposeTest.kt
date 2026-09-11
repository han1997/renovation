package han1997.renovation.ui

import han1997.renovation.ui.theme.RenovationTheme
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import han1997.renovation.testing.PolishFixture
import han1997.renovation.ui.quote.QuoteScreen
import han1997.renovation.ui.quote.QuoteViewModel
import han1997.renovation.ui.planner.PlannerScreen
import han1997.renovation.ui.planner.PlannerViewModel
import han1997.renovation.domain.planner.PlannerRules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WorkflowComposeTest {
    @get:Rule val compose = createComposeRule()
    @Test fun quoteCanCompleteTheWizardSaveAndReopen() {
        PolishFixture().use { f ->
            runBlocking { f.initialize() }
            val vm = QuoteViewModel(f.app)
            val store = ViewModelStore().apply { put("quote", vm) }
            try {
                compose.setContent { RenovationTheme(dynamicColor = false) { QuoteScreen(vm = vm) } }
                compose.onNodeWithText("新建报价方案").performScrollTo().assertIsDisplayed().performClick()
                compose.waitUntil(10000) { compose.onAllNodesWithText("房屋信息").fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithText("下一步").performClick()
                compose.onNodeWithText("下一步").assertIsNotEnabled()
                compose.onNodeWithText("添加空间").performClick()
                compose.onNodeWithText("下一步").performClick()
                compose.onNodeWithText("墙面 材料").performClick()
                compose.onNodeWithText("乳胶漆").performClick()
                compose.onNodeWithText("生成完整清单").performClick()
                screenshot("quote")
                compose.onNodeWithText("保存方案").assertIsDisplayed().performClick()
                compose.onNodeWithText("方案名称").performTextReplacement("测试报价")
                compose.onNodeWithText("保存", useUnmergedTree = true).performClick()
                compose.waitUntil(10000) { compose.onAllNodesWithText("方案名称").fetchSemanticsNodes().isEmpty() }
                compose.onNodeWithContentDescription("返回").performClick()
                compose.waitUntil(10000) { compose.onAllNodesWithText("测试报价").fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithText("测试报价").assertIsDisplayed().performClick()
                compose.waitUntil(10000) { compose.onAllNodesWithText("报价清单").fetchSemanticsNodes().isNotEmpty() }
                assertEquals("测试报价", vm.planName)
                assertTrue(vm.result!!.estimatedTotalCents > 0)
                // 同一组配置切为半包，重新走完向导，管理费应消失。
                compose.onNodeWithText("修改配置").assertIsDisplayed().performClick()
                compose.onNodeWithText("报价方式").performClick()
                compose.onNodeWithText("半包").performClick()
                compose.waitUntil(10000) { compose.onAllNodesWithText("房屋信息").fetchSemanticsNodes().isNotEmpty() }
                screenshot("semi-before-next")
                compose.onNodeWithText("下一步").assertIsEnabled().performClick()
                compose.waitUntil(10000) { compose.onAllNodesWithText("划分空间").fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithText("下一步").assertIsEnabled().performClick()
                compose.waitUntil(10000) { compose.onAllNodesWithText("正在配置的空间").fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithText("生成完整清单").assertIsEnabled().performClick()
                compose.onNodeWithText("保存方案").assertIsDisplayed()
                compose.runOnIdle { assertTrue(vm.result!!.lines.none { it.partLabel == "管理费" }) }
            } finally { store.clear() }
        }
    }
    @Test fun plannerSearchSelectionAndAssignmentAreConnected() {
        PolishFixture().use { f ->
            runBlocking { f.initialize() }
            val vm = PlannerViewModel(f.app)
            val store = ViewModelStore().apply { put("planner", vm) }
            try {
                compose.setContent { RenovationTheme(dynamicColor = false) { PlannerScreen(vm = vm) } }
                compose.waitUntil(10000) { !vm.loading }
                compose.onNodeWithText("搜索需求名称或类型").performTextInput("插座")
                val pick = PlannerRules.search(f.cache.decoboxRequirements!!, "插座").first()
                compose.onNodeWithText(pick.itemName).performClick()
                assertEquals(1, vm.state.picked.size)
                compose.onNodeWithText("下一步").performClick()
                compose.onNodeWithText("添加预设空间").performClick()
                compose.onNodeWithText("下一步").performClick()
                compose.onNodeWithText(vm.state.rooms.single().name).performClick()
                compose.onNodeWithText("下一步").performClick()
                screenshot("planner")
                compose.onNodeWithText("复制文本").performClick()
                assertEquals(1, vm.state.assignments.size)
                compose.waitUntil(10000) { !vm.saving }
                assertNotNull(runBlocking { f.db.plannerStateDao().get() })
            } finally { store.clear() }
        }
    }
    @Test fun partialModeCanGenerateAQuoteFromActualWorkInputs() {
        PolishFixture().use { f ->
            runBlocking { f.initialize() }
            val vm = QuoteViewModel(f.app)
            val store = ViewModelStore().apply { put("quote", vm) }
            try {
                compose.setContent { RenovationTheme(dynamicColor = false) { QuoteScreen(vm = vm) } }
                compose.onNodeWithText("新建报价方案").performScrollTo().performClick()
                compose.waitUntil(10000) { compose.onAllNodesWithText("房屋信息").fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithText("报价方式").performClick()
                compose.onNodeWithText("局改").performClick()
                compose.onNodeWithText("下一步").performClick()
                val wall = f.cache.decoboxCatalog!!.partialItems.first { it.id == "wall-refresh" }
                compose.onNodeWithText(wall.label).performScrollTo().performClick()
                compose.onNodeWithText("下一步").performClick()
                compose.onNodeWithText("成品保护").performScrollTo().performClick()
                compose.onNodeWithText("生成完整清单").performClick()
                compose.onNodeWithText("保存方案").assertIsDisplayed()
                compose.runOnIdle {
                    assertEquals(han1997.renovation.domain.quote.QuoteMode.PARTIAL, vm.state.mode)
                    assertTrue(vm.result!!.totalCents > 0)
                    assertTrue(vm.result!!.lines.any { it.partLabel == "局改费用" })
                }
                screenshot("partial-quote")
            } finally { store.clear() }
        }
    }

    private fun screenshot(name: String) {
        val file = java.io.File("build/reports/ui-polish/$name.png")
        requireNotNull(file.parentFile).mkdirs()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

}
