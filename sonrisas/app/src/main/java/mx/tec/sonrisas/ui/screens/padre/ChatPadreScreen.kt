package mx.tec.sonrisas.ui.screens.padre

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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
    onEnviar: (texto: String) -> Unit = {},
    onEnviarFoto: (Uri) -> Unit = {},
    onToggleImportante: (Message) -> Unit = {},
    onEliminar: (Message) -> Unit = {},
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
        onToggleImportante = onToggleImportante,
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
