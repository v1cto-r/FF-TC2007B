package mx.tec.sonrisas.ui.screens.organizacion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.ui.components.CampoTexto
import mx.tec.sonrisas.ui.components.FlagSecureDisable
import mx.tec.sonrisas.ui.components.TituloSeccion
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.theme.*

/** Asignar (instancia sin admin) o reasignar (instancia con admin) al administrador de una instancia. */
@Composable
fun OrgReasignarAdminScreen(
    instancia: InstanciaUi,
    modifier: Modifier = Modifier,
    onAtras: () -> Unit = {},
    onGuardar: () -> Unit = {}
) {
    FlagSecureDisable()
    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }

    val puedeGuardar = nombre.isNotBlank() && correo.contains("@") && contrasena.length >= 8
    val nuevo = instancia.admin == null

    Scaffold(
        modifier = modifier,
        containerColor = Fondo,
        bottomBar = {
            Box(Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
                Button(
                    onClick = onGuardar,
                    enabled = puedeGuardar,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Coral,
                        contentColor = Color.White,
                        disabledContainerColor = Coral.copy(alpha = 0.4f),
                        disabledContentColor = Color.White
                    )
                ) {
                    Text(if (nuevo) "Asignar administrador" else "Reasignar administrador")
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBarAtras(
                titulo = if (nuevo) "Asignar administrador" else "Reasignar administrador",
                subtitulo = instancia.nombre,
                onAtras = onAtras
            )

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "Ingresa los datos de la persona que administrará ${instancia.nombre}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoGris
                    )
                }
                item { Spacer(Modifier.height(4.dp)) }
                item { TituloSeccion("Administrador") }
                item { CampoTexto("Nombre completo", nombre, { nombre = it }) }
                item { CampoTexto("Correo electrónico", correo, { correo = it }) }
                item { CampoContrasenaTemporal("Contraseña temporal", contrasena, { contrasena = it }) }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun OrgReasignarAdminPreview() {
    SonrisasTheme { OrgReasignarAdminScreen(instancia = OrgMuestra.instancias[0]) }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun OrgAsignarAdminPreview() {
    SonrisasTheme { OrgReasignarAdminScreen(instancia = OrgMuestra.instancias[1]) }
}