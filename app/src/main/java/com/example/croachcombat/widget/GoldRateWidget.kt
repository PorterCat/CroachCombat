package com.example.croachcombat.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.work.*
import com.example.croachcombat.MainActivity
import com.example.croachcombat.R
import com.example.croachcombat.network.CbrApiService
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit

class GoldRateWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        scheduleWidgetUpdate(context)
    }

    override fun onDisabled(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WIDGET_UPDATE_WORK_NAME)
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prefs = context.getSharedPreferences(WIDGET_PREF_NAME, Context.MODE_PRIVATE)
        val goldRate = prefs.getFloat(KEY_GOLD_RATE, 0f)

        val views = RemoteViews(context.packageName, R.layout.widget_gold_rate)

        val rateText = if (goldRate > 0) {
            "${DecimalFormat("#.##").format(goldRate)} руб/г"
        } else {
            "Загрузка..."
        }

        views.setTextViewText(R.id.widget_rate, rateText)
        views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    companion object {
        const val WIDGET_PREF_NAME = "GoldWidgetPrefs"
        const val KEY_GOLD_RATE = "gold_rate"
        const val WIDGET_UPDATE_WORK_NAME = "gold_widget_update"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, GoldRateWidget::class.java)
            )
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val prefs = context.getSharedPreferences(WIDGET_PREF_NAME, Context.MODE_PRIVATE)
            val goldRate = prefs.getFloat(KEY_GOLD_RATE, 0f)

            val views = RemoteViews(context.packageName, R.layout.widget_gold_rate)

            views.setTextViewText(
                R.id.widget_rate,
                if (goldRate > 0) "${DecimalFormat("#.##").format(goldRate)} руб/г" else "Загрузка..." // 10313,43
            )

            views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun scheduleWidgetUpdate(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<GoldRateWorker>(
                4, TimeUnit.HOURS,
                1, TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WIDGET_UPDATE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        fun updateGoldRateImmediately(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<GoldRateWorker>().build()
            WorkManager.getInstance(context).enqueue(workRequest)
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
            val yesterday = CbrApiService.getYesterdayDate()

            val response = apiService.getMetalRates(yesterday, yesterday)
            val goldRecord = response.records.find { it.code == "1" }

            if (goldRecord != null) {
                val rateString = goldRecord.sell.replace(",", ".")
                val rate = rateString.toFloatOrNull() ?: 5432.10f

                val prefs = applicationContext.getSharedPreferences(
                    GoldRateWidget.WIDGET_PREF_NAME,
                    Context.MODE_PRIVATE
                )
                prefs.edit().putFloat(GoldRateWidget.KEY_GOLD_RATE, rate).apply()

                GoldRateWidget.updateAllWidgets(applicationContext)
            }
            Result.success()
        } catch (e: Exception) {
            val prefs = applicationContext.getSharedPreferences(
                GoldRateWidget.WIDGET_PREF_NAME,
                Context.MODE_PRIVATE
            )
            val fallbackRate = 5432.10f
            prefs.edit().putFloat(GoldRateWidget.KEY_GOLD_RATE, fallbackRate).apply()
            GoldRateWidget.updateAllWidgets(applicationContext)
            Result.success()
        }
    }
}