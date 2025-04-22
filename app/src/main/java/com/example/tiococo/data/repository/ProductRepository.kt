package com.example.tiococo.data.repository

import android.util.Log
import com.example.tiococo.data.model.Product
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ProductRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val productosRef = firestore.collection("productos")

    // Retorna Pair<ID, Product> para manejar ambos datos
    fun getProducts(): Flow<List<Product>> = callbackFlow {
        val listener = productosRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val products = snapshot.documents.map { doc ->
                doc.toObject(Product::class.java)!!.copy(id = doc.id)
            }
            trySend(products)
        }
        awaitClose { listener.remove() }
    }

    suspend fun saveProduct(product: Product): String {
        return try {
            val docRef = productosRef.add(product).await()
            docRef.id
        } catch (e: Exception) {
            Log.e("SaveFlow", "Error al guardar", e)
            throw e
        }
    }

    suspend fun updateProduct(id: String, product: Product) {
        productosRef.document(id).set(product).await()
    }

    suspend fun deleteProduct(id: String) {
        productosRef.document(id).delete().await()
    }
}