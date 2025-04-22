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
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(
                        this@ForgotPasswordActivity,
                        "Error al recuperar contraseña",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showNewPasswordDialog(username: String) {
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_new_password, null)

        val etNewPassword = dialogView.findViewById<EditText>(R.id.etNewPassword)
        val etConfirmPassword = dialogView.findViewById<EditText>(R.id.etConfirmPassword)
        val tvPasswordStrength = dialogView.findViewById<TextView>(R.id.tvPasswordStrength)

        // Cambiar el color de los elementos si quieres personalizar aún más el diseño
        tvPasswordStrength.setTextColor(ContextCompat.getColor(this, R.color.Brunswick_green))

        builder.setView(dialogView)
            .setPositiveButton("Confirmar") { dialog, _ ->
                val newPassword = etNewPassword.text.toString().trim()
                val confirmPassword = etConfirmPassword.text.toString().trim()

                if (newPassword.isNotEmpty() && newPassword == confirmPassword) {
                    // Encriptamos la nueva contraseña
                    val encryptedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt(12))

                    // Actualizamos la contraseña en Firestore
                    updatePasswordInFirestore(username, encryptedPassword)
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "Las contraseñas no coinciden o están vacías", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updatePasswordInFirestore(username: String, encryptedPassword: String) {
        val userRef = FirebaseFirestore.getInstance()
            .collection("usuarios")
            .document(username)

        val updatedData = mapOf("password" to encryptedPassword)

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