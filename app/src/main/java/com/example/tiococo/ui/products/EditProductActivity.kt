package com.example.tiococo.ui.products

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.tiococo.R
import com.example.tiococo.data.model.Product
import java.util.Locale

class EditProductActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_product)

        // Forma moderna de obtener Parcelable (para Android 12+)
        val product = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("PRODUCT", Product::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("PRODUCT")
        }

        // Inicializar vistas
        val etName = findViewById<EditText>(R.id.etProductName)
        val etQuantity = findViewById<EditText>(R.id.etQuantity)
        val etPriceDollars = findViewById<EditText>(R.id.etPriceDollars)

        // Mostrar datos actuales
        product?.let {
            etName.setText(it.name)
            etQuantity.setText(it.quantity.toString())
            etPriceDollars.setText(String.format(Locale.getDefault(), "%.2f", it.priceDollars))
            etPriceBolivares.setText(String.format(Locale.getDefault(), "%.2f", it.priceBolivares))
        }

        // Configurar botón de guardar
        findViewById<Button>(R.id.btnSave).setOnClickListener {
            val updatedProduct = Product(
                id = product?.id ?: "",
                name = etName.text.toString(),
                quantity = etQuantity.text.toString().toIntOrNull() ?: 0,
                priceDollars = etPriceDollars.text.toString().toDoubleOrNull() ?: 0.0,
                priceBolivares = etPriceBolivares.text.toString().toDoubleOrNull() ?: 0.0
            )

            val resultIntent = Intent().apply {
                putExtra("UPDATED_PRODUCT", updatedProduct)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }
}