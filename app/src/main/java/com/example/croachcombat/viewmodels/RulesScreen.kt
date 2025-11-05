package com.example.croachcombat.viewmodels

import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.croachcombat.R

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