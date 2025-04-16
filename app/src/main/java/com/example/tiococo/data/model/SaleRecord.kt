package com.example.tiococo.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SaleRecord(
    val id: String = "",
    val date: String = "", // Formato: "dd/MM/yyyy HH:mm"
    val total: Double = 0.0,
    val products: List<Product> = emptyList()
) : Parcelable