package com.example.tiococo.ui.sales

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.tiococo.R
import com.example.tiococo.databinding.ActivitySalesBinding
import com.example.tiococo.viewmodel.ProductViewModel
import com.example.tiococo.adapter.ProductAdapter
import com.example.tiococo.adapter.SaleProductsAdapter
import com.example.tiococo.data.model.Product

class SalesActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySalesBinding
    private val viewModel: ProductViewModel by viewModels()
    private lateinit var availableProductsAdapter: ProductAdapter
    private lateinit var saleProductsAdapter: SaleProductsAdapter

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
                // Pasa solo el producto (sin ID por separado)
                viewModel.addToSale(product.copy(quantity = 1)) // Cantidad inicial 1
            },
            exchangeRate = viewModel.exchangeRate.value ?: 1.0
        )

        saleProductsAdapter = SaleProductsAdapter(
            onRemoveClick = { product ->
                // Pasa el producto completo
                viewModel.removeFromSale(product)
            },
            onQuantityChange = { product, newQuantity ->
                if (newQuantity > 0) {
                    // Pasa el producto completo y la nueva cantidad
                    viewModel.updateSaleProductQuantity(product, newQuantity)
                } else {
                    viewModel.removeFromSale(product)
                }
            }
        )
    }

    private fun setupObservers() {
        viewModel.products.observe(this) { products ->
            availableProductsAdapter.submitList(products)
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
            viewModel.totalAmount.value?.let { totalUsd ->
                // Cuando la tasa cambia, actualizar el total en Bs
                val totalBs = totalUsd * rate
                binding.tvTotalBs.text = getString(R.string.total_bs_format, totalBs)
            }
        }
    }

    private fun setupUI() {
        binding.fabComplete.setOnClickListener {
            if (viewModel.saleProducts.value?.isNotEmpty() == true) {
                viewModel.registerSale()
                Toast.makeText(this, R.string.sale_registered, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, R.string.add_products_first, Toast.LENGTH_SHORT).show()
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