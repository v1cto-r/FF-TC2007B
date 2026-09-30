package mx.tec.sonrisas.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mx.tec.sonrisas.data.SesionRepository

class SesionViewModel(private val repository: SesionRepository) : ViewModel() {

    val estado: StateFlow<SesionEstado> = repository.sesion
        .map { sesion ->
            if (sesion == null) SesionEstado.Anonimo else SesionEstado.Activa(sesion)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = SesionEstado.Anonimo
        )

    fun salir() {
        viewModelScope.launch {
            repository.salir()
        }
    }
}
