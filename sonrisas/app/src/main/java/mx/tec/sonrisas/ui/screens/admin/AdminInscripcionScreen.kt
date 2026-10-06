package mx.tec.sonrisas.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import mx.tec.sonrisas.ui.components.BottomBar
import mx.tec.sonrisas.ui.components.CampoTexto
import mx.tec.sonrisas.ui.components.FlagSecureDisable
import mx.tec.sonrisas.ui.components.TituloSeccion
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.theme.*

@Composable
fun AdminInscripcionScreen(
    modifier: Modifier = Modifier,
    onIniciar: () -> Unit = {},
    onTab: (Int) -> Unit = {}
) {
    FlagSecureDisable()
    var nombreTutor by remember { mutableStateOf("") }
    var apellidoTutor by remember { mutableStateOf("") }
    var nombreAlumno by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier,
        containerColor = Fondo,
        bottomBar = { BottomBar(tabs = TabsAdmin, seleccionado = 2, onSeleccionar = onTab) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBarAtras(titulo = "Inscripción", subtitulo = "Iniciar proceso de inscripción")

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

                Spacer(Modifier.height(8.dp))
                TituloSeccion("Alumno")
                CampoTexto("Nombre del alumno", nombreAlumno, { nombreAlumno = it })

                Button(
                    onClick = onIniciar,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White)
                ) {
                    Text("Iniciar inscripción")
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AdminInscripcionPreview() {
    SonrisasTheme { AdminInscripcionScreen() }
}