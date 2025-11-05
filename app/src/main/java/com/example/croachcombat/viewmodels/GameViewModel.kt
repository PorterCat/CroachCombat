package com.example.croachcombat.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.croachcombat.database.GameRepository
import com.example.croachcombat.database.User
import com.example.croachcombat.network.CbrApiService
import com.example.croachcombat.widget.GoldRateWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class GoldCockroachState(
    val id: Long,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val size: Int = 100
)

data class GameState(
    val score: Int = 0,
    val misses: Int = 0,
    val timeLeft: Int = 60,
    val isRunning: Boolean = false,
    val goldRate: Double = 0.0,
    val goldCockroachActive: Boolean = false,
    val goldCockroach: GoldCockroachState? = null,
    val bonusActive: Boolean = false,
    val bonusPosition: Pair<Float, Float>? = null,
    val bonusEffectActive: Boolean = false,
    val bonusEffectTimeLeft: Float = 0f,
    val accelerometerData: FloatArray = FloatArray(3)
)

class GameViewModel(
    private val repository: GameRepository,
    private val cbrApiService: CbrApiService,
    private val context: Context
) : ViewModel() {

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private var goldCockroachTimer: kotlinx.coroutines.Job? = null
    private var gameTimer: kotlinx.coroutines.Job? = null
    private var bonusTimer: kotlinx.coroutines.Job? = null
    private var movementTimer: kotlinx.coroutines.Job? = null

    init {
        loadGoldRate()
    }

    fun startGame(initialTime: Int, currentUser: User?) {
        _gameState.value = GameState(
            timeLeft = initialTime,
            isRunning = true,
            goldRate = _gameState.value.goldRate
        )

        startGameTimer()
        startGoldCockroachTimer()
        startBonusTimer()
        startMovementTimer()
    }

    fun stopGame() {
        gameTimer?.cancel()
        goldCockroachTimer?.cancel()
        bonusTimer?.cancel()
        movementTimer?.cancel()
        _gameState.value = _gameState.value.copy(
            isRunning = false,
            bonusEffectActive = false,
            goldCockroachActive = false,
            goldCockroach = null
        )
    }

    fun addScore(points: Int) {
        _gameState.value = _gameState.value.copy(
            score = _gameState.value.score + points
        )
    }

    fun addMiss() {
        _gameState.value = _gameState.value.copy(
            misses = _gameState.value.misses + 1,
            score = (_gameState.value.score - 5).coerceAtLeast(0)
        )
    }

    fun spawnGoldCockroach(position: Pair<Float, Float>, areaWidth: Int, areaHeight: Int) {
        val size = 100
        val x = position.first
        val y = position.second

        val minSpeed = 1f
        val maxSpeed = 4f
        val vx = (kotlin.random.Random.nextFloat() * (maxSpeed - minSpeed) + minSpeed) * if (kotlin.random.Random.nextBoolean()) 1f else -1f
        val vy = (kotlin.random.Random.nextFloat() * (maxSpeed - minSpeed) + minSpeed) * if (kotlin.random.Random.nextBoolean()) 1f else -1f

        val goldCockroach = GoldCockroachState(
            id = System.currentTimeMillis(),
            x = x,
            y = y,
            vx = vx,
            vy = vy,
            size = size
        )

        _gameState.value = _gameState.value.copy(
            goldCockroachActive = true,
            goldCockroach = goldCockroach
        )

        viewModelScope.launch {
            kotlinx.coroutines.delay(5000)
            if (_gameState.value.goldCockroachActive) {
                _gameState.value = _gameState.value.copy(
                    goldCockroachActive = false,
                    goldCockroach = null
                )
            }
        }
    }

    fun updateGoldCockroachPosition(areaWidth: Int, areaHeight: Int) {
        val currentGoldCockroach = _gameState.value.goldCockroach ?: return

        var newX = currentGoldCockroach.x + currentGoldCockroach.vx
        var newY = currentGoldCockroach.y + currentGoldCockroach.vy
        var newVx = currentGoldCockroach.vx
        var newVy = currentGoldCockroach.vy

        if (newX <= 0f || newX + currentGoldCockroach.size >= areaWidth) {
            newVx = -newVx
            newX = newX.coerceIn(0f, (areaWidth - currentGoldCockroach.size).toFloat())
        }
        if (newY <= 0f || newY + currentGoldCockroach.size >= areaHeight) {
            newVy = -newVy
            newY = newY.coerceIn(0f, (areaHeight - currentGoldCockroach.size).toFloat())
        }

        val updatedGoldCockroach = currentGoldCockroach.copy(
            x = newX,
            y = newY,
            vx = newVx,
            vy = newVy
        )

        _gameState.value = _gameState.value.copy(goldCockroach = updatedGoldCockroach)
    }

    fun collectGoldCockroach() {
        val points = (_gameState.value.goldRate / 500).roundToInt()
        addScore(points)
        _gameState.value = _gameState.value.copy(
            goldCockroachActive = false,
            goldCockroach = null
        )
    }

    fun spawnBonus(position: Pair<Float, Float>) {
        _gameState.value = _gameState.value.copy(
            bonusActive = true,
            bonusPosition = position
        )

        viewModelScope.launch {
            kotlinx.coroutines.delay(5000)
            if (_gameState.value.bonusActive && !_gameState.value.bonusEffectActive) {
                _gameState.value = _gameState.value.copy(
                    bonusActive = false,
                    bonusPosition = null
                )
            }
        }
    }

    fun collectBonus() {
        _gameState.value = _gameState.value.copy(
            bonusActive = false,
            bonusPosition = null,
            bonusEffectActive = true,
            bonusEffectTimeLeft = 10f
        )

        viewModelScope.launch {
            var timeLeft = 10f
            while (timeLeft > 0 && _gameState.value.bonusEffectActive) {
                kotlinx.coroutines.delay(100)
                timeLeft -= 0.1f
                _gameState.value = _gameState.value.copy(
                    bonusEffectTimeLeft = timeLeft
                )
            }
            _gameState.value = _gameState.value.copy(
                bonusEffectActive = false,
                bonusEffectTimeLeft = 0f
            )
        }
    }

    fun updateAccelerometerData(data: FloatArray) {
        _gameState.value = _gameState.value.copy(accelerometerData = data)
    }

    private fun startGameTimer() {
        gameTimer?.cancel()
        gameTimer = viewModelScope.launch {
            while (_gameState.value.timeLeft > 0 && _gameState.value.isRunning) {
                kotlinx.coroutines.delay(1000)
                _gameState.value = _gameState.value.copy(
                    timeLeft = _gameState.value.timeLeft - 1
                )
            }
            if (_gameState.value.timeLeft <= 0) {
                stopGame()
            }
        }
    }

    private fun startGoldCockroachTimer() {
        goldCockroachTimer?.cancel()
        goldCockroachTimer = viewModelScope.launch {
            while (_gameState.value.isRunning) {
                kotlinx.coroutines.delay(20000)
                if (_gameState.value.isRunning && !_gameState.value.goldCockroachActive) {
                    _gameState.value = _gameState.value.copy(goldCockroachActive = true)
                }
            }
        }
    }

    private fun startBonusTimer() {
        bonusTimer?.cancel()
        bonusTimer = viewModelScope.launch {
            while (_gameState.value.isRunning) {
                kotlinx.coroutines.delay(15000)
                if (_gameState.value.isRunning && !_gameState.value.bonusActive) {
                    _gameState.value = _gameState.value.copy(bonusActive = true)
                }
            }
        }
    }

    private fun startMovementTimer() {
        movementTimer?.cancel()
        movementTimer = viewModelScope.launch {
            while (_gameState.value.isRunning) {
                kotlinx.coroutines.delay(16L)
            }
        }
    }

    private fun loadGoldRate() {
        viewModelScope.launch {
            try {
                val prefs = context.getSharedPreferences(GoldRateWidget.WIDGET_PREF_NAME, Context.MODE_PRIVATE)
                val savedRate = prefs.getFloat(GoldRateWidget.KEY_GOLD_RATE, 0f).toDouble()

                if (savedRate > 0) {
                    _gameState.value = _gameState.value.copy(goldRate = savedRate)
                }

                val today = CbrApiService.getTodayDate()
                val response = cbrApiService.getMetalRates(today, today)
                val goldRecord = response.records.find { it.code == "1" }

                if (goldRecord != null) {
                    val rate = goldRecord.sell.replace(",", ".").toDoubleOrNull() ?: 0.0
                    _gameState.value = _gameState.value.copy(goldRate = rate)
                    prefs.edit().putFloat(GoldRateWidget.KEY_GOLD_RATE, rate.toFloat()).apply()
                    GoldRateWidget.updateAllWidgets(context)
                } else {
                    val fallbackRate = 5432.10
                    _gameState.value = _gameState.value.copy(goldRate = fallbackRate)
                    prefs.edit().putFloat(GoldRateWidget.KEY_GOLD_RATE, fallbackRate.toFloat()).apply()
                    GoldRateWidget.updateAllWidgets(context)
                }
            } catch (e: Exception) {
                val prefs = context.getSharedPreferences(GoldRateWidget.WIDGET_PREF_NAME, Context.MODE_PRIVATE)
                val savedRate = prefs.getFloat(GoldRateWidget.KEY_GOLD_RATE, 0f).toDouble()
                val fallbackRate = if (savedRate > 0) savedRate else 5432.10
                _gameState.value = _gameState.value.copy(goldRate = fallbackRate)
                if (savedRate == 0.toDouble()) {
                    prefs.edit().putFloat(GoldRateWidget.KEY_GOLD_RATE, fallbackRate.toFloat()).apply()
                    GoldRateWidget.updateAllWidgets(context)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        gameTimer?.cancel()
        goldCockroachTimer?.cancel()
        bonusTimer?.cancel()
        movementTimer?.cancel()
    }
}