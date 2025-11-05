package com.example.croachcombat.database

class GameRepository(private val database: GameDatabase) {

    suspend fun insertUser(user: User): Long {
        return database.userDao().insertUser(user)
    }

    fun getAllUsers() = database.userDao().getAllUsers()

    suspend fun getUserById(userId: Long) = database.userDao().getUserById(userId)

    suspend fun saveGameRecord(record: GameRecord) {
        database.gameRecordDao().insertRecord(record)
    }

    fun getTopRecords(limit: Int = 50) = database.gameRecordDao().getTopRecordsWithUserNames(limit)

    suspend fun deleteUser(userId: Long) {
        database.userDao().deleteUser(userId)
    }
}