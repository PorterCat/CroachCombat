package com.example.croachcombat

import androidx.annotation.DrawableRes

data class Author(
    val name: String,
    @DrawableRes val photoRes: Int
)

data class GameSettings(
    var gameSpeed: Float = 1f,
    var maxCockroaches: Int = 10,
    var bonusInterval: Int = 30,
    var roundDuration: Int = 60
)