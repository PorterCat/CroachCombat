package com.example.croachcombat.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.croachcombat.database.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _showUserSelection = MutableStateFlow(false)
    val showUserSelection: StateFlow<Boolean> = _showUserSelection.asStateFlow()

    fun setCurrentUser(user: User?) {
        _currentUser.value = user
    }

    fun setSelectedTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun setShowUserSelection(show: Boolean) {
        _showUserSelection.value = show
    }
}