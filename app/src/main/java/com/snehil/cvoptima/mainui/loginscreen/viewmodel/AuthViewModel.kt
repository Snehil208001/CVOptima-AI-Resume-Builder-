package com.snehil.cvoptima.mainui.loginscreen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snehil.cvoptima.core.common.getErrorMessage
import com.snehil.cvoptima.data.local.dao.TokenDao
import com.snehil.cvoptima.data.local.entity.TokenEntity
import com.snehil.cvoptima.data.remote.ApiService
import com.snehil.cvoptima.data.remote.model.AuthRequest
import com.snehil.cvoptima.data.remote.model.RegisterRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val apiService: ApiService,
    private val tokenDao: TokenDao
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Username and password cannot be empty")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = apiService.login(AuthRequest(username = username.trim(), password = password))
                tokenDao.saveToken(TokenEntity(token = response.token))
                _authState.value = AuthState.Success(response.username)
            } catch (t: Throwable) {
                _authState.value = AuthState.Error(t.getErrorMessage())
            }
        }
    }

    fun register(username: String, email: String, password: String, firstName: String, lastName: String) {
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Username, email, and password cannot be empty")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = apiService.register(
                    RegisterRequest(
                        username = username.trim(),
                        email = email.trim(),
                        password = password,
                        firstName = firstName.trim().ifBlank { null },
                        lastName = lastName.trim().ifBlank { null }
                    )
                )
                tokenDao.saveToken(TokenEntity(token = response.token))
                _authState.value = AuthState.Success(response.username)
            } catch (t: Throwable) {
                _authState.value = AuthState.Error(t.getErrorMessage())
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
