package mx.tec.sonrisas.data.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mx.tec.sonrisas.MainActivity
import mx.tec.sonrisas.R
import mx.tec.sonrisas.data.AuthRepository
import mx.tec.sonrisas.data.ChatRepository
import mx.tec.sonrisas.data.local.dao.MessageDao

@AndroidEntryPoint
class PushService : FirebaseMessagingService() {
    @Inject lateinit var push: PushRepository
    @Inject lateinit var auth: AuthRepository
    @Inject lateinit var chats: ChatRepository
    @Inject lateinit var messages: MessageDao

    override fun onNewToken(token: String) {
        if (!auth.hasStoredSession) return
        CoroutineScope(Dispatchers.IO).launch { push.register(token) }
    }

    // The server sends data-only pushes with ids ({type, chat_id, message_id}). We fetch the chat
    // (authenticated) and build the notification here, so no message text goes through Google.
    override fun onMessageReceived(message: RemoteMessage) {
        val chatId = message.data["chat_id"] ?: return
        val messageId = message.data["message_id"] ?: return
        if (message.data["type"] != "message" || !auth.hasStoredSession) return

        runBlocking {
            try {
                chats.refresh(chatId)
            } catch (e: Exception) {
                // offline or session expired: still tell the user something arrived
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return

        val text = runBlocking { messages.getById(messageId) }?.content?.ifBlank { "Foto" } ?: "Tienes un mensaje nuevo"
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(CHANNEL, "Mensajes", NotificationManager.IMPORTANCE_HIGH))
        val open = PendingIntent.getActivity(
            this, messageId.hashCode(), Intent(this, MainActivity::class.java).putExtra(MainActivity.EXTRA_CHAT_ID, chatId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Nuevo mensaje")
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(this).notify(messageId.hashCode(), notification)
    }

    private companion object {
        const val CHANNEL = "mensajes"
    }
}
