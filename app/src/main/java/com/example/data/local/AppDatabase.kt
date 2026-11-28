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
    version = 7,
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

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE priority_contacts ADD COLUMN systemContactId TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM priority_contacts WHERE priorityLevel != 'EMERGENCY'")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS repeat_caller_rules (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        phoneNumber TEXT NOT NULL,
                        requiredCalls INTEGER NOT NULL,
                        windowMinutes INTEGER NOT NULL,
                        isEnabled INTEGER NOT NULL,
                        systemContactId TEXT NOT NULL DEFAULT ''
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS repeat_caller_rules")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS priority_contacts_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        initials TEXT NOT NULL,
                        relationship TEXT NOT NULL,
                        priorityLevel TEXT NOT NULL,
                        allowedInterruptionsJson TEXT NOT NULL,
                        isEnabled INTEGER NOT NULL,
                        phoneNumber TEXT NOT NULL DEFAULT '',
                        systemContactId TEXT NOT NULL DEFAULT ''
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO priority_contacts_new (
                        id, name, initials, relationship, priorityLevel,
                        allowedInterruptionsJson, isEnabled, phoneNumber, systemContactId
                    )
                    SELECT id, name, initials, relationship, priorityLevel,
                        allowedInterruptionsJson, isEnabled, phoneNumber, systemContactId
                    FROM priority_contacts
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE priority_contacts")
                db.execSQL("ALTER TABLE priority_contacts_new RENAME TO priority_contacts")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_companion_demo.db"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
