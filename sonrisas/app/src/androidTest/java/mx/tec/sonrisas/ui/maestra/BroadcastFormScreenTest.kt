package mx.tec.sonrisas.ui.maestra

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mx.tec.sonrisas.ui.screens.maestra.BroadcastFormScreen
import mx.tec.sonrisas.ui.theme.SonrisasTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BroadcastFormScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun broadcastFormScreen_rendersTitleAndAttachmentButton() {
        composeTestRule.setContent {
            SonrisasTheme {
                BroadcastFormScreen(
                    titulo = "Actividad",
                    descripcionAyuda = "Registra una actividad o comparte una foto con los familiares.",
                    textoBoton = "Enviar a los presentes",
                    onAtras = {}
                )
            }
        }

        // Verify title
        composeTestRule.onNodeWithText("Actividad", substring = true).assertIsDisplayed()

        // Verify attachment button
        composeTestRule.onNodeWithText("Adjuntar foto", substring = true).assertIsDisplayed()

        // Verify submit button default text
        composeTestRule.onNodeWithText("Enviar a los presentes", substring = true).assertIsDisplayed()
    }
}
