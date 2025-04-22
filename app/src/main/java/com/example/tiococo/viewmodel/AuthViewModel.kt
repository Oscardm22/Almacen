package com.example.tiococo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    // Estados para la UI
    private val _uiState = MutableStateFlow<ForgotPasswordState>(ForgotPasswordState.Idle)
    val uiState: StateFlow<ForgotPasswordState> = _uiState.asStateFlow()

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _uiState.value = ForgotPasswordState.Loading

            // Simular retraso de red (1.5 segundos)
            delay(1500)

            val userExists = mockUsers.any { it.email == email }

            _uiState.value = if (userExists) {
                ForgotPasswordState.Success("Correo de recuperación enviado a $email\n(Simulación)")
            } else {
                ForgotPasswordState.Error("No existe una cuenta con este correo")
            }
        }
    }

    // Modelo de datos mock
    data class MockUser(val email: String, val password: String)

    // Sellado de estados para la UI
    sealed class ForgotPasswordState {
        object Idle : ForgotPasswordState()
        object Loading : ForgotPasswordState()
        data class Success(val message: String) : ForgotPasswordState()
        data class Error(val errorMessage: String) : ForgotPasswordState()
    }
}