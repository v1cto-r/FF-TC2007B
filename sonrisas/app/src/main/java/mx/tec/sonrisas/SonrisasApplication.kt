package mx.tec.sonrisas

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import mx.tec.sonrisas.data.sync.SyncScheduler

@HiltAndroidApp
class SonrisasApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var syncScheduler: SyncScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // Picks up whatever was saved offline in a previous run and never made it to the server
        syncScheduler.requestSync()
    }
}
