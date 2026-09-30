package mx.tec.sonrisas.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import mx.tec.sonrisas.data.AlwaysOfflineNetworkMonitor
import mx.tec.sonrisas.data.NetworkMonitor

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {

    @Binds
    @Singleton
    abstract fun bindNetworkMonitor(impl: AlwaysOfflineNetworkMonitor): NetworkMonitor
}
