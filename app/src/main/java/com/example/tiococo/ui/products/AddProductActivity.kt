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
import android.text.Editable
import android.text.TextWatcher

class AddProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddProductBinding
    private val viewModel: ProductViewModel by viewModels()
    private var saveObserver: Observer<Boolean>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupUI()
        setupObservers()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Agregar Producto"
        }
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupUI() {
        // Validación en tiempo real - Nombre
        binding.etName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s != null && s.length > 30) {
                    binding.etName.error = "Máximo 30 caracteres"
                } else {
                    binding.etName.error = null
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Validación en tiempo real - Cantidad
        binding.etQuantity.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s != null && s.length > 5) {
                    binding.etQuantity.error = "Máximo 5 dígitos"
                } else {
                    binding.etQuantity.error = null
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Validación en tiempo real - Precio
        binding.etPriceDollars.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s != null && s.length > 10) {
                    binding.etPriceDollars.error = "Máximo 10 caracteres"
                } else {
                    binding.etPriceDollars.error = null
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

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

    private fun showProgress(show: Boolean) {
        if (show) {
            binding.toolbarProgress.visibility = View.VISIBLE
            binding.toolbarProgress.animate()
                .alpha(1f)
                .setDuration(200)
                .start()
        } else {
            binding.toolbarProgress.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    binding.toolbarProgress.visibility = View.INVISIBLE
                }
                .start()
        }
        binding.btnSave.isEnabled = !show
    }

    private fun saveProduct() {
        if (viewModel.isSaving.value == true) {
            Log.w("SaveFlow", "Guardado en progreso - ignorando clic")
            return
        }

        showProgress(true)
        val newProduct = createProductFromInputs()
        viewModel.addProduct(newProduct)
    }

    private fun createProductFromInputs(): Product {
        return with(binding) {
            Product(
                name = etName.text.toString(),
                quantity = etQuantity.text.toString().toInt(),
                priceDollars = etPriceDollars.text.toString().toDouble()
            )
        }
    }

    private fun setupObservers() {
        saveObserver?.let { viewModel.saveSuccess.removeObserver(it) }

        saveObserver = Observer<Boolean> { success ->
            showProgress(false)

            if (success) {
                Toast.makeText(this, "Producto guardado", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Error al guardar producto", Toast.LENGTH_SHORT).show()
            }
        }.also {
            viewModel.saveSuccess.observe(this, it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        saveObserver?.let { viewModel.saveSuccess.removeObserver(it) }
    }
}