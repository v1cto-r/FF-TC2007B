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
fun AdminInscripcionScreen(
    modifier: Modifier = Modifier,
    guardando: Boolean = false,
    errorMensaje: String? = null,
    onAtras: () -> Unit = {},
    onIniciar: (nombreTutor: String, apellidoTutor: String, correo: String, nombreAlumno: String) -> Unit =
        { _, _, _, _ -> }
) {
    FlagSecureDisable()
    var nombreTutor by remember { mutableStateOf("") }
    var apellidoTutor by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var nombreAlumno by remember { mutableStateOf("") }
    val correoValido = correo.contains("@") && correo.trim().length >= 5

    Scaffold(
        modifier = modifier,
        containerColor = Fondo,
        bottomBar = {
            BarraGuardar(
                texto = "Iniciar inscripción",
                habilitado = nombreTutor.isNotBlank() && correoValido && nombreAlumno.isNotBlank(),
                guardando = guardando,
                errorMensaje = errorMensaje,
                onClick = { onIniciar(nombreTutor, apellidoTutor, correo, nombreAlumno) }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBarAtras(
                titulo = "Inscripción",
                subtitulo = "Iniciar proceso de inscripción",
                onAtras = onAtras
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TituloSeccion("Padre o tutor")
                CampoTexto("Nombre", nombreTutor, { nombreTutor = it })
                CampoTexto("Apellido", apellidoTutor, { apellidoTutor = it })
                // BACKEND: POST /users exige correo (es el usuario con el que inicia sesión); el formulario original no lo pedía.
                CampoTexto("Correo electrónico", correo, { correo = it })

                Spacer(Modifier.height(8.dp))
                TituloSeccion("Alumno")
                CampoTexto("Nombre del alumno", nombreAlumno, { nombreAlumno = it })

                Text(
                    text = "Se creará la cuenta del padre o tutor con una contraseña temporal y el alumno quedará vinculado a él. El salón se asigna después.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoGris
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AdminInscripcionPreview() {
    SonrisasTheme { AdminInscripcionScreen() }
}