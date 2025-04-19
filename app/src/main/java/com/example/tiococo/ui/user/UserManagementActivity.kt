package com.example.tiococo.ui.user

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.example.tiococo.databinding.ActivityUserManagementBinding
import com.example.tiococo.R

class UserManagementActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserManagementBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUserData()
        setupButtons()
    }

    private fun setupUserData() {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        binding.tvUsername.text = sharedPref.getString("username", "Usuario no disponible")
        binding.tvEmail.text = sharedPref.getString("email", "Correo no disponible")
    }

    private fun setupButtons() {
        binding.btnChangeEmail.setOnClickListener {
            showChangeEmailDialog()
        }

        binding.btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }
    }

    private fun showChangeEmailDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_email, null)
        val editNewEmail = dialogView.findViewById<EditText>(R.id.etNewEmail)

        AlertDialog.Builder(this)
            .setTitle("Cambiar correo electrónico")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val newEmail = editNewEmail.text.toString()
                if (isValidEmail(newEmail)) {
                    updateEmail(newEmail)
                } else {
                    Toast.makeText(this, "Ingrese un correo válido", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun updateEmail(newEmail: String) {
        getSharedPreferences("user_prefs", MODE_PRIVATE).edit {
            putString("email", newEmail)
        }
        binding.tvEmail.text = newEmail
        Toast.makeText(this, "Correo actualizado", Toast.LENGTH_SHORT).show()
    }

    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null)
        val editCurrentPass = dialogView.findViewById<EditText>(R.id.etCurrentPassword)
        val editNewPass = dialogView.findViewById<EditText>(R.id.etNewPassword)
        val editConfirmPass = dialogView.findViewById<EditText>(R.id.etConfirmPassword)

        AlertDialog.Builder(this)
            .setTitle("Cambiar contraseña")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val currentPass = editCurrentPass.text.toString()
                val newPass = editNewPass.text.toString()
                val confirmPass = editConfirmPass.text.toString()

                if (validatePasswordChange(currentPass, newPass, confirmPass)) {
                    updatePassword(newPass)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun validatePasswordChange(
        currentPass: String,
        newPass: String,
        confirmPass: String
    ): Boolean {
        val savedPass = getSharedPreferences("user_prefs", MODE_PRIVATE)
            .getString("password", "") ?: ""

        return when {
            currentPass != savedPass -> {
                Toast.makeText(this, "Contraseña actual incorrecta", Toast.LENGTH_SHORT).show()
                false
            }
            newPass.length < 6 -> {
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                false
            }
            newPass != confirmPass -> {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun updatePassword(newPassword: String) {
        getSharedPreferences("user_prefs", MODE_PRIVATE).edit {
            putString("password", newPassword)
        }
        Toast.makeText(this, "Contraseña actualizada", Toast.LENGTH_SHORT).show()
    }
}