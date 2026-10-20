package mx.tec.sonrisas.ui.screens.organizacion

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.ui.components.AvatarCircular
import mx.tec.sonrisas.ui.components.BottomBar
import mx.tec.sonrisas.ui.components.EstadoCargando
import mx.tec.sonrisas.ui.components.EstadoError
import mx.tec.sonrisas.ui.components.EstadoVacio
import mx.tec.sonrisas.ui.components.FlagSecureDisable
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.state.UiState
import mx.tec.sonrisas.ui.theme.*

/** One row per estancia that has an admin: the org↔admin chat is keyed by the admin's user id. */
@Composable
fun OrgChatListScreen(
    estado: UiState<List<InstanciaUi>>,
    modifier: Modifier = Modifier,
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
                        items(estado.datos) { instancia ->
                            val admin = instancia.admin
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(if (admin != null) Modifier.clickable { onInstanciaClick(admin.id) } else Modifier),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AvatarCircular(tamano = 44.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = if (admin != null) "${instancia.nombre} – ${admin.nombre}" else instancia.nombre,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = TextoOscuro
                                        )
                                        Text(
                                            text = if (admin != null) "Chat con el administrador" else "Sin administrador asignado",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextoGris
                                        )
                                    }
                                }
                            }
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
