package mx.tec.sonrisas.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.ui.components.BottomBar
import mx.tec.sonrisas.ui.components.EstadoVacio
import mx.tec.sonrisas.ui.components.FlagSecureDisable
import mx.tec.sonrisas.ui.components.ItemPersona
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.theme.*

@Composable
fun AdminPadresScreen(
    padres: List<PadreUi>,
    modifier: Modifier = Modifier,
    onPadreClick: (PadreUi) -> Unit = {},
    onNuevoPadre: () -> Unit = {},
    onCerrarSesion: (() -> Unit)? = null,
    onTab: (Int) -> Unit = {}
) {
    FlagSecureDisable()
    Scaffold(
        modifier = modifier,
        containerColor = Fondo,
        bottomBar = { BottomBar(tabs = TabsAdmin, seleccionado = 2, onSeleccionar = onTab) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNuevoPadre,
                containerColor = Coral,
                contentColor = Color.White,
                icon = { Icon(Icons.Outlined.PersonAdd, contentDescription = null) },
                text = { Text("Nuevo padre", style = MaterialTheme.typography.labelLarge) }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBarAtras(
                titulo = "Padres",
                subtitulo = if (padres.size == 1) "1 familia" else "${padres.size} familias",
                mostrarAvatar = true,
                onCerrarSesion = onCerrarSesion
            )

            if (padres.isEmpty()) {
                EstadoVacio(
                    titulo = "Sin padres registrados",
                    subtitulo = "Crea el primero con el botón de abajo",
                    modifier = Modifier.padding(16.dp)
                )
                return@Column
            }

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(padres) { padre ->
                    ItemPersona(
                        titulo = padre.nombre,
                        subtitulo = if (padre.hijos.isEmpty()) "Sin niños registrados"
                        else padre.hijos.joinToString { it.nombre },
                        onClick = { onPadreClick(padre) }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AdminPadresPreview() {
    SonrisasTheme { AdminPadresScreen(padres = AdminMuestra.padres) }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AdminPadresVacioPreview() {
    SonrisasTheme { AdminPadresScreen(padres = emptyList()) }
}
