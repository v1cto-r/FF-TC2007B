package mx.tec.sonrisas.ui.screens.maestra

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.time.LocalDate
import mx.tec.sonrisas.domain.SemanaReporte
import mx.tec.sonrisas.domain.semanasDisponibles
import mx.tec.sonrisas.ui.components.CampoDropdown
import mx.tec.sonrisas.ui.components.TopBarAtras
import mx.tec.sonrisas.ui.theme.AmarilloSuave
import mx.tec.sonrisas.ui.theme.Coral
import mx.tec.sonrisas.ui.theme.Fondo
import mx.tec.sonrisas.ui.theme.SonrisasTheme
import mx.tec.sonrisas.ui.theme.TextoGris
import mx.tec.sonrisas.ui.theme.TextoOscuro

@Composable
fun ReporteNinoScreen(
    nombreNino: String?,
    semanas: List<SemanaReporte>,
    seleccionada: SemanaReporte,
    generando: Boolean,
    error: String?,
    pendientes: Int,
    onSeleccionar: (SemanaReporte) -> Unit,
    onGenerar: () -> Unit,
    onAtras: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(modifier = modifier, containerColor = Fondo) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopBarAtras(
                titulo = "Reporte semanal",
                subtitulo = nombreNino,
                onAtras = onAtras
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Elige la semana del reporte. Incluye la bitácora completa de lunes a viernes: " +
                        "ingreso, estadía, actividades y egreso.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoGris
                )
                CampoDropdown(
                    etiqueta = "Semana",
                    valorSeleccionado = seleccionada.etiqueta,
                    opciones = semanas.map { it.etiqueta },
                    onSeleccionar = { etiqueta -> semanas.firstOrNull { it.etiqueta == etiqueta }?.let(onSeleccionar) }
                )
                Text(
                    text = "Cubre: ${seleccionada.cobertura}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextoOscuro
                )

                if (pendientes > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = AmarilloSuave)
                    ) {
                        Text(
                            text = "Hay $pendientes registro(s) sin sincronizar en este teléfono. " +
                                "No aparecerán en el reporte hasta que se suban; conéctate a internet y vuelve a intentarlo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextoOscuro,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (error != null) {
                    Text(text = error, style = MaterialTheme.typography.bodySmall, color = Coral)
                }

                Button(
                    onClick = onGenerar,
                    enabled = !generando,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White)
                ) {
                    if (generando) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Text("Obtener reporte")
                    }
                }
            }
        }
    }
}

/** Opens the PDF in a viewer app; if there is none, offers to share it instead. */
fun abrirPdf(context: Context, archivo: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
    val ver = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, "application/pdf")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    val compartir = Intent(Intent.ACTION_SEND)
        .setType("application/pdf")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        // Without a chooser: a missing viewer throws instead of showing an empty chooser
        context.startActivity(ver)
    } catch (e: ActivityNotFoundException) {
        context.startActivity(Intent.createChooser(compartir, "Compartir reporte"))
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ReporteNinoPreview() {
    val semanas = semanasDisponibles(LocalDate.of(2026, 10, 21), cuantas = 5)
    SonrisasTheme {
        ReporteNinoScreen(
            nombreNino = "Sofía Martínez",
            semanas = semanas,
            seleccionada = semanas.first(),
            generando = false,
            error = null,
            pendientes = 2,
            onSeleccionar = {},
            onGenerar = {},
            onAtras = {}
        )
    }
}
