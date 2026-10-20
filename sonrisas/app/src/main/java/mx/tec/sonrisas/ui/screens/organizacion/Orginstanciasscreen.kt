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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.ui.components.BottomBar
import mx.tec.sonrisas.ui.components.CampoOpciones
import mx.tec.sonrisas.ui.components.CampoTexto
import mx.tec.sonrisas.ui.components.EstadoCargando
import mx.tec.sonrisas.ui.components.EstadoError
import mx.tec.sonrisas.ui.components.EstadoVacio
import mx.tec.sonrisas.ui.components.FlagSecureDisable
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.state.UiState
import mx.tec.sonrisas.ui.theme.*

private enum class FiltroInstancia(val etiqueta: String) {
    TODAS("Todas"),
    ACTIVAS("Activas"),
    SUSPENDIDAS("Suspendidas")
}

@Composable
fun OrgInstanciasScreen(
    estado: UiState<List<InstanciaUi>>,
    modifier: Modifier = Modifier,
    onInstanciaClick: (InstanciaUi) -> Unit = {},
    onNuevaInstancia: () -> Unit = {},
    onReintentar: () -> Unit = {},
    onCerrarSesion: (() -> Unit)? = null,
    onTab: (Int) -> Unit = {}
) {
    FlagSecureDisable()
    var busqueda by remember { mutableStateOf("") }
    var filtro by remember { mutableStateOf(FiltroInstancia.TODAS) }

    Scaffold(
        modifier = modifier,
        containerColor = Fondo,
        bottomBar = { BottomBar(tabs = TabsOrg, seleccionado = 1, onSeleccionar = onTab) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNuevaInstancia,
                containerColor = Coral,
                contentColor = Color.White
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Nueva instancia")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBarAtras(
                titulo = "Estancias",
                subtitulo = "Panel de organización",
                mostrarAvatar = true,
                onCerrarSesion = onCerrarSesion
            )

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (estado) {
                    UiState.Cargando -> EstadoCargando()

                    UiState.Vacio -> EstadoVacio(
                        titulo = "Sin instancias",
                        subtitulo = "Crea la primera con el botón +",
                        modifier = Modifier.padding(16.dp)
                    )

                    is UiState.Error -> EstadoError(
                        titulo = "Error al cargar",
                        mensaje = estado.mensaje,
                        onReintentar = onReintentar
                    )

                    is UiState.Exito -> {
                        val instancias = estado.datos
                        val filtradas = instancias.filter { instancia ->
                            val coincideNombre = instancia.nombre.contains(busqueda, ignoreCase = true)
                            val coincideEstado = when (filtro) {
                                FiltroInstancia.ACTIVAS -> instancia.activa
                                FiltroInstancia.SUSPENDIDAS -> !instancia.activa
                                FiltroInstancia.TODAS -> true
                            }
                            coincideNombre && coincideEstado
                        }

                        Column(Modifier.fillMaxSize()) {
                            Column(Modifier.padding(horizontal = 16.dp)) {
                                CampoTexto("Buscar guardería", busqueda, { busqueda = it })
                                Spacer(Modifier.height(4.dp))
                                CampoOpciones(
                                    etiqueta = "Mostrar",
                                    valor = filtro,
                                    opciones = FiltroInstancia.entries,
                                    onValorChange = { filtro = it },
                                    etiquetaOpcion = { it.etiqueta }
                                )
                            }

                            if (filtradas.isEmpty()) {
                                EstadoVacio(
                                    titulo = "Sin resultados",
                                    subtitulo = "Prueba con otra búsqueda o filtro",
                                    modifier = Modifier.padding(16.dp)
                                )
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(filtradas, key = { it.id }) { instancia ->
                                        TarjetaInstancia(instancia = instancia, onClick = { onInstanciaClick(instancia) })
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
private fun OrgInstanciasPreview() {
    SonrisasTheme { OrgInstanciasScreen(estado = UiState.Exito(OrgMuestra.instancias)) }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun OrgInstanciasVacioPreview() {
    SonrisasTheme { OrgInstanciasScreen(estado = UiState.Vacio) }
}
