package com.example.finfit.health.repository

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.finfit.health.model.HealthEntity
import com.example.finfit.health.model.WaterDailySummaryEntity
import com.example.finfit.health.model.WaterLogEntity

import com.example.finfit.health.model.SleepSessionEntity

@Database(
    entities = [
        HealthEntity::class,
        WaterLogEntity::class,
        WaterDailySummaryEntity::class,
        SleepSessionEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class HealthDatabase : RoomDatabase() {
    abstract fun healthDao(): HealthDao
    abstract fun waterLogDao(): WaterLogDao
    abstract fun waterSummaryDao(): WaterSummaryDao
    abstract fun sleepLogDao(): SleepLogDao

    companion object {
        @Volatile
        private var INSTANCE: HealthDatabase? = null

        /**
         * Migration 5 -> 6: Thêm 2 bảng mới cho Water Module.
         * KHÔNG phá hủy bảng health_history cũ — dữ liệu bước chân và nước cũ vẫn giữ nguyên.
         * Sau migration, WaterRepository sẽ chạy hàm Migration Legacy để backfill
         * dữ liệu waterConsumed cũ thành dummy WaterLog (nếu cần).
         */
        private val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Tạo bảng water_logs (Source of Truth cho mọi sự kiện uống nước)
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `water_logs` (
                        `id`              TEXT    NOT NULL PRIMARY KEY,
                        `date`            TEXT    NOT NULL,
                        `timestamp`       INTEGER NOT NULL,
                        `amountMl`        INTEGER NOT NULL,
                        `drinkType`       TEXT    NOT NULL,
                        `caffeineMg`      INTEGER NOT NULL,
                        `source`          TEXT    NOT NULL,
                        `contextSteps`    INTEGER NOT NULL,
                        `timezoneOffset`  INTEGER NOT NULL,
                        `isDeleted`       INTEGER NOT NULL DEFAULT 0,
                        `createdAt`       INTEGER NOT NULL,
                        `updatedAt`       INTEGER NOT NULL,
                        `syncStatus`      INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // Index trên [date] cho Hourly Chart query
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_water_logs_date` ON `water_logs` (`date`)"
                )

                // Index trên [syncStatus] cho WorkManager batch sync
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_water_logs_syncStatus` ON `water_logs` (`syncStatus`)"
                )

                // Tạo bảng water_daily_summary (Cache / Read-Model)
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `water_daily_summary` (
                        `date`               TEXT    NOT NULL PRIMARY KEY,
                        `totalConsumedMl`    INTEGER NOT NULL,
                        `dailyGoalMl`        INTEGER NOT NULL,
                        `totalCaffeineMg`    INTEGER NOT NULL,
                        `lastDrinkTimestamp` INTEGER NOT NULL,
                        `updatedAt`          INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
        
        private val MIGRATION_6_7: Migration = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sleep_session_logs` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `date` TEXT NOT NULL,
                        `bedTimeTimestamp` INTEGER NOT NULL,
                        `wakeTimeTimestamp` INTEGER NOT NULL,
                        `sleepQuality` INTEGER NOT NULL,
                        `isDeleted` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_sleep_session_logs_date` ON `sleep_session_logs` (`date`)"
                )
            }
        }

        private val MIGRATION_7_8: Migration = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE health_history ADD COLUMN carbs INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE health_history ADD COLUMN protein INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE health_history ADD COLUMN fat INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): HealthDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HealthDatabase::class.java,
                    "health_database"
                )
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
