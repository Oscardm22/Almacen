package com.example.tiococo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tiococo.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UserViewModel : ViewModel() {

    private val userRepository = UserRepository()

    sealed class RegisterState {
        object Idle : RegisterState()
        object Loading : RegisterState()
        object Success : RegisterState()
        data class Error(val message: String) : RegisterState()
    }

    private val _uiState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val uiState: StateFlow<RegisterState> = _uiState

    fun registerUser(usuario: String, encryptedPassword: String) {
        viewModelScope.launch {
            _uiState.value = RegisterState.Loading

            try {
                val exists = userRepository.userExists(usuario)
                if (exists) {
                    _uiState.value = RegisterState.Error("Este usuario ya está registrado")
                    return@launch
                }

                // Crear solo un mapa con el campo 'password'
                val userData = mapOf(
                    "password" to encryptedPassword
                )

                userRepository.createUser(usuario, userData)
                _uiState.value = RegisterState.Success

            } catch (e: Exception) {
                _uiState.value = RegisterState.Error("Error al registrar el usuario: ${e.message}")
            }
        }
    }

}