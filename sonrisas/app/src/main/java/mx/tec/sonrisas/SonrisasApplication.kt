package mx.tec.sonrisas

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mx.tec.sonrisas.data.local.dao.ChildDao
import mx.tec.sonrisas.data.local.toEntity
import mx.tec.sonrisas.domain.Child
import mx.tec.sonrisas.domain.GroupEnum

private const val DAYCARE = "Estancia Mariposa"

private val seedChildren = listOf(
    Child(fullName = "Sofía Martínez", daycareCenter = DAYCARE, group = GroupEnum.P, parentName = "María López"),
    Child(fullName = "Regina López", daycareCenter = DAYCARE, group = GroupEnum.P, parentName = "María López"),
    Child(fullName = "Valentina Ruiz", daycareCenter = DAYCARE, group = GroupEnum.P, parentName = "Carla Ruiz"),
    Child(fullName = "Mateo García", daycareCenter = DAYCARE, group = GroupEnum.P, parentName = "Jorge García"),
    Child(fullName = "Camila Fernández", daycareCenter = DAYCARE, group = GroupEnum.P, parentName = "Ana Fernández")
)

@HiltAndroidApp
class SonrisasApplication : Application() {

    @Inject lateinit var childDao: ChildDao

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (childDao.observeAll().first().isEmpty()) {
                    seedChildren.forEach { childDao.save(it.toEntity()) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
