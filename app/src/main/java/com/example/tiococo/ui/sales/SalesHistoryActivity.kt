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
import java.io.File
import androidx.core.content.FileProvider
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument

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
            },
            onReturnClick = { sale, callback ->
                showReturnConfirmationDialog(sale, callback)
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

    private fun showReturnConfirmationDialog(sale: SaleRecord, callback: (Boolean) -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar devolución")
            .setMessage("¿Estás seguro de revertir esta venta y restaurar el stock de los productos?")
            .setPositiveButton("Confirmar") { _, _ ->
                viewModel.returnSale(sale) { success ->
                    runOnUiThread {
                        if (success) {
                            Toast.makeText(
                                this,
                                "Devolución procesada - Stock restaurado",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this,
                                "Error al procesar devolución",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        callback(success)
                    }
                }
            }
            .setNegativeButton("Cancelar") { _, _ -> callback(false) }
            .setOnDismissListener { callback(false) }
            .show()
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
        binding.fabClearAll.setOnClickListener {
            showClearConfirmationDialog()
        }

        binding.fabExport.setOnClickListener {
            showExportOptionsDialog()
        }
    }

    private fun showExportOptionsDialog() {
        val options = arrayOf("Exportar a CSV", "Exportar a PDF")
        AlertDialog.Builder(this)
            .setTitle("Selecciona formato de exportación")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> exportToCsv()
                    1 -> exportToPdf()
                }
            }
            .show()
    }

    private fun showSaleDetails(sale: SaleRecord) {
        SaleDetailActivity.start(this, sale)
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

    private fun exportToCsv() {
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

    private fun exportToPdf() {
        val sales = viewModel.salesHistory.value ?: emptyList()
        if (sales.isEmpty()) {
            Toast.makeText(this, "No hay ventas para exportar", Toast.LENGTH_SHORT).show()
            return
        }

        val document = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint().apply {
            textSize = 16f
            isFakeBoldText = true
        }

        var pageNumber = 1
        var y = 40
        var page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
        val canvas: Canvas = page.canvas

        canvas.drawText("Historial de Ventas", 40f, y.toFloat(), titlePaint)
        y += 30
        canvas.drawText("Fecha | Producto | Cantidad | Precio | Total", 40f, y.toFloat(), paint)
        y += 20

        for (sale in sales) {
            for (product in sale.products) {
                val line = "${sale.date} | ${product.name} | ${product.quantity} | $${"%.2f".format(product.priceDollars)} | $${"%.2f".format(product.quantity * product.priceDollars)}"
                if (y >= 800) {
                    document.finishPage(page)
                    pageNumber++
                    page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
                    canvas.drawText("Historial de Ventas (cont.)", 40f, 40f, titlePaint)
                    y = 70
                }
                page.canvas.drawText(line, 40f, y.toFloat(), paint)
                y += 20
            }
        }

        document.finishPage(page)

        try {
            val file = File(getExternalFilesDir(null), "historial_ventas.pdf")
            document.writeTo(file.outputStream())
            document.close()
            sharePdfFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al generar PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sharePdfFile(file: File) {
        val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Compartir historial de ventas"))
    }
}