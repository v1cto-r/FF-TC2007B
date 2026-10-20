package mx.tec.sonrisas.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import mx.tec.sonrisas.data.ChatRepository
import mx.tec.sonrisas.data.ChatRepositoryImpl
import mx.tec.sonrisas.data.ChildRepository
import mx.tec.sonrisas.data.ChildRepositoryImpl
import mx.tec.sonrisas.data.DailyLogRepository
import mx.tec.sonrisas.data.DailyLogRepositoryImpl
import mx.tec.sonrisas.data.NoticeRepository
import mx.tec.sonrisas.data.NoticeRepositoryImpl
import mx.tec.sonrisas.data.PickupAuthorizationRepository
import mx.tec.sonrisas.data.PickupAuthorizationRepositoryImpl
import mx.tec.sonrisas.data.remote.ChildRemoteDataSource
import mx.tec.sonrisas.data.remote.ChildRemoteDataSourceImpl
import mx.tec.sonrisas.data.remote.DailyLogRemoteDataSource
import mx.tec.sonrisas.data.remote.DailyLogRemoteDataSourceImpl
import mx.tec.sonrisas.data.remote.EstanciaRemoteDataSource
import mx.tec.sonrisas.data.remote.EstanciaRemoteDataSourceImpl
import mx.tec.sonrisas.data.remote.MessageRemoteDataSource
import mx.tec.sonrisas.data.remote.MessageRemoteDataSourceImpl
import mx.tec.sonrisas.data.remote.NoticeRemoteDataSource
import mx.tec.sonrisas.data.remote.NoticeRemoteDataSourceImpl
import mx.tec.sonrisas.data.remote.PickupRemoteDataSource
import mx.tec.sonrisas.data.remote.PickupRemoteDataSourceImpl

/**
 * Wires each repository (the "handler" the UI calls) and each remote source
 * (still a stub) to its interface. Once the real API is implemented, only
 * the *Impl classes in data/remote change — nothing here, nor the UI.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindChildRepository(impl: ChildRepositoryImpl): ChildRepository

    @Binds
    @Singleton
    abstract fun bindDailyLogRepository(impl: DailyLogRepositoryImpl): DailyLogRepository

    @Binds
    @Singleton
    abstract fun bindNoticeRepository(impl: NoticeRepositoryImpl): NoticeRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindPickupAuthorizationRepository(impl: PickupAuthorizationRepositoryImpl): PickupAuthorizationRepository

    @Binds
    abstract fun bindChildRemoteDataSource(impl: ChildRemoteDataSourceImpl): ChildRemoteDataSource

    @Binds
    abstract fun bindDailyLogRemoteDataSource(impl: DailyLogRemoteDataSourceImpl): DailyLogRemoteDataSource

    @Binds
    abstract fun bindNoticeRemoteDataSource(impl: NoticeRemoteDataSourceImpl): NoticeRemoteDataSource

    @Binds
    abstract fun bindMessageRemoteDataSource(impl: MessageRemoteDataSourceImpl): MessageRemoteDataSource

    @Binds
    abstract fun bindPickupRemoteDataSource(impl: PickupRemoteDataSourceImpl): PickupRemoteDataSource

    @Binds
    abstract fun bindEstanciaRemoteDataSource(impl: EstanciaRemoteDataSourceImpl): EstanciaRemoteDataSource
}
