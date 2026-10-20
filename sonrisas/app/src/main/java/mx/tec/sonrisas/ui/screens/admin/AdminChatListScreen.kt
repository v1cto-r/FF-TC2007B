package mx.tec.sonrisas.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import mx.tec.sonrisas.domain.Message
import mx.tec.sonrisas.ui.components.BottomBar
import mx.tec.sonrisas.ui.components.EstadoCargando
import mx.tec.sonrisas.ui.components.EstadoError
import mx.tec.sonrisas.ui.components.EstadoVacio
import mx.tec.sonrisas.ui.components.FlagSecureDisable
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.components.chat.FilaChat
import mx.tec.sonrisas.ui.state.UiState
import mx.tec.sonrisas.ui.theme.*

/**
 * Todos los chats del admin en una lista: la organización fijada arriba y después cada maestra,
 * la conversación más reciente primero.
 *
 * @param ultimosMensajes último mensaje por chat id (el del admin para la organización, el de cada maestra).
 */
@Composable
fun AdminChatListScreen(
    estado: UiState<List<MaestroUi>>,
    miId: String,
    ultimosMensajes: Map<String, Message>,
    modifier: Modifier = Modifier,
    onChatOrg: () -> Unit = {},
    onMaestroClick: (MaestroUi) -> Unit = {},
    onReintentar: () -> Unit = {},
    onCerrarSesion: (() -> Unit)? = null,
    onTab: (Int) -> Unit = {}
) {
    FlagSecureDisable()
    Scaffold(
        modifier = modifier,
        containerColor = Fondo,
        bottomBar = { BottomBar(tabs = TabsAdmin, seleccionado = 4, onSeleccionar = onTab) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBarAtras(
                titulo = "Chats",
                subtitulo = "Organización y maestras",
                mostrarAvatar = true,
                onCerrarSesion = onCerrarSesion
            )

            // The org chat is pinned: always first, whatever the latest message.
            FilaChat(
                titulo = "Organización",
                ultimoMensaje = ultimosMensajes[miId],
                fijado = true,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                onClick = onChatOrg
            )

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (estado) {
                    UiState.Cargando -> EstadoCargando()

                    UiState.Vacio -> EstadoVacio(
                        titulo = "Sin maestras",
                        subtitulo = "Cuando registres maestras aparecerán aquí",
                        modifier = Modifier.padding(16.dp)
                    )

                    is UiState.Error -> EstadoError(
                        titulo = "Error al cargar",
                        mensaje = "No pudimos cargar tus chats",
                        onReintentar = onReintentar
                    )

                    is UiState.Exito -> LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(estado.datos.sortedByDescending { ultimosMensajes[it.id]?.sentAt }) { maestro ->
                            FilaChat(
                                titulo = maestro.nombre,
                                ultimoMensaje = ultimosMensajes[maestro.id],
                                onClick = { onMaestroClick(maestro) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AdminChatListPreview() {
    SonrisasTheme {
        AdminChatListScreen(
            estado = UiState.Exito(
                listOf(
                    MaestroUi(id = "m1", nombre = "Laura Gómez", correo = "laura@sonrisas.mx"),
                    MaestroUi(id = "m2", nombre = "Ana Ruiz", correo = "ana@sonrisas.mx")
                )
            ),
            miId = "yo",
            ultimosMensajes = mapOf(
                "m2" to Message(chatId = "m2", authorId = "m2", content = "Ya quedó el salón", sentAt = LocalDateTime.now())
            )
        )
    }
}
