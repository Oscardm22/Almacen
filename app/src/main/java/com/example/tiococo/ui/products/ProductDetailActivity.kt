package com.example.tiococo.ui.products

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.tiococo.R
import com.example.tiococo.data.model.Product

class ProductDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail)

        // Inicializar vistas
        val tvDetailName = findViewById<TextView>(R.id.tvDetailName)
        val tvDetailQuantity = findViewById<TextView>(R.id.tvDetailQuantity)
        val tvDetailPriceDollars = findViewById<TextView>(R.id.tvDetailPriceDollars)
        val tvDetailPriceBolivares = findViewById<TextView>(R.id.tvDetailPriceBolivares)
        val btnEdit = findViewById<Button>(R.id.btnEdit)
        val btnDelete = findViewById<Button>(R.id.btnDelete)

        // Recibir el producto (forma compatible)
        val product = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("PRODUCT", Product::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("PRODUCT") as? Product
        }

        if (product == null) {
            finish()
            return
        }

        // Mostrar detalles
        tvDetailName.text = getString(R.string.product_name, product.name)
        tvDetailQuantity.text = getString(R.string.product_stock, product.quantity)
        tvDetailPriceDollars.text = getString(R.string.price_dollars, product.priceDollars)
        tvDetailPriceBolivares.text = getString(R.string.price_bolivares, product.priceBolivares)

        // Botón Editar
        btnEdit.setOnClickListener {
            Intent(this, EditProductActivity::class.java).apply {
                putExtra("PRODUCT", product)
                startActivity(this)
            }
        }

        // Botón Eliminar
        btnDelete.setOnClickListener {
            AlertDialog.Builder(this).apply {
                setTitle(getString(R.string.delete_confirmation_title))
                setMessage(getString(R.string.delete_confirmation_message))
                setPositiveButton(getString(R.string.yes)) { _, _ ->
                    setResult(RESULT_OK, Intent().apply {
                        putExtra("DELETED_PRODUCT_ID", product.id)
                    })
                    finish()
                }
                setNegativeButton(getString(R.string.no), null)
                show()
            }
        }
    }
}