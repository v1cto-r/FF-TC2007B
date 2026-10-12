package mx.tec.sonrisas.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import mx.tec.sonrisas.data.local.dao.ActivityRecordDao
import mx.tec.sonrisas.data.local.dao.ChildDao
import mx.tec.sonrisas.data.local.dao.DailyLogDao
import mx.tec.sonrisas.data.local.dao.MessageDao
import mx.tec.sonrisas.data.local.dao.NoticeDao
import mx.tec.sonrisas.data.local.dao.PickupAuthorizationDao
import mx.tec.sonrisas.data.local.entities.ActivityRecordEntity
import mx.tec.sonrisas.data.local.entities.ChildEntity
import mx.tec.sonrisas.data.local.entities.DailyLogEntity
import mx.tec.sonrisas.data.local.entities.MessageEntity
import mx.tec.sonrisas.data.local.entities.NoticeEntity
import mx.tec.sonrisas.data.local.entities.PickupAuthorizationEntity

@Database(
    entities = [
        ChildEntity::class,
        DailyLogEntity::class,
        ActivityRecordEntity::class,
        NoticeEntity::class,
        MessageEntity::class,
        PickupAuthorizationEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SonrisasDatabase : RoomDatabase() {

    abstract fun childDao(): ChildDao
    abstract fun dailyLogDao(): DailyLogDao
    abstract fun activityRecordDao(): ActivityRecordDao
    abstract fun noticeDao(): NoticeDao
    abstract fun messageDao(): MessageDao
    abstract fun pickupAuthorizationDao(): PickupAuthorizationDao
}
