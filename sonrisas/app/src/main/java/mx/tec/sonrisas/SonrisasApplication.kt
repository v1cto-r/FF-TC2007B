package mx.tec.sonrisas

import android.app.Application
import android.content.Context
import mx.tec.sonrisas.data.SesionRepository
import mx.tec.sonrisas.data.SonrisasRepository

class AppContainer(context: Context) {
    val sesionRepository: SesionRepository by lazy { SesionRepository() }
    val sonrisasRepository: SonrisasRepository by lazy { SonrisasRepository() }
}

class SonrisasApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
