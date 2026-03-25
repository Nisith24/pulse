package com.pulse.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.pulse.core.data.db.DriveFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DriveFileDao {
    @Query("SELECT * FROM drive_files WHERE parentId = :parentId ORDER BY name ASC")
    fun observeFilesByParentId(parentId: String): Flow<List<DriveFileEntity>>

    @Query("SELECT * FROM drive_files WHERE parentId = :parentId")
    suspend fun getFilesByParentId(parentId: String): List<DriveFileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(files: List<DriveFileEntity>)

    @Query("DELETE FROM drive_files WHERE parentId = :parentId AND id NOT IN (:currentIds)")
    suspend fun deleteMissingFiles(parentId: String, currentIds: List<String>)

    @Transaction
    suspend fun replaceFolderContents(parentId: String, files: List<DriveFileEntity>) {
        insertAll(files)
        val currentIds = files.map { it.id }
        if (currentIds.isNotEmpty()) {
            deleteMissingFiles(parentId, currentIds)
        } else {
            deleteFolderContents(parentId)
        }
    }

    @Query("DELETE FROM drive_files WHERE parentId = :parentId")
    suspend fun deleteFolderContents(parentId: String)

    @Query("SELECT MAX(lastSyncedAt) FROM drive_files WHERE parentId = :parentId")
    suspend fun getLastSyncTime(parentId: String): Long?
}
