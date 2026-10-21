package mx.tec.sonrisas.domain

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageTest {

    @Test
    fun `default message has sent false and deleted false`() {
        val now = LocalDateTime.now()
        val message = Message(
            chatId = "child_123",
            authorId = "parent_456",
            content = "Hola maestra, buenos días.",
            sentAt = now
        )

        assertFalse(message.sent)
        assertFalse(message.synced)
        assertFalse(message.isAviso)
        assertFalse(message.deleted)
        assertFalse(message.deletePending)
        assertEquals("child_123", message.chatId)
        assertEquals("parent_456", message.authorId)
        assertEquals("Hola maestra, buenos días.", message.content)
    }

    @Test
    fun `aviso message has isAviso true`() {
        val now = LocalDateTime.now()
        val aviso = Message(
            chatId = "child_123",
            authorId = "teacher_789",
            content = "Aviso: Mañana juego con agua al aire libre.",
            sentAt = now,
            isAviso = true
        )

        assertTrue(aviso.isAviso)
    }

    @Test
    fun `message with attachment file id stores attachment file id`() {
        val now = LocalDateTime.now()
        val photoMessage = Message(
            chatId = "child_123",
            authorId = "teacher_789",
            content = "Foto de la actividad de pintura",
            sentAt = now,
            attachmentFileId = "file_uuid_abc_123"
        )

        assertEquals("file_uuid_abc_123", photoMessage.attachmentFileId)
        assertEquals("Foto de la actividad de pintura", photoMessage.content)
    }
}
