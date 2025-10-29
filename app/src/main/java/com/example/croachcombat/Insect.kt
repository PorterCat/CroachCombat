package com.example.croachcombat

import androidx.annotation.DrawableRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue

class InsectState(
    val id: Long,
    xInit: Float,
    yInit: Float,
    var vx: Float,
    var vy: Float,
    var sizePx: Int,
    @DrawableRes val resId: Int,
    var alive: Boolean = true
) {
    var x by mutableFloatStateOf(xInit)
    var y by mutableFloatStateOf(yInit)
    var rotation by mutableFloatStateOf(0f)
}