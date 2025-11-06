package com.example.croachcombat.viewmodels

import androidx.lifecycle.ViewModel
import com.example.croachcombat.database.GameRepository
import com.example.croachcombat.database.User
import com.example.croachcombat.GameSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SharedViewModel(
    private val repository: GameRepository
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _settings = MutableStateFlow(GameSettings())
    val settings: StateFlow<GameSettings> = _settings.asStateFlow()

    fun setCurrentUser(user: User?) {
        _currentUser.value = user
    }

    fun setSelectedTab(index: Int) {
        _selectedTab.value = index
    }

    fun updateSettings(s: GameSettings) {
        _settings.value = s
    }

    fun getUsers() = repository.getAllUsers()
}
