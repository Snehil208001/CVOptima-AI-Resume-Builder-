package com.snehil.cvoptima.mainui.loginscreen.viewmodel

sealed interface AuthState {
    object Idle : AuthState
    object Loading : AuthState
    data class Success(val username: String) : AuthState
    data class Error(val message: String) : AuthState
}
