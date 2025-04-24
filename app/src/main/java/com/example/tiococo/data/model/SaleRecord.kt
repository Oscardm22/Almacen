package com.example.tiococo.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SaleRecord(
    val id: String = "",
    val date: String = "",
    val totalDollars: Double = 0.0,
    val exchangeRate: Double = 1.0, // Cambiado a no-nullable con valor por defecto
    val products: List<Product> = emptyList()
) : Parcelable {
    // Propiedad calculada para total en Bs
    val totalBs: Double get() = totalDollars * exchangeRate
}