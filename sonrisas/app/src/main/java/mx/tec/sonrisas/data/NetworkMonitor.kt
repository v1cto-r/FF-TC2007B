package mx.tec.sonrisas.data

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth about whether there is a connection. Repositories
 * check it before deciding whether to write to Room or send straight to
 * the API.
 */
interface NetworkMonitor {
    fun isOnline(): Boolean
}

/**
 * There is no real connectivity detection yet: this always reports offline,
 * so the repositories that still use it (children, notices) write to Room.
 * Daily logs don't use it: they always go to Room and DailyLogSyncWorker
 * uploads them.
 */
@Singleton
class AlwaysOfflineNetworkMonitor @Inject constructor() : NetworkMonitor {
    override fun isOnline(): Boolean = false
}
