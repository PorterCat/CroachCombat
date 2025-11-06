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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.croachcombat.database.GameDatabase
import org.koin.androidx.compose.get
import com.example.croachcombat.database.GameRepository
import com.example.croachcombat.database.User
import com.example.croachcombat.network.CbrApiService
import com.example.croachcombat.ui.theme.CroachCombatTheme
import com.example.croachcombat.viewmodels.AuthorsScreen
import com.example.croachcombat.viewmodels.GameViewModel
import com.example.croachcombat.viewmodels.MainViewModel
import com.example.croachcombat.viewmodels.RecordsScreen
import com.example.croachcombat.viewmodels.RegistrationScreen
import com.example.croachcombat.viewmodels.RulesScreen
import com.example.croachcombat.viewmodels.SettingsScreen
import com.example.croachcombat.widget.GoldRateWidget
import com.example.croachcombat.widget.GoldRateWorker
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CroachCombatTheme {
                TabScreen()
            }
        }
    }
}

@Composable
fun TabScreen() {
    val mainViewModel: MainViewModel = koinViewModel()
    val repository: GameRepository = get()

    // Collect state from MainViewModel
    val currentUser by mainViewModel.currentUser.collectAsStateWithLifecycle()
    val selectedTab by mainViewModel.selectedTab.collectAsStateWithLifecycle()
    val showUserSelection by mainViewModel.showUserSelection.collectAsStateWithLifecycle()

    val tabs = listOf("Играть", "Регистрация", "Правила", "Авторы", "Настройки", "Рекорды")

    // Получаем GameViewModel
    val gameViewModel: GameViewModel = koinViewModel()

    var settings by remember { mutableStateOf(GameSettings()) }

    // Показываем диалог выбора пользователя когда нужно
    LaunchedEffect(selectedTab) {
        if (selectedTab == 0 && currentUser == null) {
            mainViewModel.setShowUserSelection(true)
        }
    }

    if (showUserSelection) {
        UserSelectionDialog(
            repository = repository,
            onUserSelected = { user ->
                mainViewModel.setCurrentUser(user)
                mainViewModel.setShowUserSelection(false)
            },
            onNewUser = {
                mainViewModel.setSelectedTab(1)
                mainViewModel.setShowUserSelection(false)
            },
            onDismiss = { mainViewModel.setShowUserSelection(false) }
        )
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .systemBarsPadding()
    ) {
        if (currentUser != null) {
            CurrentUserBar(
                user = currentUser!!,
                onLogout = {
                    mainViewModel.setCurrentUser(null)
                    gameViewModel.stopGame()
                },
                onSelectDifferent = { mainViewModel.setShowUserSelection(true) }
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
                    gameViewModel = gameViewModel,
                    modifier = Modifier.fillMaxSize()
                )
                1 -> RegistrationScreen(
                    repository = repository,
                    onUserRegistered = { user ->
                        mainViewModel.setCurrentUser(user)
                        mainViewModel.setSelectedTab(0)
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
                    onClick = { mainViewModel.setSelectedTab(index) }
                )
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

class GameViewModelFactory(
    private val repository: GameRepository,
    private val cbrApiService: CbrApiService,
    private val context: android.content.Context
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            return GameViewModel(repository, cbrApiService, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}

@Preview(showBackground = true)
@Composable
fun RegistrationScreenPreview() {
    CroachCombatTheme {
        // TabScreen()
    }
}