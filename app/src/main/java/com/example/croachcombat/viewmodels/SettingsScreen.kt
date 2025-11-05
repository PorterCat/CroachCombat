package com.example.croachcombat.viewmodels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.croachcombat.GameSettings

@Composable
fun SettingsScreen(settings: GameSettings, onSettingsChange: (GameSettings) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Настройки игры", style = MaterialTheme.typography.headlineSmall)

        Text("Скорость игры: ${String.format("%.1f", settings.gameSpeed)}")
        Slider(
            value = settings.gameSpeed,
            onValueChange = { onSettingsChange(settings.copy(gameSpeed = it)) },
            valueRange = 0.5f..3.0f,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Макс. тараканов: ${settings.maxCockroaches}")
        Slider(
            value = settings.maxCockroaches.toFloat(),
            onValueChange = { onSettingsChange(settings.copy(maxCockroaches = it.toInt())) },
            valueRange = 1f..20f,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Интервал бонусов: ${settings.bonusInterval} сек")
        Slider(
            value = settings.bonusInterval.toFloat(),
            onValueChange = { onSettingsChange(settings.copy(bonusInterval = it.toInt())) },
            valueRange = 10f..60f,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Длительность раунда: ${settings.roundDuration} сек")
        Slider(
            value = settings.roundDuration.toFloat(),
            onValueChange = { onSettingsChange(settings.copy(roundDuration = it.toInt())) },
            valueRange = 30f..180f,
            modifier = Modifier.fillMaxWidth()
        )
    }
}