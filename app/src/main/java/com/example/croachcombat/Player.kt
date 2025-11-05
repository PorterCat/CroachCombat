package com.example.croachcombat

import java.util.Date

data class Player(
    val fullName : String,
    val gender: Gender,
    val birthday: Date,
    val difficulty: Int,
    val course: Byte,
    var zodiacSign: ZodiacSign
)

fun Player.toUser(): com.example.croachcombat.database.User {
    return com.example.croachcombat.database.User(
        fullName = this.fullName,
        gender = this.gender,
        birthday = this.birthday,
        difficulty = this.difficulty,
        course = this.course,
        zodiacSign = this.zodiacSign
    )
}

fun com.example.croachcombat.database.User.toPlayer(): Player {
    return Player(
        fullName = this.fullName,
        gender = this.gender,
        birthday = this.birthday,
        difficulty = this.difficulty,
        course = this.course,
        zodiacSign = this.zodiacSign
    )
}

enum class Gender {
    Male,
    Female
}

enum class ZodiacSign {
    Aries,          // овен
    Taurus,         // телец
    Gemini,         // близнецы
    Cancer,         // рак
    Leo,            // лев
    Virgo,          // дева
    Libra,          // весы
    Scorpio,        // скорпион
    Sagittarius,    // стрелец
    Capricorn,      // козерог
    Aquarius,       // водолей
    Pisces          // рыбы
}
