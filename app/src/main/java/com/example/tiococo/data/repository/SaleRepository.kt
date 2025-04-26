package com.example.tiococo.data.repository

import android.util.Log
import com.example.tiococo.data.model.Product
import com.example.tiococo.data.model.SaleRecord
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SaleRepository(
    private val productRepository: ProductRepository
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val ventasRef = firestore.collection("ventas")

    suspend fun saveSaleWithStockUpdate(sale: SaleRecord): Boolean {
        return try {
            // 1. Primero verificar el stock (fuera de la transacción)
            verifyStockAvailability(sale.products)

            // 2. Ejecutar transacción
            firestore.runTransaction { transaction ->
                // PRIMERO: Todas las lecturas
                val productSnapshots = sale.products.map { product ->
                    val productRef = firestore.collection("productos").document(product.id)
                    product to transaction.get(productRef)
                }

                // LUEGO: Todas las escrituras
                // Registrar la venta
                val saleRef = ventasRef.document(sale.id)
                transaction.set(saleRef, sale)

                // Actualizar stocks
                productSnapshots.forEach { (product, snapshot) ->
                    val currentStock = snapshot.getLong("quantity")?.toInt() ?: 0
                    val newStock = currentStock - product.quantity

                    if (newStock < 0) {
                        throw Exception("Stock insuficiente para ${product.name}")
                    }

                    transaction.update(snapshot.reference, "quantity", newStock)
                }
            }.await()
            true
        } catch (e: Exception) {
            Log.e("SaleRepo", "Error en venta con actualización de stock", e)
            false
        }
    }

    private suspend fun verifyStockAvailability(products: List<Product>) {
        products.forEach { product ->
            // Usamos el productRepository.getProductById aquí
            val currentProduct = productRepository.getProductById(product.id)
            if (!currentProduct.hasEnoughStock(product.quantity)) {
                throw Exception("Stock insuficiente para ${product.name}")
            }
        }
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

    fun deleteAllSales(onComplete: (Boolean) -> Unit) {
        ventasRef.get()
            .addOnSuccessListener { snapshot ->
                val batch = firestore.batch()
                for (document in snapshot.documents) {
                    batch.delete(document.reference)
                }
                batch.commit()
                    .addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener { onComplete(false) }
            }
            .addOnFailureListener { onComplete(false) }
    }

    suspend fun returnSaleAndRestoreStock(sale: SaleRecord): Boolean {
        return try {
            // 1. Eliminar la venta del historial
            ventasRef.document(sale.id).delete().await()

            // 2. Revertir el stock en transacción
            firestore.runTransaction { transaction ->
                // PRIMERO: Todas las lecturas
                val productSnapshots = sale.products.map { product ->
                    val productRef = firestore.collection("productos").document(product.id)
                    product to transaction.get(productRef)
                }

                // LUEGO: Todas las escrituras
                productSnapshots.forEach { (product, snapshot) ->
                    val currentStock = snapshot.getLong("quantity")?.toInt() ?: 0
                    val newStock = currentStock + product.quantity // Sumamos para revertir

                    transaction.update(snapshot.reference, "quantity", newStock)
                }
            }.await()
            true
        } catch (e: Exception) {
            Log.e("SaleRepo", "Error en devolución", e)
            false
        }
    }
}