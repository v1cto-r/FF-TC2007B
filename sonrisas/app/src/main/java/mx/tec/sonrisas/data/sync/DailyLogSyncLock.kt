package mx.tec.sonrisas.data.sync

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex

/**
 * Upload and download take turns. A download that read the server just before an upload landed
 * would otherwise overwrite (or delete) the row the upload had just marked as synced.
 */
@Singleton
class DailyLogSyncLock @Inject constructor() {
    val mutex = Mutex()
}
