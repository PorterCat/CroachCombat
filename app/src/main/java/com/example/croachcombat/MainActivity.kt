package com.example.croachcombat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.croachcombat.database.GameDatabase
import com.example.croachcombat.database.GameRepository
import com.example.croachcombat.database.User
import com.example.croachcombat.ui.theme.CroachCombatTheme
import com.example.croachcombat.viewmodels.AuthorsScreen
import com.example.croachcombat.viewmodels.RecordsScreen
import com.example.croachcombat.viewmodels.RegistrationScreen
import com.example.croachcombat.viewmodels.RulesScreen
import com.example.croachcombat.viewmodels.SettingsScreen
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var repository: GameRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = GameDatabase.getInstance(this)
        repository = GameRepository(database)

        setContent {
            CroachCombatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TabScreen(repository = repository)
                }
            }
        }
    }
}

@Composable
fun UserSelectionDialog(
    repository: GameRepository,
    onUserSelected: (User) -> Unit,
    onNewUser: () -> Unit,
    onDismiss: () -> Unit
) {
    val users by repository.getAllUsers().collectAsState(initial = emptyList())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выберите игрока") },
        text = {
            Column {
                if (users.isEmpty()) {
                    Text("Нет зарегистрированных пользователей")
                } else {
                    LazyColumn {
                        items(users) { user ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp)
                                    .clickable { onUserSelected(user) },
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(user.fullName, style = MaterialTheme.typography.bodyLarge)
                                    Text("Курс: ${user.course}, Сложность: ${user.difficulty}")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onNewUser) {
                Text("Новый пользователь")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun TabScreen(repository: GameRepository) {
    var selectedTab by remember { mutableStateOf(0) }
    var currentUser by remember { mutableStateOf<com.example.croachcombat.database.User?>(null) }
    var showUserSelection by remember { mutableStateOf(false) }
    val tabs = listOf("Играть", "Регистрация", "Правила", "Авторы", "Настройки", "Рекорды")

    var settings by remember { mutableStateOf(GameSettings()) }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 0 && currentUser == null) {
            showUserSelection = true
        }
    }

    if (showUserSelection) {
        UserSelectionDialog(
            repository = repository,
            onUserSelected = { user ->
                currentUser = user
                showUserSelection = false
            },
            onNewUser = {
                selectedTab = 1
                showUserSelection = false
            },
            onDismiss = { showUserSelection = false }
        )
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .systemBarsPadding()
    ) {
        // Показываем текущего пользователя
        if (currentUser != null) {
            CurrentUserBar(
                user = currentUser!!,
                onLogout = { currentUser = null },
                onSelectDifferent = { showUserSelection = true }
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(top = 16.dp, bottom = 4.dp)
        ) {
            when (selectedTab) {
                0 -> GameScreen(
                    settings = settings,
                    currentUser = currentUser,
                    repository = repository,
                    modifier = Modifier.fillMaxSize()
                )
                1 -> RegistrationScreen(
                    repository = repository,
                    onUserRegistered = { user ->
                        currentUser = user
                        selectedTab = 0
                    }
                )
                2 -> RulesScreen()
                3 -> AuthorsScreen()
                4 -> SettingsScreen(settings = settings, onSettingsChange = { settings = it })
                5 -> RecordsScreen(repository = repository)
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    text = { Text(text = title, fontSize = MaterialTheme.typography.bodySmall.fontSize) },
                    selected = selectedTab == index,
                    onClick = { selectedTab = index }
                )
            }
        }
    }
}

@Composable
fun CurrentUserBar(user: User, onLogout: () -> Unit, onSelectDifferent: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(user.fullName, modifier = Modifier.weight(1f))
            Row {
                Button(
                    onClick = onSelectDifferent,
                    modifier = Modifier.height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Сменить")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onLogout,
                    modifier = Modifier.height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Выйти")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegistrationScreenPreview() {
    CroachCombatTheme {
        // TabScreen()
    }
}