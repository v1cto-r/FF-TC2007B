package mx.tec.sonrisas.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EsquemaClaro = lightColorScheme(
    primary = Coral,
    onPrimary = Color.White,
    primaryContainer = CoralSuave,
    onPrimaryContainer = TextoOscuro,

    secondary = Verde,
    onSecondary = Color.White,
    secondaryContainer = VerdeSuave,
    onSecondaryContainer = TextoOscuro,

    tertiary = Amarillo,
    onTertiary = Color.White,
    tertiaryContainer = AmarilloSuave,
    onTertiaryContainer = TextoOscuro,

    background = Fondo,
    onBackground = TextoOscuro,

    surface = Color.White,
    onSurface = TextoOscuro,
    surfaceVariant = Fondo,
    onSurfaceVariant = TextoGris,

    outline = BordeCampo,
    outlineVariant = BordeCampo
)

@Composable
fun SonrisasTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EsquemaClaro,
        typography = Tipografia,
        content = content
    )
}