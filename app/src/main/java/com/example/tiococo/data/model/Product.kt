package com.example.tiococo.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Product(
    val id: String = "",  // Añade este campo
    val name: String,
    val quantity: Int,
    val priceDollars: Double,
) : Parcelable {
    // Constructor sin argumentos para Firebase
    constructor() : this("", "", 0, 0.0)
}