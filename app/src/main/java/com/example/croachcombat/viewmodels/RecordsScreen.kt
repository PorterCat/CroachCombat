package com.example.croachcombat.viewmodels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.croachcombat.database.GameRepository
import com.example.croachcombat.database.RecordWithUserName
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun RecordsScreen(repository: GameRepository) {
    val records by repository.getTopRecords(20).collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Таблица рекордов", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (records.isEmpty()) {
            Text("Пока нет рекордов", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn {
                items(records) { record ->
                    RecordItem(record = record)
                }
            }
        }
    }
}

@Composable
fun RecordItem(record: RecordWithUserName) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(record.fullName, style = MaterialTheme.typography.bodyLarge)
                Text("Сложность: ${record.difficulty}%", style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${record.score} очков", style = MaterialTheme.typography.headlineSmall)
                Text(
                    SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(record.date),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
