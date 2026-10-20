package mx.tec.sonrisas.ui.screens.maestra

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import java.io.File
import mx.tec.sonrisas.domain.Child
import mx.tec.sonrisas.ui.components.AvatarCircular
import mx.tec.sonrisas.ui.components.CampoTexto
import mx.tec.sonrisas.ui.components.FlagSecureDisable
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.components.chat.SelectorFuenteFotoDialog
import mx.tec.sonrisas.ui.theme.*

@Composable
fun BroadcastFormScreen(
    titulo: String,
    descripcionAyuda: String,
    textoBoton: String = "Enviar a los presentes",
    ninosPresentes: List<Child> = emptyList(),
    permitirSeleccionAdjuntados: Boolean = true,
    onAtras: () -> Unit,
    onEnviar: ((titulo: String, mensaje: String) -> Unit)? = null,
    onEnviarATodos: (titulo: String, mensaje: String, fotoUri: Uri?) -> Unit = { t, m, _ -> onEnviar?.invoke(t, m) },
    onEnviarAAdjuntados: (titulo: String, mensaje: String, fotoUri: Uri?, ninosSeleccionados: List<Child>) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    FlagSecureDisable()
    val context = LocalContext.current

    var tituloMensaje by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }
    var fotoUri by remember { mutableStateOf<Uri?>(null) }
    var mostrarMenuFoto by remember { mutableStateOf(false) }
    var mostrarDialogoSeleccion by remember { mutableStateOf(false) }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var enviando by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { fotoUri = it }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { exito ->
        if (exito) {
            tempPhotoUri?.let { uri -> fotoUri = uri }
        }
    }

    val abrirCamara = {
        try {
            val file = File(context.cacheDir, "actividad_foto_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            tempPhotoUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    if (mostrarMenuFoto && !enviando) {
        SelectorFuenteFotoDialog(
            onDismiss = { mostrarMenuFoto = false },
            onCamara = abrirCamara,
            onGaleria = { galleryLauncher.launch("image/*") }
        )
    }

    if (mostrarDialogoSeleccion) {
        DialogoSeleccionNinosPresentes(
            ninos = ninosPresentes,
            enviando = enviando,
            onDismiss = { if (!enviando) mostrarDialogoSeleccion = false },
            onConfirmar = { seleccionados ->
                enviando = true
                onEnviarAAdjuntados(tituloMensaje, mensaje, fotoUri, seleccionados)
            }
        )
    }

    val tieneFoto = fotoUri != null
    val textoBotonDinamico = if (enviando) {
        "Enviando..."
    } else if (tieneFoto && permitirSeleccionAdjuntados) {
        "Enviar a los adjuntados"
    } else {
        textoBoton
    }

    Scaffold(modifier = modifier, containerColor = Fondo) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TopBarAtras(titulo = titulo, onAtras = { if (!enviando) onAtras() })
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(descripcionAyuda, style = MaterialTheme.typography.bodySmall, color = TextoGris)
                CampoTexto("Título", tituloMensaje, { if (!enviando) tituloMensaje = it })
                CampoTexto("Mensaje / Detalle", mensaje, { if (!enviando) mensaje = it }, singleLine = false)

                // Sección Opcional: Fotografía
                Text(
                    text = "Fotografía (Opcional)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextoOscuro
                )

                if (fotoUri != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Fondo)
                            ) {
                                AsyncImage(
                                    model = fotoUri,
                                    contentDescription = "Foto adjunta",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = Verde,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Foto adjuntada",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextoOscuro
                                    )
                                }
                                if (!enviando) {
                                    Row {
                                        TextButton(onClick = { mostrarMenuFoto = true }) {
                                            Text("Cambiar", color = Coral, style = MaterialTheme.typography.labelSmall)
                                        }
                                        TextButton(onClick = { fotoUri = null }) {
                                            Text("Eliminar", color = TextoGris, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { mostrarMenuFoto = true },
                        enabled = !enviando,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.AddAPhoto, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("📷 Adjuntar foto (Opcional)")
                    }
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (tieneFoto && permitirSeleccionAdjuntados) {
                            mostrarDialogoSeleccion = true
                        } else {
                            enviando = true
                            onEnviarATodos(tituloMensaje, mensaje, fotoUri)
                        }
                    },
                    enabled = !enviando && (tituloMensaje.isNotBlank() || mensaje.isNotBlank() || tieneFoto),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White)
                ) {
                    if (enviando) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(textoBotonDinamico, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DialogoSeleccionNinosPresentes(
    ninos: List<Child>,
    enviando: Boolean = false,
    onDismiss: () -> Unit,
    onConfirmar: (List<Child>) -> Unit
) {
    var seleccionados by remember { mutableStateOf(ninos.map { it.id }.toSet()) }

    AlertDialog(
        onDismissRequest = { if (!enviando) onDismiss() },
        containerColor = Color.White,
        title = {
            Text(
                text = "Seleccionar niños en la foto",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextoOscuro
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
            ) {
                Text(
                    text = "Elige los alumnos que aparecen en la fotografía. La foto y la actividad se enviarán únicamente a sus familiares.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoGris
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { if (!enviando) seleccionados = ninos.map { it.id }.toSet() }, enabled = !enviando) {
                        Text("Seleccionar Todos", color = Coral, style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(onClick = { if (!enviando) seleccionados = emptySet() }, enabled = !enviando) {
                        Text("Deseleccionar Todos", color = TextoGris, style = MaterialTheme.typography.labelSmall)
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    items(ninos) { child ->
                        val estaSeleccionado = child.id in seleccionados
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = !enviando) {
                                    seleccionados = if (estaSeleccionado) {
                                        seleccionados - child.id
                                    } else {
                                        seleccionados + child.id
                                    }
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = estaSeleccionado,
                                enabled = !enviando,
                                onCheckedChange = { check ->
                                    seleccionados = if (check) {
                                        seleccionados + child.id
                                    } else {
                                        seleccionados - child.id
                                    }
                                },
                                colors = CheckboxDefaults.colors(checkedColor = Coral)
                            )
                            Spacer(Modifier.width(6.dp))
                            AvatarCircular(tamano = 32.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = child.fullName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextoOscuro
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val listaSeleccionados = ninos.filter { it.id in seleccionados }
                    onConfirmar(listaSeleccionados)
                },
                enabled = !enviando && seleccionados.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White)
            ) {
                if (enviando) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(if (enviando) "Enviando..." else "Enviar a (${seleccionados.size}) adjuntados")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !enviando) {
                Text("Cancelar", color = TextoGris)
            }
        }
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun BroadcastFormPreview() {
    SonrisasTheme {
        BroadcastFormScreen(
            titulo = "Actividad para todos",
            descripcionAyuda = "Se enviará como mensaje de chat a los padres de todos los niños presentes hoy.",
            onAtras = {}
        )
    }
}
