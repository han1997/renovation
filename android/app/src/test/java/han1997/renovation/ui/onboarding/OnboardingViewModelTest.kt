package han1997.renovation.ui.onboarding

import han1997.renovation.RenovationApp
import han1997.renovation.data.repo.AppContainer
import han1997.renovation.data.repo.HouseProfileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class OnboardingViewModelTest {

    @Test
    fun finish_delegatesToRepo() = runTest {
        val app = mockk<RenovationApp>(relaxed = true)
        val container = mockk<AppContainer>(relaxed = true)
        every { app.container } returns container
        val repo = mockk<HouseProfileRepository>(relaxed = true)
        every { container.houseProfileRepo } returns repo
        coEvery { repo.finishOnboarding(any(), any(), any(), any(), any(), any(), any(), any()) } returns mockk()

        val vm = OnboardingViewModel(app)
        vm.finish(90.0, "t2", "clear", "mid", null, 12345.0)

        coVerify {
            repo.finishOnboarding(90.0, "t2", "clear", "mid", null, 12345.0, any(), any())
        }
    }
}
