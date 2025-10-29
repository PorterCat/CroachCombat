package com.example.croachcombat

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.max
import kotlin.random.Random

@Composable
fun GameScreen(
    settings: GameSettings,
    modifier: Modifier = Modifier
) {
    val spawnDelayMsBase = 1000L
    val spawnDelayMs = max(100L, (spawnDelayMsBase / settings.gameSpeed).toLong())
    val maxInsects = settings.maxCockroaches
    val insectMinSpeed = 1f * settings.gameSpeed
    val insectMaxSpeed = 4f * settings.gameSpeed
    val insectSizePx = 80

    val insects = remember { mutableStateListOf<InsectState>() }
    var nextId by remember { mutableStateOf(1L) }
    var score by remember { mutableStateOf(0) }
    var misses by remember { mutableStateOf(0) }
    var areaWidth by remember { mutableStateOf(1) }
    var areaHeight by remember { mutableStateOf(1) }
    var timeLeft by remember { mutableStateOf(settings.roundDuration) }
    var running by remember { mutableStateOf(true) }

    val density = LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { sz ->
                areaWidth = max(1, sz.width)
                areaHeight = max(1, sz.height)
            }
            .pointerInput(Unit) {
                detectTapGestures { _ ->
                    if (!running) return@detectTapGestures
                    misses += 1
                    score = max(0, score - 5)
                }
            }
    ) {
        LaunchedEffect(running, settings.gameSpeed, settings.maxCockroaches) {
            while (running) {
                if (insects.size < maxInsects) {
                    val size = insectSizePx
                    val x = Random.nextInt(0, max(1, areaWidth - size)).toFloat()
                    val y = Random.nextInt(0, max(1, areaHeight - size)).toFloat()
                    val vx = Random.nextFloat() * (insectMaxSpeed - insectMinSpeed) + insectMinSpeed
                    val vy = Random.nextFloat() * (insectMaxSpeed - insectMinSpeed) + insectMinSpeed
                    val dx = if (Random.nextBoolean()) vx else -vx
                    val dy = if (Random.nextBoolean()) vy else -vy
                    val res = when (Random.nextInt(3)) {
                        0 -> R.drawable.bug1
                        1 -> R.drawable.bug2
                        else -> R.drawable.bug3
                    }
                    val insect = InsectState(nextId++, x, y, dx, dy, size, res, true)
                    insect.rotation = (atan2(insect.vy, insect.vx) * 180f / PI).toFloat() + 90
                    insects.add(insect)
                }
                delay(spawnDelayMs)
            }
        }

        LaunchedEffect(running) {
            while (running) {
                val snapshot = insects.toList()
                for (insect in snapshot) {
                    insect.x += insect.vx
                    insect.y += insect.vy

                    if (insect.x <= 0f || insect.x + insect.sizePx >= areaWidth) {
                        insect.vx = -insect.vx
                    }
                    if (insect.y <= 0f || insect.y + insect.sizePx >= areaHeight) {
                        insect.vy = -insect.vy
                    }

                    insect.rotation = (atan2(insect.vy, insect.vx) * 180f / PI).toFloat() + 90
                }
                delay(16L)
            }
        }

        LaunchedEffect(running) {
            while (running && timeLeft > 0) {
                delay(1000L)
                timeLeft -= 1
                if (timeLeft <= 0) {
                    running = false
                }
            }
        }

        for (insect in insects.toList()) {
            Image(
                painter = painterResource(id = insect.resId),
                contentDescription = "bug",
                modifier = Modifier
                    .offset { IntOffset(insect.x.toInt(), insect.y.toInt()) }
                    .size(with(density) { insect.sizePx.toDp() * 2 })
                    .graphicsLayer(rotationZ = insect.rotation)
                    .pointerInput(insect.id) {
                        detectTapGestures(onTap = {
                            if (!running) return@detectTapGestures
                            insects.removeAll { it.id == insect.id }
                            score += 10
                        })
                    }
            )
        }

        Card(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .graphicsLayer { alpha = 0.6f }
                .pointerInteropFilter { false },
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Очки: $score")
                Spacer(modifier = Modifier.width(12.dp))
                Text("Промахи: $misses")
                Spacer(modifier = Modifier.width(12.dp))
                Text("Осталось: ${timeLeft}s")
            }
        }

        if (!running) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Раунд окончен", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Очки: $score")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        Button(onClick = {
                            insects.clear()
                            nextId = 1L
                            score = 0
                            misses = 0
                            timeLeft = settings.roundDuration
                            running = true
                        }) {
                            Text("Перезапустить")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(onClick = { /* Закрыть игру — просто оставить экран */ }) {
                            Text("Закрыть")
                        }
                    }
                }
            }
        }
    }
}
