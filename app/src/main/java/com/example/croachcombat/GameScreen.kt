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
import com.example.croachcombat.viewmodels.GameViewModel
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
    gameViewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val gameState by gameViewModel.gameState.collectAsState()
    val context = LocalContext.current

    val spawnDelayMsBase = 1000L
    val spawnDelayMs = max(100L, (spawnDelayMsBase / settings.gameSpeed).toLong())
    val maxInsects = settings.maxCockroaches
    val insectMinSpeed = 1f * settings.gameSpeed
    val insectMaxSpeed = 4f * settings.gameSpeed
    val insectSizePx = 80

    val insects = remember { mutableStateListOf<InsectState>() }
    var nextId by remember { mutableLongStateOf(1L) }
    var areaWidth by remember { mutableIntStateOf(1) }
    var areaHeight by remember { mutableIntStateOf(1) }
    var running by remember { mutableStateOf(true) }

    val density = LocalDensity.current

    // Sound setup
    val soundPool = remember { SoundPool.Builder().setMaxStreams(2).build() }
    var screamSoundId by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        screamSoundId = soundPool.load(context, R.raw.discord_cat_scream, 1)
    }

    // Accelerometer setup
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    var accelerometerData by remember { mutableStateOf(FloatArray(3)) }

    // Accelerometer listener
    DisposableEffect(gameState.bonusEffectActive) {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER && gameState.bonusEffectActive) {
                    accelerometerData = event.values.copyOf()
                    gameViewModel.updateAccelerometerData(event.values.copyOf())
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }

        if (gameState.bonusEffectActive) {
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // Инициализация игры при первом запуске
    LaunchedEffect(currentUser) {
        if (currentUser != null && running) {
            gameViewModel.startGame(settings.roundDuration, currentUser)
        }
    }

    // Обработка завершения игры
    LaunchedEffect(gameState.timeLeft) {
        if (gameState.timeLeft <= 0 && running) {
            running = false
            gameViewModel.stopGame()

            // Сохраняем рекорд
            if (gameState.score > 0 && currentUser != null) {
                val record = com.example.croachcombat.database.GameRecord(
                    userId = currentUser.id,
                    playerName = currentUser.fullName,
                    score = gameState.score,
                    gameDuration = settings.roundDuration,
                    difficulty = currentUser.difficulty
                )
                repository.saveGameRecord(record)
            }
        }
    }

    // Движение золотого жука
    LaunchedEffect(running, gameState.goldCockroachActive) {
        while (running) {
            if (gameState.goldCockroachActive && areaWidth > 1 && areaHeight > 1) {
                gameViewModel.updateGoldCockroachPosition(areaWidth, areaHeight)
            }
            delay(16L) // ~60 FPS
        }
    }

    // Спавн золотого жука при активации
    LaunchedEffect(running, gameState.goldCockroachActive) {
        if (running && gameState.goldCockroachActive && gameState.goldCockroach == null && areaWidth > 100 && areaHeight > 100) {
            val size = 100
            val x = Random.nextInt(0, max(1, areaWidth - size)).toFloat()
            val y = Random.nextInt(0, max(1, areaHeight - size)).toFloat()
            gameViewModel.spawnGoldCockroach(Pair(x, y), areaWidth, areaHeight)
        }
    }

    if (currentUser == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Выберите пользователя для начала игры", style = MaterialTheme.typography.headlineSmall)
        }
        return
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
                    gameViewModel.addMiss()
                }
            }
    ) {
        // Спавн обычных тараканов
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

        // Движение обычных тараканов с учетом эффекта бонуса и акселерометра
        LaunchedEffect(running, gameState.bonusEffectActive) {
            while (running) {
                val snapshot = insects.toList()
                for (insect in snapshot) {
                    // Ускорение при активном бонусе
                    val speedMultiplier = if (gameState.bonusEffectActive) 1.5f else 1f

                    // Влияние акселерометра при активном бонусе
                    if (gameState.bonusEffectActive) {
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

                    insect.x += insect.vx * speedMultiplier
                    insect.y += insect.vy * speedMultiplier

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

        // Спавн бонуса
        LaunchedEffect(running, gameState.bonusActive) {
            if (running && gameState.bonusActive && gameState.bonusPosition == null && areaWidth > 80 && areaHeight > 80) {
                val size = 80
                val x = Random.nextInt(0, max(1, areaWidth - size)).toFloat()
                val y = Random.nextInt(0, max(1, areaHeight - size)).toFloat()
                gameViewModel.spawnBonus(Pair(x, y))
            }
        }

        // Отрисовка обычных тараканов
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
                            gameViewModel.addScore(10)
                        })
                    }
            )
        }

        // Отрисовка золотого таракана - НЕПРОЗРАЧНЫЙ и с движением
        gameState.goldCockroach?.let { goldCockroach ->
            val glowAlpha = (sin(System.currentTimeMillis() * 0.01f) * 0.3f + 0.7f).coerceIn(0f, 1f)

            Image(
                painter = painterResource(id = R.drawable.gold_bug),
                contentDescription = "Gold Cockroach",
                modifier = Modifier
                    .offset { IntOffset(goldCockroach.x.toInt(), goldCockroach.y.toInt()) }
                    .size(100.dp)
                    .graphicsLayer {
                        // УБРАНА ПРОЗРАЧНОСТЬ - золотой жук полностью непрозрачный
                        alpha = 1f // Вместо glowAlpha
                        scaleX = 1f + (1f - glowAlpha) * 0.2f
                        scaleY = 1f + (1f - glowAlpha) * 0.2f
                        // Добавляем вращение как у обычных жуков
                        rotationZ = (atan2(goldCockroach.vy, goldCockroach.vx) * 180f / PI).toFloat() + 90
                    }
                    .pointerInput(goldCockroach.id) {
                        detectTapGestures(onTap = {
                            if (!running) return@detectTapGestures
                            gameViewModel.collectGoldCockroach()
                        })
                    }
            )
        }

        // Отрисовка бонуса с анимацией
        if (gameState.bonusActive && gameState.bonusPosition != null) {
            val bonus = gameState.bonusPosition!!
            val glowAlpha = (sin(System.currentTimeMillis() * 0.01f) * 0.3f + 0.7f).coerceIn(0f, 1f)

            Image(
                painter = painterResource(id = R.drawable.bonus_icon),
                contentDescription = "Bonus",
                modifier = Modifier
                    .offset { IntOffset(bonus.first.toInt(), bonus.second.toInt()) }
                    .size(80.dp)
                    .graphicsLayer {
                        alpha = glowAlpha
                        scaleX = 1f + (1f - glowAlpha) * 0.2f
                        scaleY = 1f + (1f - glowAlpha) * 0.2f
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            if (!running) return@detectTapGestures
                            gameViewModel.collectBonus()
                            soundPool.play(screamSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
                        })
                    }
            )
        }

        // Панель статистики - ИСПРАВЛЕНО: добавлен pointerInteropFilter { false }
        Card(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .graphicsLayer { alpha = 0.6f }
                .pointerInteropFilter { false }, // Это исправляет проблему с нажатиями
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Очки: ${gameState.score}")
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Промахи: ${gameState.misses}")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Время: ${gameState.timeLeft}s")
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Золото: ${String.format("%.2f", gameState.goldRate)}")
                }

                // Полоска времени бонуса
                if (gameState.bonusEffectTimeLeft > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(Color.Gray.copy(alpha = 0.5f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(gameState.bonusEffectTimeLeft / 10f)
                                .height(6.dp)
                                .background(Color.Green)
                        )
                    }
                    Text(
                        "БОНУС АКТИВЕН! Наклоняйте устройство!",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Green
                    )
                }
            }
        }

        // Экран завершения игры
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
                    Text("Очки: ${gameState.score}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        Button(onClick = {
                            insects.clear()
                            nextId = 1L
                            running = true
                            gameViewModel.startGame(settings.roundDuration, currentUser)
                        }) {
                            Text("Перезапустить")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(onClick = { /* Закрыть игру */ }) {
                            Text("Закрыть")
                        }
                    }
                }
            }
        }
    }

    // Cleanup sound pool
    DisposableEffect(Unit) {
        onDispose {
            soundPool.release()
        }
    }
}