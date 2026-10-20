package mx.tec.sonrisas.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import mx.tec.sonrisas.data.local.MIGRATION_9_10
import mx.tec.sonrisas.data.local.SonrisasDatabase
import mx.tec.sonrisas.data.local.dao.ActivityRecordDao
import mx.tec.sonrisas.data.local.dao.ChildDao
import mx.tec.sonrisas.data.local.dao.DailyLogDao
import mx.tec.sonrisas.data.local.dao.MessageDao
import mx.tec.sonrisas.data.local.dao.NoticeDao
import mx.tec.sonrisas.data.local.dao.PickupAuthorizationDao

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SonrisasDatabase =
        Room.databaseBuilder(context, SonrisasDatabase::class.java, "sonrisas.db")
            .addMigrations(MIGRATION_9_10)
            .fallbackToDestructiveMigration(true)
            .build()

    @Provides
    fun provideChildDao(db: SonrisasDatabase): ChildDao = db.childDao()

    @Provides
    fun provideDailyLogDao(db: SonrisasDatabase): DailyLogDao = db.dailyLogDao()

    @Provides
    fun provideActivityRecordDao(db: SonrisasDatabase): ActivityRecordDao = db.activityRecordDao()

    @Provides
    fun provideNoticeDao(db: SonrisasDatabase): NoticeDao = db.noticeDao()

    @Provides
    fun provideMessageDao(db: SonrisasDatabase): MessageDao = db.messageDao()

    @Provides
    fun providePickupAuthorizationDao(db: SonrisasDatabase): PickupAuthorizationDao = db.pickupAuthorizationDao()
}
