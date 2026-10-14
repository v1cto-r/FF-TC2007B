package mx.tec.sonrisas

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import mx.tec.sonrisas.data.SesionRepository
import mx.tec.sonrisas.ui.navigation.SonrisasApp
import mx.tec.sonrisas.ui.theme.SonrisasTheme

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject lateinit var sesionRepository: SesionRepository

    override fun onStart() {
        super.onStart()
        sesionRepository.alPrimerPlano()
    }

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
