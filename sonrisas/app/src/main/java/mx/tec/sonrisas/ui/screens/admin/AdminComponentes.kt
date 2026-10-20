package mx.tec.sonrisas.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mx.tec.sonrisas.ui.components.EstadoCargando
import mx.tec.sonrisas.ui.components.EstadoError
import mx.tec.sonrisas.ui.components.EstadoVacio
import mx.tec.sonrisas.ui.components.ItemPersona
import mx.tec.sonrisas.ui.state.UiState
import mx.tec.sonrisas.ui.theme.*

/** Muestra cargando, error (con reintentar) o el contenido cuando ya llegaron los datos de la API. */
@Composable
fun <T> ConEstado(
    estado: UiState<T>,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier,
    onSalir: (() -> Unit)? = null,
    contenido: @Composable (T) -> Unit
) {
    when (estado) {
        UiState.Cargando -> Box(modifier.fillMaxSize().background(Fondo)) { EstadoCargando() }

        UiState.Vacio -> Box(modifier.fillMaxSize().background(Fondo).padding(16.dp)) {
            EstadoVacio(titulo = "Sin información", subtitulo = "Todavía no hay nada por mostrar")
        }

        is UiState.Error -> Column(modifier.fillMaxSize().background(Fondo)) {
            EstadoError(
                titulo = "Algo salió mal",
                mensaje = estado.mensaje,
                onReintentar = onReintentar,
                modifier = Modifier.weight(1f)
            )
            if (onSalir != null) {
                TextButton(
                    onClick = onSalir,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Cerrar sesión", style = MaterialTheme.typography.labelLarge, color = Coral)
                }
            }
        }

        is UiState.Exito -> contenido(estado.datos)
    }
}

/** Fila con botón de opción: para elegir una sola cosa (maestra, salón, destino). */
@Composable
fun FilaRadio(titulo: String, subtitulo: String, seleccionado: Boolean, onClick: () -> Unit) {
    ItemPersona(
        titulo = titulo,
        subtitulo = subtitulo,
        resaltado = seleccionado,
        onClick = onClick,
        trailing = {
            RadioButton(
                selected = seleccionado,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = Coral)
            )
        }
    )
}

/** Fila con casilla: para elegir varias cosas (alumnos). */
@Composable
fun FilaCheck(titulo: String, subtitulo: String, marcado: Boolean, onClick: () -> Unit) {
    ItemPersona(
        titulo = titulo,
        subtitulo = subtitulo,
        resaltado = marcado,
        onClick = onClick,
        trailing = {
            Checkbox(
                checked = marcado,
                onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(checkedColor = Coral)
            )
        }
    )
}

/** Barra inferior con el error (si lo hay) y el botón de guardar. */
@Composable
fun BarraGuardar(
    texto: String,
    habilitado: Boolean,
    guardando: Boolean,
    errorMensaje: String?,
    onClick: () -> Unit
) {
    Column(Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
        if (errorMensaje != null) {
            Text(
                text = errorMensaje,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(8.dp))
        }
        Button(
            onClick = onClick,
            enabled = habilitado && !guardando,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White)
        ) {
            if (guardando) {
                CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text(texto)
            }
        }
    }
}

/** Muestra la contraseña temporal de una cuenta recién creada. La API no la vuelve a mostrar. */
@Composable
fun AdminCredencialDialog(
    credencial: CredencialUi,
    titulo: String = "Cuenta creada",
    onCerrar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        containerColor = Color.White,
        title = {
            Text(titulo, style = MaterialTheme.typography.titleMedium, color = TextoOscuro)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(credencial.nombre, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = TextoOscuro)
                Text("Correo: ${credencial.correo}", style = MaterialTheme.typography.bodyMedium, color = TextoGris)
                Text("Contraseña temporal:", style = MaterialTheme.typography.bodyMedium, color = TextoGris)
                SelectionContainer {
                    Text(
                        text = credencial.contrasena,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CoralSuave, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                        color = TextoOscuro
                    )
                }
                Text(
                    text = "Compártela de forma segura. Solo se muestra esta vez y deberá cambiarla en su primer inicio de sesión.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoGris
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onCerrar) {
                Text("Listo", style = MaterialTheme.typography.labelLarge, color = Coral)
            }
        }
    )
}