package mx.tec.sonrisas

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.AndroidEntryPoint
import mx.tec.sonrisas.ui.navigation.SonrisasApp
import mx.tec.sonrisas.ui.theme.SonrisasTheme

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SonrisasTheme {
                SonrisasApp()
            }
        }
    }
}
