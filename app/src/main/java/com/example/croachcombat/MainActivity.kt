package com.example.croachcombat

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.croachcombat.ui.theme.CroachCombatTheme
import java.text.SimpleDateFormat
import java.util.*
import android.app.DatePickerDialog
import android.content.Intent
import android.webkit.WebView
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.rememberScrollState as RememberScrollState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CroachCombatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TabScreen()
                }
            }
        }
    }
}

data class Author(
    val name: String,
    @DrawableRes val photoRes: Int
)

data class GameSettings(
    var gameSpeed: Float = 1f,
    var maxCockroaches: Int = 10,
    var bonusInterval: Int = 30,
    var roundDuration: Int = 60
)

@Composable
fun TabScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Играть", "Регистрация", "Правила", "Авторы", "Настройки")

    var settings by remember { mutableStateOf(GameSettings()) }

    Column(modifier = Modifier
        .fillMaxSize()
        .systemBarsPadding() // учёт системных панелей сверху/снизу
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(top = 16.dp, bottom = 4.dp)
        ) {
            when (selectedTab) {
                0 -> GameScreen(settings = settings)
                1 -> RegistrationScreen()
                2 -> RulesScreen()
                3 -> AuthorsScreen()
                4 -> SettingsScreen(settings = settings, onSettingsChange = { settings = it })
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding() // отодвигаем TabRow от навигационных кнопок
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

@SuppressLint("AutoboxingStateCreation")
@Composable
fun RegistrationScreen() {
    var fullName by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf(Gender.Male) }
    var course by remember { mutableStateOf<Byte>(1) }
    var difficulty by remember { mutableIntStateOf(50) }
    var birthDate by remember { mutableStateOf(Date()) }
    var showResult by remember { mutableStateOf(false) }

    val player by remember(fullName, gender, birthDate, difficulty, course) {
        derivedStateOf {
            Player(
                fullName = fullName,
                gender = gender,
                birthday = birthDate,
                difficulty = difficulty,
                course = course,
                zodiacSign = calculateZodiac(birthDate)
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Регистрация игрока", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("ФИО") },
            modifier = Modifier.fillMaxWidth()
        )

        Text("Пол:", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = gender == Gender.Male, onClick = { gender = Gender.Male })
                Text("Мужской", modifier = Modifier.padding(start = 4.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = gender == Gender.Female, onClick = { gender = Gender.Female })
                Text("Женский", modifier = Modifier.padding(start = 4.dp))
            }
        }

        CourseSelector(selectedCourse = course, onCourseSelected = { course = it })

        Text("Уровень сложности: $difficulty", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = difficulty.toFloat(),
            onValueChange = { difficulty = it.toInt() },
            valueRange = 0f..100f,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Дата рождения:", style = MaterialTheme.typography.bodyMedium)
        EasyDatePicker(
            selectedDate = birthDate,
            onDateSelected = { birthDate = it },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { showResult = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Зарегистрировать")
        }

        if (showResult) {
            ResultCard(player = player)
        }
    }
}

@Composable
fun CourseSelector(selectedCourse: Byte, onCourseSelected: (Byte) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text("Курс:", style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("$selectedCourse курс")
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            (1..5).forEach { num ->
                DropdownMenuItem(
                    text = { Text("$num курс") },
                    onClick = {
                        onCourseSelected(num.toByte())
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun EasyDatePicker(
    selectedDate: Date,
    onDateSelected: (Date) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.clickable { showDialog = true }) {
        OutlinedTextField(
            value = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(selectedDate),
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
            enabled = false
        )
    }

    if (showDialog) {
        val calendar = Calendar.getInstance().apply { time = selectedDate }
        DatePickerDialog(
            LocalContext.current,
            { _, year, month, day ->
                Calendar.getInstance().apply {
                    set(year, month, day)
                    onDateSelected(time)
                }
                showDialog = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}

@Composable
fun ResultCard(player: Player) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Данные игрока:", style = MaterialTheme.typography.headlineSmall)
            Text("ФИО: ${player.fullName}")
            Text("Пол: ${if (player.gender == Gender.Male) "Мужской" else "Женский"}")
            Text("Курс: ${player.course}")
            Text("Уровень сложности: ${player.difficulty}")

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Дата рождения: ${SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(player.birthday)}")
                Spacer(modifier = Modifier.width(20.dp))
                Image(
                    painter = painterResource(id = player.zodiacSign.getIconRes()),
                    contentDescription = player.zodiacSign.name,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

@Composable
fun RulesScreen() {
    val context = LocalContext.current
    val htmlContent = remember {
        try {
            context.resources.openRawResource(R.raw.rules)
                .bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "<h1>Файл правил не найден</h1>"
        }
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = false
                loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun AuthorsScreen() {
    val authors = listOf(
        Author("Мищук Андрей", R.drawable.author1),
        Author("Ягодкин Никита", R.drawable.author2),
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(authors) { author ->
            AuthorItem(author = author)
        }
    }
}

@Composable
fun AuthorItem(author: Author) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = author.photoRes),
                contentDescription = "Фото ${author.name}",
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = author.name,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

fun calculateZodiac(date: Date): ZodiacSign {
    val cal = Calendar.getInstance().apply { time = date }
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val month = cal.get(Calendar.MONTH) + 1
    return when (month) {
        1 -> if (day < 20) ZodiacSign.Capricorn else ZodiacSign.Aquarius
        2 -> if (day < 19) ZodiacSign.Aquarius else ZodiacSign.Pisces
        3 -> if (day < 21) ZodiacSign.Pisces else ZodiacSign.Aries
        4 -> if (day < 20) ZodiacSign.Aries else ZodiacSign.Taurus
        5 -> if (day < 21) ZodiacSign.Taurus else ZodiacSign.Gemini
        6 -> if (day < 21) ZodiacSign.Gemini else ZodiacSign.Cancer
        7 -> if (day < 23) ZodiacSign.Cancer else ZodiacSign.Leo
        8 -> if (day < 23) ZodiacSign.Leo else ZodiacSign.Virgo
        9 -> if (day < 23) ZodiacSign.Virgo else ZodiacSign.Libra
        10 -> if (day < 23) ZodiacSign.Libra else ZodiacSign.Scorpio
        11 -> if (day < 22) ZodiacSign.Scorpio else ZodiacSign.Sagittarius
        12 -> if (day < 22) ZodiacSign.Sagittarius else ZodiacSign.Capricorn
        else -> ZodiacSign.Capricorn
    }
}

@DrawableRes
fun ZodiacSign.getIconRes(): Int = when (this) {
    ZodiacSign.Aries -> R.drawable.aries
    ZodiacSign.Taurus -> R.drawable.taurus
    ZodiacSign.Gemini -> R.drawable.gemini
    ZodiacSign.Cancer -> R.drawable.cancer
    ZodiacSign.Leo -> R.drawable.leo
    ZodiacSign.Virgo -> R.drawable.virgo
    ZodiacSign.Libra -> R.drawable.libra
    ZodiacSign.Scorpio -> R.drawable.scorpio
    ZodiacSign.Sagittarius -> R.drawable.sagittarius
    ZodiacSign.Capricorn -> R.drawable.capricorn
    ZodiacSign.Aquarius -> R.drawable.aquarius
    ZodiacSign.Pisces -> R.drawable.pisces
}

@Preview(showBackground = true)
@Composable
fun RegistrationScreenPreview() {
    CroachCombatTheme {
        TabScreen()
    }
}