package com.example.croachcombat.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface GameRecordDao {
    @Insert
    suspend fun insertRecord(record: GameRecord)

    @Query("SELECT * FROM game_records ORDER BY score DESC LIMIT :limit")
    fun getTopRecords(limit: Int = 50): Flow<List<GameRecord>>

    @Query("SELECT * FROM game_records WHERE userId = :userId ORDER BY date DESC")
    fun getUserRecords(userId: Long): Flow<List<GameRecord>>

    @Query("SELECT gr.*, u.fullName FROM game_records gr INNER JOIN users u ON gr.userId = u.id ORDER BY gr.score DESC LIMIT :limit")
    fun getTopRecordsWithUserNames(limit: Int = 50): Flow<List<RecordWithUserName>>
}

data class RecordWithUserName(
    val id: Long,
    val userId: Long,
    val score: Int,
    val date: Date,
    val difficulty: Int,
    val fullName: String
)