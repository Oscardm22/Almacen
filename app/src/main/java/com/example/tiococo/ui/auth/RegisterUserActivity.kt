package com.example.tiococo.ui.auth

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.tiococo.databinding.ActivityRegisterUserBinding
import com.example.tiococo.viewmodel.UserViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.mindrot.jbcrypt.BCrypt

class RegisterUserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterUserBinding
    private val viewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterUserBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUI()
    }

    private fun setupUI() {
        binding.btnRegister.setOnClickListener {
            val username = binding.etName.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (username.length > 30 || password.length > 30) {
                Toast.makeText(this, "El nombre de usuario y la contraseña deben tener 30 caracteres o menos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val valid = validateInputs(username, password)

            if (valid) {
                // Encriptar la contraseña antes de enviarla al ViewModel
                val encryptedPassword = encryptPassword(password)

                // Pasar el nombre de usuario y la contraseña encriptada al ViewModel
                viewModel.registerUser(username, encryptedPassword)
            }
        }

        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                Log.d("RegisterState", state.toString())
                when (state) {
                    is UserViewModel.RegisterState.Success -> {
                        showToast("Usuario registrado correctamente")
                        finish()
                    }
                    is UserViewModel.RegisterState.Error -> {
                        showToast("Error: ${state.message}")
                    }
                    UserViewModel.RegisterState.Loading -> {
                        binding.btnRegister.isEnabled = false
                    }
                    UserViewModel.RegisterState.Idle -> {
                        binding.btnRegister.isEnabled = true
                    }
                }
            }
        }
    }

    private fun validateInputs(name: String, password: String): Boolean {
        return when {
            name.isEmpty() -> {
                binding.etName.error = "Ingrese el nombre de usuario"
                false
            }

            password.length < 6 -> {
                binding.etPassword.error = "Mínimo 6 caracteres"
                false
            }
            else -> true
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    fun encryptPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())  // Genera el hash de la contraseña
    }
}