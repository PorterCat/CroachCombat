package com.example.croachcombat.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_records")
data class GameRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playerName: String,
    val score: Int,

    @ColumnInfo(name = "userId")
    val userId: Long,

    val gameDuration: Int,
    val difficulty: Int,
    val date: Long = System.currentTimeMillis()
)
