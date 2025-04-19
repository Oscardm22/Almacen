package com.example.tiococo.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tiococo.databinding.ActivityLoginBinding
import com.example.tiococo.ui.home.HomeActivity
import androidx.core.content.edit

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        // Botón de Login
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString()
            val password = binding.etPassword.text.toString()

            if (validateInputs(username, password)) {
                performLogin(username, password)
            }
        }

        // Texto "Olvidé mi contraseña"
        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun validateInputs(username: String, password: String): Boolean {
        return when {
            username.isEmpty() -> {
                binding.etUsername.error = "Ingrese su nombre de usuario"
                false
            }
            password.isEmpty() -> {
                binding.etPassword.error = "Ingrese su contraseña"
                false
            }
            password.length < 6 -> {
                binding.etPassword.error = "La contraseña debe tener al menos 6 caracteres"
                false
            }
            else -> true
        }
    }

    private fun performLogin(username: String, password: String) {
        // Aquí iría tu lógica de autenticación real
        // Esto es un ejemplo con credenciales hardcodeadas
        if (username == "admin" && password == "123456") {
            // Login exitoso
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        } else {
            Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
        }
    }
}