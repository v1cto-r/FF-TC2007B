package mx.tec.sonrisas.ui.screens.organizacion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import mx.tec.sonrisas.ui.components.CampoEntero
import mx.tec.sonrisas.ui.components.CampoTexto
import mx.tec.sonrisas.ui.components.FlagSecureDisable
import mx.tec.sonrisas.ui.components.TituloSeccion
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.theme.*

/**
 * Sirve para crear y para editar:
 * - instanciaInicial = null  -> "Nueva instancia" (pide también los datos del administrador)
 * - instanciaInicial != null -> "Editar instancia" (solo datos de la guardería)
 */
@Composable
fun OrgNuevaInstanciaScreen(
    modifier: Modifier = Modifier,
    instanciaInicial: InstanciaUi? = null,
    onAtras: () -> Unit = {},
    onGuardar: () -> Unit = {}
) {
    FlagSecureDisable()
    val editando = instanciaInicial != null

    var nombre by remember { mutableStateOf(instanciaInicial?.nombre ?: "") }
    var direccion by remember { mutableStateOf(instanciaInicial?.direccion ?: "") }
    var telefono by remember { mutableStateOf(instanciaInicial?.telefono ?: "") }
    var capacidad by remember { mutableStateOf(instanciaInicial?.capacidad ?: 0) }
    var adminNombre by remember { mutableStateOf("") }
    var adminCorreo by remember { mutableStateOf("") }
    var adminContrasena by remember { mutableStateOf("") }

    val adminValido = adminNombre.isNotBlank() &&
            adminCorreo.contains("@") &&
            adminContrasena.length >= 8
    val puedeGuardar = nombre.isNotBlank() && (editando || adminValido)

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
                    Text(if (editando) "Guardar cambios" else "Crear instancia")
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBarAtras(
                titulo = if (editando) "Editar instancia" else "Nueva instancia",
                subtitulo = if (editando) instanciaInicial?.nombre else "Nueva guardería",
                onAtras = onAtras
            )

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { TituloSeccion("Datos de la guardería") }
                item { CampoTexto("Nombre", nombre, { nombre = it }) }
                item { CampoTexto("Dirección", direccion, { direccion = it }) }
                item { CampoTexto("Teléfono de contacto", telefono, { telefono = it }) }
                item { CampoEntero("Capacidad (niños)", capacidad, { capacidad = it }) }

                if (!editando) {
                    item { Spacer(Modifier.height(8.dp)) }
                    item { TituloSeccion("Administrador") }
                    item { CampoTexto("Nombre completo", adminNombre, { adminNombre = it }) }
                    item { CampoTexto("Correo electrónico", adminCorreo, { adminCorreo = it }) }
                    item { CampoContrasenaTemporal("Contraseña temporal", adminContrasena, { adminContrasena = it }) }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun OrgNuevaInstanciaPreview() {
    SonrisasTheme { OrgNuevaInstanciaScreen() }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun OrgEditarInstanciaPreview() {
    SonrisasTheme { OrgNuevaInstanciaScreen(instanciaInicial = OrgMuestra.instancias[0]) }
}