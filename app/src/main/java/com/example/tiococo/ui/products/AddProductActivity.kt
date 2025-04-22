package com.example.tiococo.ui.products

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.tiococo.databinding.ActivityAddProductBinding
import com.example.tiococo.data.model.Product
import com.example.tiococo.viewmodel.ProductViewModel
import android.util.Log
import android.view.View

class AddProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddProductBinding
    private val viewModel: ProductViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        binding.btnSave.setOnClickListener {
            if (validateInputs()) {
                saveProduct()
            }
        }
    }

    private fun validateInputs(): Boolean {
        with(binding) {
            if (etName.text.isNullOrEmpty()) {
                etName.error = "Nombre requerido"
                return false
            }

            if (etQuantity.text.isNullOrEmpty()) {
                etQuantity.error = "Cantidad requerida"
                return false
            }

            return true
        }
    }

    private fun saveProduct() {
        with(binding) {
            val product = Product(
                id = System.currentTimeMillis().toString(),
                name = etName.text.toString(),
                quantity = etQuantity.text.toString().toInt(),
                priceDollars = etPriceDollars.text.toString().toDoubleOrNull() ?: 0.0,
                priceBolivares = 0.0
            )
            viewModel.addProduct(product) // Asegúrate que esta función exista en tu ViewModel
        }
    }

    private fun setupObservers() {
        viewModel.saveSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Producto guardado", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}