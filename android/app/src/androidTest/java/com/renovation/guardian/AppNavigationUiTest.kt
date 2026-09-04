package com.renovation.guardian

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.renovation.guardian.data.repo.AppContainer
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
    fun firstLaunch_showsOnboarding() = runBlocking {
        container.clearAllData()
        composeRule.onNodeWithText("共 3 步", substring = true).assertIsDisplayed()
    }

    @Test
    fun afterOnboarding_showsHome() = runBlocking {
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
        composeRule.onNodeWithText("装修进行时").assertIsDisplayed()
    }
}
