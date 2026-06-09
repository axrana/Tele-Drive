package com.example.teledrive.data.local.dao

import androidx.room.*
import com.example.teledrive.data.local.entity.ShareToken
import kotlinx.coroutines.flow.Flow

@Dao
interface ShareTokenDao {
    @Query("SELECT * FROM share_tokens WHERE token = :token")
    suspend fun getShareToken(token: String): ShareToken?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createShareToken(token: ShareToken): Long

    @Query("DELETE FROM share_tokens WHERE id = :id")
    suspend fun deleteShareToken(id: Long)
}
