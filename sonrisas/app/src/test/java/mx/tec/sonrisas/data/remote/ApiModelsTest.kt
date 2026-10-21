package mx.tec.sonrisas.data.remote

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiModelsTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `TokenPairDto deserializes correctly`() {
        val jsonText = """{"access_token":"token_123","refresh_token":"refresh_456","pending":true}"""
        val dto = json.decodeFromString<TokenPairDto>(jsonText)

        assertEquals("token_123", dto.accessToken)
        assertEquals("refresh_456", dto.refreshToken)
        assertTrue(dto.pending)
    }

    @Test
    fun `ChildDto deserializes with profileCompletedAt and allergies`() {
        val jsonText = """
            {
                "id": "c_123",
                "full_name": "Sofía Martínez",
                "parent_id": "p_456",
                "profile_completed_at": "2026-10-19T11:00:00Z",
                "birth_date": "2021-03-12",
                "allergies": ["Lactosa", "Penicilina"]
            }
        """.trimIndent()

        val dto = json.decodeFromString<ChildDto>(jsonText)

        assertEquals("c_123", dto.id)
        assertEquals("Sofía Martínez", dto.fullName)
        assertEquals("p_456", dto.parentId)
        assertEquals("2026-10-19T11:00:00Z", dto.profileCompletedAt)
        assertEquals("2021-03-12", dto.birthDate)
        assertEquals(listOf("Lactosa", "Penicilina"), dto.allergies)
    }

    @Test
    fun `ChildProfilePatchBody serializes to JSON correctly`() {
        val body = ChildProfilePatchBody(
            birthDate = "2021-03-12",
            allergies = listOf("Cacahuates")
        )

        val jsonOutput = json.encodeToString(body)

        assertTrue(jsonOutput.contains("2021-03-12"))
        assertTrue(jsonOutput.contains("Cacahuates"))
        assertTrue(jsonOutput.contains("birth_date"))
    }

    @Test
    fun `DocumentInBody serializes with title field`() {
        val body = DocumentInBody(
            childId = "c_123",
            fileId = "f_789",
            title = "Acta de nacimiento"
        )

        val jsonOutput = json.encodeToString(body)

        assertTrue(jsonOutput.contains("child_id"))
        assertTrue(jsonOutput.contains("file_id"))
        assertTrue(jsonOutput.contains("title"))
        assertTrue(jsonOutput.contains("Acta de nacimiento"))
    }

    @Test
    fun `MessageDto toDomain converts timestamps and handles image markers`() {
        val dto = MessageDto(
            id = "m_100",
            chatId = "child_123",
            authorId = "teacher_1",
            content = "Actividad de lectura\n::Image /11111111-2222-3333-4444-555555555555.jpg",
            sentAt = "2026-10-19T12:00:00Z",
            createdAt = "2026-10-19T12:00:00Z",
            isAviso = false,
            attachmentFileId = null
        )

        val domain = dto.toDomain()

        assertEquals("m_100", domain.id)
        assertEquals("child_123", domain.chatId)
        assertEquals("teacher_1", domain.authorId)
        assertEquals("Actividad de lectura", domain.content.trim())
        assertEquals("11111111-2222-3333-4444-555555555555", domain.attachmentFileId)
        assertTrue(domain.sent)
        assertTrue(domain.synced)
        assertFalse(domain.isAviso)
    }
    @Test
    fun `MenuCheckBody serializes meals with their ingredients`() {
        val body = MenuCheckBody(listOf(MealBody("Pollo con mole", listOf("cacahuate")), MealBody("Hot cakes")))
        assertEquals(
            """{"meals":[{"name":"Pollo con mole","ingredients":["cacahuate"]},{"name":"Hot cakes","ingredients":[]}]}""",
            json.encodeToString(body)
        )
    }

    @Test
    fun `MenuCheckDto deserializes detected allergens and child conflicts`() {
        val jsonText = """
            {
                "detected": [{"name": "Cacahuate", "probability": 0.92, "meals": ["Pollo con mole"]}],
                "conflicts": [{
                    "child_id": "c_1",
                    "full_name": "Sofía Martínez",
                    "salon_name": null,
                    "conflicts": [{"allergen": "Cacahuate", "meals": ["Pollo con mole"]}]
                }]
            }
        """.trimIndent()

        val dto = json.decodeFromString<MenuCheckDto>(jsonText)
        assertEquals(0.92, dto.detected.single().probability, 0.0001)
        val nino = dto.conflicts.single()
        assertEquals("c_1", nino.childId)
        assertEquals(null, nino.salonName)
        assertEquals(AllergenConflictDto("Cacahuate", listOf("Pollo con mole")), nino.conflicts.single())
    }

    @Test
    fun `AllergenDto deserializes the catalog`() {
        val jsonText = """[{"id": "a_1", "name": "Huevo", "keywords": "huevo, clara"}]"""
        assertEquals(listOf(AllergenDto("a_1", "Huevo", "huevo, clara")), json.decodeFromString<List<AllergenDto>>(jsonText))
    }
}
