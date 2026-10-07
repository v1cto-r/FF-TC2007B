package mx.tec.sonrisas.ui.screens.organizacion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.domain.Notice
import mx.tec.sonrisas.ui.components.BottomBar
import mx.tec.sonrisas.ui.components.EstadoVacio
import mx.tec.sonrisas.ui.components.FlagSecureDisable
import mx.tec.sonrisas.ui.components.ItemEvento
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.theme.*

@Composable
fun OrgAvisosScreen(
    avisos: List<Notice>,
    modifier: Modifier = Modifier,
    onNuevoAviso: () -> Unit = {},
    onTab: (Int) -> Unit = {}
) {
    FlagSecureDisable()
    Scaffold(
        modifier = modifier,
        containerColor = Fondo,
        bottomBar = { BottomBar(tabs = TabsOrg, seleccionado = 2, onSeleccionar = onTab) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNuevoAviso,
                containerColor = Coral,
                contentColor = Color.White
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Nuevo aviso")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBarAtras(
                titulo = "Avisos",
                subtitulo = "Enviados a los administradores",
                mostrarAvatar = true
            )

            if (avisos.isEmpty()) {
                EstadoVacio(
                    titulo = "Sin avisos",
                    subtitulo = "Envía el primero con el botón +",
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(avisos) { aviso -> ItemEvento(notice = aviso) }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun OrgAvisosPreview() {
    SonrisasTheme { OrgAvisosScreen(avisos = OrgMuestra.avisos) }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun OrgAvisosVacioPreview() {
    SonrisasTheme { OrgAvisosScreen(avisos = emptyList()) }
}