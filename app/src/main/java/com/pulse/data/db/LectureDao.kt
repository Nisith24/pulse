package com.pulse.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LectureDao {
    @Query("SELECT * FROM lectures WHERE isLocal = 0 ORDER BY updatedAt DESC")
    fun getDriveLectures(): Flow<List<Lecture>>

    @Query("SELECT * FROM lectures WHERE isLocal = 1 ORDER BY updatedAt DESC")
    fun getLocalLectures(): Flow<List<Lecture>>

    @Query("SELECT * FROM lectures WHERE id = :id")
    fun getById(id: String): Flow<Lecture?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(lecture: Lecture)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<Lecture>)

    @Update
    suspend fun update(lecture: Lecture)

    @Query("DELETE FROM lectures WHERE isLocal = 0 AND id NOT IN (:ids)")
    suspend fun deleteMissingDrive(ids: List<String>)

    @Query("DELETE FROM lectures WHERE id = :id")
    suspend fun deleteById(id: String)
}
