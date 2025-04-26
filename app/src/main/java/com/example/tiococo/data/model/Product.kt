package com.example.tiococo.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Product(
    val id: String = "",
    val name: String,
    val quantity: Int,
    val priceDollars: Double,
) : Parcelable {
    constructor() : this("", "", 0, 0.0)

    // Método para verificar si hay stock suficiente
    fun hasEnoughStock(required: Int): Boolean = quantity >= required
}