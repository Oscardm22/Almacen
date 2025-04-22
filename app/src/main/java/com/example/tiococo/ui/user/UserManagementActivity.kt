package com.example.tiococo.ui.user

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.tiococo.databinding.ActivityUserManagementBinding
import com.example.tiococo.R
import com.example.tiococo.ui.auth.RegisterUserActivity
import android.content.Intent
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import org.mindrot.jbcrypt.BCrypt

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
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val username = prefs.getString("username", "") ?: ""
        val storedPassword = prefs.getString("password", "")
        Log.d("USER_PREFS", "Contraseña almacenada: $storedPassword")
        Log.d("UserPrefs", "username: ${sharedPref.getString("username", null)}")

        // Muestra directamente el ID del documento como nombre de usuario
        binding.tvUsername.text = username

        // Opcional: Si necesitas verificar que existe en Firestore
        if (username.isNotEmpty()) {
            FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(username)
                .get()
                .addOnSuccessListener { document ->
                    if (!document.exists()) {
                        Log.w("FIREBASE", "Documento no encontrado para: $username")
                    }
                }
        }
    }

    private fun setupButtons() {

        binding.btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        binding.btnAddUser.setOnClickListener {
            startActivity(Intent(this, RegisterUserActivity::class.java))
        }
    }


    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null)
        val editCurrentPass = dialogView.findViewById<EditText>(R.id.etCurrentPassword)
        val editNewPass = dialogView.findViewById<EditText>(R.id.etNewPassword)
        val editConfirmPass = dialogView.findViewById<EditText>(R.id.etConfirmPassword)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val current = editCurrentPass.text.toString()
                val new = editNewPass.text.toString()
                val confirm = editConfirmPass.text.toString()

                if (validatePasswordChange(current, new, confirm)) {
                    updatePassword(new)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }


    private fun validatePasswordChange(current: String, new: String, confirm: String): Boolean {
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val savedHash = prefs.getString("password_hash", "") // Debe ser el hash BCrypt

        return when {
            !BCrypt.checkpw(current, savedHash) -> { // Comparación segura
                Toast.makeText(this, "Contraseña actual incorrecta", Toast.LENGTH_SHORT).show()
                false
            }
            new.length < 6 -> {
                Toast.makeText(this, "La nueva contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                false
            }
            new != confirm -> {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun updatePassword(newPassword: String) {
        val username = binding.tvUsername.text.toString().trim()
        if (username.isEmpty()) {
            Toast.makeText(this, "Nombre de usuario no válido", Toast.LENGTH_SHORT).show()
            return
        }

        val newHash = BCrypt.hashpw(newPassword, BCrypt.gensalt(12))
        Log.d("FIREBASE_DEBUG", "Actualizando password para usuario: $username")

        // Operación directa con verificación de seguridad
        FirebaseFirestore.getInstance().run {
            collection("usuarios").document(username)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        document.reference.update("password", newHash)
                            .addOnSuccessListener {
                                updateLocalPassword(newHash)
                                Toast.makeText(this@UserManagementActivity,
                                    "Contraseña actualizada", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e -> handleUpdateError(e) }
                    } else {
                        Toast.makeText(this@UserManagementActivity,
                            "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e -> handleUpdateError(e) }
        }
        Toast.makeText(this, "Contraseña actualizada", Toast.LENGTH_SHORT).show()
    }
}