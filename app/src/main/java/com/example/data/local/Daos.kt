package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM smart_profiles ORDER BY isDefault DESC, name ASC")
    fun observeAll(): Flow<List<ProfileEntity>>

    @Query("SELECT COUNT(*) FROM smart_profiles")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ProfileEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ProfileEntity)

    @Query("DELETE FROM smart_profiles WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM smart_profiles")
    suspend fun clearAll()
}

@Dao
interface RoutineDao {
    @Query("SELECT * FROM smart_routines ORDER BY priority DESC, name ASC")
    fun observeAll(): Flow<List<RoutineEntity>>

    @Query("SELECT COUNT(*) FROM smart_routines")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<RoutineEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: RoutineEntity)

    @Query("DELETE FROM smart_routines WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM smart_routines")
    suspend fun clearAll()
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM priority_contacts ORDER BY name ASC")
    fun observeAll(): Flow<List<ContactEntity>>

    @Query("SELECT COUNT(*) FROM priority_contacts")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ContactEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ContactEntity)

    @Query("DELETE FROM priority_contacts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM priority_contacts")
    suspend fun clearAll()
}

@Dao
interface ActivityEventDao {
    @Query("SELECT * FROM activity_events ORDER BY timestampMillis DESC")
    fun observeAll(): Flow<List<ActivityEventEntity>>

    @Query("SELECT COUNT(*) FROM activity_events")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ActivityEventEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ActivityEventEntity)

    @Query("DELETE FROM activity_events")
    suspend fun clearAll()
}

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions ORDER BY endedAtMillis DESC")
    fun observeAll(): Flow<List<FocusSessionEntity>>

    @Query("SELECT COUNT(*) FROM focus_sessions")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<FocusSessionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FocusSessionEntity)

    @Query("DELETE FROM focus_sessions")
    suspend fun clearAll()
}

@Dao
interface NotificationStatDao {
    @Query("SELECT * FROM notification_stats ORDER BY totalCount DESC")
    fun observeAll(): Flow<List<NotificationStatEntity>>

    @Query("SELECT COUNT(*) FROM notification_stats")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<NotificationStatEntity>)

    @Query("DELETE FROM notification_stats")
    suspend fun clearAll()
}
