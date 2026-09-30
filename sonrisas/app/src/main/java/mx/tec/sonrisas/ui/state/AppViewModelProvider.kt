package mx.tec.sonrisas.ui.state

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import mx.tec.sonrisas.SonrisasApplication

object AppViewModelProvider {

    val Factory = viewModelFactory {
        initializer {
            SesionViewModel(sonrisasApplication().container.sesionRepository)
        }
        initializer {
            LoginViewModel(sonrisasApplication().container.sesionRepository)
        }
        initializer {
            MaestraViewModel(sonrisasApplication().container.sonrisasRepository)
        }
        initializer {
            PadreViewModel(sonrisasApplication().container.sonrisasRepository)
        }
    }
}

private fun CreationExtras.sonrisasApplication(): SonrisasApplication =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SonrisasApplication
