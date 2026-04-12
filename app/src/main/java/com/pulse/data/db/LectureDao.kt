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
    @Query("SELECT * FROM lectures WHERE isLocal = 0 AND (subject IS NULL OR subject = '') AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getBtrLectures(): Flow<List<Lecture>>

    @Query("SELECT * FROM lectures WHERE isLocal = 1 AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getLocalLectures(): Flow<List<Lecture>>

    @Query("SELECT * FROM lectures WHERE videoLocalPath IS NOT NULL AND videoLocalPath != '' AND isLocal = 0 AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getDownloadedLectures(): Flow<List<Lecture>>

    @Query("SELECT * FROM lectures WHERE videoId IS NOT NULL AND (videoLocalPath IS NULL OR videoLocalPath = '') AND isLocal = 0 AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getCloudOnlyLectures(): Flow<List<Lecture>>

    @Query("SELECT * FROM lectures WHERE subject = :subject AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getLecturesBySubject(subject: String): Flow<List<Lecture>>

    @Query("SELECT * FROM lectures WHERE category = :category AND isDeleted = 0 ORDER BY orderIndex ASC, updatedAt DESC")
    fun getLecturesByCategory(category: String): Flow<List<Lecture>>

    @Query("SELECT * FROM lectures WHERE tags LIKE '%' || :query || '%' OR name LIKE '%' || :query || '%' AND isDeleted = 0 ORDER BY orderIndex ASC")
    fun searchLectures(query: String): Flow<List<Lecture>>

    @Query("SELECT * FROM lectures WHERE isFavorite = 1 AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getFavoriteLectures(): Flow<List<Lecture>>

    @Query("SELECT * FROM lectures WHERE lastPosition > 0 AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getRecentlyWatchedLectures(): Flow<List<Lecture>>

    @Query("SELECT * FROM lectures WHERE id = :id AND isDeleted = 0")
    fun getById(id: String): Flow<Lecture?>

    @Query("SELECT * FROM lectures WHERE id = :id AND isDeleted = 0")
    suspend fun getByIdSync(id: String): Lecture?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(lecture: Lecture): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(list: List<Lecture>): List<Long>

    @Update
    suspend fun updateLecture(lecture: Lecture)

    @Update
    suspend fun updateAll(list: List<Lecture>)

    @androidx.room.Transaction
    suspend fun insert(lecture: Lecture) {
        val id = insertIgnore(lecture)
        if (id == -1L) {
            updateLecture(lecture)
        }
    }

    @androidx.room.Transaction
    suspend fun insertAll(list: List<Lecture>) {
        val insertResults = insertAllIgnore(list)
        val updateList = mutableListOf<Lecture>()
        for (i in insertResults.indices) {
            if (insertResults[i] == -1L) {
                updateList.add(list[i])
            }
        }
        if (updateList.isNotEmpty()) {
            updateAll(updateList)
        }
    }

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

    @Query("UPDATE lectures SET isDeleted = 1, hlcTimestamp = :hlcTimestamp WHERE isLocal = 0 AND (subject IS NULL OR subject = '') AND id NOT IN (:ids)")
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

    @Query("SELECT * FROM lectures WHERE updatedAt > :since")
    suspend fun getModifiedSince(since: Long): List<Lecture>

    @Query("SELECT * FROM lectures")
    suspend fun getAllLecturesAsList(): List<Lecture>

    @Query("SELECT * FROM lectures WHERE id IN (:ids)")
    suspend fun getLecturesByIds(ids: List<String>): List<Lecture>

    @Query("SELECT * FROM lectures WHERE lastPosition > 1000 AND isDeleted = 0 ORDER BY updatedAt DESC LIMIT 1")
    fun getRecentLecture(): Flow<Lecture?>

    @Query("SELECT * FROM lectures WHERE videoDuration > 0 AND lastPosition >= (videoDuration * 0.9) AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getCompletedLectures(): Flow<List<Lecture>>
}
