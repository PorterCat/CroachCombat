package com.example.croachcombat

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.SoundPool
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.croachcombat.database.GameRepository
import com.example.croachcombat.database.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

@Composable
fun GameScreen(
    settings: GameSettings,
    currentUser: User?,
    repository: GameRepository,
    modifier: Modifier = Modifier
) {
    val spawnDelayMsBase = 1000L
    val spawnDelayMs = max(100L, (spawnDelayMsBase / settings.gameSpeed).toLong())
    val maxInsects = settings.maxCockroaches
    val insectMinSpeed = 1f * settings.gameSpeed
    val insectMaxSpeed = 4f * settings.gameSpeed
    val insectSizePx = 80

    val bonusSpawnInterval = settings.bonusInterval * 1000L
    val bonusEffectDuration = 10000L
    val bonusVisibleDuration = 2000L
    val bonusSizePx = 100

    val bonus = remember { mutableStateOf<BonusState?>(null) }
    var bonusEffectActive by remember { mutableStateOf(false) }
    var bonusEffectTimeLeft by remember { mutableFloatStateOf(0f) }

    val insects = remember { mutableStateListOf<InsectState>() }
    var nextId by remember { mutableLongStateOf(1L) }
    var score by remember { mutableIntStateOf(0) }
    var misses by remember { mutableIntStateOf(0) }
    var areaWidth by remember { mutableIntStateOf(1) }
    var areaHeight by remember { mutableIntStateOf(1) }
    var timeLeft by remember { mutableIntStateOf(settings.roundDuration) }
    var running by remember { mutableStateOf(true) }

    val density = LocalDensity.current

    if (currentUser == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Выберите пользователя для начала игры", style = MaterialTheme.typography.headlineSmall)
        }
        return
    }

    val context = LocalContext.current
    val sensorManager = LocalContext.current.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    var accelerometerData by remember { mutableStateOf(FloatArray(3)) }
    var isAccelerometerActive by remember { mutableStateOf(false) }

    val soundPool = remember { SoundPool.Builder().setMaxStreams(2).build() }
    var screamSoundId by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        screamSoundId = soundPool.load(context, R.raw.discord_cat_scream, 1)
    }

    DisposableEffect(isAccelerometerActive) {
        if (!isAccelerometerActive) {
            onDispose { }
        }

        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    accelerometerData = event.values.copyOf()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    LaunchedEffect(running, bonusEffectActive) {
        if (!running || bonusEffectActive) return@LaunchedEffect

        while (running && !bonusEffectActive) {
            delay(bonusSpawnInterval)
            if (running && bonus.value == null && !bonusEffectActive) {
                val size = bonusSizePx
                val x = Random.nextInt(0, max(1, areaWidth - size)).toFloat()
                val y = Random.nextInt(0, max(1, areaHeight - size)).toFloat()
                bonus.value = BonusState(nextId++, x, y, size)

                launch {
                    delay(bonusVisibleDuration)
                    if (bonus.value?.effectActive != true) {
                        bonus.value = null
                    }
                }
            }
        }
    }

    LaunchedEffect(bonusEffectActive) {
        if (bonusEffectActive) {
            var timeLeft = bonusEffectDuration
            while (timeLeft > 0 && bonusEffectActive) {
                delay(100L)
                timeLeft -= 100
                bonusEffectTimeLeft = timeLeft.toFloat() / bonusEffectDuration.toFloat()
                if (timeLeft <= 0) {
                    bonusEffectActive = false
                    isAccelerometerActive = false
                    bonus.value = null
                }
            }
        }
    }

    LaunchedEffect(running, bonusEffectActive) {
        while (running) {
            if (bonusEffectActive) {
                val snapshot = insects.toList()
                for (insect in snapshot) {
                    // Добавляем влияние акселерометра на движение
                    val tiltX = accelerometerData[0] * 0.1f
                    val tiltY = accelerometerData[1] * 0.1f

                    insect.vx -= tiltX
                    insect.vy += tiltY

                    // Ограничиваем максимальную скорость
                    val maxSpeed = insectMaxSpeed * 2
                    val currentSpeed = sqrt(insect.vx * insect.vx + insect.vy * insect.vy)
                    if (currentSpeed > maxSpeed) {
                        insect.vx = insect.vx / currentSpeed * maxSpeed
                        insect.vy = insect.vy / currentSpeed * maxSpeed
                    }
                }
            }
            delay(16L)
        }
    }

    LaunchedEffect(bonus.value) {
        while (bonus.value != null) {
            delay(50L)
            bonus.value?.glowPhase = (bonus.value?.glowPhase ?: 0f) + 0.1f
        }
    }

    LaunchedEffect(running) {
        if (!running && score > 0) {
            val record = com.example.croachcombat.database.GameRecord(
                userId = currentUser.id,
                playerName = currentUser.toPlayer().fullName,
                score = score,
                gameDuration = settings.roundDuration,
                difficulty = currentUser.difficulty
            )
            repository.saveGameRecord(record)
        }
    }

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

        bonus.value?.let { currentBonus ->
            if (currentBonus.active && !currentBonus.effectActive) {
                val glowAlpha = (sin(currentBonus.glowPhase) * 0.3f + 0.7f).coerceIn(0f, 1f)

                Image(
                    painter = painterResource(id = currentBonus.resId),
                    contentDescription = "Bonus",
                    modifier = Modifier
                        .offset { IntOffset(currentBonus.x.toInt(), currentBonus.y.toInt()) }
                        .size(with(density) { currentBonus.sizePx.toDp() })
                        .graphicsLayer {
                            alpha = glowAlpha
                            scaleX = 1f + (1f - glowAlpha) * 0.2f
                            scaleY = 1f + (1f - glowAlpha) * 0.2f
                        }
                        .pointerInput(currentBonus.id) {
                            detectTapGestures(onTap = {
                                if (!running) return@detectTapGestures

                                bonus.value = BonusState(
                                    currentBonus.id,
                                    currentBonus.x,
                                    currentBonus.y,
                                    currentBonus.sizePx,
                                    currentBonus.resId,
                                    false,
                                    effectActive = true,
                                    effectTimeLeft = currentBonus.effectTimeLeft
                                )
                                bonusEffectActive = true
                                isAccelerometerActive = true

                                soundPool.play(screamSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
                            })
                        }
                )
            }

            if (bonusEffectActive) {
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(4.dp)
                        .background(Color.Gray.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .width(60.dp * bonusEffectTimeLeft)
                            .height(4.dp)
                            .background(Color.Red)
                    )
                }
            }
        }
    }
}
