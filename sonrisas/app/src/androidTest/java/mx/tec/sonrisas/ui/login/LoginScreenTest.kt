package mx.tec.sonrisas.ui.login

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import mx.tec.sonrisas.ui.screens.login.LoginScreen
import mx.tec.sonrisas.ui.theme.SonrisasTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginScreen_displaysAppTitleAndFields() {
        composeTestRule.setContent {
            SonrisasTheme {
                LoginScreen()
            }
        }

        // Verify app title
        composeTestRule.onNodeWithText("Sonrisas de Cristal").assertIsDisplayed()

        // Verify field headers
        composeTestRule.onNodeWithText("Correo electrónico").assertIsDisplayed()
        composeTestRule.onNodeWithText("Contraseña").assertIsDisplayed()

        // Verify submit button is disabled when fields are empty
        composeTestRule.onNodeWithText("Iniciar Sesión").assertIsNotEnabled()
    }

    @Test
    fun loginScreen_enablesButtonWhenFieldsAreFilled() {
        composeTestRule.setContent {
            SonrisasTheme {
                LoginScreen()
            }
        }

        // Type email into the first text field
        composeTestRule.onAllNodes(hasSetTextAction()).onFirst().performTextInput("maria@correo.com")

        // Type password into the second text field
        composeTestRule.onAllNodes(hasSetTextAction())[1].performTextInput("password123")

        // Verify submit button becomes enabled
        composeTestRule.onNodeWithText("Iniciar Sesión").assertIsEnabled()
    }
}
