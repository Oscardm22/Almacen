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
import android.view.View
import java.io.File
import android.graphics.pdf.PdfDocument
import android.graphics.Paint
import androidx.core.graphics.blue
import android.graphics.Canvas
import androidx.core.content.FileProvider
import android.util.Log
import android.view.MotionEvent

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
        when (result.resultCode) {
            RESULT_OK -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    result.data?.getParcelableExtra("NEW_PRODUCT", Product::class.java)?.let { newProduct ->
                        viewModel.addProduct(newProduct)
                        Toast.makeText(this, "Producto agregado", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    @Suppress("DEPRECATION")
                    result.data?.getParcelableExtra<Product>("NEW_PRODUCT")?.let { newProduct ->
                        viewModel.addProduct(newProduct)
                        Toast.makeText(this, "Producto agregado", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            RESULT_CANCELED -> {
                result.data?.getStringExtra("ERROR")?.let { error ->
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                }
            }
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
        setupTouchListener()
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(
            onItemClick = { product ->
                Intent(this, ProductDetailActivity::class.java).apply {
                    putExtra("PRODUCT_ID", product.id)  // Usamos product.id directamente
                    putExtra("PRODUCT", product)
                }.also { detailLauncher.launch(it) }
            },
            exchangeRate = viewModel.exchangeRate.value ?: 1.0
        )
        binding.rvProducts.adapter = productAdapter
    }

    private fun setupObservers() {
        viewModel.products.observe(this) { products ->
            productAdapter.submitList(products)  // Pasamos la lista directamente
        }

        viewModel.exchangeRate.observe(this) { rate ->
            binding.tvExchangeRate.text = getString(R.string.exchange_rate_format, "%.2f".format(rate))
        }

        // Observar el estado de la tasa de cambio (loading, success, error)
        viewModel.rateState.observe(this) { state ->
            when (state) {
                ProductViewModel.RateState.LOADING -> {
                    // Mostrar ProgressBar y ocultar la tasa
                    binding.progressBar.visibility = View.VISIBLE
                    binding.tvExchangeRate.visibility = View.GONE
                }
                ProductViewModel.RateState.SUCCESS -> {
                    // Ocultar ProgressBar y mostrar la tasa
                    binding.progressBar.visibility = View.GONE
                    binding.tvExchangeRate.visibility = View.VISIBLE
                }
                ProductViewModel.RateState.ERROR -> {
                    // Ocultar ProgressBar y mostrar un mensaje de error
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Error al actualizar la tasa", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    // Estado por defecto
                    binding.progressBar.visibility = View.GONE
                    binding.tvExchangeRate.visibility = View.VISIBLE
                }
            }
        }

        viewModel.rateUpdateMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearRateUpdateMessage()
            }
        }
    }

    private fun setupTouchListener() {
        binding.root.apply {
            // Configurar accesibilidad
            isClickable = true
            contentDescription = "Área táctil para ocultar botones"

            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        if (shouldHideButtons()) {
                            hideSecondaryButtons()
                        }
                        v.performClick()
                        true // Consumir el evento
                    }
                    else -> false
                }
            }
        }
    }

    private fun shouldHideButtons(): Boolean {
        return binding.btnOption1.isVisible ||
                binding.btnOption2.isVisible ||
                binding.btnOption3.isVisible ||
                binding.btnOption4.isVisible
    }
        private fun setupFab() {
        binding.fabAddProduct.setOnClickListener {
            toggleSecondaryButtons()
        }

        val secondaryButtons = listOf(binding.btnOption1, binding.btnOption2, binding.btnOption3, binding.btnOption4)

        secondaryButtons.forEach { button ->
            button.setOnClickListener {
                button.isEnabled = false

                when (button.id) {
                    R.id.btnOption1 -> startAddProductActivity()
                    R.id.btnOption2 -> startSalesActivity()
                    R.id.btnOption3 -> startSalesHistoryActivity()
                    R.id.btnOption4 -> showExportInventoryDialog()
                }

                hideSecondaryButtons()
                button.postDelayed({ button.isEnabled = true }, 300)
            }
        }
    }

    private fun showExportInventoryDialog() {
        val options = arrayOf("Exportar a CSV", "Exportar a PDF")
        AlertDialog.Builder(this)
            .setTitle("Exportar inventario")
            .setItems(options) { _, which ->
                val products = viewModel.products.value ?: emptyList()
                when (which) {
                    0 -> exportInventoryToCsv(products)
                    1 -> exportInventoryToPdf(products) // Ya no necesitas .map { it.second }
                }
            }
            .show()
    }

    private fun exportInventoryToCsv(products: List<Product>) {
        try {
            val file = File(getExternalFilesDir(null), "inventario.csv")
            val writer = file.bufferedWriter()
            writer.write("ID,Nombre,Cantidad,Precio ($),Precio (Bs)\n")
            for (product in products) {
                val priceInBolivares = calculatePriceInBolivares(
                    product.priceDollars,
                    viewModel.exchangeRate.value ?: 1.0
                )
                writer.write("${product.id},${product.name},${product.quantity},${product.priceDollars},$priceInBolivares\n")
            }
            writer.flush()
            writer.close()
            shareFile(file, "text/csv", "Compartir inventario (CSV)")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al exportar CSV", Toast.LENGTH_SHORT).show()
        }
    }

    private fun calculatePriceInBolivares(priceDollars: Double, exchangeRate: Double): Double {
        return priceDollars * exchangeRate // Ahora usa el parámetro exchangeRate
    }

    private fun exportInventoryToPdf(products: List<Product>) {
        try {
            val document = PdfDocument()
            val paint = Paint()
            val titlePaint = Paint().apply {
                color = color.blue
                textSize = 16f
                style = Paint.Style.FILL
            }



            var pageNumber = 1
            var y = 40
            var page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
            val canvas: Canvas = page.canvas

            canvas.drawText("Inventario de Productos", 40f, y.toFloat(), titlePaint)
            y += 30
            canvas.drawText("Nombre | Cantidad | $ | Bs", 40f, y.toFloat(), paint)
            y += 20

            for (product in products) {
                val priceInBolivares = product.priceDollars * (viewModel.exchangeRate.value ?: 1.0)
                val line = "${product.name} | ${product.quantity} | $${product.priceDollars} | Bs ${"%.2f".format(priceInBolivares)}"
                if (y >= 800) {
                    document.finishPage(page)
                    pageNumber++
                    page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
                    canvas.drawText("Inventario (cont.)", 40f, 40f, titlePaint)
                    y = 70
                }
                page.canvas.drawText(line, 40f, y.toFloat(), paint)
                y += 20
            }

            document.finishPage(page)

            val file = File(getExternalFilesDir(null), "inventario.pdf")
            document.writeTo(file.outputStream())
            document.close()
            shareFile(file, "application/pdf", "Compartir inventario (PDF)")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al exportar PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareFile(file: File, mimeType: String, title: String) {
        val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, title))
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
        // En lugar de pasar toda la lista, pasa solo lo necesario
        intent.putExtra("products_count", viewModel.products.value?.size ?: 0)
        addProductLauncher.launch(intent)
    }

    private fun startSalesActivity() {
        val intent = Intent(this, SalesActivity::class.java)
        // Considera pasar solo IDs o datos esenciales
        intent.putExtra("products_count", viewModel.products.value?.size ?: 0)
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
            btnOption4.isVisible = true
            btnOption1.alpha = 0f
            btnOption2.alpha = 0f
            btnOption3.alpha = 0f
            btnOption4.alpha = 0f
            btnOption1.animate().alpha(1f).setDuration(200).start()
            btnOption2.animate().alpha(1f).setDuration(200).start()
            btnOption3.animate().alpha(1f).setDuration(200).start()
            btnOption4.animate().alpha(1f).setDuration(200).start()
        }
    }

    private fun hideSecondaryButtons() {
        with(binding) {
            listOf(btnOption1, btnOption2, btnOption3, btnOption4).forEach { button ->
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
        val id = data.getStringExtra("PRODUCT_ID") ?: return
        val editedProduct = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            data.getParcelableExtra("EDITED_PRODUCT", Product::class.java)
        } else {
            @Suppress("DEPRECATION")
            data.getParcelableExtra<Product>("EDITED_PRODUCT")
        }
        editedProduct?.let { viewModel.updateProduct(id, it) }
    }
    private fun setupRateObserver() {
        viewModel.exchangeRate.observe(this) { rate ->
            binding.tvExchangeRate.text = getString(R.string.exchange_rate_format, "%.2f".format(rate))
        }
    }

    private fun setupRateButton() {
        binding.btnUpdateRate.setOnClickListener {
            binding.btnUpdateRate.animate().rotationBy(360f).setDuration(300).start()
            lifecycleScope.launch {
                viewModel.refreshExchangeRate()
            }
        }
    }

    private fun setupUserButton() {
        binding.fabManageUsers.setOnClickListener {
            startActivity(Intent(this, UserManagementActivity::class.java))
        }
    }

    private fun setupLogoutButton() {
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Estás seguro de que deseas salir?")
            .setPositiveButton("Sí") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun performLogout() {
        binding.root.animate().alpha(0.5f).setDuration(300).withEndAction {
// Versión corregida
            getSharedPreferences("user_prefs", MODE_PRIVATE).edit { clear() }
            val intent = Intent(this@HomeActivity, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)

            // Solución compatible con todas las versiones
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.fade_in, R.anim.fade_out)
            } else {
                @Suppress("DEPRECATION")
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            }
            finish()
        }.start()
    }

    override fun onDestroy() {
        searchJob?.cancel()
        super.onDestroy()
    }
}