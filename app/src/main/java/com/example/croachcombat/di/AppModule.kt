package com.example.croachcombat.di

import android.content.Context
import com.example.croachcombat.database.GameDatabase
import com.example.croachcombat.database.GameRepository
import com.example.croachcombat.network.CbrApiService
import com.example.croachcombat.viewmodels.GameViewModel
import com.example.croachcombat.viewmodels.MainViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { GameDatabase.getInstance(androidContext()) }
    single { GameRepository(get()) }
    single { CbrApiService.create() }

    viewModel {
        GameViewModel(
            repository = get(),
            cbrApiService = get(),
            context = androidContext()
        )
    }

    viewModel { MainViewModel() }
}