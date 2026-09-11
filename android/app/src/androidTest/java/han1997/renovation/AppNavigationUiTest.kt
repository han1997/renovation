package han1997.renovation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import han1997.renovation.data.repo.AppContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppNavigationUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val container: AppContainer
        get() = (ApplicationProvider.getApplicationContext<RenovationApp>()).container

    @Test
    fun firstLaunch_showsOnboarding() {
        runBlocking { container.clearAllData() }
        composeRule.waitUntil(15000) { composeRule.onAllNodesWithText("共 3 步", substring = true).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("共 3 步", substring = true).assertIsDisplayed()
    }

    @Test
    fun afterOnboarding_showsHome() {
        runBlocking {
        container.clearAllData()
        container.houseProfileRepo.finishOnboarding(
            areaM2 = 90.0,
            tierId = "t2",
            modeId = "clear",
            gradeId = "mid",
            startDate = null,
            totalBudgetYuan = 50000.0,
            selectedPresetIds = emptyList(),
            customSpaces = emptyList(),
        )
        }
        composeRule.waitUntil(15000) { composeRule.onAllNodesWithText("装修进行时").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("装修进行时").assertIsDisplayed()
    }
    @Test
    fun childWorkflowsHideTabsAndReturnToTheOriginalTab() {
        runBlocking {
            container.seeder.seedIfEmpty("2026-09-10")
            container.clearAllData()
            container.houseProfileRepo.finishOnboardingCents(90.0, "t2", "half", "mid", null, 20000000L, emptyList(), emptyList())
        }
        composeRule.waitUntil(15000) { composeRule.onAllNodesWithText("装修进行时").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNode(hasText("预算") and hasClickAction()).performClick()
        composeRule.onNodeWithText("逐空间报价").performScrollTo().performClick()
        composeRule.onNodeWithText("新建报价方案").assertIsDisplayed()
        composeRule.onNode(hasText("首页") and hasClickAction()).assertDoesNotExist()
        composeRule.onNodeWithContentDescription("返回").performClick()
        composeRule.onNode(hasText("预算") and hasClickAction()).assertIsDisplayed()
        composeRule.onNode(hasText("我的") and hasClickAction()).performClick()
        composeRule.onNodeWithText("需求规划").performScrollTo().performClick()
        composeRule.onNode(hasText("首页") and hasClickAction()).assertDoesNotExist()
        composeRule.onNodeWithContentDescription("返回").performClick()
        composeRule.onNode(hasText("我的") and hasClickAction()).assertIsDisplayed()
    }

    @Test
    fun fileProviderAllowsReadingTheGeneratedImage() {
        val context = ApplicationProvider.getApplicationContext<RenovationApp>()
        val bitmap = android.graphics.Bitmap.createBitmap(16, 16, android.graphics.Bitmap.Config.ARGB_8888)
        var gallery: android.net.Uri? = null
        try {
            val uri = han1997.renovation.util.ImageShareUtil.cacheBitmap(context, bitmap, "instrumented-${java.util.UUID.randomUUID()}")
            org.junit.Assert.assertEquals("image/png", context.contentResolver.getType(uri))
            context.contentResolver.openInputStream(uri).use { input ->
                org.junit.Assert.assertNotNull(input)
                org.junit.Assert.assertEquals(137, input!!.read()) // PNG 文件头
            }
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                gallery = han1997.renovation.util.ImageShareUtil.saveToGallery(context, bitmap, "validation-${java.util.UUID.randomUUID()}")
                context.contentResolver.openInputStream(gallery!!).use { input -> org.junit.Assert.assertEquals(137, input!!.read()) }
            }
        } finally {
            gallery?.let { context.contentResolver.delete(it, null, null) }
            bitmap.recycle()
        }
    }

    @Test
    fun documentExportClosesTheRealProviderStreamBeforeSuccess() {
        val context = ApplicationProvider.getApplicationContext<RenovationApp>()
        val vm = han1997.renovation.ui.nav.FileActionsViewModel(context, androidx.lifecycle.SavedStateHandle())
        val store = androidx.lifecycle.ViewModelStore().apply { put("export", vm) }
        val directory = java.io.File(context.cacheDir, "exports").apply { mkdirs() }
        val file = java.io.File(directory, "validation-${java.util.UUID.randomUUID()}.json")
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            composeRule.runOnIdle { vm.export("json", file.name, "精确金额 0.29") }
            runBlocking { kotlinx.coroutines.withTimeout(10000) { vm.requests.first() } }
            composeRule.runOnIdle { vm.finish(uri) }
            val result = runBlocking { kotlinx.coroutines.withTimeout(10000) { vm.results.first() } }
            org.junit.Assert.assertTrue(result is han1997.renovation.ui.nav.FileActionResult.Success)
            org.junit.Assert.assertEquals("精确金额 0.29", file.readText())
        } finally {
            composeRule.runOnIdle { store.clear() }
            file.delete()
        }
    }

}
