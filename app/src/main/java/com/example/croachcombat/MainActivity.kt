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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.croachcombat.ui.theme.CroachCombatTheme
import java.text.SimpleDateFormat
import java.util.*
import android.app.DatePickerDialog
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.ui.res.painterResource

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
                    RegistrationScreen()
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CroachCombatTheme {
        Greeting("Android")
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
        Text("Регистрация игрока", fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("ФИО") },
            modifier = Modifier.fillMaxWidth()
        )

        Text("Пол:")
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

        Text("Уровень сложности: $difficulty")
        Slider(
            value = difficulty.toFloat(),
            onValueChange = { difficulty = it.toInt() },
            valueRange = 0f..100f,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Дата рождения:")
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
        Text("Курс:")
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
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Данные игрока:", fontWeight = FontWeight.Bold)
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

fun calculateZodiac(date: Date): ZodiacSign {
    val cal = Calendar.getInstance().apply { time = date }
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val month = cal.get(Calendar.MONTH) + 1
    return when (month) {
        1  -> if (day < 20) ZodiacSign.Capricorn else ZodiacSign.Aquarius
        2  -> if (day < 19) ZodiacSign.Aquarius else ZodiacSign.Pisces
        3  -> if (day < 21) ZodiacSign.Pisces else ZodiacSign.Aries
        4  -> if (day < 20) ZodiacSign.Aries else ZodiacSign.Taurus
        5  -> if (day < 21) ZodiacSign.Taurus else ZodiacSign.Gemini
        6  -> if (day < 21) ZodiacSign.Gemini else ZodiacSign.Cancer
        7  -> if (day < 23) ZodiacSign.Cancer else ZodiacSign.Leo
        8  -> if (day < 23) ZodiacSign.Leo else ZodiacSign.Virgo
        9  -> if (day < 23) ZodiacSign.Virgo else ZodiacSign.Libra
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
        RegistrationScreen()
    }
}