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
 * There is no API integration or real connectivity detection yet: this
 * always reports offline, so every write flow falls back to Room. Once the
 * sync worker is implemented, this is the class that gets replaced (or made
 * dynamic) without touching a single repository.
 */
@Singleton
class AlwaysOfflineNetworkMonitor @Inject constructor() : NetworkMonitor {
    override fun isOnline(): Boolean = false
}
