package com.example.tiococo.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AuthViewModel : ViewModel() {

    // Estados para la UI
    private val _uiState = MutableStateFlow<ForgotPasswordState>(ForgotPasswordState.Idle)
    val uiState: StateFlow<ForgotPasswordState> = _uiState


    // Estados para el flujo de recuperación de contraseña
    sealed class ForgotPasswordState {
        object Idle : ForgotPasswordState()
        object Loading : ForgotPasswordState()
        data class Success(val message: String) : ForgotPasswordState()
        data class Error(val errorMessage: String) : ForgotPasswordState()
    }
}