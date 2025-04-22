package com.example.tiococo.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.example.tiococo.data.model.Usuario
import kotlinx.coroutines.tasks.await
import org.mindrot.jbcrypt.BCrypt

class UserRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val usersRef = firestore.collection("usuarios")

    // Verifica si un usuario con username y contraseña existe (optimizado)
    suspend fun verifyUser(username: String, password: String): Usuario? {
        return try {
            // 1. Consulta SOLO el documento del usuario (no uses whereEqualTo)
            val userDoc = FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(username) // Usa el username como ID del documento
                .get()
                .await()

            val user = userDoc.toObject(Usuario::class.java)

            // 2. Verifica la contraseña con BCrypt
            if (user != null && BCrypt.checkpw(password, user.password)) {
                user // Autenticación exitosa
            } else {
                null // Credenciales inválidas
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error en verifyUser", e)
            null
        }
    }

    // Obtener todos los usuarios (opcional)
    suspend fun getAllUsers(): List<Usuario> {
        val snapshot = usersRef.get().await()
        return snapshot.toObjects(Usuario::class.java)
    }

    // Crear un nuevo usuario
    suspend fun createUser(username: String, userData: Map<String, Any>) {
        FirebaseFirestore.getInstance()
            .collection("usuarios")
            .document(username) // El documento se crea con el username como ID
            .set(userData) // Solo pasamos 'password'
            .await()
    }


    suspend fun userExists(username: String): Boolean {
        return try {
            val doc = FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(username)
                .get()
                .await()
            doc.exists()
        } catch (e: Exception) {
            Log.e("Firestore", "Error al verificar si existe el usuario", e)
            false
        }
    }

}