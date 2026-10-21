package mx.tec.sonrisas.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChildTest {

    @Test
    fun `child creation has default uuid and default attributes`() {
        val child = Child(
            fullName = "Sofía Martínez",
            daycareCenter = "Sonrisas de Cristal",
            group = GroupEnum.P,
            parentName = "María López"
        )

        assertNotNull(child.id)
        assertTrue(child.id.isNotBlank())
        assertEquals("Sofía Martínez", child.fullName)
        assertEquals("Sonrisas de Cristal", child.daycareCenter)
        assertEquals(GroupEnum.P, child.group)
        assertEquals("María López", child.parentName)
    }

    @Test
    fun `profileCompleted is false when profileCompletedAt is null or blank`() {
        val pendingChildNull = Child(
            fullName = "Mateo García",
            daycareCenter = "Sonrisas de Cristal",
            group = GroupEnum.M,
            parentName = "Carlos García",
            profileCompletedAt = null
        )

        val pendingChildBlank = Child(
            fullName = "Mateo García",
            daycareCenter = "Sonrisas de Cristal",
            group = GroupEnum.M,
            parentName = "Carlos García",
            profileCompletedAt = ""
        )

        assertFalse(pendingChildNull.profileCompleted)
        assertFalse(pendingChildBlank.profileCompleted)
    }

    @Test
    fun `profileCompleted is true when profileCompletedAt is valid timestamp`() {
        val completedChild = Child(
            fullName = "Valentina Ruiz",
            daycareCenter = "Sonrisas de Cristal",
            group = GroupEnum.L,
            parentName = "Ana Ruiz",
            profileCompletedAt = "2026-10-19T10:00:00Z"
        )

        assertTrue(completedChild.profileCompleted)
    }
}
