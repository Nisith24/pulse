package com.pulse.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pulse.core.data.db.Lecture
import kotlinx.coroutines.flow.Flow

@Dao
interface LectureDao {
    @Query("SELECT * FROM lectures WHERE isLocal = 0 AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getBtrLectures(): Flow<List<Lecture>>

    @Query("SELECT * FROM lectures WHERE isLocal = 1 AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getLocalLectures(): Flow<List<Lecture>>

    @Query("SELECT * FROM lectures WHERE videoLocalPath IS NOT NULL AND videoLocalPath != '' AND isLocal = 0 AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getDownloadedLectures(): Flow<List<Lecture>>

    @Query("SELECT * FROM lectures WHERE videoId IS NOT NULL AND (videoLocalPath IS NULL OR videoLocalPath = '') AND isLocal = 0 AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getCloudOnlyLectures(): Flow<List<Lecture>>

    @Query("SELECT * FROM lectures WHERE id = :id AND isDeleted = 0")
    fun getById(id: String): Flow<Lecture?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(lecture: Lecture)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<Lecture>)

    @Update
    suspend fun update(lecture: Lecture)

    @Query("UPDATE lectures SET lastPosition = :lastPosition, videoDuration = :duration, updatedAt = :updatedAt, hlcTimestamp = :hlcTimestamp WHERE id = :id")
    suspend fun updateProgress(id: String, lastPosition: Long, duration: Long, updatedAt: Long, hlcTimestamp: String)

    @Query("UPDATE lectures SET speed = :speed, updatedAt = :updatedAt, hlcTimestamp = :hlcTimestamp WHERE id = :id")
    suspend fun updateSpeed(id: String, speed: Float, updatedAt: Long, hlcTimestamp: String)

    @Query("UPDATE lectures SET pdfLocalPath = :path, updatedAt = :updatedAt, hlcTimestamp = :hlcTimestamp WHERE id = :id")
    suspend fun updatePdfPath(id: String, path: String, updatedAt: Long, hlcTimestamp: String)

    @Query("UPDATE lectures SET videoLocalPath = :path, updatedAt = :updatedAt, hlcTimestamp = :hlcTimestamp WHERE id = :id")
    suspend fun updateVideoLocalPath(id: String, path: String, updatedAt: Long, hlcTimestamp: String)

    @Query("UPDATE lectures SET isDeleted = 1, hlcTimestamp = :hlcTimestamp WHERE isLocal = 0 AND id NOT IN (:ids)")
    suspend fun markMissingBtrDeleted(ids: List<String>, hlcTimestamp: String)

    @Query("UPDATE lectures SET isFavorite = NOT isFavorite, updatedAt = :updatedAt, hlcTimestamp = :hlcTimestamp WHERE id = :id")
    suspend fun toggleFavorite(id: String, updatedAt: Long, hlcTimestamp: String)

    @Query("UPDATE lectures SET isDeleted = 1, hlcTimestamp = :hlcTimestamp WHERE id = :id")
    suspend fun markDeleted(id: String, hlcTimestamp: String)

    @Query("SELECT * FROM lectures WHERE hlcTimestamp > :since")
    suspend fun getUpdatesSince(since: String): List<Lecture>

    @Query("UPDATE lectures SET pdfPageCount = :count, updatedAt = :updatedAt, hlcTimestamp = :hlcTimestamp WHERE id = :id")
    suspend fun updatePageCount(id: String, count: Int, updatedAt: Long, hlcTimestamp: String)

    @Query("UPDATE lectures SET lastPdfPage = :page, pdfIsHorizontal = :isHorizontal, updatedAt = :updatedAt, hlcTimestamp = :hlcTimestamp WHERE id = :id")
    suspend fun updatePdfState(id: String, page: Int, isHorizontal: Boolean, updatedAt: Long, hlcTimestamp: String)
}
