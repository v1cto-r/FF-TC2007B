package mx.tec.sonrisas.ui.padre

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mx.tec.sonrisas.domain.Child
import mx.tec.sonrisas.domain.GroupEnum
import mx.tec.sonrisas.ui.screens.padre.OnboardingHijoScreen
import mx.tec.sonrisas.ui.theme.SonrisasTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingHijoScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun onboardingHijoScreen_rendersFieldsAndButtons() {
        val child = Child(
            fullName = "Sofía Martínez",
            daycareCenter = "Sonrisas de Cristal",
            group = GroupEnum.P,
            parentName = "María López"
        )

        composeTestRule.setContent {
            SonrisasTheme {
                OnboardingHijoScreen(
                    child = child,
                    onGuardarInformacion = { _, _, _ -> }
                )
            }
        }

        // Verify TopBar title
        composeTestRule.onNodeWithText("Registro de Expediente", substring = true).assertIsDisplayed()

        // Verify form labels
        composeTestRule.onNodeWithText("Acta de Nacimiento", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Fecha de Nacimiento", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Alergias", substring = true).assertIsDisplayed()

        // Verify submit button
        composeTestRule.onNodeWithText("Completar Expediente e Iniciar", substring = true).assertIsDisplayed()
    }
}
