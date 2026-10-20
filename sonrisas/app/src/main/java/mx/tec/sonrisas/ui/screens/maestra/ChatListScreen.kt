package mx.tec.sonrisas.ui.screens.maestra

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import mx.tec.sonrisas.domain.Child
import mx.tec.sonrisas.domain.GroupEnum
import mx.tec.sonrisas.domain.Message
import mx.tec.sonrisas.ui.components.BottomBar
import mx.tec.sonrisas.ui.components.EstadoCargando
import mx.tec.sonrisas.ui.components.EstadoError
import mx.tec.sonrisas.ui.components.EstadoVacio
import mx.tec.sonrisas.ui.components.TabsMaestra
import mx.tec.sonrisas.ui.components.FlagSecureEnable
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.components.chat.FilaChat
import mx.tec.sonrisas.ui.state.ConversacionUi
import mx.tec.sonrisas.ui.state.UiState
import mx.tec.sonrisas.ui.theme.*

@Composable
fun ChatListScreen(
    estado: UiState<List<ConversacionUi>>,
    modifier: Modifier = Modifier,
    ultimoMensajeDireccion: Message? = null,
    onChatDireccion: () -> Unit = {},
    onConversacionClick: (Child) -> Unit = {},
    onReintentar: () -> Unit = {},
    onTab: (Int) -> Unit = {}
) {
    FlagSecureEnable()
    Scaffold(
        modifier = modifier,
        containerColor = Fondo,
        bottomBar = { BottomBar(tabs = TabsMaestra, seleccionado = 2, onSeleccionar = onTab) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBarAtras(titulo = "Chats", subtitulo = "Conversaciones con los padres")

            // The chat with the estancia's admin is pinned: always first, whatever the latest message.
            FilaChat(
                titulo = "Dirección",
                ultimoMensaje = ultimoMensajeDireccion,
                fijado = true,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                onClick = onChatDireccion
            )

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (estado) {
                    UiState.Cargando -> EstadoCargando()

                    UiState.Vacio -> EstadoVacio(
                        titulo = "Sin conversaciones",
                        subtitulo = "Cuando tengas niños registrados aparecerán aquí",
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
                        items(estado.datos) { conversacion ->
                            FilaChat(
                                titulo = "${conversacion.child.parentName} - ${conversacion.child.fullName.substringBefore(" ")}",
                                ultimoMensaje = conversacion.ultimoMensaje,
                                onClick = { onConversacionClick(conversacion.child) }
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
private fun ChatListPreview() {
    SonrisasTheme {
        ChatListScreen(
            estado = UiState.Exito(
                listOf(
                    ConversacionUi(
                        Child(fullName = "Sofía Martínez", daycareCenter = "Estancia Mariposa", group = GroupEnum.P, parentName = "María López"),
                        Message(chatId = "1", authorId = "padre", content = "Gracias maestra", sentAt = LocalDateTime.now())
                    ),
                    ConversacionUi(
                        Child(fullName = "Mateo García", daycareCenter = "Estancia Mariposa", group = GroupEnum.M, parentName = "Jorge García"),
                        null
                    )
                )
            )
        )
    }
}
