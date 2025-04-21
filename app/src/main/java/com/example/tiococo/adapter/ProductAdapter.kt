package com.example.tiococo.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tiococo.R
import com.example.tiococo.data.model.Product

class ProductAdapter(
    private val onItemClick: (Product) -> Unit,  // Solo recibe Product (corregido)
    private val exchangeRate: Double
) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) {

    private lateinit var context: Context

    private class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.id == newItem.id  // Compara por ID
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        private val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        private val tvPriceDollars: TextView = itemView.findViewById(R.id.tvPriceDollars)
        private val tvPriceBolivares: TextView = itemView.findViewById(R.id.tvPriceBolivares)

        fun bind(product: Product) {
            tvProductName.text = product.name
            tvQuantity.text = context.getString(R.string.stock_label, product.quantity)
            tvPriceDollars.text = context.getString(R.string.price_dollars_label, product.priceDollars)

            val priceInBolivares = product.priceDollars * exchangeRate
            tvPriceBolivares.text = context.getString(R.string.price_bolivares_label, priceInBolivares)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = getItem(position)  // Obtenemos directamente el Product
        holder.bind(product)

        // Pasamos solo el Product al hacer click
        holder.itemView.setOnClickListener {
            onItemClick(product)
        }
    }
}