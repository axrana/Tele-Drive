package com.example.teledrive.data.local.dao

import androidx.room.*
import com.example.teledrive.data.local.entity.TransferEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferDao {
    @Query("SELECT * FROM transfers ORDER BY createdAt DESC")
    fun getAllTransfers(): Flow<List<TransferEntity>>

    @Query("SELECT * FROM transfers WHERE status = 'ACTIVE' OR status = 'PENDING'")
    fun getActiveTransfers(): Flow<List<TransferEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfer(transfer: TransferEntity): Long

    @Update
    suspend fun updateTransfer(transfer: TransferEntity)

    @Query("UPDATE transfers SET status = :status, progress = :progress, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateProgress(id: Long, status: String, progress: Float, updatedAt: Long)

    @Query("DELETE FROM transfers WHERE status = 'COMPLETED' OR status = 'CANCELLED'")
    suspend fun clearFinishedTransfers()

    @Query("SELECT * FROM transfers WHERE id = :id")
    suspend fun getTransferById(id: Long): TransferEntity?

    @Delete
    suspend fun deleteTransfer(transfer: TransferEntity)
}
