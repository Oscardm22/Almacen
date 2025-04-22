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

    // Cambio clave: Especificar el tipo genérico explícitamente
    private var saveObserver: Observer<Boolean>? = null

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

    // 5. Función validateInputs() que faltaba (corrige errores de referencia)
    private fun validateInputs(): Boolean {
        with(binding) {
            if (etName.text.isNullOrBlank()) {
                etName.error = "Nombre requerido"
                return false
            }

            val quantityStr = etQuantity.text.toString()
            if (quantityStr.isBlank()) {
                etQuantity.error = "Cantidad requerida"
                return false
            }

            val quantity = quantityStr.toIntOrNull()
            if (quantity == null || quantity <= 0) {
                etQuantity.error = "Cantidad debe ser un número positivo"
                return false
            }

            val priceStr = etPriceDollars.text.toString()
            if (priceStr.isBlank()) {
                etPriceDollars.error = "Precio requerido"
                return false
            }

            val price = priceStr.toDoubleOrNull()
            if (price == null || price <= 0) {
                etPriceDollars.error = "Precio debe ser un número positivo"
                return false
            }

            return true
        }
    }

    private fun saveProduct() {
        if (viewModel.isSaving.value == true) {
            Log.w("SaveFlow", "Guardado en progreso - ignorando clic")
            return
        }

        binding.btnSave.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        val newProduct = createProductFromInputs()
        viewModel.addProduct(newProduct)
    }

    private fun createProductFromInputs(): Product {
        return with(binding) {
            Product(
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