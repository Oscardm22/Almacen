package com.example.tiococo.ui.sales

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tiococo.R
import com.example.tiococo.databinding.ActivitySalesBinding
import com.example.tiococo.viewmodel.ProductViewModel
import com.example.tiococo.adapter.ProductAdapter
import com.example.tiococo.adapter.SaleProductsAdapter
import com.example.tiococo.data.model.Product
import com.example.tiococo.data.model.SaleRecord
import androidx.lifecycle.lifecycleScope
import com.example.tiococo.data.repository.ProductRepository
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import kotlinx.coroutines.launch
import com.example.tiococo.data.repository.SaleRepository

class SalesActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySalesBinding
    private val viewModel: ProductViewModel by viewModels()
    private lateinit var availableProductsAdapter: ProductAdapter
    private lateinit var saleProductsAdapter: SaleProductsAdapter

    // Declara primero el ProductRepository
    private val productRepository: ProductRepository by lazy { ProductRepository() }
    // Luego instancia SaleRepository con la dependencia
    private val saleRepository: SaleRepository by lazy { SaleRepository(productRepository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySalesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAdapters()
        setupObservers()
        setupUI()
        setupSearchWithEditText()
    }

    private fun setupAdapters() {
        availableProductsAdapter = ProductAdapter(
            onItemClick = { product: Product ->
                viewModel.addToSale(product.copy(quantity = 1))
            },
            exchangeRate = viewModel.exchangeRate.value ?: 1.0
        ).also {
            binding.rvAvailableProducts.adapter = it
            binding.rvAvailableProducts.layoutManager = LinearLayoutManager(this)
        }

        saleProductsAdapter = SaleProductsAdapter(
            onRemoveClick = { product -> viewModel.removeFromSale(product) },
            onQuantityChange = { product, newQuantity ->
                if (newQuantity > 0) viewModel.updateSaleProductQuantity(product, newQuantity)
                else viewModel.removeFromSale(product)
            }
        ).also {
            binding.rvSaleProducts.adapter = it
            binding.rvSaleProducts.layoutManager = LinearLayoutManager(this)
        }
    }

    private fun setupObservers() {
        viewModel.products.observe(this) { products ->
            Log.d("SalesActivity", "Productos recibidos: ${products.size}")
            availableProductsAdapter.submitList(products)
            binding.rvAvailableProducts.scheduleLayoutAnimation() // Animación para refrescar

            binding.rvAvailableProducts.apply {
                if (adapter == null) {
                    adapter = availableProductsAdapter
                }
            }
        }

        viewModel.saleProducts.observe(this) { saleProducts ->
            saleProductsAdapter.submitList(saleProducts)
        }

        // Observar el total en USD
        viewModel.totalAmount.observe(this) { totalUsd ->
            binding.tvTotalUsd.text = getString(R.string.total_usd_format, totalUsd)

            // Actualizar el total en Bs con la tasa actual
            val rate = viewModel.exchangeRate.value ?: 36.0
            val totalBs = totalUsd * rate
            binding.tvTotalBs.text = getString(R.string.total_bs_format, totalBs)
        }

        // Observar el cambio de tasa de cambio
        viewModel.exchangeRate.observe(this) { rate ->
            // Actualizar los adapters con la nueva tasa
            availableProductsAdapter.updateExchangeRate(rate)
            saleProductsAdapter.updateExchangeRate(rate)

            viewModel.totalAmount.value?.let { totalUsd ->
                val totalBs = totalUsd * rate
                binding.tvTotalBs.text = getString(R.string.total_bs_format, totalBs)
            }
        }
    }

    private fun setupUI() {
        binding.fabComplete.setOnClickListener {
            val products = viewModel.saleProducts.value ?: emptyList()
            if (products.isNotEmpty()) {
                val sale = SaleRecord(
                    id = UUID.randomUUID().toString(),
                    date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()),
                    totalDollars = viewModel.totalAmount.value ?: 0.0,
                    exchangeRate = viewModel.exchangeRate.value ?: 1.0,
                    products = products.toList()
                )
                lifecycleScope.launch {
                    try {
                        val success = saleRepository.saveSaleWithStockUpdate(sale)
                        if (success) {
                            Toast.makeText(this@SalesActivity, "Venta registrada", Toast.LENGTH_SHORT).show()
                            viewModel.clearSale()
                        } else {
                            Toast.makeText(this@SalesActivity, "Error al registrar venta", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@SalesActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        binding.fabClear.setOnClickListener {
            viewModel.clearSale()
        }
    }


    private fun setupSearchWithEditText() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            private var lastSearchTime = 0L
            private val SEARCH_DELAY = 300L

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (System.currentTimeMillis() - lastSearchTime > SEARCH_DELAY) {
                    viewModel.searchProducts(s.toString())
                    lastSearchTime = System.currentTimeMillis()
                }
            }
        })

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                viewModel.searchProducts(binding.etSearch.text.toString())
                true
            } else {
                false
            }
        }
    }
}