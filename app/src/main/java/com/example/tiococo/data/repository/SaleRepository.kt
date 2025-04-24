package com.example.tiococo.data.repository

import com.example.tiococo.data.model.SaleRecord
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SaleRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val ventasRef = firestore.collection("ventas")

    // Guardar venta sin datos de usuario/dispositivo
    suspend fun saveSale(sale: SaleRecord) {
        ventasRef.document(sale.id).set(sale).await()
    }

    // Obtener todas las ventas (sin filtros)
    fun getSalesHistory(): Flow<List<SaleRecord>> = callbackFlow {
        val listener = ventasRef.addSnapshotListener { snapshot, _ ->
            snapshot?.let { trySend(it.toObjects(SaleRecord::class.java)) }
        }
        awaitClose { listener.remove() }
    }

    suspend fun deleteSale(saleId: String) {
        ventasRef.document(saleId).delete().await()
    }
}