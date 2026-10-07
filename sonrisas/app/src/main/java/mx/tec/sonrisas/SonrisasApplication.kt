package mx.tec.sonrisas

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mx.tec.sonrisas.data.local.dao.ChildDao
import mx.tec.sonrisas.data.local.dao.NoticeDao
import mx.tec.sonrisas.data.local.toEntity
import mx.tec.sonrisas.domain.Child
import mx.tec.sonrisas.domain.GroupEnum
import mx.tec.sonrisas.domain.Notice

private const val DAYCARE = "Estancia Mariposa"

private val seedChildren = listOf(
    Child(fullName = "Sofía Martínez", daycareCenter = DAYCARE, group = GroupEnum.P, parentName = "María López"),
    Child(fullName = "Regina López", daycareCenter = DAYCARE, group = GroupEnum.P, parentName = "María López"),
    Child(fullName = "Valentina Ruiz", daycareCenter = DAYCARE, group = GroupEnum.P, parentName = "Carla Ruiz"),
    Child(fullName = "Mateo García", daycareCenter = DAYCARE, group = GroupEnum.P, parentName = "Jorge García"),
    Child(fullName = "Camila Fernández", daycareCenter = DAYCARE, group = GroupEnum.P, parentName = "Ana Fernández")
)

private val seedNotices = listOf(
    Notice(id = "1", name = "Reunión de Padres", description = "Taller mensual de entrega de reportes - 4:00 PM", date = LocalDate.now().plusDays(2)),
    Notice(id = "2", name = "Vacunación programada", description = "Campaña nacional de salud infantil - 9:00 AM", date = LocalDate.now().plusDays(5))
)

@HiltAndroidApp
class SonrisasApplication : Application() {

    @Inject lateinit var childDao: ChildDao
    @Inject lateinit var noticeDao: NoticeDao

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (childDao.observeAll().first().isEmpty()) {
                    seedChildren.forEach { childDao.save(it.toEntity()) }
                }
                if (noticeDao.observeAll().first().isEmpty()) {
                    seedNotices.forEach { noticeDao.save(it.toEntity()) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
