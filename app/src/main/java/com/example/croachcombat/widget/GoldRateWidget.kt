package com.example.croachcombat.widget

import android.content.Context
import androidx.compose.ui.unit.TextUnit
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.work.CoroutineWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.croachcombat.network.CbrApiService
import com.example.croachcombat.widget.GoldRateWorker.Companion.scheduleWidgetUpdate
import kotlinx.coroutines.delay
import java.text.DecimalFormat
import java.text.ParseException
import java.util.concurrent.TimeUnit

class GoldRateWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val goldRate = WidgetDataStore.getGoldRate(context)

            Column(
                modifier = androidx.glance.GlanceModifier
                    .fillMaxSize(),
//                    .background(androidx.glance.BackgroundModifier. RadialGradient(
//                        colors = listOf(
//                            ColorProvider(androidx.compose.ui.graphics.Color(0xFFFFD700)),
//                            ColorProvider(androidx.compose.ui.graphics.Color(0xFFFFA500))
//                        )
//                    )),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Золото ЦБ",
                    style = TextStyle(
                        color = ColorProvider(androidx.compose.ui.graphics.Color.White)
                    )
                )
                Text(
                    text = if (goldRate > 0) "${DecimalFormat("#.##").format(goldRate)} руб/г" else "Загрузка...",
                    style = TextStyle(
                        color = ColorProvider(androidx.compose.ui.graphics.Color.White),
                        //fontSize = androidx.glance.unit.TextUnit(12f)
                    )
                )
            }
        }
    }

    class GoldRateWidgetReceiver : GlanceAppWidgetReceiver() {
        override val glanceAppWidget: GlanceAppWidget = GoldRateWidget()

        override fun onEnabled(context: Context?) {
            super.onEnabled(context)
            scheduleWidgetUpdate(context!!)
        }
    }
}

class GoldRateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val apiService = CbrApiService.create()
            val response = apiService.getDailyRates()
            val goldValute = response.valutes.find { it.charCode == "XAU" }

            goldValute?.let {
                val rate = parseRate(it.value) / it.nominal
                WidgetDataStore.saveGoldRate(applicationContext, rate)

                // Обновляем все виджеты
                GoldRateWidget().updateAll(applicationContext)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            // Повторяем через 30 минут при ошибке
            delay(30 * 60 * 1000L)
            Result.retry()
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

    companion object {
        fun scheduleWidgetUpdate(context: Context) {
            val periodicWorkRequest = PeriodicWorkRequestBuilder<GoldRateWorker>(
                2, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueue(periodicWorkRequest)
        }
    }
}

object WidgetDataStore {
    private const val PREF_NAME = "gold_widget_prefs"
    private const val KEY_GOLD_RATE = "gold_rate"

    fun saveGoldRate(context: Context, rate: Double) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_GOLD_RATE, rate.toFloat())
            .apply()
    }

    fun getGoldRate(context: Context): Double {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getFloat(KEY_GOLD_RATE, 0f).toDouble()
    }
}