package mx.tec.sonrisas.model

sealed interface UiState<out T> {
    data object Cargando : UiState<Nothing>
    data object Vacio : UiState<Nothing>
    data class Error(val mensaje: String = "No pudimos cargar la información") : UiState<Nothing>
    data class Exito<T>(val datos: T) : UiState<T>
}