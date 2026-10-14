package mx.tec.sonrisas.ui.components.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import mx.tec.sonrisas.domain.Message
import mx.tec.sonrisas.ui.components.EstadoCargando
import mx.tec.sonrisas.ui.components.EstadoError
import mx.tec.sonrisas.ui.components.EstadoVacio
import mx.tec.sonrisas.ui.state.UiState
import mx.tec.sonrisas.ui.theme.SonrisasTheme

@Composable
fun ChatContenido(
    estado: UiState<List<Message>>,
    currentUserId: String,
    textoVacio: String,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier,
    avatarRes: Int? = null,
    onToggleImportante: (Message) -> Unit = {},
    onEliminar: ((Message) -> Unit)? = null
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (estado) {
            UiState.Cargando -> EstadoCargando()

            UiState.Vacio -> EstadoVacio(
                titulo = "Sin mensajes",
                subtitulo = textoVacio,
                modifier = Modifier.padding(16.dp)
            )

            is UiState.Error -> EstadoError(
                titulo = "Error de conexión",
                mensaje = "No pudimos cargar la conversación",
                onReintentar = onReintentar
            )

            is UiState.Exito -> {
                val mensajes = estado.datos
                val lista = rememberLazyListState()

                LaunchedEffect(mensajes.size) {
                    if (mensajes.isNotEmpty()) lista.animateScrollToItem(mensajes.lastIndex)
                }

                LazyColumn(
                    state = lista,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(mensajes) { mensaje ->
                        BurbujaChat(
                            message = mensaje,
                            esMio = mensaje.authorId == currentUserId,
                            avatarRes = avatarRes,
                            onToggleImportante = onToggleImportante,
                            onEliminar = onEliminar
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F4EE)
@Composable
private fun ChatContenidoPreview() {
    SonrisasTheme {
        ChatContenido(
            estado = UiState.Exito(
                listOf(
                    Message(chatId = "1", authorId = "padre", content = "Hola, Sofía se divirtió mucho hoy.", sentAt = LocalDateTime.now()),
                    Message(chatId = "1", authorId = "maestra", content = "¡Muchas gracias, maestra!", sentAt = LocalDateTime.now())
                )
            ),
            currentUserId = "maestra",
            textoVacio = "Envía un mensaje al padre de familia",
            onReintentar = {}
        )
    }
}
