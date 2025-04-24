package com.example.tiococo.ui.sales

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tiococo.databinding.ActivitySaleDetailBinding
import com.example.tiococo.data.model.SaleRecord
import com.example.tiococo.adapter.SaleProductDetailAdapter
import com.example.tiococo.R

class SaleDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySaleDetailBinding

    companion object {
        private const val EXTRA_SALE = "extra_sale"

        fun start(context: Context, sale: SaleRecord) {
            val intent = Intent(context, SaleDetailActivity::class.java).apply {
                putExtra(EXTRA_SALE, sale)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySaleDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        @Suppress("DEPRECATION")
        val sale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_SALE, SaleRecord::class.java)
        } else {
            intent.getParcelableExtra(EXTRA_SALE) as? SaleRecord
        } ?: run {
            finish()
            return
        }

        setupViews(sale)
    }

    private fun setupViews(sale: SaleRecord) {
        // Información general - Versión corregida
        binding.tvSaleDate.text = getString(R.string.sale_date, sale.date)
        binding.tvTotalUsd.text = getString(R.string.total_usd, sale.totalDollars)
        binding.tvTotalBs.text = getString(R.string.total_bs, sale.totalBs)
        binding.tvItemsCount.text = getString(R.string.products_count, sale.products.size)

        // Lista de productos (sin cambios)
        val adapter = SaleProductDetailAdapter()
        binding.rvProducts.layoutManager = LinearLayoutManager(this)
        binding.rvProducts.adapter = adapter
        adapter.submitList(sale.products)

        // Botón de cierre (sin cambios)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }
}