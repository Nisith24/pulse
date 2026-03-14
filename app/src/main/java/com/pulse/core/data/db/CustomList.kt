package com.pulse.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_lists")
data class CustomList(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)
