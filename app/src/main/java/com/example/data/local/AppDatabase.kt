package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProfileEntity::class,
        RoutineEntity::class,
        ContactEntity::class,
        ActivityEventEntity::class,
        FocusSessionEntity::class,
        NotificationStatEntity::class,
        RoutineLogEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun routineDao(): RoutineDao
    abstract fun contactDao(): ContactDao
    abstract fun activityEventDao(): ActivityEventDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun notificationStatDao(): NotificationStatDao
    abstract fun routineLogDao(): RoutineLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE priority_contacts ADD COLUMN phoneNumber TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_companion_demo.db"
                )
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
