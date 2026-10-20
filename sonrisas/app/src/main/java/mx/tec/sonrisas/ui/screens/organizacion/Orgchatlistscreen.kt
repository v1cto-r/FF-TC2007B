package mx.tec.sonrisas.ui.screens.organizacion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
 * One row per estancia: the org↔admin chat is keyed by the admin's user id. Latest conversation first;
 * estancias without an admin go last and can't be opened.
 *
 * @param ultimosMensajes last message per chat id (admin user id).
 */
@Composable
fun OrgChatListScreen(
    estado: UiState<List<InstanciaUi>>,
    modifier: Modifier = Modifier,
    ultimosMensajes: Map<String, Message> = emptyMap(),
    onInstanciaClick: (adminId: String) -> Unit = {},
    onReintentar: () -> Unit = {},
    onTab: (Int) -> Unit = {}
) {
    FlagSecureDisable()
    Scaffold(
        modifier = modifier,
        containerColor = Fondo,
        bottomBar = { BottomBar(tabs = TabsOrg, seleccionado = 2, onSeleccionar = onTab) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBarAtras(titulo = "Chats", subtitulo = "Conversaciones con los administradores", mostrarAvatar = true)

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (estado) {
                    UiState.Cargando -> EstadoCargando()

                    UiState.Vacio -> EstadoVacio(
                        titulo = "Sin instancias",
                        subtitulo = "Cuando registres instancias aparecerán aquí",
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
                        val ordenadas = estado.datos.sortedWith(
                            compareBy<InstanciaUi> { it.admin == null }
                                .thenByDescending { it.admin?.let { admin -> ultimosMensajes[admin.id]?.sentAt } }
                        )
                        items(ordenadas) { instancia ->
                            val admin = instancia.admin
                            FilaChat(
                                titulo = if (admin != null) "${instancia.nombre} – ${admin.nombre}" else instancia.nombre,
                                ultimoMensaje = admin?.let { ultimosMensajes[it.id] },
                                textoSinMensajes = if (admin != null) "Sin mensajes aún" else "Sin administrador asignado",
                                onClick = admin?.let { { onInstanciaClick(it.id) } }
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
private fun OrgChatListPreview() {
    SonrisasTheme { OrgChatListScreen(estado = UiState.Exito(OrgMuestra.instancias)) }
}
