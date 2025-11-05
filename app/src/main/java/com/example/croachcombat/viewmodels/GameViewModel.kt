package com.example.croachcombat.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.croachcombat.database.GameRepository
import com.example.croachcombat.database.User
import com.example.croachcombat.network.CbrApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.ParseException
import kotlin.math.roundToInt
import kotlin.random.Random

data class GoldCockroachState(
    val id: Long,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val size: Int = 50
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
        val vx = (Random.nextFloat() * (maxSpeed - minSpeed) + minSpeed) * if (Random.nextBoolean()) 1f else -1f
        val vy = (Random.nextFloat() * (maxSpeed - minSpeed) + minSpeed) * if (Random.nextBoolean()) 1f else -1f

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

        // Отскок от стен
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
        val points = (_gameState.value.goldRate * 10).roundToInt()
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
                kotlinx.coroutines.delay(16L) // ~60 FPS
                // Движение золотых жуков будет обновляться в GameScreen с учетом areaWidth и areaHeight
            }
        }
    }

    private fun loadGoldRate() {
        viewModelScope.launch {
            try {
                val response = cbrApiService.getDailyRates()
                val goldValute = response.valutes.find { it.charCode == "XAU" }
                goldValute?.let {
                    val rate = parseRate(it.value) / it.nominal
                    _gameState.value = _gameState.value.copy(goldRate = rate)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _gameState.value = _gameState.value.copy(goldRate = 5000.0)
            }
        }
    }

    private fun parseRate(rateString: String): Double {
        return try {
            val formatted = rateString.replace(",", ".")
            DecimalFormat.getInstance().parse(formatted)?.toDouble() ?: 0.0
        } catch (e: ParseException) {
            0.0
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