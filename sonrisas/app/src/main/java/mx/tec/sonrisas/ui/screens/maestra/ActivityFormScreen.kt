package mx.tec.sonrisas.ui.screens.maestra

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.ui.components.CampoTexto
import mx.tec.sonrisas.ui.components.EstadoVacio
import mx.tec.sonrisas.ui.components.FlagSecureDisable
import mx.tec.sonrisas.ui.components.FlagSecureEnable
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.theme.Coral
import mx.tec.sonrisas.ui.theme.Fondo

@Composable
fun ActivityFormScreen(
    hayIngreso: Boolean,
    onAtras: () -> Unit,
    onGuardar: (String) -> Unit,
    onIrACheckIn: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    FlagSecureDisable()
    var nombre by remember { mutableStateOf("") }

    Scaffold(modifier = modifier, containerColor = Fondo) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBarAtras(titulo = "Registrar Actividad", onAtras = onAtras)

            if (!hayIngreso) {
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    EstadoVacio(
                        titulo = "Registra el ingreso primero",
                        subtitulo = "Antes de capturar una actividad, este niño necesita un registro de ingreso hoy"
                    )
                    Button(
                        onClick = onIrACheckIn,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White)
                    ) {
                        Text("Ir a Registrar Asistencia")
                    }
                }
                return@Scaffold
            }

            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoTexto("Nombre de la actividad", nombre, { nombre = it }, singleLine = false)
                Button(
                    onClick = { onGuardar(nombre) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White)
                ) {
                    Text("Guardar Actividad")
                }
            }
        }
    }
}
