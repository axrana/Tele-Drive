package com.example.teledrive.data.local.dao

import androidx.room.*
import com.example.teledrive.data.local.entity.JournalEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalEventDao {
    @Query("SELECT * FROM journal_events ORDER BY ts ASC, version ASC")
    fun getAllEvents(): Flow<List<JournalEvent>>

    @Query("SELECT * FROM journal_events ORDER BY ts ASC, version ASC")
    suspend fun getAllEventsSync(): List<JournalEvent>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEvent(event: JournalEvent)

    @Query("DELETE FROM journal_events")
    suspend fun clearAll()
}
