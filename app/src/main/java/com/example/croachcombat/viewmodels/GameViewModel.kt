package com.example.croachcombat.viewmodels

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

data class GameState(
    val score: Int = 0,
    val misses: Int = 0,
    val timeLeft: Int = 60,
    val isRunning: Boolean = false,
    val goldRate: Double = 0.0,
    val goldCockroachActive: Boolean = false,
    val goldCockroachPosition: Pair<Float, Float>? = null
)

class GameViewModel(
    private val repository: GameRepository,
    private val cbrApiService: CbrApiService
) : ViewModel() {

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private var goldCockroachTimer: kotlinx.coroutines.Job? = null
    private var gameTimer: kotlinx.coroutines.Job? = null

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
    }

    fun stopGame() {
        gameTimer?.cancel()
        goldCockroachTimer?.cancel()
        _gameState.value = _gameState.value.copy(isRunning = false)
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

    fun spawnGoldCockroach(position: Pair<Float, Float>) {
        _gameState.value = _gameState.value.copy(
            goldCockroachActive = true,
            goldCockroachPosition = position
        )

        viewModelScope.launch {
            kotlinx.coroutines.delay(5000) // Золотой таракан виден 5 секунд
            if (_gameState.value.goldCockroachActive) {
                _gameState.value = _gameState.value.copy(
                    goldCockroachActive = false,
                    goldCockroachPosition = null
                )
            }
        }
    }

    fun collectGoldCockroach() {
        val points = (_gameState.value.goldRate * 10).roundToInt()
        addScore(points)
        _gameState.value = _gameState.value.copy(
            goldCockroachActive = false,
            goldCockroachPosition = null
        )
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
                kotlinx.coroutines.delay(20000) // Каждые 20 секунд
                if (_gameState.value.isRunning && !_gameState.value.goldCockroachActive) {
                    // Позиция будет установлена при отрисовке
                    _gameState.value = _gameState.value.copy(goldCockroachActive = true)
                }
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
    }
}