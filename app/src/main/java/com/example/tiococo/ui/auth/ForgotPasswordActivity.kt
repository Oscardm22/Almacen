package com.example.tiococo.ui.auth

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.tiococo.databinding.ActivityForgotPasswordBinding
import com.example.tiococo.viewmodel.AuthViewModel
import com.example.tiococo.viewmodel.AuthViewModel.ForgotPasswordState
import kotlinx.coroutines.launch

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    ForgotPasswordState.Idle -> {
                        // Estado inicial, no hacer nada
                    }
                    ForgotPasswordState.Loading -> {
                        binding.progressBar.isVisible = true
                        binding.btnRecover.isEnabled = false
                        binding.etUsername.isEnabled = false
                    }
                    is ForgotPasswordState.Success -> {
                        binding.progressBar.isVisible = false
                        binding.btnRecover.isEnabled = true
                        binding.etUsername.isEnabled = true
                        Toast.makeText(
                            this@ForgotPasswordActivity,
                            state.message,
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                    is ForgotPasswordState.Error -> {
                        binding.progressBar.isVisible = false
                        binding.btnRecover.isEnabled = true
                        binding.etUsername.isEnabled = true
                        binding.etUsername.error = state.errorMessage
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnRecover.setOnClickListener {
            val email = binding.etUsername.text.toString().trim()

            if (validateEmail(email)) {
                viewModel.sendPasswordResetEmail(email)
            }
        }
    }

    private fun validateEmail(email: String): Boolean {
        return when {
            email.isEmpty() -> {
                binding.etUsername.error = "Ingrese su correo electrónico"
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.etUsername.error = "Ingrese un correo válido"
                false
            }
            else -> true
        }
    }
}