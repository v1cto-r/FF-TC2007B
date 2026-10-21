package mx.tec.sonrisas.ui.screens.organizacion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.ui.components.CampoEntero
import mx.tec.sonrisas.ui.components.CampoTexto
import mx.tec.sonrisas.ui.components.FlagSecureDisable
import mx.tec.sonrisas.ui.components.TituloSeccion
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.theme.*

/**
 * Sirve para crear y para editar instancias:
 * Permite modificar nombre, dirección, teléfono, capacidad y datos del administrador.
 */
@Composable
fun OrgNuevaInstanciaScreen(
    modifier: Modifier = Modifier,
    instanciaInicial: InstanciaUi? = null,
    contrasenaGenerada: String? = null,
    onAtras: () -> Unit = {},
    onGuardar: (nombre: String, direccion: String, telefono: String, capacidad: Int, adminNombre: String, adminCorreo: String) -> Unit = { _, _, _, _, _, _ -> },
    onCerrarDialogoContrasena: () -> Unit = {},
    onResetPassword: (String) -> Unit = {}
) {
    FlagSecureDisable()
    val editando = instanciaInicial != null

    var nombre by remember { mutableStateOf(instanciaInicial?.nombre ?: "") }
    var direccion by remember { mutableStateOf(instanciaInicial?.direccion ?: "") }
    var telefono by remember { mutableStateOf(instanciaInicial?.telefono ?: "") }
    var capacidad by remember { mutableStateOf(instanciaInicial?.capacidad ?: 0) }
    var adminNombre by remember { mutableStateOf(instanciaInicial?.admin?.nombre ?: "") }
    var adminCorreo by remember { mutableStateOf(instanciaInicial?.admin?.correo ?: "") }

    val adminValido = adminNombre.isNotBlank() && adminCorreo.contains("@")
    val puedeGuardar = nombre.isNotBlank() && adminValido

    Scaffold(
        modifier = modifier,
        containerColor = Fondo,
        bottomBar = {
            Box(Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
                Button(
                    onClick = { onGuardar(nombre, direccion, telefono, capacidad, adminNombre, adminCorreo) },
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
                titulo = if (editando) "Editar estancia" else "Nueva estancia",
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

                item { Spacer(Modifier.height(8.dp)) }
                item { TituloSeccion("Administrador") }
                item { CampoTexto("Nombre completo", adminNombre, { adminNombre = it }) }
                item { CampoTexto("Correo electrónico", adminCorreo, { adminCorreo = it }) }

                if (editando && instanciaInicial?.admin?.id != null) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { onResetPassword(instanciaInicial.admin.id) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Coral),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Coral)
                        ) {
                            Text("Generar nueva contraseña temporal")
                        }
                    }
                }
            }
        }
    }

    if (contrasenaGenerada != null) {
        AlertDialog(
            onDismissRequest = onCerrarDialogoContrasena,
            title = { Text("Contraseña Temporal") },
            text = {
                Column {
                    Text("Se ha generado una nueva contraseña temporal en el servidor.")
                    Spacer(Modifier.height(8.dp))
                    Text("Contraseña temporal:", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(contrasenaGenerada, style = MaterialTheme.typography.titleMedium, color = Coral, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Guarda esta contraseña, ya que el servidor la muestra solo en este momento.")
                }
            },
            confirmButton = {
                TextButton(onClick = onCerrarDialogoContrasena) {
                    Text("Aceptar", color = Coral, fontWeight = FontWeight.Bold)
                }
            }
        )
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
