package mx.tec.sonrisas.ui.screens.organizacion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.ui.components.FlagSecureDisable
import mx.tec.sonrisas.ui.components.ItemPersona
import mx.tec.sonrisas.ui.components.TituloSeccion
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.theme.*

@Composable
fun OrgDetalleInstanciaScreen(
    instancia: InstanciaUi,
    modifier: Modifier = Modifier,
    onAtras: () -> Unit = {},
    onEditar: () -> Unit = {},
    onReasignarAdmin: () -> Unit = {},
    onCambiarEstado: () -> Unit = {}
) {
    FlagSecureDisable()
    Scaffold(modifier = modifier, containerColor = Fondo) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBarAtras(
                titulo = instancia.nombre,
                subtitulo = instancia.ciudad,
                onAtras = onAtras
            )

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { EtiquetaEstadoInstancia(activa = instancia.activa) }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DatoNumerico(instancia.ninos.toString(), "Niños", Modifier.weight(1f))
                        DatoNumerico(instancia.maestros.toString(), "Maestros", Modifier.weight(1f))
                        DatoNumerico(instancia.salones.toString(), "Salones", Modifier.weight(1f))
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
                item { TituloSeccion("Administrador") }
                item {
                    if (instancia.admin != null) {
                        ItemPersona(titulo = instancia.admin.nombre, subtitulo = instancia.admin.correo)
                    } else {
                        Text(
                            text = "Esta instancia aún no tiene administrador asignado",
                            style = MaterialTheme.typography.bodySmall,
                            color = Coral
                        )
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
                item { TituloSeccion("Acciones") }
                item { BotonAccion("Editar datos", Coral, onEditar) }
                item {
                    BotonAccion(
                        texto = if (instancia.admin == null) "Asignar administrador" else "Reasignar admin",
                        color = Coral,
                        onClick = onReasignarAdmin
                    )
                }
                item {
                    BotonAccion(
                        texto = if (instancia.activa) "Suspender instancia" else "Reactivar instancia",
                        color = Amarillo,
                        onClick = onCambiarEstado
                    )
                }
            }
        }
    }
}

@Composable
private fun DatoNumerico(valor: String, etiqueta: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = valor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextoOscuro
            )
            Text(text = etiqueta, style = MaterialTheme.typography.bodySmall, color = TextoGris)
        }
    }
}

@Composable
private fun BotonAccion(texto: String, color: Color, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = color)
    ) {
        Text(texto)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun OrgDetalleInstanciaPreview() {
    SonrisasTheme { OrgDetalleInstanciaScreen(instancia = OrgMuestra.instancias[0]) }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun OrgDetalleInstanciaSinAdminPreview() {
    SonrisasTheme { OrgDetalleInstanciaScreen(instancia = OrgMuestra.instancias[1]) }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun OrgDetalleInstanciaSuspendidaPreview() {
    SonrisasTheme { OrgDetalleInstanciaScreen(instancia = OrgMuestra.instancias[2]) }
}