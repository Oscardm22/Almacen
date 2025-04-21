package com.example.tiococo.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Product(
    val id: String = "",  // Añade este campo
    val name: String,
    val quantity: Int,
    val priceDollars: Double,  // Precio base en dólares
    var priceBolivares: Double, // Precio calculado
    val lastRateUpdate: String = "" // Fecha última actualización
) : Parcelable