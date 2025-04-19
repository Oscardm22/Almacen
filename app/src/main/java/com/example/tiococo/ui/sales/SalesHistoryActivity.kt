package com.example.tiococo.ui.sales

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tiococo.databinding.ActivitySalesHistoryBinding
import com.example.tiococo.data.model.SaleRecord
import com.example.tiococo.adapter.SalesHistoryAdapter
import com.example.tiococo.viewmodel.ProductViewModel
import android.widget.Toast

class SalesHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySalesHistoryBinding
    private val viewModel: ProductViewModel by viewModels()
    private lateinit var adapter: SalesHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySalesHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupSearchFunctionality()
        setupFABs()
    }

    private fun setupRecyclerView() {
        adapter = SalesHistoryAdapter(
            onItemClick = { sale -> showSaleDetails(sale) },
            onDeleteClick = { saleId, callback ->
                showDeleteConfirmationDialog(saleId, callback)
            }
        )

        binding.rvSalesHistory.apply {
            layoutManager = LinearLayoutManager(this@SalesHistoryActivity)
            adapter = this@SalesHistoryActivity.adapter
        }

        viewModel.salesHistory.observe(this) { sales ->
            adapter.submitList(sales ?: emptyList())
        }
    }

    private fun showDeleteConfirmationDialog(saleId: String, callback: (Boolean) -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar venta")
            .setMessage("¿Estás seguro de eliminar esta venta del historial?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.deleteSaleRecord(saleId)
                callback(true) // Confirmar eliminación
                Toast.makeText(this, "Venta eliminada", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar") { _, _ ->
                callback(false) // Cancelar eliminación
            }
            .setOnDismissListener {
                callback(false) // Por si se cierra el diálogo sin acción
            }
            .show()
    }

    private fun setupSearchFunctionality() {
        binding.searchView.apply {
            queryHint = "Buscar por fecha (dd/MM)"
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean = false

                override fun onQueryTextChange(newText: String?): Boolean {
                    newText?.let { query ->
                        val filteredSales = viewModel.filterSalesByDate(query)
                        adapter.submitList(filteredSales)
                    }
                    return true
                }
            })
        }
    }

    private fun setupFABs() {
        // Botón para limpiar historial
        binding.fabClearAll.setOnClickListener {
            showClearConfirmationDialog()
        }

        // Botón para exportar
        binding.fabExport.setOnClickListener {
            exportCurrentSales()
        }
    }

    private fun showSaleDetails(sale: SaleRecord) {
        AlertDialog.Builder(this)
            .setTitle("Venta del ${sale.date}")
            .setMessage(buildSaleDetailsMessage(sale))
            .setPositiveButton("Aceptar", null)
            .show()
    }

    private fun buildSaleDetailsMessage(sale: SaleRecord): String {
        return """
            Total: $${"%.2f".format(sale.total)}
            Productos: ${sale.products.size}
            
            Detalles:
            ${sale.products.joinToString("\n") {
            "- ${it.name} (${it.quantity} x $${"%.2f".format(it.priceDollars)})"
        }}
        """.trimIndent()
    }

    private fun showClearConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("¿Eliminar todo el historial?")
            .setMessage("Esta acción no se puede deshacer")
            .setPositiveButton("Confirmar") { _, _ ->
                viewModel.clearSalesHistory()
                adapter.submitList(emptyList())
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun exportCurrentSales() {
        val sales = viewModel.salesHistory.value ?: emptyList()
        if (sales.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("No hay ventas para exportar")
                .setPositiveButton("Aceptar", null)
                .show()
            return
        }

        val csvFile = exportSalesToCsv(sales)
        if (csvFile != null) {
            shareCsvFile(csvFile)
        } else {
            Toast.makeText(this, "Error al generar archivo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportSalesToCsv(sales: List<SaleRecord>): File? {
        return try {
            val fileName = "historial_ventas.csv"
            val file = File(getExternalFilesDir(null), fileName)
            val writer = file.bufferedWriter()

            writer.write("ID,Fecha,Producto,Cantidad,Precio ($),Total ($)\n")
            for (sale in sales) {
                for (product in sale.products) {
                    val total = product.quantity * product.priceDollars
                    writer.write("${sale.id},${sale.date},${product.name},${product.quantity},${product.priceDollars},$total\n")
                }
            }

            writer.flush()
            writer.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun shareCsvFile(file: File) {
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(intent, "Compartir historial de ventas"))
    }

}