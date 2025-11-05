package com.example.croachcombat.database

import androidx.room.TypeConverter
import com.example.croachcombat.Gender
import com.example.croachcombat.ZodiacSign
import java.util.Date

class Converters {

    @TypeConverter
    fun fromGender(gender: Gender): String {
        return gender.name
    }

    @TypeConverter
    fun toGender(gender: String): Gender {
        return Gender.valueOf(gender)
    }

    @TypeConverter
    fun fromZodiacSign(zodiacSign: ZodiacSign): String {
        return zodiacSign.name
    }

    @TypeConverter
    fun toZodiacSign(zodiacSign: String): ZodiacSign {
        return ZodiacSign.valueOf(zodiacSign)
    }

    @TypeConverter
    fun fromDate(date: Date): Long {
        return date.time
    }

    @TypeConverter
    fun toDate(timestamp: Long): Date {
        return Date(timestamp)
    }
}