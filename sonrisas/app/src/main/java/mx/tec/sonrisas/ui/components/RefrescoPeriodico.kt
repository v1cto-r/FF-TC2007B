package mx.tec.sonrisas.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

/**
 * Calls [onRefrescar] every [cadaMs] while the screen is on top and the app in the foreground, so a
 * parent watching the home screen sees what the teacher registers without reopening it. The server
 * sends no push for daily logs, so this is how they arrive.
 */
@Composable
fun RefrescoPeriodico(cadaMs: Long = 15_000L, onRefrescar: suspend () -> Unit) {
    val owner = LocalLifecycleOwner.current
    val refrescar by rememberUpdatedState(onRefrescar)
    LaunchedEffect(owner) {
        owner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(cadaMs)
                refrescar()
            }
        }
    }
}
