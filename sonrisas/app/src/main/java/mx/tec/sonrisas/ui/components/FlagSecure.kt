package mx.tec.sonrisas.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

/**
 * Habilita FLAG_SECURE en la ventana actual para bloquear capturas de pantalla,
 * grabación de pantalla y vistas previas en la lista de aplicaciones recientes.
 *
 * Utiliza [DisposableEffect] para garantizar que cuando la pantalla sale de la
 * composición al navegar a otra parte, FLAG_SECURE se remueva automáticamente.
 */
@Composable
fun FlagSecureEnable(view: View = LocalView.current) {
    DisposableEffect(view) {
        val activity = view.context.findActivity()

        activity?.window?.addFlags(
            WindowManager.LayoutParams.FLAG_SECURE
        )

        onDispose {
            activity?.window?.clearFlags(
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
    }
}

/**
 * Deshabilita explícitamente FLAG_SECURE en pantallas abiertas donde el usuario
 * puede tomar capturas de pantalla o compartir información pública.
 */
@Composable
fun FlagSecureDisable(view: View = LocalView.current) {
    SideEffect {
        val activity = view.context.findActivity()

        activity?.window?.clearFlags(
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }
}
