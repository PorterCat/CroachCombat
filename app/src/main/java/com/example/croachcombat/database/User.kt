package com.example.croachcombat.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.croachcombat.Gender
import com.example.croachcombat.ZodiacSign
import java.util.Date

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fullName: String,
    val gender: Gender,
    val birthday: Date,
    val difficulty: Int,
    val course: Byte,
    val zodiacSign: ZodiacSign,
    val createdAt: Date = Date()
)