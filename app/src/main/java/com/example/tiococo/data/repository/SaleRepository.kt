package com.example.tiococo.data.repository

import com.example.tiococo.data.model.SaleRecord
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SaleRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val ventasRef = firestore.collection("ventas")

    // 1. Obtener historial de ventas en tiempo real
    fun getSalesHistory(): Flow<List<SaleRecord>> = callbackFlow {
        val listener = ventasRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val ventas = snapshot.toObjects(SaleRecord::class.java)
            trySend(ventas)
        }
        awaitClose { listener.remove() }
    }

    // 2. Registrar nueva venta
    suspend fun saveSale(sale: SaleRecord) {
        ventasRef.document(sale.id).set(sale).await()
    }

    // 3. Eliminar una venta
    suspend fun deleteSale(saleId: String) {
        ventasRef.document(saleId).delete().await()
    }

    // 4. Obtener ventas una sola vez (opcional)
    suspend fun getSalesOnce(): List<SaleRecord> {
        val snapshot: QuerySnapshot = ventasRef.get().await()
        return snapshot.toObjects(SaleRecord::class.java)
    }
}
