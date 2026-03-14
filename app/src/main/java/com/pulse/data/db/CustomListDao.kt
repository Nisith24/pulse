package com.pulse.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.pulse.core.data.db.CustomList
import com.pulse.core.data.db.CustomListLectureCrossRef
import com.pulse.core.data.db.Lecture
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomListDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomList(customList: CustomList): Long

    @Query("DELETE FROM custom_lists WHERE id = :listId")
    suspend fun deleteCustomList(listId: Long)

    @Query("SELECT * FROM custom_lists ORDER BY createdAt DESC")
    fun getAllCustomLists(): Flow<List<CustomList>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLectureToCustomList(crossRef: CustomListLectureCrossRef)

    @Query("DELETE FROM custom_list_lectures WHERE listId = :listId AND lectureId = :lectureId")
    suspend fun removeLectureFromCustomList(listId: Long, lectureId: String)

    @Query("SELECT * FROM custom_list_lectures WHERE listId = :listId AND lectureId = :lectureId")
    suspend fun getCrossRef(listId: Long, lectureId: String): CustomListLectureCrossRef?

    @Transaction
    @Query("""
        SELECT l.* FROM lectures l
        INNER JOIN custom_list_lectures cll ON l.id = cll.lectureId
        WHERE cll.listId = :listId
        ORDER BY cll.addedAt DESC
    """)
    fun getLecturesForCustomList(listId: Long): Flow<List<Lecture>>
}
