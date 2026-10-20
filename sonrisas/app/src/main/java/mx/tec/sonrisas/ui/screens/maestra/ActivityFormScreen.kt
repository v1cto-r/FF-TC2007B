package mx.tec.sonrisas.ui.screens.maestra

import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import java.io.File
import mx.tec.sonrisas.data.StoredPhoto
import mx.tec.sonrisas.ui.components.CampoTexto
import mx.tec.sonrisas.ui.components.EstadoVacio
import mx.tec.sonrisas.ui.components.FlagSecureDisable
import mx.tec.sonrisas.ui.components.FlagSecureEnable
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.components.rememberTomarFoto
import mx.tec.sonrisas.ui.theme.Coral
import mx.tec.sonrisas.ui.theme.Fondo

@Composable
fun ActivityFormScreen(
    hayIngreso: Boolean,
    onAtras: () -> Unit,
    fotos: List<StoredPhoto> = emptyList(),
    leyendoFoto: Boolean = false,
    onTomarFoto: (Uri) -> Unit = {},
    onQuitarFoto: (StoredPhoto) -> Unit = {},
    onGuardar: (String) -> Unit,
    onIrACheckIn: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    FlagSecureDisable()
    var nombre by remember { mutableStateOf("") }
    val tomarFoto = rememberTomarFoto(onTomarFoto)

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
                if (fotos.isNotEmpty()) {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        fotos.forEach { foto ->
                            Box {
                                AsyncImage(
                                    model = File(foto.path),
                                    contentDescription = "Foto de la actividad",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(96.dp).clip(RoundedCornerShape(10.dp))
                                )
                                IconButton(
                                    onClick = { onQuitarFoto(foto) },
                                    modifier = Modifier.align(Alignment.TopEnd).size(28.dp)
                                ) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Quitar foto", tint = Color.White)
                                }
                            }
                        }
                    }
                }
                OutlinedButton(
                    onClick = tomarFoto,
                    enabled = !leyendoFoto && fotos.size < MAX_FOTOS,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.AddAPhoto, contentDescription = null)
                    Text(if (leyendoFoto) "  Procesando foto…" else "  Agregar foto")
                }
                Button(
                    onClick = { onGuardar(nombre) },
                    enabled = nombre.isNotBlank() && !leyendoFoto,
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

// The server takes up to 20 photos per activity
private const val MAX_FOTOS = 20
