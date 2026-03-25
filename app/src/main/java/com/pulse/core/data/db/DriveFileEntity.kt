package com.pulse.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pulse.data.services.btr.BtrFile

@Entity(tableName = "drive_files")
data class DriveFileEntity(
    @PrimaryKey
    val id: String,
    val parentId: String,
    val name: String,
    val mimeType: String,
    val size: Long? = null,
    val lastSyncedAt: Long = 0L
)

fun DriveFileEntity.toDomainModel(): BtrFile {
    return BtrFile(
        id = id,
        name = name,
        mimeType = mimeType,
        size = size
    )
}

fun BtrFile.toEntity(parentId: String, lastSyncedAt: Long): DriveFileEntity {
    return DriveFileEntity(
        id = id,
        parentId = parentId,
        name = name,
        mimeType = mimeType,
        size = size,
        lastSyncedAt = lastSyncedAt
    )
}
