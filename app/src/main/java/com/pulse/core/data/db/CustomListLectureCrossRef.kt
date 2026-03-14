package com.pulse.core.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "custom_list_lectures",
    primaryKeys = ["listId", "lectureId"],
    foreignKeys = [
        ForeignKey(
            entity = CustomList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Lecture::class,
            parentColumns = ["id"],
            childColumns = ["lectureId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["listId"]), Index(value = ["lectureId"])]
)
data class CustomListLectureCrossRef(
    val listId: Long,
    val lectureId: String,
    val addedAt: Long = System.currentTimeMillis()
)
