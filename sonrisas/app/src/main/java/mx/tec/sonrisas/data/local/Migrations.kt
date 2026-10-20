package mx.tec.sonrisas.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Daily logs may hold changes not uploaded yet, so schema changes from here on are real migrations
 * instead of the destructive fallback (which would throw those changes away).
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Check-out to an authorized person, and undoing a check-out
        db.execSQL("ALTER TABLE `daily_logs` ADD COLUMN `check_out_pickupAuthorizationId` TEXT")
        db.execSQL("ALTER TABLE `daily_logs` ADD COLUMN `check_out_idPhotoFileId` TEXT")
        db.execSQL("ALTER TABLE `daily_logs` ADD COLUMN `check_out_idPhotoLocalPath` TEXT")
        db.execSQL("ALTER TABLE `daily_logs` ADD COLUMN `checkOutCleared` INTEGER NOT NULL DEFAULT 0")

        // Pickup authorizations change shape; the old rows only ever lived on this phone (demo data)
        db.execSQL("DROP TABLE IF EXISTS `pickup_authorizations`")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `pickup_authorizations` (`id` TEXT NOT NULL, `childId` TEXT NOT NULL, " +
                "`kind` TEXT NOT NULL, `startDate` TEXT NOT NULL, `weekdays` INTEGER NOT NULL, " +
                "`authorizedPersonName` TEXT NOT NULL, `relationship` TEXT NOT NULL, `phone` TEXT NOT NULL, " +
                "`identificationType` TEXT NOT NULL, `notes` TEXT, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_pickup_authorizations_childId` ON `pickup_authorizations` (`childId`)"
        )
    }
}
