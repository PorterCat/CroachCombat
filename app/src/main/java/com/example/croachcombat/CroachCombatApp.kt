package com.example.croachcombat

import android.app.Application
import com.example.croachcombat.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class CroachCombatApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@CroachCombatApp)
            modules(appModule)
        }
    }
}