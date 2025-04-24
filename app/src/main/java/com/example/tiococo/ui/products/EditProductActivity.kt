package com.example.tiococo.ui.products

import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.tiococo.R
import com.example.tiococo.data.model.Product
import com.example.tiococo.viewmodel.ProductViewModel
import java.util.Locale
import android.text.Editable
import android.text.TextWatcher


class EditProductActivity : AppCompatActivity() {

    private val viewModel: ProductViewModel by viewModels()
    private var originalProduct: Product? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_product)

        val etName = findViewById<EditText>(R.id.etProductName)
        val etQuantity = findViewById<EditText>(R.id.etQuantity)
        val etPriceDollars = findViewById<EditText>(R.id.etPriceDollars)
        val btnSave = findViewById<Button>(R.id.btnSave)

        // Obtener el producto a editar
        originalProduct = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("PRODUCT", Product::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("PRODUCT") as? Product
        }

        originalProduct?.let { product ->
            etName.setText(product.name)
            etQuantity.setText(product.quantity.toString())
            etPriceDollars.setText(String.format(Locale.getDefault(), "%.2f", product.priceDollars))
        }

        // 🔽 Validaciones en tiempo real
        etName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                etName.error = if (s != null && s.length > 30) "Máximo 30 caracteres" else null
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        etQuantity.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                etQuantity.error = if (s != null && s.length > 5) "Máximo 5 dígitos" else null
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        etPriceDollars.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                etPriceDollars.error = if (s != null && s.length > 10) "Máximo 10 caracteres" else null
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnSave.setOnClickListener {
            val updatedProduct = originalProduct?.copy(
                name = etName.text.toString(),
                quantity = etQuantity.text.toString().toIntOrNull() ?: 0,
                priceDollars = etPriceDollars.text.toString().toDoubleOrNull() ?: 0.0
            )

            if (updatedProduct != null) {
                viewModel.updateProduct(updatedProduct.id, updatedProduct)
                // Añadir resultado exitoso
                setResult(RESULT_OK)
                finish()
            } else {
                Toast.makeText(this, "Error al actualizar el producto", Toast.LENGTH_SHORT).show()
            }
        }
    }
}