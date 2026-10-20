package mx.tec.sonrisas.ui.screens.padre

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.time.LocalDateTime
import mx.tec.sonrisas.domain.Message
import mx.tec.sonrisas.ui.components.TabsPadre
import mx.tec.sonrisas.ui.components.chat.ChatScreen
import mx.tec.sonrisas.ui.state.UiState
import mx.tec.sonrisas.ui.theme.SonrisasTheme

@Composable
fun ChatPadreScreen(
    estado: UiState<List<Message>>,
    currentUserId: String,
    modifier: Modifier = Modifier,
    titulo: String = "Maestra Laura",
    subtitulo: String? = null,
    onAtras: (() -> Unit)? = null,
    onEnviar: (texto: String, esAviso: Boolean) -> Unit = { _, _ -> },
    onEnviarFoto: (Uri) -> Unit = {},
    onEliminar: ((Message) -> Unit)? = null,
    onReintentar: () -> Unit = {},
    onTab: (Int) -> Unit = {}
) {
    ChatScreen(
        estado = estado,
        currentUserId = currentUserId,
        titulo = titulo,
        textoVacio = "Escribe un mensaje a la maestra",
        modifier = modifier,
        subtitulo = subtitulo,
        tabs = TabsPadre,
        tabSeleccionado = 2,
        onTab = onTab,
        onAtras = onAtras,
        onEnviar = onEnviar,
        onEnviarFoto = onEnviarFoto,
        onEliminar = onEliminar,
        onReintentar = onReintentar
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ChatPadrePreview() {
    SonrisasTheme {
        ChatPadreScreen(
            estado = UiState.Exito(listOf(Message(chatId = "1", authorId = "padre", content = "Hola", sentAt = LocalDateTime.now()))),
            currentUserId = "padre"
        )
    }
}
