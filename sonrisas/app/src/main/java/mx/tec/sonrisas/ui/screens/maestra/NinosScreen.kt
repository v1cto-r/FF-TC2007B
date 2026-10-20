package mx.tec.sonrisas.ui.screens.maestra

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.domain.Child
import mx.tec.sonrisas.domain.GroupEnum
import mx.tec.sonrisas.ui.components.BottomBar
import mx.tec.sonrisas.ui.components.EstadoCargando
import mx.tec.sonrisas.ui.components.EstadoError
import mx.tec.sonrisas.ui.components.EstadoVacio
import mx.tec.sonrisas.ui.components.FlagSecureDisable
import mx.tec.sonrisas.ui.components.ItemNino
import mx.tec.sonrisas.ui.components.TabsMaestra
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.state.NinoConEstado
import mx.tec.sonrisas.ui.state.UiState
import mx.tec.sonrisas.ui.theme.*

@Composable
fun NinosScreen(
    estado: UiState<List<NinoConEstado>>,
    modifier: Modifier = Modifier,
    onNinoClick: (Child) -> Unit = {},
    onReintentar: () -> Unit = {},
    onTab: (Int) -> Unit = {}
) {
    FlagSecureDisable()
    var busqueda by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier,
        containerColor = Fondo,
        bottomBar = { BottomBar(tabs = TabsMaestra, seleccionado = 1, onSeleccionar = onTab) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBarAtras(titulo = "Mis Niños", subtitulo = "Lista de asistencia general")

            if (estado == UiState.Vacio || estado is UiState.Exito) {
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    placeholder = { Text("Buscar niño o niña...", color = TextoGris) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = TextoGris) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Coral,
                        unfocusedBorderColor = BordeCampo,
                        cursorColor = Coral
                    )
                )
                Spacer(Modifier.height(8.dp))
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (estado) {
                    UiState.Cargando -> EstadoCargando()

                    UiState.Vacio -> EstadoVacio(
                        titulo = "Sin niños registrados",
                        subtitulo = "Los niños de tu sala aparecerán aquí",
                        modifier = Modifier.padding(16.dp)
                    )

                    is UiState.Error -> EstadoError(
                        titulo = "Error al cargar",
                        mensaje = "No pudimos cargar la lista de niños",
                        onReintentar = onReintentar,
                        colorAcento = Verde,
                        colorTinte = VerdeSuave
                    )

                    is UiState.Exito -> {
                        val filtrados = estado.datos.filter {
                            it.child.fullName.contains(busqueda, ignoreCase = true)
                        }
                        if (filtrados.isEmpty()) {
                            Text(
                                text = "Sin resultados para \"$busqueda\"",
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(24.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextoGris
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filtrados) { ninoConEstado ->
                                    ItemNino(
                                        child = ninoConEstado.child,
                                        presente = ninoConEstado.presente,
                                        onClick = { onNinoClick(ninoConEstado.child) },
                                        recogeHoy = ninoConEstado.autorizacionHoy?.let {
                                            "${it.authorizedPersonName} (${it.relationship})"
                                        }
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun NinosPreview() {
    SonrisasTheme {
        NinosScreen(
            estado = UiState.Exito(
                listOf(
                    NinoConEstado(Child(fullName = "Sofía Martínez", daycareCenter = "Estancia Mariposa", group = GroupEnum.P, parentName = "María López"), true),
                    NinoConEstado(Child(fullName = "Valentina Ruiz", daycareCenter = "Estancia Mariposa", group = GroupEnum.M, parentName = "Carla Ruiz"), false)
                )
            )
        )
    }
}
