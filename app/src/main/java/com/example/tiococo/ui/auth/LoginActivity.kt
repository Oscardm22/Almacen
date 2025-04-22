package com.example.tiococo.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tiococo.databinding.ActivityLoginBinding
import com.example.tiococo.ui.home.HomeActivity
import androidx.core.content.edit
import com.example.tiococo.data.repository.UserRepository
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificar si ya hay una sesión activa
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)

        if (isLoggedIn) {
            // Redirigir al Home directamente
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            return
        }

        // Si no hay sesión activa, continuar con el login
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
            else -> true
        }
    }

    private fun performLogin(username: String, password: String) {
        val userRepository = UserRepository()

        lifecycleScope.launch {
            try {
                val user = userRepository.verifyUser(username, password)
                if (user != null) {
                    // Guardar sesión (asegurando que username es el ID del documento)
                    getSharedPreferences("user_prefs", MODE_PRIVATE).edit {
                        putBoolean("is_logged_in", true)
                        putString("username", username) // <- Usar el parámetro username directamente
                        putString("password_hash", user.password)
                        apply()
                    }


            // Redirigir al Home
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        } else {
            Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
        }
    }
}