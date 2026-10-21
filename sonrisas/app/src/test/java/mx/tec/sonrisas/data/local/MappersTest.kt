package mx.tec.sonrisas.data.local

import java.time.LocalDateTime
import mx.tec.sonrisas.data.local.entities.ChildEntity
import mx.tec.sonrisas.domain.GroupEnum
import mx.tec.sonrisas.domain.Message
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MappersTest {

    @Test
    fun `ChildEntity toDomain and Child toEntity roundtrip correctly`() {
        val entity = ChildEntity(
            id = "c_99",
            fullName = "Sofía Martínez",
            daycareCenter = "Sonrisas de Cristal",
            group = "P",
            parentName = "María López",
            birthdate = "2021-03-12",
            gender = "Femenino",
            allergies = "Lactosa",
            bloodType = "O+",
            emergencyContactPhone = "555-1234",
            profileCompletedAt = "2026-10-19T10:00:00Z"
        )

        val domain = entity.toDomain()

        assertEquals("c_99", domain.id)
        assertEquals("Sofía Martínez", domain.fullName)
        assertEquals("Sonrisas de Cristal", domain.daycareCenter)
        assertEquals(GroupEnum.P, domain.group)
        assertEquals("María López", domain.parentName)
        assertEquals("2021-03-12", domain.birthdate)
        assertEquals("Lactosa", domain.allergies)
        assertTrue(domain.profileCompleted)

        val backToEntity = domain.toEntity()

        assertEquals(entity.id, backToEntity.id)
        assertEquals(entity.fullName, backToEntity.fullName)
        assertEquals(entity.group, backToEntity.group)
        assertEquals(entity.profileCompletedAt, backToEntity.profileCompletedAt)
    }

    @Test
    fun `MessageEntity toDomain and Message toEntity maintain chat and aviso attributes`() {
        val now = LocalDateTime.now()
        val message = Message(
            id = "m_50",
            chatId = "child_12",
            authorId = "teacher_1",
            content = "Aviso general de la sala",
            sentAt = now,
            sent = true,
            synced = true,
            isAviso = true,
            deleted = false,
            attachmentFileId = "photo_123"
        )

        val entity = message.toEntity()

        assertEquals("m_50", entity.id)
        assertEquals("child_12", entity.chatId)
        assertEquals("teacher_1", entity.authorId)
        assertEquals("Aviso general de la sala", entity.content)
        assertTrue(entity.sent)
        assertTrue(entity.synced)
        assertTrue(entity.isAviso)
        assertFalse(entity.deleted)
        assertEquals("photo_123", entity.attachmentFileId)

        val backToDomain = entity.toDomain()

        assertEquals(message.id, backToDomain.id)
        assertEquals(message.chatId, backToDomain.chatId)
        assertEquals(message.content, backToDomain.content)
        assertTrue(backToDomain.isAviso)
    }
}
