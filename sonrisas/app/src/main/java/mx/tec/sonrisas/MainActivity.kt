package mx.tec.sonrisas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mx.tec.sonrisas.model.Mocks
import mx.tec.sonrisas.model.UiState
import mx.tec.sonrisas.ui.screens.login.LoginScreen
import mx.tec.sonrisas.ui.screens.maestra.ChatMaestraScreen
import mx.tec.sonrisas.ui.screens.maestra.InicioMaestraScreen
import mx.tec.sonrisas.ui.screens.maestra.NinosScreen
import mx.tec.sonrisas.ui.screens.maestra.PerfilNinoScreen
import mx.tec.sonrisas.ui.screens.padre.ChatPadreScreen
import mx.tec.sonrisas.ui.screens.padre.InicioPadreScreen
import mx.tec.sonrisas.ui.theme.SonrisasTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SonrisasTheme {
                LoginScreen()
                // InicioMaestraScreen(UiState.Exito(Mocks.eventos), presentes = 18, total = 20)
                //NinosScreen(UiState.Exito(Mocks.ninos))
                //PerfilNinoScreen(UiState.Exito(Mocks.ninos.first()))
                // ChatMaestraScreen(UiState.Exito(Mocks.mensajesMaestra))
                // InicioPadreScreen(UiState.Exito(Mocks.actividades))
                // ChatPadreScreen(UiState.Exito(Mocks.mensajesPadre))
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}