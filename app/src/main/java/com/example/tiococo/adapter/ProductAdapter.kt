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
    private val onItemClick: (Product) -> Unit,
    private var exchangeRate: Double
) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) {

    private lateinit var context: Context

    // Objeto de payload para identificar cambios en la tasa
    private object PayloadExchangeRateChange

    private class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }

        // Identificar cambios específicos
        override fun getChangePayload(oldItem: Product, newItem: Product): Any? {
            return if (oldItem.priceDollars != newItem.priceDollars) {
                PayloadExchangeRateChange
            } else {
                null
            }
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
            updatePriceInBolivares(product.priceDollars)

            itemView.setOnClickListener { onItemClick(product) }
        }

        // Función específica para actualizar solo el precio en BS
        fun updatePriceInBolivares(priceDollars: Double) {
            tvPriceBolivares.text = context.getString(
                R.string.price_bolivares_label,
                priceDollars * exchangeRate
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // Método para manejar actualizaciones parciales
    override fun onBindViewHolder(
        holder: ProductViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            // Solo actualizar el precio en bolívares
            holder.updatePriceInBolivares(getItem(position).priceDollars)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    // Función optimizada para actualizar la tasa
    fun updateExchangeRate(newRate: Double) {
        if (exchangeRate != newRate) {
            exchangeRate = newRate
            // Notificar cambio solo en los items visibles
            notifyItemRangeChanged(0, itemCount, PayloadExchangeRateChange)
        }
    }
}