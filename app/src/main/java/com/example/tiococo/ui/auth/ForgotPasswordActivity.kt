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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.example.tiococo.R
import org.mindrot.jbcrypt.BCrypt

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUI()
    }

    private fun setupUI() {
        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            // Reemplazo de launchWhenStarted con repeatOnLifecycle
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    when (state) {
                        ForgotPasswordState.Idle -> resetUIState()
                        ForgotPasswordState.Loading -> showLoadingState()
                        is ForgotPasswordState.Success -> handleSuccessState(state)
                        is ForgotPasswordState.Error -> handleErrorState(state)
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnRecover.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()

            if (username.isEmpty()) {
                binding.etUsername.error = "Ingrese el nombre de usuario"
                return@setOnClickListener
            }


            lifecycleScope.launch {
                try {
                    // Consultar Firestore para ver si el usuario existe
                    val userDoc = FirebaseFirestore.getInstance()
                        .collection("usuarios")
                        .document(username)
                        .get()
                        .await()

                    if (userDoc.exists()) {
                        // Si el usuario existe, mostramos el diálogo para ingresar nueva contraseña
                        showNewPasswordDialog(username)
                    } else {
                        // Si no existe, mostramos un mensaje de error
                        Toast.makeText(
                            this@ForgotPasswordActivity,
                            "Usuario no encontrado",
                            Toast.LENGTH_SHORT
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