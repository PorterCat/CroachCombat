package com.example.croachcombat

import androidx.annotation.DrawableRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue

class BonusState(
    val id: Long,
    xInit: Float,
    yInit: Float,
    var sizePx: Int,
    @DrawableRes val resId: Int = R.drawable.bonus_icon,
    var active: Boolean = true,
    var effectActive: Boolean = false,
    var effectTimeLeft: Float = 0f
) {
    var x by mutableFloatStateOf(xInit)
    var y by mutableFloatStateOf(yInit)
    var glowPhase by mutableFloatStateOf(0f)
}