package mx.tec.sonrisas.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDateTime
import mx.tec.sonrisas.domain.Message
import mx.tec.sonrisas.ui.components.chat.BurbujaChat
import mx.tec.sonrisas.ui.theme.SonrisasTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BurbujaChatTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun burbujaChat_rendersMessageContent() {
        val message = Message(
            chatId = "c_123",
            authorId = "teacher_1",
            content = "Estimados padres, favor de enviar ropa limpia mañana.",
            sentAt = LocalDateTime.now(),
            sent = true,
            isAviso = true
        )

        composeTestRule.setContent {
            SonrisasTheme {
                BurbujaChat(
                    message = message,
                    esMio = false
                )
            }
        }

        // Verify message content is displayed (substring = true because BurbujaChat appends trailing alignment spaces)
        composeTestRule.onNodeWithText("Estimados padres, favor de enviar ropa limpia mañana.", substring = true).assertIsDisplayed()
    }
}
