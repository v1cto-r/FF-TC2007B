package mx.tec.sonrisas.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.ui.components.CampoTexto
import mx.tec.sonrisas.ui.components.FlagSecureDisable
import mx.tec.sonrisas.ui.components.TituloSeccion
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.theme.*

@Composable
fun AdminCrearUsuarioScreen(
    titulo: String,
    subtitulo: String,
    modifier: Modifier = Modifier,
    guardando: Boolean = false,
    errorMensaje: String? = null,
    onAtras: () -> Unit = {},
    onCrear: (nombre: String, apellido: String, correo: String) -> Unit = { _, _, _ -> }
) {
    FlagSecureDisable()
    var nombre by remember { mutableStateOf("") }
    var apellido by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    val correoValido = correo.contains("@") && correo.trim().length >= 5

    Scaffold(
        modifier = modifier,
        containerColor = Fondo,
        bottomBar = {
            BarraGuardar(
                texto = "Crear cuenta",
                habilitado = nombre.isNotBlank() && correoValido,
                guardando = guardando,
                errorMensaje = errorMensaje,
                onClick = { onCrear(nombre, apellido, correo) }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBarAtras(titulo = titulo, subtitulo = subtitulo, onAtras = onAtras)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TituloSeccion("Datos de la persona")
                CampoTexto("Nombre", nombre, { nombre = it })
                CampoTexto("Apellido", apellido, { apellido = it })
                CampoTexto("Correo electrónico", correo, { correo = it })
                Text(
                    text = "Al crear la cuenta se genera una contraseña temporal que tendrás que compartirle. Deberá cambiarla en su primer inicio de sesión.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoGris
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AdminCrearUsuarioPreview() {
    SonrisasTheme {
        AdminCrearUsuarioScreen(titulo = "Crear maestra", subtitulo = "Nueva cuenta de docente")
    }
}