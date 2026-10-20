package mx.tec.sonrisas.ui.screens.organizacion

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import java.time.LocalDateTime
import mx.tec.sonrisas.domain.Message
import mx.tec.sonrisas.ui.components.chat.ChatScreen
import mx.tec.sonrisas.ui.state.UiState
import mx.tec.sonrisas.ui.theme.SonrisasTheme

/**
 * Pantalla de chat adaptada para la Organización.
 * Permite la comunicación entre la organización y los usuarios administradores (Admin) de las instancias.
 * Los avisos enviados por la organización se visualizan directamente como tarjetas de aviso dentro de este chat.
 */
@Composable
fun ChatOrgScreen(
    estado: UiState<List<Message>>,
    currentUserId: String,
    modifier: Modifier = Modifier,
    titulo: String = "Chat con Administrador",
    subtitulo: String? = null,
    onAtras: (() -> Unit)? = null,
    onEnviar: (texto: String, esAviso: Boolean) -> Unit = { _, _ -> },
    onEnviarFoto: (Uri) -> Unit = {},
    onReintentar: () -> Unit = {},
    onTab: (Int) -> Unit = {}
) {
    ChatScreen(
        estado = estado,
        currentUserId = currentUserId,
        titulo = titulo,
        modifier = modifier,
        subtitulo = subtitulo,
        tabs = TabsOrg,
        tabSeleccionado = 2,
        onTab = onTab,
        onAtras = onAtras,
        onEnviar = onEnviar,
        onEnviarFoto = onEnviarFoto,
        // Org↔admin messages can never be deleted.
        onEliminar = null,
        onReintentar = onReintentar
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ChatOrgPreview() {
    SonrisasTheme {
        ChatOrgScreen(
            estado = UiState.Exito(
                listOf(
                    Message(
                        chatId = "admin_1",
                        authorId = "admin",
                        content = "Hola, ¿en qué te puedo ayudar con la administración de la estancia?",
                        sentAt = LocalDateTime.now().minusMinutes(10)
                    ),
                    Message(
                        chatId = "admin_1",
                        authorId = "org",
                        content = "Aviso General: Capacitación para nuevos administradores el lunes a las 5:00 PM",
                        sentAt = LocalDateTime.now().minusMinutes(5),
                        isAviso = true
                    )
                )
            ),
            currentUserId = "org",
            titulo = "María López",
            subtitulo = "Admin - Sol Naciente",
            onAtras = {}
        )
    }
}
