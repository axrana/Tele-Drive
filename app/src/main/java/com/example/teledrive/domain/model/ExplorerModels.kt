package com.example.teledrive.domain.model

import com.example.teledrive.data.local.entity.FileEntity
import com.example.teledrive.data.local.entity.Folder

sealed class ExplorerState {
    object Loading : ExplorerState()
    data class Success(
        val folders: List<Folder>,
        val files: List<FileEntity>
    ) : ExplorerState()
    data class Error(val message: String) : ExplorerState()
}

sealed class TransferStatus {
    object Pending : TransferStatus()
    data class Active(val progress: Float) : TransferStatus()
    object Completed : TransferStatus()
    data class Failed(val error: String) : TransferStatus()
    object Cancelled : TransferStatus()
}
