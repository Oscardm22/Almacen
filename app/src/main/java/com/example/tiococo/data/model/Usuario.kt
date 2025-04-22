package com.example.tiococo.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize

data class Usuario(
    val password: String = ""
) : Parcelable
