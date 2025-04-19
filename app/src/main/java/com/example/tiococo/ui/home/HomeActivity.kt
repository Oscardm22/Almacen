package com.example.tiococo.ui.home

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.tiococo.R
import com.example.tiococo.adapter.ProductAdapter
import com.example.tiococo.data.model.Product
import com.example.tiococo.databinding.ActivityHomeBinding
import com.example.tiococo.ui.products.AddProductActivity
import com.example.tiococo.ui.products.ProductDetailActivity
import com.example.tiococo.ui.sales.SalesActivity
import com.example.tiococo.ui.sales.SalesHistoryActivity
import com.example.tiococo.viewmodel.ProductViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.text.TextWatcher
import android.text.Editable
import androidx.appcompat.app.AlertDialog
import com.example.tiococo.ui.auth.LoginActivity
import com.example.tiococo.ui.user.UserManagementActivity
import androidx.core.content.edit
import android.os.Build

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val viewModel: ProductViewModel by viewModels()
    private lateinit var productAdapter: ProductAdapter
    private var searchJob: Job? = null

    // Contracts para lanzar Activities con resultado
    private val detailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { data ->
                when (data.getStringExtra("ACTION")) {
                    "DELETE" -> handleDelete(data)
                    "EDIT" -> handleEdit(data)
                }
            }
        }
    }

    private val addProductLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, "Producto agregado exitosamente", Toast.LENGTH_SHORT).show()
        }
    }

    private val salesLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, "Venta registrada", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUserButton()
        setupLogoutButton()
        setupRecyclerView()
        setupObservers()
        setupFab()
        setupSearchView()
        setupRateObserver()
        setupRateButton()
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter { selectedProduct ->
            Intent(this, ProductDetailActivity::class.java).apply {
                putExtra("PRODUCT", selectedProduct)
            }.also { detailLauncher.launch(it) }
        }

        binding.rvProducts.apply {
            adapter = productAdapter
        }
    }

    private fun setupObservers() {
        viewModel.products.observe(this) { products ->
            productAdapter.submitList(products)
        }

        viewModel.exchangeRate.observe(this) { rate ->
            binding.tvExchangeRate.text = getString(R.string.exchange_rate_format, "%.2f".format(rate))
        }

        viewModel.rateUpdateMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearRateUpdateMessage()
            }
        }
    }

    private fun setupFab() {
        binding.fabAddProduct.setOnClickListener {
            toggleSecondaryButtons()
        }

        val secondaryButtons = listOf(binding.btnOption1, binding.btnOption2, binding.btnOption3)

        secondaryButtons.forEach { button ->
            button.setOnClickListener {
                button.isEnabled = false

                when (button.id) {
                    R.id.btnOption1 -> startAddProductActivity()
                    R.id.btnOption2 -> startSalesActivity()
                    R.id.btnOption3 -> startSalesHistoryActivity()
                }

                hideSecondaryButtons()
                button.postDelayed({ button.isEnabled = true }, 300)
            }
        }
    }

    private fun setupSearchView() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300)
                    viewModel.searchProducts(s?.toString() ?: "")
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun startAddProductActivity() {
        val intent = Intent(this, AddProductActivity::class.java)
        addProductLauncher.launch(intent)
    }

    private fun startSalesActivity() {
        val intent = Intent(this, SalesActivity::class.java).apply {
            putParcelableArrayListExtra("products", ArrayList(viewModel.products.value ?: emptyList()))
        }
        salesLauncher.launch(intent)
    }

    private fun startSalesHistoryActivity() {
        val intent = Intent(this, SalesHistoryActivity::class.java)
        startActivity(intent)
    }

    private fun toggleSecondaryButtons() {
        with(binding) {
            if (btnOption1.isVisible) {
                hideSecondaryButtons()
            } else {
                showSecondaryButtons()
            }
        }
    }

    private fun showSecondaryButtons() {
        with(binding) {
            btnOption1.isVisible = true
            btnOption2.isVisible = true
            btnOption3.isVisible = true
            btnOption1.alpha = 0f
            btnOption2.alpha = 0f
            btnOption3.alpha = 0f
            btnOption1.animate().alpha(1f).setDuration(200).start()
            btnOption2.animate().alpha(1f).setDuration(200).start()
            btnOption3.animate().alpha(1f).setDuration(200).start()
        }
    }

    private fun hideSecondaryButtons() {
        with(binding) {
            listOf(btnOption1, btnOption2, btnOption3).forEach { button ->
                button.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction {
                        button.isVisible = false
                        button.isEnabled = true
                    }
                    .start()
            }
        }
    }

    private fun handleDelete(data: Intent) {
        data.getStringExtra("DELETED_PRODUCT_ID")?.let { id ->
            viewModel.deleteProduct(id)
        }
    }

    private fun handleEdit(data: Intent) {
        val editedProduct = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            data.getParcelableExtra("EDITED_PRODUCT", Product::class.java)
        } else {
            @Suppress("DEPRECATION")
            data.getParcelableExtra("EDITED_PRODUCT")
        }
        editedProduct?.let { viewModel.updateProduct(it) }
    }
    private fun setupRateObserver() {
        viewModel.exchangeRate.observe(this) { rate ->
            binding.tvExchangeRate.text = getString(R.string.exchange_rate_format, "%.2f".format(rate))

            // Opcional: Mostrar notificación cuando cambia
            Toast.makeText(this, "Tasa actualizada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRateButton() {
        binding.btnUpdateRate.setOnClickListener {
            // Animación de rotación
            binding.btnUpdateRate.animate().rotationBy(360f).setDuration(300).start()

            lifecycleScope.launch {
                viewModel.refreshExchangeRate()
            }
        }
    }
    override fun onDestroy() {
        searchJob?.cancel()
        super.onDestroy()
    }
}