package com.example.tiococo.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tiococo.databinding.ActivityLoginBinding
import com.example.tiococo.ui.home.HomeActivity
import androidx.core.content.edit
import com.example.tiococo.data.repository.UserRepository
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import kotlinx.coroutines.delay
import android.view.animation.AnimationUtils
import com.example.tiococo.R

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
        // Validación campo de usuario
        binding.etUsername.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s != null && s.length > 30) {
                    binding.etUsername.error = "Máximo 30 caracteres"
                } else {
                    binding.etUsername.error = null
                }
            }
        })

        // Validación campo de contraseña
        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s != null && s.length > 30) {
                    binding.etPassword.error = "Máximo 30 caracteres"
                } else {
                    binding.etPassword.error = null
                }
            }
        })

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
            username.length > 30 -> {
                binding.etUsername.error = "Máximo 30 caracteres"
                false
            }
            password.isEmpty() -> {
                binding.etPassword.error = "Ingrese su contraseña"
                false
            }
            password.length > 30 -> {
                binding.etPassword.error = "Máximo 30 caracteres"
                false
            }
            else -> true
        }
    }

    private fun performLogin(username: String, password: String) {
        val userRepository = UserRepository()

        // Mostrar animación y deshabilitar botón
        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false
        binding.btnLogin.alpha = 0.7f

        lifecycleScope.launch {
            try {
                val user = userRepository.verifyUser(username, password)

                // Pequeño delay para que se vea la animación (opcional)
                delay(500) // Importar kotlinx.coroutines.delay

                if (user != null) {
                    // Guardar sesión
                    getSharedPreferences("user_prefs", MODE_PRIVATE).edit {
                        putBoolean("is_logged_in", true)
                        putString("username", username)
                        putString("password_hash", user.password)
                        apply()
                    }

                    startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                    finish()
                } else {
                    showLoginError()
                }
            } catch (e: Exception) {
                showLoginError(e.message ?: "Error desconocido")
            } finally {
                // Ocultar animación y habilitar botón
                binding.progressBar.visibility = View.GONE
                binding.btnLogin.isEnabled = true
                binding.btnLogin.alpha = 1f
            }
        }
    }

    private fun showLoginError(errorMessage: String? = null) {
        runOnUiThread {
            val message = errorMessage ?: "Credenciales incorrectas"
            Toast.makeText(this@LoginActivity, message, Toast.LENGTH_SHORT).show()
            binding.progressBar.visibility = View.GONE
            binding.btnLogin.isEnabled = true
            binding.btnLogin.alpha = 1f

            // Animación de shake para el formulario
            val shake = AnimationUtils.loadAnimation(this, R.anim.shake)
            binding.usernameInputLayout.startAnimation(shake)
            binding.passwordInputLayout.startAnimation(shake)
        }
    }
}