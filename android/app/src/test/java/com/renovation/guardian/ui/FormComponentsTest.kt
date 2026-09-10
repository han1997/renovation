package com.renovation.guardian.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import com.renovation.guardian.ui.components.*
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
class FormComponentsTest {
    @get:Rule val compose = createComposeRule()
    @Test fun invalidNumericDraftDisablesContinueInsteadOfUsingTheOldValue() {
        var committed = 10.0
        compose.setContent {
            val form = remember { FormState() }
            MaterialTheme { CompositionLocalProvider(LocalFormState provides form) { Column {
                NumberField("面积", 10.0) { committed = it!! }
                Button(enabled = form.valid, onClick = {}) { Text("下一步") }
            } } }
        }
        compose.onNodeWithText("面积").performTextReplacement("NaN")
        compose.onNodeWithText("下一步").assertIsNotEnabled()
        assertEquals(10.0, committed, 0.0)
        compose.onNodeWithText("面积").performTextReplacement("12.5")
        compose.onNodeWithText("下一步").assertIsEnabled()
        assertEquals(12.5, committed, 0.0)
    }
    @Test fun checkboxLabelIsClickableAndPlainCardHasNoFakeAction() {
        var checked by mutableStateOf(false)
        compose.setContent { MaterialTheme { Column {
            ToggleRow("防水工程", checked) { checked = it }
            SectionCard { Text("仅展示") }
        } } }
        compose.onNodeWithText("防水工程").performClick()
        compose.runOnIdle { assertTrue(checked) }
        compose.onNodeWithText("仅展示").assertHasNoClickAction()
    }
    @Test fun numericDraftSurvivesSavedStateRestoration() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { MaterialTheme { NumberField("面积", 10.0) {} } }
        compose.onNodeWithText("面积").performTextReplacement("12.5")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("面积").assertTextContains("12.5")
    }

}
