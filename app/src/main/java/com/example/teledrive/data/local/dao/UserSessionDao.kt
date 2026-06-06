package com.example.teledrive.data.local.dao

import androidx.room.*
import com.example.teledrive.data.local.entity.UserSession
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSessionDao {
    @Query("SELECT * FROM user_session WHERE id = 1")
    fun getUserSession(): Flow<UserSession?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: UserSession)

    @Query("DELETE FROM user_session")
    suspend fun clearSession()
}
