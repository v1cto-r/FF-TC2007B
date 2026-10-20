package mx.tec.sonrisas

import android.Manifest
import android.os.Build
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import mx.tec.sonrisas.data.SesionRepository
import mx.tec.sonrisas.ui.navigation.SonrisasApp
import mx.tec.sonrisas.ui.theme.SonrisasTheme

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject lateinit var sesionRepository: SesionRepository

    // chat that should open (a child's chat id is the child id) (set when the user taps a push notification)
    private var chatPendiente by mutableStateOf<String?>(null)

    override fun onStart() {
        super.onStart()
        sesionRepository.alPrimerPlano()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chatPendiente = intent.getStringExtra(EXTRA_CHAT_ID)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 33) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
                .launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            SonrisasTheme {
                SonrisasApp(chatPendiente = chatPendiente, onChatAbierto = { chatPendiente = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        chatPendiente = intent.getStringExtra(EXTRA_CHAT_ID)
    }

    companion object {
        const val EXTRA_CHAT_ID = "chat_id"
    }
}
